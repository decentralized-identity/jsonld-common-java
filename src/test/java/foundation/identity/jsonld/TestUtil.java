package foundation.identity.jsonld;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

class TestUtil {

	static String read(InputStream inputStream) throws Exception {

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder buffer = new StringBuilder();

		String line;
		while ((line = reader.readLine()) != null) buffer.append(line).append("\n");

		return buffer.toString();
	}
}
