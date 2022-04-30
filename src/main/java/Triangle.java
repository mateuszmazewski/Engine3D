public class Triangle {
    private Vec3D[] vecs;
    private double luminance = 0.0;

    public Triangle() {
    }

    public Triangle(Vec3D[] vecs) {
        this.vecs = vecs;
    }

    public Triangle clone() {
        Triangle clonedTriangle = new Triangle();
        Vec3D[] clonedVecs = {new Vec3D(), new Vec3D(), new Vec3D()};
        for (int i = 0; i < 3; i++) {
            clonedVecs[i].setX(vecs[i].getX());
            clonedVecs[i].setY(vecs[i].getY());
            clonedVecs[i].setZ(vecs[i].getZ());
        }
        clonedTriangle.setVecs(clonedVecs);
        clonedTriangle.luminance = luminance;
        return clonedTriangle;
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

    public void setLuminance(double luminance) {
        this.luminance = luminance;
    }

    public double getLuminance() {
        return luminance;
    }

    @Override
    public String toString() {
        return "[" + vecs[0] + ", " + vecs[1] + ", " + vecs[2] + "]";
    }

}
