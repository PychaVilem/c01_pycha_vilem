package model;

public class Edge {
    private int x1, y1, x2, y2; // Změněno z final na mutable pro orientate()

    public Edge(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public Edge(Point p1, Point p2) {
        this.x1 = p1.getX();
        this.y1 = p1.getY();
        this.x2 = p2.getX();
        this.y2 = p2.getY();
    }

    public boolean isHorizontal() {
        // podmínka, jestli je horizontální - y souřadnice jsou stejné
        return y1 == y2;
    }

    public void orientate() {
        if(y1 > y2) {
            // Prohodím vrcholy (y a x)
            int tempX = x1;
            int tempY = y1;
            x1 = x2;
            y1 = y2;
            x2 = tempX;
            y2 = tempY;
        }
    }

    public boolean isIntersection(int y) {
        // kontrola, zda horizontální čára y protíná tuto hranu
        // hrana musi byt orientovana (y1 < y2)
        return y1 <= y && y < y2;
    }

    public int getIntersection(int y) {
        // spocteni pruseciku - linearni interpolace
        // pouziti rovnici primky: x = x1 + (x2 - x1) * (y - y1) / (y2 - y1)
        if (y2 == y1) {
            return x1; // Horizontalni hrana (nemela by byt)
        }
        int x = x1 + (int) Math.round((double)(x2 - x1) * (y - y1) / (y2 - y1));
        return x;
    }

    public int getX1() {
        return x1;
    }

    public int getX2() {
        return x2;
    }

    public int getY1() {
        return y1;
    }

    public int getY2() {
        return y2;
    }
}
