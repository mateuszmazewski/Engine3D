import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.util.*;
import java.util.List;

import static java.lang.Math.abs;

public class Display extends Canvas implements Runnable {
    private static final int FRAMES_PER_SECOND = 60;
    private long currentFps = 0;


    private Thread thread;
    private final JFrame frame;
    private final String title = "Engine 3D";

    private final static int WIDTH = 800;
    private final static int HEIGHT = 600;
    private static boolean running = false;

    String cubesMeshFilename = "cubes.txt";
    String trianglesMeshFilename = "triangles.txt";
    String teapotMeshFilename = "teapot.obj";
    String cowMeshFilename = "cow.obj";
    String catMeshFilename = "cat.obj";
    String spotMeshFilename = "spot.obj";
    String sphereMeshFilename = "sphere.obj";
    String simpleSphereMeshFilename = "simpleSphere.obj";

    private Vec3D cameraPosition = new Vec3D(0, 0, 0);
    private Vec3D lookDirection; // Unit vector that points the direction that camera is turned into
    private double cameraRotX = 0.0;
    private double yaw = 0.0; // Rotation (radians) in the Y axis
    private double cameraRotZ = 0.0;
    private double fov = 70.0;

    private final Map<Integer, Boolean> keysPressed;

    private final double cameraStep = 0.1;

    Mesh cubes;
    Mesh triangles;
    Mesh teapot;
    Mesh cow;
    Mesh cat;
    Mesh spot;
    Mesh sphere;
    Mesh simpleSphere;

    private final List<Triangle> projectedTriangles;
    private final List<Mesh> currentMeshes;
    private final List<Mesh> allMeshes;
    private int meshId = 0;

    private boolean drawMesh = false;
    private boolean scanlineProof = false;
    private String drawingMethod = "alg. skaningowy";
    private boolean rotXactive = false;
    private boolean rotYactive = false;
    private boolean rotZactive = false;

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
        currentMeshes = new ArrayList<>();
        allMeshes = new ArrayList<>();

        MeshReader meshReader = new MeshReader();
        cubes = meshReader.readMeshFromFile(cubesMeshFilename);
        triangles = meshReader.readMeshFromFile(trianglesMeshFilename);
        teapot = meshReader.readFromObjFile(teapotMeshFilename);
        cow = meshReader.readFromObjFile(cowMeshFilename);
        cat = meshReader.readFromObjFile(catMeshFilename);
        spot = meshReader.readFromObjFile(spotMeshFilename);
        sphere = meshReader.readFromObjFile(sphereMeshFilename);
        simpleSphere = meshReader.readFromObjFile(simpleSphereMeshFilename);

        allMeshes.add(cubes);
        allMeshes.add(triangles);
        allMeshes.add(teapot);
        allMeshes.add(cow);
        allMeshes.add(cat);
        allMeshes.add(spot);
        allMeshes.add(sphere);
        allMeshes.add(simpleSphere);
        currentMeshes.add(teapot);

