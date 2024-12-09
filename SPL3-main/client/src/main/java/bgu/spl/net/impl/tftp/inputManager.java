package bgu.spl.net.impl.tftp;

import java.util.Scanner;

public class inputManager extends Thread {

    private final ConnectionManager sender;

    public inputManager(ConnectionManager SendingManager){
        this.sender = SendingManager;
    }
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String userInput;
        System.out.print("Enter a commands:\n ");
        // Continuously listen for keyboard input until the thread is interrupted
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            userInput = scanner.nextLine();
            sender.addMessageToQueue(userInput);
            if (userInput.equalsIgnoreCase("DISC")) {
                break;
            }
        }

        scanner.close();
        System.out.println("Keyboard input thread has stopped.");
    }
}