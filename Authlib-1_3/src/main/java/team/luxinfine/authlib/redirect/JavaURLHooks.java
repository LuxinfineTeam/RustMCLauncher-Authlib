package team.luxinfine.authlib.redirect;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public final class JavaURLHooks {

    //Вызывается рефлексией до запуска сервера/клиента
    public static void init() {
        URL.setURLStreamHandlerFactory(protocol -> {
            //legacy версии игры для mojang api использовали только http протокол
            if (protocol.equals("http"))
                return new HTTPHandler();
            return null;
        });
    }

    private static final class HTTPHandler extends sun.net.www.protocol.http.Handler {
        @Override
        protected URLConnection openConnection(URL u, Proxy p) throws IOException {
            URLConnection redirect = MojangAPIRedirect.hookMinecraftHttpAPI(u);
            if (redirect != null)
                return redirect;
            return super.openConnection(u, p);
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return openConnection(u, null);
        }
    }
}
