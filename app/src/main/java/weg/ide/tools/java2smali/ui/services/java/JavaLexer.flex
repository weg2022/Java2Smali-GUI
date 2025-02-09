package weg.ide.tools.java2smali.ui.services.java;

import java.io.Reader;
import java.io.IOException;import java.sql.Types;

%%

%{

    public static final int PlainStyle = 0;
    public static final int KeywordStyle = 1;
    public static final int OperatorStyle = 2;
    public static final int SeparatorStyle = 3;
    public static final int StringStyle = 4;
    public static final int NumberStyle = 5;
    public static final int MetadataStyle = 6;
    public static final int IdentifierStyle = 7;
    public static final int NamespaceStyle = 8;
    public static final int TypeStyle = 9;
    public static final int FieldStyle = 10;
    public static final int VariableStyle = 11;
    public static final int FunctionStyle = 12;
    public static final int FunctionCallStyle = 13;
    public static final int ParameterStyle = 14;
    public static final int CommentStyle = 15;
    public static final int DocCommentStyle = 16;
    private boolean myAssertKeyword;
    private boolean myEnumKeyword;
    private boolean myVarKeyword;

    public JavaLexer(){
 	     this(11);
    }

    public JavaLexer(int level) {
        this((java.io.Reader)null);
        setLevel(level);
   }

   public void setLevel(int level){
         myAssertKeyword = level >= 4;
         myEnumKeyword =level >= 5;
         myVarKeyword = level >=10;
   }

   public int getDefaultState(){
  		return YYINITIAL;
  	}

  	public int getLine(){
  		return yyline;
  	}

  	public int getColumn(){
  		return yycolumn;
  	}

%}
%public
%unicode
%class JavaLexer
%type int
%line
%column


WHITE_SPACE_CHAR = [\ \n\r\t\f]
WS_CHAR = [\ \t\f]
LINE_END_CHAR = [\n\r]

IDENTIFIER = [:jletter:] [:jletterdigit:]*

C_STYLE_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
END_OF_LINE_COMMENT="/""/"[^\r\n]*

