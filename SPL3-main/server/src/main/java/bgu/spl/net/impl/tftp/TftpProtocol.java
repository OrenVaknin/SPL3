package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class TftpProtocol implements BidiMessagingProtocol<Command> {
    private int connectionId;
    private Connections<Command> connections;
    private String userName = null;
    private boolean shouldTerminate = false;
    private final List<byte[]> allDataList = new ArrayList<>();
    private short blockNumber = 0;
    private String newFileName = null;


    @Override
    public void start(int connectionId, Connections<Command> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(Command message, ConnectionHandler<Command> handler) {
        // TODO implement this
        Command ftpResponse;
        switch (message.getOpcode()) {
            case 1: // RRQ
                RRQ(message.getData());
                break;
            case 2: // WRQ
                WRQ(new String(message.getData(), StandardCharsets.UTF_8));
                break;
            case 3: // DATA
                DATA(message);
                break;
            case 4: // ACK
                ACK();
                break;
            case 5: // ERROR
            case 6: // DIRQ
                DIRQ();
                break;
            case 7: // LOGR
                if (!connections.connect(connectionId, handler, new String(message.getData(), StandardCharsets.UTF_8))){
                    handler.send(new ErrorCommand(ErrorCommand.ErrorType.USER_ALREADY_LOGGED_IN.getCode()));
                    break;
                }
                
                ftpResponse = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
                ftpResponse.setBlockNumber((short) 0);
                this.userName = new String(message.getData(), StandardCharsets.UTF_8);
                this.connections.send(connectionId, ftpResponse);
                break;

            case 8: // DELRQ
                DELRQ(new String(message.getData(), StandardCharsets.UTF_8));
                break;
//            case 9: // BCAST
//                connections.sendToAll(message);
            case 10: // DISC
                if (!connections.isConnected(userName))
                    connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.USER_NOT_LOGGED_IN.getCode()));
                else {
                    Command response = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
                    response.setBlockNumber((short) 0);
                    connections.send(connectionId, response);
                    connections.disconnect(connectionId, userName);
                    userName = null;
                }
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + message.getOpcode());
        }
    }


    public void DELRQ(String msg) {
        //delete file
        try {
            if (FileManager.deleteFile(msg)) {
                Command BCAST = new Command(Command.CommandOpcode.BCAST.getOpcodeValue());
                BCAST.setAddedDeleted((byte) 0);
                BCAST.setData(msg.getBytes());
                connections.sendToAll(BCAST);

                Command ACK = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
                ACK.setBlockNumber((short) 0);
                connections.send(connectionId, ACK);

            } else
                connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.FILE_NOT_FOUND.getCode()));
        } catch (Exception e) {
            connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.FILE_NOT_FOUND.getCode()));
        }
    }

    public void RRQ(byte[] msg) {

        try {
            byte[] file = FileManager.readFile(new String(msg, StandardCharsets.UTF_8));
            if (file == null)
                connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.FILE_NOT_FOUND.getCode()));
            else {
                splitData(file);
                blockNumber--;
                Command ACK = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
                ACK.setBlockNumber((short) 0);
                connections.send(connectionId, ACK);
            }

        } catch (Exception e) {
            connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.FILE_NOT_FOUND.getCode()));
        }

    }

    public void WRQ(String filename) {
        try {

            if (FileManager.fileExists(filename))
                connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.FILE_ALREADY_EXISTS.getCode()));
            else {
                Command BCAST = new Command(Command.CommandOpcode.BCAST.getOpcodeValue());
                BCAST.setAddedDeleted((byte) 0);
                BCAST.setData(filename.getBytes());
                connections.sendToAll(BCAST);

                newFileName = filename;
                Command ACK = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
                ACK.setBlockNumber((short) 0);
                connections.send(connectionId, ACK);
            }
        } catch (Exception e) {
            connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.FILE_NOT_FOUND.getCode()));
        }
    }

    public void DIRQ() {

        String allNamesString = String.join("\0", FileManager.getAllFileNames());

        splitData(allNamesString.getBytes(StandardCharsets.UTF_8));
        sendData();
    }

    public void DATA(Command msg) {
        if (msg.getBlockNumber() != allDataList.size()) {
            ErrorCommand error = new ErrorCommand(ErrorCommand.ErrorType.NOT_DEFINED.getCode());
            connections.send(connectionId, error);
            allDataList.clear();
            return;
        }
        Command ACK = new Command(Command.CommandOpcode.ACK.getOpcodeValue());
        ACK.setBlockNumber(msg.getBlockNumber());
        connections.send(connectionId, ACK);

        allDataList.add(msg.getData());
        if (msg.getData().length < 512) {
            byte[] data = uniteData();
            try {
                if (!FileManager.writeFile(newFileName, data))
                    connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.NOT_DEFINED.getCode()));
                else {
                    Command BCAST = new Command(Command.CommandOpcode.BCAST.getOpcodeValue());
                    BCAST.setAddedDeleted((byte) 1);
                    BCAST.setData(newFileName.getBytes(StandardCharsets.UTF_8));
                    connections.sendToAll(BCAST);
                }

            } catch (Exception e) {
                connections.send(connectionId, new ErrorCommand(ErrorCommand.ErrorType.NOT_DEFINED.getCode()));
            }
        }
    }

    public void ACK() {
        if (!allDataList.isEmpty()) {
            blockNumber++;
            sendData();
        }
    }

    public void ERROR(Command msg) {

    }

    public void splitData(byte[] data) {
        int startIndex = 0;
        while (startIndex < data.length) {
            int endIndex = Math.min(startIndex + 512, data.length);
            byte[] subArray = new byte[endIndex - startIndex];
            System.arraycopy(data, startIndex, subArray, 0, endIndex - startIndex);
            allDataList.add(subArray);
            startIndex = endIndex;
        }
    }

    public byte[] uniteData() {
        int size = (allDataList.size() - 1) * 512 + allDataList.get(allDataList.size() - 1).length;

        byte[] unitedData = new byte[size];
        int index = 0;
        for (byte[] data : allDataList) {
            System.arraycopy(data, 0, unitedData, index, data.length);
            index += data.length;
        }
        allDataList.clear();
        return unitedData;
    }

    public void sendData() {
        if (allDataList.isEmpty())
        {
            Command response = new ErrorCommand(ErrorCommand.ErrorType.FILE_NOT_FOUND.getCode());//(Command.CommandOpcode.DATA.getOpcodeValue());
            connections.send(connectionId, response);
            System.out.println("sent error: Files is empty");
        }
        else {
            Command response = new Command(Command.CommandOpcode.DATA.getOpcodeValue());
            response.setData(allDataList.remove(0));
            response.setBlockNumber(blockNumber);
            connections.send(connectionId, response);
            System.out.println("sent data: " + response.getBlockNumber());
            if (allDataList.isEmpty())
                blockNumber = 0;
        }
    }


    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
