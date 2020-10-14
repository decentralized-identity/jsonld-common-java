package foundation.identity.jsonld;

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

	public static void jsonLdAddAll(JsonLDObject jsonLDObject, Map<String, Object> jsonObject) {
		jsonLDObject.getJsonObject().putAll(jsonObject);
	}

	public static void jsonLdAdd(JsonLDObject jsonLDObject, String term, Object value) {

		jsonLdAddList(jsonLDObject, term, Collections.singletonList(value));
	}

	public static void jsonLdAddList(JsonLDObject jsonLDObject, String term, List<? extends Object> values) {

		if (jsonLDObject.getJsonObject() == null || term == null || values == null) throw new NullPointerException();
		if (values.size() < 1) return;

		Object jsonValueExisting = jsonLDObject.getJsonObject().get(term);

		if (jsonValueExisting == null)  {
			if (values.size() == 1) {
				jsonLDObject.getJsonObject().put(term, values.get(0));
			} else {
				List<Object> jsonArray = new ArrayList<>(values);
				jsonLDObject.getJsonObject().put(term, jsonArray);
			}
		} else if (jsonValueExisting instanceof List<?>)  {
			List<Object> jsonArray = new ArrayList<>((List<Object>) jsonValueExisting);
			jsonArray.addAll(values);
			jsonLDObject.getJsonObject().put(term, jsonArray);
		} else {
			List<Object> jsonArray = new ArrayList<>();
			jsonArray.add(jsonValueExisting);
			jsonArray.addAll(values);
			jsonLDObject.getJsonObject().put(term, jsonArray);
		}
	}

	/*
	 * remove
	 */

	public static void jsonLdRemove(JsonLDObject jsonLDObject, String term) {

		jsonLDObject.getJsonObject().remove(term);
	}

	/*
	 * get
	 */

	public static Object jsonLdGetJsonValue(Map<String, Object> jsonObject, String term) {

		return jsonObject.get(term);
	}

	public static List<Object> jsonLdGetJsonArray(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof List<?>) {
			return (List<Object>) entry;
		} else {
			throw new IllegalArgumentException("Cannot get json array '" + term + "' from " + jsonObject);
		}
	}

	public static Map<String, Object> jsonLdGetJsonObject(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof Map<?, ?>) {
			return (Map<String, Object>) entry;
		} else if (entry instanceof List<?> && ((List<Object>) entry).size() == 1 && ((List<Object>) entry).get(0) instanceof Map<?, ?>) {
			return (Map<String, Object>) ((List<Object>) entry).get(0);
		} else {
			throw new IllegalArgumentException("Cannot get json object '" + term + "' from " + jsonObject);
		}
	}

	public static String jsonLdGetString(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof String) {
			return (String) entry;
		} else if (entry instanceof List<?>) {
			if (((List<Object>) entry).size() == 1 && ((List<Object>) entry).get(0) instanceof String) {
				return (String) ((List<Object>) entry).get(0);
			} else {
				throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject + " (list)");
			}
		} else {
			throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject);
		}
	}

	public static List<String> jsonLdGetStringList(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof String) {
			return Collections.singletonList((String) entry);
		} else if (entry instanceof List<?>) {
			return ((List<Object>) entry).stream().map(x -> x instanceof String ? (String) x : null).collect(Collectors.toList());
		} else {
			throw new IllegalArgumentException("Cannot get string list '" + term + "' from " + jsonObject);
		}
	}

	/*
	 * contains
	 */

	public static boolean jsonLdContainsString(Map<String, Object> jsonObject, String term, String value) {

		Object entry = jsonObject.get(term);
		if (entry == null) return false;

		if (entry instanceof String)
			return ((String) entry).equals(value);
		else if (entry instanceof List<?>)
			return ((List<Object>) entry).contains(value);
		else
			return false;
	}
}