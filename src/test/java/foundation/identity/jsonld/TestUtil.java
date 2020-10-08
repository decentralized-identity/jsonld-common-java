package foundation.identity.jsonld;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

class TestUtil {

	static String read(InputStream inputStream) throws Exception {

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuffer buffer = new StringBuffer();

		String line;
		while ((line = reader.readLine()) != null) buffer.append(line + "\n");

		return buffer.toString();
	}
}
