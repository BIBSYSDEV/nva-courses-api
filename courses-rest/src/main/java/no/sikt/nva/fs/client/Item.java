package no.sikt.nva.fs.client;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD.ShortClassName")
public final class Item {
    @SuppressWarnings("PMD.ShortFieldName")
    private final FsIdentifier id;

    public Item(@JsonProperty("id") final FsIdentifier id) {
        this.id = id;
    }

    public FsIdentifier getId() {
        return id;
    }
}
