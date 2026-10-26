package team.luxinfine.rslauncher.server;

import team.luxinfine.authlib.GameProfile;
import team.luxinfine.authlib.SimpleJson;
import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Имплементация API лаунчера для серверной части игры через HTTP API
 * @see team.luxinfine.authlib.CommonAPI
 */
@Environment(TargetSide.SERVER)
public class ServerAPI {
    private static final Logger LOGGER = Logger.getLogger("AuthLib");

    private static final String BASE_URL = _baseUrl();
    private static final String ACCESS_TOKEN = _accessToken();

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    public static void init() {
        //no-op метод для инициализации URL и токена
    }

    @Nullable
    public static GameProfile has_joined(@Nonnull String user, boolean by_uuid, @Nonnull String serverID) {
        Map<String, String> query = new HashMap<>();
        query.put("server_id", serverID);
        query.put("player", user);
        query.put("by_uuid", String.valueOf(by_uuid));

        try {
            String json = request("GET", "/session/has_joined", query, null);
            if (json == null) {
                return null;
            }
            return parseProfile(SimpleJson.parse(json));
        } catch (Exception e) {
            LOGGER.warning("Error while handling check_join: " + e);
            return null;
        }
    }

    @Nonnull
    public static Map<String, GameProfile> lookupProfiles(@Nonnull List<String> users, boolean by_uuid) {
        StringBuilder body = new StringBuilder();
        body.append("{\"by_uuid\":").append(by_uuid).append(",\"ids\":[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) {
                body.append(',');
            }
            body.append(SimpleJson.quote(users.get(i)));
        }
        body.append("]}");

        try {
            String json = request("POST", "/session/profiles", null, body.toString());
            if (json == null) {
                return new HashMap<>();
            }
            return parseProfiles(SimpleJson.parse(json));
        } catch (Exception e) {
            LOGGER.warning("Error while handling lookupProfiles: " + e);
            return new HashMap<>();
        }
    }

    @Nullable
    private static GameProfile parseProfile(@Nullable Object node) {
        if (node == null) {
            return null;
        }
        Map<String, Object> obj = asObject(node);
        String id = requireString(obj, "id");
        String name = requireString(obj, "name");
        return new GameProfile(UUID.fromString(id), name, parseProperties(obj.get("properties")));
    }

    @Nonnull
    private static Map<String, GameProfile> parseProfiles(@Nullable Object node) {
        Map<String, GameProfile> result = new HashMap<>();
        if (node == null) {
            return result;
        }
        Map<String, Object> root = asObject(node);
        Object profiles = root.get("profiles");
        if (!(profiles instanceof Map)) {
            return result;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) profiles).entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                continue;
            }
            try {
                GameProfile profile = parseProfile(entry.getValue());
                if (profile != null) {
                    result.put((String) entry.getKey(), profile);
                }
            } catch (RuntimeException e) {
                LOGGER.warning("Skipped malformed profile '" + entry.getKey() + "': " + e);
            }
        }
        return result;
    }

    @Nonnull
    private static Map<String, String> parseProperties(@Nullable Object node) {
        Map<String, String> properties = new HashMap<>();
        if (!(node instanceof Map)) return properties;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet())
            if (entry.getKey() instanceof String && entry.getValue() instanceof String)
                properties.put((String) entry.getKey(), (String) entry.getValue());
        return properties;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object node) {
        if (!(node instanceof Map))
            throw new IllegalArgumentException("Expected JSON object but got " + node);
        return (Map<String, Object>) node;
    }

    @Nonnull
    private static String requireString(@Nonnull Map<String, Object> obj, @Nonnull String field) {
        Object value = obj.get(field);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Expected string field '" + field + "' but got " + value);
        }
        return (String) value;
    }

    @Nullable
    private static String request(
            String method, String path, @Nullable Map<String, String> query, @Nullable String body
    ) throws IOException {
        HttpURLConnection conn = null;
        try {
            StringBuilder url = new StringBuilder();
            url.append(BASE_URL);
            url.append(path);
            if (query != null && !query.isEmpty()) {
                url.append('?');
                boolean first = true;
                for (Map.Entry<String, String> entry : query.entrySet()) {
                    if (!first) {
                        url.append('&');
                    }
                    first = false;
                    url.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    url.append('=');
                    url.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
            }

            conn = (HttpURLConnection) new URL(url.toString()).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);

            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = conn.getResponseCode();
            if (status != 200)
                throw new IOException("HTTP error " + status);

            try (InputStream in = conn.getInputStream()) {
                String json = readAll(in);
                if (json.isEmpty()) {
                    return null;
                }
                return json;
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4096];
        int read;
        while ((read = in.read(buf)) != -1) {
            sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String _baseUrl() {
        String value = System.getProperty("LFLauncher.Server.ApiUrl");
        if (value == null || value.trim().isEmpty()) {
            LOGGER.log(Level.SEVERE, "JVM arg -DLFLauncher.Server.ApiUrl not present!");
            System.exit(1);
        }
        return value.trim().replaceAll("/+$", "");
    }

    private static String _accessToken() {
        String value = System.getProperty("LFLauncher.Server.AccessToken");
        if (value == null || value.trim().isEmpty()) {
            LOGGER.log(Level.SEVERE, "JVM arg -DLFLauncher.Server.AccessToken not present!");
            System.exit(1);
        }
        return value.trim();
    }
}
