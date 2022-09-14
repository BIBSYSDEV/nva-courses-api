package no.sikt.nva.fs.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InstitutionConfig {

    private final int code;
    private final String username;
    private final String password;

    @JsonCreator
    public InstitutionConfig(@JsonProperty("code") final int code,
                             @JsonProperty("username") final String username,
                             @JsonProperty("password") final String password) {

        this.code = code;
        this.username = username;
        this.password = password;
    }

    public final int getCode() {
        return code;
    }

    public final String getUsername() {
        return username;
    }

    public final String getPassword() {
        return password;
    }
}
