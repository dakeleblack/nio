package nio.test1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author bitao
 * @date 2021/11/5 下午1:53
 */

public class NIOServer {
    private static final Logger logger = LoggerFactory.getLogger(NIOServer.class);

    //可以使用线程池来处理实际的IO请求操作，主线程可以继续进行其他Channel就绪事件的处理
    public static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        //创建一个在本地端口进行监听的服务Socket通道，并设置为非阻塞方式，即服务端的Channel
        ServerSocketChannel ssc = ServerSocketChannel.open();
        //必须配置为非阻塞模式才能往selector上注册该Channel，否则会报错，因为selector模式本身就是非阻塞模式
        ssc.configureBlocking(false);
        //服务端启动
        ssc.socket().bind(new InetSocketAddress(9000));
        logger.info("服务端启动成功");
        //实例化一个选择器Selector
        Selector selector = Selector.open();
        //把ServerSocketChannel注册到selector上，并且使得selector对ServerSocketChannel的接受连接事件感兴趣，即对客户端accept连接操作感兴趣
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        /**
         * 服务端一直轮询选择器，若注册的某个Channel上有该channel感兴趣的事件，那么选择器就会把该Channel筛选出来以供
         * 后续的IO事件处理器进行处理
         */
        while (true) {
            logger.info("等待事件发生。。");
            // 轮询监听channel里的key，select是阻塞的，accept()也是阻塞的
            int select = selector.select();
            logger.info("当前有{}个Channel发生就绪事件", select);
            // 有客户端请求，被轮询监听到，处理各个就绪事件
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                //每个向Selector注册的Channel都会对应一个特定的SelectionKey，从该SelectionKey中可以获取对应的Channel
                SelectionKey key = it.next();
                //删除本次处理的SelectionKey，防止下次select重复处理
                it.remove();
                //处理对应SelectionKey上的就绪事件
                handle(key);
            }
        }
    }

    /**
     * 事件处理器，匹配不同的就绪事件进行处理
     *
     * @param key
     * @throws IOException
     */
    private static void handle(SelectionKey key) throws IOException {
        //处理客户端连接事件
        if (key.isAcceptable()) {
            logger.info("有客户端连接事件发生了。。");
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            //NIO非阻塞体现：此处accept方法是阻塞的，但是这里因为是发生了连接事件，即一定会有新的客户端连接请求，所以这个方法会马上执行完，不会阻塞
            //处理完连接请求不会继续等待客户端的数据发送，此处得到客户端对应的SocketChannel
            SocketChannel sc = ssc.accept();
            //设置非阻塞模式并向Selector选择器进行注册
            sc.configureBlocking(false);
            //通过Selector监听服务端的SocketChannel时对读事件感兴趣
            sc.register(key.selector(), SelectionKey.OP_READ);
        } else if (key.isReadable()) {//处理客户端可读数据事件
            logger.info("有客户端数据可读事件发生了。。");
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            //NIO非阻塞体现:首先read方法不会阻塞，其次这种事件响应模型，当调用到read方法时肯定是发生了客户端发送数据的事件
            //因此一定可以从客户端的SocketChannel中读取到数据
            int len = sc.read(buffer);
            if (len != -1) {
                logger.info("读取到客户端发送的数据：" + new String(buffer.array(), 0, len));
            } else {
                logger.warn("客户端连接关闭。。。");
                //当客户端断开时，需要关闭服务端的SocketChannel
                sc.close();
                key.cancel();
                return;
            }
            //服务端发送数据给客户端
            ByteBuffer bufferToWrite = ByteBuffer.wrap("HelloClient".getBytes());
            sc.write(bufferToWrite);
            //对客户端的SocketChannel监听读数据和写数据事件，只有注册了写事件，才会监听channel上的写就绪事件
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } else if (key.isWritable()) {
            SocketChannel sc = (SocketChannel) key.channel();
            logger.info("write事件");
            // NIO事件触发是水平触发
            // 使用Java的NIO编程的时候，在没有数据可以往外写的时候要取消写事件，在有数据往外写的时候再注册写事件
            //取消监听写事件，重新只对读事件感兴趣
            key.interestOps(SelectionKey.OP_READ);
        }
    }
}

