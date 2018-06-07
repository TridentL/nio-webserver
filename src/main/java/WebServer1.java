import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class WebServer1 {

    public WebServer1() {
        try {
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8000));
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        selectionKey.attach(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
                    selectionKey.attach(new Runnable() {
                        @Override
                        public void run() {
                            ByteBuffer buf = ByteBuffer.allocate(8);
                            try {
                                String request = "";
                                long bytesRead;
                                while ((bytesRead = socketChannel.read(buf)) > 0) {
                                    buf.flip();
                                    while (buf.hasRemaining()) {
                                        request += (char)buf.get();
                                    }
                                    buf.clear();
                                }
                                if(bytesRead == -1) {
                                    socketChannel.close();
                                }
                                System.out.println(request);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectionKeys.iterator();
            //Selector如果发现channel有OP_ACCEPT或READ事件发生，下列遍历就会进行。
            while (it.hasNext()) {
                //来一个事件 第一次触发一个accepter线程
                //以后触发SocketReadHandler
                SelectionKey sKey = it.next();
                it.remove();
                dispatch(sKey);
            }
        }
    }

    private void dispatch(SelectionKey key) {
        Runnable runnable = (Runnable) key.attachment();
        if (runnable != null) {
            runnable.run();
        }
    }
}
