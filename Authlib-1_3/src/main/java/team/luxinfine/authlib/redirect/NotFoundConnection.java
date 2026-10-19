package team.luxinfine.authlib.redirect;

import java.net.URL;

public class NotFoundConnection extends CustomHttpConnection {

    NotFoundConnection(URL url) {
        super(url);
    }

    @Override
    protected void resolve() {
        fail(404, "HTTP 404 Not Found");
    }
}
