package foundation.identity.jsonld;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.api.impl.ToRdfApi;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.io.nquad.NQuadsWriter;
import foundation.identity.jsonld.normalization.NormalizationAlgorithm;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonLDObject {

	public static final URI[] DEFAULT_JSONLD_CONTEXTS = new URI[] { };
	public static final String[] DEFAULT_JSONLD_TYPES = new String[] { };
	public static final String DEFAULT_JSONLD_PREDICATE = null;

	private DocumentLoader documentLoader;
	private JsonObjectBuilder jsonObjectBuilder;

	protected JsonLDObject(DocumentLoader documentLoader) {
		this.documentLoader = documentLoader;
		this.jsonObjectBuilder = Json.createObjectBuilder();
	}

	public JsonLDObject(DocumentLoader documentLoader, JsonObject jsonObject) {
		this.documentLoader = documentLoader;
		this.jsonObjectBuilder = Json.createObjectBuilder(jsonObject);
	}

	protected JsonLDObject() {
		this((DocumentLoader) null);
	}

	public JsonLDObject(JsonObject jsonObject) {
		this((DocumentLoader) null, jsonObject);
	}

	/*
	 * Factory methods
	 */

	public static class Builder<T extends Builder<T, J>, J extends JsonLDObject> {

		private boolean defaultContexts = false;
		private boolean defaultTypes = false;
		private List<URI> contexts;
		private List<String> types;
		private URI id;

		protected J jsonLDObject;

		protected Builder(J jsonLDObject) {
			this.jsonLDObject = jsonLDObject;
		}

		public J build() {

			// add JSON-LD properties
			if (this.defaultContexts) { if (this.contexts == null) this.contexts = new ArrayList<>(); this.contexts.addAll(0, JsonLDObject.getDefaultJsonLDContexts(this.jsonLDObject.getClass())); }
			if (this.defaultTypes) { if (this.types == null) this.types = new ArrayList<>(); this.types.addAll(0, JsonLDObject.getDefaultJsonLDTypes(this.jsonLDObject.getClass())); }
			if (this.contexts != null) JsonLDUtils.jsonLdAddStringList(this.jsonLDObject.getJsonObjectBuilder(), Keywords.CONTEXT, this.contexts.stream().map(JsonLDUtils::uriToString).collect(Collectors.toList()));
			if (this.types != null) JsonLDUtils.jsonLdAddStringList(this.jsonLDObject.getJsonObjectBuilder(), JsonLDKeywords.JSONLD_TERM_TYPE, this.types);
			if (this.id != null) JsonLDUtils.jsonLdAddString(this.jsonLDObject.getJsonObjectBuilder(), JsonLDKeywords.JSONLD_TERM_ID, JsonLDUtils.uriToString(this.id));

			return this.jsonLDObject;
		}

		public T template(J template) {
			JsonLDUtils.jsonLdAddAll(this.jsonLDObject.getJsonObjectBuilder(), template.getJsonObject());
			return (T) this;
		}

		public T remove(String term) {
			JsonLDUtils.jsonLdRemove(this.jsonLDObject.getJsonObjectBuilder(), term);
			return (T) this;
		}

		public T defaultContexts(boolean defaultContexts) {
			this.defaultContexts = defaultContexts;
			return (T) this;
		}

		public T contexts(List<URI> contexts) {
			this.contexts = new ArrayList<URI> (contexts);
			return (T) this;
		}

		public T context(URI context) {
			return this.contexts(Collections.singletonList(context));
		}

		public T defaultTypes(boolean defaultTypes) {
			this.defaultTypes = defaultTypes;
			return (T) this;
		}

		public T types(List<String> types) {
			this.types = new ArrayList<String> (types);
			return (T) this;
		}

		public T type(String type) {
			return this.types(Collections.singletonList(type));
		}

		public T id(URI id) {
			this.id = id;
			return (T) this;
		}
	}

	public static Builder builder() {
		return new Builder(new JsonLDObject());
	}

	/*
	 * Reading the JSON-LD object
	 */

	public static <C extends JsonLDObject> C fromJson(Class<C> cl, Reader reader) {
		JsonObject jsonObject = Json.createReader(reader).readObject();
		try {
			Constructor<C> constructor = cl.getConstructor(JsonObject.class);
			return constructor.newInstance(jsonObject);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
			throw new Error(ex);
		}
	}

	public static JsonLDObject fromJson(Reader reader) {
		return fromJson(JsonLDObject.class, reader);
	}

	public static <C extends JsonLDObject> C fromJson(Class<C> cl, String json) {
		return fromJson(cl, new StringReader(json));
	}

	public static JsonLDObject fromJson(String json) {
		return fromJson(JsonLDObject.class, json);
	}

	/*
	 * Adding, getting, and removing the JSON-LD object
	 */

	public void addToJsonLDObject(JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(this.getClass());
		JsonLDUtils.jsonLdAddJsonValue(jsonLdObject.getJsonObjectBuilder(), term, this.getJsonObject());
	}

	public static <C extends JsonLDObject> C getFromJsonLDObject(Class<C> cl, JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(cl);
		JsonObject jsonObject = JsonLDUtils.jsonLdGetJsonObject(jsonLdObject.getJsonObject(), term);
		if (jsonObject == null) return null;
		try {
			Constructor<C> constructor = cl.getConstructor(JsonObject.class);
			return constructor.newInstance(jsonObject);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
			throw new Error(ex);
		}
	}

	public static JsonLDObject getFromJsonLDObject(JsonLDObject jsonLdObject) {
		return getFromJsonLDObject(JsonLDObject.class, jsonLdObject);
	}

	public static <C extends JsonLDObject> void removeFromJsonLdObject(Class<C> cl, JsonLDObject jsonLdObject) {
		String term = getDefaultJsonLDPredicate(cl);
		JsonLDUtils.jsonLdRemove(jsonLdObject.getJsonObjectBuilder(), term);
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

	public JsonObjectBuilder getJsonObjectBuilder() {
		return this.jsonObjectBuilder;
	}

	public synchronized JsonObject getJsonObject() {
		JsonObject jsonObject = this.jsonObjectBuilder.build();
		this.jsonObjectBuilder = Json.createObjectBuilder(jsonObject);
		return jsonObject;
	}

	public List<URI> getContexts() {
		return JsonLDUtils.jsonLdGetStringList(this.getJsonObject(), Keywords.CONTEXT).stream().map(JsonLDUtils::stringToUri).collect(Collectors.toList());
	}

	public final List<String> getTypes() {
		return JsonLDUtils.jsonLdGetStringList(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_TYPE);
	}

	public final String getType() {
		return JsonLDUtils.jsonLdGetString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_TYPE);
	}

	public final boolean isType(String type) {
		return JsonLDUtils.jsonLdContainsString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_TYPE, type);
	}

	public final URI getId() {
		return JsonLDUtils.stringToUri(JsonLDUtils.jsonLdGetString(this.getJsonObject(), JsonLDKeywords.JSONLD_TERM_ID));
	}

	/*
	 * Serialization
	 */

	private static final JsonWriterFactory jsonWriterFactory;
	private static final JsonWriterFactory jsonWriterFactoryPretty;

	static {

		Map<String, Object> properties = new HashMap<>(1);
		Map<String, Object> propertiesPretty = new HashMap<>(1);
		propertiesPretty.put(JsonGenerator.PRETTY_PRINTING, true);
		jsonWriterFactory = Json.createWriterFactory(properties);
		jsonWriterFactoryPretty = Json.createWriterFactory(propertiesPretty);
	}

	public RdfDataset toDataset() throws JsonLDException {

		JsonDocument jsonDocument = JsonDocument.of(MediaType.JSON_LD, this.getJsonObject());
		ToRdfApi toRdfApi = JsonLd.toRdf(jsonDocument);
		if (this.getDocumentLoader() != null) toRdfApi.loader(this.getDocumentLoader());
		toRdfApi.ordered(true);
		try {
			return toRdfApi.get();
		} catch (JsonLdError ex) {
			throw new JsonLDException(ex);
		}
	}

	public String toNQuads() throws JsonLDException, IOException {

		RdfDataset rdfDataset = this.toDataset();
		StringWriter stringWriter = new StringWriter();
		NQuadsWriter nQuadsWriter = new NQuadsWriter(stringWriter);
		nQuadsWriter.write(rdfDataset);
		return stringWriter.toString();
	}

	public String normalize(NormalizationAlgorithm.Version version) throws JsonLDException {

		RdfDataset rdfDataset = this.toDataset();
		return new NormalizationAlgorithm(version).main(rdfDataset);
	}

	public String toJson(boolean pretty) {

		JsonWriterFactory jsonWriterFactory = pretty ? JsonLDObject.jsonWriterFactoryPretty : JsonLDObject.jsonWriterFactory;
		StringWriter stringWriter = new StringWriter();
		JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter);
		jsonWriter.writeObject(this.getJsonObject());
		jsonWriter.close();
		return stringWriter.toString();
	}

	public String toJson() {

		return this.toJson(false);
	}

	/*
	 * Helper methods
	 */

	private static <C extends JsonLDObject> List<URI> getDefaultJsonLDContexts(Class<C> cl) {
		try {
			Field field = cl.getField("DEFAULT_JSONLD_CONTEXTS");
			return Arrays.asList((URI[]) field.get(null));
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			throw new Error(ex);
		}
	}

	private static <C extends JsonLDObject> List<String> getDefaultJsonLDTypes(Class<C> cl) {
		try {
			Field field = cl.getField("DEFAULT_JSONLD_TYPES");
			return Arrays.asList((String[]) field.get(null));
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			throw new Error(ex);
		}
	}

	private static <C extends JsonLDObject> String getDefaultJsonLDPredicate(Class<C> cl) {
		try {
			Field field = cl.getField("DEFAULT_JSONLD_PREDICATE");
			return (String) field.get(null);
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			throw new Error(ex);
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