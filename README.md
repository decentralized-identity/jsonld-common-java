# jsonld-common-java

## Information

This is an implementation of helper objects and functions for JSON-LD documents.

## Maven

Build:

	mvn clean install

Dependency:

	<repositories>
		<repository>
			<id>danubetech-maven-public</id>
			<url>https://repo.danubetech.com/repository/maven-public/</url>
		</repository>
	</repositories>

	<dependency>
		<groupId>decentralized-identity</groupId>
		<artifactId>jsonld-common-java</artifactId>
		<version>1.3.0</version>
	</dependency>

## Example

Example code:

    JsonLDObject jsonLdObject = JsonLDObject.fromJson(new FileReader("input.jsonld"));
    
    JsonLDObject jsonLdObject2 = JsonLDObject.builder()
            .context(URI.create("https://example.com/mycontext/1.0"))
            .type("SampleDocument")
            .build();
    
    JsonLDUtils.jsonLdAdd(jsonLdObject, "property", jsonLdObject2);
    
    System.out.println(jsonLdObject.toJson(true));

## About

<img align="left" src="https://raw.githubusercontent.com/decentralized-identity/jsonld-common-java/main/docs/logo-dif.png" width="115">

Decentralized Identity Foundation - https://identity.foundation/

<br clear="left" />

<img align="left" height="70" src="https://raw.githubusercontent.com/decentralized-identity/jsonld-common-java/main/docs/logo-ngi-essiflab.png">

This software library is part of a project that has received funding from the European Union's Horizon 2020 research and innovation programme under grant agreement No 871932
