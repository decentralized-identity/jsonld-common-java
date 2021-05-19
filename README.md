![DIF Logo](https://raw.githubusercontent.com/decentralized-identity/universal-resolver/master/docs/logo-dif.png)

# jsonld-common-java

## Information

This is a work-in-progress implementation of helper objects and functions for JSON-LD documents.

Not ready for production use! Use at your own risk! Pull requests welcome.

## Maven

Dependency:

	<dependency>
		<groupId>decentralized-identity</groupId>
		<artifactId>jsonld-common-java</artifactId>
		<version>0.1-SNAPSHOT</version>
		<scope>compile</scope>
	</dependency>

## Example

Example code:

    JsonLDObject jsonLdObject = JsonLDObject.fromJson(new FileReader("input.jsonld"));
    
    JsonLDObject jsonLDObject2 = JsonLDObject.builder()
            .context(URI.create("https://example.com/mycontext/1.0"))
            .type("SampleDocument")
            .build();
    
    JsonLDUtils.jsonLdAdd(jsonLdObject, "property", jsonLDObject2);
    
    System.out.println(jsonLdObject.toJson(true));

## About

<img align="left" src="https://raw.githubusercontent.com/decentralized-identity/jsonld-common-java/master/docs/logo-dif.png" width="115">

Decentralized Identity Foundation - https://identity.foundation/

<br clear="left" />

<img align="left" src="https://raw.githubusercontent.com/decentralized-identity/jsonld-common-java/master/docs/logo-ngi-essiflab.png" width="115">

Supported by [ESSIF-Lab](https://essif-lab.eu/), which is made possible with financial support from the European Commission's [Next Generation Internet](https://ngi.eu/) programme.
