package com.travelsky.base.repository.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * filter DSL 词法解析器（上提自 jpa / mybatis 模块的同名内部类，逻辑保持一致）。
 * <p>将 {@code [[name,=,john]]} 形式字符串解析为嵌套结构化列表；
 * 语法归一见 {@link FilterParser}。
 */
public final class FilterStringParser {

    private FilterStringParser() {
    }

    /**
     * 入口方法：将字符串解析为结构化条件。
     */
    public static List<Object> parse(String filter) {
        filter = filter.trim();
        if (!filter.startsWith(FilterSymbolConst.LEFT_BUCKET_STR) || !filter.endsWith(FilterSymbolConst.RIGHT_BUCKET_STR)) {
            throw new IllegalArgumentException("Invalid filter format: must start with '[' and end with ']'");
        }
        return parseList(filter, 1, filter.length() - 1);
    }

    // 递归解析列表
    private static List<Object> parseList(String filter, int start, int end) {
        List<Object> result = new ArrayList<>();
        int index = start;
        while (index < end) {
            char ch = filter.charAt(index);
            if (ch == FilterSymbolConst.LEFT_BUCKET) {
                // 嵌套列表
                int closeIndex = findClosingBracket(filter, index);
                result.add(parseList(filter, index + 1, closeIndex));
                index = closeIndex + 1;
            } else if (ch == FilterSymbolConst.COMMA || ch == ' ') {
                // 分隔符，跳过
                index++;
            } else if (ch == FilterSymbolConst.RIGHT_BUCKET) {
                // 列表结束
                break;
            } else {
                // 解析字符串（条件或逻辑符）
                int nextComma = filter.indexOf(FilterSymbolConst.COMMA, index);
                int nextBracket = filter.indexOf(']', index);
                int next = Math.min(nextComma == -1 ? Integer.MAX_VALUE : nextComma, nextBracket == -1 ? Integer.MAX_VALUE : nextBracket);
                if (next == Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Invalid filter format: missing ',' or ']'");
                }
                String token = filter.substring(index, next).trim();
                if (!token.isEmpty()) {
                    result.add(token);
                }
                index = next;
            }
        }
        return result;
    }

    // 找到匹配的闭合括号
    private static int findClosingBracket(String filter, int start) {
        Stack<Character> stack = new Stack<>();
        stack.push(FilterSymbolConst.LEFT_BUCKET);
        for (int i = start + 1; i < filter.length(); i++) {
            char ch = filter.charAt(i);
            if (ch == FilterSymbolConst.LEFT_BUCKET) {
                stack.push(FilterSymbolConst.LEFT_BUCKET);
            } else if (ch == FilterSymbolConst.RIGHT_BUCKET) {
                stack.pop();
                if (stack.isEmpty()) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Invalid filter format: unmatched '['");
    }
}
