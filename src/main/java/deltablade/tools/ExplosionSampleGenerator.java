package deltablade.tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Generates sample explosion sprite sheets without requiring JavaFX Application.
 * Pure Java implementation - no JavaFX runtime needed.
 * 
 * Run via: mvn exec:java -Dexec.mainClass=deltablade.tools.ExplosionSampleGenerator
 * 
 * Outputs 8-bit RGBA PNGs (color type 6, non-interlaced) compatible with
 * DeltaBlade's EmbeddedTextures.decodePng().
 */
public class ExplosionSampleGenerator {

    private static final int HIRES_SCALE = 4;
    
    private final Random random = new Random();
    private long currentSeed;
    
    private int[][] accumulatorR;
    private int[][] accumulatorG;
    private int[][] accumulatorB;
    private int[][] accumulatorA;
    
    private int[] imageBuffer;
    private int imageWidth;
    private int imageHeight;
    
    private List<Particle> particles = new ArrayList<>();
    private double shockwaveMaxRadius;
    
    private double duration;
    private int frameCount;
    private int frameSize;
    private int particleCount;
    private double sizeOverLife;
    private int[] coreColor;
    private int[] midColor;
    private int[] smokeColor;
    private double gravity;
    private double velocity;
    private double drag;
    private double flashIntensity;
    private int debrisCount;
    private boolean fireEnabled;
    private boolean sparksEnabled;
    private boolean smokeEnabled;
    private boolean shockwaveEnabled;
    private boolean debrisEnabled;
    private boolean flashEnabled;