DIGIT = [0-9]
DIGIT_OR_UNDERSCORE = [_0-9]
DIGITS = {DIGIT} | {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT_OR_UNDERSCORE = [_0-9A-Fa-f]

INTEGER_LITERAL = {DIGITS} | {HEX_INTEGER_LITERAL} | {BIN_INTEGER_LITERAL}
LONG_LITERAL = {INTEGER_LITERAL} [Ll]
HEX_INTEGER_LITERAL = 0 [Xx] {HEX_DIGIT_OR_UNDERSCORE}*
BIN_INTEGER_LITERAL = 0 [Bb] {DIGIT_OR_UNDERSCORE}*

FLOAT_LITERAL = ({DEC_FP_LITERAL} | {HEX_FP_LITERAL}) [Ff] | {DIGITS} [Ff]
DOUBLE_LITERAL = ({DEC_FP_LITERAL} | {HEX_FP_LITERAL}) [Dd]? | {DIGITS} [Dd]
DEC_FP_LITERAL = {DIGITS} {DEC_EXPONENT} | {DEC_SIGNIFICAND} {DEC_EXPONENT}?
DEC_SIGNIFICAND = "." {DIGITS} | {DIGITS} "." {DIGIT_OR_UNDERSCORE}*
DEC_EXPONENT = [Ee] [+-]? {DIGIT_OR_UNDERSCORE}*
HEX_FP_LITERAL = {HEX_SIGNIFICAND} {HEX_EXPONENT}
HEX_SIGNIFICAND = 0 [Xx] ({HEX_DIGIT_OR_UNDERSCORE}+ "."? | {HEX_DIGIT_OR_UNDERSCORE}* "." {HEX_DIGIT_OR_UNDERSCORE}+)
HEX_EXPONENT = [Pp] [+-]? {DIGIT_OR_UNDERSCORE}*

ESCAPE_SEQUENCE = \\[^\r\n]
CHARACTER_LITERAL = "'" ([^\\\'\r\n] | {ESCAPE_SEQUENCE})* ("'"|\\)?
STRING_LITERAL = \" ([^\\\"\r\n] | {ESCAPE_SEQUENCE})* (\"|\\)?

TypeIdentifier = {SimpleTypeIdentifier}
SimpleTypeIdentifier = [A-Z][:jletterdigit:]*
SimpleNameIdentifier = [a-z][:jletterdigit:]*
%state  IN_PACKAGE, IN_JAVA_DOC_COMMENT

%%
<YYINITIAL>{

    {C_STYLE_COMMENT} |
    {END_OF_LINE_COMMENT} { return CommentStyle; }
   // {DOC_COMMENT} { return DocCommentStyle; }
   "/**" { yybegin(IN_JAVA_DOC_COMMENT); return DocCommentStyle; }

    {LONG_LITERAL} |
    {INTEGER_LITERAL}  |
    {FLOAT_LITERAL}  |
    {DOUBLE_LITERAL}  { return NumberStyle; }
    {CHARACTER_LITERAL}  |
    {STRING_LITERAL} { return StringStyle; }

"package" { yybegin(IN_PACKAGE); return KeywordStyle;}

"import" {  yybegin(IN_PACKAGE); return KeywordStyle;}

 "true" |
 "false" |
 "null"|
 "abstract" |
 "boolean" |
 "break"  |
 "byte"  |
 "case" |
 "catch" |
 "char" |
 "class" |
 "const" |
 "continue" |
 "default"  |
 "do"  |
 "double" |
 "else"  |
 "extends" |
 "final" |
 "finally" |
 "float"  |
 "for" |
 "goto" |
 "if" |
 "implements" |
 //"import" |
 "instanceof"  |
 "int"  |
 "interface" |
 "long"  |
 "native"  |
 "new"  |
 //"package" |
 "private" |
 "public"  |
 "short" |
 "super" |
 "switch" |
 "synchronized" |
 "this" |
 "throw" |
 "protected" |
 "transient" |
 "return" |
 "void" |
 "static" |
 "strictfp" |
 "while" |
 "try" |
 "volatile" |
 "throws" { return KeywordStyle; }

 "assert" { return myAssertKeyword ? KeywordStyle : PlainStyle; }
 "enum" { return myEnumKeyword ? KeywordStyle : PlainStyle; }
 "var" { return myVarKeyword ? KeywordStyle : PlainStyle; }

 "@" {IDENTIFIER} { return TypeStyle; }

 {TypeIdentifier} { return TypeStyle; }

 {IDENTIFIER} { return IdentifierStyle; }

 "==" |
 "!=" |
 "||" |
 "++" |
 "--" |
 "<"  |
 "<=" |
 "<<="|
 "<<" |
 ">"  |
 "&"  |
 "&&" |
 "+=" |
 "-=" |
 "*=" |
 "/=" |
 "&=" |
 "|=" |
 "^=" |
 "%=" |
 "=" |
 "!" |
 "~" |
 "?" |
 ":" |
 "+" |
 "-" |
 "*" |
 "/" |
 "|" |
 "^" |
 "%" |
 "::" |
 "->" { return OperatorStyle; }

 "("   |
 ")"   |
 "{"   |
 "}"   |
 "["   |
 "]"   |
 ";"   |
 ","   |
 "..." |
 "."   { return SeparatorStyle; }


 "@" |
{WHITE_SPACE_CHAR}+ {return PlainStyle;}

 }

 <IN_PACKAGE> {

  {WS_CHAR}+ { return PlainStyle; }

  ";" { yybegin(YYINITIAL); return SeparatorStyle; }

  {LINE_END_CHAR}+  { yybegin(YYINITIAL); return PlainStyle; }

 "static" { return KeywordStyle; }

 "." { return SeparatorStyle; }

 "*" { return OperatorStyle; }

 {TypeIdentifier} { return TypeStyle; }

 {SimpleNameIdentifier} { return NamespaceStyle; }
 }

 <IN_JAVA_DOC_COMMENT> {
  .|\n { return DocCommentStyle; }

   \* "/"  { yybegin(YYINITIAL); return DocCommentStyle; }

   "@" {IDENTIFIER} { return MetadataStyle;  }

   "<" {IDENTIFIER} ">" { return TypeStyle;  }

   "</" {IDENTIFIER} ">" { return TypeStyle;  }
 }

. { return PlainStyle; }
 <<EOF>>  { return YYEOF; }

