public class Edge {
    Triangle triangle;
    Vec3D p1;
    Vec3D p2;
    Double xIntersection;

    public Edge(Triangle triangle, Vec3D p1, Vec3D p2) {
        this.triangle = triangle;
        this.p1 = p1;
        this.p2 = p2;
    }

    public Double xIntersection(int y) {
        double minY = Math.min(p1.getY(), p2.getY());
        double maxY = Math.max(p1.getY(), p2.getY());

        if (y <= minY || y >= maxY) {
            // There is no intersection
            return null;
        }

        return Util.scaleToRange(p1.getY(), p2.getY(), y, p1.getX(), p2.getX());
    }

    public Triangle getTriangle() {
        return triangle;
    }

    public void setTriangle(Triangle triangle) {
        this.triangle = triangle;
    }

    public Vec3D getP1() {
        return p1;
    }

    public void setP1(Vec3D p1) {
        this.p1 = p1;
    }

    public Vec3D getP2() {
        return p2;
    }

    public void setP2(Vec3D p2) {
        this.p2 = p2;
    }

    public Double getxIntersection() {
        return xIntersection;
    }

    public void setxIntersection(Double xIntersection) {
        this.xIntersection = xIntersection;
    }
}
