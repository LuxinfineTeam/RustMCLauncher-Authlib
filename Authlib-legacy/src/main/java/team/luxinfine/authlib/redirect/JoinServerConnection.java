package team.luxinfine.authlib.redirect;

import team.luxinfine.authlib.CommonAPI;
import team.luxinfine.mcsplitter.api.Environment;
import team.luxinfine.mcsplitter.api.TargetSide;

import java.net.URL;

/**
 * Обработка подключения клиента к игровому серверу. На вход приходит ссылка в формате<BR>
 * {@code http://session.minecraft.net/game/joinserver.jsp?user=${URLEncoder.encode(username)}&sessionId=${URLEncoder.encode(session)}&serverId=${URLEncoder.encode(serverId)}}
 * Ожидаемый ответ "ok" в случае успеха или строка с ошибкой
 */
@Environment(TargetSide.CLIENT)
final class JoinServerConnection extends CustomHttpConnection {
    static final String JOIN_PATH = "/game/joinserver.jsp";

    JoinServerConnection(URL url) {
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

        try {
            CommonAPI.joinMCServer(serverId);
        } catch (Throwable e) {
            respond(e.toString());
            return;
        }

        respond("ok");
    }
}
