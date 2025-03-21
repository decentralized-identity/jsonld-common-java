package foundation.identity.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.api.ToRdfApi;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import com.apicatalog.rdf.canon.RdfCanon;
import com.apicatalog.rdf.nquads.NQuadsWriter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class JsonLDObject {

	public static final URI[] DEFAULT_JSONLD_CONTEXTS = new URI[] { };
	public static final String[] DEFAULT_JSONLD_TYPES = new String[] { };
	public static final String DEFAULT_JSONLD_PREDICATE = null;
	public static final DocumentLoader DEFAULT_DOCUMENT_LOADER = ConfigurableDocumentLoader.DOCUMENT_LOADER;

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final ObjectWriter objectWriterDefault = objectMapper.writer();
	private static final ObjectWriter objectWriterPretty = objectMapper.writerWithDefaultPrettyPrinter();

	private final Map<String, Object> jsonObject;
	private DocumentLoader documentLoader;

	@JsonCreator
	public JsonLDObject() {
		this(new LinkedHashMap<>());
	}

	protected JsonLDObject(Map<String, Object> jsonObject) {
		this.jsonObject = jsonObject;
		this.documentLoader = getDefaultDocumentLoader(this.getClass());
	}

	/*
	 * Factory methods
	 */

	public static class Builder<B extends Builder<B>> {

		private JsonLDObject base = null;
		private boolean forceContextsArray = false;
		private boolean forceTypesArray = false;
		private boolean defaultContexts = false;
		private boolean defaultTypes = false;
		private List<URI> contexts = null;
		private List<String> types = null;
		private URI id = null;
		private Map<String, Object> properties = null;

		private boolean isBuilt = false;
		protected final JsonLDObject jsonLdObject;

		protected Builder(JsonLDObject jsonLdObject) {
			this.jsonLdObject = jsonLdObject;
		}

		public JsonLDObject build() {

			if (this.isBuilt) throw new IllegalStateException("JSON-LD object has already been built.");
			this.isBuilt = true;

			// add JSON-LD properties

			if (this.base != null) {
				JsonLDUtils.jsonLdAddAll(this.jsonLdObject, this.base.getJsonObject());
			}
			if (this.defaultContexts) {
				List<URI> contexts = new ArrayList<>(JsonLDObject.getDefaultJsonLDContexts(this.jsonLdObject.getClass()));
				if (this.contexts != null) contexts.addAll(this.contexts);
				if (! contexts.isEmpty()) this.contexts = contexts;
			}
			if (this.defaultTypes) {
				List<String> types = new ArrayList<>(JsonLDObject.getDefaultJsonLDTypes(this.jsonLdObject.getClass()));
				if (this.types != null) types.addAll(this.types);
				if (! types.isEmpty()) this.types = types;
			}
			if (this.contexts != null) {
				if (this.forceContextsArray) {
					JsonLDUtils.jsonLdAddAsJsonArray(this.jsonLdObject, Keywords.CONTEXT, this.contexts.stream().map(JsonLDUtils::uriToString).collect(Collectors.toList()));
				} else {
					JsonLDUtils.jsonLdAdd(this.jsonLdObject, Keywords.CONTEXT, this.contexts.stream().map(JsonLDUtils::uriToString).collect(Collectors.toList()));
				}
			}
			if (this.types != null) {
				if (this.forceTypesArray) {
					JsonLDUtils.jsonLdAddAsJsonArray(this.jsonLdObject, JsonLDKeywords.JSONLD_TERM_TYPE, this.types);
				} else {
					JsonLDUtils.jsonLdAdd(this.jsonLdObject, JsonLDKeywords.JSONLD_TERM_TYPE, this.types);
				}
			}
			if (this.id != null) {
				JsonLDUtils.jsonLdAdd(this.jsonLdObject, JsonLDKeywords.JSONLD_TERM_ID, JsonLDUtils.uriToString(this.id));
			}
			if (this.properties != null) {
				JsonLDUtils.jsonLdAddAll(this.jsonLdObject, this.properties);
			}

			return this.jsonLdObject;
		}

		public B base(JsonLDObject base) {
			this.base = base;
			return (B) this;
		}

		public B forceContextsArray(boolean forceContextsArray) {
			this.forceContextsArray = forceContextsArray;
			return (B) this;
		}

		public B forceTypesArray(boolean forceTypesArray) {
			this.forceTypesArray = forceTypesArray;
			return (B) this;
		}

		public B defaultContexts(boolean defaultContexts) {
			this.defaultContexts = defaultContexts;
			return (B) this;
		}

		public B defaultTypes(boolean defaultTypes) {
			this.defaultTypes = defaultTypes;
			return (B) this;
		}

		public B contexts(List<URI> contexts) {
			this.contexts = contexts;
			return (B) this;
		}

		public B context(URI context) {
			return this.contexts(context == null ? null : Collections.singletonList(context));
		}

		public B types(List<String> types) {
			this.types = types;
			return (B) this;
		}

		public B type(String type) {
			return this.types(type == null ? null : Collections.singletonList(type));
		}

		public B id(URI id) {
			this.id = id;
			return (B) this;
		}

		public B properties(Map<String, Object> properties) {
			this.properties = properties;
			return (B) this;
		}
	}

	public static Builder<? extends Builder<?>> builder() {
		return new Builder<>(new JsonLDObject());
	}

	public static JsonLDObject fromJsonObject(Map<String, Object> jsonObject) {
		return new JsonLDObject(jsonObject);
	}

	public static JsonLDObject fromJsonLDObject(JsonLDObject jsonLDObject) { return fromJsonObject(jsonLDObject.getJsonObject()); }

	public static JsonLDObject fromJson(Reader reader) {
		return new JsonLDObject(readJson(reader));
	}

	public static JsonLDObject fromJson(String json) {
		return new JsonLDObject(readJson(json));
	}

	public static JsonLDObject fromMap(Map<String, Object> jsonObject) {
		return new JsonLDObject(jsonObject);
	}

	/*
	 * Adding, getting, and removing the JSON-LD object
	 */

	public void addToJsonLDObject(JsonLDObject jsonLdObject, String term) {
		JsonLDUtils.jsonLdAdd(jsonLdObject, term, this.getJsonObject());
	}

	public void addToJsonLDObject(JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(this.getClass());
		addToJsonLDObject(jsonLdObject, term);
	}

	public void addToJsonLDObjectAsJsonArray(JsonLDObject jsonLdObject, String term) {
		JsonLDUtils.jsonLdAddAsJsonArray(jsonLdObject, term, this.getJsonObject());
	}

	public void addToJsonLDObjectAsJsonArray(JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(this.getClass());
		addToJsonLDObjectAsJsonArray(jsonLdObject, term);
	}

	public static <C extends JsonLDObject> C getFromJsonLDObject(Class<C> cl, JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(cl);
		Map<String, Object> jsonObject = JsonLDUtils.jsonLdGetJsonObject(jsonLdObject.getJsonObject(), term);
		if (jsonObject == null) return null;
		try {
			Method method = cl.getMethod("fromMap", Map.class);
			return (C) method.invoke(null, jsonObject);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <C extends JsonLDObject> List<C> getFromJsonLDObjectAsList(Class<C> cl, JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(cl);
		Object jsonValue = JsonLDUtils.jsonLdGetJsonValue(jsonLdObject.getJsonObject(), term);
		if (jsonValue == null) return null;
		if (jsonValue instanceof Map) {
			jsonValue = Collections.singletonList(jsonValue);
		}
		if (jsonValue instanceof List jsonValueList) {
			return ((List<Map<String, Object>>) jsonValueList).stream().map(jsonObject -> {
				try {
					Method method = cl.getMethod("fromMap", Map.class);
					return (C) method.invoke(null, jsonObject);
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
					throw new RuntimeException(ex);
				}
			}).toList();
		} else {
			throw new IllegalArgumentException("Cannot get JSON-LD object '" + term + "' from " + jsonLdObject);
		}
	}

	public static JsonLDObject getFromJsonLDObject(JsonLDObject jsonLdObject) {
		return getFromJsonLDObject(JsonLDObject.class, jsonLdObject);
	}

	public static <C extends JsonLDObject> void removeFromJsonLdObject(Class<C> cl, JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(cl);
		JsonLDUtils.jsonLdRemove(jsonLdObject, term);
	}

	public static void removeFromJsonLdObject(JsonLDObject jsonLdObject) {
		removeFromJsonLdObject(JsonLDObject.class, jsonLdObject);
	}

	/*
	 * Getters and setters
	 */

	public DocumentLoader getDocumentLoader() {
		return this.documentLoader;
	}

	public void setDocumentLoader(DocumentLoader documentLoader) {
		this.documentLoader = documentLoader;
	}

	@JsonValue
	public Map<String, Object> getJsonObject() {
		return this.jsonObject;
	}

	@JsonAnySetter
	public void setJsonObjectKeyValue(String key, Object value) {

		this.getJsonObject().put(key, value);
	}

	public List<URI> getContexts() {
		List<String> contextStrings = JsonLDUtils.jsonLdGetStringList(this.getJsonObject(), Keywords.CONTEXT);
		return contextStrings == null ? null : contextStrings.stream().map(JsonLDUtils::stringToUri).collect(Collectors.toList());
	}

	public final List<String> getTypes() {
		List<String> result = JsonLDUtils.jsonLdGetStringList(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_AT_TYPE);
		if (result == null) result = JsonLDUtils.jsonLdGetStringList(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_TYPE);
		return result;
	}

	public final String getType() {
		String result = JsonLDUtils.jsonLdGetString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_AT_TYPE);
		if (result == null) result = JsonLDUtils.jsonLdGetString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_TYPE);
		return result;
	}

	public final boolean isType(String type) {
		boolean result = JsonLDUtils.jsonLdContainsString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_AT_TYPE, type);
		if (! result) result = JsonLDUtils.jsonLdContainsString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_TYPE, type);
		return result;
	}

	public final URI getId() {
		URI result = JsonLDUtils.stringToUri(JsonLDUtils.jsonLdGetString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_AT_ID));
		if (result == null) result = JsonLDUtils.stringToUri(JsonLDUtils.jsonLdGetString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_ID));
		return result;
	}

	/*
	 * Reading the JSON-LD object
	 */

	protected static Map<String, Object> readJson(Reader reader) {
		try {
			return objectMapper.readValue(reader, Map.class);
		} catch (IOException ex) {
			throw new RuntimeException("Cannot read JSON: " + ex.getMessage(), ex);
		}
	}

	protected static Map<String, Object> readJson(String json) {
		return readJson(new StringReader(json));
	}

	/*
	 * Writing the JSON-LD object
	 */

	public ToRdfApi toRdfApi() {
		JsonLdOptions options = this.getDocumentLoader() != null ? new JsonLdOptions(this.getDocumentLoader()) : new JsonLdOptions();
		options.setOrdered(true);
		JsonDocument jsonDocument = JsonDocument.of(MediaType.JSON_LD, this.toJsonObject());
		ToRdfApi toRdfApi = JsonLd.toRdf(jsonDocument);
		toRdfApi.options(options);
		return toRdfApi;
	}

	public RdfDataset toDataset() throws JsonLDException {
		try {
			return this.toRdfApi().get();
		} catch (JsonLdError ex) {
			throw new JsonLDException(ex);
		}
	}

	public String toNQuads() throws JsonLDException, IOException {
		StringWriter stringWriter = new StringWriter();
		NQuadsWriter nQuadsWriter = new NQuadsWriter(stringWriter);
        try {
            this.toRdfApi().provide(nQuadsWriter);
		} catch (JsonLdError ex) {
			throw new JsonLDException(ex);
		}
		return stringWriter.toString();
	}

	public String toJson(boolean pretty) {
		ObjectWriter objectWriter = pretty ? objectWriterPretty : objectWriterDefault;
		try {
			return objectWriter.writeValueAsString(this.getJsonObject());
		} catch (JsonProcessingException ex) {
			throw new RuntimeException("Cannot write JSON: " + ex.getMessage(), ex);
		}
	}

	public String toJson() {
		return this.toJson(false);
	}

	public String normalize() throws JsonLDException, IOException {
		RdfCanon rdfCanon = RdfCanon.create("SHA-256");
		StringWriter stringWriter = new StringWriter();
		RdfQuadConsumer nQuadsWriter = new NQuadsWriter(stringWriter);

        try {
            this.toRdfApi().provide(rdfCanon);
			rdfCanon.provide(nQuadsWriter);
        } catch (RdfConsumerException ex) {
            throw new IOException("Cannot consume RDF: " + ex.getMessage(), ex);
        } catch (JsonLdError ex) {
			throw new JsonLDException(ex);
        }

        return stringWriter.getBuffer().toString();
	}

	public String normalize(String algorithm) throws JsonLDException, NoSuchAlgorithmException, IOException {
		if ((! "urdna2015".equals(algorithm)) && (! "RDFC-1.0".equals(algorithm))) throw new NoSuchAlgorithmException("Algorithm not supported: " + algorithm);
		return this.normalize();
	}

	public Map<String, Object> toMap() {
		return this.getJsonObject();
	}

	public synchronized JsonObject toJsonObject() {
		return Json.createObjectBuilder(this.getJsonObject()).build();
	}

	/*
	 * Helper methods
	 */

	public static <C extends JsonLDObject> DocumentLoader getDefaultDocumentLoader(Class<C> cl) {
		try {
			Field field = cl.getField("DEFAULT_DOCUMENT_LOADER");
			return (DocumentLoader) field.get(null);
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <C extends JsonLDObject> List<URI> getDefaultJsonLDContexts(Class<C> cl) {
		try {
			Field field = cl.getField("DEFAULT_JSONLD_CONTEXTS");
			return Arrays.asList((URI[]) field.get(null));
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <C extends JsonLDObject> List<String> getDefaultJsonLDTypes(Class<C> cl) {
		try {
			Field field = cl.getField("DEFAULT_JSONLD_TYPES");
			return Arrays.asList((String[]) field.get(null));
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static <C extends JsonLDObject> String getDefaultJsonLDPredicate(Class<C> cl) {
		try {
			Field field = cl.getField("DEFAULT_JSONLD_PREDICATE");
			return (String) field.get(null);
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			throw new RuntimeException(ex);
		}
	}

	/*
	 * Object methods
	 */

	@Override
	public String toString() {
		return this.toJson(false);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JsonLDObject that = (JsonLDObject) o;
		return Objects.equals(this.getJsonObject(), that.getJsonObject());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getJsonObject());
	}
}