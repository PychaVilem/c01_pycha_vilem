package fill;

import model.Edge;
import model.Point;
import model.Polygon;
import raster.Raster;
import rasterize.LineRasterizer;
import rasterize.PolygonRasterizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.OptionalInt;

public class ScanLineFiller implements Filler {
    private LineRasterizer lineRasterizer;
    private PolygonRasterizer polygonRasterizer;
    private Polygon polygon;
    private Raster raster;
    private int fillColor = 0xffff00; // zlulta
    private int borderColor = 0xff0000; // cervena
    private Point seedPoint; // pocatecni bod

    public ScanLineFiller(LineRasterizer lineRasterizer, PolygonRasterizer polygonRasterizer, Polygon polygon, Raster raster) {
        this(lineRasterizer, polygonRasterizer, polygon, raster, null);
    }

    public ScanLineFiller(LineRasterizer lineRasterizer, PolygonRasterizer polygonRasterizer, Polygon polygon, Raster raster, Point seedPoint) {
        this.lineRasterizer = lineRasterizer;
        this.polygonRasterizer = polygonRasterizer;
        this.polygon = polygon;
        this.raster = raster;
        this.seedPoint = seedPoint;
    }

    @Override
    public void fill() {
        if (polygon.getSize() < 3) {
            return;
        }

        //cervena hranice
        int originalColor = polygon.getColor();
        polygon.setColor(borderColor);
        polygonRasterizer.rasterize(polygon);
        polygon.setColor(originalColor);

        //vyplenni
        if (seedPoint != null) {
            fillWithSeedPoint(seedPoint);
        } else {
            fillEntirePolygon();
        }
    }

  //vypleni celku
    private void fillEntirePolygon() {
        ArrayList<Edge> edges = buildEdges();
        if (edges.isEmpty()) {
            return;
        }

        int[] yBounds = findYBounds();
        int yMin = yBounds[0];
        int yMax = yBounds[1];

        for (int y = yMin; y <= yMax; y++) {
            ArrayList<Integer> intersections = findIntersections(edges, y);
            Collections.sort(intersections);
            fillBetweenIntersections(intersections, y);
        }
    }

