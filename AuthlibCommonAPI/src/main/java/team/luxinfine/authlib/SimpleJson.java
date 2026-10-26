package team.luxinfine.authlib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Простой парсер JSON для разбора ответов launcher server
//В отличии от полноценного парсера, этот НЕ делает жесткую проверку целостности json и может читать даже
//частично поврежденный json, тк глубокая валидая нам попросту не требуется.
//В старых версиях игры, где-то до 1.7.10, нет GSON из коробки, именно потому сделан такой ручной аналог
public final class SimpleJson {

    /**
     * Парсит json строку, возвращает прочитанное значение. JsonArray преобразуется в List, JsonObject в Map
     */
    public static Object parse(String json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON string is null");
        }
        Parser parser = new Parser(json);
        parser.skipWhitespace();
        if (parser.eof()) {
            throw parser.error("empty input");
        }
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.eof()) {
            throw parser.error("unexpected trailing characters");
        }
        return value;
    }

    /**
     * Экранирует строку и оборачивает её в кавычки.
     * Используется для сборки тела запроса
     */
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        appendEscaped(sb, value);
        sb.append('"');
        return sb.toString();
    }

    private static void appendEscaped(StringBuilder sb, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int pad = hex.length(); pad < 4; pad++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
    }

    private static final class Parser {
        private final String src;
        private int pos;

        private Parser(String src) {
            this.src = src;
        }

        private boolean eof() {
            return pos >= src.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException("Malformed JSON at index " + pos + ": " + message);
        }

        private void skipWhitespace() {
            while (!eof()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private char peek() {
            if (eof()) {
                throw error("unexpected end of input");
            }
            return src.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        private void expect(char expected) {
            char c = next();
            if (c != expected) {
                throw error("expected '" + expected + "' but got '" + c + "'");
            }
        }

        private Object parseValue() {
            char c = peek();
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                    return parseLiteral("true", Boolean.TRUE);
                case 'f':
                    return parseLiteral("false", Boolean.FALSE);
                case 'n':
                    return parseLiteral("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return parseNumber();
                    }
                    throw error("unexpected character '" + c + "'");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!src.startsWith(literal, pos)) {
                throw error("unexpected token, expected '" + literal + "'");
            }
            pos += literal.length();
            return value;
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("expected a string key");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, parseValue());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error("expected ',' or '}' but got '" + c + "'");
                }
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw error("expected ',' or ']' but got '" + c + "'");
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    //Управляющие символы внутри строки пропускаем как есть (GSON тоже разборчик ленивый)
                    sb.append(c);
                    continue;
                }
                char escaped = next();
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 > src.length()) {
                            throw error("truncated unicode escape");
                        }
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw error("invalid unicode escape '\\u" + hex + "'");
                        }
                        break;
                    default:
                        throw error("invalid escape '\\" + escaped + "'");
                }
            }
        }

        private Object parseNumber() {
            int start = pos;
            if (peek() == '-') {
                pos++;
            }
            while (!eof() && src.charAt(pos) >= '0' && src.charAt(pos) <= '9') {
                pos++;
            }
            boolean fractional = false;
            if (!eof() && src.charAt(pos) == '.') {
                fractional = true;
                do {
                    pos++;
                } while (!eof() && src.charAt(pos) >= '0' && src.charAt(pos) <= '9');
            }
            if (!eof() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                fractional = true;
                pos++;
                if (!eof() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                    pos++;
                }
                while (!eof() && src.charAt(pos) >= '0' && src.charAt(pos) <= '9') {
                    pos++;
                }
            }
            String text = src.substring(start, pos);
            if (text.isEmpty() || text.equals("-")) {
                throw error("invalid number");
            }
            if (!fractional) {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException overflow) {
                    //Слишком большое для long — отдаём как double
                }
            }
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException e) {
                throw error("invalid number '" + text + "'");
            }
        }
    }
}
