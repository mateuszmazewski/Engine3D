public class Triangle {
    private Vec3D[] vecs;

    public Triangle(Vec3D[] vecs) {
        this.vecs = vecs;
    }

    public Vec3D[] getVecs() {
        return vecs;
    }

    public void setVecs(Vec3D[] vecs) {
        if (vecs != null && vecs.length != 3) {
            throw new IllegalArgumentException("Triangle must have 3 vertices");
        }
        this.vecs = vecs;
    }

    @Override
    public String toString() {
        return "[" + vecs[0] + ", " + vecs[1] + ", " + vecs[2] + "]";
    }

}
