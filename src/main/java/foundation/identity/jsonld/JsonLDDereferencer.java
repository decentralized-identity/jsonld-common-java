package foundation.identity.jsonld;

import com.apicatalog.jsonld.uri.UriResolver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class JsonLDDereferencer {

    public static class Function implements java.util.function.Function<Object, JsonLDObject> {

        private JsonLDObject jsonLdDocument;
        private URI baseUri;

        public Function(JsonLDObject jsonLdDocument, URI baseUri) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = baseUri;
        }

        public Function(JsonLDObject jsonLdDocument) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = null;
        }

        @Override
        public JsonLDObject apply(Object o) {

            if (o instanceof JsonLDObject) return (JsonLDObject) o;
            else if (o instanceof Map) return JsonLDObject.fromJsonObject((Map<String, Object>) o);
            else if (o instanceof String) {
                try {
                    URI uri = new URI((String) o);
                    if (! uri.isAbsolute() && this.baseUri != null) uri = URI.create(UriResolver.resolve(this.baseUri, (String) o));
                    if (! uri.isAbsolute()) throw new IllegalArgumentException("No base URI for relative uri " + uri);
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