    // vyplenni oblasti jen ze seedpointu
    private void fillWithSeedPoint(Point seedPoint) {
        int seedX = seedPoint.getX();
        int seedY = seedPoint.getY();
        
        if (seedX < 0 || seedX >= raster.getWidth() || seedY < 0 || seedY >= raster.getHeight()) {
            return;
        }

        // zjistim barvu pozadi
        OptionalInt seedPixelColor = raster.getPixel(seedX, seedY);
        if (seedPixelColor.isEmpty()) {
            return;
        }
        int backgroundColor = seedPixelColor.getAsInt();
        
        if (backgroundColor == borderColor || backgroundColor == fillColor) {
            return;
        }

        // najdu dosazitelne pixely
        boolean[][] reachable = findReachablePixels(seedX, seedY, backgroundColor);

        ArrayList<Edge> edges = buildEdges();
        int[] yBounds = findYBounds();
        int yMin = yBounds[0];
        int yMax = yBounds[1];

        // vyplnim jen dosazitelne pixely
        for (int y = yMin; y <= yMax; y++) {
            ArrayList<Integer> intersections = findIntersections(edges, y);
            Collections.sort(intersections);
            
            for (int i = 0; i < intersections.size(); i += 2) {
                if (i + 1 < intersections.size()) {
                    int xStart = intersections.get(i);
                    int xEnd = intersections.get(i + 1);
                    
                    if (xStart > xEnd) {
                        int temp = xStart;
                        xStart = xEnd;
                        xEnd = temp;
                    }
                    
                    for (int x = xStart; x <= xEnd; x++) {
                        if (x >= 0 && x < raster.getWidth() && y >= 0 && y < raster.getHeight()) {
                            if (reachable[x][y]) {
                                OptionalInt pixelColor = raster.getPixel(x, y);
                                if (pixelColor.isPresent()) {
                                    int color = pixelColor.getAsInt();
                                    if (color != borderColor && color != fillColor) {
                                        raster.setPixel(x, y, fillColor);
                                    }
                                } else {
                                    raster.setPixel(x, y, fillColor);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // najdu dosazitelne body z seed pointu
    private boolean[][] findReachablePixels(int seedX, int seedY, int backgroundColor) {
        boolean[][] reachable = new boolean[raster.getWidth()][raster.getHeight()];
        java.util.Queue<IntPoint> queue = new java.util.LinkedList<>();
        queue.add(new IntPoint(seedX, seedY));
        reachable[seedX][seedY] = true;

        while (!queue.isEmpty()) {
            IntPoint p = queue.poll();
            int x = p.x;
            int y = p.y;

            int[] dx = {0, 1, 0, -1};
            int[] dy = {1, 0, -1, 0};
            
            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];
                
                if (nx >= 0 && nx < raster.getWidth() && ny >= 0 && ny < raster.getHeight()) {
                    if (!reachable[nx][ny]) {
                        OptionalInt pixelColor = raster.getPixel(nx, ny);
                        if (pixelColor.isPresent()) {
                            int color = pixelColor.getAsInt();
                            if (color == backgroundColor && color != borderColor && color != fillColor) {
                                reachable[nx][ny] = true;
                                queue.add(new IntPoint(nx, ny));
                            }
                        } else {
                            reachable[nx][ny] = true;
                            queue.add(new IntPoint(nx, ny));
                        }
                    }
                }
            }
        }
        
        return reachable;
    }

    // seznam hran polygonu (bez horizontalnich)
    private ArrayList<Edge> buildEdges() {
        ArrayList<Edge> edges = new ArrayList<>();
        for (int i = 0; i < polygon.getSize(); i++) {
            int indexA = i;
            int indexB = (i + 1) % polygon.getSize();

            Point a = polygon.getPoint(indexA);
            Point b = polygon.getPoint(indexB);

            Edge edge = new Edge(a, b);
            if (!edge.isHorizontal()) {
                edge.orientate();
                edges.add(edge);
            }
        }
        return edges;
    }

    // min a max y souradnice polygonu
    private int[] findYBounds() {
        int yMin = polygon.getPoint(0).getY();
        int yMax = polygon.getPoint(0).getY();
        for (int i = 1; i < polygon.getSize(); i++) {
            int y = polygon.getPoint(i).getY();
            if (y < yMin) yMin = y;
            if (y > yMax) yMax = y;
        }
        return new int[]{yMin, yMax};
    }

    // pruseciky scan-line s hranami polygonu
    private ArrayList<Integer> findIntersections(ArrayList<Edge> edges, int y) {
        ArrayList<Integer> intersections = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.isIntersection(y)) {
                int x = edge.getIntersection(y);
                intersections.add(x);
            }
        }
        Collections.sort(intersections);
        
        // odstranim duplicity
        ArrayList<Integer> uniqueIntersections = new ArrayList<>();
        for (int i = 0; i < intersections.size(); i++) {
            if (i == 0 || !intersections.get(i).equals(intersections.get(i - 1))) {
                uniqueIntersections.add(intersections.get(i));
            }
        }
        return uniqueIntersections;
    }

    // vyplnim mezi dvojicemi pruseciku
    private void fillBetweenIntersections(ArrayList<Integer> intersections, int y) {
        for (int i = 0; i < intersections.size(); i += 2) {
            if (i + 1 < intersections.size()) {
                int xStart = intersections.get(i);
                int xEnd = intersections.get(i + 1);
                
                for (int x = xStart; x <= xEnd; x++) {
                    if (x >= 0 && x < raster.getWidth() && y >= 0 && y < raster.getHeight()) {
                        OptionalInt pixelColor = raster.getPixel(x, y);
                        if (pixelColor.isPresent()) {
                            int color = pixelColor.getAsInt();
                            if (color != borderColor && color != fillColor) {
                                raster.setPixel(x, y, fillColor);
                            }
                        } else {
                            raster.setPixel(x, y, fillColor);
                        }
                    }
                }
            }
        }
    }

    // pomocna trida pro souradnice v BFS
    private static class IntPoint {
        int x, y;
        IntPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
