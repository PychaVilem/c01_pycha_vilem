package raster;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.OptionalInt;

public class RasterBufferedImage implements Raster {

    private BufferedImage image;

    public RasterBufferedImage(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public void setPixel(int x, int y, int color) {
        // ošetřit zápis mimo raster
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return; // Ignorujeme zápis mimo hranice
        }
        image.setRGB(x, y, color);
    }

    @Override
    public OptionalInt getPixel(int x, int y) {
        // podmínka - kontrola hranic
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return OptionalInt.empty(); // Vrátíme prázdný Optional pro pixely mimo hranice
        }
        return OptionalInt.of(image.getRGB(x, y));
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public void clear() {
        Graphics g = image.getGraphics();
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
    }

    public BufferedImage getImage() {
        return image;
    }
}
