package foundation.identity.jsonld;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JsonLDDereferencer {

    public static class Function implements java.util.function.Function<Object, JsonLDObject> {

        private JsonLDObject jsonLdDocument;

        public Function(JsonLDObject jsonLdDocument) {
            this.jsonLdDocument = jsonLdDocument;
        }

        @Override
        public JsonLDObject apply(Object o) {

            if (o instanceof JsonLDObject) return (JsonLDObject) o;
            else if (o instanceof Map) return JsonLDObject.fromJsonObject((Map<String, Object>) o);
            else if (o instanceof String) {
                try {
                    URI uri = new URI((String) o);
                    return findByIdInJsonLdObject(this.jsonLdDocument, uri);
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Cannot dereference non-URI string: " + o);
                }
            } else {
                throw new IllegalArgumentException("Cannot dereference non-URI value: " + o);
            }
        }
    }

    public static JsonLDObject findByIdInJsonLdObject(JsonLDObject jsonLDObject, URI uri) {

        if (uri.equals(jsonLDObject.getId())) return jsonLDObject;

        for (Object value : jsonLDObject.getJsonObject().values()) {
            if (value instanceof Map) {
                JsonLDObject foundJsonLDObject = findByIdInJsonLdObject(JsonLDObject.fromJsonObject((Map<String, Object>) value), uri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
            else if (value instanceof List) {
                JsonLDObject foundJsonLDObject = findByIdInList((List<Object>) value, uri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
        }

        return null;
    }

    private static JsonLDObject findByIdInList(List<Object> list, URI uri) {

        for (Object value : list) {
            if (value instanceof Map) {
                JsonLDObject foundJsonLDObject = findByIdInJsonLdObject(JsonLDObject.fromJsonObject((Map<String, Object>) value), uri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
            else if (value instanceof List) {
                JsonLDObject foundJsonLDObject = findByIdInList((List<Object>) value, uri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
        }

        return null;
    }
}
