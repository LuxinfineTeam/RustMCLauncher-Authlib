package team.luxinfine.authlib.redirect;

import team.luxinfine.authlib.GameProfile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

final class UserTextureConnection extends HttpURLConnection {
    private final String username;
    private final String propertyName;

    private InputStream stream;
    private int contentLength = -1;
    private String failure;

    UserTextureConnection(URL url, String username, String propertyName) {
        super(url);
        this.username = username;
        this.propertyName = propertyName;
    }

    @Override
    public synchronized void connect() throws IOException {
        if (connected)
            return;
        resolve();
        connected = true;
    }

    private void resolve() throws IOException {
        GameProfile profile = MojangAPIRedirect.lookupProfile(username);
        if (profile == null) {
            this.failure = "HTTP 404 Not Found: Player " + username + " not found";
            return;
        }

        String textureUrl = profile.properties.get(propertyName);
        if (textureUrl == null || textureUrl.trim().isEmpty()) {
            this.failure = "HTTP 404 Not Found: Player " + username + " has no " + propertyName;
            return;
        }

        URLConnection remote = new URL(textureUrl).openConnection();
        remote.setConnectTimeout(getConnectTimeout());
        remote.setReadTimeout(getReadTimeout());
        try {
            this.stream = remote.getInputStream();
        } catch (FileNotFoundException e) {
            this.failure = "HTTP 404 Not Found: Player " + username + " " + propertyName + " not available: " + e;
            return;
        }
        this.contentLength = remote.getContentLength();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        if (failure != null)
            throw new FileNotFoundException(failure);
        return stream;
    }

    @Override
    public int getResponseCode() throws IOException {
        connect();
        return failure != null ? HTTP_NOT_FOUND : HTTP_OK;
    }

    @Override
    public String getResponseMessage() throws IOException {
        connect();
        return failure != null ? "HTTP/1.0 404 Not Found" : "HTTP/1.0 200 OK";
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void disconnect() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
            stream = null;
        }
        connected = false;
        failure = null;
        contentLength = -1;
    }

    @Override
    public String getContentType() {
        return failure != null ? null : "image/png";
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public long getContentLengthLong() {
        return contentLength;
    }

    @Override
    public String getHeaderField(String name) {
        if (name == null)
            return null;
        if ("content-type".equalsIgnoreCase(name))
            return "image/png";
        if ("content-length".equalsIgnoreCase(name) && contentLength >= 0)
            return String.valueOf(contentLength);
        return null;
    }
}
