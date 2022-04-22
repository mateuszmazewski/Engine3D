import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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

    private Vec3D cameraPosition;
    private Vec3D lookDirection; // Unit vector that points the direction that camera is turned into
    private double cameraRotX;
    private double yaw; // Rotation (radians) in the Y axis
    private double cameraRotZ;

    private double cameraStep = 0.1;

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

        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                Vec3D forwardVec = Vec3D.mult(lookDirection, cameraStep); // Velocity vector

                switch (keyCode) {
                    case KeyEvent.VK_SPACE:
                        cameraPosition.setY(cameraPosition.getY() + cameraStep);
                        break;
                    case KeyEvent.VK_SHIFT:
                        cameraPosition.setY(cameraPosition.getY() - cameraStep);
                        break;
                    case KeyEvent.VK_D:
                        cameraPosition.setX(cameraPosition.getX() + cameraStep);
                        break;
                    case KeyEvent.VK_A:
                        cameraPosition.setX(cameraPosition.getX() - cameraStep);
                        break;
                    case KeyEvent.VK_W:
                        cameraPosition = Vec3D.add(cameraPosition, forwardVec);
                        break;
                    case KeyEvent.VK_S:
                        cameraPosition = Vec3D.subtract(cameraPosition, forwardVec);
                        break;

                    case KeyEvent.VK_LEFT:
                        yaw -= 1.0;
                        break;
                    case KeyEvent.VK_RIGHT:
                        yaw += 1.0;
                        break;
                    case KeyEvent.VK_DOWN:
                        cameraRotX -= 1.0;
                        break;
                    case KeyEvent.VK_UP:
                        cameraRotX += 1.0;
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        mesh = new MeshReader().readMeshFromFile(meshFilename);
        System.out.println(mesh.toString());

        projectionMatrix = Matrix.makeProjection(70.0, (double) HEIGHT / WIDTH, 0.1, 1000);
        projectedTriangles = new ArrayList<>();

        cameraPosition = new Vec3D(0, 0, 0);
        cameraRotX = 0.0;
        yaw = 0.0;
        cameraRotZ = 0.0;

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
                frame.setTitle(title + " | " + frames + " FPS | " + "Camera pos.: " + cameraPosition + " | Look dir.: " + lookDirection);
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

        Graphics2D graphics = (Graphics2D) bs.getDrawGraphics();

        // By default in SWING (0,0) is in the top left corner
        // Inverse the y axis and put (0,0) in bottom left corner
        /*
        int m = HEIGHT / 2;
        graphics.translate(0, m);
        graphics.scale(1, -1);
        graphics.translate(0, -m);
        */

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
        double angle = 0;//System.currentTimeMillis() / 1000.0;
        Matrix matrixRotX = Matrix.makeRotationX(angle);
        Matrix matrixRotZ = Matrix.makeRotationZ(angle);
        Matrix matrixTranslation = Matrix.makeTranslation(0.0, 0.0, 3.0);
        Matrix matrixWorld = Matrix.mult(matrixRotZ, matrixRotX);
        matrixWorld = Matrix.mult(matrixWorld, matrixTranslation);

        Vec3D upVec = new Vec3D(0, 1, 0);

        // Target point that camera should look at
        Vec3D targetVec = new Vec3D(0, 0, 1);
        Matrix cameraRotXMatrix = Matrix.makeRotationX(-cameraRotX / 100);
        Matrix cameraRotYMatrix = Matrix.makeRotationY(-yaw / 100);

        // Unit vector rotated in Y axis by yaw radians around (0, 0, 0)
        lookDirection = Vec3D.multMatrixVector(cameraRotXMatrix, targetVec);
        lookDirection = Vec3D.multMatrixVector(cameraRotYMatrix, lookDirection);

        targetVec = Vec3D.add(cameraPosition, lookDirection);

        Matrix cameraMatrix = Matrix.makePointAtMatrix(cameraPosition, targetVec, upVec);
        Matrix viewMatrix = Matrix.quickInverse(cameraMatrix);

        Triangle transformedTriangle, projectedTriangle;
        Triangle viewedTriangle;
        Vec3D[] vecs;

        // TODO - nie trzeba klonować trójkątów bo funkcje zwracają i tak nowe wektory
        projectedTriangles.clear();
        for (Triangle t : mesh.getTriangles()) {
            transformedTriangle = t.clone();
            vecs = transformedTriangle.getVecs();

            // Rotate Z, rotate X, translate
            for (int i = 0; i < 3; i++) {
                vecs[i] = Vec3D.multMatrixVector(matrixWorld, vecs[i]);
            }

            // Convert from world space to view space
            viewedTriangle = transformedTriangle.clone();
            vecs = viewedTriangle.getVecs();
            for (int i = 0; i < 3; i++) {
                vecs[i] = Vec3D.multMatrixVector(viewMatrix, vecs[i]);
            }

            projectedTriangle = viewedTriangle.clone();
            vecs = projectedTriangle.getVecs();
            for (int i = 0; i < 3; i++) {
                // Project from 3D to 2D
                vecs[i] = Vec3D.multMatrixVector(projectionMatrix, vecs[i]);

                // Normalise
                if (vecs[i].getW() > Util.EPS) {
                    vecs[i] = Vec3D.divide(vecs[i], vecs[i].getW());
                }

                // Invert X and Y (in SWING y axis is pointing down by default)
                vecs[i].setX(-vecs[i].getX());
                //vecs[i].setY(-vecs[i].getY());

                // Offset (0, 0) from bottom left corner to center of the screen
                Vec3D offset = new Vec3D(1, 1, 0);
                vecs[i] = Vec3D.add(vecs[i], offset);

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
