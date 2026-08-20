import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** 生成不带抗锯齿的 16x16 Minecraft 物品像素贴图。 */
public final class PixelOrePngGenerator {
    private static final int SIZE = 16;
    private static final int TRANSPARENT = 0x00000000;
    private static final int SHADOW = 0xFF3A2923;
    private static final int ROCK_DARK = 0xFF5D514A;
    private static final int ROCK = 0xFF85766A;
    private static final int ROCK_LIGHT = 0xFFAA9683;
    private static final int COPPER_DARK = 0xFF7A3321;
    private static final int COPPER = 0xFFB95A32;
    private static final int COPPER_LIGHT = 0xFFE08A4E;
    private static final int POWDER_DARK = 0xFF6E2D20;
    private static final int POWDER = 0xFFB65A36;
    private static final int POWDER_LIGHT = 0xFFE59A5D;
    private static final int NEUTRAL_DARK = 0xFF45484A;
    private static final int NEUTRAL = 0xFF767B7D;
    private static final int NEUTRAL_LIGHT = 0xFFB0B4AF;

    private PixelOrePngGenerator() {}

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length == 0 ? "src/main/resources/assets/tfc_oreprocess/textures/item" : args[0]);
        Files.createDirectories(output);
        write(output.resolve("crushed_copper_ore.png"), crushedOre());
        write(output.resolve("copper_ore_powder.png"), powder());
        write(output.resolve("copper_ore_pellet_25mb.png"), pellet(false));
        write(output.resolve("copper_ore_pellet_100mb.png"), pellet(true));
        write(output.resolve("processed_ore.png"), neutralOre());
        write(output.resolve("ore_powder.png"), neutralPowder());
        write(output.resolve("ore_pellet_25mb.png"), neutralPellet(false));
        write(output.resolve("ore_pellet_100mb.png"), neutralPellet(true));
    }

    private static BufferedImage crushedOre() {
        BufferedImage image = base();
        polygon(image, new int[]{2, 4, 3, 8, 5, 12, 9, 14, 14, 12, 13, 8, 15, 5, 11, 2, 7, 3}, ROCK_DARK);
        polygon(image, new int[]{3, 4, 4, 8, 6, 11, 10, 13, 13, 10, 12, 6, 10, 3, 7, 4}, ROCK);
        pixels(image, new int[][]{{4, 5}, {5, 9}, {8, 4}, {10, 11}, {12, 7}}, ROCK_LIGHT);
        pixels(image, new int[][]{{5, 6}, {6, 7}, {7, 8}, {8, 9}, {9, 10}, {10, 9}, {11, 8}}, COPPER_DARK);
        pixels(image, new int[][]{{6, 6}, {7, 7}, {8, 8}, {9, 9}, {10, 8}}, COPPER_LIGHT);
        return image;
    }

    private static BufferedImage powder() {
        BufferedImage image = base();
        polygon(image, new int[]{2, 10, 4, 7, 7, 8, 9, 6, 13, 8, 14, 12, 11, 14, 5, 13}, POWDER_DARK);
        polygon(image, new int[]{3, 10, 5, 8, 8, 9, 10, 7, 13, 9, 12, 12, 6, 12}, POWDER);
        pixels(image, new int[][]{{4, 10}, {6, 9}, {8, 10}, {10, 8}, {11, 11}, {7, 12}}, POWDER_LIGHT);
        pixels(image, new int[][]{{3, 12}, {5, 7}, {9, 6}, {13, 11}}, ROCK_DARK);
        return image;
    }

    private static BufferedImage pellet(boolean large) {
        BufferedImage image = base();
        if (large) {
            polygon(image, new int[]{2, 5, 4, 3, 11, 3, 14, 6, 13, 12, 10, 14, 4, 13, 2, 10}, COPPER_DARK);
            polygon(image, new int[]{4, 5, 5, 4, 10, 4, 12, 6, 11, 11, 9, 12, 5, 11, 4, 9}, COPPER);
            pixels(image, new int[][]{{5, 5}, {6, 4}, {7, 4}, {8, 5}, {9, 5}, {10, 6}}, COPPER_LIGHT);
            pixels(image, new int[][]{{3, 7}, {3, 8}, {3, 9}, {12, 7}, {12, 8}, {12, 9}}, SHADOW);
        } else {
            polygon(image, new int[]{4, 6, 6, 4, 11, 5, 13, 8, 11, 12, 6, 12, 4, 10}, COPPER_DARK);
            polygon(image, new int[]{5, 6, 7, 5, 10, 6, 12, 8, 10, 10, 7, 11, 5, 9}, COPPER);
            pixels(image, new int[][]{{6, 6}, {7, 5}, {8, 6}, {9, 6}, {10, 7}}, COPPER_LIGHT);
        }
        return image;
    }

    private static BufferedImage neutralOre() {
        BufferedImage image = base();
        polygon(image, new int[]{2, 4, 3, 8, 5, 12, 9, 14, 14, 12, 13, 8, 15, 5, 11, 2, 7, 3}, NEUTRAL_DARK);
        polygon(image, new int[]{3, 4, 4, 8, 6, 11, 10, 13, 13, 10, 12, 6, 10, 3, 7, 4}, NEUTRAL);
        pixels(image, new int[][]{{4, 5}, {5, 9}, {8, 4}, {10, 11}, {12, 7}}, NEUTRAL_LIGHT);
        return image;
    }

    private static BufferedImage neutralPowder() {
        BufferedImage image = base();
        polygon(image, new int[]{2, 10, 4, 7, 7, 8, 9, 6, 13, 8, 14, 12, 11, 14, 5, 13}, NEUTRAL_DARK);
        polygon(image, new int[]{3, 10, 5, 8, 8, 9, 10, 7, 13, 9, 12, 12, 6, 12}, NEUTRAL);
        pixels(image, new int[][]{{4, 10}, {6, 9}, {8, 10}, {10, 8}, {11, 11}, {7, 12}}, NEUTRAL_LIGHT);
        return image;
    }

    private static BufferedImage neutralPellet(boolean large) {
        BufferedImage image = base();
        if (large) {
            polygon(image, new int[]{2, 5, 4, 3, 11, 3, 14, 6, 13, 12, 10, 14, 4, 13, 2, 10}, NEUTRAL_DARK);
            polygon(image, new int[]{4, 5, 5, 4, 10, 4, 12, 6, 11, 11, 9, 12, 5, 11, 4, 9}, NEUTRAL);
            pixels(image, new int[][]{{5, 5}, {6, 4}, {7, 4}, {8, 5}, {9, 5}, {10, 6}}, NEUTRAL_LIGHT);
        } else {
            polygon(image, new int[]{4, 6, 6, 4, 11, 5, 13, 8, 11, 12, 6, 12, 4, 10}, NEUTRAL_DARK);
            polygon(image, new int[]{5, 6, 7, 5, 10, 6, 12, 8, 10, 10, 7, 11, 5, 9}, NEUTRAL);
            pixels(image, new int[][]{{6, 6}, {7, 5}, {8, 6}, {9, 6}, {10, 7}}, NEUTRAL_LIGHT);
        }
        return image;
    }

    private static BufferedImage base() {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) image.setRGB(x, y, TRANSPARENT);
        return image;
    }

    private static void polygon(BufferedImage image, int[] points, int color) {
        for (int i = 0; i < points.length; i += 2) image.setRGB(points[i], points[i + 1], color);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (inside(x, y, points)) image.setRGB(x, y, color);
            }
        }
    }

    private static boolean inside(int x, int y, int[] p) {
        boolean result = false;
        for (int i = 0, j = p.length - 2; i < p.length; i += 2) {
            int xi = p[i], yi = p[i + 1], xj = p[j], yj = p[j + 1];
            if ((yi > y) != (yj > y) && x < (double) (xj - xi) * (y - yi) / (yj - yi) + xi) result = !result;
            j = i;
        }
        return result;
    }

    private static void pixels(BufferedImage image, int[][] points, int color) {
        for (int[] point : points) image.setRGB(point[0], point[1], color);
    }

    private static void write(Path path, BufferedImage image) throws IOException {
        if (!ImageIO.write(image, "png", path.toFile())) throw new IOException("PNG 编码器不可用: " + path);
    }
}
