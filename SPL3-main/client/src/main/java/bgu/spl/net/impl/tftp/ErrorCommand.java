package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;

public class ErrorCommand extends Command {

    public ErrorCommand(short errorCode) {
        super(Command.CommandOpcode.ERROR.getOpcodeValue());
        this.setErrorCode(errorCode);
    }

    public static enum ErrorType {
        NOT_DEFINED((short)0, "Not defined, see error message (if any)."),
        FILE_NOT_FOUND((short)1, "File not found – RRQ/DELRQ of non-existing file."),
        ACCESS_VIOLATION((short)2, "Access violation – File cannot be written, read or deleted."),
        DISK_FULL((short)3, "Disk full or allocation exceeded – No room in disk."),
        ILLEGAL_OPERATION((short)4, "Illegal TFTP operation – Unknown Opcode."),
        FILE_ALREADY_EXISTS((short)5, "File already exists – File name exists on WRQ."),
        USER_NOT_LOGGED_IN((short)6, "User not logged in – Any opcode received before Login completes."),
        USER_ALREADY_LOGGED_IN((short)7, "User already logged in – Login username already connected.");

        private final short code;
        private final byte[] messageBytes;

        ErrorType(short code, String message) {
            this.code = code;
            this.messageBytes = message.getBytes(StandardCharsets.UTF_8);
        }

        public short getCode() {
            return code;
        }

        public byte[] getMessageBytes() {
            return messageBytes;
        }

        public static ErrorType getByCode(short code) {
            for (ErrorType errorType : ErrorType.values()) {
                if (errorType.code == code) {
                    return errorType;
                }
            }
            // If the error code does not match any ErrorType, return null or throw an exception
            return null; // or throw new IllegalArgumentException("Unknown error code: " + code);
        }

//    public static final Map<Integer, byte[]> errorType = new HashMap<>();
//
//    // Static block to initialize opcodeValues
//    static {
//        // Adding opcode values to the map
//        errorType.put(0, "Not defined, see error message (if any).".getBytes(StandardCharsets.UTF_8));
//        errorType.put(1, "File not found – RRQ/DELRQ of non-existing file.".getBytes(StandardCharsets.UTF_8));
//        errorType.put(2, "Access violation – File cannot be written, read or deleted.".getBytes(StandardCharsets.UTF_8));
//        errorType.put(3, "Disk full or allocation exceeded – No room in disk.".getBytes(StandardCharsets.UTF_8));
//        errorType.put(4, "Illegal TFTP operation – Unknown Opcode.".getBytes(StandardCharsets.UTF_8));
//        errorType.put(5, "File already exists – File name exists on WRQ.".getBytes(StandardCharsets.UTF_8));
//        errorType.put(6, "User not logged in – Any opcode received before Login completes.".getBytes(StandardCharsets.UTF_8));
//        errorType.put(7, "User already logged in – Login username already connected.".getBytes(StandardCharsets.UTF_8));
    }
}