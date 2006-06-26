
//
// Licensed Material 
// (C) Copyright IBM Corp, 2006
//

//
// This is the grammar specification from the Final Draft of the generic spec.
// It has been modified by Philippe Charles and Vijay Saraswat for use with 
// X10. 
// (1) Removed TypeParameters from class/interface/method declarations
// (2) Removed TypeParameters from types.
// (3) Removed Annotations -- cause conflicts with @ used in places.
// (4) Removed EnumDeclarations.
// 12/28/2004

package x10.parser;

public interface X10Parsersym {
    public final static int
      TK_IntegerLiteral = 33,
      TK_LongLiteral = 34,
      TK_FloatingPointLiteral = 35,
      TK_DoubleLiteral = 36,
      TK_CharacterLiteral = 37,
      TK_StringLiteral = 38,
      TK_MINUS_MINUS = 29,
      TK_OR = 95,
      TK_MINUS = 52,
      TK_MINUS_EQUAL = 96,
      TK_NOT = 57,
      TK_NOT_EQUAL = 97,
      TK_REMAINDER = 89,
      TK_REMAINDER_EQUAL = 98,
      TK_AND = 99,
      TK_AND_AND = 100,
      TK_AND_EQUAL = 101,
      TK_LPAREN = 1,
      TK_RPAREN = 6,
      TK_MULTIPLY = 88,
      TK_MULTIPLY_EQUAL = 102,
      TK_COMMA = 53,
      TK_DOT = 51,
      TK_DIVIDE = 90,
      TK_DIVIDE_EQUAL = 103,
      TK_COLON = 55,
      TK_SEMICOLON = 25,
      TK_QUESTION = 114,
      TK_AT = 64,
      TK_LBRACKET = 4,
      TK_RBRACKET = 58,
      TK_XOR = 104,
      TK_XOR_EQUAL = 105,
      TK_LBRACE = 49,
      TK_OR_OR = 115,
      TK_OR_EQUAL = 106,
      TK_RBRACE = 65,
      TK_TWIDDLE = 59,
      TK_PLUS = 54,
      TK_PLUS_PLUS = 30,
      TK_PLUS_EQUAL = 107,
      TK_LESS = 82,
      TK_LEFT_SHIFT = 87,
      TK_LEFT_SHIFT_EQUAL = 108,
      TK_LESS_EQUAL = 91,
      TK_EQUAL = 81,
      TK_EQUAL_EQUAL = 109,
      TK_GREATER = 61,
      TK_ELLIPSIS = 116,
      TK_ARROW = 26,
      TK_abstract = 47,
      TK_assert = 67,
      TK_boolean = 7,
      TK_break = 68,
      TK_byte = 8,
      TK_case = 92,
      TK_catch = 120,
      TK_char = 9,
      TK_class = 56,
      TK_const = 83,
      TK_continue = 69,
      TK_default = 93,
      TK_do = 70,
      TK_double = 10,
      TK_enum = 127,
      TK_else = 110,
      TK_extends = 111,
      TK_false = 39,
      TK_final = 48,
      TK_finally = 121,
      TK_float = 11,
      TK_for = 71,
      TK_goto = 128,
      TK_if = 72,
      TK_implements = 112,
      TK_import = 122,
      TK_instanceof = 94,
      TK_int = 12,
      TK_interface = 60,
      TK_long = 13,
      TK_native = 117,
      TK_new = 31,
      TK_null = 40,
      TK_package = 124,
      TK_private = 44,
      TK_protected = 45,
      TK_public = 43,
      TK_return = 73,
      TK_short = 14,
      TK_static = 46,
      TK_strictfp = 50,
      TK_super = 32,
      TK_switch = 74,
      TK_synchronized = 62,
      TK_this = 28,
      TK_throw = 75,
      TK_throws = 123,
      TK_transient = 84,
      TK_true = 41,
      TK_try = 76,
      TK_void = 27,
      TK_volatile = 85,
      TK_while = 66,
      TK_activitylocal = 129,
      TK_async = 77,
      TK_ateach = 78,
      TK_atomic = 63,
      TK_await = 15,
      TK_boxed = 130,
      TK_clocked = 125,
      TK_current = 21,
      TK_extern = 118,
      TK_finish = 79,
      TK_foreach = 80,
      TK_fun = 131,
      TK_future = 24,
      TK_here = 42,
      TK_local = 22,
      TK_method = 23,
      TK_mutable = 86,
      TK_next = 16,
      TK_now = 17,
      TK_nullable = 18,
      TK_or = 5,
      TK_placelocal = 132,
      TK_reference = 3,
      TK_unsafe = 113,
      TK_value = 2,
      TK_when = 19,
      TK_EOF_TOKEN = 119,
      TK_IDENTIFIER = 20,
      TK_SlComment = 133,
      TK_MlComment = 134,
      TK_DocComment = 135,
      TK_GREATER_EQUAL = 136,
      TK_RIGHT_SHIFT = 137,
      TK_UNSIGNED_RIGHT_SHIFT = 138,
      TK_RIGHT_SHIFT_EQUAL = 139,
      TK_UNSIGNED_RIGHT_SHIFT_EQUAL = 140,
      TK_any = 126,
      TK_ERROR_TOKEN = 141;

