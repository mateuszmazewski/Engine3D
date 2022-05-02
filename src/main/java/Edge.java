public class Edge {
    Triangle triangle;
    Vec3D p1;
    Vec3D p2;

    public Edge(Triangle triangle, Vec3D p1, Vec3D p2) {
        this.triangle = triangle;
        this.p1 = p1;
        this.p2 = p2;
    }

    public Integer xIntersection(int y) {
        int min, max;
        min = (int)Math.min(p1.getY(), p2.getY());
        max = (int)Math.max(p1.getY(), p2.getY());

        if (y < min || y > max) {
            // There is no intersection
            return null;
        }

        double a = (p2.getY() - p1.getY()) / (p2.getX() - p1.getX());
        double b = p1.getY() - a * p1.getX();
        double xCross = (p1.getY() - b) / a;

        return (int) xCross;
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
}
