package bgu.spl.net.impl.tftp;

import java.nio.ByteBuffer;

public class Command {
    private final short opcode;
    private byte[] data;
    private Short blockNumber;
    private Short PacketSize;
    private Boolean addedDeleted;
    private Short errorCode;

    public enum CommandOpcode {
        RRQ(1, "RRQ"),
        WRQ(2, "WRQ"),
        DATA(3, "DATA"),
        ACK(4, "ACK"),
        ERROR(5, "ERROR"),
        DIRQ(6, "DIRQ"),
        LOGRQ(7, "LOGRQ"),
        DELRQ(8, "DELRQ"),
        BCAST(9, "BCAST"),
        DISC(10, "DISC");


        private final int opcodeValue;
        private final String operation;

        CommandOpcode(int opcodeValue, String operation) {
            this.opcodeValue = opcodeValue;
            this.operation = operation;
        }

        public short getOpcodeValue() {
            return (short) opcodeValue;
        }

        public String getOperation() {
            return operation;
        }
    }

    public Command(short opcode) {
        this.opcode = opcode;
    }

    public static Command errorCommand(short ErrorCode) {
        return new ErrorCommand(ErrorCode);
    }

    public short getOpcode() {
        return opcode;
    }

    public byte[] getData() {
        return data;
    }

    public Short getBlockNumber() {
        return blockNumber;
    }

    public Short getPacketSize() {
        return PacketSize;
    }

    public Boolean getAddedDeleted() {
        return addedDeleted;
    }

    public Short getErrorCode() {
        return errorCode;
    }

    public void setData(byte[] data) {
        this.data = data;
        if (this.opcode == CommandOpcode.DATA.getOpcodeValue())
            this.PacketSize = (short) data.length;
    }

    public void setBlockNumber(short blockNumber) {
        this.blockNumber = blockNumber;
    }

    public void setPacketSize(short PacketSize) {
        this.PacketSize = PacketSize;
    }

    public void setAddedDeleted(byte AddedDeleted) {
        this.addedDeleted = AddedDeleted > 0;
    }

    public void setErrorCode(short ErrorCode) {
        this.errorCode = ErrorCode;
    }

    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(1 << 9 + 6);

        buffer.putShort(opcode);

        switch (opcode) {
            case 1: // RRQ
            case 2: // WRQ
            case 7: // LOGRQ
            case 8: // DELRQ
                buffer.put(data);
                buffer.put((byte) 0); // Add termination byte
                break;
            case 3: // DATA
                buffer.putShort(PacketSize);
                buffer.putShort(blockNumber);
                buffer.put(data);
                break;
            case 4: // ACK
                buffer.putShort(blockNumber);
                break;
            case 5: // ERROR
                buffer.putShort(errorCode);
                buffer.put(data);
                buffer.put((byte) 0); // Add termination byte
                break;
            case 9: // BCAST
                buffer.put((byte) (addedDeleted ? 1 : 0));
                buffer.put(data);
                buffer.put((byte) 0); // Add termination byte
                break;

            case 6: // DIRQ
            case 10: // DISC
                break; // No additional data needed
            default:
                throw new IllegalArgumentException("Invalid opcode");
        }

        byte[] encoded = new byte[buffer.position()];
        buffer.flip(); // Prepare for reading
        buffer.get(encoded); // Read into byte array
        return encoded;
    }

    public static Command createCommand(String message) {
        String[] parts = message.split("\\s+");
        String commandName = parts[0].toUpperCase();

        // Get the opcode corresponding to the command name
        CommandOpcode opcodeEnum = CommandOpcode.valueOf(commandName);
        short opcodeValue = opcodeEnum.getOpcodeValue();

        // Create a Command object with the extracted opcode
        Command command = new Command(opcodeValue);

        // Check if the command requires additional data

        // Extract additional data based on the opcode
        try {
            switch (opcodeValue) {

                case 6:
                case 10: // DIRQ, DISC
                    break;

                case 1:
                case 2:
                case 7:
                case 8: // RRQ WRQ LOGRQ DELRQ
                    String filename = parts[1]; // The second part is the filename
                    command.setData(filename.getBytes()); // Set the filename as data
                    break;

                case 3: // DATA
                    short packetSize = Short.parseShort(parts[1]); // The second part is the packet size
                    short blockNumberData = Short.parseShort(parts[2]); // The third part is the block number
                    byte[] data = parts[3].getBytes(); // The fourth part is the data
                    command.setPacketSize(packetSize);
                    command.setBlockNumber(blockNumberData);
                    command.setData(data);
                    break;

                case 4: // ACK
                    short blockNumber = Short.parseShort(parts[1]); // The second part is the block number
                    command.setBlockNumber(blockNumber);
                    break;


                case 5: // ERROR
                    short errorCode = Short.parseShort(parts[1]); // The second part is the error code
                    String errorMessage = parts.length > 2 ? parts[2] : ""; // Optional error message
                    command.setErrorCode(errorCode);
                    command.setData(errorMessage.getBytes()); // Set the error message as data
                    break;

                case 9: // BCAST
                    byte addedDeleted = Byte.parseByte(parts[1]); // The second part indicates added/deleted
                    String filenameBCast = parts[2]; // The third part is the filename
                    command.setAddedDeleted(addedDeleted);
                    command.setData(filenameBCast.getBytes()); // Set the filename as data
                    break;
                default:
                    return null;
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }


        return command;
    }
}

