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
        glfwSetWindowPos(window, (vidmode.width() - 800) / 2, (vidmode.hegith() - 600) / 2);

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
    }
}
