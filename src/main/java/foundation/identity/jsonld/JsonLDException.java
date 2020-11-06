package foundation.identity.jsonld;

import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.api.JsonLdErrorCode;

public class JsonLDException extends Exception {

    private JsonLdError ex;

    public JsonLDException(JsonLdError ex) {
        super(ex.getMessage(), ex);
        this.ex = ex;
    }

    public JsonLdErrorCode getCode() {
        return this.ex.getCode();
    }
}
