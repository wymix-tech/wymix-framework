package com.travelsky.base.utils;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @deprecated 与 {@link CompressUtil} 功能重叠，请统一迁移到 {@link CompressUtil}。
 * 本类仅为存量代码提供兼容（内部直接委托 {@link CompressUtil}），计划下个大版本移除。
 *
 * <p>行为差异说明：本类历史上自带「短字符串（≤100 字符）不压缩」阈值与 Deflater 复用优化，
 * 合并后统一走 {@link CompressUtil} 的 GZIP 实现，压缩/解压结果与 {@link CompressUtil} 完全一致。</p>
 */
@Deprecated
public class FastCompressUtil {

    private FastCompressUtil() {
    }

    public static String compress(String str) {
        try {
            return CompressUtil.compress(str);
        } catch (IOException e) {
            // GZIP 内存流写入不会实际抛出 IOException，与旧实现兜底语义一致
            throw new UncheckedIOException(e);
        }
    }

    public static String decompress(String compressed) {
        try {
            return CompressUtil.decompress(compressed);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
