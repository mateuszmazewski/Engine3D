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

    String meshFilename = "mesh.txt";
    String teapotFilename = "teapot.obj";

    private Vec3D cameraPosition = new Vec3D(0, 0, 0);
    private Vec3D lookDirection; // Unit vector that points the direction that camera is turned into
    private double cameraRotX = 0.0;
    private double yaw = 0.0; // Rotation (radians) in the Y axis
    private double cameraRotZ = 0.0;
    private double fov = 70.0;

    private final Map<String, Boolean> keysPressed;

    private final double cameraStep = 0.1;

    private final List<Triangle> projectedTriangles;
    private final List<Mesh> meshes;

    private boolean drawMesh = false;

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

        projectedTriangles = new ArrayList<>();
        meshes = new ArrayList<>();

        MeshReader meshReader = new MeshReader();
        Mesh cubes = meshReader.readMeshFromFile(meshFilename);
        Mesh teapot = meshReader.readFromObjFile(teapotFilename);
        //meshes.add(cubes);
        meshes.add(teapot);

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
            scanlineDraw(graphics);
            for (Triangle triangle : projectedTriangles) {
                //fillTriangle(graphics, triangle);
                if (drawMesh) {
                    graphics.setColor(Color.WHITE);
                    drawTriangle(graphics, triangle);
                }
            }
        }

        graphics.dispose();
        bs.show();
    }

    private void scanlineDraw(Graphics2D graphics) {
        List<Edge> edges = new ArrayList<>();
        List<Edge> activeEdges = new ArrayList<>();

        for (Triangle t : projectedTriangles) {
            Vec3D[] vecs = t.getVecs();
            edges.add(new Edge(t, vecs[0], vecs[1]));
            edges.add(new Edge(t, vecs[1], vecs[2]));
            edges.add(new Edge(t, vecs[2], vecs[0]));
        }

        for (int y = 0; y < HEIGHT; y++) {
            activeEdges.clear();

            for (Edge e : edges) {
                if (e.xIntersection(y) != null) {
                    activeEdges.add(e);
                }
            }

            activeEdges.sort((Edge e1, Edge e2) -> {
                // Sort by x of crossing point
                double a1 = (e1.p2.getY() - e1.p1.getY()) / (e1.p2.getX() - e1.p1.getX());
                double a2 = (e2.p2.getY() - e2.p1.getY()) / (e2.p2.getX() - e2.p1.getX());
                double b1 = e1.p1.getY() - a1 * e1.p1.getX();
                double b2 = e2.p1.getY() - a2 * e2.p1.getX();
                double xCross1 = (e1.p1.getY() - b1) / a1;
                double xCross2 = (e2.p1.getY() - b2) / a2;
                return Double.compare(xCross1, xCross2);
            });

            // Start from the beginning of each line
            int x = 0;
            List<Triangle> activeTriangles = new ArrayList<>();

            for (Edge ae : activeEdges) {
                int xIntersection = ae.xIntersection(y);

                if (activeTriangles.size() == 0) {
                    graphics.setColor(Color.BLACK);
                } else if (activeTriangles.size() == 1) {
                    int lum = (int) (255 * activeTriangles.get(0).getLuminance());
                    graphics.setColor(new Color(lum, lum, lum));
                } else {
                    Triangle closestTriangle = activeTriangles.get(0);
                    Vec3D[] ctVecs = closestTriangle.getVecs();
                    double zClosest = (ctVecs[0].getZ() + ctVecs[1].getZ() + ctVecs[2].getZ()) / 3.0;
                    for(Triangle t : activeTriangles) {
                        int xMid = (x + xIntersection) / 2;
                        // TODO: Calculate z for x = xMid
                        Vec3D[] vecs = t.getVecs();
                        double z = (vecs[0].getZ() + vecs[1].getZ() + vecs[2].getZ()) / 3.0;
                        if(z < zClosest) {
                            closestTriangle = t;
                            zClosest = z;
                        }
                    }

                    int lum = (int) (255 * closestTriangle.getLuminance());
                    graphics.setColor(new Color(lum, lum, lum));
                }

                graphics.drawLine(x, y, xIntersection, y);
                x = xIntersection; // TODO: xIntersection + 1 ?
                if (!activeTriangles.contains(ae.getTriangle())) {
                    // Going inside the triangle
                    activeTriangles.add(ae.getTriangle());
                } else {
                    // Going outside the triangle
                    activeTriangles.remove(ae.getTriangle());
                }
            }

            // Draw the rest of the line
            graphics.setColor(Color.BLACK);
            graphics.drawLine(x, y, WIDTH, y);
        }
    }

    public void update() {
        double angle = 0.0;//System.currentTimeMillis() / 1000.0;
        Matrix matrixRotX = Matrix.makeRotationX(angle);
        Matrix matrixRotZ = Matrix.makeRotationZ(angle);

        Matrix matrixTranslation = Matrix.makeTranslation(0.0, 0.0, 3.0); // Optionally move whole scene

        Matrix worldMatrix = Matrix.mult(matrixRotZ, matrixRotX);
        worldMatrix = Matrix.mult(worldMatrix, matrixTranslation);

        Vec3D upVec = new Vec3D(0, 1, 0);

        // Target point that camera should look at
        Vec3D targetVec = new Vec3D(0, 0, 1);

        Matrix cameraRot;
        Matrix cameraRotXMatrix = Matrix.makeRotationX(-cameraRotX / 100);
        Matrix cameraRotYMatrix = Matrix.makeRotationY(-yaw / 100);
        Matrix cameraRotZMatrix = Matrix.makeRotationZ(-cameraRotZ / 100);

        cameraRot = Matrix.mult(cameraRotXMatrix, cameraRotYMatrix);

        // Unit vector rotated in Y axis by yaw radians around (0, 0, 0)
        lookDirection = Vec3D.multMatrixVector(cameraRot, targetVec);
        upVec = Vec3D.multMatrixVector(cameraRotZMatrix, upVec);

        targetVec = Vec3D.add(cameraPosition, lookDirection);

        Matrix cameraMatrix = Matrix.makePointAtMatrix(cameraPosition, targetVec, upVec);
        Matrix viewMatrix = Matrix.quickInverse(cameraMatrix);

        Matrix projectionMatrix = Matrix.makeProjection(fov, (double) HEIGHT / WIDTH, 0.1, 1000);

        Triangle transformedTriangle, projectedTriangle, viewedTriangle;
        Vec3D[] vecs;

        projectedTriangles.clear();
        for (Mesh mesh : meshes) {
            for (Triangle t : mesh.getTriangles()) {
                transformedTriangle = t.clone();
                vecs = transformedTriangle.getVecs();

                // Rotate Z, rotate X (optional deformation), move further from the camera
                // Convert from object space to world space
                for (int i = 0; i < 3; i++) {
                    vecs[i] = Vec3D.multMatrixVector(worldMatrix, vecs[i]);
                }

                // Check if it's a rear wall
                Vec3D normal, line1, line2;
                line1 = Vec3D.subtract(vecs[1], vecs[0]);
                line2 = Vec3D.subtract(vecs[2], vecs[0]);
                normal = Vec3D.crossProduct(line1, line2);
                normal = Vec3D.normalise(normal);

                Vec3D cameraRay = Vec3D.subtract(vecs[0], cameraPosition);

                // How much of the normal projects onto a ray cast from camera to the triangle
                if (Vec3D.dotProduct(normal, cameraRay) > 0.0) {
                    // Rear wall -> invisible
                    continue;
                }

                // Illumination
                Vec3D lightDirection = new Vec3D(0, 0, -1);
                lightDirection = Vec3D.normalise(lightDirection);
                double dotProduct = Vec3D.dotProduct(normal, lightDirection);
                if (dotProduct > 0.0) {
                    transformedTriangle.setLuminance(dotProduct);
                }

                // Convert from world space to view space
                viewedTriangle = transformedTriangle.clone();
                vecs = viewedTriangle.getVecs();
                for (int i = 0; i < 3; i++) {
                    vecs[i] = Vec3D.multMatrixVector(viewMatrix, vecs[i]);
                }

                projectedTriangle = viewedTriangle.clone();
                vecs = projectedTriangle.getVecs();
                int invisibleVecs = 0;
                for (int i = 0; i < 3; i++) {
                    // Project from 3D to 2D
                    // Convert from world space to screen space
                    vecs[i] = Vec3D.multMatrixVector(projectionMatrix, vecs[i]);

                    // Normalise
                    if (vecs[i].getW() > Util.EPS) {
                        vecs[i] = Vec3D.divide(vecs[i], vecs[i].getW());
                    }

                    // Partial clipping -- count how many verts of a triangle are invisible
                    if (Math.abs(vecs[i].getX()) > 1.0 || Math.abs(vecs[i].getY()) > 1.0 || Math.abs(vecs[i].getZ()) > 1) {
                        invisibleVecs++;
                    }

                    // Invert X and Y (in SWING y axis is pointing down by default)
                    //vecs[i].setX(-vecs[i].getX());
                    vecs[i].setY(-vecs[i].getY());

                    // Offset from range [-1, 1] to range [0, 2]
                    Vec3D offset = new Vec3D(1, 1, 0);
                    vecs[i] = Vec3D.add(vecs[i], offset);

                    // Scale x, y to screen size
                    vecs[i].setX(vecs[i].getX() * 0.5 * WIDTH);
                    vecs[i].setY(vecs[i].getY() * 0.5 * HEIGHT);
                }
                // Partial clipping -- remove only when all 3 verts are invisible
                if (invisibleVecs < 3) {
                    projectedTriangles.add(projectedTriangle);
                }
            }
            //System.out.println("Aktualnie wyświetlanych trójkątów: " + projectedTriangles.size());

            // Draw triangles from back to front (painter's algorithm)
            projectedTriangles.sort((Triangle t1, Triangle t2) -> {
                Vec3D[] vecs1 = t1.getVecs();
                Vec3D[] vecs2 = t2.getVecs();
                double z1 = (vecs1[0].getZ() + vecs1[1].getZ() + vecs1[2].getZ()) / 3.0;
                double z2 = (vecs2[0].getZ() + vecs2[1].getZ() + vecs2[2].getZ()) / 3.0;
                return Double.compare(z2, z1);
            });

            Vec3D forwardVec = Vec3D.mult(lookDirection, cameraStep); // Velocity vector forward
            Vec3D rightVec = Vec3D.crossProduct(upVec, forwardVec);
            upVec = Vec3D.normalise(upVec);
            upVec = Vec3D.mult(upVec, cameraStep);
            rightVec = Vec3D.normalise(rightVec);
            rightVec = Vec3D.mult(rightVec, cameraStep); // Velocity vector right

            if (keysPressed.get("space")) {
                cameraPosition = Vec3D.add(cameraPosition, upVec);
            }
            if (keysPressed.get("shift")) {
                cameraPosition = Vec3D.subtract(cameraPosition, upVec);
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
    }

    private void drawTriangle(Graphics g, Triangle triangle) {
        Vec3D[] vecs = triangle.getVecs();
        g.drawLine((int) vecs[0].getX(), (int) vecs[0].getY(), (int) vecs[1].getX(), (int) vecs[1].getY());
        g.drawLine((int) vecs[1].getX(), (int) vecs[1].getY(), (int) vecs[2].getX(), (int) vecs[2].getY());
        g.drawLine((int) vecs[2].getX(), (int) vecs[2].getY(), (int) vecs[0].getX(), (int) vecs[0].getY());
    }

    private void fillTriangle(Graphics g, Triangle triangle) {
        Vec3D[] vecs = triangle.getVecs();
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];

        for (int i = 0; i < 3; i++) {
            xPoints[i] = (int) vecs[i].getX();
            yPoints[i] = (int) vecs[i].getY();
        }

        // Create a polygon representing the triangle
        Polygon p = new Polygon(xPoints, yPoints, 3);

        int lum = (int) (255 * triangle.getLuminance());
        g.setColor(new Color(lum, lum, lum));
        g.fillPolygon(p);
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

                    case KeyEvent.VK_M:
                        drawMesh = !drawMesh;
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
