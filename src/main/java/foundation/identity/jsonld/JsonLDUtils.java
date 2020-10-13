package foundation.identity.jsonld;

import javax.json.*;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class JsonLDUtils {

	/*
	 * convert
	 */

	public static final SimpleDateFormat DATE_FORMAT;
	public static final SimpleDateFormat DATE_FORMAT_MILLIS;

	static {

		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

		DATE_FORMAT_MILLIS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
		DATE_FORMAT_MILLIS.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public static URI stringToUri(String string) {
		return string == null ? null : URI.create(string);
	}

	public static String uriToString(URI uri) {
		return uri == null ? null : uri.toString();
	}

	public static Date stringToDate(String string) {
		try {
			return string == null ? null : DATE_FORMAT.parse(string);
		} catch (ParseException ex) {
			try {
				return DATE_FORMAT_MILLIS.parse(string);
			} catch (ParseException ex2) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
		}
	}

	public static String dateToString(Date date) {
		return date == null ? null : DATE_FORMAT.format(date);
	}

	/*
	 * add
	 */

	public static void jsonLdAddAll(JsonLDObject jsonLDObject, JsonObject jsonObject) {
		for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet())
			jsonLDObject.getJsonObjectBuilder().add(entry.getKey(), entry.getValue());
	}

	public static void jsonLdAddAllJsonValueMap(JsonLDObject jsonLDObject, Map<String, JsonValue> map) {
		for (Map.Entry<String, JsonValue> entry : map.entrySet())
			jsonLDObject.getJsonObjectBuilder().add(entry.getKey(), entry.getValue());
	}

	public static void jsonLdAddAllStringMap(JsonLDObject jsonLDObject, Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet())
			jsonLDObject.getJsonObjectBuilder().add(entry.getKey(), Json.createValue(entry.getValue()));
	}

	public static void jsonLdAddJsonValue(JsonLDObject jsonLDObject, String term, JsonValue jsonValue) {
		jsonLdAddJsonValueList(jsonLDObject, term, Collections.singletonList(jsonValue));
	}

	public static void jsonLdAddJsonValueList(JsonLDObject jsonLDObject, String term, List<JsonValue> jsonValues) {

		if (jsonLDObject.getJsonObjectBuilder() == null || term == null || jsonValues == null) throw new NullPointerException();
		if (jsonValues.size() < 1) return;

		JsonValue jsonValueExisting = jsonLDObject.getJsonObject().get(term);

		if (jsonValueExisting == null) {
			if (jsonValues.size() == 1) {
				jsonLDObject.getJsonObjectBuilder().add(term, jsonValues.get(0));
			} else {
				JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
				for (JsonValue jsonValue : jsonValues) jsonArrayBuilder.add(jsonValue);
				jsonLDObject.getJsonObjectBuilder().add(term, jsonArrayBuilder);
			}
		} else if (jsonValueExisting instanceof JsonArray) {
			JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
			for (JsonValue jsonValue : ((JsonArray) jsonValueExisting)) jsonArrayBuilder.add(jsonValue);
			for (JsonValue jsonValue : jsonValues) jsonArrayBuilder.add(jsonValue);
			jsonLDObject.getJsonObjectBuilder().add(term, jsonArrayBuilder);
		} else {
			JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
			jsonArrayBuilder.add(jsonValueExisting);
			for (JsonValue jsonValue : jsonValues) jsonArrayBuilder.add(jsonValue);
			jsonLDObject.getJsonObjectBuilder().add(term, jsonArrayBuilder);
		}
	}

	public static void jsonLdAddString(JsonLDObject jsonLDObject, String term, String value) {

		jsonLdAddStringList(jsonLDObject, term, Collections.singletonList(value));
	}

	public static void jsonLdAddStringList(JsonLDObject jsonLDObject, String term, List<String> values) {

		if (jsonLDObject.getJsonObjectBuilder() == null || term == null || values == null) throw new NullPointerException();
		if (values.size() < 1) return;

		JsonValue jsonValueExisting = jsonLDObject.getJsonObject().get(term);

		if (jsonValueExisting == null)  {
			if (values.size() == 1) {
				jsonLDObject.getJsonObjectBuilder().add(term, Json.createValue(values.get(0)));
			} else {
				JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
				for (String value : values) jsonArrayBuilder.add(Json.createValue(value));
				jsonLDObject.getJsonObjectBuilder().add(term, jsonArrayBuilder);
			}
		} else if (jsonValueExisting instanceof JsonArray)  {
			JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
			for (JsonValue jsonValue : ((JsonArray) jsonValueExisting)) jsonArrayBuilder.add(jsonValue);
			for (String value : values) jsonArrayBuilder.add(Json.createValue(value));
			jsonLDObject.getJsonObjectBuilder().add(term, jsonArrayBuilder);
		} else {
			JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
			jsonArrayBuilder.add(jsonValueExisting);
			for (String value : values) jsonArrayBuilder.add(Json.createValue(value));
			jsonLDObject.getJsonObjectBuilder().add(term, jsonArrayBuilder);
		}
	}

	/*
	 * remove
	 */

	public static void jsonLdRemove(JsonLDObject jsonLDObject, String term) {

		jsonLDObject.getJsonObjectBuilder().remove(term);
	}

	/*
	 * get
	 */

	public static Map<String, JsonValue> jsonLdGetAsJsonValueMap(JsonObject jsonObject) {

		return jsonObject;
	}

	public static JsonValue jsonLdGetJsonValue(JsonObject jsonObject, String term) {

		return jsonObject.get(term);
	}

	public static List<JsonValue> jsonLdGetJsonValueList(JsonObject jsonObject, String term) {

		JsonValue entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof JsonArray) {
			return (JsonArray) entry;
		} else {
			return Collections.singletonList(entry);
		}
	}

	public static JsonArray jsonLdGetJsonArray(JsonObject jsonObject, String term) {

		JsonValue entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof JsonArray) {
			return (JsonArray) entry;
		} else {
			throw new IllegalArgumentException("Cannot get json array '" + term + "' from " + jsonObject);
		}
	}

	public static JsonObject jsonLdGetJsonObject(JsonObject jsonObject, String term) {

		JsonValue entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof JsonObject) {
			return (JsonObject) entry;
		} else if (entry instanceof JsonArray && ((JsonArray) entry).size() == 1 && ((JsonArray) entry).get(0) instanceof JsonObject) {
			return (JsonObject) ((JsonArray) entry).get(0);
		} else {
			throw new IllegalArgumentException("Cannot get json object '" + term + "' from " + jsonObject);
		}
	}

	public static String jsonLdGetString(JsonObject jsonObject, String term) {

		JsonValue entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof JsonString) {
			return ((JsonString) entry).getString();
		} else if (entry instanceof JsonArray) {
			if (((JsonArray) entry).size() == 1 && ((JsonArray) entry).get(0) instanceof JsonString) {
				return ((JsonArray) entry).getString(0);
			} else {
				throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject + " (array)");
			}
		} else {
			throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject);
		}
	}

	public static List<String> jsonLdGetStringList(JsonObject jsonObject, String term) {

		JsonValue entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof JsonString) {
			return Collections.singletonList(((JsonString) entry).getString());
		} else if (entry instanceof JsonArray) {
			return ((JsonArray) entry).stream().map(x -> x instanceof JsonString ? ((JsonString) x).getString() : null).collect(Collectors.toList());
		} else {
			throw new IllegalArgumentException("Cannot get string list '" + term + "' from " + jsonObject);
		}
	}

	/*
	 * contains
	 */

	public static boolean jsonLdContainsString(JsonObject jsonObject, String term, String value) {

		JsonValue entry = jsonObject.get(term);
		if (entry == null) return false;

		if (entry instanceof JsonString)
			return ((JsonString) entry).getString().equals(value);
		else if (entry instanceof JsonArray)
			return ((JsonArray) entry).contains(value);
		else
			return false;
	}
}