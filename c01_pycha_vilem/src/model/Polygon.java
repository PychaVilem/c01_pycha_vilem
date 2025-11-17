package model;

import java.util.ArrayList;
import java.util.List;

public class Polygon {
    private final List<Point> points;
    private int color = 0xff0000; // vychozi cervena

    public Polygon() {
        this.points = new ArrayList<>();
    }

    public Polygon(List<Point> points) {
        // zkopiruje vstupni seznam
        this.points = new ArrayList<>(points);
    }

    public void addPoint(Point p) {
        points.add(p);
    }

    public Point getPoint(int index) {
        return points.get(index);
    }

    public int getSize() {
        return points.size();
    }

    public List<Point> getPoints() {
        // vrati kopii seznamu
        return new ArrayList<>(points);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    // nastaveni pozici vrcholu
    public void setPoint(int index, Point point) {
        if (index >= 0 && index < points.size()) {
            points.set(index, point);
        }
    }

    // odstraneni vrchol
    public void removePoint(int index) {
        if (index >= 0 && index < points.size()) {
            points.remove(index);
        }
    }
}
