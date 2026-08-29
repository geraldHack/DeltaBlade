#!/usr/bin/env python3
"""
Procedural explosion sprite sheet generator using Pillow.
Generates 8-frame 64x64 pixel-art explosion animations.
Output: 512x64 horizontal strips, 8-bit RGBA PNG (colortype 6).
"""

import math
import random
import base64
import struct
import zlib
from PIL import Image, ImageDraw, ImageFilter

FRAME_SIZE = 64
FRAME_COUNT = 8
SHEET_WIDTH = FRAME_SIZE * FRAME_COUNT
SHEET_HEIGHT = FRAME_SIZE

random.seed(42)


class Particle:
    def __init__(self, x, y, vx, vy, life, size, color, particle_type):
        self.x = x
        self.y = y
        self.vx = vx
        self.vy = vy
        self.life = life
        self.max_life = life
        self.size = size
        self.color = color
        self.particle_type = particle_type

    def update(self, dt=1.0):
        self.x += self.vx * dt
        self.y += self.vy * dt
        self.life -= dt
        if self.particle_type == 'smoke':
            self.vy -= 0.3 * dt
            self.vx *= 0.95
        elif self.particle_type == 'debris':
            self.vy += 0.5 * dt
        elif self.particle_type == 'spark':
            self.vx *= 0.9
            self.vy *= 0.9

    @property
    def alpha(self):
        if self.life <= 0:
            return 0
        ratio = self.life / self.max_life
        if self.particle_type == 'flash':
            return int(255 * ratio * ratio)
        elif self.particle_type == 'fire':
            return int(200 * ratio)
        elif self.particle_type == 'smoke':
            return int(150 * (1 - ratio) * ratio * 4)
        elif self.particle_type == 'spark':
            return int(255 * ratio)
        elif self.particle_type == 'debris':
            return int(200 * ratio)
        return int(255 * ratio)


def blend_additive(base, overlay):
    """Additive blend for fire effects."""
    r = min(255, base[0] + overlay[0])
    g = min(255, base[1] + overlay[1])
    b = min(255, base[2] + overlay[2])
    a = min(255, base[3] + overlay[3])
    return (r, g, b, a)


def blend_alpha_over(base, overlay):
    """Alpha-over compositing for smoke."""
    oa = overlay[3] / 255.0
    ba = base[3] / 255.0
    out_a = oa + ba * (1 - oa)
    if out_a == 0:
        return (0, 0, 0, 0)
    out_r = int((overlay[0] * oa + base[0] * ba * (1 - oa)) / out_a)
    out_g = int((overlay[1] * oa + base[1] * ba * (1 - oa)) / out_a)
    out_b = int((overlay[2] * oa + base[2] * ba * (1 - oa)) / out_a)
    return (out_r, out_g, out_b, int(out_a * 255))


def draw_circle_soft(img, cx, cy, radius, color):
    """Draw a soft-edged circle with the given color (RGBA)."""
    pixels = img.load()
    w, h = img.size
    r2 = radius * radius
    for y in range(max(0, int(cy - radius - 2)), min(h, int(cy + radius + 3))):
        for x in range(max(0, int(cx - radius - 2)), min(w, int(cx + radius + 3))):
            dist2 = (x - cx) ** 2 + (y - cy) ** 2
            if dist2 < r2:
                falloff = 1.0 - (dist2 / r2) ** 0.5
                alpha = int(color[3] * falloff)
                if alpha > 0:
                    overlay = (color[0], color[1], color[2], alpha)
                    base = pixels[x, y]
                    pixels[x, y] = blend_additive(base, overlay)


def draw_ring(img, cx, cy, radius, thickness, color):
    """Draw a ring (shockwave)."""
    pixels = img.load()
    w, h = img.size
    outer_r2 = (radius + thickness/2) ** 2
    inner_r2 = max(0, radius - thickness/2) ** 2
    for y in range(max(0, int(cy - radius - thickness)), min(h, int(cy + radius + thickness + 1))):
        for x in range(max(0, int(cx - radius - thickness)), min(w, int(cx + radius + thickness + 1))):
            dist2 = (x - cx) ** 2 + (y - cy) ** 2
            if inner_r2 <= dist2 <= outer_r2:
                dist_from_ring = abs(math.sqrt(dist2) - radius)
                falloff = 1.0 - (dist_from_ring / (thickness/2))
                falloff = max(0, min(1, falloff))
                alpha = int(color[3] * falloff)
                if alpha > 0:
                    overlay = (color[0], color[1], color[2], alpha)
                    base = pixels[x, y]
                    pixels[x, y] = blend_additive(base, overlay)


