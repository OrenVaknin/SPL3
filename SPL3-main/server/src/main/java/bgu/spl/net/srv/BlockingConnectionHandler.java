package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Connections<T> connections;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;


    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol, Connections<T> connections) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            protocol.start(sock.getPort(), this.connections); // port is the connectionId

            // TODO: implement the protocol
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T command = encdec.decodeNextByte((byte) read);
                if (command != null) {
                    System.out.println("got message");
                    protocol.process(command, this);
                }
            }
            System.out.println("Connection Handler terminated");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        try {
            out.write(encdec.encode(msg));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
