/*
 * ProsideBTServerApp.java
 */

package prosidebtserver;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

import java.net.*;
import java.io.*;

/**
 * The main class of the application.
 */
public class ProsideBTServerApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new ProsideBTServerView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of ProsideBTServerApp
     */
    public static ProsideBTServerApp getApplication() {
        return Application.getInstance(ProsideBTServerApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {

        launch(ProsideBTServerApp.class, args);
        
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            serverSocket = new ServerSocket(18001);
            System.err.println("Server started on port: 18001.");
        } catch (IOException e) {
            System.err.println("Could not listen on port: 18001.");
            System.exit(-1);
        }

        try{
            while (listening)
                new ProsideBTServerThread(serverSocket.accept()).start();

             serverSocket.close();
         } catch (IOException e) {
            System.err.println("Error while listening.");
            System.exit(-1);
        }
        
    }
}
