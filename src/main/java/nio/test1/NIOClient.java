package nio.test1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author bitao
 * @date 2021/11/5 下午1:54
 */

public class NIOClient {
    private static final Logger logger = LoggerFactory.getLogger(NIOClient.class);
    //通道管理器，NIO选择器
    private Selector selector;

    /**
     * 启动客户端测试
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        NIOClient client = new NIOClient();
        client.initClient("localhost", 9000);
        client.connect();
    }

    /**
     * 初始化NIO客户端，获得一个Socket通道，并对该通道做一些初始化的工作
     *
     * @param ip   连接的服务器的ip
     * @param port 连接的服务器的端口号
     * @throws IOException
     */
    public void initClient(String ip, int port) throws IOException {
        // 获得一个客户端Socket通道
        SocketChannel channel = SocketChannel.open();
        // 设置通道为非阻塞
        channel.configureBlocking(false);
        // 初始化一个通道管理器
        this.selector = Selector.open();
        //客户端连接服务端,其实方法执行并没有实现连接，需要在listen（）方法中调
        //用channel.finishConnect() 才能完成连接
        channel.connect(new InetSocketAddress(ip, port));
        //也可以直接调用SocketChannel的open()方法与服务端进行连接
//        SocketChannel channel1 = SocketChannel.open(new InetSocketAddress(ip, port));
        //将通道管理器和该通道绑定，并为该通道注册SelectionKey.OP_CONNECT事件，向选择器注册Channel，并监听该Channel上的Connect事件
        //注意，CONNECT事件只会在客户端的SocketChannel上发送，而ACCEPT事件只会在服务端的SocketChannel上发生
        channel.register(selector, SelectionKey.OP_CONNECT);
    }

    /**
     * 采用轮询的方式监听selector上是否有需要处理的事件，如果有，则进行处理
     *
     * @throws IOException
     */
    public void connect() throws IOException {
        // 轮询selector，选择注册Channel上的感兴趣事件
        while (true) {
            selector.select();
            // 获得selector中选中的项的迭代器
            Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                // 删除已选的key,以防下一次轮询重复处理
                it.remove();
                // 连接事件发生
                if (key.isConnectable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    // 如果正在连接，则完成连接
                    if (channel.isConnectionPending()) {
                        channel.finishConnect();
                    } else {
                        logger.info("客户端连接服务端成功");
                    }
                    // 设置成非阻塞，之前貌似已经设置为非阻塞了
                    channel.configureBlocking(false);
                    //客户端给服务端发送信息，此时服务端就可以读取到数据了
                    ByteBuffer buffer = ByteBuffer.wrap("HelloServer".getBytes());
                    channel.write(buffer);
                    //为了可以接收到服务端给客户端发送的信息，需要给客户端的SocketChannel通道设置读事件。
                    channel.register(this.selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    read(key);
                }
            }
        }
    }

    /**
     * 处理读取服务端发来的信息 的事件
     *
     * @param key
     * @throws IOException
     */
    public void read(SelectionKey key) throws IOException {
        //和服务端的read方法一样
        // 服务器可读取消息:得到事件发生的Socket通道
        SocketChannel channel = (SocketChannel) key.channel();
        // 创建读取的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int len = channel.read(buffer);
        if (len != -1) {
            logger.info("客户端收到信息：" + new String(buffer.array(), 0, len));
        }
    }
}

