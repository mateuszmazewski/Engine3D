import java.util.ArrayList;
import java.util.List;

public class Mesh {
    private List<Triangle> triangles;

    public Mesh() {
        triangles = new ArrayList<>();
    }

    public void addTriangle(Triangle triangle) {
        triangles.add(triangle);
    }

    public List<Triangle> getTriangles() {
        return triangles;
    }

    public void setTriangles(List<Triangle> triangles) {
        this.triangles = triangles;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Triangle t : triangles) {
            sb.append(t.toString()).append("\n");
        }
        return sb.toString();
    }

}
