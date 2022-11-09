package moe.yushi.authlibinjector.httpd;

public class InvalidSessionException extends Exception {
    public InvalidSessionException(String message) {
        super(message);
    }
}
