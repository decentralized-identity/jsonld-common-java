package foundation.identity.jsonld

import foundation.identity.jsonld.JsonLDCredential
import java.net.URI

class JsonLDCredential : JsonLDObject() {

    companion object {
        @JvmField val DEFAULT_JSONLD_CONTEXTS = arrayOf(URI("https://www.w3.org/2018/credentials/v1"))
        fun builder() = Builder(JsonLDCredential())
    }

    class Builder<B : Builder<B>>(jsonLdObject: JsonLDObject?) : JsonLDObject.Builder<B>(jsonLdObject) {
        override fun build(): JsonLDCredential {
            super.build()
            return jsonLdObject as JsonLDCredential
        }
    }
}

