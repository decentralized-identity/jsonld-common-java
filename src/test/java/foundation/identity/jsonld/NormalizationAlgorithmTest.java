package foundation.identity.jsonld;

import foundation.identity.jsonld.normalization.NormalizationAlgorithm;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NormalizationAlgorithmTest {

	@Test
	@SuppressWarnings("unchecked")
	public void testNormalizationInput() throws Throwable {

		JsonLDObject jsonLdObject = JsonLDObject.fromJson(new InputStreamReader(NormalizationAlgorithmTest.class.getResourceAsStream("input.jsonld")));
		String normalizedDocument = TestUtil.read(NormalizationAlgorithmTest.class.getResourceAsStream("input.normalized"));

		assertEquals(normalizedDocument, jsonLdObject.normalize(NormalizationAlgorithm.Version.URDNA2015));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNormalizationSigned() throws Throwable {

		JsonLDObject jsonLdObject = JsonLDObject.fromJson(new InputStreamReader(NormalizationAlgorithmTest.class.getResourceAsStream("signed.good.rsa.jsonld")));
		String normalizedDocument = TestUtil.read(NormalizationAlgorithmTest.class.getResourceAsStream("signed.good.rsa.normalized"));

		assertEquals(normalizedDocument, jsonLdObject.normalize(NormalizationAlgorithm.Version.URDNA2015));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNormalizationVerifiableCredential() throws Throwable {

		JsonLDObject jsonLdObject = JsonLDObject.fromJson(new InputStreamReader(NormalizationAlgorithmTest.class.getResourceAsStream("input.vc.jsonld")));
		String normalizedDocument = TestUtil.read(NormalizationAlgorithmTest.class.getResourceAsStream("input.vc.normalized"));

		assertEquals(normalizedDocument, jsonLdObject.normalize(NormalizationAlgorithm.Version.URDNA2015));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNormalizationVerifiablePresentation() throws Throwable {

		JsonLDObject jsonLdObject = JsonLDObject.fromJson(new InputStreamReader(NormalizationAlgorithmTest.class.getResourceAsStream("input.vp.jsonld")));
		String normalizedDocument = TestUtil.read(NormalizationAlgorithmTest.class.getResourceAsStream("input.vp.normalized"));

		assertEquals(normalizedDocument, jsonLdObject.normalize(NormalizationAlgorithm.Version.URDNA2015));
	}
}
