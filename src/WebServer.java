import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class WebServer {
    public WebServer(){
        try {
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void onAccept(Selector selector, SelectionKey key) throws IOException {
        SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ );
    }

    private  void  onRead(Selector selector, SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        //System.out.println("\n-----------------------------\n");

        long bytesRead = -1;
        try{
            bytesRead = clientChannel.read(buf);
        }catch (IOException ioe) {}

        if (bytesRead == -1) {
            clientChannel.close();
        } else if (bytesRead > 0) {
            buf.flip();
            while (buf.hasRemaining()) {
                System.out.print((char) buf.get());
            }
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }

    private void  onWrite(Selector selector, SelectionKey key) throws  IOException{
        String content = getHtmlContent();
        int contentLength  = content.getBytes().length;
        String response = "HTTP/1.1 200 OK\n" +
                "Content-Type:text/html;charset=UTF-8\n" +
                "Content-Length:"+ contentLength +"\n" +
                "\n" +
                content;
        ByteBuffer buf = ByteBuffer.allocate(response.getBytes().length);
        buf.put(response.getBytes());
        buf.flip();
        SocketChannel clientChannel = (SocketChannel) key.channel();
        clientChannel.write(buf);
        key.interestOps(SelectionKey.OP_READ);
    }

    private String getHtmlContent() {
        return "<html>\n" +
                "<head>\n" +
                "<title>welcome</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "hello world\n" +
                "</body>\n" +
                "</html>";
    }

    private void listen() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8000));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector,  SelectionKey.OP_ACCEPT );

        while (true) {
            selector.select();
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if(key.isAcceptable()) {
                    onAccept(selector,key);
                }
                if( key.isReadable()) {
                    onRead(selector,key);
                }
                if(key.isValid() && key.isWritable()) {
                    onWrite(selector,key);
                }
            }
        }
    }
}
