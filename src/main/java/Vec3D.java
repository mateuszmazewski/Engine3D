import java.util.Objects;

import static java.lang.Math.abs;

public class Vec3D {
    private double x = 0.0;
    private double y = 0.0;
    private double z = 0.0;
    private double w = 1.0;
    private double lum = 0.0;

    public Vec3D() {
    }

    public Vec3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vec3D add(Vec3D vec1, Vec3D vec2) {
        return new Vec3D(
                vec1.x + vec2.x,
                vec1.y + vec2.y,
                vec1.z + vec2.z
        );
    }

    public static Vec3D subtract(Vec3D vec1, Vec3D vec2) {
        return new Vec3D(
                vec1.x - vec2.x,
                vec1.y - vec2.y,
                vec1.z - vec2.z
        );
    }

    public static Vec3D mult(Vec3D vec1, double v) {
        return new Vec3D(
                vec1.x * v,
                vec1.y * v,
                vec1.z * v
        );
    }

    public static Vec3D divide(Vec3D vec1, double v) {
        if (v < Util.EPS) {
            throw new IllegalArgumentException("Cannot divide by 0");
        }
        return new Vec3D(
                vec1.x / v,
                vec1.y / v,
                vec1.z / v
        );
    }

    public static double dotProduct(Vec3D vec1, Vec3D vec2) {
        return vec1.x * vec2.x + vec1.y * vec2.y + vec1.z * vec2.z;
    }

    public static double length(Vec3D vec) {
        return Math.sqrt(dotProduct(vec, vec));
    }

    public static Vec3D normalise(Vec3D vec) {
        double length = length(vec);
        return Vec3D.divide(vec, length);
    }

    public static Vec3D crossProduct(Vec3D vec1, Vec3D vec2) {
        Vec3D crossProduct = new Vec3D();
        crossProduct.x = vec1.y * vec2.z - vec1.z * vec2.y;
        crossProduct.y = vec1.z * vec2.x - vec1.x * vec2.z;
        crossProduct.z = vec1.x * vec2.y - vec1.y * vec2.x;
        return crossProduct;
    }

    public static Vec3D multMatrixVector(Matrix m, Vec3D v) {
        Vec3D result = new Vec3D();
        result.x = v.x * m.get(0, 0) + v.y * m.get(1, 0) + v.z * m.get(2, 0) + v.w * m.get(3, 0);
        result.y = v.x * m.get(0, 1) + v.y * m.get(1, 1) + v.z * m.get(2, 1) + v.w * m.get(3, 1);
        result.z = v.x * m.get(0, 2) + v.y * m.get(1, 2) + v.z * m.get(2, 2) + v.w * m.get(3, 2);
        result.w = v.x * m.get(0, 3) + v.y * m.get(1, 3) + v.z * m.get(2, 3) + v.w * m.get(3, 3);
        return result;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getW() {
        return w;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getLum() {
        return lum;
    }

    public void setLum(double lum) {
        this.lum = lum;
    }

    public String toString() {
        return "[" + Util.round(x, 2) + ", " + Util.round(y, 2) + ", " + Util.round(z, 2) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vec3D) {
            Vec3D vec = (Vec3D) o;
            return abs(vec.x - x) < Util.EPS && abs(vec.y - y) < Util.EPS && abs(vec.z - z) < Util.EPS && abs(vec.w - w) < Util.EPS;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }
}
