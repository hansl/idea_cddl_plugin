package online.larsen.cddl.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static online.larsen.cddl.psi.CddlTypes.*;

// Lexer for CDDL as specified by RFC 8610 Appendix B, updated by RFC 9682.
// Deviations from the ABNF are deliberately lenient (tabs, leading zeros, any
// application-string prefix) so the editor can tokenize work-in-progress files.

%%

%{
  public _CddlLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _CddlLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE=[ \t\f\r\n]+

EALPHA=[a-zA-Z@_$]
DIGIT=[0-9]
HEXDIG=[0-9a-fA-F]
ID={EALPHA}([-.]*({EALPHA}|{DIGIT}))*

UINT={DIGIT}+|0[xX]{HEXDIG}+|0[bB][01]+
EXPONENT=[eE][+-]?{DIGIT}+
HEXFLOAT=0[xX]{HEXDIG}+(\.{HEXDIG}+)?[pP][+-]?{DIGIT}+
NUMBER=-?({HEXFLOAT}|{UINT}|{DIGIT}+\.{DIGIT}+{EXPONENT}?|{DIGIT}+{EXPONENT})

TEXT_CHAR=[^\"\\\r\n]|\\[^\r\n]
TEXT_STRING=\"{TEXT_CHAR}*\"
UNTERMINATED_TEXT_STRING=\"{TEXT_CHAR}*\\?

APP_PREFIX=[a-zA-Z][a-zA-Z0-9]*
BYTE_STRING={APP_PREFIX}?\'([^\'\\]|\\[^])*\'
UNTERMINATED_BYTE_STRING={APP_PREFIX}?\'([^\'\\\r\n]|\\[^\r\n])*\\?

%%

<YYINITIAL> {
  {WHITE_SPACE}                 { return WHITE_SPACE; }

  ";#"[^\r\n]*                  { return DIRECTIVE; }
  ";"[^\r\n]*                   { return COMMENT; }

  "//="                         { return GCHOICE_ASSIGN; }
  "/="                          { return TCHOICE_ASSIGN; }
  "//"                          { return GCHOICE; }
  "/"                           { return TCHOICE; }
  "=>"                          { return ARROW; }
  "="                           { return ASSIGN; }
  "..."                         { return EXCLUSIVE_RANGE; }
  ".."                          { return INCLUSIVE_RANGE; }
  "("                           { return LPAREN; }
  ")"                           { return RPAREN; }
  "{"                           { return LBRACE; }
  "}"                           { return RBRACE; }
  "["                           { return LBRACKET; }
  "]"                           { return RBRACKET; }
  "<"                           { return LT; }
  ">"                           { return GT; }
  ","                           { return COMMA; }
  ":"                           { return COLON; }
  "^"                           { return CARET; }
  "?"                           { return QUESTION; }
  "*"                           { return STAR; }
  "+"                           { return PLUS; }
  "~"                           { return TILDE; }
  "&"                           { return AMPERSAND; }

  // "#6", "#6.32": tags; "#0" ... "#7.25": major types with optional additional information; "#": any.
  "#6"("."{UINT})?              { return TAG_HEAD; }
  "#"{DIGIT}("."{UINT})?        { return MAJOR_TYPE_HEAD; }
  "#"                           { return HASH; }

  // A control operator is "." immediately followed by an identifier, e.g. ".size".
  "."{ID}                       { return CTLOP; }
  "."                           { return DOT; }

  {NUMBER}                      { return NUMBER; }
  {TEXT_STRING}                 { return TEXT_STRING; }
  {UNTERMINATED_TEXT_STRING}    { return TEXT_STRING; }
  {BYTE_STRING}                 { return BYTE_STRING; }
  {UNTERMINATED_BYTE_STRING}    { return BYTE_STRING; }
  {ID}                          { return ID; }
}

[^] { return BAD_CHARACTER; }
