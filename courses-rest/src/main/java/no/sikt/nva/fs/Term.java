package no.sikt.nva.fs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Term {
    SPRING("VÅR", 1),
    SUMMER("SOM", 2),
    FALL("HØST", 3),
    WINTER("VIT", 4);

    private final String code;
    private final int seqNo;

    Term(String code, int seqNo) {
        this.code = code;
        this.seqNo = seqNo;
    }

    public String getCode() {
        return code;
    }

    public static List<Term> getAfterIncluding(final Term term) {
        return Arrays.stream(Term.values()).filter(t -> t.seqNo >= term.seqNo).collect(Collectors.toList());
    }

    public static List<Term> getBeforeExcluding(final Term term) {
        return Arrays.stream(Term.values()).filter(t -> t.seqNo < term.seqNo).collect(Collectors.toList());
    }
}
