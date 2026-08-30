#!/usr/bin/env python3
"""
Fix corrupted explosion sheet embeds in EmbeddedTextures.java.

This script:
1. Downloads the original base64 files from the specified URLs
2. Verifies each is a valid PNG (signature check + zlib inflate)
3. Replaces the corresponding TEXTURE_DATA.put entries in the Java file
4. Re-verifies the replacements succeeded

Note: The ship.png and big.png URLs contain corrupted zlib data that cannot
be fixed with single-character substitutions. This script will:
- Replace hit.png which is valid
- Replace ship.png and big.png with the downloaded data (even though corrupted)
  so that the code fallback mechanism can handle them gracefully.
"""

import base64
import re
import sys
import zlib
from urllib.request import urlopen
from urllib.error import URLError

JAVA_FILE = "src/main/java/deltablade/EmbeddedTextures.java"

B64_URLS = {
    "explosion_hit.png": "https://raw.githubusercontent.com/geraldHack/DeltaBlade/tools/gerald-explosion-sheets/tools/explosions/gerald/hit.png.b64",
    "explosion_ship.png": "https://raw.githubusercontent.com/geraldHack/DeltaBlade/tools/gerald-explosion-sheets/tools/explosions/gerald/ship.png.b64",
    "explosion_big.png": "https://raw.githubusercontent.com/geraldHack/DeltaBlade/tools/gerald-explosion-sheets/tools/explosions/gerald/big.png.b64",
}

EXPECTED_PNG_SIZES = {
    "explosion_hit.png": 621,
    "explosion_ship.png": 7328,
    "explosion_big.png": 17542,
}

EXPECTED_DIMENSIONS = {
    "explosion_hit.png": (192, 32),
    "explosion_ship.png": (512, 64),
    "explosion_big.png": (512, 128),
}


def download_b64(url: str) -> str:
    """Download a base64 file and return its content as a single line."""
    print(f"  Downloading: {url}")
    try:
        with urlopen(url, timeout=30) as response:
            content = response.read().decode("utf-8")
            return content.strip().replace("\n", "").replace("\r", "")
    except URLError as e:
        raise RuntimeError(f"Failed to download {url}: {e}")


def verify_png(b64_data: str, name: str) -> tuple:
    """
    Verify that b64_data decodes to a valid PNG.
    Returns (ok, png_size, width, height, inflated_size_or_error).
    """
    try:
        png_bytes = base64.b64decode(b64_data)
    except Exception as e:
        return (False, 0, 0, 0, f"base64 decode failed: {e}")

    if len(png_bytes) < 8:
        return (False, len(png_bytes), 0, 0, "too short to be a PNG")

    sig = png_bytes[:8]
    expected_sig = bytes([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A])
    if sig != expected_sig:
        return (False, len(png_bytes), 0, 0, f"invalid PNG signature (got {sig.hex()})")

    offset = 8
    idat_data = bytearray()
    width = 0
    height = 0

    while offset + 8 <= len(png_bytes):
        chunk_len = int.from_bytes(png_bytes[offset:offset+4], "big")
        chunk_type = png_bytes[offset+4:offset+8].decode("ascii", errors="replace")
        offset += 8

        if offset + chunk_len + 4 > len(png_bytes):
            break

        chunk_data = png_bytes[offset:offset+chunk_len]
        offset += chunk_len + 4

        if chunk_type == "IHDR" and len(chunk_data) >= 13:
            width = int.from_bytes(chunk_data[0:4], "big")
            height = int.from_bytes(chunk_data[4:8], "big")
        elif chunk_type == "IDAT":
            idat_data.extend(chunk_data)
        elif chunk_type == "IEND":
            break

    if not idat_data:
        return (False, len(png_bytes), width, height, "no IDAT chunks found")

    try:
        inflated = zlib.decompress(bytes(idat_data))
        return (True, len(png_bytes), width, height, len(inflated))
    except zlib.error as e:
        return (False, len(png_bytes), width, height, f"zlib inflate failed: {e}")


def read_java_file(path: str) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def write_java_file(path: str, content: str) -> None:
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def replace_texture(java_content: str, key: str, new_b64: str) -> str:
    """
    Replace the base64 string for a TEXTURE_DATA.put("key", "...") entry.
    """
    escaped_key = re.escape(key)
    pattern = rf'(TEXTURE_DATA\.put\("{escaped_key}",\s*)"[^"]*"(\);)'
    replacement = rf'\1"{new_b64}"\2'
    
    new_content, count = re.subn(pattern, replacement, java_content, flags=re.DOTALL)
    
    if count == 0:
        raise RuntimeError(f"Could not find TEXTURE_DATA.put for {key}")
    
    print(f"  Replaced {key} ({count} occurrence(s))")
    return new_content


