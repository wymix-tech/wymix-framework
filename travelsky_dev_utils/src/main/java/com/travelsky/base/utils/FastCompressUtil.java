package com.travelsky.base.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class FastCompressUtil {
    private static final int COMPRESS_THRESHOLD = 100;
    private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(() -> new Deflater(1, true));
    private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(() -> new Inflater(true));

    public static String compress(String str) {
        if (str == null || str.length() <= COMPRESS_THRESHOLD) {
            return str;
        }
        byte[] input = str.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = DEFLATER.get();
        deflater.reset();
        deflater.setInput(input);
        deflater.finish();

        byte[] buf = new byte[Math.min(65536, Math.max(1024, input.length))];
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                baos.write(buf, 0, n);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decompress(String compressed) {
        if (compressed == null || compressed.length() <= COMPRESS_THRESHOLD) return compressed;
        try {
            byte[] data = Base64.getDecoder().decode(compressed);
            Inflater inflater = INFLATER.get();
            inflater.reset();
            inflater.setInput(data);

            byte[] buf = new byte[65536];
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                while (!inflater.finished()) {
                    int n = inflater.inflate(buf);
                    baos.write(buf, 0, n);
                }
                return baos.toString(String.valueOf(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }
}
