package no.sikt.nva.fs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum Term {
    SPRING("V\u00C5R", 1), // VÅR
    SUMMER("SOM", 2),
    FALL("H\u00D8ST", 3), // HØST
    WINTER("VIT", 4);

    private static final Map<String, Term> CODE_TO_TERM_MAP = new ConcurrentHashMap<>();

    static {
        Arrays.stream(values()).forEach(term -> CODE_TO_TERM_MAP.put(term.getCode(), term));
    }

    private final String code;
    private final int seqNo;

    Term(String code, int seqNo) {
        this.code = code;
        this.seqNo = seqNo;
    }

    public String getCode() {
        return code;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public static List<Term> getAfterIncluding(final Term term) {
        return Arrays.stream(Term.values()).filter(t -> t.seqNo >= term.seqNo).collect(Collectors.toList());
    }

    public static List<Term> getBeforeExcluding(final Term term) {
        return Arrays.stream(Term.values()).filter(t -> t.seqNo < term.seqNo).collect(Collectors.toList());
    }

    public static Term fromCode(final String code) {
        final Term term = CODE_TO_TERM_MAP.get(code);
        if (term == null) {
            throw new RuntimeException("Term code not supported: " + code);
        }

        return term;
    }
}
