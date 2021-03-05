package foundation.identity.jsonld.validation;

import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.api.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.processor.ExpansionProcessor;
import foundation.identity.jsonld.JsonLDObject;

import javax.json.JsonArray;

public class Validation {

    private static void validateTrue(boolean valid) throws IllegalStateException {

        if (! valid) throw new IllegalStateException();
    }

    private static void validateJsonLd(JsonLDObject jsonLdObject) {

        try {

            JsonDocument jsonDocument = JsonDocument.of(MediaType.JSON_LD, jsonLdObject.toJsonObject());

            JsonLdOptions jsonLdOptions = new JsonLdOptions();
            jsonLdOptions.setDocumentLoader(jsonLdObject.getDocumentLoader());

            JsonArray array = ExpansionProcessor.expand(jsonDocument, jsonLdOptions, false);
            int originalCountWithoutAtContext = jsonLdObject.getJsonObject().size();
            int expandedCount = array.getJsonObject(0).size();

            if (jsonLdObject.getJsonObject().containsKey("@context")) originalCountWithoutAtContext--;

            if (expandedCount != originalCountWithoutAtContext) throw new IllegalStateException("Undefined JSON-LD terms: " + expandedCount + " instead of " + originalCountWithoutAtContext);
        } catch (JsonLdError ex) {

            throw new RuntimeException(ex.getMessage());
        }
    }

    private static void validateRun(Runnable runnable, String message) throws IllegalStateException {

        try {

            runnable.run();
        } catch (Exception ex) {

            if (ex.getMessage() != null && ! ex.getMessage().isEmpty()) message = message + " (" + ex.getMessage().trim() + ")";
            throw new IllegalStateException(message);
        }
    }

    public static void validate(JsonLDObject jsonLdObject) throws IllegalStateException {

        validateRun(() -> { validateJsonLd(jsonLdObject); }, "Unknown JSON-LD terms found.");
    }
}
