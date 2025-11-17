package rasterize;

import raster.RasterBufferedImage;

import java.awt.*;

public class LineRasterizerGraphics extends LineRasterizer {
    private Color currentColor = Color.RED;

    public LineRasterizerGraphics(RasterBufferedImage raster) {
        super(raster);
    }

    public void setColor(int color) {
        // Převod z int RGB na Color
        this.currentColor = new Color(color);
    }

    @Override
    public void rasterize(int x1, int y1, int x2, int y2) {
        Graphics g = raster.getImage().getGraphics();
        g.setColor(currentColor);
        g.drawLine(x1, y1, x2, y2);
    }
}
