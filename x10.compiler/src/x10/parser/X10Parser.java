
//
// This is the grammar specification from the Final Draft of the generic spec.
// It has been modified by Philippe Charles and Vijay Saraswat for use with 
// X10. 
// (1) Removed TypeParameters from class/interface/method declarations
// (2) Removed TypeParameters from types.
// (3) Removed Annotations -- cause conflicts with @ used in places.
// (4) Removed EnumDeclarations.
// 12/28/2004// 12/25/2004
// This is the basic X10 grammar specification without support for generic types.
//Intended for the Feb 2005 X10 release.

package x10.parser;

import java.util.*;
import polyglot.ast.*;
import polyglot.lex.*;
import polyglot.util.*;
import polyglot.parse.*;
import polyglot.types.*;
import polyglot.*;
import polyglot.ast.Assert;
import polyglot.ext.x10.ast.*;
import polyglot.ext.x10.types.X10TypeSystem;
import polyglot.ext.x10.ast.X10NodeFactory;
import polyglot.ext.jl.parse.Name;
import polyglot.frontend.Parser;
import polyglot.frontend.FileSource;
import polyglot.main.Report;

import com.ibm.lpg.*;

public class X10Parser extends PrsStream implements RuleAction, Parser
{
    X10Parser prsStream;
    LexStream lexStream;
    ParseTable prs;
    BacktrackingParser btParser;

    public X10Parser(LexStream lexStream)
    {
        super(lexStream);
        this.lexStream = lexStream;            
        this.prsStream = this;
        this.prs = new X10Parserprs();

        try
        {
            prsStream.remapTerminalSymbols(orderedTerminalSymbols(), X10Parserprs.EOFT_SYMBOL);
        }
        catch(NullExportedSymbolsException e) {
        }
        catch(NullTerminalSymbolsException e) {
        }
        catch(UnimplementedTerminalsException e)
        {
            java.util.ArrayList unimplemented_symbols = e.getSymbols();
            System.out.println("The Lexer will not scan the following token(s):");
            for (int i = 0; i < unimplemented_symbols.size(); i++)
            {
                Integer id = (Integer) unimplemented_symbols.get(i);
                System.out.println("    " + X10Parsersym.orderedTerminalSymbols[id.intValue()]);               
            }
            System.out.println();                        
        }
        catch(UndefinedEofSymbolException e)
        {
            System.out.println("The Lexer does not implement the Eof symbol " +
                               X10Parsersym.orderedTerminalSymbols[X10Parserprs.EOFT_SYMBOL]);
            throw new Error(e);
        } 
    }

    public String[] orderedTerminalSymbols() { return X10Parsersym.orderedTerminalSymbols; }
        
    public polyglot.ast.Node parser()
    {
        try
        {
            btParser = new BacktrackingParser(this, prs, this);
        }
        catch (NotBacktrackParseTableException e)
        {
            System.out.println("****Error: Regenerate X10Parserprs.java with -BACKTRACK option");
            throw new Error(e);
        }
        catch (BadParseSymFileException e)
        {
            System.out.println("****Error: Bad Parser Symbol File -- X10Parsersym.java");
            throw new Error(e);
        }

        try
        {
            return (polyglot.ast.Node) btParser.parse();
        }
        catch (BadParseException e)
        {
            reset(e.error_token); // point to error token
            DiagnoseParser diagnoseParser = new DiagnoseParser(this, prs);
            diagnoseParser.diagnose(e.error_token);
        }

        return null;
    }

    private ErrorQueue eq;
    private X10TypeSystem ts;
    private X10NodeFactory nf;
    private FileSource source;

        
    public X10Parser(LexStream lexStream, TypeSystem t, NodeFactory n, FileSource source, ErrorQueue q)
    {
        this(lexStream);
        this.ts = (X10TypeSystem) t;
        this.nf = (X10NodeFactory) n;
        this.source = source;
        this.eq = q;
    }

    public polyglot.ast.Node parse() {
        try
        {
            SourceFile sf = (SourceFile) parser();
            if (bad_rule != 0)
                throw new RuntimeException("Rule " + bad_rule + " has not yet been implemented");

            if (sf != null)
                return sf.source(source);

            eq.enqueue(ErrorInfo.SYNTAX_ERROR, "Unable to parse " + source.name() + ".");
        }
        catch (RuntimeException e) {
            // Let the Compiler catch and report it.
            throw e;
        }
        catch (Exception e) {
            // Used by cup to indicate a non-recoverable error.
            eq.enqueue(ErrorInfo.SYNTAX_ERROR, e.getMessage());
        }

        return null;
    }
    
    public String file()
    {
        return prsStream.getFileName();
    }

    private Position pos()
    {
        int i = btParser.getFirstToken(),
            j = btParser.getLastToken();
        return new Position(prsStream.getFileName(),
                            prsStream.getLine(i),
                            prsStream.getColumn(i),
                            prsStream.getEndLine(j),
                            prsStream.getEndColumn(j));
    }

    private Position pos(int i)
    {
        return new Position(prsStream.getFileName(),
                            prsStream.getLine(i),
                            prsStream.getColumn(i),
                            prsStream.getEndLine(i),
                            prsStream.getEndColumn(i));
    }

    private Position pos(int i, int j)
    {
        return new Position(prsStream.getFileName(),
                            prsStream.getLine(i),
                            prsStream.getColumn(i),
                            prsStream.getEndLine(j),
                            prsStream.getEndColumn(j));
    }

    /**
     * Return the source position of the declaration.
     */
    public Position pos (VarDeclarator n)
    {
      if (n == null) return null;
      return n.pos;
    }

    private polyglot.lex.Operator op(int i) {
        return new Operator(pos(i), prsStream.getName(i), prsStream.getKind(i));
    }

    private polyglot.lex.Identifier id(int i) {
        return new Identifier(pos(i), prsStream.getName(i), X10Parsersym.TK_IDENTIFIER);
    }
    private String comment(int i) {
        String s = prsStream.getName(i);
        if (s != null && s.startsWith("/**") && s.endsWith("*/")) {
            return s +"\n";
        }
        return null;
    }

   /** Pretend to have parsed new <T>Array.pointwiseOp 
     * { public <T> apply(Formal) MethodBody } 
     * instead of (Formal) MethodBody. Note that Formal may have 
     * exploded vars.
     * @author vj
    */
    private New makeInitializer( Position pos, TypeNode resultType, 
                                 X10Formal f, Block body ) {
      if (f.hasExplodedVars()) {
        List s = new TypedList(new LinkedList(), Stmt.class, false);
        s.addAll( f.explode() );
        s.addAll( body.statements() );
        body = body.statements( s );
      }
      Flags flags = Flags.PUBLIC;
      // resulttype is a.
      List l1 = new TypedList(new LinkedList(), X10Formal.class, false);
      l1.add(f);
      MethodDecl decl = nf.MethodDecl(pos, flags, resultType, 
                                    "apply", l1,
                                      new LinkedList(), body);
      //  new ClassOrInterfaceType ( ArgumentListopt ) ClassBodyopt
      Name tArray = new Name(nf, ts, pos, resultType.toString() + "Array");
      Name tArrayPointwiseOp = new Name(nf, ts, pos, tArray, "pointwiseOp");
      List classDecl = new TypedList(new LinkedList(), MethodDecl.class, false);
      classDecl.add( decl );
      New initializer = nf.New(pos, tArrayPointwiseOp.toType(), new LinkedList(), 
                             nf.ClassBody( pos, classDecl ) );
      return initializer;
    }
    /* Roll our own integer parser.  We can't use Long.parseLong because
     * it doesn't handle numbers greater than 0x7fffffffffffffff correctly.
     */
    private long parseLong(String s, int radix)
    {
        long x = 0L;

        s = s.toLowerCase();

        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);

            if (c < '0' || c > '9') {
                c = c - 'a' + 10;
            }
            else {
                c = c - '0';
            }