    public static void main(String[] args) {
        ExplosionSampleGenerator gen = new ExplosionSampleGenerator();
        
        try {
            Path explosionsDir = Path.of("src/main/resources/assets/textures/explosions");
            Files.createDirectories(explosionsDir);
            
            System.out.println("Generating sample explosions...");
            
            gen.generatePreset("treffer", explosionsDir);
            gen.generatePreset("schiff", explosionsDir);
            gen.generatePreset("boss", explosionsDir);
            
            System.out.println("Sample explosions exported to: " + explosionsDir.toAbsolutePath());
            System.out.println("Done!");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("Failed to generate samples: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private int[] rgb(int r, int g, int b) {
        return new int[] {r, g, b};
    }
    
    private void generatePreset(String presetId, Path outDir) throws IOException {
        applyPreset(presetId);
        
        int cols = Math.min(8, frameCount);
        int rows = (frameCount + cols - 1) / cols;
        double fps = frameCount / duration;
        
        int sheetW = cols * frameSize;
        int sheetH = rows * frameSize;
        int[] sheet = new int[sheetW * sheetH];
        
        for (int i = 0; i < frameCount; i++) {
            double frameTime = (i / (double) frameCount) * duration;
            
            imageWidth = frameSize;
            imageHeight = frameSize;
            imageBuffer = new int[frameSize * frameSize];
            
            renderFrame(frameTime);
            
            int col = i % cols;
            int row = i / cols;
            int destX = col * frameSize;
            int destY = row * frameSize;
            
            for (int y = 0; y < frameSize; y++) {
                for (int x = 0; x < frameSize; x++) {
                    sheet[(destY + y) * sheetW + (destX + x)] = imageBuffer[y * frameSize + x];
                }
            }
        }
        
        String filename = "explosion_" + presetId + ".png";
        byte[] pngBytes = encodePng8BitRgba(sheet, sheetW, sheetH);
        Files.write(outDir.resolve(filename), pngBytes);
        
        String jsonFilename = "explosion_" + presetId + ".json";
        String json = String.format(
            "{\n  \"name\": \"%s\",\n  \"frameWidth\": %d,\n  \"frameHeight\": %d,\n  \"frameCount\": %d,\n  \"fps\": %.2f,\n  \"columns\": %d,\n  \"rows\": %d\n}",
            presetId, frameSize, frameSize, frameCount, fps, cols, rows
        );
        Files.writeString(outDir.resolve(jsonFilename), json);
        
        System.out.println("  Generated: " + filename + " (" + sheetW + "x" + sheetH + ", " + frameCount + " frames)");
    }
    
    private void applyPreset(String presetId) {
        currentSeed = presetId.hashCode() * 12345L;
        
        switch (presetId) {
            case "treffer" -> {
                duration = 0.2;
                frameCount = 6;
                frameSize = 32;
                particleCount = 40;
                sizeOverLife = 0.4;
                coreColor = rgb(255, 255, 255);
                midColor = rgb(255, 255, 0);
                smokeColor = rgb(70, 65, 60);
                gravity = 0;
                velocity = 120;
                drag = 4.0;
                flashIntensity = 1.2;
                debrisCount = 3;
                fireEnabled = false;
                sparksEnabled = true;
                smokeEnabled = false;
                shockwaveEnabled = false;
                debrisEnabled = true;
                flashEnabled = true;
            }
            case "schiff" -> {
                duration = 0.5;
                frameCount = 8;
                frameSize = 64;
                particleCount = 100;
                sizeOverLife = 1.0;
                coreColor = rgb(255, 255, 255);
                midColor = rgb(255, 165, 0);
                smokeColor = rgb(50, 45, 40);
                gravity = 60;
                velocity = 180;
                drag = 2.2;
                flashIntensity = 0.9;
                debrisCount = 10;
                fireEnabled = true;
                sparksEnabled = true;
                smokeEnabled = true;
                shockwaveEnabled = false;
                debrisEnabled = true;
                flashEnabled = true;
            }
            case "boss" -> {
                duration = 1.0;
                frameCount = 14;
                frameSize = 96;
                particleCount = 280;
                sizeOverLife = 1.6;
                coreColor = rgb(255, 255, 255);
                midColor = rgb(255, 69, 0);
                smokeColor = rgb(35, 30, 25);
                gravity = 50;
                velocity = 260;
                drag = 1.5;
                flashIntensity = 1.3;
                debrisCount = 25;
                fireEnabled = true;
                sparksEnabled = true;
                smokeEnabled = true;
                shockwaveEnabled = true;
                debrisEnabled = true;
                flashEnabled = true;
            }
            default -> throw new IllegalArgumentException("Unknown preset: " + presetId);
        }
        
        initializeParticles();
    }
    
    private void initializeParticles() {
        particles.clear();
        random.setSeed(currentSeed);
        
        int hiresSize = frameSize * HIRES_SCALE;
        double centerX = hiresSize / 2.0;
        double centerY = hiresSize / 2.0;
        double vel = velocity * HIRES_SCALE;
        
        if (smokeEnabled) {
            int smokeCount = particleCount / 3;
            for (int i = 0; i < smokeCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = vel * (0.15 + random.nextDouble() * 0.35);
                double size = (5 + random.nextDouble() * 8) * HIRES_SCALE;
                double delay = random.nextDouble() * 0.15;
                particles.add(new Particle(
                    centerX + (random.nextDouble() - 0.5) * 6 * HIRES_SCALE,
                    centerY + (random.nextDouble() - 0.5) * 6 * HIRES_SCALE,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.SMOKE,
                    0.6 + random.nextDouble() * 0.4, delay
                ));
            }
        }
        
        if (fireEnabled) {
            int fireCount = particleCount / 3;
            for (int i = 0; i < fireCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = vel * (0.25 + random.nextDouble() * 0.6);
                double size = (3 + random.nextDouble() * 5) * HIRES_SCALE;
                particles.add(new Particle(
                    centerX + (random.nextDouble() - 0.5) * 3 * HIRES_SCALE,
                    centerY + (random.nextDouble() - 0.5) * 3 * HIRES_SCALE,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.FIRE, 0.7 + random.nextDouble() * 0.3, 0
                ));
            }
        }
        
        if (sparksEnabled) {
            int sparkCount = particleCount / 4;
            for (int i = 0; i < sparkCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = vel * (0.7 + random.nextDouble() * 0.9);
                double size = (1 + random.nextDouble() * 2) * HIRES_SCALE;
                particles.add(new Particle(
                    centerX, centerY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.SPARK, 0.5 + random.nextDouble() * 0.5, 0
                ));
            }
        }
        
        int coreCount = particleCount / 8 + 5;
        for (int i = 0; i < coreCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = vel * (0.05 + random.nextDouble() * 0.2);
            double size = (2 + random.nextDouble() * 4) * HIRES_SCALE;
            particles.add(new Particle(
                centerX + (random.nextDouble() - 0.5) * 2 * HIRES_SCALE,
                centerY + (random.nextDouble() - 0.5) * 2 * HIRES_SCALE,
                Math.cos(angle) * speed, Math.sin(angle) * speed,
                size, ParticleType.CORE, 0.25 + random.nextDouble() * 0.25, 0
            ));
        }
        
        if (debrisEnabled) {
            for (int i = 0; i < debrisCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = vel * (0.4 + random.nextDouble() * 0.8);
                double size = (1.5 + random.nextDouble() * 2.5) * HIRES_SCALE;
                particles.add(new Particle(
                    centerX, centerY,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    size, ParticleType.DEBRIS, 0.8 + random.nextDouble() * 0.2, 0
                ));
            }
        }
        
        shockwaveMaxRadius = frameSize * HIRES_SCALE * 0.42;
    }
    
    private void renderFrame(double time) {
        int hiresSize = frameSize * HIRES_SCALE;
        
        if (accumulatorR == null || accumulatorR.length != hiresSize) {
            accumulatorR = new int[hiresSize][hiresSize];
            accumulatorG = new int[hiresSize][hiresSize];
            accumulatorB = new int[hiresSize][hiresSize];
            accumulatorA = new int[hiresSize][hiresSize];
        }
        
        for (int y = 0; y < hiresSize; y++) {
            for (int x = 0; x < hiresSize; x++) {
                accumulatorR[y][x] = 0;
                accumulatorG[y][x] = 0;
                accumulatorB[y][x] = 0;
                accumulatorA[y][x] = 0;
            }
        }
        
        double progress = Math.min(1.0, time / duration);
        double grav = gravity * HIRES_SCALE;
        double centerX = hiresSize / 2.0;
        double centerY = hiresSize / 2.0;
        
        simulateParticles(time, grav);
        
        if (flashEnabled && progress < 0.15) {
            double flashProgress = progress / 0.15;
            double currentFlash = flashIntensity * (1.0 - flashProgress * flashProgress);
            if (currentFlash > 0.05) {
                double flashRadius = hiresSize * 0.3 * (0.5 + flashProgress * 0.5);
                
                for (int y = 0; y < hiresSize; y++) {
                    for (int x = 0; x < hiresSize; x++) {
                        double dx = x - centerX;
                        double dy = y - centerY;
                        double dist = Math.sqrt(dx*dx + dy*dy);
                        if (dist < flashRadius) {
                            double falloff = 1.0 - (dist / flashRadius);
                            falloff = falloff * falloff;
                            int alpha = (int)(currentFlash * falloff * 255);
                            accumulatorR[y][x] = Math.min(255 * 4, accumulatorR[y][x] + coreColor[0] * alpha / 255);
                            accumulatorG[y][x] = Math.min(255 * 4, accumulatorG[y][x] + coreColor[1] * alpha / 255);
                            accumulatorB[y][x] = Math.min(255 * 4, accumulatorB[y][x] + coreColor[2] * alpha / 255);
                            accumulatorA[y][x] = Math.min(255 * 4, accumulatorA[y][x] + alpha);
                        }
                    }
                }
            }
        }
        
        if (shockwaveEnabled && progress > 0.02 && progress < 0.7) {
            double shockProgress = (progress - 0.02) / 0.68;
            double radius = shockwaveMaxRadius * shockProgress;
            double ringAlpha = Math.max(0, 0.7 * (1.0 - shockProgress));
            double thickness = 3.0 * HIRES_SCALE * (1.0 - shockProgress * 0.5);
            
            for (int y = 0; y < hiresSize; y++) {
                for (int x = 0; x < hiresSize; x++) {
                    double dx = x - centerX;
                    double dy = y - centerY;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    double ringDist = Math.abs(dist - radius);
                    if (ringDist < thickness) {
                        double falloff = 1.0 - (ringDist / thickness);
                        int alpha = (int)(ringAlpha * falloff * 255);
                        accumulatorR[y][x] = Math.min(255 * 4, accumulatorR[y][x] + midColor[0] * alpha / 255);
                        accumulatorG[y][x] = Math.min(255 * 4, accumulatorG[y][x] + midColor[1] * alpha / 255);
                        accumulatorB[y][x] = Math.min(255 * 4, accumulatorB[y][x] + midColor[2] * alpha / 255);
                        accumulatorA[y][x] = Math.min(255 * 4, accumulatorA[y][x] + alpha);
                    }
                }
            }
        }
        
        List<Particle> sortedParticles = new ArrayList<>(particles);
        sortedParticles.sort((a, b) -> Integer.compare(getLayerOrder(a.type), getLayerOrder(b.type)));
        
        for (Particle p : sortedParticles) {
            if (p.life <= 0 || progress < p.delay) continue;
            
            double adjustedProgress = (progress - p.delay) / (1.0 - p.delay);
            double life = Math.max(0, 1.0 - (adjustedProgress / p.lifeFactor));
            
            double size = p.baseSize * (0.4 + 0.6 * life) * sizeOverLife;
            double alpha = life;
            
            int[] color;
            boolean additive = false;
            
            switch (p.type) {
                case CORE -> {
                    color = coreColor;
                    alpha *= 0.95;
                    additive = true;
                }
                case FIRE -> {
                    double blend = life * life;
                    color = interpolateColor(smokeColor, interpolateColor(midColor, coreColor, blend), life);
                    alpha *= 0.85;
                    additive = true;
                }
                case SPARK -> {
                    color = interpolateColor(midColor, coreColor, life);
                    alpha *= life;
                    additive = true;
                    size = Math.max(HIRES_SCALE, size * 0.6);
                }
                case SMOKE -> {
                    color = smokeColor;
                    alpha *= 0.5 * life;
                    size *= 1.8 - life * 0.6;
                    additive = false;
                }
                case DEBRIS -> {
                    color = interpolateColor(rgb(77, 77, 77), midColor, life * 0.5);
                    alpha *= 0.9;
                    additive = false;
                }
                default -> {
                    color = rgb(255, 255, 255);
                    additive = true;
                }
            }
            
            drawParticle(hiresSize, p.x, p.y, size, color, alpha, p.type == ParticleType.SPARK, additive);
        }
        
        for (int y = 0; y < frameSize; y++) {
            for (int x = 0; x < frameSize; x++) {
                int totalR = 0, totalG = 0, totalB = 0, totalA = 0;
                
                for (int sy = 0; sy < HIRES_SCALE; sy++) {
                    for (int sx = 0; sx < HIRES_SCALE; sx++) {
                        int hx = x * HIRES_SCALE + sx;
                        int hy = y * HIRES_SCALE + sy;
                        totalR += accumulatorR[hy][hx];
                        totalG += accumulatorG[hy][hx];
                        totalB += accumulatorB[hy][hx];
                        totalA += accumulatorA[hy][hx];
                    }
                }
                
                int samples = HIRES_SCALE * HIRES_SCALE;
                int r = Math.min(255, totalR / samples);
                int g = Math.min(255, totalG / samples);
                int b = Math.min(255, totalB / samples);
                int a = Math.min(255, totalA / samples);
                
                imageBuffer[y * frameSize + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }
    
    private int getLayerOrder(ParticleType type) {
        return switch (type) {
            case SMOKE -> 0;
            case DEBRIS -> 1;
            case FIRE -> 2;
            case SPARK -> 3;
            case CORE -> 4;
        };
    }
    
    private void simulateParticles(double targetTime, double grav) {
        for (Particle p : particles) {
            if (targetTime < p.delay) continue;
            
            double simTime = targetTime - p.delay;
            double simDuration = duration - p.delay;
            
            double px = p.startX;
            double py = p.startY;
            double vx = p.startVx;
            double vy = p.startVy;
            
            double dt = 1.0 / 120.0;
            double t = 0;
            
            while (t < simTime) {
                double step = Math.min(dt, simTime - t);
                vy += grav * step;
                vx *= Math.pow(1.0 - drag * 0.1, step * 60);
                vy *= Math.pow(1.0 - drag * 0.1, step * 60);
                px += vx * step;
                py += vy * step;
                t += step;
            }
            
            p.x = px;
            p.y = py;
            
            double progress = simTime / Math.max(0.001, simDuration);
            p.life = Math.max(0, 1.0 - (progress / p.lifeFactor));
        }
    }
    
    private void drawParticle(int size, double cx, double cy, double psize, int[] color, double alpha, boolean isSpark, boolean additive) {
        int icx = (int) cx;
        int icy = (int) cy;
        int radius = (int) Math.ceil(psize / 2);
        
        int r = color[0];
        int g = color[1];
        int b = color[2];
        
        if (isSpark) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int px = icx + dx;
                    int py = icy + dy;
                    if (px < 0 || px >= size || py < 0 || py >= size) continue;
                    double falloff = (dx == 0 && dy == 0) ? 1.0 : 0.3;
                    int a = (int)(alpha * falloff * 255);
                    if (additive) {
                        accumulatorR[py][px] = Math.min(255 * 4, accumulatorR[py][px] + r * a / 255);
                        accumulatorG[py][px] = Math.min(255 * 4, accumulatorG[py][px] + g * a / 255);
                        accumulatorB[py][px] = Math.min(255 * 4, accumulatorB[py][px] + b * a / 255);
                    } else {
                        accumulatorR[py][px] = r;
                        accumulatorG[py][px] = g;
                        accumulatorB[py][px] = b;
                    }
                    accumulatorA[py][px] = Math.min(255 * 4, accumulatorA[py][px] + a);
                }
            }
            return;
        }
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = icx + dx;
                int py = icy + dy;
                if (px < 0 || px >= size || py < 0 || py >= size) continue;
                
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist <= psize / 2) {
                    double falloff = 1.0 - (dist / (psize / 2));
                    falloff = falloff * falloff;
                    int a = (int)(alpha * falloff * 255);
                    
                    if (additive) {
                        accumulatorR[py][px] = Math.min(255 * 4, accumulatorR[py][px] + r * a / 255);
                        accumulatorG[py][px] = Math.min(255 * 4, accumulatorG[py][px] + g * a / 255);
                        accumulatorB[py][px] = Math.min(255 * 4, accumulatorB[py][px] + b * a / 255);
                    } else {
                        int existingA = accumulatorA[py][px];
                        if (existingA < a) {
                            accumulatorR[py][px] = r;
                            accumulatorG[py][px] = g;
                            accumulatorB[py][px] = b;
                        }
                    }
                    accumulatorA[py][px] = Math.min(255 * 4, accumulatorA[py][px] + a);
                }
            }
        }
    }
    
    private int[] interpolateColor(int[] c1, int[] c2, double t) {
        t = Math.max(0, Math.min(1, t));
        return new int[] {
            (int)(c1[0] + (c2[0] - c1[0]) * t),
            (int)(c1[1] + (c2[1] - c1[1]) * t),
            (int)(c1[2] + (c2[2] - c1[2]) * t)
        };
    }
    
    private byte[] encodePng8BitRgba(int[] pixels, int w, int h) throws IOException {
        byte[] rawData = new byte[h * (1 + w * 4)];
        int idx = 0;
        
        for (int y = 0; y < h; y++) {
            rawData[idx++] = 0;
            for (int x = 0; x < w; x++) {
                int argb = pixels[y * w + x];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                rawData[idx++] = (byte) r;
                rawData[idx++] = (byte) g;
                rawData[idx++] = (byte) b;
                rawData[idx++] = (byte) a;
            }
        }
        
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(rawData);
        deflater.finish();
        
        byte[] compressedBuffer = new byte[rawData.length + 1024];
        int compressedLen = deflater.deflate(compressedBuffer);
        deflater.end();
        
        byte[] compressed = new byte[compressedLen];
        System.arraycopy(compressedBuffer, 0, compressed, 0, compressedLen);
        
        verifyInflate(compressed, w, h);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        out.write(new byte[] {(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        writeInt(ihdr, w);
        writeInt(ihdr, h);
        ihdr.write(8);
        ihdr.write(6);
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        writeChunk(out, "IHDR", ihdr.toByteArray());
        
        writeChunk(out, "IDAT", compressed);
        
        writeChunk(out, "IEND", new byte[0]);
        
        return out.toByteArray();
    }
    
    private void verifyInflate(byte[] compressed, int w, int h) throws IOException {
        try {
            java.util.zip.Inflater inflater = new java.util.zip.Inflater();
            inflater.setInput(compressed);
            byte[] output = new byte[h * (1 + w * 4) + 1024];
            int total = 0;
            while (!inflater.finished()) {
                int count = inflater.inflate(output, total, output.length - total);
                if (count == 0 && inflater.needsInput()) break;
                total += count;
            }
            inflater.end();
            
            int expected = h * (1 + w * 4);
            if (total < expected) {
                throw new IOException("Inflate verification failed: got " + total + " bytes, expected " + expected);
            }
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException("Inflate verification failed: " + e.getMessage());
        }
    }
    
    private void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
        writeInt(out, data.length);
        byte[] typeBytes = type.getBytes("ASCII");
        out.write(typeBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(out, (int) crc.getValue());
    }
    
    private void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >> 24) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }
    
    private enum ParticleType {
        CORE, FIRE, SPARK, SMOKE, DEBRIS
    }
    
    private static class Particle {
        double x, y;
        double startX, startY;
        double startVx, startVy;
        double baseSize;
        double life;
        double lifeFactor;
        double delay;
        ParticleType type;
        
        Particle(double x, double y, double vx, double vy, double size, ParticleType type, double lifeFactor, double delay) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.startY = y;
            this.startVx = vx;
            this.startVy = vy;
            this.baseSize = size;
            this.type = type;
            this.lifeFactor = lifeFactor;
            this.life = 1.0;
            this.delay = delay;
        }
    }
}
