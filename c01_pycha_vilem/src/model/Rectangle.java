package model;

import java.util.ArrayList;
import java.util.List;

public class Rectangle extends Polygon {

    public Rectangle(List<Point> points) {
        super(points);
    }

    public static Rectangle fromBaseAndHeight(Point baseStart, Point baseEnd, Point heightPoint) {
        List<Point> points = new ArrayList<>();
        
        // Vypocet vysky (rozdil Y souradnic)
        int height = heightPoint.getY() - baseStart.getY();
        
        // 1. baseStart
        points.add(baseStart);
        // 2. baseEnd
        points.add(baseEnd);
        // 3. baseEnd + height (doprava nahoru/dolu)
        points.add(new Point(baseEnd.getX(), baseEnd.getY() + height));
        // 4. baseStart + height (doleva nahoru/dolu)
        points.add(new Point(baseStart.getX(), baseStart.getY() + height));
        
        return new Rectangle(points);
    }
}
