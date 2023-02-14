package foundation.identity.jsonld

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI

class JsonLDObjectTests {

    companion object {
        init {
            (JsonLDObject.DEFAULT_DOCUMENT_LOADER as ConfigurableDocumentLoader).isEnableHttps = true
        }
    }

    @Test
    fun createJsonLDObject() {
        val jsonLDObject = JsonLDObject.builder().apply {
            contexts(
                listOf(
                    URI.create("https://www.w3.org/2018/credentials/v1"),
                    URI.create("https://w3id.org/citizenship/v1")
                )
            )
            types(listOf("PermanentResident", "Person"))
            properties(
                mapOf(
                    "givenName" to "Marion",
                    "familyName" to "Mustermann"
                )
            )
        }.build()
        Assertions.assertEquals(4, jsonLDObject.toDataset().size())
    }

    @Test
    fun createDerivedJsonLDObjectWithDefaultContexts() {
        val jsonLDCredential = JsonLDCredential.builder().apply {
            contexts(
                listOf(
                    URI("https://w3id.org/citizenship/v1")
                )
            )
            defaultContexts(true)
            types(listOf("PermanentResident", "Person"))
            properties(
                mapOf(
                    "givenName" to "Marion",
                    "familyName" to "Mustermann"
                )
            )
        }.build()
        Assertions.assertEquals(4, jsonLDCredential.toDataset().size())
        Assertions.assertEquals(
            jsonLDCredential.contexts, listOf(
                URI("https://www.w3.org/2018/credentials/v1"),
                URI("https://w3id.org/citizenship/v1")
            )
        )
    }

}