      public final static String orderedTerminalSymbols[] = {
                 "",
                 "LPAREN",
                 "value",
                 "reference",
                 "LBRACKET",
                 "or",
                 "RPAREN",
                 "boolean",
                 "byte",
                 "char",
                 "double",
                 "float",
                 "int",
                 "long",
                 "short",
                 "await",
                 "next",
                 "now",
                 "nullable",
                 "when",
                 "IDENTIFIER",
                 "current",
                 "local",
                 "method",
                 "future",
                 "SEMICOLON",
                 "ARROW",
                 "void",
                 "this",
                 "MINUS_MINUS",
                 "PLUS_PLUS",
                 "new",
                 "super",
                 "IntegerLiteral",
                 "LongLiteral",
                 "FloatingPointLiteral",
                 "DoubleLiteral",
                 "CharacterLiteral",
                 "StringLiteral",
                 "false",
                 "null",
                 "true",
                 "here",
                 "public",
                 "private",
                 "protected",
                 "static",
                 "abstract",
                 "final",
                 "LBRACE",
                 "strictfp",
                 "DOT",
                 "MINUS",
                 "COMMA",
                 "PLUS",
                 "COLON",
                 "class",
                 "NOT",
                 "RBRACKET",
                 "TWIDDLE",
                 "interface",
                 "GREATER",
                 "synchronized",
                 "atomic",
                 "AT",
                 "RBRACE",
                 "while",
                 "assert",
                 "break",
                 "continue",
                 "do",
                 "for",
                 "if",
                 "return",
                 "switch",
                 "throw",
                 "try",
                 "async",
                 "ateach",
                 "finish",
                 "foreach",
                 "EQUAL",
                 "LESS",
                 "const",
                 "transient",
                 "volatile",
                 "mutable",
                 "LEFT_SHIFT",
                 "MULTIPLY",
                 "REMAINDER",
                 "DIVIDE",
                 "LESS_EQUAL",
                 "case",
                 "default",
                 "instanceof",
                 "OR",
                 "MINUS_EQUAL",
                 "NOT_EQUAL",
                 "REMAINDER_EQUAL",
                 "AND",
                 "AND_AND",
                 "AND_EQUAL",
                 "MULTIPLY_EQUAL",
                 "DIVIDE_EQUAL",
                 "XOR",
                 "XOR_EQUAL",
                 "OR_EQUAL",
                 "PLUS_EQUAL",
                 "LEFT_SHIFT_EQUAL",
                 "EQUAL_EQUAL",
                 "else",
                 "extends",
                 "implements",
                 "unsafe",
                 "QUESTION",
                 "OR_OR",
                 "ELLIPSIS",
                 "native",
                 "extern",
                 "EOF_TOKEN",
                 "catch",
                 "finally",
                 "import",
                 "throws",
                 "package",
                 "clocked",
                 "any",
                 "enum",
                 "goto",
                 "activitylocal",
                 "boxed",
                 "fun",
                 "placelocal",
                 "SlComment",
                 "MlComment",
                 "DocComment",
                 "GREATER_EQUAL",
                 "RIGHT_SHIFT",
                 "UNSIGNED_RIGHT_SHIFT",
                 "RIGHT_SHIFT_EQUAL",
                 "UNSIGNED_RIGHT_SHIFT_EQUAL",
                 "ERROR_TOKEN"
             };

    public final static boolean isValidForParser = true;
}
