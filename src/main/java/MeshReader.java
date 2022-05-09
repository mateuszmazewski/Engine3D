import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MeshReader {

    public Mesh readMeshFromFile(String filename) {
        Mesh mesh = new Mesh();
        String line;
        String[] splittedLine;

        int linesCount = 0;
        int vec3dCount = 0;

        Vec3D vec3d;
        Vec3D[] triangleVecs = {null, null, null};
        Triangle triangle = new Triangle();

        File file = new File(filename);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            while ((line = reader.readLine()) != null) {
                linesCount++;
                if (line.startsWith("//") || line.trim().isEmpty()) {
                    continue;
                }

                splittedLine = line.split("\\s+|,\\s+");

                if (splittedLine.length == 4 && splittedLine[0].equals("rgb")) {
                    int r = Integer.parseInt(splittedLine[1]);
                    int g = Integer.parseInt(splittedLine[2]);
                    int b = Integer.parseInt(splittedLine[3]);
                    triangle.setRGB(r, g, b);
                    continue;
                }

                if (splittedLine.length != 3) { // Not a triangle
                    throw new IOException(filename + ", line " + linesCount + ": Not a triangle");
                }

                vec3d = new Vec3D();

                vec3d.setX(Double.parseDouble(splittedLine[0]));
                vec3d.setY(Double.parseDouble(splittedLine[1]));
                vec3d.setZ(Double.parseDouble(splittedLine[2]));

                triangleVecs[vec3dCount] = vec3d;
                vec3dCount++;

                if (vec3dCount == 3) {
                    triangle.setVecs(triangleVecs.clone());
                    mesh.addTriangle(triangle);
                    triangle = new Triangle();
                    vec3dCount = 0;
                }
            }

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
        return mesh;
    }

    public Mesh readFromObjFile(String filename) {
        Mesh mesh = new Mesh();
        String line;
        String[] splittedLine;

        int linesCount = 0;
        int[] f = new int[3];

        Vec3D vec3d;
        List<Vec3D> verts = new ArrayList<>();
        Vec3D[] triangleVecs = {null, null, null};

        File file = new File(filename);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            while ((line = reader.readLine()) != null) {
                linesCount++;
                if (line.startsWith("//") || line.trim().isEmpty()) {
                    continue;
                }

                splittedLine = line.split("\\s+");
                if (splittedLine.length != 4) {
                    throw new IOException(filename + ", line " + linesCount + ": Invalid data");
                }

                vec3d = new Vec3D();

                if (splittedLine[0].equals("v")) {
                    vec3d.setX(Double.parseDouble(splittedLine[1]));
                    vec3d.setY(Double.parseDouble(splittedLine[2]));
                    vec3d.setZ(Double.parseDouble(splittedLine[3]));

                    verts.add(vec3d);
                } else if (splittedLine[0].equals("f")) {
                    f[0] = Integer.parseInt(splittedLine[1]);
                    f[1] = Integer.parseInt(splittedLine[2]);
                    f[2] = Integer.parseInt(splittedLine[3]);
                    for (int i = 0; i < 3; i++) {
                        triangleVecs[i] = verts.get(f[i] - 1);
                    }
                    mesh.addTriangle(new Triangle(triangleVecs.clone()));
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
        return mesh;
    }
}
