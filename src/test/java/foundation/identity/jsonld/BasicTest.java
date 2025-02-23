package foundation.identity.jsonld;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicTest {

	@Test
	public void testBasic() throws Exception {

		JsonLDObject jsonLDObject = JsonLDObject.builder()
				.context(URI.create("http://testcontext/test.jsonld"))
				.id(URI.create("did:ex:123"))
				.type("MyObject")
				.build();

		assertEquals(1, jsonLDObject.getContexts().size());
		assertEquals(URI.create("http://testcontext/test.jsonld"), jsonLDObject.getContexts().get(0));
		assertEquals(URI.create("did:ex:123"), jsonLDObject.getId());
		assertEquals("MyObject", jsonLDObject.getType());
	}
}
