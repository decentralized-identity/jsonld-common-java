package foundation.identity.jsonld.normalization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifierIssuer {

    private String prefix;
    private int counter;
    private Map<String, String> existing;
    List<String> existingOrder;

    public IdentifierIssuer(String prefix, int counter, Map<String, String> existing, List<String> existingOrder) {

        this.prefix = prefix;
        this.counter = counter;
        this.existing = existing;
        this.existingOrder = existingOrder;
    }

    public IdentifierIssuer(String prefix) {

        this.prefix = prefix;
        this.counter = 0;
        this.existing = new HashMap<>();
        this.existingOrder = new ArrayList<>();
    }

    public IdentifierIssuer clone() {

        return new IdentifierIssuer(
                this.prefix,
                this.counter,
                new HashMap<>(this.existing),
                new ArrayList<>(this.existingOrder));
    }

    public String getId(String oldId) {

        if (! "".equals(oldId)) {
            if (this.existing.containsKey(oldId)) return this.existing.get(oldId);
        }

        String id = this.prefix + this.counter;
        this.counter++;

        if (! "".equals(oldId)) {
            this.existing.put(oldId, id);
            this.existingOrder.add(oldId);
        }
        return id;
    }

    public boolean hasId(String oldId) {

        return this.existing.containsKey(oldId);
    }
}
