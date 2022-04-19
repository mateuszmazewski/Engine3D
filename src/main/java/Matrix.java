public class Matrix {
    private double[][] data;

    public Matrix() {
        data = new double[4][4];
    }

    public static Matrix makeIdentity() {
        Matrix m = new Matrix();
        for (int i = 0; i < 4; i++) {
            m.set(i, i, 1.0);
        }
        return m;
    }

    public static Matrix makeRotationX(double angleRad) {
        Matrix m = new Matrix();
        m.set(0, 0, 1.0);
        m.set(1, 1, Math.cos(angleRad));
        m.set(1, 2, Math.sin(angleRad));
        m.set(2, 1, -Math.sin(angleRad));
        m.set(2, 2, Math.cos(angleRad));
        m.set(3, 3, 1.0);
        return m;
    }

    public static Matrix makeRotationY(double angleRad) {
        Matrix m = new Matrix();
        m.set(0, 0, Math.cos(angleRad));
        m.set(0, 2, Math.sin(angleRad));
        m.set(2, 0, -Math.sin(angleRad));
        m.set(1, 1, 1.0);
        m.set(2, 2, Math.cos(angleRad));
        m.set(3, 3, 1.0);
        return m;
    }

    public static Matrix makeRotationZ(double angleRad) {
        Matrix m = new Matrix();
        m.set(0, 0, Math.cos(angleRad));
        m.set(0, 1, Math.sin(angleRad));
        m.set(1, 0, -Math.sin(angleRad));
        m.set(1, 1, Math.cos(angleRad));
        m.set(2, 2, 1.0);
        m.set(3, 3, 1.0);
        return m;
    }

    public static Matrix makeTranslation(double x, double y, double z) {
        Matrix m = new Matrix();
        m.set(0, 0, 1.0);
        m.set(1, 1, 1.0);
        m.set(2, 2, 1.0);
        m.set(3, 3, 1.0);
        m.set(3, 0, x);
        m.set(3, 1, y);
        m.set(3, 2, z);
        return m;
    }

    public static Matrix makeProjection(double fovDegrees, double aspectRatio, double zNear, double zFar) {
        Matrix m = new Matrix();
        double fovRad = fovDegrees * (2 * Math.PI) / 360.0;
        double fovCoefficient = 1.0 / Math.tan(0.5 * fovRad);

        m.set(0, 0, aspectRatio * fovCoefficient);
        m.set(1, 1, fovCoefficient);
        m.set(2, 2, zFar / (zFar - zNear));
        m.set(3, 2, (-zFar * zNear) / (zFar - zNear));
        m.set(2, 3, 1.0);
        m.set(3, 3, 0.0);
        return m;
    }

    public static Matrix mult(Matrix A, Matrix B) {
        Matrix C = new Matrix();
        double sum;

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                sum = 0.0;
                for (int k = 0; k < 4; k++) {
                    sum += A.data[r][k] * B.data[k][c];
                }
                C.data[r][c] = sum;
            }
        }

        return C;
    }

    public static Matrix makePointAtMatrix(Vec3D position, Vec3D target, Vec3D up) {
        Vec3D newForward = Vec3D.subtract(target, position);
        newForward = Vec3D.normalise(newForward);

        Vec3D a = Vec3D.mult(newForward, Vec3D.dotProduct(up, newForward));
        Vec3D newUp = Vec3D.subtract(up, a);
        newUp = Vec3D.normalise(newUp);

        Vec3D newRight = Vec3D.crossProduct(newUp, newForward);

        // Rotation and translation matrix
        Matrix pointAtMatrix = new Matrix();
        pointAtMatrix.set(0, 0, newRight.getX());
        pointAtMatrix.set(0, 1, newRight.getY());
        pointAtMatrix.set(0, 2, newRight.getZ());
        pointAtMatrix.set(0, 3, 0.0);

        pointAtMatrix.set(1, 0, newUp.getX());
        pointAtMatrix.set(1, 1, newUp.getY());
        pointAtMatrix.set(1, 2, newUp.getZ());
        pointAtMatrix.set(1, 3, 0.0);

        pointAtMatrix.set(2, 0, newForward.getX());
        pointAtMatrix.set(2, 1, newForward.getY());
        pointAtMatrix.set(2, 2, newForward.getZ());
        pointAtMatrix.set(2, 3, 0.0);

        pointAtMatrix.set(3, 0, position.getX());
        pointAtMatrix.set(3, 1, position.getY());
        pointAtMatrix.set(3, 2, position.getZ());
        pointAtMatrix.set(3, 3, 1.0);

        return pointAtMatrix;
    }

    public static Matrix quickInverse(Matrix m) {
        // Only for "PointAtMatrix"
        Matrix inv = new Matrix();
        double v;

        inv.set(0, 0, m.get(0, 0));
        inv.set(0, 1, m.get(1, 0));
        inv.set(0, 2, m.get(2, 0));
        inv.set(0, 3, 0.0);

        inv.set(1, 0, m.get(0, 1));
        inv.set(1, 1, m.get(1, 1));
        inv.set(1, 2, m.get(2, 1));
        inv.set(1, 3, 0.0);

        inv.set(2, 0, m.get(0, 2));
        inv.set(2, 1, m.get(1, 2));
        inv.set(2, 2, m.get(2, 2));
        inv.set(2, 3, 0.0);

        v = -(m.get(3, 0) * m.get(0, 0) + m.get(3, 1) * m.get(1, 0) + m.get(3, 2) * m.get(2, 0));
        inv.set(3, 0, v);
        v = -(m.get(3, 0) * m.get(0, 1) + m.get(3, 1) * m.get(1, 1) + m.get(3, 2) * m.get(2, 1));
        inv.set(3, 1, v);
        v = -(m.get(3, 0) * m.get(0, 2) + m.get(3, 1) * m.get(1, 2) + m.get(3, 2) * m.get(2, 2));
        inv.set(3, 2, v);
        inv.set(3, 3, 1.0);

        return inv;
    }

    public void fill(double value) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                set(r, c, value);
            }
        }
    }

    public double get(int r, int c) {
        return data[r][c];
    }

    public void set(int r, int c, double v) {
        data[r][c] = v;
    }

}