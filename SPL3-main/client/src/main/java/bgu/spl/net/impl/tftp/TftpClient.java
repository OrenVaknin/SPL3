package bgu.spl.net.impl.tftp;

public class TftpClient {


    public static void main(String[] args) {

        // Create and start the message sending thread
        ConnectionManager messageSendingThread = new ConnectionManager(args[0], Integer.parseInt(args[1]));
        messageSendingThread.start();

        // Create and start the keyboard input thread
        inputManager keyboardInputThread = new inputManager(messageSendingThread);
        keyboardInputThread.start();
    }
}