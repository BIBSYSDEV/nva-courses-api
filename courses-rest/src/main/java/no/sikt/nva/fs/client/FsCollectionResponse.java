package no.sikt.nva.fs.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class FsCollectionResponse {
    private final List<Item> items;

    @JsonCreator
    public FsCollectionResponse(@JsonProperty("items") final List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }
}
