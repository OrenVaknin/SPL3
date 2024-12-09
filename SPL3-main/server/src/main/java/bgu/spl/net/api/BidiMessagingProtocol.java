package bgu.spl.net.api;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public interface BidiMessagingProtocol<T>  {
	/**
	 * Used to initiate the current client protocol with its personal connection ID and the connections implementation
	**/
    void start(int connectionId, Connections<T> connections);
    
    void process(T message, ConnectionHandler<T> handler);
	
	/**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}
