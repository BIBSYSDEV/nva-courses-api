package no.sikt.nva.fs.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FsConfig {
    private final String baseUri;
    private final List<InstitutionConfig> institutions;

    @JsonCreator
    public FsConfig(@JsonProperty("baseUri") String baseUri,
                    @JsonProperty("institutions") List<InstitutionConfig> institutions) {

        this.baseUri = baseUri;
        this.institutions = new ArrayList<>(institutions);
    }

    public String getBaseUri() {
        return baseUri;
    }

    public List<InstitutionConfig> getInstitutions() {
        return Collections.unmodifiableList(institutions);
    }
}