            x *= radix;
            x += c;
        }

        return x;
    }

    private long parseLong(String s)
    {
        int radix,
            start_index,
            end_index = (s.charAt(s.length() - 1) == 'l' || s.charAt(s.length() - 1) == 'L'
                                                   ? s.length() - 1
                                                   : s.length());
        if (s.charAt(0) == '0')
        {
           if (s.length() > 1 && (s.charAt(1) == 'x' || s.charAt(1) == 'X'))
           {
               radix = 16;
               start_index = 2;
           }
           else
           {
               radix = 8;
               start_index = 0;
           }
        }
        else
        {
            radix = 10;
            start_index = 0;
        }

        return parseLong(s.substring(start_index, end_index), radix);
    }

    private polyglot.lex.IntegerLiteral int_lit(int i, int radix)
    {
        long x = parseLong(prsStream.getName(i), radix);
        return new IntegerLiteral(pos(i), (int) x, X10Parsersym.TK_IntegerLiteral);
    }

    private polyglot.lex.IntegerLiteral int_lit(int i)
    {
        long x = parseLong(prsStream.getName(i));
        return new IntegerLiteral(pos(i), (int) x, X10Parsersym.TK_IntegerLiteral);
    }

    private polyglot.lex.LongLiteral long_lit(int i, int radix)
    {
        long x = parseLong(prsStream.getName(i), radix);
        return new LongLiteral(pos(i), x, X10Parsersym.TK_LongLiteral);
    }

    private polyglot.lex.LongLiteral long_lit(int i)
    {
        long x = parseLong(prsStream.getName(i));
        return new LongLiteral(pos(i), x, X10Parsersym.TK_LongLiteral);
    }

    private polyglot.lex.FloatLiteral float_lit(int i)
    {
        try {
            String s = prsStream.getName(i);
            int end_index = (s.charAt(s.length() - 1) == 'f' || s.charAt(s.length() - 1) == 'F'
                                                       ? s.length() - 1
                                                       : s.length());
            float x = Float.parseFloat(s.substring(0, end_index));
            return new FloatLiteral(pos(i), x, X10Parsersym.TK_FloatingPointLiteral);
        }
        catch (NumberFormatException e) {
            eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                       "Illegal float literal \"" + prsStream.getName(i) + "\"", pos(i));
            return null;
        }
    }

    private polyglot.lex.DoubleLiteral double_lit(int i)
    {
        try {
            String s = prsStream.getName(i);
            int end_index = (s.charAt(s.length() - 1) == 'd' || s.charAt(s.length() - 1) == 'D'
                                                       ? s.length() - 1
                                                       : s.length());
            double x = Double.parseDouble(s.substring(0, end_index));
            return new DoubleLiteral(pos(i), x, X10Parsersym.TK_DoubleLiteral);
        }
        catch (NumberFormatException e) {
            eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                       "Illegal float literal \"" + prsStream.getName(i) + "\"", pos(i));
            return null;
        }
    }

    private polyglot.lex.CharacterLiteral char_lit(int i)
    {
        char x;
        String s = prsStream.getName(i);
        if (s.charAt(1) == '\\') {
            switch(s.charAt(2)) {
                case 'u':
                    x = (char) parseLong(s.substring(3, s.length() - 1), 16);
                    break;
                case 'b':
                    x = '\b';
                    break;
                case 't':
                    x = '\t';
                    break;
                case 'n':
                    x = '\n';
                    break;
                case 'f':
                    x = '\f';
                    break;
                case 'r':
                    x = '\r';
                    break;
                case '\"':
                    x = '\"';
                    break;
                case '\'':
                    x = '\'';
                    break;
                case '\\':
                    x = '\\';
                    break;
                default:
                    x = (char) parseLong(s.substring(2, s.length() - 1), 8);
                    if (x > 255)
                        eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                                   "Illegal character literal " + s, pos(i));
        }
        }
        else {
            assert(s.length() == 3);
            x = s.charAt(1);
        }

        return new CharacterLiteral(pos(i), x, X10Parsersym.TK_CharacterLiteral);
    }

    private polyglot.lex.BooleanLiteral boolean_lit(int i)
    {
        return new BooleanLiteral(pos(i), prsStream.getKind(i) == X10Parsersym.TK_true, prsStream.getKind(i));
    }

    private polyglot.lex.StringLiteral string_lit(int i)
    {
        String s = prsStream.getName(i);
        char x[] = new char[s.length()];
        int j = 1,
            k = 0;
        while(j < s.length() - 1) {
            if (s.charAt(j) != '\\')
                x[k++] = s.charAt(j++);
            else {
                switch(s.charAt(j + 1)) {
                    case 'u':
                        x[k++] = (char) parseLong(s.substring(j + 2, j + 6), 16);
                        j += 6;
                        break;
                    case 'b':
                        x[k++] = '\b';
                        j += 2;
                        break;
                    case 't':
                        x[k++] = '\t';
                        j += 2;
                        break;
                    case 'n':
                        x[k++] = '\n';
                        j += 2;
                        break;
                    case 'f':
                        x[k++] = '\f';
                        j += 2;
                        break;
                    case 'r':
                        x[k++] = '\r';
                        j += 2;
                        break;
                    case '\"':
                        x[k++] = '\"';
                        j += 2;
                        break;
                    case '\'':
                        x[k++] = '\'';
                        j += 2;
                        break;
                    case '\\':
                        x[k++] = '\\';
                        j += 2;
                        break;
                    default:
                    {
                        int n = j + 1;
                        while(Character.isDigit(s.charAt(n)))
                            n++;
                        char c = (char) parseLong(s.substring(j + 1, n), 8);
                        if (c > 255)
                            eq.enqueue(ErrorInfo.LEXICAL_ERROR,
                                       "Illegal character (" + s.substring(j, n) + ") in string literal " + s, pos(i));
                        x[k++] = c;
                        j = n;
                    }
                }
            }
        }

        return new StringLiteral(pos(i), new String(x, 0, k), X10Parsersym.TK_StringLiteral);
    }

    private polyglot.lex.NullLiteral null_lit(int i)
    {
        return new NullLiteral(pos(i), X10Parsersym.TK_null);
    }



    int bad_rule = 0;

    public void ruleAction(int ruleNumber)
    {
        if (bad_rule != 0)
            return;

        switch (ruleNumber)
        {
 
            //
            // Rule 1:  identifier ::= IDENTIFIER
            //
            case 1: {
     if (prsStream.getKind(btParser.getToken(1)) != X10Parsersym.TK_IDENTIFIER
         && (Report.should_report("parser", 2))) {
         Report.report(2,"Parser turning keyword " +
                       prsStream.getName(btParser.getToken(1)) +
                       " at " +
                       prsStream.getLine(btParser.getToken(1)) +
                       ":" +
                       prsStream.getColumn(btParser.getToken(1)) +
                       " into an identifier");
      }
                break;
            }
     
            //
            // Rule 2:  PrimitiveType ::= NumericType
            //
            case 2:
                break;
 
            //
            // Rule 3:  PrimitiveType ::= boolean
            //
            case 3: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Boolean()));
                break;
            }
     
            //
            // Rule 4:  NumericType ::= IntegralType
            //
            case 4:
                break;
 
            //
            // Rule 5:  NumericType ::= FloatingPointType
            //
            case 5:
                break;
 
            //
            // Rule 6:  IntegralType ::= byte
            //
            case 6: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Byte()));
                break;
            }
     
            //
            // Rule 7:  IntegralType ::= char
            //
            case 7: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Char()));
                break;
            }
     
            //
            // Rule 8:  IntegralType ::= short
            //
            case 8: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Short()));
                break;
            }
     
            //
            // Rule 9:  IntegralType ::= int
            //
            case 9: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Int()));
                break;
            }
     
            //
            // Rule 10:  IntegralType ::= long
            //
            case 10: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Long()));
                break;
            }
     
            //
            // Rule 11:  FloatingPointType ::= float
            //
            case 11: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Float()));
                break;
            }
     
            //
            // Rule 12:  FloatingPointType ::= double
            //
            case 12: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Double()));
                break;
            }
     
            //
            // Rule 13:  ReferenceType ::= ClassOrInterfaceType
            //
            case 13:
                break;
 
            //
            // Rule 14:  ReferenceType ::= ArrayType
            //
            case 14:
                break;
 
            //
            // Rule 15:  ClassType ::= TypeName
            //
            case 15: {
//vj                    assert(btParser.getSym(2) == null); // generic not yet supported
                Name a = (Name) btParser.getSym(1);
                btParser.setSym1(a.toType());
                break;
            }
     
            //
            // Rule 16:  InterfaceType ::= TypeName
            //
            case 16: {
//vj                    assert(btParser.getSym(2) == null); // generic not yet supported
                Name a = (Name) btParser.getSym(1);
                btParser.setSym1(a.toType());
                break;
            }
     
            //
            // Rule 17:  TypeName ::= identifier
            //
            case 17: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 18:  TypeName ::= TypeName . identifier
            //
            case 18: {
                Name a = (Name) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(new Name(nf,
                                  ts,
                                  pos(btParser.getFirstToken(), btParser.getLastToken()),
                                  a,
                                  b.getIdentifier()));
                break;
            }
     
            //
            // Rule 19:  ClassName ::= TypeName
            //
            case 19:
                break;
 
            //
            // Rule 20:  TypeVariable ::= identifier
            //
            case 20:
                break;
 
            //
            // Rule 21:  ArrayType ::= Type [ ]
            //
            case 21: {
                TypeNode a = (TypeNode) btParser.getSym(1);
                btParser.setSym1(nf.array(a, pos(), 1));
                break;
            }
     
            //
            // Rule 22:  TypeParameter ::= TypeVariable TypeBoundopt
            //
            case 22:
                bad_rule = 22;
                break;
 
            //
            // Rule 23:  TypeBound ::= extends ClassOrInterfaceType AdditionalBoundListopt
            //
            case 23:
                bad_rule = 23;
                break;
 
            //
            // Rule 24:  AdditionalBoundList ::= AdditionalBound
            //
            case 24:
                bad_rule = 24;
                break;
 
            //
            // Rule 25:  AdditionalBoundList ::= AdditionalBoundList AdditionalBound
            //
            case 25:
                bad_rule = 25;
                break;
 
            //
            // Rule 26:  AdditionalBound ::= & InterfaceType
            //
            case 26:
                bad_rule = 26;
                break;
 
            //
            // Rule 27:  TypeArguments ::= < ActualTypeArgumentList >
            //
            case 27:
                bad_rule = 27;
                break;
 
            //
            // Rule 28:  ActualTypeArgumentList ::= ActualTypeArgument
            //
            case 28:
                bad_rule = 28;
                break;
 
            //
            // Rule 29:  ActualTypeArgumentList ::= ActualTypeArgumentList , ActualTypeArgument
            //
            case 29:
                bad_rule = 29;
                break;
 
            //
            // Rule 30:  Wildcard ::= ? WildcardBoundsOpt
            //
            case 30:
                bad_rule = 30;
                break;
 
            //
            // Rule 31:  WildcardBounds ::= extends ReferenceType
            //
            case 31:
                bad_rule = 31;
                break;
 
            //
            // Rule 32:  WildcardBounds ::= super ReferenceType
            //
            case 32:
                bad_rule = 32;
                break;
 
            //
            // Rule 33:  PackageName ::= identifier
            //
            case 33: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 34:  PackageName ::= PackageName . identifier
            //
            case 34: {
                Name a = (Name) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(new Name(nf,
                                  ts,
                                  pos(btParser.getFirstToken(), btParser.getLastToken()),
                                  a,
                                  b.getIdentifier()));
                break;
            }
     
            //
            // Rule 35:  ExpressionName ::= identifier
            //
            case 35: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 36:  ExpressionName ::= AmbiguousName . identifier
            //
            case 36: {
                Name a = (Name) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(new Name(nf,
                                  ts,
                                  pos(btParser.getFirstToken(), btParser.getLastToken()),
                                  a,
                                  b.getIdentifier()));
                break;
            }
     
            //
            // Rule 37:  MethodName ::= identifier
            //
            case 37: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 38:  MethodName ::= AmbiguousName . identifier
            //
            case 38: {
                Name a = (Name) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(new Name(nf,
                                  ts,
                                  pos(btParser.getFirstToken(), btParser.getLastToken()),
                                  a,
                                  b.getIdentifier()));
                break;
            }
     
            //
            // Rule 39:  PackageOrTypeName ::= identifier
            //
            case 39: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 40:  PackageOrTypeName ::= PackageOrTypeName . identifier
            //
            case 40: {
                Name a = (Name) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(new Name(nf,
                                  ts,
                                  pos(btParser.getFirstToken(), btParser.getLastToken()),
                                  a,
                                  b.getIdentifier()));
                break;
            }
     
            //
            // Rule 41:  AmbiguousName ::= identifier
            //
            case 41: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 42:  AmbiguousName ::= AmbiguousName . identifier
            //
            case 42: {
                Name a = (Name) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(new Name(nf,
                                  ts,
                                  pos(btParser.getFirstToken(), btParser.getLastToken()),
                                  a,
                                  b.getIdentifier()));
               break;
            }
     
            //
            // Rule 43:  CompilationUnit ::= PackageDeclarationopt ImportDeclarationsopt TypeDeclarationsopt
            //
            case 43: {
                PackageNode a = (PackageNode) btParser.getSym(1);
                List b = (List) btParser.getSym(2),
                     c = (List) btParser.getSym(3);
                // Add import x10.lang.* by default.
                Name x10 = new Name(nf, ts, pos(), "x10");
                Name x10Lang = new Name(nf, ts, pos(), x10, "lang");
                Import x10LangImport = 
                nf.Import(pos(btParser.getFirstToken(), btParser.getLastToken()), Import.PACKAGE, x10Lang.toString());
                b.add(x10LangImport);
                btParser.setSym1(nf.SourceFile(pos(btParser.getFirstToken(), btParser.getLastToken()), a, b, c));
                break;
            }
     
            //
            // Rule 44:  ImportDeclarations ::= ImportDeclaration
            //
            case 44: {
                List l = new TypedList(new LinkedList(), Import.class, false);
                Import a = (Import) btParser.getSym(1);
                l.add(a);
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 45:  ImportDeclarations ::= ImportDeclarations ImportDeclaration
            //
            case 45: {
                List l = (TypedList) btParser.getSym(1);
                Import b = (Import) btParser.getSym(2);
                if (b != null)
                    l.add(b);
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 46:  TypeDeclarations ::= TypeDeclaration
            //
            case 46: {
                List l = new TypedList(new LinkedList(), TopLevelDecl.class, false);
                TopLevelDecl a = (TopLevelDecl) btParser.getSym(1);
                if (a != null)
                    l.add(a);
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 47:  TypeDeclarations ::= TypeDeclarations TypeDeclaration
            //
            case 47: {
                List l = (TypedList) btParser.getSym(1);
                TopLevelDecl b = (TopLevelDecl) btParser.getSym(2);
                if (b != null)
                    l.add(b);
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 48:  PackageDeclaration ::= package PackageName ;
            //
            case 48: {
//vj                    assert(btParser.getSym(1) == null); // generic not yet supported
                Name a = (Name) btParser.getSym(2);
                btParser.setSym1(a.toPackage());
                break;
            }
     
            //
            // Rule 49:  ImportDeclaration ::= SingleTypeImportDeclaration
            //
            case 49:
                break;
 
            //
            // Rule 50:  ImportDeclaration ::= TypeImportOnDemandDeclaration
            //
            case 50:
                break;
 
            //
            // Rule 51:  ImportDeclaration ::= SingleStaticImportDeclaration
            //
            case 51:
                break;
 
            //
            // Rule 52:  ImportDeclaration ::= StaticImportOnDemandDeclaration
            //
            case 52:
                break;
 
            //
            // Rule 53:  SingleTypeImportDeclaration ::= import TypeName ;
            //
            case 53: {
                Name a = (Name) btParser.getSym(2);
                btParser.setSym1(nf.Import(pos(btParser.getFirstToken(), btParser.getLastToken()), Import.CLASS, a.toString()));
                break;
            }
     
            //
            // Rule 54:  TypeImportOnDemandDeclaration ::= import PackageOrTypeName . * ;
            //
            case 54: {
                Name a = (Name) btParser.getSym(2);
                btParser.setSym1(nf.Import(pos(btParser.getFirstToken(), btParser.getLastToken()), Import.PACKAGE, a.toString()));
                break;
            }
     
            //
            // Rule 55:  SingleStaticImportDeclaration ::= import static TypeName . identifier ;
            //
            case 55:
                bad_rule = 55;
                break;
 
            //
            // Rule 56:  StaticImportOnDemandDeclaration ::= import static TypeName . * ;
            //
            case 56:
                bad_rule = 56;
                break;
 
            //
            // Rule 57:  TypeDeclaration ::= ClassDeclaration
            //
            case 57:
                break;
 
            //
            // Rule 58:  TypeDeclaration ::= InterfaceDeclaration
            //
            case 58:
                break;
 
            //
            // Rule 59:  TypeDeclaration ::= ;
            //
            case 59: {
                btParser.setSym1(null);
                break;
            }
     
            //
            // Rule 60:  ClassDeclaration ::= NormalClassDeclaration
            //
            case 60:
                break;
 
            //
            // Rule 61:  NormalClassDeclaration ::= ClassModifiersopt class identifier Superopt Interfacesopt ClassBody
            //
            case 61: {
                Flags a = (Flags) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
//vj                    assert(btParser.getSym(4) == null);
                TypeNode c = (TypeNode) btParser.getSym(4);
                // by default extend x10.lang.Object
                if (c == null) {
                  c= new Name(nf, ts, pos(), "x10.lang.Object").toType();
                }
                List d = (List) btParser.getSym(5);
                ClassBody e = (ClassBody) btParser.getSym(6);
                btParser.setSym1(a.isValue()
                             ? nf.ValueClassDecl(pos(btParser.getFirstToken(), btParser.getLastToken()), 
                                                 a, b.getIdentifier(), c, d, e) 
                             : nf.ClassDecl(pos(btParser.getFirstToken(), btParser.getLastToken()), 
                                            a, b.getIdentifier(), c, d, e));
                break;
            }
     
            //
            // Rule 62:  ClassModifiers ::= ClassModifier
            //
            case 62:
                break;
 
            //
            // Rule 63:  ClassModifiers ::= ClassModifiers ClassModifier
            //
            case 63: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 64:  ClassModifier ::= public
            //
            case 64: {
                btParser.setSym1(Flags.PUBLIC);
                break;
            }
     
            //
            // Rule 65:  ClassModifier ::= protected
            //
            case 65: {
                btParser.setSym1(Flags.PROTECTED);
                break;
            }
     
            //
            // Rule 66:  ClassModifier ::= private
            //
            case 66: {
                btParser.setSym1(Flags.PRIVATE);
                break;
            }
     
            //
            // Rule 67:  ClassModifier ::= abstract
            //
            case 67: {
                btParser.setSym1(Flags.ABSTRACT);
                break;
            }
     
            //
            // Rule 68:  ClassModifier ::= static
            //
            case 68: {
                btParser.setSym1(Flags.STATIC);
                break;
            }
     
            //
            // Rule 69:  ClassModifier ::= final
            //
            case 69: {
                btParser.setSym1(Flags.FINAL);
                break;
            }
     
            //
            // Rule 70:  ClassModifier ::= strictfp
            //
            case 70: {
                btParser.setSym1(Flags.STRICTFP);
                break;
            }
     
            //
            // Rule 71:  TypeParameters ::= < TypeParameterList >
            //
            case 71:
                bad_rule = 71;
                break;
 
            //
            // Rule 72:  TypeParameterList ::= TypeParameter
            //
            case 72:
                bad_rule = 72;
                break;
 
            //
            // Rule 73:  TypeParameterList ::= TypeParameterList , TypeParameter
            //
            case 73:
                bad_rule = 73;
                break;
 
            //
            // Rule 74:  Super ::= extends ClassType
            //
            case 74: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 75:  Interfaces ::= implements InterfaceTypeList
            //
            case 75: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 76:  InterfaceTypeList ::= InterfaceType
            //
            case 76: {
                List l = new TypedList(new LinkedList(), TypeNode.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 77:  InterfaceTypeList ::= InterfaceTypeList , InterfaceType
            //
            case 77: {
                List l = (TypedList) btParser.getSym(1);
                l.add(btParser.getSym(2));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 78:  ClassBody ::= { ClassBodyDeclarationsopt }
            //
            case 78: {
                btParser.setSym1(nf.ClassBody(pos(btParser.getFirstToken(), btParser.getLastToken()), (List) btParser.getSym(2)));
                break;
            }
     
            //
            // Rule 79:  ClassBodyDeclarations ::= ClassBodyDeclaration
            //
            case 79:
                break;
 
            //
            // Rule 80:  ClassBodyDeclarations ::= ClassBodyDeclarations ClassBodyDeclaration
            //
            case 80: {
                List a = (List) btParser.getSym(1),
                     b = (List) btParser.getSym(2);
                a.addAll(b);
                // btParser.setSym1(a);
                break;
            }
     
            //
            // Rule 81:  ClassBodyDeclaration ::= ClassMemberDeclaration
            //
            case 81:
                break;
 
            //
            // Rule 82:  ClassBodyDeclaration ::= InstanceInitializer
            //
            case 82: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                Block a = (Block) btParser.getSym(1);
                l.add(nf.Initializer(pos(), Flags.NONE, a));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 83:  ClassBodyDeclaration ::= StaticInitializer
            //
            case 83: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                Block a = (Block) btParser.getSym(1);
                l.add(nf.Initializer(pos(), Flags.STATIC, a));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 84:  ClassBodyDeclaration ::= ConstructorDeclaration
            //
            case 84: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 85:  ClassMemberDeclaration ::= FieldDeclaration
            //
            case 85:
                break;
 
            //
            // Rule 86:  ClassMemberDeclaration ::= MethodDeclaration
            //
            case 86: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 87:  ClassMemberDeclaration ::= ClassDeclaration
            //
            case 87: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 88:  ClassMemberDeclaration ::= InterfaceDeclaration
            //
            case 88: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 89:  ClassMemberDeclaration ::= ;
            //
            case 89: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 90:  FieldDeclaration ::= FieldModifiersopt Type VariableDeclarators ;
            //
            case 90: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                Flags a = (Flags) btParser.getSym(1);
                TypeNode b = (TypeNode) btParser.getSym(2);
                List c = (List) btParser.getSym(3);
                for (Iterator i = c.iterator(); i.hasNext();)
                {
                    X10VarDeclarator d = (X10VarDeclarator) i.next();
                    if (d.hasExplodedVars())
                      // TODO: Report this exception correctly.
                      throw new Error("Field Declarations may not have exploded variables." + pos());
                    d.setFlag(a);
                    l.add(nf.FieldDecl(pos(btParser.getFirstToken(2), btParser.getLastToken()),
                                       d.flags,
                                       nf.array(b, pos(btParser.getFirstToken(2), btParser.getLastToken(2)), d.dims),
                                       d.name,
                                       d.init));
                }
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 91:  VariableDeclarators ::= VariableDeclarator
            //
            case 91: {
                List l = new TypedList(new LinkedList(), X10VarDeclarator.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 92:  VariableDeclarators ::= VariableDeclarators , VariableDeclarator
            //
            case 92: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(3));
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 93:  VariableDeclarator ::= VariableDeclaratorId
            //
            case 93:
                break;
 
            //
            // Rule 94:  VariableDeclarator ::= VariableDeclaratorId = VariableInitializer
            //
            case 94: {
                X10VarDeclarator a = (X10VarDeclarator) btParser.getSym(1);
                Expr b = (Expr) btParser.getSym(3);
                a.init = b; 
                // btParser.setSym1(a); 
                break;
            }
     
            //
            // Rule 95:  VariableDeclaratorId ::= identifier
            //
            case 95: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new X10VarDeclarator(pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 96:  VariableDeclaratorId ::= VariableDeclaratorId [ ]
            //
            case 96: {
                X10VarDeclarator a = (X10VarDeclarator) btParser.getSym(1);
                a.dims++;
                // btParser.setSym1(a);
                break;
            }
     
            //
            // Rule 97:  VariableDeclaratorId ::= identifier [ IdentifierList ]
            //
            case 97: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                List paramList = (List) btParser.getSym(3);
                btParser.setSym1(new X10VarDeclarator(pos(), a.getIdentifier(), paramList));
                break;
            }
     
            //
            // Rule 98:  VariableDeclaratorId ::= [ IdentifierList ]
            //
            case 98: {
                String name = polyglot.ext.x10.visit.X10PrettyPrinterVisitor.getId();
                List paramList = (List) btParser.getSym(2);
                btParser.setSym1(new X10VarDeclarator(pos(), name, paramList));
                break;
            }
     
            //
            // Rule 99:  VariableInitializer ::= Expression
            //
            case 99:
                break;
 
            //
            // Rule 100:  VariableInitializer ::= ArrayInitializer
            //
            case 100:
                break;
 
            //
            // Rule 101:  FieldModifiers ::= FieldModifier
            //
            case 101:
                break;
 
            //
            // Rule 102:  FieldModifiers ::= FieldModifiers FieldModifier
            //
            case 102: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 103:  FieldModifier ::= public
            //
            case 103: {
                btParser.setSym1(Flags.PUBLIC);
                break;
            }
     
            //
            // Rule 104:  FieldModifier ::= protected
            //
            case 104: {
                btParser.setSym1(Flags.PROTECTED);
                break;
            }
     
            //
            // Rule 105:  FieldModifier ::= private
            //
            case 105: {
                btParser.setSym1(Flags.PRIVATE);
                break;
            }
     
            //
            // Rule 106:  FieldModifier ::= static
            //
            case 106: {
                btParser.setSym1(Flags.STATIC);
                break;
            }
     
            //
            // Rule 107:  FieldModifier ::= final
            //
            case 107: {
                btParser.setSym1(Flags.FINAL);
                break;
            }
     
            //
            // Rule 108:  FieldModifier ::= transient
            //
            case 108: {
                btParser.setSym1(Flags.TRANSIENT);
                break;
            }
     
            //
            // Rule 109:  FieldModifier ::= volatile
            //
            case 109: {
                btParser.setSym1(Flags.VOLATILE);
                break;
            }
     
            //
            // Rule 110:  MethodDeclaration ::= MethodHeader MethodBody
            //
            case 110: {
                MethodDecl a = (MethodDecl) btParser.getSym(1);
                List l = a.formals();
                List s = new TypedList(new LinkedList(), Stmt.class, false);
                Block b = (Block) btParser.getSym(2);
                
                for (Iterator i = l.iterator(); i.hasNext(); ) {
                   X10Formal d = (X10Formal) i.next();
                   if (d.hasExplodedVars())
                     s.addAll( d.explode());
                }
                if (! s.isEmpty()) {
                  s.addAll(b.statements());
                  b = b.statements(s);
                }
                Flags f = a.flags();
                if (f.contains(Flags.ATOMIC)) {
                   List ss = new TypedList(new LinkedList(), Stmt.class, false);
                   ss.add(nf.Atomic(pos(), nf.Here(pos()), b));
                   b = b.statements(ss);
                   a = a.flags(f.clear(Flags.ATOMIC));
                }
                btParser.setSym1(a.body(b));
                break;
            }
     
            //
            // Rule 111:  MethodHeader ::= MethodModifiersopt ResultType MethodDeclarator Throwsopt
            //
            case 111: {
                Flags a = (Flags) btParser.getSym(1);
//vj                    assert(btParser.getSym(2) == null);
                TypeNode b = (TypeNode) btParser.getSym(2);
                Object[] o = (Object []) btParser.getSym(3);
                    Name c = (Name) o[0];
                    List d = (List) o[1];
                    Integer e = (Integer) o[2];
                List f = (List) btParser.getSym(4);

                if (b.type() == ts.Void() && e.intValue() > 0)
                {
                    // TODO: error!!!
                    assert(false);
                }

                btParser.setSym1(nf.MethodDecl(pos(btParser.getFirstToken(2), btParser.getLastToken(3)),
                                       a,
                                       nf.array((TypeNode) b, pos(btParser.getFirstToken(2), btParser.getLastToken(2)), e.intValue()),
                                       c.toString(),
                                       d,
                                       f,
                                       null));
                break;
            }
     
            //
            // Rule 112:  ResultType ::= Type
            //
            case 112:
                break;
 
            //
            // Rule 113:  ResultType ::= void
            //
            case 113: {
                btParser.setSym1(nf.CanonicalTypeNode(pos(), ts.Void()));
                break;
            }
     
            //
            // Rule 114:  MethodDeclarator ::= identifier ( FormalParameterListopt )
            //
            case 114: {
                Object[] a = new Object[3];
                a[0] =  new Name(nf, ts, pos(), id(btParser.getToken(1)).getIdentifier());
                a[1] = btParser.getSym(3);
                a[2] = new Integer(0);
                btParser.setSym1(a);
                break;
            }
     
            //
            // Rule 115:  MethodDeclarator ::= MethodDeclarator [ ]
            //
            case 115: {
                Object[] a = (Object []) btParser.getSym(1);
                a[2] = new Integer(((Integer) a[2]).intValue() + 1);
                // btParser.setSym1(a);
                break;
            }
     
            //
            // Rule 116:  FormalParameterList ::= LastFormalParameter
            //
            case 116: {
                List l = new TypedList(new LinkedList(), Formal.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 117:  FormalParameterList ::= FormalParameters , LastFormalParameter
            //
            case 117: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(3));
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 118:  FormalParameters ::= FormalParameter
            //
            case 118: {
                List l = new TypedList(new LinkedList(), Formal.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 119:  FormalParameters ::= FormalParameters , FormalParameter
            //
            case 119: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(3));
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 120:  FormalParameter ::= VariableModifiersopt Type VariableDeclaratorId
            //
            case 120: {
                Flags f = (Flags) btParser.getSym(1);
                TypeNode a = (TypeNode) btParser.getSym(2);
                X10VarDeclarator b = (X10VarDeclarator) btParser.getSym(3);
                b.setFlag(f);
                btParser.setSym1(nf.Formal(pos(), nf.array(a, pos(btParser.getFirstToken(2), btParser.getLastToken(2)), b.dims), b));
                break;
            }
     
            //
            // Rule 122:  VariableModifiers ::= VariableModifiers VariableModifier
            //
            case 122: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 123:  VariableModifier ::= final
            //
            case 123: {
                btParser.setSym1(Flags.FINAL);
                break;
            }
     
            //
            // Rule 124:  LastFormalParameter ::= VariableModifiersopt Type ...opt VariableDeclaratorId
            //
            case 124: {
                Flags f = (Flags) btParser.getSym(1);
                TypeNode a = (TypeNode) btParser.getSym(2);
                assert(btParser.getSym(3) == null);
                X10VarDeclarator b = (X10VarDeclarator) btParser.getSym(4);
                b.setFlag(f);
                btParser.setSym1(nf.Formal(pos(), nf.array(a, pos(btParser.getFirstToken(2), btParser.getLastToken(2)), b.dims), b));
                break;
            }
     
            //
            // Rule 125:  MethodModifiers ::= MethodModifier
            //
            case 125:
                break;
 
            //
            // Rule 126:  MethodModifiers ::= MethodModifiers MethodModifier
            //
            case 126: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 127:  MethodModifier ::= public
            //
            case 127: {
                btParser.setSym1(Flags.PUBLIC);
                break;
            }
     
            //
            // Rule 128:  MethodModifier ::= protected
            //
            case 128: {
                btParser.setSym1(Flags.PROTECTED);
                break;
            }
     
            //
            // Rule 129:  MethodModifier ::= private
            //
            case 129: {
                btParser.setSym1(Flags.PRIVATE);
                break;
            }
     
            //
            // Rule 130:  MethodModifier ::= abstract
            //
            case 130: {
                btParser.setSym1(Flags.ABSTRACT);
                break;
            }
     
            //
            // Rule 131:  MethodModifier ::= static
            //
            case 131: {
                btParser.setSym1(Flags.STATIC);
                break;
            }
     
            //
            // Rule 132:  MethodModifier ::= final
            //
            case 132: {
                btParser.setSym1(Flags.FINAL);
                break;
            }
     
            //
            // Rule 133:  MethodModifier ::= synchronized
            //
            case 133: {
                btParser.setSym1(Flags.SYNCHRONIZED);
                break;
            }
     
            //
            // Rule 134:  MethodModifier ::= native
            //
            case 134: {
                btParser.setSym1(Flags.NATIVE);
                break;
            }
     
            //
            // Rule 135:  MethodModifier ::= strictfp
            //
            case 135: {
                btParser.setSym1(Flags.STRICTFP);
                break;
            }
     
            //
            // Rule 136:  Throws ::= throws ExceptionTypeList
            //
            case 136: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 137:  ExceptionTypeList ::= ExceptionType
            //
            case 137: {
                List l = new TypedList(new LinkedList(), TypeNode.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 138:  ExceptionTypeList ::= ExceptionTypeList , ExceptionType
            //
            case 138: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(3));
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 139:  ExceptionType ::= ClassType
            //
            case 139:
                break;
 
            //
            // Rule 140:  MethodBody ::= Block
            //
            case 140:
                break;
 
            //
            // Rule 141:  MethodBody ::= ;
            //
            case 141:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 142:  InstanceInitializer ::= Block
            //
            case 142:
                break;
 
            //
            // Rule 143:  StaticInitializer ::= static Block
            //
            case 143: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 144:  ConstructorDeclaration ::= ConstructorModifiersopt ConstructorDeclarator Throwsopt ConstructorBody
            //
            case 144: {
                Flags m = (Flags) btParser.getSym(1);
                Object[] o = (Object []) btParser.getSym(2);
                    Name a = (Name) o[1];
                    List b = (List) o[2];
                List c = (List) btParser.getSym(3);
                Block d = (Block) btParser.getSym(4);

                btParser.setSym1(nf.ConstructorDecl(pos(), m, a.toString(), b, c, d));
                break;
            }
     
            //
            // Rule 145:  ConstructorDeclarator ::= SimpleTypeName ( FormalParameterListopt )
            //
            case 145: {
//vj                    assert(btParser.getSym(1) == null);
                Object[] a = new Object[3];
//vj                    a[0] = btParser.getSym(1);
                a[1] = btParser.getSym(1);
                a[2] = btParser.getSym(3);
                btParser.setSym1(a);
                break;
            }
     
            //
            // Rule 146:  SimpleTypeName ::= identifier
            //
            case 146: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 147:  ConstructorModifiers ::= ConstructorModifier
            //
            case 147:
                break;
 
            //
            // Rule 148:  ConstructorModifiers ::= ConstructorModifiers ConstructorModifier
            //
            case 148: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 149:  ConstructorModifier ::= public
            //
            case 149: {
                btParser.setSym1(Flags.PUBLIC);
                break;
            }
     
            //
            // Rule 150:  ConstructorModifier ::= protected
            //
            case 150: {
                btParser.setSym1(Flags.PROTECTED);
                break;
            }
     
            //
            // Rule 151:  ConstructorModifier ::= private
            //
            case 151: {
                btParser.setSym1(Flags.PRIVATE);
                break;
            }
     
            //
            // Rule 152:  ConstructorBody ::= { ExplicitConstructorInvocationopt BlockStatementsopt }
            //
            case 152: {
                Stmt a = (Stmt) btParser.getSym(2);
                List l;
                if (a == null)
                    l = (List) btParser.getSym(3);
                else
                {
                    l = new TypedList(new LinkedList(), Stmt.class, false);
                     List l2 = (List) btParser.getSym(3);
                    l.add(a);
                    l.addAll(l2);
                }
                btParser.setSym1(nf.Block(pos(), l));
                break;
            }
     
            //
            // Rule 153:  ExplicitConstructorInvocation ::= this ( ArgumentListopt ) ;
            //
            case 153: {
//vj                    assert(btParser.getSym(1) == null);
                List b = (List) btParser.getSym(3);
                btParser.setSym1(nf.ThisCall(pos(), b));
                break;
            }
     
            //
            // Rule 154:  ExplicitConstructorInvocation ::= super ( ArgumentListopt ) ;
            //
            case 154: {
//vj                    assert(btParser.getSym(1) == null);
                List b = (List) btParser.getSym(3);
                btParser.setSym1(nf.SuperCall(pos(), b));
                break;
            }
     
            //
            // Rule 155:  ExplicitConstructorInvocation ::= Primary . this ( ArgumentListopt ) ;
            //
            case 155: {
                Expr a = (Expr) btParser.getSym(1);
//vj                    assert(btParser.getSym(2) == null);
                List b = (List) btParser.getSym(5);
                btParser.setSym1(nf.ThisCall(pos(), a, b));
                break;
            }
     
            //
            // Rule 156:  ExplicitConstructorInvocation ::= Primary . super ( ArgumentListopt ) ;
            //
            case 156: {
                Expr a = (Expr) btParser.getSym(1);
//vj                    assert(btParser.getSym(2) == null);
                List b = (List) btParser.getSym(5);
                btParser.setSym1(nf.SuperCall(pos(), a, b));
                break;
            }
     
            //
            // Rule 157:  EnumDeclaration ::= ClassModifiersopt enum identifier Interfacesopt EnumBody
            //
            case 157:
                bad_rule = 157;
                break;
 
            //
            // Rule 158:  EnumBody ::= { EnumConstantsopt ,opt EnumBodyDeclarationsopt }
            //
            case 158:
                bad_rule = 158;
                break;
 
            //
            // Rule 159:  EnumConstants ::= EnumConstant
            //
            case 159:
                bad_rule = 159;
                break;
 
            //
            // Rule 160:  EnumConstants ::= EnumConstants , EnumConstant
            //
            case 160:
                bad_rule = 160;
                break;
 
            //
            // Rule 161:  EnumConstant ::= identifier Argumentsopt ClassBodyopt
            //
            case 161:
                bad_rule = 161;
                break;
 
            //
            // Rule 162:  Arguments ::= ( ArgumentListopt )
            //
            case 162: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 163:  EnumBodyDeclarations ::= ; ClassBodyDeclarationsopt
            //
            case 163:
                bad_rule = 163;
                break;
 
            //
            // Rule 164:  InterfaceDeclaration ::= NormalInterfaceDeclaration
            //
            case 164:
                break;
 
            //
            // Rule 165:  NormalInterfaceDeclaration ::= InterfaceModifiersopt interface identifier ExtendsInterfacesopt InterfaceBody
            //
            case 165: {
                Flags a = (Flags) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
//vj                    assert(btParser.getSym(4) == null);
                List c = (List) btParser.getSym(4);
                ClassBody d = (ClassBody) btParser.getSym(5);
                btParser.setSym1(nf.ClassDecl(pos(), a.Interface(), b.getIdentifier(), null, c, d));
                break;
            }
     
            //
            // Rule 166:  InterfaceModifiers ::= InterfaceModifier
            //
            case 166:
                break;
 
            //
            // Rule 167:  InterfaceModifiers ::= InterfaceModifiers InterfaceModifier
            //
            case 167: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 168:  InterfaceModifier ::= public
            //
            case 168: {
                btParser.setSym1(Flags.PUBLIC);
                break;
            }
     
            //
            // Rule 169:  InterfaceModifier ::= protected
            //
            case 169: {
                btParser.setSym1(Flags.PROTECTED);
                break;
            }
     
            //
            // Rule 170:  InterfaceModifier ::= private
            //
            case 170: {
                btParser.setSym1(Flags.PRIVATE);
                break;
            }
     
            //
            // Rule 171:  InterfaceModifier ::= abstract
            //
            case 171: {
                btParser.setSym1(Flags.ABSTRACT);
                break;
            }
     
            //
            // Rule 172:  InterfaceModifier ::= static
            //
            case 172: {
                btParser.setSym1(Flags.STATIC);
                break;
            }
     
            //
            // Rule 173:  InterfaceModifier ::= strictfp
            //
            case 173: {
                btParser.setSym1(Flags.STRICTFP);
                break;
            }
     
            //
            // Rule 174:  ExtendsInterfaces ::= extends InterfaceType
            //
            case 174: {
                List l = new TypedList(new LinkedList(), TypeNode.class, false);
                l.add(btParser.getSym(2));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 175:  ExtendsInterfaces ::= ExtendsInterfaces , InterfaceType
            //
            case 175: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(3));
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 176:  InterfaceBody ::= { InterfaceMemberDeclarationsopt }
            //
            case 176: {
                List a = (List)btParser.getSym(2);
                btParser.setSym1(nf.ClassBody(pos(), a));
                break;
            }
     
            //
            // Rule 177:  InterfaceMemberDeclarations ::= InterfaceMemberDeclaration
            //
            case 177:
                break;
 
            //
            // Rule 178:  InterfaceMemberDeclarations ::= InterfaceMemberDeclarations InterfaceMemberDeclaration
            //
            case 178: {
                List l = (List) btParser.getSym(1),
                     l2 = (List) btParser.getSym(2);
                l.addAll(l2);
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 179:  InterfaceMemberDeclaration ::= ConstantDeclaration
            //
            case 179:
                break;
 
            //
            // Rule 180:  InterfaceMemberDeclaration ::= AbstractMethodDeclaration
            //
            case 180: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 181:  InterfaceMemberDeclaration ::= ClassDeclaration
            //
            case 181: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 182:  InterfaceMemberDeclaration ::= InterfaceDeclaration
            //
            case 182: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 183:  InterfaceMemberDeclaration ::= ;
            //
            case 183: {
                btParser.setSym1(Collections.EMPTY_LIST);
                break;
            }
     
            //
            // Rule 184:  ConstantDeclaration ::= ConstantModifiersopt Type VariableDeclarators
            //
            case 184: {
                List l = new TypedList(new LinkedList(), ClassMember.class, false);
                Flags a = (Flags) btParser.getSym(1);
                TypeNode b = (TypeNode) btParser.getSym(2);
                List c = (List) btParser.getSym(3);
                for (Iterator i = c.iterator(); i.hasNext();)
                {
                    X10VarDeclarator d = (X10VarDeclarator) i.next();
                    if (d.hasExplodedVars())
                      // TODO: Report this exception correctly.
                      throw new Error("Field Declarations may not have exploded variables." + pos());
                    l.add(nf.FieldDecl(pos(btParser.getFirstToken(2), btParser.getLastToken()),
                                       a,
                                       nf.array(b, pos(btParser.getFirstToken(2), btParser.getLastToken(2)), d.dims),
                                       d.name,
                                       d.init));
                }
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 185:  ConstantModifiers ::= ConstantModifier
            //
            case 185:
                break;
 
            //
            // Rule 186:  ConstantModifiers ::= ConstantModifiers ConstantModifier
            //
            case 186: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 187:  ConstantModifier ::= public
            //
            case 187: {
                btParser.setSym1(Flags.PUBLIC);
                break;
            }
     
            //
            // Rule 188:  ConstantModifier ::= static
            //
            case 188: {
                btParser.setSym1(Flags.STATIC);
                break;
            }
     
            //
            // Rule 189:  ConstantModifier ::= final
            //
            case 189: {
                btParser.setSym1(Flags.FINAL);
                break;
            }
     
            //
            // Rule 190:  AbstractMethodDeclaration ::= AbstractMethodModifiersopt ResultType MethodDeclarator Throwsopt ;
            //
            case 190: {
                Flags a = (Flags) btParser.getSym(1);
//vj                    assert(btParser.getSym(2) == null);
                TypeNode b = (TypeNode) btParser.getSym(2);
                Object[] o = (Object []) btParser.getSym(3);
                    Name c = (Name) o[0];
                    List d = (List) o[1];
                    Integer e = (Integer) o[2];
                List f = (List) btParser.getSym(4);

                if (b.type() == ts.Void() && e.intValue() > 0)
                {
                    // TODO: error!!!
                    assert(false);
                }

                btParser.setSym1(nf.MethodDecl(pos(btParser.getFirstToken(2), btParser.getLastToken(3)),
                                       a,
                                       nf.array((TypeNode) b, pos(btParser.getFirstToken(2), btParser.getLastToken(2)), e.intValue()),
                                       c.toString(),
                                       d,
                                       f,
                                       null));
                break;
            }
     
            //
            // Rule 191:  AbstractMethodModifiers ::= AbstractMethodModifier
            //
            case 191:
                break;
 
            //
            // Rule 192:  AbstractMethodModifiers ::= AbstractMethodModifiers AbstractMethodModifier
            //
            case 192: {
                Flags a = (Flags) btParser.getSym(1),
                      b = (Flags) btParser.getSym(2);
                btParser.setSym1(a.set(b));
                break;
            }
     
            //
            // Rule 193:  AbstractMethodModifier ::= public
            //
            case 193: {
                btParser.setSym1(Flags.PUBLIC);
                break;
            }
     
            //
            // Rule 194:  AbstractMethodModifier ::= abstract
            //
            case 194: {
                btParser.setSym1(Flags.ABSTRACT);
                break;
            }
     
            //
            // Rule 195:  AnnotationTypeDeclaration ::= InterfaceModifiersopt @ interface identifier AnnotationTypeBody
            //
            case 195:
                bad_rule = 195;
                break;
 
            //
            // Rule 196:  AnnotationTypeBody ::= { AnnotationTypeElementDeclarationsopt }
            //
            case 196:
                bad_rule = 196;
                break;
 
            //
            // Rule 197:  AnnotationTypeElementDeclarations ::= AnnotationTypeElementDeclaration
            //
            case 197:
                bad_rule = 197;
                break;
 
            //
            // Rule 198:  AnnotationTypeElementDeclarations ::= AnnotationTypeElementDeclarations AnnotationTypeElementDeclaration
            //
            case 198:
                bad_rule = 198;
                break;
 
            //
            // Rule 199:  AnnotationTypeElementDeclaration ::= AbstractMethodModifiersopt Type identifier ( ) DefaultValueopt ;
            //
            case 199:
                bad_rule = 199;
                break;
 
            //
            // Rule 200:  AnnotationTypeElementDeclaration ::= ConstantDeclaration
            //
            case 200:
                bad_rule = 200;
                break;
 
            //
            // Rule 201:  AnnotationTypeElementDeclaration ::= ClassDeclaration
            //
            case 201:
                bad_rule = 201;
                break;
 
            //
            // Rule 202:  AnnotationTypeElementDeclaration ::= InterfaceDeclaration
            //
            case 202:
                bad_rule = 202;
                break;
 
            //
            // Rule 203:  AnnotationTypeElementDeclaration ::= EnumDeclaration
            //
            case 203:
                bad_rule = 203;
                break;
 
            //
            // Rule 204:  AnnotationTypeElementDeclaration ::= AnnotationTypeDeclaration
            //
            case 204:
                bad_rule = 204;
                break;
 
            //
            // Rule 205:  AnnotationTypeElementDeclaration ::= ;
            //
            case 205:
                bad_rule = 205;
                break;
 
            //
            // Rule 206:  DefaultValue ::= default ElementValue
            //
            case 206:
                bad_rule = 206;
                break;
 
            //
            // Rule 207:  Annotations ::= Annotation
            //
            case 207:
                bad_rule = 207;
                break;
 
            //
            // Rule 208:  Annotations ::= Annotations Annotation
            //
            case 208:
                bad_rule = 208;
                break;
 
            //
            // Rule 209:  Annotation ::= NormalAnnotation
            //
            case 209:
                bad_rule = 209;
                break;
 
            //
            // Rule 210:  Annotation ::= MarkerAnnotation
            //
            case 210:
                bad_rule = 210;
                break;
 
            //
            // Rule 211:  Annotation ::= SingleElementAnnotation
            //
            case 211:
                bad_rule = 211;
                break;
 
            //
            // Rule 212:  NormalAnnotation ::= @ TypeName ( ElementValuePairsopt )
            //
            case 212:
                bad_rule = 212;
                break;
 
            //
            // Rule 213:  ElementValuePairs ::= ElementValuePair
            //
            case 213:
                bad_rule = 213;
                break;
 
            //
            // Rule 214:  ElementValuePairs ::= ElementValuePairs , ElementValuePair
            //
            case 214:
                bad_rule = 214;
                break;
 
            //
            // Rule 215:  ElementValuePair ::= SimpleName = ElementValue
            //
            case 215:
                bad_rule = 215;
                break;
 
            //
            // Rule 216:  SimpleName ::= identifier
            //
            case 216: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 217:  ElementValue ::= ConditionalExpression
            //
            case 217:
                bad_rule = 217;
                break;
 
            //
            // Rule 218:  ElementValue ::= Annotation
            //
            case 218:
                bad_rule = 218;
                break;
 
            //
            // Rule 219:  ElementValue ::= ElementValueArrayInitializer
            //
            case 219:
                bad_rule = 219;
                break;
 
            //
            // Rule 220:  ElementValueArrayInitializer ::= { ElementValuesopt ,opt }
            //
            case 220:
                bad_rule = 220;
                break;
 
            //
            // Rule 221:  ElementValues ::= ElementValue
            //
            case 221:
                bad_rule = 221;
                break;
 
            //
            // Rule 222:  ElementValues ::= ElementValues , ElementValue
            //
            case 222:
                bad_rule = 222;
                break;
 
            //
            // Rule 223:  MarkerAnnotation ::= @ TypeName
            //
            case 223:
                bad_rule = 223;
                break;
 
            //
            // Rule 224:  SingleElementAnnotation ::= @ TypeName ( ElementValue )
            //
            case 224:
                bad_rule = 224;
                break;
 
            //
            // Rule 225:  ArrayInitializer ::= { VariableInitializersopt ,opt }
            //
            case 225: {
                List a = (List) btParser.getSym(2);
                if (a == null)
                     btParser.setSym1(nf.ArrayInit(pos()));
                else btParser.setSym1(nf.ArrayInit(pos(), a));
                break;
            }
     
            //
            // Rule 226:  VariableInitializers ::= VariableInitializer
            //
            case 226: {
                List l = new TypedList(new LinkedList(), Expr.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 227:  VariableInitializers ::= VariableInitializers , VariableInitializer
            //
            case 227: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(3));
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 228:  Block ::= { BlockStatementsopt }
            //
            case 228: {
                List l = (List) btParser.getSym(2);
                btParser.setSym1(nf.Block(pos(), l));
                break;
            }
     
            //
            // Rule 229:  BlockStatements ::= BlockStatement
            //
            case 229: {
                List l = new TypedList(new LinkedList(), Stmt.class, false),
                     l2 = (List) btParser.getSym(1);
                l.addAll(l2);
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 230:  BlockStatements ::= BlockStatements BlockStatement
            //
            case 230: {
                List l = (List) btParser.getSym(1),
                     l2 = (List) btParser.getSym(2);
                l.addAll(l2);
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 231:  BlockStatement ::= LocalVariableDeclarationStatement
            //
            case 231:
                break;
 
            //
            // Rule 232:  BlockStatement ::= ClassDeclaration
            //
            case 232: {
                ClassDecl a = (ClassDecl) btParser.getSym(1);
                List l = new TypedList(new LinkedList(), Stmt.class, false);
                l.add(nf.LocalClassDecl(pos(), a));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 233:  BlockStatement ::= Statement
            //
            case 233: {
                List l = new TypedList(new LinkedList(), Stmt.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 234:  LocalVariableDeclarationStatement ::= LocalVariableDeclaration ;
            //
            case 234:
                break;
 
            //
            // Rule 235:  LocalVariableDeclaration ::= VariableModifiersopt Type VariableDeclarators
            //
            case 235: {
                Flags flags = (Flags) btParser.getSym(1);
                TypeNode a = (TypeNode) btParser.getSym(2);
                List b = (List) btParser.getSym(3);

                List l = new TypedList(new LinkedList(), LocalDecl.class, false);
                List s = new TypedList(new LinkedList(), Stmt.class, false);
                for (Iterator i = b.iterator(); i.hasNext(); )
                {
                    X10VarDeclarator d = (X10VarDeclarator) i.next();
                    d.setFlag( flags ); 
                    // use d.flags below and not flags, setFlag may change it.
                    l.add(nf.LocalDecl(d.pos,  d.flags,
                                       nf.array(a, pos(d), d.dims), d.name, d.init));
                    if (d.hasExplodedVars())
                       s.addAll( d.explode() );
                }
                l.addAll(s); 
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 236:  Statement ::= StatementWithoutTrailingSubstatement
            //
            case 236:
                break;
 
            //
            // Rule 237:  Statement ::= LabeledStatement
            //
            case 237:
                break;
 
            //
            // Rule 238:  Statement ::= IfThenStatement
            //
            case 238:
                break;
 
            //
            // Rule 239:  Statement ::= IfThenElseStatement
            //
            case 239:
                break;
 
            //
            // Rule 240:  Statement ::= WhileStatement
            //
            case 240:
                break;
 
            //
            // Rule 241:  Statement ::= ForStatement
            //
            case 241:
                break;
 
            //
            // Rule 242:  StatementWithoutTrailingSubstatement ::= Block
            //
            case 242:
                break;
 
            //
            // Rule 243:  StatementWithoutTrailingSubstatement ::= EmptyStatement
            //
            case 243:
                break;
 
            //
            // Rule 244:  StatementWithoutTrailingSubstatement ::= ExpressionStatement
            //
            case 244:
                break;
 
            //
            // Rule 245:  StatementWithoutTrailingSubstatement ::= AssertStatement
            //
            case 245:
                break;
 
            //
            // Rule 246:  StatementWithoutTrailingSubstatement ::= SwitchStatement
            //
            case 246:
                break;
 
            //
            // Rule 247:  StatementWithoutTrailingSubstatement ::= DoStatement
            //
            case 247:
                break;
 
            //
            // Rule 248:  StatementWithoutTrailingSubstatement ::= BreakStatement
            //
            case 248:
                break;
 
            //
            // Rule 249:  StatementWithoutTrailingSubstatement ::= ContinueStatement
            //
            case 249:
                break;
 
            //
            // Rule 250:  StatementWithoutTrailingSubstatement ::= ReturnStatement
            //
            case 250:
                break;
 
            //
            // Rule 251:  StatementWithoutTrailingSubstatement ::= SynchronizedStatement
            //
            case 251:
                break;
 
            //
            // Rule 252:  StatementWithoutTrailingSubstatement ::= ThrowStatement
            //
            case 252:
                break;
 
            //
            // Rule 253:  StatementWithoutTrailingSubstatement ::= TryStatement
            //
            case 253:
                break;
 
            //
            // Rule 254:  StatementNoShortIf ::= StatementWithoutTrailingSubstatement
            //
            case 254:
                break;
 
            //
            // Rule 255:  StatementNoShortIf ::= LabeledStatementNoShortIf
            //
            case 255:
                break;
 
            //
            // Rule 256:  StatementNoShortIf ::= IfThenElseStatementNoShortIf
            //
            case 256:
                break;
 
            //
            // Rule 257:  StatementNoShortIf ::= WhileStatementNoShortIf
            //
            case 257:
                break;
 
            //
            // Rule 258:  StatementNoShortIf ::= ForStatementNoShortIf
            //
            case 258:
                break;
 
            //
            // Rule 259:  IfThenStatement ::= if ( Expression ) Statement
            //
            case 259: {
                Expr a = (Expr) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.If(pos(), a, b));
                break;
            }
     
            //
            // Rule 260:  IfThenElseStatement ::= if ( Expression ) StatementNoShortIf else Statement
            //
            case 260: {
                Expr a = (Expr) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                Stmt c = (Stmt) btParser.getSym(7);
                btParser.setSym1(nf.If(pos(), a, b, c));
                break;
            }
     
            //
            // Rule 261:  IfThenElseStatementNoShortIf ::= if ( Expression ) StatementNoShortIf else StatementNoShortIf
            //
            case 261: {
                Expr a = (Expr) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                Stmt c = (Stmt) btParser.getSym(7);
                btParser.setSym1(nf.If(pos(), a, b, c));
                break;
            }
     
            //
            // Rule 262:  EmptyStatement ::= ;
            //
            case 262: {
                btParser.setSym1(nf.Empty(pos()));
                break;
            }
     
            //
            // Rule 263:  LabeledStatement ::= identifier : Statement
            //
            case 263: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                Stmt b = (Stmt) btParser.getSym(3);
                btParser.setSym1(nf.Labeled(pos(), a.getIdentifier(), b));
                break;
            }
     
            //
            // Rule 264:  LabeledStatementNoShortIf ::= identifier : StatementNoShortIf
            //
            case 264: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                Stmt b = (Stmt) btParser.getSym(3);
                btParser.setSym1(nf.Labeled(pos(), a.getIdentifier(), b));
                break;
            }
     
            //
            // Rule 265:  ExpressionStatement ::= StatementExpression ;
            //
            case 265: {
                Expr a = (Expr) btParser.getSym(1);
                btParser.setSym1(nf.Eval(pos(), a));
                break;
            }
     
            //
            // Rule 266:  StatementExpression ::= Assignment
            //
            case 266:
                break;
 
            //
            // Rule 267:  StatementExpression ::= PreIncrementExpression
            //
            case 267:
                break;
 
            //
            // Rule 268:  StatementExpression ::= PreDecrementExpression
            //
            case 268:
                break;
 
            //
            // Rule 269:  StatementExpression ::= PostIncrementExpression
            //
            case 269:
                break;
 
            //
            // Rule 270:  StatementExpression ::= PostDecrementExpression
            //
            case 270:
                break;
 
            //
            // Rule 271:  StatementExpression ::= MethodInvocation
            //
            case 271:
                break;
 
            //
            // Rule 272:  StatementExpression ::= ClassInstanceCreationExpression
            //
            case 272:
                break;
 
            //
            // Rule 273:  AssertStatement ::= assert Expression ;
            //
            case 273: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Assert(pos(), a));
                break;
            }
     
            //
            // Rule 274:  AssertStatement ::= assert Expression : Expression ;
            //
            case 274: {
                Expr a = (Expr) btParser.getSym(2),
                     b = (Expr) btParser.getSym(4);
                btParser.setSym1(nf.Assert(pos(), a, b));
                break;
            }
     
            //
            // Rule 275:  SwitchStatement ::= switch ( Expression ) SwitchBlock
            //
            case 275: {
                Expr a = (Expr) btParser.getSym(3);
                List b = (List) btParser.getSym(5);
                btParser.setSym1(nf.Switch(pos(), a, b));
                break;
            }
     
            //
            // Rule 276:  SwitchBlock ::= { SwitchBlockStatementGroupsopt SwitchLabelsopt }
            //
            case 276: {
                List l = (List) btParser.getSym(2),
                     l2 = (List) btParser.getSym(3);
                l.addAll(l2);
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 277:  SwitchBlockStatementGroups ::= SwitchBlockStatementGroup
            //
            case 277:
                break;
 
            //
            // Rule 278:  SwitchBlockStatementGroups ::= SwitchBlockStatementGroups SwitchBlockStatementGroup
            //
            case 278: {
                List l = (List) btParser.getSym(1),
                     l2 = (List) btParser.getSym(2);
                l.addAll(l2);
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 279:  SwitchBlockStatementGroup ::= SwitchLabels BlockStatements
            //
            case 279: {
                List l = new TypedList(new LinkedList(), SwitchElement.class, false);

                List l1 = (List) btParser.getSym(1),
                     l2 = (List) btParser.getSym(2);
                l.addAll(l1);
                l.add(nf.SwitchBlock(pos(), l2));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 280:  SwitchLabels ::= SwitchLabel
            //
            case 280: {
                List l = new TypedList(new LinkedList(), Case.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 281:  SwitchLabels ::= SwitchLabels SwitchLabel
            //
            case 281: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(2));
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 282:  SwitchLabel ::= case ConstantExpression :
            //
            case 282: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Case(pos(), a));
                break;
            }
     
            //
            // Rule 283:  SwitchLabel ::= case EnumConstant :
            //
            case 283:
                bad_rule = 283;
                break;
 
            //
            // Rule 284:  SwitchLabel ::= default :
            //
            case 284: {
                btParser.setSym1(nf.Default(pos()));
                break;
            }
     
            //
            // Rule 285:  EnumConstant ::= identifier
            //
            case 285:
                bad_rule = 285;
                break;
 
            //
            // Rule 286:  WhileStatement ::= while ( Expression ) Statement
            //
            case 286: {
                Expr a = (Expr) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.While(pos(), a, b));
                break;
            }
     
            //
            // Rule 287:  WhileStatementNoShortIf ::= while ( Expression ) StatementNoShortIf
            //
            case 287: {
                Expr a = (Expr) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.While(pos(), a, b));
                break;
            }
     
            //
            // Rule 288:  DoStatement ::= do Statement while ( Expression ) ;
            //
            case 288: {
                Stmt a = (Stmt) btParser.getSym(2);
                Expr b = (Expr) btParser.getSym(5);
                btParser.setSym1(nf.Do(pos(), a, b));
                break;
            }
     
            //
            // Rule 289:  ForStatement ::= BasicForStatement
            //
            case 289:
                break;
 
            //
            // Rule 290:  ForStatement ::= EnhancedForStatement
            //
            case 290:
                break;
 
            //
            // Rule 291:  BasicForStatement ::= for ( ForInitopt ; Expressionopt ; ForUpdateopt ) Statement
            //
            case 291: {
                List a = (List) btParser.getSym(3);
                Expr b = (Expr) btParser.getSym(5);
                List c = (List) btParser.getSym(7);
                Stmt d = (Stmt) btParser.getSym(9);
                btParser.setSym1(nf.For(pos(), a, b, c, d));
                break;
            }
     
            //
            // Rule 292:  ForStatementNoShortIf ::= for ( ForInitopt ; Expressionopt ; ForUpdateopt ) StatementNoShortIf
            //
            case 292: {
                List a = (List) btParser.getSym(3);
                Expr b = (Expr) btParser.getSym(5);
                List c = (List) btParser.getSym(7);
                Stmt d = (Stmt) btParser.getSym(9);
                btParser.setSym1(nf.For(pos(), a, b, c, d));
                break;
            }
     
            //
            // Rule 293:  ForInit ::= StatementExpressionList
            //
            case 293:
                break;
 
            //
            // Rule 294:  ForInit ::= LocalVariableDeclaration
            //
            case 294: {
                List l = new TypedList(new LinkedList(), ForInit.class, false),
                     l2 = (List) btParser.getSym(1);
                l.addAll(l2);
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 295:  ForUpdate ::= StatementExpressionList
            //
            case 295:
                break;
 
            //
            // Rule 296:  StatementExpressionList ::= StatementExpression
            //
            case 296: {
                List l = new TypedList(new LinkedList(), Eval.class, false);
                Expr a = (Expr) btParser.getSym(1);
                l.add(nf.Eval(pos(), a));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 297:  StatementExpressionList ::= StatementExpressionList , StatementExpression
            //
            case 297: {
                List l = (List) btParser.getSym(1);
                Expr a = (Expr) btParser.getSym(3);
                l.add(nf.Eval(pos(), a));
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 298:  BreakStatement ::= break identifieropt ;
            //
            case 298: {
                Name a = (Name) btParser.getSym(2);
                if (a == null)
                     btParser.setSym1(nf.Break(pos()));
                else btParser.setSym1(nf.Break(pos(), a.toString()));
                break;
            }
     
            //
            // Rule 299:  ContinueStatement ::= continue identifieropt ;
            //
            case 299: {
                Name a = (Name) btParser.getSym(2);
                if (a == null)
                     btParser.setSym1(nf.Continue(pos()));
                else btParser.setSym1(nf.Continue(pos(), a.toString()));
                break;
            }
     
            //
            // Rule 300:  ReturnStatement ::= return Expressionopt ;
            //
            case 300: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Return(pos(), a));
                break;
            }
     
            //
            // Rule 301:  ThrowStatement ::= throw Expression ;
            //
            case 301: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Throw(pos(), a));
                break;
            }
     
            //
            // Rule 302:  SynchronizedStatement ::= synchronized ( Expression ) Block
            //
            case 302: {
                Expr a = (Expr) btParser.getSym(3);
                Block b = (Block) btParser.getSym(5);
                btParser.setSym1(nf.Synchronized(pos(), a, b));
                break;
            }
     
            //
            // Rule 303:  TryStatement ::= try Block Catches
            //
            case 303: {
                Block a = (Block) btParser.getSym(2);
                List b = (List) btParser.getSym(3);
                btParser.setSym1(nf.Try(pos(), a, b));
                break;
            }
     
            //
            // Rule 304:  TryStatement ::= try Block Catchesopt Finally
            //
            case 304: {
                Block a = (Block) btParser.getSym(2);
                List b = (List) btParser.getSym(3);
                Block c = (Block) btParser.getSym(4);
                btParser.setSym1(nf.Try(pos(), a, b, c));
                break;
            }
     
            //
            // Rule 305:  Catches ::= CatchClause
            //
            case 305: {
                List l = new TypedList(new LinkedList(), Catch.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 306:  Catches ::= Catches CatchClause
            //
            case 306: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(2));
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 307:  CatchClause ::= catch ( FormalParameter ) Block
            //
            case 307: {
                Formal a = (Formal) btParser.getSym(3);
                Block b = (Block) btParser.getSym(5);
                btParser.setSym1(nf.Catch(pos(), a, b));
                break;
            }
     
            //
            // Rule 308:  Finally ::= finally Block
            //
            case 308: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 309:  Primary ::= PrimaryNoNewArray
            //
            case 309:
                break;
 
            //
            // Rule 310:  Primary ::= ArrayCreationExpression
            //
            case 310:
                break;
 
            //
            // Rule 311:  PrimaryNoNewArray ::= Literal
            //
            case 311:
                break;
 
            //
            // Rule 312:  PrimaryNoNewArray ::= Type . class
            //
            case 312: {
                Object o = btParser.getSym(1);
                if (o instanceof Name)
                {
                    Name a = (Name) o;
                    btParser.setSym1(nf.ClassLit(pos(), a.toType()));
                }
                else if (o instanceof TypeNode)
                {
                    TypeNode a = (TypeNode) o;
                    btParser.setSym1(nf.ClassLit(pos(), a));
                }
                else if (o instanceof CanonicalTypeNode)
                {
                    CanonicalTypeNode a = (CanonicalTypeNode) o;
                    btParser.setSym1(nf.ClassLit(pos(), a));
                }
                else assert(false);
                break;
            }
     
            //
            // Rule 313:  PrimaryNoNewArray ::= void . class
            //
            case 313: {
                btParser.setSym1(nf.ClassLit(pos(),
                                     nf.CanonicalTypeNode(pos(btParser.getToken(1)), ts.Void())));
                break;
            }
     
            //
            // Rule 314:  PrimaryNoNewArray ::= this
            //
            case 314: {
                btParser.setSym1(nf.This(pos()));
                break;
            }
     
            //
            // Rule 315:  PrimaryNoNewArray ::= ClassName . this
            //
            case 315: {
                Name a = (Name) btParser.getSym(1);
                btParser.setSym1(nf.This(pos(), a.toType()));
                break;
            }
     
            //
            // Rule 316:  PrimaryNoNewArray ::= ( Expression )
            //
            case 316: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 317:  PrimaryNoNewArray ::= ClassInstanceCreationExpression
            //
            case 317:
                break;
 
            //
            // Rule 318:  PrimaryNoNewArray ::= FieldAccess
            //
            case 318:
                break;
 
            //
            // Rule 319:  PrimaryNoNewArray ::= MethodInvocation
            //
            case 319:
                break;
 
            //
            // Rule 320:  PrimaryNoNewArray ::= ArrayAccess
            //
            case 320:
                break;
 
            //
            // Rule 321:  Literal ::= IntegerLiteral
            //
            case 321: {
                polyglot.lex.IntegerLiteral a = int_lit(btParser.getToken(1));
                btParser.setSym1(nf.IntLit(pos(), IntLit.INT, a.getValue().intValue()));
                break;
            }
     
            //
            // Rule 322:  Literal ::= LongLiteral
            //
            case 322: {
                polyglot.lex.LongLiteral a = long_lit(btParser.getToken(1));
                btParser.setSym1(nf.IntLit(pos(), IntLit.LONG, a.getValue().longValue()));
                break;
            }
     
            //
            // Rule 323:  Literal ::= FloatingPointLiteral
            //
            case 323: {
                polyglot.lex.FloatLiteral a = float_lit(btParser.getToken(1));
                btParser.setSym1(nf.FloatLit(pos(), FloatLit.FLOAT, a.getValue().floatValue()));
                break;
            }
     
            //
            // Rule 324:  Literal ::= DoubleLiteral
            //
            case 324: {
                polyglot.lex.DoubleLiteral a = double_lit(btParser.getToken(1));
                btParser.setSym1(nf.FloatLit(pos(), FloatLit.DOUBLE, a.getValue().doubleValue()));
                break;
            }
     
            //
            // Rule 325:  Literal ::= BooleanLiteral
            //
            case 325: {
                polyglot.lex.BooleanLiteral a = boolean_lit(btParser.getToken(1));
                btParser.setSym1(nf.BooleanLit(pos(), a.getValue().booleanValue()));
                break;
            }
     
            //
            // Rule 326:  Literal ::= CharacterLiteral
            //
            case 326: {
                polyglot.lex.CharacterLiteral a = char_lit(btParser.getToken(1));
                btParser.setSym1(nf.CharLit(pos(), a.getValue().charValue()));
                break;
            }
     
            //
            // Rule 327:  Literal ::= StringLiteral
            //
            case 327: {
                polyglot.lex.StringLiteral a = string_lit(btParser.getToken(1));
                btParser.setSym1(nf.StringLit(pos(), a.getValue()));
                break;
            }
     
            //
            // Rule 328:  Literal ::= null
            //
            case 328: {
                btParser.setSym1(nf.NullLit(pos()));
                break;
            }
     
            //
            // Rule 329:  BooleanLiteral ::= true
            //
            case 329:
                break;
 
            //
            // Rule 330:  BooleanLiteral ::= false
            //
            case 330:
                break;
 
            //
            // Rule 331:  ClassInstanceCreationExpression ::= new ClassOrInterfaceType ( ArgumentListopt ) ClassBodyopt
            //
            case 331: {
//vj                    assert(btParser.getSym(2) == null);
                TypeNode a = (TypeNode) btParser.getSym(2);
//vj                    assert(btParser.getSym(4) == null);
                List b = (List) btParser.getSym(4);
                ClassBody c = (ClassBody) btParser.getSym(6);
                if (c == null)
                     btParser.setSym1(nf.New(pos(), a, b));
                else btParser.setSym1(nf.New(pos(), a, b, c));
                break;
            }
     
            //
            // Rule 332:  ClassInstanceCreationExpression ::= Primary . new identifier ( ArgumentListopt ) ClassBodyopt
            //
            case 332: {
                Expr a = (Expr) btParser.getSym(1);
//vj                    assert(btParser.getSym(2) == null);
                Name b = new Name(nf, ts, pos(), id(btParser.getToken(4)).getIdentifier());
//vj                    assert(btParser.getSym(4) == null);
                List c = (List) btParser.getSym(6);
                ClassBody d = (ClassBody) btParser.getSym(8);
                if (d == null)
                     btParser.setSym1(nf.New(pos(), a, b.toType(), c));
                else btParser.setSym1(nf.New(pos(), a, b.toType(), c, d));
                break;
            }
     
            //
            // Rule 333:  ClassInstanceCreationExpression ::= AmbiguousName . new identifier ( ArgumentListopt ) ClassBodyopt
            //
            case 333: {
                Name a = (Name) btParser.getSym(1);
//vj                    assert(btParser.getSym(4) == null);
                Name b = new Name(nf, ts, pos(), id(btParser.getToken(4)).getIdentifier());
//vj                    assert(btParser.getSym(6) == null);
                List c = (List) btParser.getSym(6);
                ClassBody d = (ClassBody) btParser.getSym(8);
                if (d == null)
                     btParser.setSym1(nf.New(pos(), a.toExpr(), b.toType(), c));
                else btParser.setSym1(nf.New(pos(), a.toExpr(), b.toType(), c, d));
                break;
            }
     
            //
            // Rule 334:  ArgumentList ::= Expression
            //
            case 334: {
                List l = new TypedList(new LinkedList(), Expr.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 335:  ArgumentList ::= ArgumentList , Expression
            //
            case 335: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(3));
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 336:  DimExprs ::= DimExpr
            //
            case 336: {
                List l = new TypedList(new LinkedList(), Expr.class, false);
                l.add(btParser.getSym(1));
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 337:  DimExprs ::= DimExprs DimExpr
            //
            case 337: {
                List l = (List) btParser.getSym(1);
                l.add(btParser.getSym(2));
                //btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 338:  DimExpr ::= [ Expression ]
            //
            case 338: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(a.position(pos()));
                break;
            }
     
            //
            // Rule 339:  Dims ::= [ ]
            //
            case 339: {
                btParser.setSym1(new Integer(1));
                break;
            }
     
            //
            // Rule 340:  Dims ::= Dims [ ]
            //
            case 340: {
                Integer a = (Integer) btParser.getSym(1);
                btParser.setSym1(new Integer(a.intValue() + 1));
                break;
            }
     
            //
            // Rule 341:  FieldAccess ::= Primary . identifier
            //
            case 341: {
                Expr a = (Expr) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(nf.Field(pos(), a, b.getIdentifier()));
                break;
            }
     
            //
            // Rule 342:  FieldAccess ::= super . identifier
            //
            case 342: {
                polyglot.lex.Identifier a = id(btParser.getToken(3));
                btParser.setSym1(nf.Field(pos(btParser.getLastToken()), nf.Super(pos(btParser.getFirstToken())), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 343:  FieldAccess ::= ClassName . super . identifier
            //
            case 343: {
                Name a = (Name) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                btParser.setSym1(nf.Field(pos(btParser.getLastToken()), nf.Super(pos(btParser.getFirstToken(3)), a.toType()), b.getIdentifier()));
                break;
            }
     
            //
            // Rule 344:  MethodInvocation ::= MethodName ( ArgumentListopt )
            //
            case 344: {
                Name a = (Name) btParser.getSym(1);
                List b = (List) btParser.getSym(3);
                btParser.setSym1(nf.Call(pos(), a.prefix == null ? null : a.prefix.toReceiver(), a.name, b));
                break;
            }
     
            //
            // Rule 345:  MethodInvocation ::= Primary . identifier ( ArgumentListopt )
            //
            case 345: {
                Expr a = (Expr) btParser.getSym(1);
//vj                    assert(btParser.getSym(3) == null);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                List c = (List) btParser.getSym(5);
                btParser.setSym1(nf.Call(pos(), a, b.getIdentifier(), c));
                break;
            }
     
            //
            // Rule 346:  MethodInvocation ::= super . identifier ( ArgumentListopt )
            //
            case 346: {
//vj                    assert(btParser.getSym(3) == null);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                List c = (List) btParser.getSym(5);
                btParser.setSym1(nf.Call(pos(), nf.Super(pos(btParser.getFirstToken())), b.getIdentifier(), c));
                break;
            }
     
            //
            // Rule 347:  MethodInvocation ::= ClassName . super . identifier ( ArgumentListopt )
            //
            case 347: {
                Name a = (Name) btParser.getSym(1);
//vj                    assert(btParser.getSym(5) == null);
                polyglot.lex.Identifier b = id(btParser.getToken(5));
                List c = (List) btParser.getSym(7);
                btParser.setSym1(nf.Call(pos(), nf.Super(pos(btParser.getFirstToken(3)), a.toType()), b.getIdentifier(), c));
                break;
            }
     
            //
            // Rule 348:  PostfixExpression ::= Primary
            //
            case 348:
                break;
 
            //
            // Rule 349:  PostfixExpression ::= ExpressionName
            //
            case 349: {
                Name a = (Name) btParser.getSym(1);
                btParser.setSym1(a.toExpr());
                break;
            }
     
            //
            // Rule 350:  PostfixExpression ::= PostIncrementExpression
            //
            case 350:
                break;
 
            //
            // Rule 351:  PostfixExpression ::= PostDecrementExpression
            //
            case 351:
                break;
 
            //
            // Rule 352:  PostIncrementExpression ::= PostfixExpression ++
            //
            case 352: {
                Expr a = (Expr) btParser.getSym(1);
                btParser.setSym1(nf.Unary(pos(), a, Unary.POST_INC));
                break;
            }
     
            //
            // Rule 353:  PostDecrementExpression ::= PostfixExpression --
            //
            case 353: {
                Expr a = (Expr) btParser.getSym(1);
                btParser.setSym1(nf.Unary(pos(), a, Unary.POST_DEC));
                break;
            }
     
            //
            // Rule 354:  UnaryExpression ::= PreIncrementExpression
            //
            case 354:
                break;
 
            //
            // Rule 355:  UnaryExpression ::= PreDecrementExpression
            //
            case 355:
                break;
 
            //
            // Rule 356:  UnaryExpression ::= + UnaryExpression
            //
            case 356: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Unary(pos(), Unary.POS, a));
                break;
            }
     
            //
            // Rule 357:  UnaryExpression ::= - UnaryExpression
            //
            case 357: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Unary(pos(), Unary.NEG, a));
                break;
            }
     
            //
            // Rule 359:  PreIncrementExpression ::= ++ UnaryExpression
            //
            case 359: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Unary(pos(), Unary.PRE_INC, a));
                break;
            }
     
            //
            // Rule 360:  PreDecrementExpression ::= -- UnaryExpression
            //
            case 360: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Unary(pos(), Unary.PRE_DEC, a));
                break;
            }
     
            //
            // Rule 361:  UnaryExpressionNotPlusMinus ::= PostfixExpression
            //
            case 361:
                break;
 
            //
            // Rule 362:  UnaryExpressionNotPlusMinus ::= ~ UnaryExpression
            //
            case 362: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Unary(pos(), Unary.BIT_NOT, a));
                break;
            }
     
            //
            // Rule 363:  UnaryExpressionNotPlusMinus ::= ! UnaryExpression
            //
            case 363: {
                Expr a = (Expr) btParser.getSym(2);
                btParser.setSym1(nf.Unary(pos(), Unary.NOT, a));
                break;
            }
     
            //
            // Rule 365:  MultiplicativeExpression ::= UnaryExpression
            //
            case 365:
                break;
 
            //
            // Rule 366:  MultiplicativeExpression ::= MultiplicativeExpression * UnaryExpression
            //
            case 366: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.MUL, b));
                break;
            }
     
            //
            // Rule 367:  MultiplicativeExpression ::= MultiplicativeExpression / UnaryExpression
            //
            case 367: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.DIV, b));
                break;
            }
     
            //
            // Rule 368:  MultiplicativeExpression ::= MultiplicativeExpression % UnaryExpression
            //
            case 368: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.MOD, b));
                break;
            }
     
            //
            // Rule 369:  AdditiveExpression ::= MultiplicativeExpression
            //
            case 369:
                break;
 
            //
            // Rule 370:  AdditiveExpression ::= AdditiveExpression + MultiplicativeExpression
            //
            case 370: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.ADD, b));
                break;
            }
     
            //
            // Rule 371:  AdditiveExpression ::= AdditiveExpression - MultiplicativeExpression
            //
            case 371: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.SUB, b));
                break;
            }
     
            //
            // Rule 372:  ShiftExpression ::= AdditiveExpression
            //
            case 372:
                break;
 
            //
            // Rule 373:  ShiftExpression ::= ShiftExpression << AdditiveExpression
            //
            case 373: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.SHL, b));
                break;
            }
     
            //
            // Rule 374:  ShiftExpression ::= ShiftExpression > > AdditiveExpression
            //
            case 374: {
                // TODO: make sure that there is no space between the ">" signs
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(4);
                btParser.setSym1(nf.Binary(pos(), a, Binary.SHR, b));
                break;
            }
     
            //
            // Rule 375:  ShiftExpression ::= ShiftExpression > > > AdditiveExpression
            //
            case 375: {
                // TODO: make sure that there is no space between the ">" signs
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(5);
                btParser.setSym1(nf.Binary(pos(), a, Binary.USHR, b));
                break;
            }
     
            //
            // Rule 376:  RelationalExpression ::= ShiftExpression
            //
            case 376:
                break;
 
            //
            // Rule 377:  RelationalExpression ::= RelationalExpression < ShiftExpression
            //
            case 377: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.LT, b));
                break;
            }
     
            //
            // Rule 378:  RelationalExpression ::= RelationalExpression > ShiftExpression
            //
            case 378: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.GT, b));
                break;
            }
     
            //
            // Rule 379:  RelationalExpression ::= RelationalExpression <= ShiftExpression
            //
            case 379: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.LE, b));
                break;
            }
     
            //
            // Rule 380:  RelationalExpression ::= RelationalExpression > = ShiftExpression
            //
            case 380: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(4);
                btParser.setSym1(nf.Binary(pos(), a, Binary.GE, b));
                break;
            }
     
            //
            // Rule 381:  EqualityExpression ::= RelationalExpression
            //
            case 381:
                break;
 
            //
            // Rule 382:  EqualityExpression ::= EqualityExpression == RelationalExpression
            //
            case 382: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.EQ, b));
                break;
            }
     
            //
            // Rule 383:  EqualityExpression ::= EqualityExpression != RelationalExpression
            //
            case 383: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.NE, b));
                break;
            }
     
            //
            // Rule 384:  AndExpression ::= EqualityExpression
            //
            case 384:
                break;
 
            //
            // Rule 385:  AndExpression ::= AndExpression & EqualityExpression
            //
            case 385: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.BIT_AND, b));
                break;
            }
     
            //
            // Rule 386:  ExclusiveOrExpression ::= AndExpression
            //
            case 386:
                break;
 
            //
            // Rule 387:  ExclusiveOrExpression ::= ExclusiveOrExpression ^ AndExpression
            //
            case 387: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.BIT_XOR, b));
                break;
            }
     
            //
            // Rule 388:  InclusiveOrExpression ::= ExclusiveOrExpression
            //
            case 388:
                break;
 
            //
            // Rule 389:  InclusiveOrExpression ::= InclusiveOrExpression | ExclusiveOrExpression
            //
            case 389: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.BIT_OR, b));
                break;
            }
     
            //
            // Rule 390:  ConditionalAndExpression ::= InclusiveOrExpression
            //
            case 390:
                break;
 
            //
            // Rule 391:  ConditionalAndExpression ::= ConditionalAndExpression && InclusiveOrExpression
            //
            case 391: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.COND_AND, b));
                break;
            }
     
            //
            // Rule 392:  ConditionalOrExpression ::= ConditionalAndExpression
            //
            case 392:
                break;
 
            //
            // Rule 393:  ConditionalOrExpression ::= ConditionalOrExpression || ConditionalAndExpression
            //
            case 393: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Binary(pos(), a, Binary.COND_OR, b));
                break;
            }
     
            //
            // Rule 394:  ConditionalExpression ::= ConditionalOrExpression
            //
            case 394:
                break;
 
            //
            // Rule 395:  ConditionalExpression ::= ConditionalOrExpression ? Expression : ConditionalExpression
            //
            case 395: {
                Expr a = (Expr) btParser.getSym(1),
                     b = (Expr) btParser.getSym(3),
                     c = (Expr) btParser.getSym(5);
                btParser.setSym1(nf.Conditional(pos(), a, b, c));
                break;
            }
     
            //
            // Rule 396:  AssignmentExpression ::= ConditionalExpression
            //
            case 396:
                break;
 
            //
            // Rule 397:  AssignmentExpression ::= Assignment
            //
            case 397:
                break;
 
            //
            // Rule 398:  Assignment ::= LeftHandSide AssignmentOperator AssignmentExpression
            //
            case 398: {
                Expr a = (Expr) btParser.getSym(1);
                Assign.Operator b = (Assign.Operator) btParser.getSym(2);
                Expr c = (Expr) btParser.getSym(3);
                btParser.setSym1(nf.Assign(pos(), a, b, c));
                break;
            }
     
            //
            // Rule 399:  LeftHandSide ::= ExpressionName
            //
            case 399: {
                Name a = (Name) btParser.getSym(1);
                btParser.setSym1(a.toExpr());
                break;
            }
     
            //
            // Rule 400:  LeftHandSide ::= FieldAccess
            //
            case 400:
                break;
 
            //
            // Rule 401:  LeftHandSide ::= ArrayAccess
            //
            case 401:
                break;
 
            //
            // Rule 402:  AssignmentOperator ::= =
            //
            case 402: {
                btParser.setSym1(Assign.ASSIGN);
                break;
            }
     
            //
            // Rule 403:  AssignmentOperator ::= *=
            //
            case 403: {
                btParser.setSym1(Assign.MUL_ASSIGN);
                break;
            }
     
            //
            // Rule 404:  AssignmentOperator ::= /=
            //
            case 404: {
                btParser.setSym1(Assign.DIV_ASSIGN);
                break;
            }
     
            //
            // Rule 405:  AssignmentOperator ::= %=
            //
            case 405: {
                btParser.setSym1(Assign.MOD_ASSIGN);
                break;
            }
     
            //
            // Rule 406:  AssignmentOperator ::= +=
            //
            case 406: {
                btParser.setSym1(Assign.ADD_ASSIGN);
                break;
            }
     
            //
            // Rule 407:  AssignmentOperator ::= -=
            //
            case 407: {
                btParser.setSym1(Assign.SUB_ASSIGN);
                break;
            }
     
            //
            // Rule 408:  AssignmentOperator ::= <<=
            //
            case 408: {
                btParser.setSym1(Assign.SHL_ASSIGN);
                break;
            }
     
            //
            // Rule 409:  AssignmentOperator ::= > > =
            //
            case 409: {
                // TODO: make sure that there is no space between the ">" signs
                btParser.setSym1(Assign.SHR_ASSIGN);
                break;
            }
     
            //
            // Rule 410:  AssignmentOperator ::= > > > =
            //
            case 410: {
                // TODO: make sure that there is no space between the ">" signs
                btParser.setSym1(Assign.USHR_ASSIGN);
                break;
            }
     
            //
            // Rule 411:  AssignmentOperator ::= &=
            //
            case 411: {
                btParser.setSym1(Assign.BIT_AND_ASSIGN);
                break;
            }
     
            //
            // Rule 412:  AssignmentOperator ::= ^=
            //
            case 412: {
                btParser.setSym1(Assign.BIT_XOR_ASSIGN);
                break;
            }
     
            //
            // Rule 413:  AssignmentOperator ::= |=
            //
            case 413: {
                btParser.setSym1(Assign.BIT_OR_ASSIGN);
                break;
            }
     
            //
            // Rule 414:  Expression ::= AssignmentExpression
            //
            case 414:
                break;
 
            //
            // Rule 415:  ConstantExpression ::= Expression
            //
            case 415:
                break;
 
            //
            // Rule 416:  Dimsopt ::= $Empty
            //
            case 416: {
                btParser.setSym1(new Integer(0));
                break;
            }
     
            //
            // Rule 417:  Dimsopt ::= Dims
            //
            case 417:
                break;
 
            //
            // Rule 418:  Catchesopt ::= $Empty
            //
            case 418: {
                btParser.setSym1(new TypedList(new LinkedList(), Catch.class, false));
                break;
            }
     
            //
            // Rule 419:  Catchesopt ::= Catches
            //
            case 419:
                break;
 
            //
            // Rule 420:  identifieropt ::= $Empty
            //
            case 420:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 421:  identifieropt ::= identifier
            //
            case 421: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 422:  ForUpdateopt ::= $Empty
            //
            case 422: {
                btParser.setSym1(new TypedList(new LinkedList(), ForUpdate.class, false));
                break;
            }
     
            //
            // Rule 423:  ForUpdateopt ::= ForUpdate
            //
            case 423:
                break;
 
            //
            // Rule 424:  Expressionopt ::= $Empty
            //
            case 424:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 425:  Expressionopt ::= Expression
            //
            case 425:
                break;
 
            //
            // Rule 426:  ForInitopt ::= $Empty
            //
            case 426: {
                btParser.setSym1(new TypedList(new LinkedList(), ForInit.class, false));
                break;
            }
     
            //
            // Rule 427:  ForInitopt ::= ForInit
            //
            case 427:
                break;
 
            //
            // Rule 428:  SwitchLabelsopt ::= $Empty
            //
            case 428: {
                btParser.setSym1(new TypedList(new LinkedList(), Case.class, false));
                break;
            }
     
            //
            // Rule 429:  SwitchLabelsopt ::= SwitchLabels
            //
            case 429:
                break;
 
            //
            // Rule 430:  SwitchBlockStatementGroupsopt ::= $Empty
            //
            case 430: {
                btParser.setSym1(new TypedList(new LinkedList(), SwitchElement.class, false));
                break;
            }
     
            //
            // Rule 431:  SwitchBlockStatementGroupsopt ::= SwitchBlockStatementGroups
            //
            case 431:
                break;
 
            //
            // Rule 432:  VariableModifiersopt ::= $Empty
            //
            case 432: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 433:  VariableModifiersopt ::= VariableModifiers
            //
            case 433:
                break;
 
            //
            // Rule 434:  VariableInitializersopt ::= $Empty
            //
            case 434:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 435:  VariableInitializersopt ::= VariableInitializers
            //
            case 435:
                break;
 
            //
            // Rule 436:  ElementValuesopt ::= $Empty
            //
            case 436:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 437:  ElementValuesopt ::= ElementValues
            //
            case 437:
                bad_rule = 437;
                break;
 
            //
            // Rule 438:  ElementValuePairsopt ::= $Empty
            //
            case 438:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 439:  ElementValuePairsopt ::= ElementValuePairs
            //
            case 439:
                bad_rule = 439;
                break;
 
            //
            // Rule 440:  DefaultValueopt ::= $Empty
            //
            case 440:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 441:  DefaultValueopt ::= DefaultValue
            //
            case 441:
                break;
 
            //
            // Rule 442:  AnnotationTypeElementDeclarationsopt ::= $Empty
            //
            case 442:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 443:  AnnotationTypeElementDeclarationsopt ::= AnnotationTypeElementDeclarations
            //
            case 443:
                bad_rule = 443;
                break;
 
            //
            // Rule 444:  AbstractMethodModifiersopt ::= $Empty
            //
            case 444: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 445:  AbstractMethodModifiersopt ::= AbstractMethodModifiers
            //
            case 445:
                break;
 
            //
            // Rule 446:  ConstantModifiersopt ::= $Empty
            //
            case 446: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 447:  ConstantModifiersopt ::= ConstantModifiers
            //
            case 447:
                break;
 
            //
            // Rule 448:  InterfaceMemberDeclarationsopt ::= $Empty
            //
            case 448: {
                btParser.setSym1(new TypedList(new LinkedList(), ClassMember.class, false));
                break;
            }
     
            //
            // Rule 449:  InterfaceMemberDeclarationsopt ::= InterfaceMemberDeclarations
            //
            case 449:
                break;
 
            //
            // Rule 450:  ExtendsInterfacesopt ::= $Empty
            //
            case 450: {
                btParser.setSym1(new TypedList(new LinkedList(), TypeNode.class, false));
                break;
            }
     
            //
            // Rule 451:  ExtendsInterfacesopt ::= ExtendsInterfaces
            //
            case 451:
                break;
 
            //
            // Rule 452:  InterfaceModifiersopt ::= $Empty
            //
            case 452: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 453:  InterfaceModifiersopt ::= InterfaceModifiers
            //
            case 453:
                break;
 
            //
            // Rule 454:  ClassBodyopt ::= $Empty
            //
            case 454:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 455:  ClassBodyopt ::= ClassBody
            //
            case 455:
                break;
 
            //
            // Rule 456:  Argumentsopt ::= $Empty
            //
            case 456:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 457:  Argumentsopt ::= Arguments
            //
            case 457:
                bad_rule = 457;
                break;
 
            //
            // Rule 458:  EnumBodyDeclarationsopt ::= $Empty
            //
            case 458:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 459:  EnumBodyDeclarationsopt ::= EnumBodyDeclarations
            //
            case 459:
                bad_rule = 459;
                break;
 
            //
            // Rule 460:  ,opt ::= $Empty
            //
            case 460:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 461:  ,opt ::= ,
            //
            case 461:
                break;
 
            //
            // Rule 462:  EnumConstantsopt ::= $Empty
            //
            case 462:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 463:  EnumConstantsopt ::= EnumConstants
            //
            case 463:
                bad_rule = 463;
                break;
 
            //
            // Rule 464:  ArgumentListopt ::= $Empty
            //
            case 464: {
                btParser.setSym1(new TypedList(new LinkedList(), Catch.class, false));
                break;
            }
     
            //
            // Rule 465:  ArgumentListopt ::= ArgumentList
            //
            case 465:
                break;
 
            //
            // Rule 466:  BlockStatementsopt ::= $Empty
            //
            case 466: {
                btParser.setSym1(new TypedList(new LinkedList(), Stmt.class, false));
                break;
            }
     
            //
            // Rule 467:  BlockStatementsopt ::= BlockStatements
            //
            case 467:
                break;
 
            //
            // Rule 468:  ExplicitConstructorInvocationopt ::= $Empty
            //
            case 468:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 469:  ExplicitConstructorInvocationopt ::= ExplicitConstructorInvocation
            //
            case 469:
                break;
 
            //
            // Rule 470:  ConstructorModifiersopt ::= $Empty
            //
            case 470: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 471:  ConstructorModifiersopt ::= ConstructorModifiers
            //
            case 471:
                break;
 
            //
            // Rule 472:  ...opt ::= $Empty
            //
            case 472:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 473:  ...opt ::= ...
            //
            case 473:
                break;
 
            //
            // Rule 474:  FormalParameterListopt ::= $Empty
            //
            case 474: {
                btParser.setSym1(new TypedList(new LinkedList(), Formal.class, false));
                break;
            }
     
            //
            // Rule 475:  FormalParameterListopt ::= FormalParameterList
            //
            case 475:
                break;
 
            //
            // Rule 476:  Throwsopt ::= $Empty
            //
            case 476: {
                btParser.setSym1(new TypedList(new LinkedList(), TypeNode.class, false));
                break;
            }
     
            //
            // Rule 477:  Throwsopt ::= Throws
            //
            case 477:
                break;
 
            //
            // Rule 478:  MethodModifiersopt ::= $Empty
            //
            case 478: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 479:  MethodModifiersopt ::= MethodModifiers
            //
            case 479:
                break;
 
            //
            // Rule 480:  FieldModifiersopt ::= $Empty
            //
            case 480: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 481:  FieldModifiersopt ::= FieldModifiers
            //
            case 481:
                break;
 
            //
            // Rule 482:  ClassBodyDeclarationsopt ::= $Empty
            //
            case 482: {
                btParser.setSym1(new TypedList(new LinkedList(), ClassMember.class, false));
                break;
            }
     
            //
            // Rule 483:  ClassBodyDeclarationsopt ::= ClassBodyDeclarations
            //
            case 483:
                break;
 
            //
            // Rule 484:  Interfacesopt ::= $Empty
            //
            case 484: {
                btParser.setSym1(new TypedList(new LinkedList(), TypeNode.class, false));
                break;
            }
     
            //
            // Rule 485:  Interfacesopt ::= Interfaces
            //
            case 485:
                break;
 
            //
            // Rule 486:  Superopt ::= $Empty
            //
            case 486:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 487:  Superopt ::= Super
            //
            case 487:
                break;
 
            //
            // Rule 488:  TypeParametersopt ::= $Empty
            //
            case 488:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 489:  TypeParametersopt ::= TypeParameters
            //
            case 489:
                break;
 
            //
            // Rule 490:  ClassModifiersopt ::= $Empty
            //
            case 490: {
                btParser.setSym1(Flags.NONE);
                break;
            }
     
            //
            // Rule 491:  ClassModifiersopt ::= ClassModifiers
            //
            case 491:
                break;
 
            //
            // Rule 492:  Annotationsopt ::= $Empty
            //
            case 492:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 493:  Annotationsopt ::= Annotations
            //
            case 493:
                bad_rule = 493;
                break;
 
            //
            // Rule 494:  TypeDeclarationsopt ::= $Empty
            //
            case 494: {
                btParser.setSym1(new TypedList(new LinkedList(), TopLevelDecl.class, false));
                break;
            }
     
            //
            // Rule 495:  TypeDeclarationsopt ::= TypeDeclarations
            //
            case 495:
                break;
 
            //
            // Rule 496:  ImportDeclarationsopt ::= $Empty
            //
            case 496: {
                btParser.setSym1(new TypedList(new LinkedList(), Import.class, false));
                break;
            }
     
            //
            // Rule 497:  ImportDeclarationsopt ::= ImportDeclarations
            //
            case 497:
                break;
 
            //
            // Rule 498:  PackageDeclarationopt ::= $Empty
            //
            case 498:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 499:  PackageDeclarationopt ::= PackageDeclaration
            //
            case 499:
                break;
 
            //
            // Rule 500:  WildcardBoundsOpt ::= $Empty
            //
            case 500:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 501:  WildcardBoundsOpt ::= WildcardBounds
            //
            case 501:
                bad_rule = 501;
                break;
 
            //
            // Rule 502:  AdditionalBoundListopt ::= $Empty
            //
            case 502:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 503:  AdditionalBoundListopt ::= AdditionalBoundList
            //
            case 503:
                bad_rule = 503;
                break;
 
            //
            // Rule 504:  TypeBoundopt ::= $Empty
            //
            case 504:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 505:  TypeBoundopt ::= TypeBound
            //
            case 505:
                bad_rule = 505;
                break;
 
            //
            // Rule 506:  TypeArgumentsopt ::= $Empty
            //
            case 506:
                btParser.setSym1(null);
                break;
 
            //
            // Rule 507:  TypeArgumentsopt ::= TypeArguments
            //
            case 507:
                bad_rule = 507;
                break;
 
            //
            // Rule 508:  Type ::= DataType PlaceTypeSpecifieropt
            //
            case 508: {
           // Just parse the placetype and drop it for now.
                    break;
            }
         
            //
            // Rule 509:  Type ::= nullable < Type >
            //
            case 509: {
                TypeNode a = (TypeNode) btParser.getSym(3);
                btParser.setSym1(nf.Nullable(pos(), a));
                          break;
            }
              
            //
            // Rule 510:  Type ::= future < Type >
            //
            case 510: {
                TypeNode a = (TypeNode) btParser.getSym(3);
                btParser.setSym1(nf.Future(pos(), a));
                          break;
            }

              
            //
            // Rule 511:  DataType ::= PrimitiveType
            //
            case 511:
                break; 
            
               
            //
            // Rule 512:  DataType ::= ClassOrInterfaceType
            //
            case 512:
                break; 
            
               
            //
            // Rule 513:  DataType ::= ArrayType
            //
            case 513:
                break; 
             
            //
            // Rule 514:  PlaceTypeSpecifier ::= @ PlaceType
            //
            case 514:
                break; 
 
            //
            // Rule 515:  PlaceType ::= place
            //
            case 515:
                break; 
 
            //
            // Rule 516:  PlaceType ::= activity
            //
            case 516:
                break; 
 
            //
            // Rule 517:  PlaceType ::= method
            //
            case 517:
                break; 
 
            //
            // Rule 518:  PlaceType ::= current
            //
            case 518:
                break; 
 
            //
            // Rule 519:  PlaceType ::= PlaceExpression
            //
            case 519:
                break; 
 
            //
            // Rule 520:  ClassOrInterfaceType ::= TypeName DepParametersopt
            //
            case 520: { 
            Name a = (Name) btParser.getSym(1);
            TypeNode t = a.toType();
            DepParameterExpr b = (DepParameterExpr) btParser.getSym(2);
            btParser.setSym1(b == null ? t : nf.ParametricTypeNode(pos(), t, b));
                    break;
            }
        
            //
            // Rule 521:  DepParameters ::= ( DepParameterExpr )
            //
            case 521:
                break; 
        
            //
            // Rule 522:  DepParameterExpr ::= ArgumentList WhereClauseopt
            //
            case 522: {
             List a = (List) btParser.getSym(1);                           
             Expr b = (Expr) btParser.getSym(2);
             btParser.setSym1(nf.DepParameterExpr(pos(),a,b));
                    break;
            }
        
            //
            // Rule 523:  DepParameterExpr ::= WhereClause
            //
            case 523: {
             Expr b = (Expr) btParser.getSym(1);
             btParser.setSym1(nf.DepParameterExpr(pos(), null, b));
                    break;
            }
        
            //
            // Rule 524:  WhereClause ::= : Expression
            //
            case 524:
                break; 
 
            //
            // Rule 526:  X10ArrayType ::= Type [ . ]
            //
            case 526: {
                TypeNode a = (TypeNode) btParser.getSym(1);
                TypeNode t = nf.X10ArrayTypeNode(pos(), a, false, null);
                btParser.setSym1(t);
                break;
            }
     
            //
            // Rule 527:  X10ArrayType ::= Type reference [ . ]
            //
            case 527: {
             TypeNode a = (TypeNode) btParser.getSym(1);
             btParser.setSym1(nf.X10ArrayTypeNode(pos(), a, false, null));
                    break;
            }
        
            //
            // Rule 528:  X10ArrayType ::= Type value [ . ]
            //
            case 528: {
             TypeNode a = (TypeNode) btParser.getSym(1);
                btParser.setSym1(nf.X10ArrayTypeNode(pos(), a, true, null));
                    break;
            }
        
            //
            // Rule 529:  X10ArrayType ::= Type [ DepParameterExpr ]
            //
            case 529: {
             TypeNode a = (TypeNode) btParser.getSym(1);
             DepParameterExpr b = (DepParameterExpr) btParser.getSym(2);
             btParser.setSym1(nf.X10ArrayTypeNode(pos(), a, false, b));
                    break;
            }
        
            //
            // Rule 530:  X10ArrayType ::= Type reference [ DepParameterExpr ]
            //
            case 530: {
             TypeNode a = (TypeNode) btParser.getSym(1);
             DepParameterExpr b = (DepParameterExpr) btParser.getSym(2);
             btParser.setSym1(nf.X10ArrayTypeNode(pos(), a, false, b));
                    break;
            }
        
            //
            // Rule 531:  X10ArrayType ::= Type value [ DepParameterExpr ]
            //
            case 531: {
             TypeNode a = (TypeNode) btParser.getSym(1);
             DepParameterExpr b = (DepParameterExpr) btParser.getSym(2);
             btParser.setSym1(nf.X10ArrayTypeNode(pos(), a, true, b));
                    break;
            }
        
            //
            // Rule 532:  ObjectKind ::= value
            //
            case 532:
                bad_rule = 532;
                break; 
 
            //
            // Rule 533:  ObjectKind ::= reference
            //
            case 533:
                bad_rule = 533;
                break; 
 
            //
            // Rule 534:  MethodModifier ::= atomic
            //
            case 534: {
                btParser.setSym1(Flags.ATOMIC);
                break;
            }
     
            //
            // Rule 535:  MethodModifier ::= extern
            //
            case 535: {
                btParser.setSym1(Flags.NATIVE);
                break;
            }
     
            //
            // Rule 536:  ClassDeclaration ::= ValueClassDeclaration
            //
            case 536:
                break; 
 
            //
            // Rule 537:  ValueClassDeclaration ::= ClassModifiersopt value identifier Superopt Interfacesopt ClassBody
            //
            case 537: {
                Flags a = (Flags) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(3));
                TypeNode c = (TypeNode) btParser.getSym(4);
                List d = (List) btParser.getSym(5);
                ClassBody e = (ClassBody) btParser.getSym(6);
                btParser.setSym1(nf.ValueClassDecl(pos(btParser.getFirstToken(), btParser.getLastToken()), a, b.getIdentifier(), c, d, e));
                break;
            }  
            //
            // Rule 538:  ValueClassDeclaration ::= ClassModifiersopt value class identifier Superopt Interfacesopt ClassBody
            //
            case 538: {
                Flags a = (Flags) btParser.getSym(1);
                polyglot.lex.Identifier b = id(btParser.getToken(4));
                TypeNode c = (TypeNode) btParser.getSym(5);
                List d = (List) btParser.getSym(6);
                ClassBody e = (ClassBody) btParser.getSym(7);
                btParser.setSym1(nf.ValueClassDecl(pos(btParser.getFirstToken(), btParser.getLastToken()), a, b.getIdentifier(), c, d, e));
                break;
            }   
            //
            // Rule 539:  ArrayCreationExpression ::= new ArrayBaseType Unsafeopt [ ] ArrayInitializer
            //
            case 539: {
                TypeNode a = (TypeNode) btParser.getSym(2);
                ArrayInit d = (ArrayInit) btParser.getSym(6);
                // btParser.setSym1(nf.ArrayConstructor(pos(), a, false, null, d));
                btParser.setSym1(nf.NewArray(pos(), a, 1, d));
                break;
            }
     
            //
            // Rule 540:  ArrayCreationExpression ::= new ArrayBaseType Unsafeopt [ Expression ]
            //
            case 540: {
                TypeNode a = (TypeNode) btParser.getSym(2);
                boolean unsafe = (btParser.getSym(3) != null);
                Expr c = (Expr) btParser.getSym(5);
                btParser.setSym1(nf.ArrayConstructor(pos(), a, unsafe, false, c, null));
                break;
            }
     
            //
            // Rule 541:  ArrayCreationExpression ::= new ArrayBaseType Unsafeopt [ Expression ] Expression
            //
            case 541: {
     TypeNode a = (TypeNode) btParser.getSym(2);
     boolean unsafe = (btParser.getSym(3) != null);
     Expr distr = (Expr) btParser.getSym(5);
     Expr initializer = (Expr) btParser.getSym(7);
    btParser.setSym1(nf.ArrayConstructor(pos(), a, unsafe, false, distr, initializer));
                break;
            }
     
            //
            // Rule 542:  ArrayCreationExpression ::= new ArrayBaseType Unsafeopt [ Expression ] ( FormalParameter ) MethodBody
            //
            case 542: {
     TypeNode a = (TypeNode) btParser.getSym(2);
     boolean unsafe = (btParser.getSym(3) != null);
     Expr distr = (Expr) btParser.getSym(5);
     X10Formal f = (X10Formal) btParser.getSym(8);
     Block body = (Block) btParser.getSym(10);
     New initializer = makeInitializer( pos(btParser.getFirstToken(7), btParser.getLastToken(10)), 
                       a, f, body );
    btParser.setSym1(nf.ArrayConstructor(pos(), a, unsafe, false, distr, initializer));
                break;
            }
     
            //
            // Rule 543:  ArrayCreationExpression ::= new ArrayBaseType value Unsafeopt [ Expression ]
            //
            case 543: {
                TypeNode a = (TypeNode) btParser.getSym(2);
                boolean unsafe = (btParser.getSym(3) != null);
                Expr c = (Expr) btParser.getSym(6);
                btParser.setSym1(nf.ArrayConstructor(pos(), a, unsafe, true, c, null));
                break;
            }
     
            //
            // Rule 544:  ArrayCreationExpression ::= new ArrayBaseType value Unsafeopt [ Expression ] Expression
            //
            case 544: {
                TypeNode a = (TypeNode) btParser.getSym(2);
                boolean unsafe = (btParser.getSym(4) != null);
                Expr c = (Expr) btParser.getSym(6);
                Expr d = (Expr) btParser.getSym(8);
                btParser.setSym1(nf.ArrayConstructor(pos(), a, unsafe, true, c, d));
                break;
            }
               
            //
            // Rule 545:  X10ArrayInitializer ::= Expression
            //
            case 545:
                break;
                     // Sigh this is not trivial to do just yet :-(
           
            //
            // Rule 546:  X10ArrayInitializer ::= ( FormalParameter ) MethodBody
            //
            case 546:
                bad_rule = 546;
                break;
                     
            //
            // Rule 547:  ArrayBaseType ::= PrimitiveType
            //
            case 547:
                break;
          
            
            //
            // Rule 548:  ArrayBaseType ::= ClassOrInterfaceType
            //
            case 548:
                break;
          
            //
            // Rule 549:  ArrayAccess ::= ExpressionName [ ArgumentList ]
            //
            case 549: {
           Name e = (Name) btParser.getSym(1);
           List b = (List) btParser.getSym(3);
           if (b.size() == 1)
           btParser.setSym1(nf.X10ArrayAccess1(pos(), e.toExpr(), (Expr) b.get(0)));
           else btParser.setSym1(nf.X10ArrayAccess(pos(), e.toExpr(), b));
                     break;
            }
         
            //
            // Rule 550:  ArrayAccess ::= PrimaryNoNewArray [ ArgumentList ]
            //
            case 550: { 
           Expr a = (Expr) btParser.getSym(1);
           List b = (List) btParser.getSym(3);
           if (b.size() == 1)
           btParser.setSym1(nf.X10ArrayAccess1(pos(), a, (Expr) b.get(0)));
           else btParser.setSym1(nf.X10ArrayAccess(pos(), a, b));
                    break;
            }
        
            //
            // Rule 551:  Statement ::= NowStatement
            //
            case 551:
                break; 
 
            //
            // Rule 552:  Statement ::= ClockedStatement
            //
            case 552:
                break; 
 
            //
            // Rule 553:  Statement ::= AsyncStatement
            //
            case 553:
                break; 
 
            //
            // Rule 554:  Statement ::= AtomicStatement
            //
            case 554:
                break; 
 
            //
            // Rule 555:  Statement ::= WhenStatement
            //
            case 555:
                break; 
 
            //
            // Rule 556:  Statement ::= ForEachStatement
            //
            case 556:
                break; 
 
            //
            // Rule 557:  Statement ::= AtEachStatement
            //
            case 557:
                break; 
 
            //
            // Rule 558:  Statement ::= FinishStatement
            //
            case 558:
                break; 
 
            //
            // Rule 559:  StatementWithoutTrailingSubstatement ::= NextStatement
            //
            case 559:
                break; 
 
            //
            // Rule 560:  StatementWithoutTrailingSubstatement ::= AwaitStatement
            //
            case 560:
                break; 
 
            //
            // Rule 561:  StatementNoShortIf ::= NowStatementNoShortIf
            //
            case 561:
                break; 
 
            //
            // Rule 562:  StatementNoShortIf ::= ClockedStatementNoShortIf
            //
            case 562:
                break; 
 
            //
            // Rule 563:  StatementNoShortIf ::= AsyncStatementNoShortIf
            //
            case 563:
                break; 
 
            //
            // Rule 564:  StatementNoShortIf ::= AtomicStatementNoShortIf
            //
            case 564:
                break; 
 
            //
            // Rule 565:  StatementNoShortIf ::= WhenStatementNoShortIf
            //
            case 565:
                break; 
 
            //
            // Rule 566:  StatementNoShortIf ::= ForEachStatementNoShortIf
            //
            case 566:
                break; 
 
            //
            // Rule 567:  StatementNoShortIf ::= AtEachStatementNoShortIf
            //
            case 567:
                break; 
 
            //
            // Rule 568:  StatementNoShortIf ::= FinishStatementNoShortIf
            //
            case 568:
                break; 
 
            //
            // Rule 569:  NowStatement ::= now ( Clock ) Statement
            //
            case 569: {
                Name a = (Name) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.Now(pos(), a.toExpr(), b));
                break;
            }
     
            //
            // Rule 570:  ClockedStatement ::= clocked ( ClockList ) Statement
            //
            case 570: {
                List a = (List) btParser.getSym(3);
                Block b = (Block) btParser.getSym(5);
                btParser.setSym1(nf.Clocked(pos(), a, b));
                break;
            }
     
            //
            // Rule 571:  AsyncStatement ::= async PlaceExpressionSingleListopt Statement
            //
            case 571: {
                Expr e = (Expr) btParser.getSym(2);
                Stmt b = (Stmt) btParser.getSym(3);
                btParser.setSym1(nf.Async(pos(), (e == null ? nf.Here(pos(btParser.getFirstToken())) : e), b));
                break;
            }
     
            //
            // Rule 572:  AtomicStatement ::= atomic PlaceExpressionSingleListopt Statement
            //
            case 572: {
                Expr e = (Expr) btParser.getSym(2);
                Stmt b = (Stmt) btParser.getSym(3);
                btParser.setSym1(nf.Atomic(pos(), (e == null ? nf.Here(pos(btParser.getFirstToken())) : e), b));
                break;
            }
     
            //
            // Rule 573:  WhenStatement ::= when ( Expression ) Statement
            //
            case 573: {
                Expr e = (Expr) btParser.getSym(3);
                Stmt s = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.When(pos(), e,s));
                break;
            }
     
            //
            // Rule 574:  WhenStatement ::= WhenStatement or ( Expression ) Statement
            //
            case 574: {
                When w = (When) btParser.getSym(1);
                Expr e = (Expr) btParser.getSym(4);
                Stmt s = (Stmt) btParser.getSym(6);
                When.Branch wb = nf.WhenBranch(pos(btParser.getFirstToken(2), btParser.getLastToken(6)), e, s);
                w.add(wb);
                btParser.setSym1(w);
                break;
            }
     
            //
            // Rule 575:  ForEachStatement ::= foreach ( FormalParameter : Expression ) Statement
            //
            case 575: {
       X10Formal f = (X10Formal) btParser.getSym(3);
       Formal ff =  f.flags(f.flags().Final()); // make it final
       Expr e = (Expr) btParser.getSym(5);
       Stmt s = (Stmt) btParser.getSym(7);
       X10Loop x = nf.ForEach(pos(), ff, e, 
                              f.hasExplodedVars() 
                              ? nf.Block(pos(), f.explode(s))
                              : s);
       btParser.setSym1(x);
              break;
            }  
            //
            // Rule 576:  AtEachStatement ::= ateach ( FormalParameter : Expression ) Statement
            //
            case 576: {
       X10Formal f = (X10Formal) btParser.getSym(3);
       Formal ff = f.flags(f.flags().Final()); // make it final
       Expr e = (Expr) btParser.getSym(5);
       Stmt s = (Stmt) btParser.getSym(7);
       X10Loop x = nf.AtEach(pos(), ff, e, 
                              f.hasExplodedVars() 
                              ? nf.Block(pos(), f.explode(s))
                              : s);
       btParser.setSym1(x);
              break;
            }  
            //
            // Rule 577:  EnhancedForStatement ::= for ( FormalParameter : Expression ) Statement
            //
            case 577: {
      X10Formal f = (X10Formal) btParser.getSym(3);
       Formal ff = f.flags(f.flags().Final()); // make it final
       Expr e = (Expr) btParser.getSym(5);
       Stmt s = (Stmt) btParser.getSym(7);
       X10Loop x = nf.ForLoop(pos(), ff, e, 
                              f.hasExplodedVars() 
                              ? nf.Block(pos(), f.explode(s))
                              : s);
       btParser.setSym1(x);           
              break;
            }  
            //
            // Rule 578:  FinishStatement ::= finish Statement
            //
            case 578: {
                Stmt b = (Stmt) btParser.getSym(2);
                btParser.setSym1(nf.Finish(pos(),  b));
                break;
            }
     
            //
            // Rule 579:  NowStatementNoShortIf ::= now ( Clock ) StatementNoShortIf
            //
            case 579: {
                Name a = (Name) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.Now(pos(), a.toExpr(), b));
                break;
            }
     
            //
            // Rule 580:  ClockedStatementNoShortIf ::= clocked ( ClockList ) StatementNoShortIf
            //
            case 580: {
                List a = (List) btParser.getSym(3);
                Stmt b = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.Clocked(pos(), a, b));
                break;
            }
     
            //
            // Rule 581:  AsyncStatementNoShortIf ::= async PlaceExpressionSingleListopt StatementNoShortIf
            //
            case 581: {
                Expr e = (Expr) btParser.getSym(2);
                Stmt b = (Stmt) btParser.getSym(3);
                btParser.setSym1(nf.Async(pos(), (e == null ? nf.Here(pos(btParser.getFirstToken())) : e), b));
                break;
            }
     
            //
            // Rule 582:  AtomicStatementNoShortIf ::= atomic StatementNoShortIf
            //
            case 582: {
                Expr e = (Expr) btParser.getSym(2);
                Stmt b = (Stmt) btParser.getSym(3);
                btParser.setSym1(nf.Atomic(pos(), (e == null ? nf.Here(pos(btParser.getFirstToken())) : e), b));
                break;
            }
     
            //
            // Rule 583:  WhenStatementNoShortIf ::= when ( Expression ) StatementNoShortIf
            //
            case 583: {
                Expr e = (Expr) btParser.getSym(3);
                Stmt s = (Stmt) btParser.getSym(5);
                btParser.setSym1(nf.When(pos(), e,s));
                break;
            }
     
            //
            // Rule 584:  WhenStatementNoShortIf ::= WhenStatement or ( Expression ) StatementNoShortIf
            //
            case 584: {
                When w = (When) btParser.getSym(1);
                Expr e = (Expr) btParser.getSym(4);
                Stmt s = (Stmt) btParser.getSym(6);
                When.Branch wb = nf.WhenBranch(pos(btParser.getFirstToken(2), btParser.getLastToken(6)), e, s);
                w.add(wb);
                btParser.setSym1(w);
                break;
            }
     
            //
            // Rule 585:  ForEachStatementNoShortIf ::= foreach ( FormalParameter : Expression ) StatementNoShortIf
            //
            case 585: {
       X10Formal f = (X10Formal) btParser.getSym(3);
       Formal ff = f.flags(f.flags().Final()); // make it final
       Expr e = (Expr) btParser.getSym(5);
       Stmt s = (Stmt) btParser.getSym(7);
       X10Loop x = nf.ForEach(pos(), ff, e, 
                              f.hasExplodedVars() 
                              ? nf.Block(pos(), f.explode(s))
                              : s);
       btParser.setSym1(x);
              break;
            }  
            //
            // Rule 586:  AtEachStatementNoShortIf ::= ateach ( FormalParameter : Expression ) StatementNoShortIf
            //
            case 586: {
       X10Formal f = (X10Formal) btParser.getSym(3);
       Formal ff = f.flags(f.flags().Final()); // make it final
       Expr e = (Expr) btParser.getSym(5);
       Stmt s = (Stmt) btParser.getSym(7);
       X10Loop x = nf.AtEach(pos(), ff, e, 
                              f.hasExplodedVars() 
                              ? nf.Block(pos(), f.explode(s))
                              : s);
       btParser.setSym1(x);
              break;
            }  
            //
            // Rule 587:  EnhancedForStatementNoShortIf ::= for ( FormalParameter : Expression ) StatementNoShortIf
            //
            case 587: {
       X10Formal f = (X10Formal) btParser.getSym(3);
       Formal ff = f.flags(f.flags().Final()); // make it final
       Expr e = (Expr) btParser.getSym(5);
       Stmt s = (Stmt) btParser.getSym(7);
       X10Loop x = nf.ForLoop(pos(), ff, e, 
                              f.hasExplodedVars() 
                              ? nf.Block(pos(), f.explode(s))
                              : s);
       btParser.setSym1(x);
               break;
            }  
            //
            // Rule 588:  FinishStatementNoShortIf ::= finish StatementNoShortIf
            //
            case 588: {
                Stmt b = (Stmt) btParser.getSym(2);
                btParser.setSym1(nf.Finish(pos(),  b));
                break;
            }
     
            //
            // Rule 589:  PlaceExpressionSingleList ::= ( PlaceExpression )
            //
            case 589: {
                btParser.setSym1(btParser.getSym(2));
                break;
            }
     
            //
            // Rule 590:  PlaceExpression ::= here
            //
            case 590: {
                  btParser.setSym1(nf.Here(pos(btParser.getFirstToken())));
                break;
            }
     
            //
            // Rule 591:  PlaceExpression ::= this
            //
            case 591: {
                btParser.setSym1(nf.Field(pos(btParser.getFirstToken()), nf.This(pos(btParser.getFirstToken())), "place"));
                break;
            }
     
            //
            // Rule 592:  PlaceExpression ::= Expression
            //
            case 592:
                break;
     
            //
            // Rule 593:  PlaceExpression ::= ArrayAccess
            //
            case 593:
                bad_rule = 593;
                break; 
 
            //
            // Rule 594:  NextStatement ::= next ;
            //
            case 594: {
                btParser.setSym1(nf.Next(pos()));
                break;
            }
     
            //
            // Rule 595:  AwaitStatement ::= await Expression ;
            //
            case 595: { 
         Expr e = (Expr) btParser.getSym(2);
         btParser.setSym1(nf.Await(pos(), e));
                 break;
            }
     
            //
            // Rule 596:  ClockList ::= Clock
            //
            case 596: {
                Name c = (Name) btParser.getSym(1);
                List l = new TypedList(new LinkedList(), Expr.class, false);
                l.add(c.toExpr());
                btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 597:  ClockList ::= ClockList , Clock
            //
            case 597: {
                List l = (List) btParser.getSym(1);
                Name c = (Name) btParser.getSym(3);
                l.add(c.toExpr());
                // btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 598:  Clock ::= identifier
            //
            case 598: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
     
            //
            // Rule 599:  CastExpression ::= ( Type ) UnaryExpressionNotPlusMinus
            //
            case 599: {
                TypeNode a = (TypeNode) btParser.getSym(2);
                Expr b = (Expr) btParser.getSym(4);
                btParser.setSym1(nf.Cast(pos(), a, b));
                break;
            }
     
            //
            // Rule 600:  MethodInvocation ::= Primary -> identifier ( ArgumentListopt )
            //
            case 600: { 
          Expr a = (Expr) btParser.getSym(1);
          polyglot.lex.Identifier b = id(btParser.getToken(3));
          List c = (List) btParser.getSym(5);
          btParser.setSym1(nf.RemoteCall(pos(), a, b.getIdentifier(), c));
                 break;
            } 
     
            //
            // Rule 601:  RelationalExpression ::= RelationalExpression instanceof Type
            //
            case 601: {
                Expr a = (Expr) btParser.getSym(1);
                TypeNode b = (TypeNode) btParser.getSym(3);
                btParser.setSym1(nf.Instanceof(pos(), a, b));
                break;
            }
     
            //
            // Rule 602:  ExpressionName ::= here
            //
            case 602: {
          btParser.setSym1(new Name(nf, ts, pos(), "here"){
              public Expr toExpr() {
                 return ((X10NodeFactory) nf).Here(pos);
              }
           });

                  break;
            }
       
            //
            // Rule 603:  IdentifierList ::= IdentifierList , identifier
            //
            case 603: { 
       List l = (List) btParser.getSym(1);
       polyglot.lex.Identifier a = id(btParser.getToken(3));
       l.add(new Name(nf, ts, pos(), a.getIdentifier()));
       btParser.setSym1(l);
                break;
            }
     
            //
            // Rule 604:  IdentifierList ::= identifier
            //
            case 604: {
       polyglot.lex.Identifier a = id(btParser.getToken(1));
       List l = new TypedList(new LinkedList(), Name.class, false);
       l.add(new Name(nf, ts, pos(), a.getIdentifier()));
       btParser.setSym1(l);
                 break;
            }
     
            //
            // Rule 605:  Primary ::= FutureExpression
            //
            case 605:
                break; 
 
            //
            // Rule 606:  Primary ::= [ ArgumentList ]
            //
            case 606: {
       Name x10 = new Name(nf, ts, pos(), "x10");
        Name x10Lang = new Name(nf, ts, pos(), x10, "lang");
        Name x10LangRegion = new Name(nf, ts, pos(), x10Lang, "region");
        Name x10LangRegionFactory = new Name(nf, ts, pos(), x10LangRegion, "factory");
        Name x10LangRegionFactoryRegion = new Name(nf, ts, pos(), x10LangRegionFactory, "region");
        Name x10LangPoint = new Name(nf, ts, pos(), x10Lang, "point");
        Name x10LangPointFactory = new Name(nf, ts, pos(), x10LangPoint, "factory");
        Name x10LangPointFactoryPoint = new Name(nf, ts, pos(), x10LangPointFactory, "point");

        List a = (List) btParser.getSym(2);
        Tuple tuple  = nf.Tuple(pos(), x10LangPointFactoryPoint, x10LangRegionFactoryRegion, a);
        btParser.setSym1(tuple);
               break;
            }
     
            //
            // Rule 607:  AssignmentExpression ::= Expression -> Expression
            //
            case 607: {
        Expr a = (Expr) btParser.getSym(1);
        Expr b = (Expr) btParser.getSym(3);
        //System.out.println("Distribution:" + a + "|" + b + "|");
        // x10.lang.region.factory.region(  ArgumentList )
        // Construct the MethodName
        Name x10 = new Name(nf, ts, pos(), "x10");
        Name x10Lang = new Name(nf, ts, pos(), x10, "lang");

        Name x10LangDistribution = new Name(nf, ts, pos(), x10Lang, "distribution");
        Name x10LangDistributionFactory = 
             new Name(nf, ts, pos(), x10LangDistribution, "factory");
        Name x10LangDistributionFactoryConstant = 
            new Name(nf, ts, pos(), x10LangDistributionFactory, "constant");
        List l = new TypedList(new LinkedList(), Expr.class, false);
        l.add(a);
        l.add(b);
        Call call = nf.Call(pos(), x10LangDistributionFactoryConstant.prefix.toReceiver(), "constant", l);
        btParser.setSym1(call);
               break;
            }
     
            //
            // Rule 608:  Primary ::= Expression : Expression
            //
            case 608: {

        Expr a = (Expr) btParser.getSym(1);
        Expr b = (Expr) btParser.getSym(3);
        Name x10 = new Name(nf, ts, pos(), "x10");
        Name x10Lang = new Name(nf, ts, pos(), x10, "lang");

        Name x10LangRegion = new Name(nf, ts, pos(), x10Lang, "region");
        Name x10LangRegionFactory = new Name(nf, ts, pos(), x10LangRegion, "factory");
        Name x10LangRegionFactoryRegion = new Name(nf, ts, pos(), x10LangRegionFactory, "region");
        List l = new TypedList(new LinkedList(), Expr.class, false);
        l.add(a);
        l.add(b);
        Call regionCall = nf.Call( pos(), x10LangRegionFactoryRegion.prefix.toReceiver(), "region", l  );
        btParser.setSym1(regionCall);
               break;
            }
     
            //
            // Rule 609:  FutureExpression ::= future PlaceExpressionSingleListopt { Expression }
            //
            case 609: {
                Expr e1 = (Expr) btParser.getSym(2),
                     e2 = (Expr) btParser.getSym(4);
                btParser.setSym1(nf.Future(pos(), (e1 == null ? nf.Here(pos(btParser.getFirstToken())) : e1), e2));
                break;
            }
     
            //
            // Rule 610:  FieldModifier ::= mutable
            //
            case 610: {
                btParser.setSym1(Flags.MUTABLE);
                break;
            }
     
            //
            // Rule 611:  FieldModifier ::= const
            //
            case 611: {
                btParser.setSym1(Flags.PUBLIC.set(Flags.STATIC).set(Flags.FINAL));
                break;
            }
     
            //
            // Rule 612:  FunExpression ::= fun Type ( FormalParameterListopt ) { Expression }
            //
            case 612:
                bad_rule = 612;
                break; 
 
            //
            // Rule 613:  MethodInvocation ::= MethodName ( ArgumentListopt ) ( ArgumentListopt )
            //
            case 613:
                bad_rule = 613;
                break; 
 
            //
            // Rule 614:  MethodInvocation ::= Primary . identifier ( ArgumentListopt ) ( ArgumentListopt )
            //
            case 614:
                bad_rule = 614;
                break; 
 
            //
            // Rule 615:  MethodInvocation ::= super . identifier ( ArgumentListopt ) ( ArgumentListopt )
            //
            case 615:
                bad_rule = 615;
                break; 
 
            //
            // Rule 616:  MethodInvocation ::= ClassName . super . identifier ( ArgumentListopt ) ( ArgumentListopt )
            //
            case 616:
                bad_rule = 616;
                break; 
 
            //
            // Rule 617:  MethodInvocation ::= TypeName . identifier ( ArgumentListopt ) ( ArgumentListopt )
            //
            case 617:
                bad_rule = 617;
                break; 
 
            //
            // Rule 618:  ClassInstanceCreationExpression ::= new ClassOrInterfaceType ( ArgumentListopt ) ( ArgumentListopt ) ClassBodyopt
            //
            case 618:
                bad_rule = 618;
                break; 
 
            //
            // Rule 619:  ClassInstanceCreationExpression ::= Primary . new identifier ( ArgumentListopt ) ( ArgumentListopt ) ClassBodyopt
            //
            case 619:
                bad_rule = 619;
                break; 
 
            //
            // Rule 620:  ClassInstanceCreationExpression ::= AmbiguousName . new identifier ( ArgumentListopt ) ( ArgumentListopt ) ClassBodyopt
            //
            case 620:
                bad_rule = 620;
                break; 
 
            //
            // Rule 621:  PlaceTypeSpecifieropt ::= $Empty
            //
            case 621:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 622:  PlaceTypeSpecifieropt ::= PlaceTypeSpecifier
            //
            case 622:
                break; 
 
            //
            // Rule 623:  DepParametersopt ::= $Empty
            //
            case 623:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 624:  DepParametersopt ::= DepParameters
            //
            case 624:
                break; 
 
            //
            // Rule 625:  WhereClauseopt ::= $Empty
            //
            case 625:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 626:  WhereClauseopt ::= WhereClause
            //
            case 626:
                break; 
 
            //
            // Rule 627:  ObjectKindopt ::= $Empty
            //
            case 627:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 628:  ObjectKindopt ::= ObjectKind
            //
            case 628:
                break; 
 
            //
            // Rule 629:  ArrayInitializeropt ::= $Empty
            //
            case 629:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 630:  ArrayInitializeropt ::= ArrayInitializer
            //
            case 630:
                break; 
 
            //
            // Rule 631:  PlaceExpressionSingleListopt ::= $Empty
            //
            case 631:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 632:  PlaceExpressionSingleListopt ::= PlaceExpressionSingleList
            //
            case 632:
                break; 
 
            //
            // Rule 633:  ArgumentListopt ::= $Empty
            //
            case 633:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 634:  ArgumentListopt ::= ArgumentList
            //
            case 634:
                break; 
 
            //
            // Rule 635:  DepParametersopt ::= $Empty
            //
            case 635:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 636:  DepParametersopt ::= DepParameters
            //
            case 636:
                break; 
 
            //
            // Rule 637:  Unsafeopt ::= $Empty
            //
            case 637:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 638:  Unsafeopt ::= unsafe
            //
            case 638: { btParser.setSym1(nf.Here(pos(btParser.getFirstToken(1))));           break;
            } 
 
            //
            // Rule 639:  ParamIdopt ::= $Empty
            //
            case 639:
                btParser.setSym1(null);
                break; 
 
            //
            // Rule 640:  ParamIdopt ::= identifier
            //
            case 640: {
                polyglot.lex.Identifier a = id(btParser.getToken(1));
                btParser.setSym1(new Name(nf, ts, pos(), a.getIdentifier()));
                break;
            }
        
            default:
                break;
        }
        return;
    }
}

