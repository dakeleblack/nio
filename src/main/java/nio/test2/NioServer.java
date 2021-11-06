package nio.test2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NioServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(NioServer.class);
    //每个服务端包含一个Selector对象，用于轮询注册Channel上的就绪事件
    private Selector selector;

    private ByteBuffer readBuf = ByteBuffer.allocate(1024);

    private ByteBuffer writeBuf = ByteBuffer.allocate(1024);

    public NioServer(int port) {
        try {
            //实例化Selector
            selector = Selector.open();
            //实例化ServerSocketChannel
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            //ServerSocketChannel设置为非阻塞模式，并向Selector注册，此channel只关注接收客户端连接事件
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            //注册连接事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("success start NioServer,port is {}", port);
        } catch (Exception e) {
            logger.error("init NioServer fail:", e);
        }
    }

    /**
     * 接收到客户端新的连接请求时的处理逻辑，即ServerSocketChannel关注的新建连接事件，调用此事件处理器时
     * 服务端一定接收到了新的连接请求，否则不会进入该事件处理器
     *
     * @param selectionKey 就绪通道的唯一键
     */
    private void accept(SelectionKey selectionKey) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            //由于ServerSocketChannel设置了非阻塞模式，所以这里不阻塞，没有连接则直接返回，而且此处一定有连接
            //若有则获取客户端的SocketChannel
            SocketChannel socketChannel = serverSocketChannel.accept();
            //将客户端的SocketChannel也设为非阻塞模式，客户端那边已经设置过
            socketChannel.configureBlocking(false);
            //客户端的SocketChannel在注册后先关注该channel的读取事件
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (Exception e) {
            logger.error("create new client connection:{} fail:", selectionKey, e);
        }
    }

    /**
     * SocketChannel读数据事件就绪的处理逻辑，即SocketChannel关注的事件，需要把channel中的数据写到buffer
     * 所以buffer是读模式
     *
     * @param selectionKey 就绪通道的唯一键
     */
    private void read(SelectionKey selectionKey) {
        readBuf.clear();
        //获取socketChannel
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            //读取channel数据写入buffer，此方法不会阻塞，count=0是没有数据，count=-1应该是客户端关闭了
            int count = socketChannel.read(readBuf);
            logger.info("count is {}", count);
            //客户端关闭时需要关闭服务端的socketChannel
            if (count == -1) {
                logger.info("客户端连接关闭。。。");
                //客户端断开时，需要关闭服务端的SocketChannel
                socketChannel.close();
                selectionKey.cancel();
                return;
            } else if (count == 0) {
                logger.warn("no data to read...");
                return;
            }
            String msg = new String(readBuf.array(), StandardCharsets.UTF_8).trim();
            logger.info("receive NioClient msg:{}", msg);
            //向客户端返回响应
            this.write(selectionKey, msg + ":nio server response");
        } catch (Exception e) {
            logger.error("read data from channel:{} fail", selectionKey, e);
            //注意这里不能在finally中关闭socketChannel，否则这个客户端对应的channel就关闭了，后续就不能使用了
            try {
                socketChannel.close();
            } catch (Exception e1) {
                logger.error("close socketChannel fail:", e1);
            }
            selectionKey.cancel();
        }
    }

    /**
     * 写消息至nio客户端
     *
     * @param selectionKey 客户端SocketChannel对应的唯一键
     * @param msg          消息内容
     */
    private void write(SelectionKey selectionKey, String msg) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        writeBuf.clear();
        writeBuf.put(msg.getBytes());
        //将buffer置为读模式，即从buffer中读取数据写到channel中
        writeBuf.flip();
        try {
            socketChannel.write(writeBuf);
        } catch (Exception e) {
            logger.error("write response to NioClient:{} fail:", selectionKey, e);
            //注意这里不能在finally中关闭socketChannel，否则这个客户端对应的channel就关闭了，后续就不能在使用了
            try {
                socketChannel.close();
            } catch (Exception e1) {
                logger.error("close socketChannel fail:", e1);
            }
            selectionKey.cancel();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                //此处会阻塞直至有一个客户端Channel有就绪事件
                selector.select();
                Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
                //对每一个准备就绪的channel进行操作
                while (selectionKeyIterator.hasNext()) {
                    SelectionKey selectionKey = selectionKeyIterator.next();
                    //删除此selectionKey，否则后续会一直有此selectionKey
                    selectionKeyIterator.remove();
                    if (selectionKey.isValid()) {
                        if (selectionKey.isAcceptable()) {
                            accept(selectionKey);
                        } else if (selectionKey.isReadable()) {
                            read(selectionKey);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("select ready channel fail:", e);
            }
        }
    }
}
