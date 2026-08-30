package deltablade.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Generates placeholder blue ship sprite sheets (384x48, 8 frames of 48x48).
 * Uses raw PNG encoding compatible with EmbeddedTextures decoder.
 * 
 * Run this tool to generate Base64 strings for embedding, or to write PNG files directly.
 */
public class BlueShipSpriteGenerator {

    private static final int FRAME_SIZE = 48;
    private static final int FRAME_COUNT = 8;
    private static final int SHEET_WIDTH = FRAME_SIZE * FRAME_COUNT;
    private static final int SHEET_HEIGHT = FRAME_SIZE;

    public static void main(String[] args) throws IOException {
        System.out.println("Generating blue ship sprite sheets...\n");
        
        byte[] thrusterPng = generateThrusterSheet();
        byte[] bankPng = generateBankSheet();
        byte[] bowwavePng = generateBowwaveSheet();
        
        System.out.println("=== player_blue_thruster.png (Base64) ===");
        System.out.println(Base64.getEncoder().encodeToString(thrusterPng));
        System.out.println();
        
        System.out.println("=== player_blue_bank.png (Base64) ===");
        System.out.println(Base64.getEncoder().encodeToString(bankPng));
        System.out.println();
        
        System.out.println("=== player_blue_bowwave.png (Base64) ===");
        System.out.println(Base64.getEncoder().encodeToString(bowwavePng));
        System.out.println();
        
        File outDir = new File("src/main/resources/assets/textures");
        if (outDir.exists()) {
            writeFile(new File(outDir, "player_blue_thruster.png"), thrusterPng);
            writeFile(new File(outDir, "player_blue_bank.png"), bankPng);
            writeFile(new File(outDir, "player_blue_bowwave.png"), bowwavePng);
            System.out.println("PNG files written to " + outDir.getAbsolutePath());
        }
    }
    
