// javac -classpath ".;C:\Users\Katie\Documents\CSC313\lwjgl\*" Celshading.java
// java -classpath ".;C:\Users\Katie\Documents\CSC313\lwjgl\*" Celshading

import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.util.Arrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class CelShading {
    // The window handle
    private long window;
    private Model model;

    public void run() {
        System.out.println("Starting LWJGL " + Version.getVersion() + "!");

        try {
            init(); // Initialize GLFW and OpenGL

            // Check if the model is loaded successfully
            if (model == null) {
                throw new RuntimeException("Model could not be loaded. Exiting...");
            }

            loop(); // Main rendering loop

            // Free resourcesa and terminate GLFW
            cleanup();
        }
        finally {
            // Get the current error callback
            GLFWErrorCallback callback = glfwSetErrorCallback(null);
            if (callback != null) {
                callback.free(); // Only free it if it's not null
            }
            glfwTerminate(); // Terminate GLFW
        }
    }

    private void init() {
        // Load the OBJ model
        try {
            model = loadModel("sphere.obj");
            if (model == null) {
                throw new RuntimeException("Failed to load the model.");
            }
            else {
                System.out.println("Model loaded successfully.");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading model", e);
        }

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWIndowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFE_TRUE);

        // Create the window
        window = glfwCreateWindow(800, 600, "Cel Shading with LWJGL", NULL, NULL);
        if (window == null) {
            throw new RuntimeException("Failed to create the GLFW window.");
        }
        else {
            System.out.println("GLFW window created successfully.");
        }

        // Center the window on the screen
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - 800) / 2, (vidmode.height() - 600) / 2);

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's OpenGL context,
            // or any context that is managed externally. LWJGL detects the context  that is
            // current in the current thread, creates the GLCapabilities instance and makes
            // the OpenGL bindings available for use.
        GL.createCapabilities();

        // Set up OpenGL settings, e.g., lighting, shading
        setupLighting();
        setupMaterial();

        // Set clear color
        glClearColor(0.0f, 1.0f, 1.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close the window or pressed the ESC key
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer
            setupCamera();
            renderOutline(model);

            // Render the model here
            renderModel(model); // Call your renderModel function

            glfwSwapBuffers(window); // Swap the color buffers

            // Poll for window events. The key callback above will only be invoked during this call
            glfwPollEvents();
        }
    }

    private void cleanup() {
        // Free the key callback if it was set
        if (glfwSetKeyCallback(window, null) != null) {
            glfwKeyCallback(window, null).free();
        }

        // Destroy the window if it exists
        if (window != NULL) {
            glfwDestroyWindow(window);
        }
    }

    private void setPerspective(float fov, float aspectRatio, float near, float far) {
        float fH = (float) Math.tan(Math.toRadians(fov / 2)) * near;
        float fW = fH * aspectRatio;

        glFrustum(-fW, fW, -fH, fH, near, far);
    }

    private void gluLookAt (float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        // Calculate forward vector
        float fx = centerX - eyeX;
        float fy = centerY - eyeY;
        float fz = centerZ - eyeZ;
        float rlf = 1.0f / (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx *= rlf;
        fy *= rlf;
        fz *= rlf;

        // Calculate up vector
        float rlsu = 1.0f / (float) Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        float sx = upY * fz - upZ * fy;
        float sy = upZ * fx - upX * fz;
        float sz = upX * fy - upY * fx;

        // Recompute up vector
        float ux = fy * sz - fz * sy;
        float uy = fz * sx - fx * sz;
        float uz = fx * sy - fy * sx;

        matrix.put(new float[] {
                sx, ux, -fx, 0.0f,
                sy, uy, -fy, 0.0f,
                sz, uz, -fz, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
        matrix.flip();

        glMultMatrix(matrix);   // Apply the matrix

        glTranslate(-eyeX, -eyeY, -eyeZ);   // Translate to camera position
    }

    private void setupCamera() {
        // Set up projection matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        // Create a perspective projection
        // Field of view: 45 degrees, aspect ratio: window width / height, near: 0.1, far: 100
        float aspectRatio = 800.0f / 600.0f;
        setPerspective(45.0f, aspectRatio, 0.1f, 100.0f);

        // Set up modelview matrix (camera/view transformation)
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        // Move the camera back a bit so we can see the object
        gluLookAt(0.0f, 0.0f, 10.0f,    // Eye/Camera position
                0.0f, 0.0f, 0.0f,   // Center of the scene (what we're looking at)
                0.0f, 1.0f, 0.0f);  // Up vector
    }

    public void setupLighting() {
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);

        // configure OpenGL to use the vertex colors for lighting calculations
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

        // Set light parameters
        FloatBuffer ambientLight = BufferUtils.createFloatBuffer(4).put(new float[] {0.2f, 0.2f, 0.2f, 1.0f});
        ambientLight.flip();
        glLightfv(GL_LIGHT0, GL_AMBIENT, ambientLight);

        FloatBuffer diffuseLight = BufferUtils.createFloatBuffer(4).put(new float[] {0.9f, 0.9f, 0.9f, 1.0f});
        diffuseLight.flip();
        glLightfv(GL_LIGHT0, GL_DIFFUSE, diffuseLight);

        FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4).put(new float[] {10.0f, 0.9f, 0.9f, 1.0f});
        lightPosition.flip();
        glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);

        FloatBuffer noSpecular = BufferUtils.createFloatBuffer(4).put(new float[] {0.0f, 0.0f, 0.0f, 1.0f});
        noSpecular.flip();
        glLightfv(GL_LIGHT0, GL_SPECULAR, noSpecular);

        glShadeModel(GL_FLAT);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    public void setupMaterial() {
        // Define material properties
        FloatBuffer matAmbient = BufferUtils.createFloatBuffer(4).put(new float[] {0.2f, 0.2f, 0.2f, 1.0f});
        FloatBuffer matDiffuse = BufferUtils.createFloatBuffer(4).put(new float[] {0.8f, 0.8f, 0.8f, 1.0f});
        FloatBuffer matSpecular = BufferUtils.createFloatBuffer(4).put(new float[] {1.0f, 1.0f, 1.0f, 1.0f});
        float shininess = 32.0f;

        matAmbient.flip();
        matDiffuse.flip();
        matSpecular.flip();

        // Set material properties
        glMaterialfv(GL_FRONT, GL_AMBIENT, matAmbient);
        glMaterialfv(GL_FRONT, GL_AMBIENT, matDiffuse);
        glMaterialfv(GL_FRONT, GL_AMBIENT, matSpecular);
        glMaterialf(GL_FRONT, GL_AMBIENT, shininess);
    }

    public void renderModel (Model model) {
        // Re-enable lighting and set flat shading mode
        glEnable(GL_LIGHTING);
        glShadeModel(GL_FLAT);

        glEnable(GL_DEPTH_TEST);

        // Use the consistent light direction from the lighting setup
        float[] lightDir = {10.0f, 15.0f, 10.0f};

        glBegin(GL_TRIANGLES);
        for (int[] face : model.faces) {
            // Calculate face normal for lighting
            float[] normal = calculateFaceNormal(model, face);
            if (normal != null) {
                // Calculate diffuse lighting based on the face normal
                float lightIntensity = calculateDiffuseLight(normal, lightDir);

                // Use the calculated intensity to set the color
                g1Color3f(lightIntensity, lightIntensity, lightIntensity);  // Expect shades of gray
            }

            // Render each vertex of the face
            for (int i = 0; i < 3; i++) {
                int vertexIndex = face[i + 3];
                float[] vertex = model.vertices.get(vertexIndex);
                g1Vertex3f(vertex[0], vertex[1], vertex[2]);
            }
        }
        glEnd();
    }

    private float[] calculateFaceNormal (Model model, int[] face) {
        if (face.length < 9) return null;

        float[] v0 = model.vertices.get(face[0]);
        float[] v1 = model.vertices.get(face[3]);
        float[] v2 = model.vertices.get(face[6]);

        float[] edge1 = {v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
        float[] edge2 = {v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};

        float[] normal = {
                edge1[1] * edge2[2] - edge1[2] * edge2[1],
                edge1[2] * edge2[0] - edge1[0] * edge2[2],
                edge1[0] * edge2[1] - edge1[1] * edge2[0]
        };

        float length = (float) Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
        if (length == 0) {
            System.out.println("Invalid normal length (zero) for face: " + Arrays.toString(face));
            return new float[] {0, 0, 1};   // Default normal if calculation fails
        }

        float[] normalizedNormal = {normal[0] / length, normal[1] / length, normal[2] / length};

        return normalizedNormal;
    }

    private float calculateDiffuseLight(float[] normal, float[] lightDir) {
        // Normalize the light direction
        float magnitude = (float) Math.sqrt(lightDir[0] * lightDir[0] + lightDir[1] * lightDir[1] + lightDir[2] * lightDir[2]);
        float[] normalizedLightDir = {lightDir[0] / magnitude,
        lightDir[1] / magnitude, lightDir[2] / magnitude};

        // Calculate the dot product between the normal and light direction
        float dotProduct = normal[0] * normalizedLightDir[0] + normal[1] * normalizedLightDir[1] + normal[2] * normalizedLightDir[2];
        float intensity = Math.max(0, dotProduct);

        return intensity;
    }

    // Quantize the light intensity into "levels" (e.g., 4 leveles for cel shading)
    private float quantizeLight(float intensity, int levels) {
        // Map the intensity to the nearest quantization level
        float step = 1.0f / levels;
        return Math.round(intensity / step) * step;
    }

    public void renderOutline(Model model) {
        glDisable(GL_LIGHTING); //Disable lighting for outline

        glPolygonMode(GL_BACK, GL_LINE); // Render the back faces as wireframes
        glLineWidth(3.0f); // Set the outline thickness

        glColor3f(0.0f, 0.0f, 0.0f); // Black color for the outline

        glBegin(GL_TRIANGLES);
        for (int[] face : model.faces) {
            for (int i = 0; i < 3; i++) {
                int vertexIndex = face[i * 3];

                if (vertexIndex < 0 || vertexIndex >= model.vertices.size()) {
                    System.out.println("Invalid vertex index: " + vertexIndex);
                    continue;
                }

                float[] vertex = model.vertices.get(vertexIndex);

                // Render the vertex with a slight offset (inflate the geometry)
                glVertex3f(vertex[0] * 1.01f, vertex[1] * 1.0f, vertex[2] * 1.01f);
            }
        }
        glEnd();

        glPolygonMode(GL_BLACK_GL_FILL); // Reset polygon mode
    }

    public static void main(String[] args) {
        new CelShading().run();;
    }

    public static class Model {
        public List<float[]> vertices;
        public List<float[]> normals;
        public List<int[]> faces;

        public Model(List<float[]> vertices, List<float[]> normals, List<int[]> faces) {
            this.vertices = vertices;
            this.normals = normals;
            this.faces = faces;
        }
    }

    public static Model loadModel(String filename) throws IOException {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 0) {
                    continue; // Skip empty lines
                }

                switch (parts[0]) {
                    case "v":
                        //Vertex position (v x y z)
                        if (parts.length < 4) {
                            System.out.println("Invalid vertex definition: " + line);
                            continue; // Skip this line if the vertex format is wrong
                        }
                        float[] vertex = new float[] {
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        };
                        vertices.add(vertex);
                        break;

                    case "vn":
                        // Vertex normal (vn x y z)
                        if (parts.length < 4) {
                            System.out.println("Invalid normal definition: " + line);
                            continue;
                        }
                        float[] normal = new float[] {
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        };
                        normals.add(normal);
                        break;

                    case "f":
                        // Handle face formats: v/vt/vn or v//vn
                        int[] face = new int[9]; // Allocate for vertex, texture, and normal indices
                        for (int i = 1; i <= 3; i ++) {
                            String[] faceParts = parts[i].split("/");

                            int vertexIndex = Integer.parseInt(faceParts[0]) - 1;
                            if (vertexIndex < 0 || vertexIndex >= vertices.size()) {
                                System.out.println("Invalid vertex index in OBJ file: " + vertexIndex);
                                continue; // Skip this face if the vertex index is out of bounds
                            }
                            face[(i - 1) * 3] = vertexIndex; // Vertex index
                            if (faceParts.length == 3) {
                                // Handle normal index
                                int normalIndex = Integer.parseInt(faceParts[2]) - 1;
                                if (normalIndex < 0 || normalIndex >= normals.size()) {
                                    System.out.println("Invalid normal index in OBJ file: " + normalIndex);
                                    continue; // Skip this face if the normal index is out of bounds
                                }
                                face[(i - 1) * 3 + 2] = normalIndex; // Normal index
                            }
                        }
                        faces.add(face);
                        break;

                    default:
                        //ignore other lines(vt, comments, etc.)
                        break;
                }
            }
        }

        // Check if the model has valid data
        if (vertices.isEmpty()) {
            System.out.println("No vertices were loaded from the OBJ file.");
            return null;
        }
        if (faces.isEmpty()) {
            System.out.println("No faces were loaded from the OBJ file.");
            return null;
        }

        return new Model(vertices, normals, faces);
    }
}
