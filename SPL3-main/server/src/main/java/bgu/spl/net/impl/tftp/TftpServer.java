package bgu.spl.net.impl.tftp;


import bgu.spl.net.srv.Server;

public class TftpServer {

    public static void main(String[] args) {


        if(FileManager.init()){
            // function in the base server
            Server.threadPerClient(
                    Integer.parseInt(args[0]), //port
                    TftpProtocol::new, //protocol factory
                    TftpEncoderDecoder::new, //message encoder decoder factory
                    ConnectionsImpl::getInstance //connections factory
            ).serve();
        }
    }
}
