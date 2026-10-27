package team.luxinfine.authlib;

/// Исключение при блокировке попытки входа на сервер со стороны плагинов лаунчер-сервера
public class ServerJoinError extends Exception {
    public ServerJoinError(String message) {
        super(message);
    }
}
