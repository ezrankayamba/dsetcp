package tz.co.nezatech.dsetp.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.nezatech.dsetp.util.message.StartOfDayDownload;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZipUtil {
    static Logger logger= LoggerFactory.getLogger(GZipUtil.class);

    public static void main(String[] args) {
        byte[] hex=TCPUtil.hexToBytes(TestConsts.msg36HexSpaced);
        byte[] bytes = StartOfDayDownload.decompress(hex);
        System.out.println(TCPUtil.text(bytes));
    }

    public static byte[] decompress(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
            logger.debug("Decompressed successfully");
        } catch (IOException e) {
            logger.debug("Decompression failed, could be not compressed. Returning original bytes");
            return contentBytes;
        }
        return out.toByteArray();
    }
}