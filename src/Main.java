
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Main {
    Selector selector;
    ServerSocketChannel serverSocketChannel;

    public Main() {
        try {
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void listen() throws IOException{
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
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
                    SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ );
                }

                if( key.isReadable()) {
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    System.out.println("\n-----------------------------\n");

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

                if(key.isValid() && key.isWritable()) {
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
            }
        }
    }

    private String getHtmlContent() {
        return "<html>\n" +
                "<head>\n" +
                "<title>welcome</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "陈大侠 hello world\n" +
                "</body>\n" +
                "</html>";
    }

    public static void main(String[] args) {
        Main main = new Main();
    }
}
