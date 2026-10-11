package team.luxinfine.authlib;

import com.google.gson.*;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(TargetSide.SERVER)
public class ServerAPI {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private static final String BASE_URL = _baseUrl();
    private static final String ACCESS_TOKEN = _accessToken();

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    @Nullable
    public static GameProfile has_joined(@Nonnull String user, boolean by_uuid, @Nonnull String serverID) {
        Map<String, String> query = new HashMap<>();
        query.put("server_id", serverID);
        query.put("player", user);
        query.put("by_uuid", String.valueOf(by_uuid));

        try {
            JsonElement result = request("GET", "/session/has_joined", query, null);
            return GSON.fromJson(result, GameProfile.class);
        } catch (Exception e) {
            LOGGER.warn("Error while handling check_join: {}", e.toString());
            return null;
        }
    }

    @Nonnull
    public static Map<String, GameProfile> lookupProfiles(@Nonnull List<String> users, boolean by_uuid) {
        JsonObject req = new JsonObject();
        req.addProperty("by_uuid", by_uuid);
        JsonArray ids = new JsonArray();
        for (String u : users) {
            ids.add(new JsonPrimitive(u));
        }
        req.add("ids", ids);

        try {
            JsonElement result = request("POST", "/session/profiles", null, req.toString());
            return GSON.fromJson(result, BulkProfilesResponse.class).profiles;
        } catch (Exception e) {
            LOGGER.warn("Error while handling lookupProfiles: {}", e.toString());
            return new HashMap<>();
        }
    }

    static final class BulkProfilesResponse {
        public Map<String, GameProfile> profiles;
    }

    private static JsonElement request(
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
                return new JsonParser().parse(json);
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
            LOGGER.error("[Authlib] Не задан JVM аргумент -DLFLauncher.Server.ApiUrl");
            System.exit(1);
        }
        return value.trim().replaceAll("/+$", "");
    }

    private static String _accessToken() {
        String value = System.getProperty("LFLauncher.Server.AccessToken");
        if (value == null || value.trim().isEmpty()) {
            LOGGER.error("[Authlib] Не задан JVM аргумент -DLFLauncher.Server.AccessToken");
            System.exit(1);
        }
        return value.trim();
    }
}
