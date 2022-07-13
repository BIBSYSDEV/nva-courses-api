package no.sikt.nva.fs.client;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD.ShortClassName")
public final class Item {
    private final String href;

    public Item(@JsonProperty("href") final String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }
}
