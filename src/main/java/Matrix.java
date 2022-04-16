public class Matrix {
    private int ncols;
    private int nrows;
    private double data[];

    public Matrix(int r, int c) {
        this.ncols = c;
        this.nrows = r;
        data = new double[c * r];
    }

    public static Matrix mult(Matrix A, Matrix B) {
        Matrix C = new Matrix(A.rows(), B.cols());

        for (int r = 0; r < A.rows(); r++) {
            for (int c = 0; c < B.cols(); c++) {
                double s = 0;
                for (int k = 0; k < A.cols(); k++) {
                    s += A.get(r, k) * B.get(k, c);
                }
                C.set(r, c, s);
            }
        }

        return C;
    }

    public void fill(double value) {
        for (int r = 0; r < nrows; r++) {
            for (int c = 0; c < ncols; c++) {
                set(r, c, value);
            }
        }
    }

    public double get(int r, int c) {
        return data[r * ncols + c];
    }

    public void set(int r, int c, double v) {
        data[r * ncols + c] = v;
    }

    public int rows() {
        return nrows;
    }

    public int cols() {
        return ncols;
    }
}