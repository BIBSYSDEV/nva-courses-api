package no.sikt.nva.fs.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FsCourse {
    @JsonProperty("kode")
    private final String code;

    public FsCourse(@JsonProperty("kode") final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
