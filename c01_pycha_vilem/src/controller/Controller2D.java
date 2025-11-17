package controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import clip.Clipper;
import fill.Filler;
import fill.ScanLineFiller;
import fill.SeedFiller;
import model.Line;
import model.Point;
import model.Rectangle;
import model.Polygon;
import rasterize.*;
import view.Panel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

public class Controller2D {
    private final Panel panel;
    private int color = 0xffffff;

    private LineRasterizer lineRasterizer;
    private PolygonRasterizer polygonRasterizer;

    // To draw
    private Polygon polygon = new Polygon();
    private Polygon polygonClipper = new Polygon();
    private ArrayList<Rectangle> rectangles = new ArrayList<>();
    private ArrayList<Polygon> completedPolygons = new ArrayList<>();

    private DrawingMode currentMode = DrawingMode.DRAWING_POLYGON;
   
    // Seed filling
    private Filler seedFiller;
    private Point previewSeedFillPoint; // preview (modra)
    private ArrayList<Point> committedSeedFillPoints = new ArrayList<>(); // ulozene (zelena)
    
    // Scanline filling - mapa polygon -> seed point
    private java.util.Map<Polygon, Point> scanlineFillSeeds = new java.util.HashMap<>();
    
    // Clipping
    private boolean clippingActive = false;
    private List<Point> savedClipperPoints = null;
    
    // Polygon editing
    private Polygon editingPolygon = null;
    private int selectedVertexIndex = -1;
    private boolean isDraggingVertex = false;
    private static final int VERTEX_DETECTION_RADIUS = 12;

    private ArrayList<Line> lines = new ArrayList<>();
    private int startX, startY;
    private boolean isLineStartSet;
    
    // Rectangle drawing
    private Point rectangleBaseStart = null;
    private Point rectangleBaseEnd = null;
    private Point currentMousePosition = null;
    private boolean isDrawingRectangle = false;
    private int rectangleClickCount = 0;
    
    // Polygon drawing - pruzna hrana
    private boolean isDrawingPolygonEdge = false;

    public Controller2D(Panel panel) {
        this.panel = panel;

    
        lineRasterizer = new LineRasterizerGraphics(panel.getRaster());
        polygonRasterizer = new PolygonRasterizer(lineRasterizer);
        
        polygonClipper.setColor(0x87ceeb); // svetle modra

        initListeners();
    }

