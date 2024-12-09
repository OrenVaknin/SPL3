package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SendingManager extends Thread {
    private static final Object lock = new Object();
    private static volatile boolean running = true;
    private static volatile String messageToSend = null;
    private String address;
    private int port;

    public SendingManager(String address, int port){
        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(address, port);
            OutputStream out = socket.getOutputStream();

            while (running) {
                String message;
                synchronized (lock) {
                    while (messageToSend == null) {
                        lock.wait();
                    }
                    message = messageToSend;
                    messageToSend = null;
                }

                // Send the message to the server
                Command commandToSend = Command.createCommand(message);
                if(commandToSend != null){
                    out.write(commandToSend.encode());
                    out.flush(); // Flush the output stream to ensure the message is sent immediately
                }
            }

            // Close resources
            out.close();
            socket.close();
            System.out.println("Message sending thread has stopped.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addMessageToQueue(String message) {
        synchronized (lock) {
            messageToSend = message;
            lock.notify();
        }
    }

    public static void stopThread() {
        running = false;
    }
}
