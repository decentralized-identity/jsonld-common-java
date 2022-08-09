package foundation.identity.jsonld;

public class JsonLDDereferencingException extends RuntimeException {

    public JsonLDDereferencingException() {
    }

    public JsonLDDereferencingException(String message) {
        super(message);
    }

    public JsonLDDereferencingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonLDDereferencingException(Throwable cause) {
        super(cause);
    }

    public JsonLDDereferencingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
