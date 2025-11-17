package fill;

import raster.Raster;

import java.util.OptionalInt;
import java.util.Stack;

public class SeedFiller implements Filler {
    private final Raster raster;
    private final int fillColor;
    private final int borderColor;
    private int backgroundColor;
    private final int startX, startY;

    public SeedFiller(Raster raster, int fillColor, int borderColor, int startX, int startY) {
        this.raster = raster;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.startX = startX;
        this.startY = startY;

        // nacteni barvu pixelu, na ktery jsem klikl (startx, starty)
        OptionalInt pixelColor = raster.getPixel(startX, startY);
        if(pixelColor.isPresent())
            this.backgroundColor = pixelColor.getAsInt();
    }

    @Override
    public void fill() {
        seedFillIterative(startX, startY);
    }

    private void seedFillIterative(int startX, int startY) {
        // kontrola hranic
        if (startX < 0 || startX >= raster.getWidth() || startY < 0 || startY >= raster.getHeight()) {
            return;
        }

        // kontrola jestli uz neni vyplneny nebo hranice
        OptionalInt startPixel = raster.getPixel(startX, startY);
        if (startPixel.isEmpty() || startPixel.getAsInt() == fillColor || startPixel.getAsInt() == borderColor) {
            return;
        }

        Stack<Point> stack = new Stack<>();
        stack.push(new Point(startX, startY));

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int x = p.x;
            int y = p.y;

            if (x < 0 || x >= raster.getWidth() || y < 0 || y >= raster.getHeight()) {
                continue;
            }

            OptionalInt pixelColor = raster.getPixel(x, y);
            if (pixelColor.isEmpty()) {
                continue;
            }

            // obarveni jen pozadi, ne hranici
            if (pixelColor.getAsInt() != backgroundColor || pixelColor.getAsInt() == borderColor) {
                continue;
            }

            raster.setPixel(x, y, fillColor);

            // pridani sousedy do zasobniku
            stack.push(new Point(x + 1, y));
            stack.push(new Point(x - 1, y));
            stack.push(new Point(x, y + 1));
            stack.push(new Point(x, y - 1));
        }
    }

    // pomocna trida pro souradnice
    private static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
