package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * This class implements the MessageEncoderDecoder interface for the TFTP protocol.
 * It is responsible for encoding and decoding the messages in the TFTP protocol.
 */
public class TftpEncoderDecoder implements MessageEncoderDecoder<Command> {

    private byte[] bytes = new byte[1 << 9]; //start with 512
    private int len = 0;
    private final ByteBuffer shortBuffer = ByteBuffer.allocate(2);
    private Command command;

    /**
     * This method is responsible for decoding the next byte of the message.
     * It handles different types of commands and their specific decoding logic.
     *
     * @param nextByte The next byte to decode
     * @return The decoded command if the message is complete, null otherwise
     */
    @Override
    public Command decodeNextByte(byte nextByte) {

        if (command == null) {  //indicates that we are still reading the opcode
            short temp = decodeShort(nextByte);

            if (temp == 6 || temp == 10) // DIRQ or DISC
                return new Command(temp);
            if (temp != -1)
                command = new Command(temp);
        } else
            switch (command.getOpcode()) {
                case 1: case 2: case 7: case 8: // RRQ  WRQ  LOGRQ  DELRQ
                    return checkEndOfMessage(nextByte);

                case 3: // DATA
                    if (command.getPacketSize() == null) {
                        short temp = decodeShort(nextByte);
                        if (temp != -1)
                            command.setPacketSize(temp);
                    } else if (command.getBlockNumber() == null) {
                        short temp = decodeShort(nextByte);
                        if (temp != -1)
                            command.setBlockNumber(temp);
                    } else {
                        pushByte(nextByte);
                        if (len == command.getPacketSize()) {
                            command.setData(Arrays.copyOf(bytes, len));
                            return returnCommand();
                        }
                    }
                    return null;

                case 4: // ACK
                    if (command.getBlockNumber() == null) {
                        short temp = decodeShort(nextByte);
                        if (temp != -1) {
                            command.setBlockNumber(temp);
                            return returnCommand();
                        }
                    }
                    return null;

                case 5: // ERROR
                    if (command.getErrorCode() == null) {
                        short temp = decodeShort(nextByte);
                        if (temp != -1) {
                            command.setErrorCode(temp);
                            return returnCommand();
                        }
                    }
                    return null;

                case 9: // BCAST
                    if (command.getAddedDeleted() == null)
                        command.setAddedDeleted(nextByte);
                    else
                        return checkEndOfMessage(nextByte);
            }
        return null;
    }

    /**
     * This method checks if the end of the message has been reached.
     *
     * @param nextByte The next byte to check
     * @return The decoded command if the end of the message has been reached, null otherwise
     */
    private Command checkEndOfMessage(byte nextByte) {
        if (nextByte == 0) // End of message
        {
            command.setData(Arrays.copyOf(bytes, len));
            return returnCommand();
        }
        pushByte(nextByte);
        return null;
    }

    /**
     * This method returns the command and resets the decoder.
     *
     * @return The command
     */
    private Command returnCommand() {
        Command temp = command;
        resetDecoder();
        return temp;
    }

    /**
     * This method resets the decoder.
     */
    private void resetDecoder() {
        command = null;
        len = 0;
        shortBuffer.clear();
        bytes = new byte[1 << 9];
    }


    /**
     * This method decodes a short value from the next byte of the message.
     *
     * @param nextByte The next byte to decode
     * @return The decoded short value if two bytes have been read, -1 otherwise
     */
    private short decodeShort(byte nextByte) {
        shortBuffer.put(nextByte);
        if (!shortBuffer.hasRemaining()) { //we read 2 bytes and therefore can take the opcode
            short ret = (short) ((shortBuffer.get(0) & 0xff) << 8 | (shortBuffer.get(1) & 0xff)); // todo 0xff on the left might not be correct
            shortBuffer.clear();
            return ret;
        }
        return -1;
    }


    /**
     * This method pushes the next byte into the buffer.
     *
     * @param nextByte The next byte to push
     */
    private void pushByte(byte nextByte) {
        if (len >= bytes.length) // Resize buffer if necessary
            bytes = Arrays.copyOf(bytes, len * 2);
        bytes[len++] = nextByte;
    }


    /**
     * This method encodes a command into a byte array.
     *
     * @param message The command to encode
     * @return The encoded byte array
     */
    @Override
    public byte[] encode(Command message) {
        return message.encode();
    }
}