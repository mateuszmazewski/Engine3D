import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

public class Display extends Canvas implements Runnable {
    private static final int FRAMES_PER_SECOND = 60;
    public static double EPS = 10e-8;

    private Thread thread;
    private final JFrame frame;
    private final String title = "Engine 3D";

    private final static int WIDTH = 800;
    private final static int HEIGHT = 600;
    private static boolean running = false;

    private final Mesh mesh;
    String meshFilename = "cube.txt";

    private Matrix projectionMatrix;

    private List<Triangle> projectedTriangles;

    public Display() {
        frame = new JFrame(title);
        Dimension dimension = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(dimension);
        frame.add(this);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        mesh = new MeshReader().readMeshFromFile(meshFilename);
        System.out.println(mesh.toString());

        projectionMatrix = initProjectionMatrix();
        projectedTriangles = new ArrayList<>();

        start();
    }

    private Matrix initProjectionMatrix() {
        Matrix pm = new Matrix(4, 4);
        double zNear = 0.1;
        double zFar = 1000.0;
        double fovDegrees = 90.0;
        double fovRad = fovDegrees / (2 * Math.PI);
        double fovCoefficient = 1.0 / Math.tan(0.5 * fovRad);
        double aspectRatio = (double) HEIGHT / WIDTH;

        pm.fill(0.0);
        pm.set(0, 0, aspectRatio * fovCoefficient);
        pm.set(1, 1, fovCoefficient);
        pm.set(2, 2, zFar / (zFar - zNear));
        pm.set(3, 2, (-zFar * zNear) / (zFar - zNear));
        pm.set(2, 3, 1.0);

        return pm;
    }

    private Vec3D mult(Vec3D vec, Matrix matrix) {
        Matrix vecAsMatrix = new Matrix(1, 3);
        vecAsMatrix.set(0, 0, vec.getX());
        vecAsMatrix.set(0, 1, vec.getY());
        vecAsMatrix.set(0, 2, vec.getZ());

        Matrix result = Matrix.mult(vecAsMatrix, matrix);
        double z = result.get(0, 3);
        // Divide x and y by z (further objects are smaller)
        if (z > EPS) { // Not division by 0
            for (int c = 0; c < 3; c++) {
                result.set(0, c, result.get(0, c) / z);
            }
        }

        return new Vec3D(result.get(0, 0), result.get(0, 1), result.get(0, 2));
    }

    public synchronized void start() {
        running = true;
        thread = new Thread(this, "display");
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        long timer = System.currentTimeMillis();
        final double frameTime = 1000000000.0 / FRAMES_PER_SECOND;
        double delta = 0; // when reaches 1 -> update the frame
        long frames = 0;
        long now;

        while (running) {
            now = System.nanoTime();
            delta += (now - lastTime) / frameTime;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            render();
            frames++;

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                frame.setTitle(title + " | " + frames + " FPS");
                frames = 0;
            }
        }

        stop();
    }

    public void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }

        Graphics graphics = bs.getDrawGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        graphics.setColor(Color.WHITE);
        if (!projectedTriangles.isEmpty()) {
            for (Triangle triangle : projectedTriangles) {
                drawTriangle(graphics, triangle);
            }
        }

        graphics.dispose();
        bs.show();
    }

    public void update() {
        Matrix matrixRotX = new Matrix(4, 4);
        Matrix matrixRotZ = new Matrix(4, 4);
        double angle = System.currentTimeMillis() / 3000.0;

        matrixRotX.fill(0.0);
        matrixRotZ.fill(0.0);

        matrixRotX.set(0, 0, 1.0);
        matrixRotX.set(1, 1, Math.cos(angle * 0.5));
        matrixRotX.set(1, 2, Math.sin(angle * 0.5));
        matrixRotX.set(2, 1, -Math.sin(angle * 0.5));
        matrixRotX.set(2, 2, Math.cos(angle * 0.5));
        matrixRotX.set(3, 3, 1.0);

        matrixRotZ.set(0, 0, Math.cos(angle));
        matrixRotZ.set(0, 1, Math.sin(angle));
        matrixRotZ.set(1, 0, -Math.sin(angle));
        matrixRotZ.set(1, 1, Math.cos(angle));
        matrixRotZ.set(2, 2, 1.0);
        matrixRotZ.set(3, 3, 1.0);

        Triangle translatedTriangle;
        Triangle projectedTriangle;
        Triangle triangleRotatedZX;
        Vec3D[] vecs;

        projectedTriangles.clear();
        for (Triangle t : mesh.getTriangles()) {
            triangleRotatedZX = t.clone();
            vecs = triangleRotatedZX.getVecs();
            for (int i = 0; i < 3; i++) {
                vecs[i] = mult(vecs[i], matrixRotZ);
                vecs[i] = mult(vecs[i], matrixRotX);
            }

            translatedTriangle = triangleRotatedZX;
            for (Vec3D vec : translatedTriangle.getVecs()) {
                vec.setZ(vec.getZ() + 3.0);
            }

            projectedTriangle = translatedTriangle;
            vecs = projectedTriangle.getVecs();
            for (int i = 0; i < 3; i++) {
                vecs[i] = mult(vecs[i], projectionMatrix);
                // Scale x, y range from [-1, 1] to [0, 2]
                vecs[i].setX(vecs[i].getX() + 1.0);
                vecs[i].setY(vecs[i].getY() + 1.0);

                // Scale x, y to screen size
                vecs[i].setX(vecs[i].getX() * 0.5 * WIDTH);
                vecs[i].setY(vecs[i].getY() * 0.5 * HEIGHT);
            }
            projectedTriangles.add(projectedTriangle);
        }
    }

    private void drawTriangle(Graphics g, Triangle triangle) {
        Vec3D[] vecs = triangle.getVecs();
        g.drawLine((int) vecs[0].getX(), (int) vecs[0].getY(), (int) vecs[1].getX(), (int) vecs[1].getY());
        g.drawLine((int) vecs[1].getX(), (int) vecs[1].getY(), (int) vecs[2].getX(), (int) vecs[2].getY());
        g.drawLine((int) vecs[2].getX(), (int) vecs[2].getY(), (int) vecs[0].getX(), (int) vecs[0].getY());
    }
}
