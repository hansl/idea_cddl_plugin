package online.larsen.cddl

/** The control operators registered with IANA (plus a few widely implemented extensions). */
object CddlControlOperators {
    class Operator(
        /** Name including the leading dot, e.g. `.size`. */
        val name: String,
        val reference: String,
        val signature: String,
        val description: String,
        val standard: Boolean = true,
    )

    val ALL: List<Operator> = listOf(
        Operator(".size", "RFC 8610", "(bstr / tstr / uint) .size (uint / range)",
            "Limits the size in bytes of a byte or text string, or of an unsigned integer."),
        Operator(".bits", "RFC 8610", "(bstr / uint) .bits (uint)",
            "Only the bits whose numbers are listed by the controller may be set in the target."),
        Operator(".regexp", "RFC 8610", "tstr .regexp tstr",
            "The text string must match the XML Schema (XSD) regular expression given as controller."),
        Operator(".cbor", "RFC 8610", "bstr .cbor type",
            "The byte string contains an encoded CBOR data item that matches the controller type."),
        Operator(".cborseq", "RFC 8610", "bstr .cborseq array",
            "The byte string contains a CBOR sequence whose items match the elements of the controller array."),
        Operator(".within", "RFC 8610", "type .within type",
            "The target must also match the controller: it is a subset of the controller type."),
        Operator(".and", "RFC 8610", "type .and type",
            "The data item must match both the target and the controller (type intersection)."),
        Operator(".lt", "RFC 8610", "number .lt number", "The value must be less than the controller."),
        Operator(".le", "RFC 8610", "number .le number", "The value must be less than or equal to the controller."),
        Operator(".gt", "RFC 8610", "number .gt number", "The value must be greater than the controller."),
        Operator(".ge", "RFC 8610", "number .ge number",
            "The value must be greater than or equal to the controller."),
        Operator(".eq", "RFC 8610", "type .eq value", "The value must be equal to the controller."),
        Operator(".ne", "RFC 8610", "type .ne value", "The value must not be equal to the controller."),
        Operator(".default", "RFC 8610", "type .default value",
            "Documents the value assumed when an optional item is absent."),
        Operator(".sdnv", "RFC 9090", "bstr .sdnv uint",
            "The byte string contains the SDNV (Self-Delimiting Numeric Value) encoding of the controller."),
        Operator(".sdnvseq", "RFC 9090", "bstr .sdnvseq [* uint]",
            "The byte string contains a sequence of SDNV encodings of the controller's elements."),
        Operator(".oid", "RFC 9090", "bstr .oid [* uint]",
            "The byte string contains the BER encoding of the object identifier whose arcs are given."),
        Operator(".plus", "RFC 9165", "number .plus number",
            "Computes the sum of target and controller (numeric addition), e.g. to define offsets."),
        Operator(".cat", "RFC 9165", "(tstr / bstr) .cat (tstr / bstr)",
            "Concatenates the target and controller strings."),
        Operator(".det", "RFC 9165", "(tstr / bstr) .det (tstr / bstr)",
            "Like .cat, but removes common leading whitespace (dedents) from both strings first."),
        Operator(".abnf", "RFC 9165", "tstr .abnf tstr",
            "The text string must match the ABNF grammar given by the controller."),
        Operator(".abnfb", "RFC 9165", "bstr .abnfb tstr",
            "The byte string must match the ABNF grammar given by the controller."),
        Operator(".feature", "RFC 9165", "type .feature (tstr / [tstr, any])",
            "Marks the target as only used when the named feature is selected."),
        Operator(".b64u", "RFC 9741", "tstr .b64u bstr",
            "The text is the base64url encoding (without padding) of the controller byte string."),
        Operator(".b64u-sloppy", "RFC 9741", "tstr .b64u-sloppy bstr",
            "Like .b64u, but unused bits at the end of the encoding are not checked."),
        Operator(".b64c", "RFC 9741", "tstr .b64c bstr",
            "The text is the classic base64 encoding (with padding) of the controller byte string."),
        Operator(".b64c-sloppy", "RFC 9741", "tstr .b64c-sloppy bstr",
            "Like .b64c, but unused bits at the end of the encoding are not checked."),
        Operator(".b45", "RFC 9741", "tstr .b45 bstr",
            "The text is the base45 encoding of the controller byte string."),
        Operator(".b32", "RFC 9741", "tstr .b32 bstr",
            "The text is the base32 encoding (without padding) of the controller byte string."),
        Operator(".h32", "RFC 9741", "tstr .h32 bstr",
            "The text is the base32hex encoding (without padding) of the controller byte string."),
        Operator(".hex", "RFC 9741", "tstr .hex bstr",
            "The text is the base16 encoding (either case) of the controller byte string."),
        Operator(".hexlc", "RFC 9741", "tstr .hexlc bstr",
            "The text is the lowercase base16 encoding of the controller byte string."),
        Operator(".hexuc", "RFC 9741", "tstr .hexuc bstr",
            "The text is the uppercase base16 encoding of the controller byte string."),
        Operator(".base10", "RFC 9741", "tstr .base10 int",
            "The text is the decimal representation (without leading zeros) of the controller integer."),
        Operator(".printf", "RFC 9741", "tstr .printf [format: tstr, * any]",
            "The text is the result of printf-style formatting of the controller array's elements."),
        Operator(".json", "RFC 9741", "tstr .json type",
            "The text is a JSON value which, converted to CBOR, matches the controller type."),
        Operator(".join", "RFC 9741", "(tstr / bstr) .join [* (tstr / bstr)]",
            "The string is the concatenation of the elements of the controller array."),
        Operator(".pcre", "cddl-rs", "tstr .pcre tstr",
            "The text string must match the PCRE regular expression given as controller.", standard = false),
    )

    private val BY_NAME: Map<String, Operator> = ALL.associateBy { it.name }

    operator fun get(name: String): Operator? = BY_NAME[name]

    /** Operators whose controller is a regular expression. */
    val REGEXP_OPERATORS: Set<String> = setOf(".regexp", ".pcre")
}
