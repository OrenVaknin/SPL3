package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class ConnectionManager extends Thread implements MessagingProtocol<Command> {
    private final TftpEncoderDecoder decenc = new TftpEncoderDecoder();
    private final byte[] buffer = new byte[518]; // Adjust buffer size based on expected message sizes
    private final Object lock = new Object();
    private volatile boolean running = true;
    private volatile String messageToSend = null;
    private String address;
    private int port;
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private Command lastRequest = null;
    private ArrayList<Byte> allDataList = new ArrayList<>();
    private byte[] allData = null;
    //private boolean done = true;
    private int lastPacketBlock = -1;
    private final ArrayList<byte[]> splitDataList = new ArrayList<>();
    private boolean isRegistered = false;
    private boolean receiveAgain = false;


    public ConnectionManager(String address, int port) {
        this.address = address;
        this.port = port;
        try {
            this.socket = new Socket(address, port);
            out = socket.getOutputStream();
            in = socket.getInputStream();
        } catch (Exception e) {
            System.out.println("Error creating socket: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                boolean waitForResponse = true;

                if (lastPacketBlock == -1 && !(lastRequest != null && lastRequest.getOpcode() == Command.CommandOpcode.RRQ.getOpcodeValue()) && !(lastRequest != null && lastRequest.getOpcode() == Command.CommandOpcode.WRQ.getOpcodeValue())) {
                    String message = getInput();
                    waitForResponse = sendToServer(message);
                    if (message.equalsIgnoreCase("DISC"))
                        terminate();
                }
                if (waitForResponse)
                    receiveFromServer();
                if (receiveAgain) {
                    receiveFromServer();
                    receiveAgain = false;
                }
            }
            // Close resources
            out.close();
            socket.close();
            System.out.println("Message sending thread has stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getInput() {
        String message;
        synchronized (lock) {
            while (messageToSend == null)
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            message = messageToSend;
            messageToSend = null;
        }
        return message;
    }

    private boolean preSendToServer(Command commandToSend) {

        short op = commandToSend.getOpcode();
        if (!isRegistered && op != Command.CommandOpcode.LOGRQ.getOpcodeValue()) {
            System.out.println("Error: You must log in first.");
            return false;
        }

        if (isRegistered && op == Command.CommandOpcode.LOGRQ.getOpcodeValue()) {
            System.out.println("Error: You are already logged in.");
            return false;
        }

        if (op == Command.CommandOpcode.RRQ.getOpcodeValue())
            if (FileManager.fileExists(new String(commandToSend.getData(), StandardCharsets.UTF_8))) {
                System.out.println("Error: File is already exist.");
                return false;
            }

        else if (op == Command.CommandOpcode.WRQ.getOpcodeValue())
                if (!FileManager.fileExists(new String(commandToSend.getData(), StandardCharsets.UTF_8))) {
                    System.out.println("Error: File doesn't exist.");
                    return false;
                }
        return true;
    }

    private boolean sendToServer(String message) {
        Command c = Command.createCommand(message);
        if (c != null && preSendToServer(c))
            return sendToServer(c);
        if(c == null)
            System.out.println("Error sending data to server: nice try, troller.");
        return false;
    }

    private boolean sendToServer(Command commandToSend) {
        // Send the message to the server
        if (commandToSend != null) {
            try {
                out.write(commandToSend.encode());
                out.flush(); // Flush the output stream to ensure the message is sent immediately
                if (commandToSend.getOpcode() != Command.CommandOpcode.ACK.getOpcodeValue()
                        && commandToSend.getOpcode() != Command.CommandOpcode.DATA.getOpcodeValue())
                    lastRequest = commandToSend;
                return true;
            } catch (IOException e) {
                System.out.println("Error sending data to server: " + e.getMessage());
                return false;
            }
        }
        System.out.println("Error sending data to server: nice try, troller.");
        return false;
    }


    private void receiveFromServer() {
        try {
            int read;
            Command command = null;

            // Keep reading from the server as long as data is coming
            while (command == null) {
                read = in.read(buffer);
                // Process each byte through the decoder
                for (int i = 0; i < read; i++) {
                    command = decenc.decodeNextByte(buffer[i]);
                    if (command != null) {
                        process(command);
                        if (command.getOpcode() == Command.CommandOpcode.BCAST.getOpcodeValue())
                            receiveFromServer();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error receiving data from server: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void addMessageToQueue(String message) {
        synchronized (lock) {
            messageToSend = message;
            lock.notify();
        }
    }

    @Override
    public void process(Command msg) {

        if (msg.getOpcode() == Command.CommandOpcode.BCAST.getOpcodeValue()) // BCAST
        {
            String operation = msg.getAddedDeleted() ? "added." : "deleted.";
            System.out.println("BCAST: file " + new String(msg.getData()) + " has been " + operation);
        } else if (msg.getOpcode() == Command.CommandOpcode.ERROR.getOpcodeValue()) // ERROR
        {
            ErrorCommand.ErrorType errorType = ErrorCommand.ErrorType.getByCode(msg.getErrorCode());
            if (errorType != null)
                System.out.println("ERROR: " + new String(errorType.getMessageBytes()));
            else
                System.out.println("ERROR: Unknown error code: " + msg.getErrorCode());
        } else if (msg.getOpcode() == Command.CommandOpcode.ACK.getOpcodeValue()) { // ACK
            System.out.println("ACK: block number " + msg.getBlockNumber() + " received");
            if (!isRegistered) isRegistered = true;
            if (lastRequest != null && lastRequest.getOpcode() == Command.CommandOpcode.RRQ.getOpcodeValue()) { // should return ACK
                Command ACK = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
                ACK.setBlockNumber((short) 0);
                sendToServer(ACK);
            } else if (lastRequest != null && lastRequest.getOpcode() == Command.CommandOpcode.WRQ.getOpcodeValue()) { // should send data
                if (splitDataList.isEmpty())
                    splitData(new String(lastRequest.getData(), StandardCharsets.UTF_8));
                lastPacketBlock++;
                sendData();
            }
        } else if (msg.getOpcode() == Command.CommandOpcode.DATA.getOpcodeValue()) // DATA
            getAllData(msg);
        else
            System.out.println("Unknown command: " + msg.getOpcode());
    }


    private void getAllData(Command msg) {

        if (msg.getBlockNumber() != lastPacketBlock + 1) {
            sendToServer(new ErrorCommand(ErrorCommand.ErrorType.ILLEGAL_OPERATION.getCode()));
            lastPacketBlock = -1;
            allDataList = new ArrayList<>();
            System.out.println("Error! block number is not as expected.");
            return;
        }
        System.out.println("DATA: block number " + msg.getBlockNumber() + " received");
        Command ACK = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
        ACK.setBlockNumber(msg.getBlockNumber());
        sendToServer(ACK);

        lastPacketBlock = msg.getData().length == 512 ? msg.getBlockNumber() : -1;

        byte[] data = msg.getData();
        for (byte b : data)
            allDataList.add(b); // may need to reverse it

        if (data.length < 512) {
            allData = new byte[allDataList.size()];
            for (int i = 0; i < allData.length; i++)
                allData[i] = allDataList.get(i);
            allDataList = new ArrayList<>();

            try {
                if (lastRequest.getOpcode() == Command.CommandOpcode.RRQ.getOpcodeValue())
                    FileManager.writeFile(new String(lastRequest.getData()), allData);

                else if (lastRequest.getOpcode() == Command.CommandOpcode.DIRQ.getOpcodeValue()) {
                    String[] files = new String(allData).split("\0");
                    for (String file : files)
                        System.out.println(file);
                }
                lastRequest = null;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public void splitData(String filename) {
        try {
            byte[] data = FileManager.readFile(filename);
            if (data == null) {
                System.out.println("Error reading file: " + filename);
                return;
            }

            int startIndex = 0;
            while (startIndex < data.length) {
                int endIndex = Math.min(startIndex + 512, data.length);
                byte[] subArray = new byte[endIndex - startIndex];
                System.arraycopy(data, startIndex, subArray, 0, endIndex - startIndex);
                splitDataList.add(subArray);
                startIndex = endIndex;
            }
            System.out.println("File " + filename + " has been split into " + splitDataList.size() + " packets.");
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    public void sendData() {
        if (splitDataList.isEmpty()) {
            lastRequest = null;
            return;
        }
        byte[] data = splitDataList.remove(0);
        Command DATA = new Command(Command.CommandOpcode.DATA.getOpcodeValue());
        DATA.setBlockNumber((short) lastPacketBlock);
        DATA.setData(data);
        DATA.setPacketSize((short) data.length);
        sendToServer(DATA);
        System.out.println("sent data: " + DATA.getBlockNumber());
        if (splitDataList.isEmpty()) {
            System.out.println("File has been sent.");
            lastRequest = null;
            lastPacketBlock = -1;
            receiveAgain = true;
        }
    }


    @Override
    public boolean shouldTerminate() {
        return running;
    }

    public void terminate() {
        running = false;
    }
}
