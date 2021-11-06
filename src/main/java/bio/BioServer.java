package bio;

/**
 * @author bitao
 * @date 2021/11/2 下午3:35
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BioServer {
    private static Logger logger = LoggerFactory.getLogger(BioServer.class);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9000);
        while (true) {
            logger.info("等待客户端连接.....");
            //阻塞方法
            final Socket socket = serverSocket.accept();
            logger.info("有客户端连接了.....");
            //有一个客户端连接就启动一个线程处理该客户端的IO事件
            new Thread(() -> {
                try {
                    handle(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * 服务端处理客户端端IO事件
     * @param socket
     * @throws IOException
     */
    private static void handle(Socket socket) throws IOException {
        logger.info("thread id = " + Thread.currentThread().getId());
        byte[] bytes = new byte[1024];
        logger.info("服务端准备读取客户端数据");
        //接收客户端的数据，阻塞方法，没有数据可读时就阻塞
        int read = socket.getInputStream().read(bytes);
        logger.info("服务端读取客户端数据完毕");
        if (read != -1) {
            logger.info("服务端接收到客户端的数据：" + new String(bytes, 0, read));
            logger.info("thread id = " + Thread.currentThread().getId());
        } else {
            //客户端关闭时也会发送一个IO读事件
            logger.warn("客户端关闭");
        }
        logger.info("服务端发送数据至客户端");
        socket.getOutputStream().write("HelloClient".getBytes());
        socket.getOutputStream().flush();
    }
}

