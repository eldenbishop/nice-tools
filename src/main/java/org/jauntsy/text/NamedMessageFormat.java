package org.jauntsy.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Created by ebishop on 3/12/15.
 */
public class NamedMessageFormat {

    public static final Function<Object, String> DEFAULT_SERIALIZER = new CleanSerializer();

    private final String pattern;
    private final Function<Object,String> serializer;

    public NamedMessageFormat(String pattern) {
        this(pattern, DEFAULT_SERIALIZER);
    }

    public NamedMessageFormat(String pattern, Function<Object,String> serializer) {
        this.pattern = pattern;
        this.serializer = serializer;
    }

    public String format(Map properties) {
        return format(pattern, properties, serializer);
    }

    /**
     * Creates a NamedMessageFormat with the given pattern and uses it
     * to format the given properties.
     *
     * @param pattern   the pattern string
     * @param properties object(s) to format
     * @return the formatted string
     * @exception IllegalArgumentException if the pattern is invalid,
     *            or if an argument in the <code>properties</code> map
     *            is not of the type expected by the format element(s)
     *            that use it.
     */
    public static String format(String pattern, Map<String,Object> properties) {
        return format(pattern, properties, DEFAULT_SERIALIZER);
    }

    public static String format(String pattern, Map<String,Object> properties, Function<Object,String> serializer) {
        try {
            Map<String,Object> flattened = flatten(properties, serializer);
            List<String> names = new ArrayList<>(flattened.keySet());
            String convertedPattern = convert(pattern, names);
            List<Object> args = new ArrayList<>();
            for (String name : names) {
                args.add(flattened.get(name));
            }
            try {
                return MessageFormat.format(convertedPattern, args.toArray());
            } catch(Exception ex) {
                throw new IllegalArgumentException("Error formatting: " + convertedPattern + ", with args: " + args, ex);
            }
        } catch(Exception ex) {
            throw new RuntimeException("Error formatting: " + pattern + ", with args: " + properties, ex);
        }
    }

    private static Map<String,Object> flatten(Map<String,Object> parameters, Function<Object,String> serializer) {
        Map<String,Object> result = new LinkedHashMap<>();
        flatten(result, null, parameters, serializer);
        return result;
    }

    private static void flatten(Map<String,Object> result, String context, Map parameters, Function<Object,String> serializer) {
        parameters.forEach((k, v) -> {
            if (k instanceof String) {
                String path = context == null ? ((String)k) : context + "." + k;
                if (isSimpleType(v)) {
                    result.put(path, v);
                } else {
                    result.put(path, serializer.apply(v));
                }
                if (v instanceof Map) {
                    flatten(result, path, (Map) v, serializer);
                }
            }
        });
    }

