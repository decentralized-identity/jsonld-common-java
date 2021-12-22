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
        private boolean ignoreErrors;

        public Function(JsonLDObject jsonLdDocument, URI baseUri, boolean ignoreErrors) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = baseUri;
            this.ignoreErrors = ignoreErrors;
        }

        public Function(JsonLDObject jsonLdDocument, URI baseUri) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = baseUri;
            this.ignoreErrors = false;
        }

        public Function(JsonLDObject jsonLdDocument) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = null;
            this.ignoreErrors = false;
        }

        @Override
        public JsonLDObject apply(Object o) {

            URI uri = null;
            JsonLDObject result = null;

            if (o instanceof JsonLDObject) return (JsonLDObject) o;
            else if (o instanceof Map) return JsonLDObject.fromMap((Map<String, Object>) o);
            else if (o instanceof String) {
                try {
                    uri = new URI((String) o);
                    result = findByIdInJsonLdObject(this.jsonLdDocument, uri, this.baseUri);
                    if (result != null) return result;
                } catch (URISyntaxException ex) {
                    if (this.ignoreErrors) return null; else throw new IllegalArgumentException("Cannot dereference non-URI string: " + o);
                }
            } else {
                if (this.ignoreErrors) return null; throw new IllegalArgumentException("Cannot dereference non-URI value: " + o);
            }

            if (this.ignoreErrors) return null; throw new IllegalArgumentException("No result for dereferencing URI " + uri);
        }
    }

    public static JsonLDObject findByIdInJsonLdObject(JsonLDObject jsonLdObject, URI uri, URI baseUri) {

        if (jsonLdObject.getId() != null) {

            URI findId = uri;
            if (! findId.isAbsolute() && baseUri == null) throw new IllegalArgumentException("No base URI for relative uri " + findId);
            findId = URI.create(UriResolver.resolve(baseUri, findId.toString()));

            URI idUri = jsonLdObject.getId();
            if (! idUri.isAbsolute() && baseUri == null) throw new IllegalArgumentException("No base URI for relative 'id' uri " + uri);
            idUri = URI.create(UriResolver.resolve(baseUri, idUri.toString()));

            if (findId.equals(idUri)) return jsonLdObject;
        }

        for (Object value : jsonLdObject.getJsonObject().values()) {
            if (value instanceof Map) {
                JsonLDObject foundJsonLDObject = findByIdInJsonLdObject(JsonLDObject.fromMap((Map<String, Object>) value), uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
            else if (value instanceof List) {
                JsonLDObject foundJsonLDObject = findByIdInList((List<Object>) value, uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
        }

        return null;
    }

    private static JsonLDObject findByIdInList(List<Object> list, URI uri, URI baseUri) {

        for (Object value : list) {
            if (value instanceof Map) {
                JsonLDObject foundJsonLDObject = findByIdInJsonLdObject(JsonLDObject.fromMap((Map<String, Object>) value), uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
            else if (value instanceof List) {
                JsonLDObject foundJsonLDObject = findByIdInList((List<Object>) value, uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
        }

        return null;
    }
}