        keysPressed = new HashMap<>();
        keysPressed.put(KeyEvent.VK_W, false);
        keysPressed.put(KeyEvent.VK_S, false);
        keysPressed.put(KeyEvent.VK_A, false);
        keysPressed.put(KeyEvent.VK_D, false);
        keysPressed.put(KeyEvent.VK_SPACE, false);
        keysPressed.put(KeyEvent.VK_SHIFT, false);
        keysPressed.put(KeyEvent.VK_UP, false);
        keysPressed.put(KeyEvent.VK_DOWN, false);
        keysPressed.put(KeyEvent.VK_LEFT, false);
        keysPressed.put(KeyEvent.VK_RIGHT, false);
        keysPressed.put(KeyEvent.VK_Q, false);
        keysPressed.put(KeyEvent.VK_E, false);
        keysPressed.put(KeyEvent.VK_R, false);
        keysPressed.put(KeyEvent.VK_F, false);
        keysPressed.put(KeyEvent.VK_1, false);
        keysPressed.put(KeyEvent.VK_2, false);
        keysPressed.put(KeyEvent.VK_TAB, false);
        keysPressed.put(KeyEvent.VK_X, false);
        keysPressed.put(KeyEvent.VK_Y, false);
        keysPressed.put(KeyEvent.VK_Z, false);

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
                currentFps = frames;
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
            if (drawingMethod.equals("alg. skaningowy")) {
                scanlineDraw(graphics);
            }
            for (Triangle triangle : projectedTriangles) {
                if (drawingMethod.equals("alg. malarski")) {
                    fillTriangle(graphics, triangle);
                }
                if (drawMesh) {
                    graphics.setColor(Color.WHITE);
                    drawTriangle(graphics, triangle);
                }
            }
        }

        graphics.setColor(Color.WHITE);
        graphics.drawString("metoda rysowania: " + drawingMethod, 5, 20);
        /*
        graphics.drawString("FPS: " + currentFps, 5, 40);
        graphics.drawString("Camera pos.: " + cameraPosition, 5, 60);
        graphics.drawString("Look dir.: " + lookDirection, 5, 80);
        graphics.drawString("FOV: " + Util.round(fov, 2), 5, 100);
        */
        graphics.dispose();
        bs.show();
    }

    private void scanlineDraw(Graphics2D graphics) {
        List<Edge> edges = new ArrayList<>();
        List<Edge> activeEdges = new ArrayList<>();

        /*
        for (Triangle t : projectedTriangles) {
            for (Vec3D v : t.getVecs()) {
                v.setLum(t.getLuminance());
            }
        }
        */

        // TODO - poprawić <Vec3D, Vec3D>
        Map<Vec3D, Vec3D> uniqueVecs = new HashMap<>();
        Map<Vec3D, Integer> vecsCount = new HashMap<>();
        for (Triangle t : projectedTriangles) {
            for (Vec3D vec : t.getVecs()) {
                if (uniqueVecs.containsKey(vec)) {
                    Vec3D v = uniqueVecs.get(vec);
                    int count = vecsCount.get(v);
                    v.setLum((v.getLum() * count + vec.getLum()) / (count + 1));
                } else {
                    uniqueVecs.put(vec, vec);
                }

                if (vecsCount.containsKey(vec)) {
                    int count = vecsCount.get(vec);
                    count++;
                    vecsCount.replace(vec, count);
                } else {
                    vecsCount.put(vec, 1);
                }
            }
        }

        // Initialize edges list with all edges with their corresponding endpoints
        for (Triangle t : projectedTriangles) {
            Vec3D[] vecs = t.getVecs();
            edges.add(new Edge(t, vecs[0], vecs[1]));
            edges.add(new Edge(t, vecs[1], vecs[2]));
            edges.add(new Edge(t, vecs[2], vecs[0]));
        }

        // Iterate through every scanline
        for (int y = 0; y < HEIGHT; y += scanlineProof ? 5 : 1) {
            activeEdges.clear();

            // Initialize active edges list with all edges that are crossing by the current scanline
            for (Edge e : edges) {
                e.setxIntersection(e.xIntersection(y));
                if (e.getxIntersection() != null) {
                    activeEdges.add(e);
                }
            }

            // Sort active edges list by increasing order of x of the intersection point
            activeEdges.sort(Comparator.comparingDouble(Edge::getxIntersection));

            // Start from the beginning of each scanline
            int x = 0;
            List<Triangle> activeTriangles = new ArrayList<>();

            for (Edge ae : activeEdges) {
                double xIntersection = ae.getxIntersection();

                Triangle closestTri = null;
                if (activeTriangles.size() == 0) {
                    // No triangles -- draw background
                    graphics.setColor(Color.BLACK);
                } else if (activeTriangles.size() == 1) {
                    // One triangle -- no overlapping, so draw this triangle
                    determineColor(graphics, activeTriangles.get(0));
                    closestTri = activeTriangles.get(0);
                } else {
                    // More than one triangle -- find out which is the closest one and draw only this one
                    Triangle closestTriangle = activeTriangles.get(0);
                    double zClosest = Double.MAX_VALUE;
                    for (Triangle t : activeTriangles) {
                        double xMid = (x + xIntersection) / 2;

                        // Calculate z for x = xMid
                        double x1 = t.getVecs()[0].getX();
                        double x2 = t.getVecs()[1].getX();
                        double x3 = t.getVecs()[2].getX();
                        double y1 = t.getVecs()[0].getY();
                        double y2 = t.getVecs()[1].getY();
                        double y3 = t.getVecs()[2].getY();
                        double z1 = t.getVecs()[0].getZ();
                        double z2 = t.getVecs()[1].getZ();
                        double z3 = t.getVecs()[2].getZ();
                        double denominator = (x2 - x1) * (y3 - y1) - (x3 - x1) * (y2 - y1);
                        double z = z1 + ((x2 - x1) * (z3 - z1) - (x3 - x1) * (z2 - z1)) / denominator * (y - y1) - ((y2 - y1) * (z3 - z1) - (y3 - y1) * (z2 - z1)) / denominator * (xMid - x1);

                        Vec3D xMidPoint = new Vec3D(xMid, y, z);
                        Vec3D rayFromCameraToxMidPoint = Vec3D.subtract(xMidPoint, cameraPosition);
                        double distance = Vec3D.length(rayFromCameraToxMidPoint);
                        z -= distance;

                        if (z < zClosest) {
                            closestTriangle = t;
                            zClosest = z;
                        }
                    }

                    closestTri = closestTriangle;
                }

                if (closestTri == null) {
                    // Draw a section between intersection points
                    graphics.drawLine(x, y, (int) xIntersection, y);
                } else {
                    Vec3D[] vecs = getVecsWithGouraudOrder(closestTri, y);
                    Vec3D a = uniqueVecs.get(vecs[0]);
                    Vec3D b = uniqueVecs.get(vecs[1]);
                    Vec3D c = uniqueVecs.get(vecs[2]);

                    //System.out.println(a.getLum() + " " + b.getLum() + " " + c.getLum() + "\n");

                    double xD = Util.scaleToRange(a.getY(), b.getY(), y, a.getX(), b.getX());
                    double xF = Util.scaleToRange(a.getY(), c.getY(), y, a.getX(), c.getX());
                    double lumD = a.getLum() * (b.getY() - y) / (b.getY() - a.getY()) + b.getLum() * (y - a.getY()) / (b.getY() - a.getY()); // I_D
                    double lumF;
                    if (a.getY() != c.getY()) {
                        lumF = a.getLum() * (c.getY() - y) / (c.getY() - a.getY()) + c.getLum() * (y - a.getY()) / (c.getY() - a.getY());
                    } else {
                        lumF = a.getLum() * (c.getX() - xIntersection) / (c.getX() - a.getX()) + c.getLum() * (xIntersection - a.getX()) / (c.getX() - a.getX());
                    }

                    double lumX;
                    if (x == (int) xD) {
                        lumX = lumD;
                    } else {
                        lumX = lumD * ((xF - x) / (xF - xD)) + lumF * ((x - xD) / (xF - xD));
                    }

                    double lumXIntersection;
                    if ((int) xIntersection == (int) xF) {
                        lumXIntersection = lumF;
                    } else {
                        lumXIntersection = lumD * ((xF - xIntersection) / (xF - xD)) + lumF * ((xIntersection - xD) / (xF - xD));
                    }

                    if (closestTri.getR() != null && closestTri.getG() != null && closestTri.getB() != null) {
                        graphics.setColor(new Color(closestTri.getR(), closestTri.getG(), closestTri.getB()));
                        graphics.drawLine(x, y, (int) xIntersection, y);
                    } else {
                        drawGradientLine(graphics, (int) (lumX * 255), (int) (lumXIntersection * 255), x, (int) xIntersection, y, y);
                    }
                }
                x = (int) xIntersection;

                // Update section info
                if (!activeTriangles.contains(ae.getTriangle())) {
                    // Going inside the triangle
                    activeTriangles.add(ae.getTriangle());
                } else {
                    // Going outside the triangle
                    activeTriangles.remove(ae.getTriangle());
                }
            }
        }
    }

    // returns triangle's vecs, with first element being the one that contains two edges intersecting with Y
    private Vec3D[] getVecsWithGouraudOrder(Triangle triangle, int y) {
        Vec3D[] vecs = triangle.getVecs();
        int vecsLength = vecs.length; // always equal 3 btw
        // we could just check if the edge between remaining vecs does not intersect, however that may fail in some cases
        for (int i = 0; i < vecsLength; i++) {
            Edge e1 = new Edge(triangle, vecs[i], vecs[(i + 1) % vecsLength]);
            Edge e2 = new Edge(triangle, vecs[i], vecs[(i + 2) % vecsLength]);
            if (e1.xIntersection(y) != null && e2.xIntersection(y) != null) {
                vecs = new Vec3D[]{vecs[i], vecs[(i + 1) % vecsLength], vecs[(i + 2) % vecsLength]};
                break;
            }
        }
        // TODO: sometimes the loop does not quit by break

        double line01XForGivenY = Util.scaleToRange(vecs[0].getY(), vecs[1].getY(), y, vecs[0].getX(), vecs[1].getX());
        double line02XForGivenY = Util.scaleToRange(vecs[0].getY(), vecs[2].getY(), y, vecs[0].getX(), vecs[2].getX());
        if (line01XForGivenY > line02XForGivenY) {
            Vec3D temp = vecs[2];
            vecs[2] = vecs[1];
            vecs[1] = temp;
        }
        return vecs;
    }

    private void drawGradientLine(Graphics2D g2d, int startLum, int endLum, int startX, int endX, int startY, int endY) {
        if (startLum < 0 || startLum > 255) {
            startLum = 0;
        }
        if (endLum < 0 || endLum > 255) {
            endLum = 0;
        }

        Color startColor = new Color(startLum, startLum, startLum);
        Color endColor = new Color(endLum, endLum, endLum);

        GradientPaint gradient = new GradientPaint(startX, startY, startColor, endX, endY, endColor);
        g2d.setPaint(gradient);

        g2d.drawLine(startX, startY, endX, endY);
    }

    public void update() {
        double angle = System.currentTimeMillis() / 1000.0;
        Matrix matrixRotX = Matrix.makeRotationX(angle);
        Matrix matrixRotY = Matrix.makeRotationY(angle);
        Matrix matrixRotZ = Matrix.makeRotationZ(angle);

        Matrix matrixTranslation = Matrix.makeTranslation(0.0, 0.0, 3.0); // Optionally move whole scene

        Matrix worldMatrix = Matrix.makeIdentity();
        if (rotXactive) {
            worldMatrix = Matrix.mult(worldMatrix, matrixRotX);
        }
        if (rotYactive) {
            worldMatrix = Matrix.mult(worldMatrix, matrixRotY);
        }
        if (rotZactive) {
            worldMatrix = Matrix.mult(worldMatrix, matrixRotZ);
        }
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
        for (Mesh mesh : currentMeshes) {
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
                Vec3D lightSourcePos = new Vec3D(0, 3, 0);
                //lightSourcePos = Vec3D.normalise(lightSourcePos);

                Vec3D rayFromVecToLightSource;
                double[] vecsLum = {0.0, 0.0, 0.0};
                for (int i = 0; i < 3; i++) {
                    Vec3D vec = vecs[i];
                    rayFromVecToLightSource = Vec3D.subtract(lightSourcePos, vec);
                    rayFromVecToLightSource = Vec3D.normalise(rayFromVecToLightSource);
                    double dotProduct = Vec3D.dotProduct(normal, rayFromVecToLightSource);
                    if (dotProduct > 0.0) {
                        transformedTriangle.setLuminance(dotProduct);
                        vecsLum[i] = dotProduct;
                    }
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
                    if (abs(vecs[i].getX()) > 1.0 || abs(vecs[i].getY()) > 1.0 || abs(vecs[i].getZ()) > 1) {
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
                    Vec3D[] projectedVecs = projectedTriangle.getVecs();
                    for (int i = 0; i < 3; i++) {
                        projectedVecs[i].setLum(vecsLum[i]);
                    }
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

            if (keysPressed.get(KeyEvent.VK_SPACE)) {
                cameraPosition = Vec3D.add(cameraPosition, upVec);
            }
            if (keysPressed.get(KeyEvent.VK_SHIFT)) {
                cameraPosition = Vec3D.subtract(cameraPosition, upVec);
            }
            if ((keysPressed.get(KeyEvent.VK_D))) {
                cameraPosition = Vec3D.add(cameraPosition, rightVec);
            }
            if (keysPressed.get(KeyEvent.VK_A)) {
                cameraPosition = Vec3D.subtract(cameraPosition, rightVec);
            }
            if (keysPressed.get(KeyEvent.VK_W)) {
                cameraPosition = Vec3D.add(cameraPosition, forwardVec);
            }
            if (keysPressed.get(KeyEvent.VK_S)) {
                cameraPosition = Vec3D.subtract(cameraPosition, forwardVec);
            }

            if (keysPressed.get(KeyEvent.VK_LEFT)) {
                yaw -= 1.0;
            }
            if (keysPressed.get(KeyEvent.VK_RIGHT)) {
                yaw += 1.0;
            }
            if (keysPressed.get(KeyEvent.VK_DOWN)) {
                cameraRotX -= 1.0;
            }
            if (keysPressed.get(KeyEvent.VK_UP)) {
                cameraRotX += 1.0;
            }
            if (keysPressed.get(KeyEvent.VK_Q)) {
                cameraRotZ -= 1.0;
            }
            if (keysPressed.get(KeyEvent.VK_E)) {
                cameraRotZ += 1.0;
            }
            if (keysPressed.get(KeyEvent.VK_R)) {
                if (fov < 179.0) {
                    fov += 1.0;
                }
            }
            if (keysPressed.get(KeyEvent.VK_F)) {
                if (fov > 1.0) {
                    fov -= 1.0;
                }
            }
            if (keysPressed.get(KeyEvent.VK_1)) {
                drawingMethod = "alg. skaningowy";
            }
            if (keysPressed.get(KeyEvent.VK_2)) {
                drawingMethod = "alg. malarski";
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

        determineColor(g, triangle);

        g.fillPolygon(p);
    }

    private void determineColor(Graphics g, Triangle triangle) {
        if (triangle.getR() != null && triangle.getG() != null && triangle.getB() != null) {
            g.setColor(new Color(triangle.getR(), triangle.getG(), triangle.getB()));
        } else {
            int lum = (int) (255 * triangle.getLuminance());
            g.setColor(new Color(lum, lum, lum));
        }
    }

    private KeyListener createKeyListener() {
        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                keysPressed.put(keyCode, true);

                if (keyCode == KeyEvent.VK_M) {
                    drawMesh = !drawMesh;
                }
                if (keyCode == KeyEvent.VK_N) {
                    meshId = (meshId + 1) % allMeshes.size();
                    currentMeshes.clear();
                    currentMeshes.add(allMeshes.get(meshId));
                }
                if (keyCode == KeyEvent.VK_P) {
                    scanlineProof = !scanlineProof;
                }
                if (keyCode == KeyEvent.VK_X) {
                    rotXactive = !rotXactive;
                }
                if (keyCode == KeyEvent.VK_Y) {
                    rotYactive = !rotYactive;
                }
                if (keyCode == KeyEvent.VK_Z) {
                    rotZactive = !rotZactive;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                keysPressed.put(keyCode, false);
            }
        };
    }
}
