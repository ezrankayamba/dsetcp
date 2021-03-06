package tz.co.nezatech.dsetp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import tz.co.nezatech.dsetp.util.config.Config;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Base64;

public class RSAUtil {

    private static String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCgFGVfrY4jQSoZQWWygZ83roKXWD4YeT2x2p41dGkPixe73rT2IW04glagN2vgoZoHuOPqa5and6kAmK2ujmCHu6D1auJhE2tXP+yLkpSiYMQucDKmCsWMnW9XlC5K7OSL77TXXcfvTvyZcjObEz6LIBRzs6+FqpFbUO9SJEfh6wIDAQAB";
    private static String privateKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAKAUZV+tjiNBKhlBZbKBnzeugpdYPhh5PbHanjV0aQ+LF7vetPYhbTiCVqA3a+Chmge44+prlqd3qQCYra6OYIe7oPVq4mETa1c/7IuSlKJgxC5wMqYKxYydb1eULkrs5IvvtNddx+9O/JlyM5sTPosgFHOzr4WqkVtQ71IkR+HrAgMBAAECgYAkQLo8kteP0GAyXAcmCAkA2Tql/8wASuTX9ITD4lsws/VqDKO64hMUKyBnJGX/91kkypCDNF5oCsdxZSJgV8owViYWZPnbvEcNqLtqgs7nj1UHuX9S5yYIPGN/mHL6OJJ7sosOd6rqdpg6JRRkAKUV+tmN/7Gh0+GFXM+ug6mgwQJBAO9/+CWpCAVoGxCA+YsTMb82fTOmGYMkZOAfQsvIV2v6DC8eJrSa+c0yCOTa3tirlCkhBfB08f8U2iEPS+Gu3bECQQCrG7O0gYmFL2RX1O+37ovyyHTbst4s4xbLW4jLzbSoimL235lCdIC+fllEEP96wPAiqo6dzmdH8KsGmVozsVRbAkB0ME8AZjp/9Pt8TDXD5LHzo8mlruUdnCBcIo5TMoRG2+3hRe1dHPonNCjgbdZCoyqjsWOiPfnQ2Brigvs7J4xhAkBGRiZUKC92x7QKbqXVgN9xYuq7oIanIM0nz/wq190uq0dh5Qtow7hshC/dSK3kmIEHe8z++tpoLWvQVgM538apAkBoSNfaTkDZhFavuiVl6L8cWCoDcJBItip8wKQhXwHp0O3HLg10OEd14M58ooNfpgt+8D8/8/2OOFaR0HzA+2Dm";

    public static PublicKey getPublicKey(String base64PublicKey) {
        PublicKey publicKey;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String fromHex(String s) throws UnsupportedEncodingException {
        byte bs[] = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            bs[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return new String(bs, "UTF8");
    }


    public static String getPublicKeyAsXml(RSAPublicKey pk) {
        String xml = null;
        try {

            org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
            ObjectMapper mapper = new XmlMapper();
            RSAKeyValue rsa = new RSAKeyValue();
            rsa.setExponent(new String(base64.encode(pk.getPublicExponent().toByteArray())));
            rsa.setModulus(new String(base64.encode(pk.getModulus().toByteArray())));
            xml = mapper.writeValueAsString(rsa);
            return xml;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return xml;
    }

    public static PrivateKey getPrivateKey(String base64PrivateKey) {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey.getBytes()));
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privateKey;
    }


    public static byte[] encrypt(String data, String publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        return cipher.doFinal(data.getBytes());
    }

    public static byte[] encryptFromXmlKey(String data, String publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        return encryptFromXmlKey(data, publicKey, "RSA", "RSA");
    }

    public static byte[] encryptFromXmlKey(String data, String publicKey, String rsa, String rsaKeyfactory) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance(rsa);
        PublicKey pub = getPublicKeyXml(publicKey, rsaKeyfactory);
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        return cipher.doFinal(data.getBytes());
    }

    public static PublicKey getPublicKeyXml(String xml) {
        return getPublicKeyXml(xml, "RSA");
    }

    public static PublicKey getPublicKeyXml(String xml, String rsaKeyfactory) {
        PublicKey publicKey = null;
        try {

            org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
            XmlMapper mapper = new XmlMapper();
            RSAKeyValue rsa = mapper.readValue(xml, RSAKeyValue.class);
            byte[] mod = base64.decode(rsa.getModulus());
            byte[] exp = base64.decode(rsa.getExponent());
            BigInteger modules = new BigInteger(1, mod);
            BigInteger exponent = new BigInteger(1, exp);
            KeyFactory factory = KeyFactory.getInstance(rsaKeyfactory);
            RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(modules, exponent);
            publicKey = factory.generatePublic(pubSpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return publicKey;
    }

    public static String decrypt(byte[] data, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(data));
    }

    public static String decrypt(String data, String base64PrivateKey) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return decrypt(Base64.getDecoder().decode(data.getBytes()), getPrivateKey(base64PrivateKey));
    }

    public static void main(String[] args) throws IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException {
        try {
            /*String encryptedString = Base64.getEncoder().encodeToString(encrypt("Dhiraj is the author", publicKey));
            System.out.println(encryptedString);
            String decryptedString = RSAUtil.decrypt(encryptedString, privateKey);
            System.out.println(decryptedString);*/
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<RSAKeyValue>\n" +
                    "   <Modulus>44nt+m2mDwwteh30v+FdJQE0sT+7Rz0+MDFhFVHpDvChUwab5\n" +
                    "v0HxN4S2mfyfgyFpXh5AFTiu1eDZFzixOGbEgYNG/jbx1k2yh72oeI4LKIuypsoeGqFccI6eP7WF5o2t\n" +
                    "S1XGYvpEWwZpuSnXt6CLANMLEn64PC1RPpXzFiIAOs=</Modulus>\n" +
                    "   <Exponent>AQAB</Exponent>\n" +
                    "</RSAKeyValue>";
            PublicKey publicKeyXml = getPublicKeyXml(xml);
            String s = publicKeyXml.toString();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}