package no.sikt.nva.fs;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class TestConfig {
    public static final ObjectMapper restApiMapper = JsonUtils.dtoObjectMapper;

    private TestConfig() {

    }
}
