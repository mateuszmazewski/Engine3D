import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Vec3D cameraPosition = new Vec3D(0, 0, 0);
    private Vec3D lookDirection; // Unit vector that points the direction that camera is turned into
    private double cameraRotX = 0.0;
    private double yaw = 0.0; // Rotation (radians) in the Y axis
    private double cameraRotZ = 0.0;
    private double fov = 70.0;

    private final Map<String, Boolean> keysPressed;

    private final double cameraStep = 0.1;

    private Matrix projectionMatrix;

    private final List<Triangle> projectedTriangles;

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

        frame.addKeyListener(createKeyListener());

        mesh = new MeshReader().readMeshFromFile(meshFilename);
        System.out.println(mesh.toString());

        projectedTriangles = new ArrayList<>();

        keysPressed = new HashMap<>();
        keysPressed.put("w", false);
        keysPressed.put("s", false);
        keysPressed.put("a", false);
        keysPressed.put("d", false);
        keysPressed.put("space", false);
        keysPressed.put("shift", false);
        keysPressed.put("up", false);
        keysPressed.put("down", false);
        keysPressed.put("left", false);
        keysPressed.put("right", false);
        keysPressed.put("q", false);
        keysPressed.put("e", false);
        keysPressed.put("r", false);
        keysPressed.put("f", false);

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
                frame.setTitle(title + " | " + frames + " FPS | " + "Camera pos.: " + cameraPosition + " | Look dir.: " + lookDirection + " | FOV: " + Util.round(fov, 2));
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
        Matrix cameraRotZMatrix = Matrix.makeRotationZ(-cameraRotZ / 100);

        // Unit vector rotated in Y axis by yaw radians around (0, 0, 0)
        lookDirection = Vec3D.multMatrixVector(cameraRotXMatrix, targetVec);
        lookDirection = Vec3D.multMatrixVector(cameraRotYMatrix, lookDirection);
        upVec = Vec3D.multMatrixVector(cameraRotZMatrix, upVec);

        targetVec = Vec3D.add(cameraPosition, lookDirection);

        Matrix cameraMatrix = Matrix.makePointAtMatrix(cameraPosition, targetVec, upVec);
        Matrix viewMatrix = Matrix.quickInverse(cameraMatrix);

        projectionMatrix = Matrix.makeProjection(fov, (double) HEIGHT / WIDTH, 0.1, 1000);

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

        Vec3D forwardVec = Vec3D.mult(lookDirection, cameraStep); // Velocity vector forward
        Vec3D rightVec = Vec3D.crossProduct(upVec, forwardVec);
        rightVec = Vec3D.normalise(rightVec);
        rightVec = Vec3D.mult(rightVec, cameraStep); // Velocity vector right

        if (keysPressed.get("space")) {
            cameraPosition.setY(cameraPosition.getY() + cameraStep);
        }
        if (keysPressed.get("shift")) {
            cameraPosition.setY(cameraPosition.getY() - cameraStep);
        }
        if ((keysPressed.get("d"))) {
            cameraPosition = Vec3D.add(cameraPosition, rightVec);
        }
        if (keysPressed.get("a")) {
            cameraPosition = Vec3D.subtract(cameraPosition, rightVec);
        }
        if (keysPressed.get("w")) {
            cameraPosition = Vec3D.add(cameraPosition, forwardVec);
        }
        if (keysPressed.get("s")) {
            cameraPosition = Vec3D.subtract(cameraPosition, forwardVec);
        }

        if (keysPressed.get("left")) {
            yaw -= 1.0;
        }
        if (keysPressed.get("right")) {
            yaw += 1.0;
        }
        if (keysPressed.get("down")) {
            cameraRotX -= 1.0;
        }
        if (keysPressed.get("up")) {
            cameraRotX += 1.0;
        }
        if (keysPressed.get("q")) {
            cameraRotZ -= 1.0;
        }
        if (keysPressed.get("e")) {
            cameraRotZ += 1.0;
        }
        if (keysPressed.get("r")) {
            if (fov < 179.0) {
                fov += 1.0;
            }
        }
        if (keysPressed.get("f")) {
            if (fov > 1.0) {
                fov -= 1.0;
            }
        }
    }

    private void drawTriangle(Graphics g, Triangle triangle) {
        Vec3D[] vecs = triangle.getVecs();
        g.drawLine((int) vecs[0].getX(), (int) vecs[0].getY(), (int) vecs[1].getX(), (int) vecs[1].getY());
        g.drawLine((int) vecs[1].getX(), (int) vecs[1].getY(), (int) vecs[2].getX(), (int) vecs[2].getY());
        g.drawLine((int) vecs[2].getX(), (int) vecs[2].getY(), (int) vecs[0].getX(), (int) vecs[0].getY());
    }

    private KeyListener createKeyListener() {
        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                switch (keyCode) {
                    case KeyEvent.VK_SPACE:
                        keysPressed.put("space", true);
                        break;
                    case KeyEvent.VK_SHIFT:
                        keysPressed.put("shift", true);
                        break;
                    case KeyEvent.VK_D:
                        keysPressed.put("d", true);
                        break;
                    case KeyEvent.VK_A:
                        keysPressed.put("a", true);
                        break;
                    case KeyEvent.VK_W:
                        keysPressed.put("w", true);
                        break;
                    case KeyEvent.VK_S:
                        keysPressed.put("s", true);
                        break;

                    case KeyEvent.VK_LEFT:
                        keysPressed.put("left", true);
                        break;
                    case KeyEvent.VK_RIGHT:
                        keysPressed.put("right", true);
                        break;
                    case KeyEvent.VK_DOWN:
                        keysPressed.put("down", true);
                        break;
                    case KeyEvent.VK_UP:
                        keysPressed.put("up", true);
                        break;
                    case KeyEvent.VK_Q:
                        keysPressed.put("q", true);
                        break;
                    case KeyEvent.VK_E:
                        keysPressed.put("e", true);
                        break;
                    case KeyEvent.VK_R:
                        keysPressed.put("r", true);
                        break;
                    case KeyEvent.VK_F:
                        keysPressed.put("f", true);
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();

                switch (keyCode) {
                    case KeyEvent.VK_SPACE:
                        keysPressed.put("space", false);
                        break;
                    case KeyEvent.VK_SHIFT:
                        keysPressed.put("shift", false);
                        break;
                    case KeyEvent.VK_D:
                        keysPressed.put("d", false);
                        break;
                    case KeyEvent.VK_A:
                        keysPressed.put("a", false);
                        break;
                    case KeyEvent.VK_W:
                        keysPressed.put("w", false);
                        break;
                    case KeyEvent.VK_S:
                        keysPressed.put("s", false);
                        break;

                    case KeyEvent.VK_LEFT:
                        keysPressed.put("left", false);
                        break;
                    case KeyEvent.VK_RIGHT:
                        keysPressed.put("right", false);
                        break;
                    case KeyEvent.VK_DOWN:
                        keysPressed.put("down", false);
                        break;
                    case KeyEvent.VK_UP:
                        keysPressed.put("up", false);
                        break;
                    case KeyEvent.VK_Q:
                        keysPressed.put("q", false);
                        break;
                    case KeyEvent.VK_E:
                        keysPressed.put("e", false);
                        break;
                    case KeyEvent.VK_R:
                        keysPressed.put("r", false);
                        break;
                    case KeyEvent.VK_F:
                        keysPressed.put("f", false);
                        break;
                }
            }
        };
    }
}