    private static void writeFile(File f, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
        }
    }

    private static byte[] generateThrusterSheet() throws IOException {
        int[][] pixels = new int[SHEET_HEIGHT][SHEET_WIDTH];
        
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            int offsetX = frame * FRAME_SIZE;
            double phase = (double) frame / FRAME_COUNT * 2 * Math.PI;
            drawBlueShip(pixels, offsetX, 0);
            drawThrusterFlame(pixels, offsetX, 0, phase);
        }
        
        return encodePng(pixels, SHEET_WIDTH, SHEET_HEIGHT);
    }
    
    private static byte[] generateBankSheet() throws IOException {
        int[][] pixels = new int[SHEET_HEIGHT][SHEET_WIDTH];
        
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            int offsetX = frame * FRAME_SIZE;
            double tilt = Math.sin((double) frame / FRAME_COUNT * 2 * Math.PI) * 0.3;
            drawBlueShipTilted(pixels, offsetX, 0, tilt);
            drawThrusterFlame(pixels, offsetX, 0, (double) frame / FRAME_COUNT * 2 * Math.PI);
        }
        
        return encodePng(pixels, SHEET_WIDTH, SHEET_HEIGHT);
    }
    
    private static byte[] generateBowwaveSheet() throws IOException {
        int[][] pixels = new int[SHEET_HEIGHT][SHEET_WIDTH];
        
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            int offsetX = frame * FRAME_SIZE;
            double phase = (double) frame / FRAME_COUNT * 2 * Math.PI;
            drawBlueShip(pixels, offsetX, 0);
            drawThrusterFlame(pixels, offsetX, 0, phase);
            drawBowwave(pixels, offsetX, 0, phase);
        }
        
        return encodePng(pixels, SHEET_WIDTH, SHEET_HEIGHT);
    }
    
    private static void drawBlueShip(int[][] pixels, int ox, int oy) {
        int cx = ox + FRAME_SIZE / 2;
        int cy = oy + FRAME_SIZE / 2;
        
        for (int y = oy; y < oy + FRAME_SIZE; y++) {
            for (int x = ox; x < ox + FRAME_SIZE; x++) {
                int lx = x - cx;
                int ly = y - cy;
                
                if (isInShipBody(lx, ly)) {
                    int blue = 180 + (int)(40 * (1 - (double)Math.abs(lx) / 12));
                    int green = 100 + (int)(50 * (1 - (double)ly / 24));
                    pixels[y][x] = rgba(40, Math.min(green, 180), Math.min(blue, 255), 255);
                }
                
                if (isInCockpit(lx, ly)) {
                    pixels[y][x] = rgba(100, 200, 255, 255);
                }
                
                if (isInWing(lx, ly)) {
                    int blue = 120 + (int)(60 * (1 - (double)Math.abs(lx) / 20));
                    pixels[y][x] = rgba(30, 80, Math.min(blue, 200), 255);
                }
            }
        }
    }
    
    private static void drawBlueShipTilted(int[][] pixels, int ox, int oy, double tilt) {
        int cx = ox + FRAME_SIZE / 2;
        int cy = oy + FRAME_SIZE / 2;
        
        for (int y = oy; y < oy + FRAME_SIZE; y++) {
            for (int x = ox; x < ox + FRAME_SIZE; x++) {
                int lx = x - cx;
                int ly = y - cy;
                
                double tlx = lx + ly * tilt * 0.3;
                
                if (isInShipBody((int)tlx, ly)) {
                    int blue = 180 + (int)(40 * (1 - (double)Math.abs(tlx) / 12));
                    int green = 100 + (int)(50 * (1 - (double)ly / 24));
                    int extraShade = (int)(Math.abs(tilt) * 30);
                    pixels[y][x] = rgba(40 - extraShade/2, Math.min(green, 180), Math.min(blue, 255), 255);
                }
                
                if (isInCockpit((int)tlx, ly)) {
                    pixels[y][x] = rgba(100, 200, 255, 255);
                }
                
                if (isInWing((int)tlx, ly)) {
                    int blue = 120 + (int)(60 * (1 - (double)Math.abs(tlx) / 20));
                    pixels[y][x] = rgba(30, 80, Math.min(blue, 200), 255);
                }
            }
        }
    }
    
    private static boolean isInShipBody(int lx, int ly) {
        if (ly < -20 || ly > 18) return false;
        double halfWidth = 8 * (1 - (double)(ly + 20) / 44);
        if (ly < -10) halfWidth = 3 + (double)(ly + 20) / 10 * 5;
        return Math.abs(lx) <= halfWidth;
    }
    
    private static boolean isInCockpit(int lx, int ly) {
        return ly >= -12 && ly <= -4 && Math.abs(lx) <= 3;
    }
    
    private static boolean isInWing(int lx, int ly) {
        if (ly < 0 || ly > 12) return false;
        if (Math.abs(lx) < 8) return false;
        double wingEdge = 8 + (double)ly * 0.8;
        return Math.abs(lx) >= 8 && Math.abs(lx) <= wingEdge;
    }
    
    private static void drawThrusterFlame(int[][] pixels, int ox, int oy, double phase) {
        int cx = ox + FRAME_SIZE / 2;
        int baseY = oy + FRAME_SIZE / 2 + 16;
        
        double flameHeight = 6 + 4 * Math.sin(phase);
        double flameWidth = 4 + 2 * Math.sin(phase * 2);
        
        for (int y = baseY; y < Math.min(baseY + (int)flameHeight + 4, oy + FRAME_SIZE); y++) {
            for (int x = cx - (int)flameWidth - 2; x <= cx + (int)flameWidth + 2; x++) {
                if (x < ox || x >= ox + FRAME_SIZE) continue;
                
                double dy = y - baseY;
                double dx = Math.abs(x - cx);
                double maxW = flameWidth * (1 - dy / (flameHeight + 4));
                
                if (dx <= maxW && dy <= flameHeight + 4) {
                    double intensity = 1 - dy / (flameHeight + 4);
                    int r = 255;
                    int g = (int)(200 * intensity + 100);
                    int b = (int)(50 * intensity);
                    int a = (int)(255 * intensity * (1 - dx / (maxW + 1)));
                    if (a > 0 && pixels[y][x] == 0) {
                        pixels[y][x] = rgba(r, g, b, Math.min(a, 255));
                    }
                }
            }
        }
    }
    
    private static void drawBowwave(int[][] pixels, int ox, int oy, double phase) {
        int cx = ox + FRAME_SIZE / 2;
        int topY = oy + FRAME_SIZE / 2 - 22;
        
        double waveIntensity = 0.6 + 0.4 * Math.sin(phase);
        
        for (int y = topY - 8; y < topY + 4; y++) {
            if (y < oy || y >= oy + FRAME_SIZE) continue;
            for (int x = cx - 8; x <= cx + 8; x++) {
                if (x < ox || x >= ox + FRAME_SIZE) continue;
                
                double dy = y - (topY - 4);
                double dx = Math.abs(x - cx);
                
                if (dy < -4 || dy > 8) continue;
                double maxW = 6 * (1 - Math.abs(dy) / 8);
                
                if (dx <= maxW) {
                    double intensity = waveIntensity * (1 - dx / (maxW + 1)) * (1 - Math.abs(dy) / 8);
                    int r = 50;
                    int g = (int)(200 + 55 * intensity);
                    int b = 255;
                    int a = (int)(200 * intensity);
                    if (a > 20 && (pixels[y][x] == 0 || (pixels[y][x] & 0xFF) < a)) {
                        pixels[y][x] = rgba(r, Math.min(g, 255), b, Math.min(a, 255));
                    }
                }
            }
        }
    }
    
    private static int rgba(int r, int g, int b, int a) {
        return ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF);
    }

    private static byte[] encodePng(int[][] pixels, int width, int height) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        baos.write(new byte[] { (byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        writeInt(ihdr, width);
        writeInt(ihdr, height);
        ihdr.write(8);
        ihdr.write(6);
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        writeChunk(baos, "IHDR", ihdr.toByteArray());
        
        ByteArrayOutputStream rawData = new ByteArrayOutputStream();
        for (int y = 0; y < height; y++) {
            rawData.write(0);
            for (int x = 0; x < width; x++) {
                int rgba = pixels[y][x];
                rawData.write((rgba >> 24) & 0xFF);
                rawData.write((rgba >> 16) & 0xFF);
                rawData.write((rgba >> 8) & 0xFF);
                rawData.write(rgba & 0xFF);
            }
        }
        
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        DeflaterOutputStream dos = new DeflaterOutputStream(compressed, deflater);
        dos.write(rawData.toByteArray());
        dos.finish();
        dos.close();
        
        writeChunk(baos, "IDAT", compressed.toByteArray());
        writeChunk(baos, "IEND", new byte[0]);
        
        return baos.toByteArray();
    }
    
    private static void writeChunk(ByteArrayOutputStream baos, String type, byte[] data) throws IOException {
        writeInt(baos, data.length);
        byte[] typeBytes = type.getBytes("US-ASCII");
        baos.write(typeBytes);
        baos.write(data);
        
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(baos, (int) crc.getValue());
    }
    
    private static void writeInt(ByteArrayOutputStream baos, int value) {
        baos.write((value >> 24) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write(value & 0xFF);
    }
}