def generate_explosion(explosion_type='ship'):
    """Generate an 8-frame explosion sprite sheet."""
    sheet = Image.new('RGBA', (SHEET_WIDTH, SHEET_HEIGHT), (0, 0, 0, 0))
    
    cx, cy = FRAME_SIZE // 2, FRAME_SIZE // 2
    
    particles = []
    
    if explosion_type == 'hit':
        particles.append(Particle(cx, cy, 0, 0, life=2, size=12, color=(255, 255, 220), particle_type='flash'))
        for i in range(20):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(1.5, 4)
            particles.append(Particle(
                cx, cy,
                math.cos(angle) * speed, math.sin(angle) * speed,
                life=random.uniform(5, 9),
                size=random.uniform(2, 6),
                color=(255, 255, 150),
                particle_type='spark'
            ))
        for i in range(12):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(0.5, 2.5)
            particles.append(Particle(
                cx, cy,
                math.cos(angle) * speed, math.sin(angle) * speed,
                life=random.uniform(5, 9),
                size=random.uniform(4, 10),
                color=(255, 200, 50),
                particle_type='fire'
            ))
    
    elif explosion_type == 'ship':
        particles.append(Particle(cx, cy, 0, 0, life=2, size=25, color=(255, 255, 255), particle_type='flash'))
        for i in range(20):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(1, 4)
            particles.append(Particle(
                cx + random.uniform(-5, 5), cy + random.uniform(-5, 5),
                math.cos(angle) * speed, math.sin(angle) * speed,
                life=random.uniform(4, 8),
                size=random.uniform(6, 15),
                color=(255, random.randint(100, 200), 0),
                particle_type='fire'
            ))
        for i in range(10):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(0.5, 2)
            particles.append(Particle(
                cx + random.uniform(-8, 8), cy + random.uniform(-8, 8),
                math.cos(angle) * speed, math.sin(angle) * speed - 0.5,
                life=random.uniform(5, 9),
                size=random.uniform(8, 18),
                color=(100, 100, 100),
                particle_type='smoke'
            ))
        for i in range(8):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(3, 7)
            particles.append(Particle(
                cx, cy,
                math.cos(angle) * speed, math.sin(angle) * speed,
                life=random.uniform(3, 6),
                size=random.uniform(2, 4),
                color=(255, 200, 100),
                particle_type='debris'
            ))
    
    elif explosion_type == 'boss':
        particles.append(Particle(cx, cy, 0, 0, life=3, size=30, color=(255, 255, 255), particle_type='flash'))
        for i in range(35):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(1, 5)
            particles.append(Particle(
                cx + random.uniform(-8, 8), cy + random.uniform(-8, 8),
                math.cos(angle) * speed, math.sin(angle) * speed,
                life=random.uniform(5, 9),
                size=random.uniform(8, 20),
                color=(255, random.randint(80, 180), 0),
                particle_type='fire'
            ))
        for i in range(15):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(0.3, 1.5)
            particles.append(Particle(
                cx + random.uniform(-10, 10), cy + random.uniform(-10, 10),
                math.cos(angle) * speed, math.sin(angle) * speed - 0.3,
                life=random.uniform(6, 10),
                size=random.uniform(10, 22),
                color=(80, 80, 80),
                particle_type='smoke'
            ))
        for i in range(15):
            angle = random.uniform(0, 2 * math.pi)
            speed = random.uniform(4, 9)
            particles.append(Particle(
                cx, cy,
                math.cos(angle) * speed, math.sin(angle) * speed,
                life=random.uniform(4, 7),
                size=random.uniform(2, 5),
                color=(255, 220, 150),
                particle_type='debris'
            ))
    
    for frame_idx in range(FRAME_COUNT):
        frame = Image.new('RGBA', (FRAME_SIZE, FRAME_SIZE), (0, 0, 0, 0))
        
        if explosion_type in ('ship', 'boss'):
            ring_progress = frame_idx / (FRAME_COUNT - 1)
            ring_radius = 5 + ring_progress * 25
            ring_alpha = int(180 * (1 - ring_progress))
            if ring_alpha > 10:
                ring_color = (255, 200, 100, ring_alpha)
                draw_ring(frame, cx, cy, ring_radius, 3 - ring_progress * 2, ring_color)
        
        smoke_particles = [p for p in particles if p.particle_type == 'smoke' and p.life > 0]
        for p in smoke_particles:
            alpha = p.alpha
            if alpha > 0:
                color = (p.color[0], p.color[1], p.color[2], alpha)
                draw_circle_soft(frame, p.x, p.y, p.size, color)
        
        fire_particles = [p for p in particles if p.particle_type in ('fire', 'flash') and p.life > 0]
        for p in fire_particles:
            alpha = p.alpha
            if alpha > 0:
                color = (p.color[0], p.color[1], p.color[2], alpha)
                draw_circle_soft(frame, p.x, p.y, p.size, color)
        
        other_particles = [p for p in particles if p.particle_type in ('spark', 'debris') and p.life > 0]
        for p in other_particles:
            alpha = p.alpha
            if alpha > 0:
                color = (p.color[0], p.color[1], p.color[2], alpha)
                size = max(1, p.size * (p.life / p.max_life))
                draw_circle_soft(frame, p.x, p.y, size, color)
        
        sheet.paste(frame, (frame_idx * FRAME_SIZE, 0))
        
        for p in particles:
            p.update(1.0)
    
    return sheet


