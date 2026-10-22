package online.larsen.cddl.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import online.larsen.cddl.psi.CddlTokenSets;
import online.larsen.cddl.psi.CddlTypes;

/** External rules for the generated {@link CddlParser}. */
public class CddlParserUtil extends GeneratedParserUtilBase {
  /**
   * True when the next token is the first one on its line. Newlines are not significant in CDDL, but rules almost
   * always start on a new line, which lets error recovery tell the start of the next rule from a stray name.
   */
  public static boolean atLineStart(PsiBuilder builder, int level) {
    builder.getTokenType(); // skips whitespace and comments
    for (int step = -1; ; step--) {
      IElementType type = builder.rawLookup(step);
      if (type == null) return true;
      if (CddlTokenSets.COMMENTS.contains(type)) return true;
      if (type != TokenType.WHITE_SPACE) return false;
      int start = builder.rawTokenTypeStart(step);
      int end = builder.rawTokenTypeStart(step + 1);
      if (StringUtilHolder.containsLineBreak(builder.getOriginalText(), start, end)) return true;
    }
  }

  /**
   * True when the next token is '<'. Unlike a token match, this does not record '<' as an expected token, which would
   * otherwise show up in the error message after every name ("'<' expected").
   */
  public static boolean atGenericArgs(PsiBuilder builder, int level) {
    return builder.getTokenType() == CddlTypes.LT;
  }

  private static final class StringUtilHolder {
    static boolean containsLineBreak(CharSequence text, int start, int end) {
      for (int i = start; i < end; i++) {
        char c = text.charAt(i);
        if (c == '\n' || c == '\r') return true;
      }
      return false;
    }
  }
}
