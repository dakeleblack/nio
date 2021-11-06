package bio;

/**
 * @author bitao
 * @date 2021/11/2 下午3:43
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public class BioClient {
    private static Logger logger = LoggerFactory.getLogger(BioClient.class);

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 9000);
        //向服务端发送数据
        socket.getOutputStream().write("HelloServer".getBytes());
        socket.getOutputStream().flush();
        logger.info("客户端向服务端发送数据结束");
        byte[] bytes = new byte[1024];
        //接收服务端回传的数据
        int read = socket.getInputStream().read(bytes);
        if (read != -1) {
            logger.info("服务端接收到服务端的数据：" + new String(bytes, 0, read));
        }
        //关闭客户端
        socket.close();
    }
}

