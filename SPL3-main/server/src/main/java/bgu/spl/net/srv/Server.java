package bgu.spl.net.srv;


import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.Closeable;
import java.util.function.Supplier;

public interface Server<T> extends Closeable {

    /**
     * The main loop of the server, Starts listening and handling new clients.
     */
    void serve();

    /**
     *This function returns a new instance of a thread per client pattern server
     * @param port The port for the server socket
     * @param protocolFactory A factory that creates new MessagingProtocols
     * @param encoderDecoderFactory A factory that creates new MessageEncoderDecoder
     * @param <T> The Message Object for the protocol
     * @return A new Thread per client server
     */
    public static <T> Server<T>  threadPerClient(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encoderDecoderFactory,
            Supplier<Connections<T>> connectionsFactory) {

        return new BaseServer<T>(port, protocolFactory, encoderDecoderFactory, connectionsFactory) {
            @Override
            protected void execute(BlockingConnectionHandler<T>  handler) {
                new Thread(handler).start();
            }
        };
    }
}
