import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

public class Display extends Canvas implements Runnable {
    private static final int FRAMES_PER_SECOND = 60;


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

        projectionMatrix = Matrix.makeProjection(90.0, (double) HEIGHT / WIDTH, 0.1, 1000);
        projectedTriangles = new ArrayList<>();

        start();
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
        double angle = System.currentTimeMillis() / 1000.0;
        Matrix matrixRotX = Matrix.makeRotationX(angle);
        Matrix matrixRotZ = Matrix.makeRotationZ(angle);
        Matrix matrixTranslation = Matrix.makeTranslation(0.0, 0.0, 3.0);
        Matrix matrixWorld = Matrix.mult(matrixRotZ, matrixRotX);
        matrixWorld = Matrix.mult(matrixWorld, matrixTranslation);

        Triangle transformedTriangle;
        Vec3D[] vecs;

        projectedTriangles.clear();
        for (Triangle t : mesh.getTriangles()) {
            transformedTriangle = t.clone();
            vecs = transformedTriangle.getVecs();

            // Rotate Z, rotate X, translate
            for (int i = 0; i < 3; i++) {
                vecs[i] = Vec3D.multVectorMatrix(vecs[i], matrixWorld);
            }

            for (int i = 0; i < 3; i++) {
                // Project from 3D to 2D
                vecs[i] = Vec3D.multVectorMatrix(vecs[i], projectionMatrix);

                // Normalise vector (divide projected x, y, z by z)
                vecs[i] = Vec3D.divide(vecs[i], vecs[i].getW());

                // Offset x, y range from [-1, 1] to [0, 2] (to visible normalised space)
                Vec3D offset = new Vec3D(1, 1, 0);
                vecs[i] = Vec3D.add(vecs[i], offset);

                // Scale x, y to screen size
                // Offset screen space to the center of the screen
                // i.e. (0,0) is in the center of the screen
                vecs[i].setX(vecs[i].getX() * 0.5 * WIDTH);
                vecs[i].setY(vecs[i].getY() * 0.5 * HEIGHT);
            }
            projectedTriangles.add(transformedTriangle);
        }
    }

    private void drawTriangle(Graphics g, Triangle triangle) {
        Vec3D[] vecs = triangle.getVecs();
        g.drawLine((int) vecs[0].getX(), (int) vecs[0].getY(), (int) vecs[1].getX(), (int) vecs[1].getY());
        g.drawLine((int) vecs[1].getX(), (int) vecs[1].getY(), (int) vecs[2].getX(), (int) vecs[2].getY());
        g.drawLine((int) vecs[2].getX(), (int) vecs[2].getY(), (int) vecs[0].getX(), (int) vecs[0].getY());
    }
}
