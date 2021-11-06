package aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author bitao
 * @date 2021/11/6 下午2:10
 */

public class AIOServer {

    private static final Logger logger = LoggerFactory.getLogger(AIOServer.class);

    public static void main(String[] args) throws Exception {
        //启动服务端
        final AsynchronousServerSocketChannel serverChannel =
                AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(9000));
        logger.info("服务端启动成功...");
        //等待接收客户端连接
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            //客户端与服务端连接建立后的回调函数，通知服务端，并由服务端调用
            @Override
            public void completed(final AsynchronousSocketChannel socketChannel, Object attachment) {
                try {
                    // 在此接收客户端连接，如果不写这行代码后面的客户端连接连不上服务端
                    serverChannel.accept(attachment, this);
                    logger.info("客户端地址:{}",socketChannel.getRemoteAddress());
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    //读取客户端发送的数据，若客户端关闭，AIO下的服务端不会接收到读事件
                    socketChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                        //读取数据完成后的回调函数，通知服务端，并由服务端调用
                        @Override
                        public void completed(Integer result, ByteBuffer buffer) {
                            if (result == -1) {
                                logger.warn("客户端已断开。。。");
                                try {
                                    socketChannel.close();
                                } catch (IOException e) {
                                    logger.error("关闭服务端的客户端连接失败");
                                }
                            }
                            //buffer缓冲区置为读模式
                            buffer.flip();
                            logger.info(new String(buffer.array(), 0, result));
                            //服务端向客户端发送消息
                            socketChannel.write(ByteBuffer.wrap("HelloClient".getBytes()));
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer buffer) {
                            exc.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });

        Thread.sleep(Integer.MAX_VALUE);
    }
}

