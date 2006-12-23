
//#line 18 "c:/eclipse/workspace-3.1/x10.compiler/src/x10/parser/x10.g"
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
      TK_IntegerLiteral = 37,
      TK_LongLiteral = 38,
      TK_FloatingPointLiteral = 39,
      TK_DoubleLiteral = 40,
      TK_CharacterLiteral = 41,
      TK_StringLiteral = 42,
      TK_MINUS_MINUS = 34,
      TK_OR = 103,
      TK_MINUS = 53,
      TK_MINUS_EQUAL = 109,
      TK_NOT = 60,
      TK_NOT_EQUAL = 93,
      TK_REMAINDER = 89,
      TK_REMAINDER_EQUAL = 110,
      TK_AND = 104,
      TK_AND_AND = 94,
      TK_AND_EQUAL = 111,
      TK_LPAREN = 1,
      TK_RPAREN = 26,
      TK_MULTIPLY = 87,
      TK_MULTIPLY_EQUAL = 112,
      TK_COMMA = 57,
      TK_DOT = 33,
      TK_DIVIDE = 90,
      TK_DIVIDE_EQUAL = 113,
      TK_COLON = 58,
      TK_SEMICOLON = 28,
      TK_QUESTION = 105,
      TK_AT = 65,
      TK_LBRACKET = 4,
      TK_RBRACKET = 63,
      TK_XOR = 95,
      TK_XOR_EQUAL = 114,
      TK_LBRACE = 51,
      TK_OR_OR = 106,
      TK_OR_EQUAL = 115,
      TK_RBRACE = 69,
      TK_TWIDDLE = 64,
      TK_PLUS = 54,
      TK_PLUS_PLUS = 35,
      TK_PLUS_EQUAL = 116,
      TK_LESS = 66,
      TK_LEFT_SHIFT = 92,
      TK_LEFT_SHIFT_EQUAL = 117,
      TK_LESS_EQUAL = 91,
      TK_EQUAL = 71,
      TK_EQUAL_EQUAL = 96,
      TK_GREATER = 59,
      TK_ELLIPSIS = 118,
      TK_ARROW = 30,
      TK_abstract = 55,
      TK_assert = 72,
      TK_boolean = 7,
      TK_break = 73,
      TK_byte = 8,
      TK_case = 107,
      TK_catch = 122,
      TK_char = 9,
      TK_class = 61,
      TK_const = 97,
      TK_continue = 74,
      TK_default = 108,
      TK_do = 75,
      TK_double = 10,
      TK_enum = 134,
      TK_else = 119,
      TK_extends = 123,
      TK_false = 43,
      TK_final = 52,
      TK_finally = 124,
      TK_float = 11,
      TK_for = 76,
      TK_goto = 135,
      TK_if = 77,
      TK_implements = 98,
      TK_import = 125,
      TK_instanceof = 102,
      TK_int = 12,
      TK_interface = 62,
      TK_long = 13,
      TK_native = 126,
      TK_new = 44,
      TK_null = 45,
      TK_package = 131,
      TK_private = 48,
      TK_protected = 49,
      TK_property = 78,
      TK_public = 36,
      TK_return = 79,
      TK_short = 14,
      TK_static = 50,
      TK_strictfp = 56,
      TK_super = 32,
      TK_switch = 80,
      TK_synchronized = 67,
      TK_this = 29,
      TK_throw = 81,
      TK_throws = 127,
      TK_transient = 99,
      TK_true = 46,
      TK_try = 82,
      TK_void = 31,
      TK_volatile = 100,
      TK_while = 70,
      TK_activitylocal = 136,
      TK_async = 83,
      TK_ateach = 84,
      TK_atomic = 68,
      TK_await = 15,
      TK_boxed = 137,
      TK_clocked = 132,
      TK_compilertest = 138,
      TK_current = 22,
      TK_extern = 128,
      TK_finish = 85,
      TK_foreach = 86,
      TK_fun = 139,
      TK_future = 27,
      TK_here = 47,
      TK_local = 16,
      TK_method = 23,
      TK_mutable = 101,
      TK_next = 17,
      TK_nonblocking = 129,
      TK_now = 18,
      TK_nullable = 19,
      TK_or = 5,
      TK_placelocal = 140,
      TK_reference = 24,
      TK_safe = 3,
      TK_self = 120,
      TK_sequential = 130,
      TK_unsafe = 6,
      TK_value = 2,
      TK_when = 20,
      TK_EOF_TOKEN = 121,
      TK_IDENTIFIER = 21,
      TK_SlComment = 141,
      TK_MlComment = 142,
      TK_DocComment = 143,
      TK_GREATER_EQUAL = 144,
      TK_RIGHT_SHIFT = 145,
      TK_UNSIGNED_RIGHT_SHIFT = 146,
      TK_RIGHT_SHIFT_EQUAL = 147,
      TK_UNSIGNED_RIGHT_SHIFT_EQUAL = 148,
      TK_LOCATION = 149,
      TK_location = 25,
      TK_ErrorId = 88,
      TK_any = 133,
      TK_ERROR_TOKEN = 150;

      public final static String orderedTerminalSymbols[] = {
                 "",
                 "LPAREN",
                 "value",
                 "safe",
                 "LBRACKET",
                 "or",
                 "unsafe",
                 "boolean",
                 "byte",
                 "char",
                 "double",
                 "float",
                 "int",
                 "long",
                 "short",
                 "await",
                 "local",
                 "next",
                 "now",
                 "nullable",
                 "when",
                 "IDENTIFIER",
                 "current",
                 "method",
                 "reference",
                 "location",
                 "RPAREN",
                 "future",
                 "SEMICOLON",
                 "this",
                 "ARROW",
                 "void",
                 "super",
                 "DOT",
                 "MINUS_MINUS",
                 "PLUS_PLUS",
                 "public",
                 "IntegerLiteral",
                 "LongLiteral",
                 "FloatingPointLiteral",
                 "DoubleLiteral",
                 "CharacterLiteral",
                 "StringLiteral",
                 "false",
                 "new",
                 "null",
                 "true",
                 "here",
                 "private",
                 "protected",
                 "static",
                 "LBRACE",
                 "final",
                 "MINUS",
                 "PLUS",
                 "abstract",
                 "strictfp",
                 "COMMA",
                 "COLON",
                 "GREATER",
                 "NOT",
                 "class",
                 "interface",
                 "RBRACKET",
                 "TWIDDLE",
                 "AT",
                 "LESS",
                 "synchronized",
                 "atomic",
                 "RBRACE",
                 "while",
                 "EQUAL",
                 "assert",
                 "break",
                 "continue",
                 "do",
                 "for",
                 "if",
                 "property",
                 "return",
                 "switch",
                 "throw",
                 "try",
                 "async",
                 "ateach",
                 "finish",
                 "foreach",
                 "MULTIPLY",
                 "ErrorId",
                 "REMAINDER",
                 "DIVIDE",
                 "LESS_EQUAL",
                 "LEFT_SHIFT",
                 "NOT_EQUAL",
                 "AND_AND",
                 "XOR",
                 "EQUAL_EQUAL",
                 "const",
                 "implements",
                 "transient",
                 "volatile",
                 "mutable",
                 "instanceof",
                 "OR",
                 "AND",
                 "QUESTION",
                 "OR_OR",
                 "case",
                 "default",
                 "MINUS_EQUAL",
                 "REMAINDER_EQUAL",
                 "AND_EQUAL",
                 "MULTIPLY_EQUAL",
                 "DIVIDE_EQUAL",
                 "XOR_EQUAL",
                 "OR_EQUAL",
                 "PLUS_EQUAL",
                 "LEFT_SHIFT_EQUAL",
                 "ELLIPSIS",
                 "else",
                 "self",
                 "EOF_TOKEN",
                 "catch",
                 "extends",
                 "finally",
                 "import",
                 "native",
                 "throws",
                 "extern",
                 "nonblocking",
                 "sequential",
                 "package",
                 "clocked",
                 "any",
                 "enum",
                 "goto",
                 "activitylocal",
                 "boxed",
                 "compilertest",
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
                 "LOCATION",
                 "ERROR_TOKEN"
             };

    public final static boolean isValidForParser = true;
}