def verify_png(filepath):
    """Verify PNG is 8-bit RGBA, has valid zlib, and corners are transparent."""
    with open(filepath, 'rb') as f:
        data = f.read()
    
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        return False, "Invalid PNG signature"
    
    pos = 8
    ihdr_found = False
    idat_data = b''
    
    while pos < len(data):
        length = struct.unpack('>I', data[pos:pos+4])[0]
        chunk_type = data[pos+4:pos+8]
        chunk_data = data[pos+8:pos+8+length]
        
        if chunk_type == b'IHDR':
            ihdr_found = True
            width = struct.unpack('>I', chunk_data[0:4])[0]
            height = struct.unpack('>I', chunk_data[4:8])[0]
            bit_depth = chunk_data[8]
            color_type = chunk_data[9]
            if bit_depth != 8:
                return False, f"Bit depth is {bit_depth}, expected 8"
            if color_type != 6:
                return False, f"Color type is {color_type}, expected 6 (RGBA)"
        
        elif chunk_type == b'IDAT':
            idat_data += chunk_data
        
        pos += 12 + length
    
    if not ihdr_found:
        return False, "No IHDR chunk found"
    
    try:
        zlib.decompress(idat_data)
    except zlib.error as e:
        return False, f"Invalid zlib data: {e}"
    
    img = Image.open(filepath)
    pixels = img.load()
    w, h = img.size
    corners = [(0, 0), (w-1, 0), (0, h-1), (w-1, h-1)]
    for cx, cy in corners:
        if pixels[cx, cy][3] != 0:
            return False, f"Corner ({cx},{cy}) alpha is {pixels[cx, cy][3]}, expected 0"
    
    for frame_idx in range(FRAME_COUNT):
        frame_x = frame_idx * FRAME_SIZE
        has_content = False
        for y in range(FRAME_SIZE):
            for x in range(FRAME_SIZE):
                if pixels[frame_x + x, y][3] > 0:
                    has_content = True
                    break
            if has_content:
                break
        if not has_content:
            return False, f"Frame {frame_idx} is empty"
    
    return True, "OK"


def main():
    import os
    
    output_dir = '/workspace/tools/explosions'
    os.makedirs(output_dir, exist_ok=True)
    
    types = [
        ('hit', 'explosion_hit.png'),
        ('ship', 'explosion_ship.png'),
        ('boss', 'explosion_big.png'),
    ]
    
    for exp_type, filename in types:
        print(f"Generating {exp_type} explosion...")
        sheet = generate_explosion(exp_type)
        
        png_path = os.path.join(output_dir, filename)
        sheet.save(png_path, 'PNG', compress_level=9)
        
        valid, msg = verify_png(png_path)
        if not valid:
            print(f"  ERROR: {msg}")
            continue
        print(f"  Verified: {msg}")
        
        with open(png_path, 'rb') as f:
            png_bytes = f.read()
        b64_str = base64.b64encode(png_bytes).decode('ascii')
        
        b64_path = png_path + '.b64'
        with open(b64_path, 'w') as f:
            f.write(b64_str)
        
        print(f"  Saved: {png_path} ({len(png_bytes)} bytes)")
        print(f"  Base64: {b64_path} ({len(b64_str)} chars)")
    
    print("\nGenerating TEXTURE_DATA snippet...")
    snippet_path = os.path.join(output_dir, 'texture_data_snippet.java')
    with open(snippet_path, 'w') as f:
        f.write("        // Explosion sprite sheets (8 frames of 64x64 each, horizontal strip)\n")
        for exp_type, filename in types:
            b64_path = os.path.join(output_dir, filename + '.b64')
            if not os.path.exists(b64_path):
                print(f"  Skipping {filename} (no base64 file)")
                continue
            with open(b64_path, 'r') as bf:
                b64_str = bf.read()
            f.write(f'        TEXTURE_DATA.put("{filename}",\n')
            f.write(f'            "{b64_str}");\n')
    
    print(f"  Snippet: {snippet_path}")
    print("\nDone!")


if __name__ == '__main__':
    main()
