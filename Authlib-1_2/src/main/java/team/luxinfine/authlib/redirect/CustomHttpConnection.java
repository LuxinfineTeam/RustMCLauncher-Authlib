package team.luxinfine.authlib.redirect;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

abstract class CustomHttpConnection extends HttpURLConnection {
    private final Map<String, String> parameters;

    private byte[] body;
    private int statusCode = HTTP_OK;
    private String failure;

    CustomHttpConnection(URL url) {
        super(url);
        this.parameters = parseParameters(url.getQuery());
    }

    /**
     * Выполняет запрос к эндпоинту, заполняя ответ через {@link #respond(String)}
     * либо ошибку через {@link #fail(int, String)}.
     */
    protected abstract void resolve();

    @Override
    public synchronized void connect() {
        if (connected)
            return;
        resolve();
        connected = true;
    }

    protected final Map<String, String> parameters() {
        return parameters;
    }

    protected final void respond(String text) {
        this.body = text.getBytes(StandardCharsets.UTF_8);
        this.statusCode = HTTP_OK;
        this.failure = null;
    }

    protected final void fail(int statusCode, String message) {
        this.statusCode = statusCode;
        this.failure = message;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        if (failure != null)
            throw new FileNotFoundException(failure);
        return new ByteArrayInputStream(body);
    }

    @Override
    public int getResponseCode() {
        connect();
        return statusCode;
    }

    @Override
    public String getResponseMessage() {
        connect();
        if (failure == null)
            return "HTTP/1.0 200 OK";
        if (statusCode == HTTP_BAD_REQUEST)
            return "HTTP/1.0 400 Bad Request";
        if (statusCode == HTTP_NOT_FOUND)
            return "HTTP/1.0 404 Not Found";
        return "HTTP/1.0 500 Internal Server Error";
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void disconnect() {
        connected = false;
        body = null;
        statusCode = HTTP_OK;
        failure = null;
    }

    @Override
    public String getContentType() {
        return failure != null ? null : "text/plain";
    }

    @Override
    public int getContentLength() {
        return body != null ? body.length : -1;
    }

    @Override
    public long getContentLengthLong() {
        return getContentLength();
    }

    @Override
    public String getHeaderField(String name) {
        if (name == null)
            return null;
        if ("content-type".equalsIgnoreCase(name))
            return getContentType();
        if ("content-length".equalsIgnoreCase(name) && body != null)
            return String.valueOf(body.length);
        return null;
    }

    // Разбор query параметров запроса
    private static Map<String, String> parseParameters(String query) {
        Map<String, String> parameters = new HashMap<>();
        if (query == null || query.isEmpty())
            return parameters;

        for (String pair : query.split("&")) {
            if (pair.isEmpty())
                continue;
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            parameters.put(decode(key), decode(value));
        }
        return parameters;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cant decode URL param '" + value + "'", e);
        }
    }
}
