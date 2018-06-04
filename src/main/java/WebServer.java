import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class WebServer {
    public WebServer() {
        try {
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onAccept(Selector selector, SelectionKey key) throws IOException {
        SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
        if( clientChannel == null) {
            return;
        }
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private void onRead(Selector selector, SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        //System.out.println("\n-----------------------------\n");

        long bytesRead = -1;
        try {
            bytesRead = clientChannel.read(buf);
        } catch (IOException ioe) {
        }
        if (bytesRead > 0) {
            buf.flip();
            String request = "";
            while (buf.hasRemaining()) {
                request += buf.get();
            }
            if (key.isWritable()) {
                onWrite(selector, key);
            }
        }
    }

    private AtomicInteger count = new AtomicInteger(0);


    private void onWrite(Selector selector, SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        String content = getHtmlContent(clientChannel);
        int contentLength = content.getBytes().length;
        String response = "HTTP/1.1 200 OK\n" +
                "Content-Type:text/html;charset=UTF-8\n" +
                "Content-Length:" + contentLength + "\n" +
                "\n" +
                content;
        ByteBuffer buf = ByteBuffer.allocate(response.getBytes().length);
        buf.put(response.getBytes());
        buf.flip();
        clientChannel.write(buf);
        int result = count.addAndGet(1);
        System.out.println(result);
    }

    private String getHtmlContent(SocketChannel clientChannel) throws IOException {
        String clientInfo = clientChannel.getRemoteAddress().toString();
        return "<html>\n" +
                "<head>\n" +
                "<title>welcome</title>\n" +
                "</head>\n" +
                "<body>\n" +
                clientInfo +
                " hello world\n" +
                "</body>\n" +
                "</html>";
    }

    private void select(ServerSocketChannel serverSocketChannel) throws IOException {
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            selector.select();
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isAcceptable()) {
                    onAccept(selector, key);
                }
                if (key.isReadable()) {
                    onRead(selector, key);
                }
            }
        }
    }

    private void listen() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8000));
        serverSocketChannel.configureBlocking(false);

        CountDownLatch countDownLatch = new CountDownLatch(100);
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 100;i++) {
            executorService.execute(() -> {
                try {
                    countDownLatch.countDown();
                    select(serverSocketChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
