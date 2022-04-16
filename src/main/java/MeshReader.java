import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MeshReader {

    public Mesh readMeshFromFile(String filename) {
        Mesh mesh = new Mesh();
        String line;
        String[] splittedLine;

        int linesCount = 0;
        int vec3dCount = 0;

        Vec3D vec3d;
        Vec3D[] triangleVecs = {null, null, null};
        int i = 0;
        double coordinate;

        File f = new File(filename);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));

            while ((line = reader.readLine()) != null) {
                linesCount++;
                if (line.startsWith("//") || line.trim().isEmpty()) {
                    continue;
                }

                splittedLine = line.split("\\s+|,\\s+");
                if (splittedLine.length != 3) { // Not a triangle
                    throw new IOException(filename + ", line " + linesCount + ": Not a triangle");
                }

                vec3d = new Vec3D();

                for (String s : splittedLine) {
                    coordinate = Double.parseDouble(s);
                    switch (i) {
                        case 0:
                            vec3d.setX(coordinate);
                            break;
                        case 1:
                            vec3d.setY(coordinate);
                            break;
                        case 2:
                            vec3d.setZ(coordinate);
                            break;
                        default:
                            throw new IOException("Too many coordinates for a triangle");
                    }
                    i++;
                }
                triangleVecs[vec3dCount] = vec3d;
                vec3dCount++;

                if (vec3dCount == 3) {
                    mesh.addTriangle(new Triangle(triangleVecs.clone()));
                    vec3dCount = 0;
                }

                i = 0;
            }

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
        return mesh;
    }
}