    private void initListeners() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentMode == DrawingMode.DRAWING_RECTANGLE) {
                    handleRectangleClick(e);
                } else if (currentMode == DrawingMode.DRAWING_CLIPPER) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // levy tlacitko - pridam bod
                        polygonClipper.addPoint(new Point(e.getX(), e.getY()));
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // pravy tlacitko - ulozim vyber a oriznu
                        if (polygonClipper.getSize() >= 3) {
                            savedClipperPoints = new ArrayList<>(polygonClipper.getPoints());
                            clippingActive = true;
                            polygonClipper = new Polygon();
                            polygonClipper.setColor(0x87ceeb);
                            drawScene();
                        }
                    }
                } else if (currentMode == DrawingMode.DRAWING_POLYGON) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (polygon.getSize() == 0) {
                            // zacina se novy polygon - deaktivujee oriznuti
                            clippingActive = false;
                            savedClipperPoints = null;
                            polygon = new Polygon();
                            polygon.addPoint(new Point(e.getX(), e.getY()));
                        }
                        isDrawingPolygonEdge = true;
                        currentMousePosition = new Point(e.getX(), e.getY());
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // pravy tlacitko - dokonceni polygon
                        if (polygon.getSize() >= 3) {
                            polygon.setColor(0xffffff);
                            completedPolygons.add(polygon);
                            polygon = new Polygon();
                            isDrawingPolygonEdge = false;
                            currentMousePosition = null;
                        }
                    }
                } else if (currentMode == DrawingMode.SCANLINE) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        Point seedPoint = new Point(e.getX(), e.getY());
                        scanlineFillSeeds.clear();
                        
                        // najde se polygon ktery obsahuje ten bod
                        if (polygon.getSize() >= 3 && isPointInPolygon(seedPoint, polygon)) {
                            scanlineFillSeeds.put(polygon, seedPoint);
                        } else {
                            for (Polygon completedPolygon : completedPolygons) {
                                if (completedPolygon.getSize() >= 3 && isPointInPolygon(seedPoint, completedPolygon)) {
                                    scanlineFillSeeds.put(completedPolygon, seedPoint);
                                    break;
                                }
                            }
                        }
                    }
                } else if (currentMode == DrawingMode.SEED_FILL) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // levy - preview (modra)
                        previewSeedFillPoint = new Point(e.getX(), e.getY());
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // pravy - ulozim jako zelenou
                        if (previewSeedFillPoint != null) {
                            committedSeedFillPoints.add(new Point(previewSeedFillPoint.getX(), previewSeedFillPoint.getY()));
                            seedFiller = new SeedFiller(panel.getRaster(), 0x00ff00, 0xff0000,
                                    previewSeedFillPoint.getX(), previewSeedFillPoint.getY());
                            seedFiller.fill();
                            previewSeedFillPoint = null;
                        }
                    }
                } else if (currentMode == DrawingMode.EDIT_POLYGON) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // levy - najdu nejblizsi vrchol a zacnu ho presouvat
                        VertexInfo vertexInfo = findNearestVertex(new Point(e.getX(), e.getY()));
                        if (vertexInfo != null) {
                            editingPolygon = vertexInfo.polygon;
                            selectedVertexIndex = vertexInfo.vertexIndex;
                            isDraggingVertex = true;
                        }
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // pravy - smazu vrchol
                        VertexInfo vertexInfo = findNearestVertex(new Point(e.getX(), e.getY()));
                        if (vertexInfo != null && vertexInfo.polygon.getSize() > 3) {
                            vertexInfo.polygon.removePoint(vertexInfo.vertexIndex);
                            if (editingPolygon == vertexInfo.polygon && selectedVertexIndex == vertexInfo.vertexIndex) {
                                editingPolygon = null;
                                selectedVertexIndex = -1;
                                isDraggingVertex = false;
                            }
                        }
                    }
                }

                drawScene();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentMode == DrawingMode.DRAWING_POLYGON && e.getButton() == MouseEvent.BUTTON1) {
                    // pruzna hrana - prida bod kde pusitil tlacitko
                    if (isDrawingPolygonEdge && polygon.getSize() > 0) {
                        polygon.addPoint(new Point(e.getX(), e.getY()));
                    }
                    isDrawingPolygonEdge = false;
                    currentMousePosition = null;
                    drawScene();
                } else if (currentMode == DrawingMode.EDIT_POLYGON && e.getButton() == MouseEvent.BUTTON1) {
                    if (isDraggingVertex) {
                        isDraggingVertex = false;
                        drawScene();
                    }
                }
            }
        });
        
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentMode == DrawingMode.DRAWING_RECTANGLE && isDrawingRectangle) {
                    currentMousePosition = new Point(e.getX(), e.getY());
                    drawScene();
                } else if (currentMode == DrawingMode.DRAWING_POLYGON && isDrawingPolygonEdge) {
                    // aktualizuje pruznou hranu
                    currentMousePosition = new Point(e.getX(), e.getY());
                    drawScene();
                } else if (currentMode == DrawingMode.EDIT_POLYGON && isDraggingVertex) {
                    // presouva vrchol
                    if (editingPolygon != null && selectedVertexIndex >= 0) {
                        editingPolygon.setPoint(selectedVertexIndex, new Point(e.getX(), e.getY()));
                        drawScene();
                    }
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                if (currentMode == DrawingMode.DRAWING_RECTANGLE && isDrawingRectangle) {
                    currentMousePosition = new Point(e.getX(), e.getY());
                    drawScene();
                }
            }
        });
        

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
    }
    
    private void handleRectangleClick(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (rectangleClickCount == 0) {
                // prvni klik - zacatek zakladny
                clippingActive = false;
                savedClipperPoints = null;
                rectangleBaseStart = new Point(e.getX(), e.getY());
                isDrawingRectangle = true;
                rectangleClickCount++;
                currentMousePosition = new Point(e.getX(), e.getY());
            } else if (rectangleClickCount == 1) {
                // druhy klik - konec zakladny
                rectangleBaseEnd = new Point(e.getX(), e.getY());
                rectangleClickCount++;
                currentMousePosition = new Point(e.getX(), e.getY());
            } else if (rectangleClickCount == 2) {
                // treti klik - vyska
                Point heightPoint = new Point(e.getX(), e.getY());
                Rectangle rect = Rectangle.fromBaseAndHeight(rectangleBaseStart, rectangleBaseEnd, heightPoint);
                rect.setColor(0xffffff); // bila
                rectangles.add(rect);
                rectangleBaseStart = null;
                rectangleBaseEnd = null;
                currentMousePosition = null;
                isDrawingRectangle = false;
                rectangleClickCount = 0;
            }
        }
    }

    private void handleKeyPress(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case KeyEvent.VK_O:
                currentMode = DrawingMode.DRAWING_CLIPPER;
                polygonClipper.setColor(0x87ceeb);
                break;
                
            case KeyEvent.VK_P:
                currentMode = DrawingMode.DRAWING_POLYGON;
                clippingActive = false;
                savedClipperPoints = null;
                break;
                
            case KeyEvent.VK_R:
                currentMode = DrawingMode.DRAWING_RECTANGLE;
                isDrawingRectangle = false;
                rectangleBaseStart = null;
                rectangleBaseEnd = null;
                currentMousePosition = null;
                rectangleClickCount = 0;
                clippingActive = false;
                savedClipperPoints = null;
                break;
                
            case KeyEvent.VK_S:
                currentMode = DrawingMode.SCANLINE;
                break;

            case KeyEvent.VK_F:
                currentMode = DrawingMode.SEED_FILL;
                break;
                
            case KeyEvent.VK_E:
                currentMode = DrawingMode.EDIT_POLYGON;
                editingPolygon = null;
                selectedVertexIndex = -1;
                isDraggingVertex = false;
                break;

            case KeyEvent.VK_C:
                clearAll();
                break;
        }

        // Po kazde zmene se prekresli
        drawScene();
    }
    
 
  
    private void clearAll() {
        polygon = new Polygon();
        polygonClipper = new Polygon();
        polygonClipper.setColor(0x87ceeb); // svetla modra
        clippingActive = false;
        savedClipperPoints = null;
        completedPolygons.clear();
        rectangles.clear();
        lines.clear();
        previewSeedFillPoint = null;
        committedSeedFillPoints.clear();
        scanlineFillSeeds.clear();
        isDrawingRectangle = false;
        rectangleBaseStart = null;
        rectangleBaseEnd = null;
        currentMousePosition = null;
        rectangleClickCount = 0;
    }

    private void drawScene() {
        panel.getRaster().clear();

        polygonRasterizer.rasterize(polygonClipper);
        
        // dokoncene polygony
        for (Polygon completedPolygon : completedPolygons) {
            polygonRasterizer.rasterize(completedPolygon);
        }
        
        // aktualne kresleny polygon
        if (polygon.getSize() > 0) {
            polygonRasterizer.rasterize(polygon);
        }
        
        // pruzna hrana polygonu
        if (currentMode == DrawingMode.DRAWING_POLYGON && isDrawingPolygonEdge
                && polygon.getSize() > 0 && currentMousePosition != null) {
            if (lineRasterizer instanceof LineRasterizerGraphics) {
                ((LineRasterizerGraphics) lineRasterizer).setColor(polygon.getColor());
            }
            Point last = polygon.getPoint(polygon.getSize() - 1);
            lineRasterizer.rasterize(
                    last.getX(),
                    last.getY(),
                    currentMousePosition.getX(),
                    currentMousePosition.getY()
            );
        }
        
        // obdelniky
        for (Rectangle rect : rectangles) {
            polygonRasterizer.rasterize(rect);
        }
        
        // nahled obdelniku (bila)
        if (isDrawingRectangle && rectangleBaseStart != null && currentMousePosition != null) {
            if (lineRasterizer instanceof LineRasterizerGraphics) {
                ((LineRasterizerGraphics) lineRasterizer).setColor(0xffffff);
            }
            
            if (rectangleClickCount == 1) {
                // prvni klik - zakladna
                lineRasterizer.rasterize(
                    rectangleBaseStart.getX(), 
                    rectangleBaseStart.getY(), 
                    currentMousePosition.getX(), 
                    currentMousePosition.getY()
                );
            } else if (rectangleClickCount == 2 && rectangleBaseEnd != null) {
                // druhy klik - nahled obdelniku
                lineRasterizer.rasterize(
                    rectangleBaseStart.getX(), 
                    rectangleBaseStart.getY(), 
                    rectangleBaseEnd.getX(), 
                    rectangleBaseEnd.getY()
                );
                int height = currentMousePosition.getY() - rectangleBaseStart.getY();
                lineRasterizer.rasterize(
                    rectangleBaseStart.getX(), 
                    rectangleBaseStart.getY() + height, 
                    rectangleBaseEnd.getX(), 
                    rectangleBaseEnd.getY() + height
                );
                lineRasterizer.rasterize(
                    rectangleBaseStart.getX(), 
                    rectangleBaseStart.getY(), 
                    rectangleBaseStart.getX(), 
                    rectangleBaseStart.getY() + height
                );
                lineRasterizer.rasterize(
                    rectangleBaseEnd.getX(), 
                    rectangleBaseEnd.getY(), 
                    rectangleBaseEnd.getX(), 
                    rectangleBaseEnd.getY() + height
                );
            }
        }

        // vrcholy pro editaci
        if (currentMode == DrawingMode.EDIT_POLYGON) {
            drawVertices();
        }

        // scanline filling
        for (java.util.Map.Entry<Polygon, Point> entry : scanlineFillSeeds.entrySet()) {
            Polygon filledPolygon = entry.getKey();
            Point seedPoint = entry.getValue();
            if (filledPolygon.getSize() >= 3) {
                Filler scanLine = new ScanLineFiller(lineRasterizer, polygonRasterizer, filledPolygon, panel.getRaster(), seedPoint);
                scanLine.fill();
            }
        }

        // seed filler - trvale ulozene (zelena)
        if (!committedSeedFillPoints.isEmpty()) {
            for (Point p : committedSeedFillPoints) {
                seedFiller = new SeedFiller(panel.getRaster(), 0x00ff00, 0xff0000,
                        p.getX(), p.getY());
                seedFiller.fill();
            }
        }

        // seed filler - preview (modra)
        if (currentMode == DrawingMode.SEED_FILL && previewSeedFillPoint != null) {
            seedFiller = new SeedFiller(panel.getRaster(), 0x0000ff, 0xff0000,
                    previewSeedFillPoint.getX(), previewSeedFillPoint.getY());
            seedFiller.fill();
        }

        // oriznuti
        if (clippingActive && savedClipperPoints != null && savedClipperPoints.size() >= 3) {
            Clipper clipper = new Clipper();
            clipper.clipRaster(panel.getRaster(), savedClipperPoints);
        }

        panel.repaint();
    }

    //  kontroluje jestli je bod uvnitr polygonu
    private boolean isPointInPolygon(Point point, Polygon polygon) {
        if (polygon.getSize() < 3) {
            return false;
        }

        int x = point.getX();
        int y = point.getY();
        boolean inside = false;

        for (int i = 0, j = polygon.getSize() - 1; i < polygon.getSize(); j = i++) {
            Point pi = polygon.getPoint(i);
            Point pj = polygon.getPoint(j);

            int xi = pi.getX();
            int yi = pi.getY();
            int xj = pj.getX();
            int yj = pj.getY();

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (double)(yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }

    // pomocna trida pro info o vrcholu
    private static class VertexInfo {
        Polygon polygon;
        int vertexIndex;
        
        VertexInfo(Polygon polygon, int vertexIndex) {
            this.polygon = polygon;
            this.vertexIndex = vertexIndex;
        }
    }

    // najde nejblizsi vrchol k kliknutemu bodu
    private VertexInfo findNearestVertex(Point clickPoint) {
        int clickX = clickPoint.getX();
        int clickY = clickPoint.getY();
        double minDistance = VERTEX_DETECTION_RADIUS + 1;
        VertexInfo nearestVertex = null;

        for (Polygon completedPolygon : completedPolygons) {
            if (completedPolygon.getSize() < 3) {
                continue;
            }
            for (int i = 0; i < completedPolygon.getSize(); i++) {
                Point vertex = completedPolygon.getPoint(i);
                double distance = Math.sqrt(Math.pow(clickX - vertex.getX(), 2) + Math.pow(clickY - vertex.getY(), 2));
                if (distance < minDistance && distance <= VERTEX_DETECTION_RADIUS) {
                    minDistance = distance;
                    nearestVertex = new VertexInfo(completedPolygon, i);
                }
            }
        }

        if (polygon.getSize() >= 3) {
            for (int j = 0; j < polygon.getSize(); j++) {
                Point vertex = polygon.getPoint(j);
                double distance = Math.sqrt(Math.pow(clickX - vertex.getX(), 2) + Math.pow(clickY - vertex.getY(), 2));
                if (distance < minDistance && distance <= VERTEX_DETECTION_RADIUS) {
                    minDistance = distance;
                    nearestVertex = new VertexInfo(polygon, j);
                }
            }
        }

        return nearestVertex;
    }

    // vykresli vrcholy polygonu 
    private void drawVertices() {
        if (!(lineRasterizer instanceof LineRasterizerGraphics)) {
            return;
        }
        
        LineRasterizerGraphics graphicsRasterizer = (LineRasterizerGraphics) lineRasterizer;
        int vertexSize = 4;
        
        for (Polygon completedPolygon : completedPolygons) {
            if (completedPolygon.getSize() < 3) {
                continue;
            }
            for (int i = 0; i < completedPolygon.getSize(); i++) {
                Point vertex = completedPolygon.getPoint(i);
                int vertexX = vertex.getX();
                int vertexY = vertex.getY();
                
                boolean isSelected = (editingPolygon == completedPolygon && selectedVertexIndex == i);
                int color = isSelected ? 0x0000ff : 0xffff00;
                graphicsRasterizer.setColor(color);
                
                drawVertexSquare(graphicsRasterizer, vertexX, vertexY, vertexSize);
            }
        }

        if (polygon.getSize() >= 3) {
            for (int i = 0; i < polygon.getSize(); i++) {
                Point vertex = polygon.getPoint(i);
                int vertexX = vertex.getX();
                int vertexY = vertex.getY();
                
                boolean isSelected = (editingPolygon == polygon && selectedVertexIndex == i);
                int color = isSelected ? 0x0000ff : 0xffff00;
                graphicsRasterizer.setColor(color);
                
                drawVertexSquare(graphicsRasterizer, vertexX, vertexY, vertexSize);
            }
        }
    }

    // vykresli ctverecek kolem vrcholu
    private void drawVertexSquare(LineRasterizerGraphics rasterizer, int x, int y, int size) {
        rasterizer.rasterize(x - size, y - size, x + size, y - size);
        rasterizer.rasterize(x + size, y - size, x + size, y + size);
        rasterizer.rasterize(x + size, y + size, x - size, y + size);
        rasterizer.rasterize(x - size, y + size, x - size, y - size);
    }

}
