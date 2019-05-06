package tz.co.nezatech.dsetp.util;

import tz.co.nezatech.dsetp.util.db.ConnectionPool;
import tz.co.nezatech.dsetp.util.model.Contract;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Properties;

public class TestApp {
    public static void main(String[] args) throws IOException {
        /*Properties props = new Properties();
        props.setProperty("jdbcUrl", "jdbc:mysql://localhost:3306/agm?useSSL=false");
        props.setProperty("dataSource.user", "agm");
        props.setProperty("dataSource.password", "agm");
        props.setProperty("dataSource.cachePrepStmts", "true");
        props.setProperty("dataSource.prepStmtCacheSize", "250");
        props.setProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        ConnectionPool.configure(props);

        Contract c = new Contract("Test");
        */
        String key = "<RSAKeyValue><Modulus>vcBbUDIROaZu92FdNNKdwJBxpOFKmrcgtbMDtAdzN/gnulLX1hezoRcbIzEIQaEig33Q78yIGrjBJx9oDoIYt5HB2hHclWNgav4Su58soTu46hTK2ee89vtHn4rs8kwvr6pJ/a4bkb9l09Vd8MuJoMcwoMgxA7VpcDH6Petlixc=</Modulus><Exponent>AQAB</Exponent></RSAKeyValue>";
        String data = "80:00:00:00:11:25:52:02:60:a9:2e:ff:e7:e4:61:42:42:1f:d7:67:fc:72:3d:a6:29:29:6b:ee:cf:fe:40:1b:cb:a5:46:7b:55:61:32:c1:49:9e:29:09:ed:12:22:c4:11:cd:f0:c5:b2:2f:a1:a7:ad:4a:4b:ba:f8:28:7e:cd:c8:55:aa:3b:2f:47:b8:00:f2:9e:95:34:27:bc:ac:a4:8b:84:82:24:6c:15:2e:2e:06:c2:cf:9c:c3:1b:9a:7a:b2:7f:7f:67:a1:f1:16:2b:4a:5f:57:3b:51:fc:63:ab:7d:c9:5a:04:c1:4a:92:a5:e5:5b:58:5c:d9:ce:8d:80:81:ca:8f:e9:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00";

        try {
            byte[] bytes = RSAUtil.encryptFromXmlKey("Pass@123", key);
            System.out.println(TCPUtil.text(withLen(new String(bytes))));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static byte[] withLen(String text) {
        byte[] bytes = text.getBytes();
        byte[] res = new byte[bytes.length + 4];

        System.arraycopy(TCPUtil.getBytes(bytes.length, true), 0, res, 0, 4);
        System.arraycopy(bytes, 0, res, 4, bytes.length);

        return res;
    }
}


