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
        double fovRad = fovDegrees / (2 * Math.PI);
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