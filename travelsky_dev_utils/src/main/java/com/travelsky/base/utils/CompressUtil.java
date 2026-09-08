package com.travelsky.base.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 字符串 GZIP 压缩/解压工具（框架内唯一的压缩实现）。
 */
public final class CompressUtil {

    private CompressUtil() {
    }

    public static String compress(String string) throws IOException {
        if (StringUtils.isBlank(string)) {
            return string;
        }
        String dest;
        byte[] byteArray;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();){
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(string.getBytes(StandardCharsets.UTF_8));
            }
            byteArray = out.toByteArray();
        }

        dest = Base64.getEncoder().encodeToString(byteArray);
        return dest;
    }

    /**
     * uncompress
     *
     * @param string string
     * @return String
     * @throws IOException
     */
    public static String decompress(String string) throws IOException {
        if (StringUtils.isBlank(string)) {
            return string;
        }
        byte[] byteArray;
        try {
            byteArray = Base64.getDecoder().decode(string);
        } catch (Exception e) {
            return string;
        }
        String dest;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
             GZIPInputStream gzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = gzip.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            dest = out.toString(StandardCharsets.UTF_8.name());
        }
        return dest;
    }
}
