package clip;

import model.Point;
import raster.Raster;

import java.util.ArrayList;
import java.util.List;

public class Clipper {
    
    //  dodelat sutherland-hodgman algoritmus
    public List<Point> clip(List<Point> clipperPoints, List<Point> pointsToClip) {
        List<Point> pointsToReturn = new ArrayList<>();
        return pointsToReturn;
    }

    public void clipRaster(Raster raster, List<Point> clipperPoints) {
        if (clipperPoints.size() < 3) {
            return;
        }

        int width = raster.getWidth();
        int height = raster.getHeight();

        // projedu vsechny pixely
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point pixelPoint = new Point(x, y);
                if (!isPointInPolygon(pixelPoint, clipperPoints)) {
                    raster.setPixel(x, y, 0x000000); // smazu vse mimo
                }
            }
        }
    }

    // ray casting algoritmus - kontroluje jestli je bod uvnitr polygonu
    private boolean isPointInPolygon(Point point, List<Point> polygonPoints) {
        if (polygonPoints.size() < 3) {
            return false;
        }

        int x = point.getX();
        int y = point.getY();
        boolean inside = false;

        for (int i = 0, j = polygonPoints.size() - 1; i < polygonPoints.size(); j = i++) {
            Point pi = polygonPoints.get(i);
            Point pj = polygonPoints.get(j);

            int xi = pi.getX();
            int yi = pi.getY();
            int xj = pj.getX();
            int yj = pj.getY();

            // kontroluju jestli paprsek protina hranu
            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (double)(yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }
}