    public static String escapeSimpleMessage(@NotNull String message) {
        Objects.requireNonNull(message);
        StringBuilder sb = new StringBuilder();
        int mode = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (mode == 0) {
                if ('\'' == c) {
                    sb.append("''");
                } else if ('{' == c || '}' == c) {
                    sb.append("'");
                    sb.append(c);
                    mode = 1;
                } else {
                    sb.append(c);
                }
            } else { // in a quote
                if ('{' == c || '}' == c) {
                    sb.append(c);
                } else if ('\'' == c) {
                    sb.append("''");
                } else {
                    sb.append("'"); // end the quote
                    sb.append(c);
                    mode = 0;
                }
            }
        }
        if (mode == 1) { // finish the quote
            sb.append("'");
        }
        return sb.toString();
    }

    private enum MODE { DEFAULT, QUOTE, FIND_SYMBOL_START, SYMBOL_BUILDING, SYMBOL_NESTED }

    static String convert(String pattern, List<String> names) {
        StringBuilder sb = new StringBuilder();
        MODE mode = MODE.DEFAULT;
        String symbol = null;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            boolean hasNext = i < pattern.length() - 1;
            char next = hasNext ? pattern.charAt(i + 1) : 0;
            if (mode == MODE.DEFAULT) {
                if ('\'' == c) {
                    if (hasNext && '\'' == next) {
                        sb.append("''");
                        i++;
                    } else {
                        sb.append("'");
                        mode = MODE.QUOTE;
                    }
                } else if ('{' == c) {
                    sb.append('{');
                    mode = MODE.FIND_SYMBOL_START;
                } else {
                    sb.append(c);
                }
            } else if (mode == MODE.QUOTE) {
                if ('\'' == c) {
                    if (hasNext && '\'' == next) {
                        sb.append("''");
                        i++;
                    } else {
                        sb.append("'");
                        mode = MODE.DEFAULT;
                    }
                } else {
                    sb.append(c);
                }
            } else if (mode == MODE.FIND_SYMBOL_START) {
                if (Character.isWhitespace(c)) {
                    sb.append(c);
                } else if (Character.isJavaIdentifierStart(c)) {
                    symbol = "" + c;
                    mode = MODE.SYMBOL_BUILDING;
                } else {
                    throw new IllegalArgumentException(pattern);
                }
            } else if (mode == MODE.SYMBOL_NESTED) {
                if (Character.isJavaIdentifierStart(c)) {
                    symbol += c;
                    mode = MODE.SYMBOL_BUILDING;
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (mode == MODE.SYMBOL_BUILDING) {
                if (Character.isJavaIdentifierPart(c)) {
                    symbol += c;
                } else if ('.' == c) {
                    symbol += '.';
                    mode = MODE.SYMBOL_NESTED;
                } else {
                    if (!names.contains(symbol)) {
                        IllegalArgumentException ex = new IllegalArgumentException("Named parameter {" + symbol + "} is not one of " + names);
                        ex.printStackTrace();
                        throw ex;
                    }
                    int idx = names.indexOf(symbol);
                    sb.append(idx);
                    mode = MODE.DEFAULT;
                    symbol = null;
                    sb.append(c);
                }
            } else {
                throw new IllegalStateException();
            }
        }
        return sb.toString();
    }

    public static class CleanSerializer implements Function<Object,String> {

        @Override
        public String apply(Object o) {
            return formatValue(o);
        }

        @NotNull
        private static String formatValue(@Nullable Object value) {
            if (value == null || isSimpleType(value)) return String.valueOf(value);
            StringBuilder sb = new StringBuilder();
            formatValue(sb, value);
            return sb.toString();
        }

        private static void formatValue(@NotNull StringBuilder sb, Object value) {
            formatValue(sb, value, 0);
        }

        private static void formatValue(@NotNull StringBuilder sb, Object value, int depth) {
            if (value instanceof Collection) {
                if (depth > 0) sb.append("[");
                Collection c = (Collection)value;
                if (c.size() > 0) {
                    Iterator i = c.iterator();
                    formatValue(sb, i.next(), depth + 1);
                    while (i.hasNext()) {
                        sb.append(", ");
                        formatValue(sb, i.next(), depth + 1);
                    }
                }
                if (depth > 0) sb.append("]");
            } else if (value instanceof Map) {
                Map map = (Map)value;
                sb.append("[");
                if (map.size() == 0) {
                    sb.append(":");
                } else {
                    Set<String> keys = new TreeSet<>();
                    for (Object o : map.keySet())
                        if (o instanceof String)
                            keys.add((String)o);
                    if (keys.size() == 0) {
                        sb.append(":");
                    } else {
                        boolean first = true;
                        for (String key : keys) {
                            if (first) first = false;
                            else sb.append(", ");
                            sb.append(key);
                            sb.append(": ");
                            formatValue(sb, map.get(key), depth + 1);
                        }
                    }
                }
                sb.append("]");
            } else {
                sb.append(value);
            }
        }

    }

    private static boolean isSimpleType(@Nullable Object o) {
        return o == null || o instanceof String || o instanceof Number || o instanceof Boolean;
    }

}