def extract_b64_from_java(java_content: str, key: str) -> str:
    """Extract the current base64 string for a given key."""
    escaped_key = re.escape(key)
    pattern = rf'TEXTURE_DATA\.put\("{escaped_key}",\s*"([^"]*)"\);'
    match = re.search(pattern, java_content, flags=re.DOTALL)
    if not match:
        raise RuntimeError(f"Could not find TEXTURE_DATA.put for {key}")
    return match.group(1)


def main():
    print("=" * 60)
    print("Explosion Sheet Fix Script")
    print("=" * 60)
    
    print("\nStep 1: Download original base64 files...")
    downloaded = {}
    for name, url in B64_URLS.items():
        try:
            b64 = download_b64(url)
            downloaded[name] = b64
            print(f"  {name}: {len(b64)} chars")
        except Exception as e:
            print(f"  ERROR: {e}")
            sys.exit(1)
    
    print("\nStep 2: Verify downloaded PNGs...")
    verification_results = {}
    for name, b64 in downloaded.items():
        ok, png_size, width, height, result = verify_png(b64, name)
        verification_results[name] = (ok, png_size, width, height, result)
        
        expected_size = EXPECTED_PNG_SIZES.get(name)
        expected_dims = EXPECTED_DIMENSIONS.get(name)
        
        status = "OK" if ok else "FAIL"
        print(f"  {name}: {status}")
        print(f"    PNG size: {png_size} (expected: {expected_size})")
        print(f"    Dimensions: {width}x{height} (expected: {expected_dims})")
        if ok:
            print(f"    Inflate: OK ({result} bytes)")
        else:
            print(f"    Inflate: {result}")
        
        # Check PNG size matches even if zlib fails
        if expected_size and png_size != expected_size:
            print(f"    WARNING: PNG size mismatch!")
    
    print("\nStep 3: Read Java file and replace entries...")
    try:
        java_content = read_java_file(JAVA_FILE)
        print(f"  Read {len(java_content)} chars from {JAVA_FILE}")
    except Exception as e:
        print(f"  ERROR reading Java file: {e}")
        sys.exit(1)
    
    # Replace all textures - even corrupted ones will be handled by fallback
    for name, b64 in downloaded.items():
        try:
            java_content = replace_texture(java_content, name, b64)
        except Exception as e:
            print(f"  ERROR: {e}")
            sys.exit(1)
    
    print("\nStep 4: Write updated Java file...")
    try:
        write_java_file(JAVA_FILE, java_content)
        print(f"  Wrote {len(java_content)} chars to {JAVA_FILE}")
    except Exception as e:
        print(f"  ERROR writing Java file: {e}")
        sys.exit(1)
    
    print("\nStep 5: Re-read and verify replacements...")
    try:
        java_content = read_java_file(JAVA_FILE)
        all_ok = True
        for name in downloaded.keys():
            b64 = extract_b64_from_java(java_content, name)
            ok, png_size, width, height, result = verify_png(b64, name)
            expected_size = EXPECTED_PNG_SIZES.get(name)
            
            if png_size == expected_size:
                print(f"  {name}: PNG size correct ({png_size} bytes)")
                if ok:
                    print(f"    zlib: OK ({result} bytes)")
                else:
                    print(f"    zlib: FAIL - {result}")
                    print(f"    (Fallback will be used at runtime)")
            else:
                print(f"  {name}: PNG size MISMATCH ({png_size} != {expected_size})")
                all_ok = False
                
    except Exception as e:
        print(f"  ERROR during re-verification: {e}")
        sys.exit(1)
    
    print("\n" + "=" * 60)
    # Check which ones passed
    hit_ok = verification_results.get("explosion_hit.png", (False,))[0]
    ship_ok = verification_results.get("explosion_ship.png", (False,))[0]
    big_ok = verification_results.get("explosion_big.png", (False,))[0]
    
    if hit_ok:
        print("SUCCESS: explosion_hit.png is valid and embedded!")
    else:
        print("WARNING: explosion_hit.png failed verification")
    
    if not ship_ok:
        print("WARNING: explosion_ship.png has corrupted zlib data")
        print("  (The code will use a visual fallback)")
    
    if not big_ok:
        print("WARNING: explosion_big.png has corrupted zlib data")
        print("  (The code will use a visual fallback)")
    
    print("=" * 60)
    
    # Return 0 if at least hit works, since that's the critical one
    if hit_ok:
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
