package foundation.identity.jsonld.normalization;

import com.apicatalog.rdf.*;
import com.apicatalog.rdf.impl.DefaultRdfProvider;
import com.apicatalog.rdf.io.nquad.NQuadsWriter;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class NormalizationAlgorithm {

    private static String[] POSITIONS = new String[] { "s", "o", "g" };

    private Map<String, Map<String, Object>> blankNodeInfo;
    private Map<String, List<String>> hashToBlankNodes;
    private IdentifierIssuer canonicalIssuer;
    private List<RdfNQuad> quads;
    private String[] lines;
    private String version;

    public NormalizationAlgorithm(String version) {

        this.blankNodeInfo  = new HashMap<>();
        this.canonicalIssuer = new IdentifierIssuer("_:c14n");
        this.quads = new ArrayList<>();
        this.version = version;
    }

    public List<RdfNQuad> getQuads() {
        return this.quads;
    }

    public void normalize(RdfDataset dataset) {

        // 1) Create the normalisation state

        // 2) For every quad in input dataset:

        Map<String, List<RdfTriple>> graphs = new HashMap<>();
        graphs.put("@default", dataset.getDefaultGraph().toList());
        for (RdfResource graphName : dataset.getGraphNames()) graphs.put(graphName.getValue(), dataset.getGraph(graphName).get().toList());
        for (Map.Entry<String, List<RdfTriple>> graph : graphs.entrySet()) {
            String graphName = graph.getKey();
            List<RdfTriple> triples = graph.getValue();
            if ("@default".equals(graphName)) {
                graphName = "";
            }
            for (RdfTriple triple : triples) {
                RdfResource quadSubject = triple.getSubject();
                RdfResource quadPredicate = triple.getPredicate();
                RdfValue quadObject = triple.getObject();
                RdfResource quadGraphName = null;
                if (! graphName.equals("")) {
                    if (graphName.indexOf("_:") == 0)
                        quadGraphName = NewBlankNode(graphName);
                    else
                        quadGraphName = NewIRI(graphName);
                }
                RdfNQuad quad = DefaultRdfProvider.INSTANCE.createNQuad(
                        quadSubject,
                        quadPredicate,
                        quadObject,
                        quadGraphName);
                this.quads.add(quad);

                // 2.1) For each blank node that occurs in the quad, add
                // a reference to the quad using the blank node identifier
                // in the blank node to quads map, creating a new entry if necessary.

                for (RdfValue attrNode : Arrays.asList(quad.getSubject(), quad.getObject(), quad.getGraphName().isEmpty() ? null : quad.getGraphName().get())) {
                    if (attrNode != null) {
                        if (IsBlankNode(attrNode)) {
                            String id = attrNode.getValue();
                            Map<String, Object> bNodeInfo = this.blankNodeInfo.get(id);
                            if (bNodeInfo == null) {
                                bNodeInfo = new HashMap<>();
                                bNodeInfo.put("quads", new ArrayList<RdfNQuad>());
                                this.blankNodeInfo.put(id, bNodeInfo);
                            }
                            ((List<RdfNQuad>) bNodeInfo.get("quads")).add(quad);
                        }
                    }
                }
            }
        }

        // 3) Create a list of non-normalized blank node identifiers and
        // populate it using the keys from the blank node to quads map.

        Map<String, Boolean> nonNormalized = new HashMap<>();
        for (String id : this.blankNodeInfo.keySet()) nonNormalized.put(id, Boolean.TRUE);

        // 4) Initialize simple, a boolean flag, to true.

        boolean simple = true;

        // 5) While simple is true, issue canonical identifiers for blank nodes:

        while (simple) {

            // 5.1) Set simple to false.

            simple = false;

            // 5.2) Clear hash to blank nodes map.

            this.hashToBlankNodes = new HashMap<>();

            // 5.3) For each blank node identifier in non-normalized identifiers:

            for (String id : nonNormalized.keySet()) {

                // 5.3.1) Create a hash, hash, according to the Hash First Degree Quads algorithm.

                String hash = this.hashFirstDegreeQuad(id);

                // 5.3.2) Add hash and identifier to hash to blank nodes map,
                // creating a new entry if necessary.

                List<String> bNodeList = this.hashToBlankNodes.get(hash);
                if (bNodeList == null) {
                    bNodeList = new ArrayList<>();
                    this.hashToBlankNodes.put(hash, bNodeList);
                }
                bNodeList.add(id);
            }

            // 5.4) For each hash to identifier list mapping in hash to blank
            // nodes map, lexicographically-sorted by hash:

            String[] sortedHashes = new String[this.hashToBlankNodes.size()];
            int i = 0;
            for (String key : this.hashToBlankNodes.keySet()) {
                sortedHashes[i] = key;
                i++;
            }
            Arrays.sort(sortedHashes);
            for (String hash : sortedHashes) {
                List<String> idList = this.hashToBlankNodes.get(hash);

                // 5.4.1) If the length of identifier list is greater than 1,
                // continue to the next mapping.

                if (idList.size() > 1) continue;

                // 5.4.2) Use the Issue Identifier algorithm, passing canonical
                // issuer and the single blank node identifier in identifier
                // list, identifier, to issue a canonical replacement identifier
                // for identifier.

                String id = idList.get(0);
                this.canonicalIssuer.getId(id);

                // 5.4.3) Remove identifier from non-normalized identifiers.

                nonNormalized.remove(id);

                // 5.4.4) Remove hash from the hash to blank nodes map.

                this.hashToBlankNodes.remove(hash);

                // 5.4.5) Set simple to true.

                simple = true;
            }
        }

        // 6) For each hash to identifier list mapping in hash to blank nodes
        // map, lexicographically-sorted by hash:

        String[] sortedHashes = new String[this.hashToBlankNodes.size()];
        int i = 0;
        for (String key : this.hashToBlankNodes.keySet()) {
            sortedHashes[i] = key;
            i++;
        }
        Arrays.sort(sortedHashes);
        for (String hash : sortedHashes) {
            List<String> idList = this.hashToBlankNodes.get(hash);

            // 6.1) Create hash path list where each item will be a result of
            // running the Hash N-Degree Quads algorithm.

            Map<String, List<IdentifierIssuer>> hashPaths = new HashMap<>();

            // 6.2) For each blank node identifier identifier in identifier list:

            for (String id : idList) {

                // 6.2.1) If a canonical identifier has already been issued for
                // identifier, continue to the next identifier.

                if (this.canonicalIssuer.hasId(id)) continue;

                // 6.2.2) Create temporary issuer, an identifier issuer
                // initialized with the prefix _:b.

                IdentifierIssuer issuer = new IdentifierIssuer("_:b");

                // 6.2.3) Use the Issue Identifier algorithm, passing temporary
                // issuer and identifier, to issue a new temporary blank node
                // identifier for identifier.

                issuer.getId(id);


                // 6.2.4) Run the Hash N-Degree Quads algorithm, passing
                // temporary issuer, and append the result to the hash path
                // list.

                Object[] hashAndNewIssuer = this.hashNDegreeQuads(id, issuer);
                String hash2 = (String) hashAndNewIssuer[0];
                IdentifierIssuer newIssuer = (IdentifierIssuer) hashAndNewIssuer[1];
                List<IdentifierIssuer> issuerList = hashPaths.get(hash2);
                if (issuerList == null) {
                    issuerList = new ArrayList<>();
                    hashPaths.put(hash2, issuerList);
                }
                issuerList.add(newIssuer);
            }

            // 6.3) For each result in the hash path list,
            // lexicographically-sorted by the hash in result:

            String[] sortedHashes2 = new String[hashPaths.size()];
            int i2 = 0;
            for (String key : hashPaths.keySet()) {
                sortedHashes2[i2] = key;
                i2++;
            }
            Arrays.sort(sortedHashes2);
            for (String hash3 : sortedHashes2) {
                for (IdentifierIssuer resultIssuer : hashPaths.get(hash3)) {

                    // 6.3.1) For each blank node identifier, existing identifier,
                    // that was issued a temporary identifier by identifier issuer
                    // in result, issue a canonical identifier, in the same order,
                    // using the Issue Identifier algorithm, passing canonical
                    // issuer and existing identifier.

                    for (String existing : resultIssuer.existingOrder) {
                        this.canonicalIssuer.getId(existing);
                    }
                }
            }
        }

        // Note: At this point all blank nodes in the set of RDF quads have been
        // assigned canonical identifiers, which have been stored in the
        // canonical issuer. Here each quad is updated by assigning each of its
        // blank nodes its new identifier.

        // 7) For each quad, quad, in input dataset:

        this.lines = new String[this.quads.size()];
        int i2 = 0;
        for (RdfNQuad quad : this.quads) {

            // 7.1) Create a copy, quad copy, of quad and replace any existing blank
            // node identifiers using the canonical identifiers previously issued by
            // canonical issuer.
            // Note: We optimize away the copy here.

            List<RdfValue> copyAttrNodes = new ArrayList<>();
            for (RdfValue attrNode : Arrays.asList(quad.getSubject(), quad.getObject(), quad.getGraphName().isEmpty() ? null : quad.getGraphName().get())) {
                if (attrNode != null) {
                    String attrValue = attrNode.getValue();
                    if (IsBlankNode(attrNode) && attrValue.indexOf("_:c14n") != 0) {
                        copyAttrNodes.add(NewBlankNode(this.canonicalIssuer.getId(attrValue)));
                    } else {
                        copyAttrNodes.add(attrNode);
                    }
                } else {
                    copyAttrNodes.add(null);
                }
            }
            RdfResource copyGraphName = null;
            if (copyAttrNodes.get(2) != null) {
                copyGraphName = (RdfResource) copyAttrNodes.get(2);
            }
            RdfNQuad copyQuad = DefaultRdfProvider.INSTANCE.createNQuad(
                    (RdfResource) copyAttrNodes.get(0),
                    quad.getPredicate(),
                    copyAttrNodes.get(1),
                    copyGraphName
            );

            // 7.2) Add quad copy to the normalized dataset.

            String name = null;
            RdfResource nameVal = copyQuad.getGraphName().isEmpty() ? null : copyQuad.getGraphName().get();
            if (nameVal != null) {
                name = nameVal.getValue();
            }
            this.lines[i2] = toNQuad(copyQuad, name);
            i2++;
        }

        // sort normalized output

        Arrays.sort(this.lines);
    }

    public String main(RdfDataset dataset) {

        // Steps 1 through 7.2, plus sorting

        this.normalize(dataset);

        // 8) Return the normalized dataset.

        return String.join("", this.lines);
    }

    private String hashFirstDegreeQuad(String id) {

        // return cached hash

        Map<String, Object> info = this.blankNodeInfo.get(id);
        if (info.containsKey("hash")) return (String) info.get("hash");

        // 1) Initialize nquads to an empty list. It will be used to store quads
        // in N-Quads format.

        List<String> nquads = new ArrayList<>();

        // 2) Get the list of quads associated with the reference blank
        // node identifier in the blank node to quads map.

        List<RdfNQuad> quads = (List<RdfNQuad>) info.get("quads");

        // 3) For each quad quad in quads:

        for (RdfNQuad quad : quads) {

            // 3.1) Serialize the quad in N-Quads format with the following
            // special rule:

            // 3.1.1) If any component in quad is an blank node, then serialize
            // it using a special identifier as follows:

            // 3.1.2) If the blank node's existing blank node identifier
            // matches the reference blank node identifier then use the
            // blank node identifier _:a, otherwise, use the blank node
            // identifier _:z.

            RdfValue graphCopy = this.modifyFirstDegreeComponent(id, quad.getGraphName().isEmpty() ? null : quad.getGraphName().get(), true);
            String name = null;
            if (graphCopy != null) {
                name = graphCopy.getValue();
            }

            RdfNQuad quadCopy = NewQuad(
                    (RdfResource) this.modifyFirstDegreeComponent(id, quad.getSubject(), false),
                    quad.getPredicate(),
                    this.modifyFirstDegreeComponent(id, quad.getObject(), false),
                    name
            );

            nquads.add(toNQuad(quadCopy, name));
        }

        // 4) Sort nquads in lexicographical order.

        Collections.sort(nquads);

        // 5) Return the hash that results from passing the sorted, joined nquads
        // through the hash algorithm.

        String hash = this.hashNQuads(nquads);
        info.put("hash", hash);
        return hash;
    }

    private RdfValue modifyFirstDegreeComponent(String id, RdfValue component, boolean isGraph) {
        if (! IsBlankNode(component)) {
            return component;
        }
        String val = "";
        if ("URDNA2015".equals(this.version)) {
            if (id.equals(component.getValue())) {
                val = "_:a";
            } else {
                val = "_:z";
            }
        } else {
            if (isGraph) {
                val = "_:g";
            } else {
                if (id.equals(component.getValue())) {
                    val = "_:a";
                } else {
                    val = "_:z";
                }
            }
        }
        return NewBlankNode(val);
    }

    //4.7) Hash Related Blank Node

    private String hashRelatedBlankNode(String related, RdfNQuad quad, IdentifierIssuer issuer, String position) {

        // 1) Set the identifier to use for related, preferring first the
        // canonical identifier for related if issued, second the identifier
        // issued by issuer if issued, and last, if necessary, the result of
        // the Hash First Degree Quads algorithm, passing related.

        String id;
        if (this.canonicalIssuer.hasId(related)) {
            id = this.canonicalIssuer.getId(related);
        } else if (issuer.hasId(related)) {
            id = issuer.getId(related);
        } else {
            id = this.hashFirstDegreeQuad(related);
        }

        // 2) Initialize a string input to the value of position.
        // Note: We use a hash object instead.

        MessageDigest md = this.createHash();
        md.update(position.getBytes());

        // 3) If position is not g, append <, the value of the predicate in
        // quad, and > to input.

        if (! "g".equals(position)) {
            md.update(this.getRelatedPredicate(quad).getBytes());
        }

        // 4) Append identifier to input.

        md.update(id.getBytes());

        // 5) Return the hash that results from passing input through the hash
        // algorithm.

        return this.encodeHex(md.digest());
    }

    // 4.8) Hash N-Degree Quads

    private Object[] hashNDegreeQuads(String id, IdentifierIssuer issuer) {

        // 1) Create a hash to related blank nodes map for storing hashes that
        // identify related blank nodes.
        // Note: 2) and 3) handled within `createHashToRelated`

        Map<String, List<String>> hashToRelated = this.createHashToRelated(id, issuer);

        // 4) Create an empty string, data to hash.
        // Note: We create a hash object instead.

        MessageDigest md = this.createHash();

        // 5) For each related hash to blank node list mapping in hash to
        // related blank nodes map, sorted lexicographically by related hash:

        String[] sortedHashes = new String[hashToRelated.size()];
        int i = 0;
        for (String key : hashToRelated.keySet()) {
            sortedHashes[i] = key;
            i++;
        }
        Arrays.sort(sortedHashes);
        for (String hash : sortedHashes) {
            List<String> blankNodes = hashToRelated.get(hash);

            // 5.1) Append the related hash to the data to hash.

            md.update(hash.getBytes());

            // 5.2) Create a string chosen path.

            String chosenPath = "";

            // 5.3) Create an unset chosen issuer variable.

            IdentifierIssuer chosenIssuer = null;

            // 5.4) For each permutation of blank node list:

            Permutator permutator  = this.newPermutator(blankNodes);
            while (permutator.hasNext()) {
                List<String> permutation = permutator.next();

                // 5.4.1) Create a copy of issuer, issuer copy.

                IdentifierIssuer issuerCopy = issuer.clone();

                // 5.4.2) Create a string path.

                String path = "";

                // 5.4.3) Create a recursion list, to store blank node
                // identifiers that must be recursively processed by this
                // algorithm.

                List<String> recursionList = new ArrayList<>();

                // 5.4.4) For each related in permutation:

                boolean skipToNextPermutation = false;
                for (String related : permutation) {

                    // 5.4.4.1) If a canonical identifier has been issued for
                    // related, append it to path.

                    if (this.canonicalIssuer.hasId(related)) {
                        path += this.canonicalIssuer.getId(related);
                    } else {

                        // 5.4.4.2) Otherwise:

                        // 5.4.4.2.1) If issuer copy has not issued an
                        // identifier for related, append related to recursion
                        // list.

                        if (! issuerCopy.hasId(related)) {
                            recursionList.add(related);
                        }


                        // 5.4.4.2.2) Use the Issue Identifier algorithm,
                        // passing issuer copy and related and append the result
                        // to path.

                        path += issuerCopy.getId(related);
                    }

                    // 5.4.4.3) If chosen path is not empty and the length of
                    // path is greater than or equal to the length of chosen
                    // path and path is lexicographically greater than chosen
                    // path, then skip to the next permutation.

                    if (chosenPath.length() != 0 && path.length() >= chosenPath.length() && path.compareTo(chosenPath) > 0) {
                        skipToNextPermutation = true;
                        break;
                    }
                }

                if (skipToNextPermutation) {
                    continue;
                }

                // 5.4.5) For each related in recursion list:

                for (String related : recursionList) {

                    // 5.4.5.1) Set result to the result of recursively
                    // executing the Hash N-Degree Quads algorithm, passing
                    // related for identifier and issuer copy for path
                    // identifier issuer.

                    Object[] resultHashAndResultIssuer = this.hashNDegreeQuads(related, issuerCopy);
                    String resultHash = (String) resultHashAndResultIssuer[0];
                    IdentifierIssuer resultIssuer = (IdentifierIssuer) resultHashAndResultIssuer[1];

                    // 5.4.5.2) Use the Issue Identifier algorithm, passing
                    // issuer copy and related and append the result to path.

                    path += issuerCopy.getId(related);

                    // 5.4.5.3) Append <, the hash in result, and > to path.

                    path += "<" + resultHash + ">";

                    // 5.4.5.4) Set issuer copy to the identifier issuer in
                    // result.

                    issuerCopy = resultIssuer;

                    // 5.4.5.5) If chosen path is not empty and the length of
                    // path is greater than or equal to the length of chosen
                    // path and path is lexicographically greater than chosen
                    // path, then skip to the next permutation.

                    if (chosenPath.length() != 0 && path.length() >= chosenPath.length() && path.compareTo(chosenPath) > 0) {
                        skipToNextPermutation = true;
                        break;
                    }
                }

                if (skipToNextPermutation) {
                    continue;
                }

                // 5.4.6) If chosen path is empty or path is lexicographically
                // less than chosen path, set chosen path to path and chosen
                // issuer to issuer copy.

                if (chosenPath.length() == 0 || path.compareTo(chosenPath) < 0 ) {
                    chosenPath = path;
                    chosenIssuer = issuerCopy;
                }
            }

            // 5.5) Append chosen path to data to hash.

            md.update(chosenPath.getBytes());

            // 5.6) Replace issuer, by reference, with chosen issuer.

            issuer = chosenIssuer;
        }

        // 6) Return issuer and the hash that results from passing data to hash
        // through the hash algorithm.

        Object[] hashAndIssuer = new Object[2];
        hashAndIssuer[0] = this.encodeHex(md.digest());
        hashAndIssuer[1] = issuer;
        return hashAndIssuer;
    }

    // helper to create appropriate hash object

    private MessageDigest createHash() {
        try {
            if ("URDNA2015".equals(this.version)) {
                return MessageDigest.getInstance("SHA-256");
            } else {
                return MessageDigest.getInstance("SHA-1");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // helper to hash a list of nquads

    private String hashNQuads(List<String> nquads) {
        MessageDigest h = this.createHash();
        for (String nquad : nquads) {
            h.update(nquad.getBytes());
        }
        return encodeHex(h.digest());
    }

    // helper for getting a related predicate

    private String getRelatedPredicate(RdfNQuad quad) {
        if ("URDNA2015".equals(this.version)) {
            return "<" + quad.getPredicate().getValue() + ">";
        } else {
            return quad.getPredicate().getValue();
        }
    }

    // helper for creating hash to related blank nodes map

    private Map<String, List<String>> createHashToRelated(String id, IdentifierIssuer issuer) {

        // 1) Create a hash to related blank nodes map for storing hashes that
        // identify related blank nodes.

        Map<String, List<String>> hashToRelated = new HashMap<>();

        // 2) Get a reference, quads, to the list of quads in the blank node to
        // quads map for the key identifier.

        List<RdfNQuad> quads = (List<RdfNQuad>) this.blankNodeInfo.get(id).get("quads");

        // 3) For each quad in quads:

        String related, position;
        if ("URDNA2015".equals(this.version)) {
            for (RdfNQuad quad : quads) {

                // 3.1) For each component in quad, if component is the subject,
                // object, and graph name and it is a blank node that is not
                // identified by identifier:

                int i = 0;
                for (RdfValue attrNode : Arrays.asList(quad.getSubject(), quad.getObject(), quad.getGraphName().isEmpty() ? null : quad.getGraphName().get())) {
                    if (attrNode != null) {
                        String attrValue = attrNode.getValue();
                        if (IsBlankNode(attrNode) && ! id.equals(attrValue)) {

                            // 3.1.1) Set hash to the result of the Hash Related Blank
                            // Node algorithm, passing the blank node identifier for
                            // component as related, quad, path identifier issuer as
                            // issuer, and position as either s, o, or g based on
                            // whether component is a subject, object, graph name,
                            // respectively.

                            related = attrValue;
                            position = POSITIONS[i];
                            String hash = this.hashRelatedBlankNode(related, quad, issuer, position);

                            // 3.1.2) Add a mapping of hash to the blank node identifier
                            // for component to hash to related blank nodes map, adding
                            // an entry as necessary.

                            List<String> relatedList = hashToRelated.get(hash);
                            if (relatedList == null) {
                                relatedList = new ArrayList<>();
                                hashToRelated.put(hash, relatedList);
                            }
                            relatedList.add(related);
                        }
                    }
                    i++;
                }
            }
        } else {
            for (RdfNQuad quad : quads) {

                // 3.1) If the quad's subject is a blank node that does not match
                // identifier, set hash to the result of the Hash Related Blank Node
                // algorithm, passing the blank node identifier for subject as
                // related, quad, path identifier issuer as issuer, and p as
                // position.

                if (IsBlankNode(quad.getSubject()) && ! id.equals(quad.getSubject().getValue())) {
                    related = quad.getSubject().getValue();
                    position = "p";
                } else if (IsBlankNode(quad.getObject()) && ! id.equals(quad.getObject().getValue())) {

                    // 3.2) Otherwise, if quad's object is a blank node that does
                    // not match identifier, to the result of the Hash Related Blank
                    // Node algorithm, passing the blank node identifier for object
                    // as related, quad, path identifier issuer as issuer, and r
                    // as position.

                    related = quad.getObject().getValue();
                    position = "r";
                } else {
                    continue;
                }

                // 3.4) Add a mapping of hash to the blank node identifier for the
                // component that matched (subject or object) to hash to related
                // blank nodes map, adding an entry as necessary.

                String hash = this.hashRelatedBlankNode(related, quad, issuer, position);
                List<String> relatedList = hashToRelated.get(hash);
                if (relatedList == null) {
                    relatedList = new ArrayList<>();
                    hashToRelated.put(hash, relatedList);
                }
                relatedList.add(related);
            }
        }
        return hashToRelated;
    }

    private String encodeHex(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    private static class Permutator {
        private List<String> list;
        private boolean done;
        private Map<String, Boolean> left;

        private boolean hasNext() {
            return ! this.done;
        }

        private List<String> next() {
            List<String> rval = new ArrayList<>(this.list.size());
            for (String elem : this.list) {
                rval.add(elem);
            }

            // Calculate the next permutation using Steinhaus-Johnson-Trotter
            // permutation algorithm

            // get largest mobile element k
            // (mobile: element is greater than the one it is looking at)

            String k = "";
            int pos = 0;
            int length = this.list.size();
            for (int i=0; i<length; i++) {
                String element = this.list.get(i);
                Boolean left = this.left.get(element);
                if (("".equals(k) || element.compareTo(k) > 0) && // TODO check this
                        ((Boolean.TRUE.equals(left) && i > 0 && element.compareTo(this.list.get(i-1)) > 0) || (! Boolean.TRUE.equals(left) && i < (length-1) && element.compareTo(this.list.get(i+1)) > 0))) {
                    k = element;
                    pos = i;
                }
            }

            // no more permutations

            if ("".equals(k)) {
                this.done = true;
            } else {

                // swap k and the element it is looking at

                int swap;
                if (Boolean.TRUE.equals(this.left.get(k))) {
                    swap = pos - 1;
                } else {
                    swap = pos + 1;
                }
                this.list.set(pos, this.list.get(swap));
                this.list.set(swap, k);

                // reverse the direction of all element larger than k

                for (int i=0; i<length; i++) {
                    if (this.list.get(i).compareTo(k) > 0) {
                        this.left.put(this.list.get(i), ! this.left.get(this.list.get(i)));
                    }
                }
            }

            return rval;
        }
    }

    private static Permutator newPermutator(List<String> list) {
        Permutator  p = new Permutator();
        p.list = new ArrayList<>(list.size());
        for (String elem : list) {
            p.list.add(elem);
        }
        Collections.sort(p.list);
        p.done = false;
        p.left = new HashMap<>(list.size());
        for (String i : p.list) {
            p.left.put("i", Boolean.TRUE);
        }
        return p;
    }

    private static boolean IsBlankNode(RdfValue node) {
        return node != null && node.isBlankNode();
    }

    private static RdfResource NewBlankNode(String val) {
        return DefaultRdfProvider.INSTANCE.createBlankNode(val);
    }

    private static RdfResource NewIRI(String val) {
        return DefaultRdfProvider.INSTANCE.createIRI(val);
    }

    private static RdfNQuad NewQuad(RdfResource subject, RdfResource predicate, RdfValue object, String graphName) {
        RdfResource graph = null;
        if (graphName != null && ! "".equals(graphName) && ! "@default".equals(graphName)) {

            // TODO: i'm not yet sure if this should be added or if the
            // graph should only be represented by the keys in the dataset

            if (graphName.startsWith("_:")) {
                graph = NewBlankNode(graphName);
            } else {
                graph = NewIRI(graphName);
            }
        }
        return DefaultRdfProvider.INSTANCE.createNQuad(
                subject,
                predicate,
                object,
                graph);
    }

    private static String toNQuad(RdfNQuad quad, String graphName) {
        RdfNQuad quadCopy = NewQuad(
                quad.getSubject(),
                quad.getPredicate(),
                quad.getObject(),
                graphName
        );
        StringWriter writer = new StringWriter();
        NQuadsWriter nQuadsWriter = new NQuadsWriter(writer);
        try {
            nQuadsWriter.write(quadCopy);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return writer.toString();
    }
}

