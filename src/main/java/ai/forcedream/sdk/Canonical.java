package ai.forcedream.sdk;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exact replica of the server's wfCanonical: JSON.stringify(obj, Object.keys(obj).sort()).
 * Sorted keys, no whitespace. Ported verbatim from the real, published @forcedream/sdk's
 * canonical.js, which is itself a verbatim port of the server's own canonical.ts -- not
 * rewritten from memory.
 *
 * Uses a custom, minimal serializer rather than a general-purpose JSON library for this
 * specific step, since exact byte-for-byte output matters here (a single differing byte
 * changes the signed bytes and breaks every signature check). A general JSON library's
 * default number formatting was directly tested and confirmed unsafe before writing this --
 * see jsNumber below.
 */
public final class Canonical {
    private Canonical() {}

    public static String wfCanonical(Map<String, Object> obj) {
        TreeMap<String, Object> sorted = new TreeMap<>(obj);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append("\":");
            sb.append(serializeValue(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    private static String serializeValue(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return '"' + escape(s) + '"';
        if (v instanceof Number n) return jsNumber(n.doubleValue());
        if (v instanceof Boolean b) return b.toString();
        throw new IllegalArgumentException("Unsupported type for canonicalization: " + v.getClass());
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Mirrors JS's Number(x) -> JSON.stringify() behavior: whole values with no decimal
     * point, fractional values preserved, never scientific notation. Directly tested before
     * this was written: Java's default Double.toString() produces scientific notation for
     * anything above ~10^7 (e.g. "1.783860125E9"), which would silently corrupt every real
     * timestamp field and break every signature -- a more severe version of the same bug
     * class that required fixes in every other language SDK tonight. Also tested against
     * BigDecimal.stripTrailingZeros()'s own known gotcha with trailing zeros (100, 1000)
     * before relying on it.
     */
    public static String jsNumber(double d) {
        BigDecimal bd = BigDecimal.valueOf(d);
        if (bd.stripTrailingZeros().scale() <= 0) {
            return bd.toBigInteger().toString();
        }
        return bd.stripTrailingZeros().toPlainString();
    }

    public static String sha256Hex(String s) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(s.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
