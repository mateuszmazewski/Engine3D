public class Matrix {
    private double[][] data;

    public Matrix() {
        data = new double[4][4];
        fill(0.0);
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
        m.data[0][0] = 1.0;
        m.data[1][1] = Math.cos(angleRad);
        m.data[1][2] = Math.sin(angleRad);
        m.data[2][1] = -Math.sin(angleRad);
        m.data[2][2] = Math.cos(angleRad);
        m.data[3][3] = 1.0;
        return m;
    }

    public static Matrix makeRotationY(double angleRad) {
        Matrix m = new Matrix();
        m.data[0][0] = Math.cos(angleRad);
        m.data[0][2] = Math.sin(angleRad);
        m.data[2][0] = -Math.sin(angleRad);
        m.data[1][1] = 1.0;
        m.data[2][2] = Math.cos(angleRad);
        m.data[3][3] = 1.0;
        return m;
    }

    public static Matrix makeRotationZ(double angleRad) {
        Matrix m = new Matrix();
        m.data[0][0] = Math.cos(angleRad);
        m.data[0][1] = Math.sin(angleRad);
        m.data[1][0] = -Math.sin(angleRad);
        m.data[1][1] = Math.cos(angleRad);
        m.data[2][2] = 1.0;
        m.data[3][3] = 1.0;
        return m;
    }

    public static Matrix makeTranslation(double x, double y, double z) {
        Matrix m = makeIdentity();
        m.data[3][0] = x;
        m.data[3][1] = y;
        m.data[3][2] = z;
        return m;
    }

    public static Matrix makeProjection(double fovDegrees, double aspectRatio, double zNear, double zFar) {
        Matrix m = new Matrix();
        double fovRad = fovDegrees * (2 * Math.PI) / 360.0;
        double fovCoefficient = 1.0 / Math.tan(0.5 * fovRad);

        m.data[0][0] = aspectRatio * fovCoefficient;
        m.data[1][1] = fovCoefficient;
        m.data[2][2] = zFar / (zFar - zNear);
        m.data[3][2] = (-zFar * zNear) / (zFar - zNear);
        m.data[2][3] = 1.0;
        m.data[3][3] = 0.0;
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
        Matrix m = new Matrix();
        m.data[0][0] = newRight.getX();
        m.data[0][1] = newRight.getY();
        m.data[0][2] = newRight.getZ();
        m.data[0][3] = 0.0;

        m.data[1][0] = newUp.getX();
        m.data[1][1] = newUp.getY();
        m.data[1][2] = newUp.getZ();
        m.data[1][3] = 0.0;

        m.data[2][0] = newForward.getX();
        m.data[2][1] = newForward.getY();
        m.data[2][2] = newForward.getZ();
        m.data[2][3] = 0.0;

        m.data[3][0] = position.getX();
        m.data[3][1] = position.getY();
        m.data[3][2] = position.getZ();
        m.data[3][3] = 1.0;

        return m;
    }

    public static Matrix quickInverse(Matrix m) {
        // Only for rotation / translation matrices
        Matrix inv = new Matrix();

        inv.data[0][0] = m.data[0][0];
        inv.data[0][1] = m.data[1][0];
        inv.data[0][2] = m.data[2][0];
        inv.data[0][3] = 0.0;

        inv.data[1][0] = m.data[0][1];
        inv.data[1][1] = m.data[1][1];
        inv.data[1][2] = m.data[2][1];
        inv.data[1][3] = 0.0;

        inv.data[2][0] = m.data[0][2];
        inv.data[2][1] = m.data[1][2];
        inv.data[2][2] = m.data[2][2];
        inv.data[2][3] = 0.0;

        inv.data[3][0] = -(m.data[3][0] * inv.data[0][0] + m.data[3][1] * inv.data[1][0] + m.data[3][2] * inv.data[2][0]);
        inv.data[3][1] = -(m.data[3][0] * inv.data[0][1] + m.data[3][1] * inv.data[1][1] + m.data[3][2] * inv.data[2][1]);
        inv.data[3][2] = -(m.data[3][0] * inv.data[0][2] + m.data[3][1] * inv.data[1][2] + m.data[3][2] * inv.data[2][2]);
        inv.data[3][3] = 1.0;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[\n");
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                sb.append(data[r][c]);
                if (c < 3) {
                    sb.append(", ");
                } else {
                    sb.append("\n");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}