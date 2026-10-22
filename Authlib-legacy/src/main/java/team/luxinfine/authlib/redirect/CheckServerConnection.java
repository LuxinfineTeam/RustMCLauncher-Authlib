package team.luxinfine.authlib.redirect;

import team.luxinfine.authlib.GameProfile;
import team.luxinfine.authlib.CommonAPI;
import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import java.net.URL;

/**
 * Обработка серверного запроса на проверку входа игрока. На вход приходит ссылка в формате<BR>
 * {@code http://session.minecraft.net/game/checkserver.jsp?user=${URLEncoder.encode(username)}&serverId=${URLEncoder.encode(serverId)}}
 * Ожидаемый ответ "YES" в случае успеха. Любой другой ответ отклоняет соединение и не выводится никуда
 */
@Environment(TargetSide.SERVER)
final class CheckServerConnection extends CustomHttpConnection {
    static final String CHECK_PATH = "/game/checkserver.jsp";

    CheckServerConnection(URL url) {
        super(url);
    }

    @Override
    protected void resolve() {
        String user = parameters().get("user");
        String serverId = parameters().get("serverId");
        if (user == null || serverId == null) {
            fail(HTTP_BAD_REQUEST, "HTTP 400 Bad Request: Missing 'user' or 'serverId' parameter");
            return;
        }

        GameProfile profile = CommonAPI.has_joined(user, false, serverId);
        respond(profile == null ? "NO" : "YES");
    }
}
