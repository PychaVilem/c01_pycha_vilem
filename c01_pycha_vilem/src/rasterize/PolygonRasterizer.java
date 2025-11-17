package rasterize;

import model.Line;
import model.Polygon;

public class PolygonRasterizer {
    private LineRasterizer lineRasterizer;

    public PolygonRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    public void setLineRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    public void rasterize(Polygon polygon) {
        // Pokud je méně jak 3 pointy, nevykrelsuju

        // Nastavení barvy polygonu před vykreslením
        if (lineRasterizer instanceof LineRasterizerGraphics) {
            ((LineRasterizerGraphics) lineRasterizer).setColor(polygon.getColor());
        }

        // cyklus od i = 0 do konce seznamu pointů
        for (int i = 0; i < polygon.getSize(); i++) {
            int indexA = i;
            int indexB = (i + 1) % polygon.getSize();

            // spojím pointy na indexu A a indexu B
            Line line =new Line(polygon.getPoint(indexA), polygon.getPoint(indexB));
            lineRasterizer.rasterize(line);
        }

    }
}
