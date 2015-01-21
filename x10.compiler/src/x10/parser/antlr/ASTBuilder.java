package x10.parser.antlr;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JDialog;

import lpg.runtime.IToken;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import polyglot.ast.AmbTypeNode;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Case;
import polyglot.ast.Catch;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.FlagsNode;
import polyglot.ast.ForInit;
import polyglot.ast.ForUpdate;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Import;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.PackageNode;
import polyglot.ast.ProcedureDecl;
import polyglot.ast.SourceFile;
import polyglot.ast.Stmt;
import polyglot.ast.SwitchElement;
import polyglot.ast.TopLevelDecl;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.frontend.FileSource;
import polyglot.frontend.Compiler;
import polyglot.parse.ParsedName;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.Position;
import polyglot.util.TypedList;
import x10.X10CompilerOptions;
import x10.ast.AmbMacroTypeNode;
import x10.ast.AnnotationNode;
import x10.ast.AtExpr;
import x10.ast.ClosureCall;
import x10.ast.DepParameterExpr;
import x10.ast.PropertyDecl;
import x10.ast.SettableAssign;
import x10.ast.TypeDecl;
import x10.ast.TypeParamNode;
import x10.ast.X10Binary_c;
import x10.ast.X10Formal;
import x10.ast.X10Unary_c;
import x10.extension.X10Ext;
import x10.parser.X10SemanticRules.JPGPosition;
import x10.parserGen.*;
import x10.parserGen.X10Parser.AnnotationStatementContext;
import x10.parserGen.X10Parser.ApplyOperatorDeclarationContext;
import x10.parserGen.X10Parser.AssertStatement0Context;
import x10.parserGen.X10Parser.AssertStatement1Context;
import x10.parserGen.X10Parser.AssignPropertyCallContext;
import x10.parserGen.X10Parser.AsyncStatement0Context;
import x10.parserGen.X10Parser.AsyncStatement1Context;
import x10.parserGen.X10Parser.AtEachStatement0Context;
import x10.parserGen.X10Parser.AtEachStatement1Context;
import x10.parserGen.X10Parser.AtExpressionContext;
import x10.parserGen.X10Parser.AtStatementContext;
import x10.parserGen.X10Parser.AtomicStatementContext;
import x10.parserGen.X10Parser.BasicForStatementContext;
import x10.parserGen.X10Parser.BinaryOperatorDeclContext;
import x10.parserGen.X10Parser.BinaryOperatorDeclThisLeftContext;
import x10.parserGen.X10Parser.BinaryOperatorDeclThisRightContext;
import x10.parserGen.X10Parser.BreakStatementContext;
import x10.parserGen.X10Parser.CastExpression0Context;
import x10.parserGen.X10Parser.CastExpression1Context;
import x10.parserGen.X10Parser.CastExpression2Context;
import x10.parserGen.X10Parser.CatchClauseContext;
import x10.parserGen.X10Parser.CatchesContext;
import x10.parserGen.X10Parser.ClassDeclarationContext;
import x10.parserGen.X10Parser.ClassNameContext;
import x10.parserGen.X10Parser.ClassTypeContext;
import x10.parserGen.X10Parser.ClockedClauseoptContext;
import x10.parserGen.X10Parser.ClosureBody0Context;
import x10.parserGen.X10Parser.ClosureBody1Context;
import x10.parserGen.X10Parser.ClosureBody2Context;
import x10.parserGen.X10Parser.ClosureExpressionContext;
import x10.parserGen.X10Parser.CompilationUnitContext;
import x10.parserGen.X10Parser.ConstrainedTypeContext;
import x10.parserGen.X10Parser.ConstraintConjunctionoptContext;
import x10.parserGen.X10Parser.ConstructorDeclarationContext;
import x10.parserGen.X10Parser.ContinueStatementContext;
import x10.parserGen.X10Parser.ConversionOperatorDeclarationExplicitContext;
import x10.parserGen.X10Parser.ConversionOperatorDeclarationImplicitContext;
import x10.parserGen.X10Parser.DepNamedType0Context;
import x10.parserGen.X10Parser.DepNamedType1Context;
import x10.parserGen.X10Parser.DepParametersContext;
import x10.parserGen.X10Parser.DoStatementContext;
import x10.parserGen.X10Parser.EmptyStatementContext;
import x10.parserGen.X10Parser.EnhancedForStatement0Context;
import x10.parserGen.X10Parser.EnhancedForStatement1Context;
import x10.parserGen.X10Parser.ExplicitConstructorInvocationPrimarySuperContext;
import x10.parserGen.X10Parser.ExplicitConstructorInvocationPrimaryThisContext;
import x10.parserGen.X10Parser.ExplicitConstructorInvocationSuperContext;
import x10.parserGen.X10Parser.ExplicitConstructorInvocationThisContext;
import x10.parserGen.X10Parser.ExplicitConversionOperatorDecl0Context;
import x10.parserGen.X10Parser.ExplicitConversionOperatorDecl1Context;
import x10.parserGen.X10Parser.ExpressionContext;
import x10.parserGen.X10Parser.ExpressionStatementContext;
import x10.parserGen.X10Parser.FieldDeclarationContext;
import x10.parserGen.X10Parser.FinallyBlockContext;
import x10.parserGen.X10Parser.FinishStatement0Context;
import x10.parserGen.X10Parser.FinishStatement1Context;
import x10.parserGen.X10Parser.ForInit0Context;
import x10.parserGen.X10Parser.ForInit1Context;
import x10.parserGen.X10Parser.ForStatement0Context;
import x10.parserGen.X10Parser.ForStatement1Context;
import x10.parserGen.X10Parser.ForUpdateContext;
import x10.parserGen.X10Parser.FormalParametersContext;
import x10.parserGen.X10Parser.FunctionTypeContext;
import x10.parserGen.X10Parser.HasZeroConstraintContext;
import x10.parserGen.X10Parser.IfThenElseStatementContext;
import x10.parserGen.X10Parser.IfThenStatementContext;
import x10.parserGen.X10Parser.ImplicitConversionOperatorDeclarationContext;
import x10.parserGen.X10Parser.ImportDeclarationContext;
import x10.parserGen.X10Parser.ImportDeclarationsoptContext;
import x10.parserGen.X10Parser.InterfaceDeclarationContext;
import x10.parserGen.X10Parser.IsRefConstraintContext;
import x10.parserGen.X10Parser.LabeledStatementContext;
import x10.parserGen.X10Parser.LastExpressionContext;
import x10.parserGen.X10Parser.LoopStatement0Context;
import x10.parserGen.X10Parser.LoopStatement1Context;
import x10.parserGen.X10Parser.LoopStatement2Context;
import x10.parserGen.X10Parser.LoopStatement3Context;
import x10.parserGen.X10Parser.MethodDeclarationApplyOpContext;
import x10.parserGen.X10Parser.MethodDeclarationBinaryOpContext;
import x10.parserGen.X10Parser.MethodDeclarationConversionOpContext;
import x10.parserGen.X10Parser.MethodDeclarationMethodContext;
import x10.parserGen.X10Parser.MethodDeclarationPrefixOpContext;
import x10.parserGen.X10Parser.MethodDeclarationSetOpContext;
import x10.parserGen.X10Parser.MethodModifierContext;
import x10.parserGen.X10Parser.MethodModifierModifierContext;
import x10.parserGen.X10Parser.MethodModifierPropertyContext;
import x10.parserGen.X10Parser.MethodModifiersoptContext;
import x10.parserGen.X10Parser.ModifierAbstractContext;
import x10.parserGen.X10Parser.ModifierAnnotationContext;
import x10.parserGen.X10Parser.ModifierAtomicContext;
import x10.parserGen.X10Parser.ModifierClockedContext;
import x10.parserGen.X10Parser.ModifierContext;
import x10.parserGen.X10Parser.ModifierFinalContext;
import x10.parserGen.X10Parser.ModifierNativeContext;
import x10.parserGen.X10Parser.ModifierPrivateContext;
import x10.parserGen.X10Parser.ModifierProtectedContext;
import x10.parserGen.X10Parser.ModifierPublicContext;
import x10.parserGen.X10Parser.ModifierStaticContext;
import x10.parserGen.X10Parser.ModifierTransientContext;
import x10.parserGen.X10Parser.ModifiersoptContext;
import x10.parserGen.X10Parser.NamedType0Context;
import x10.parserGen.X10Parser.NamedType1Context;
import x10.parserGen.X10Parser.NamedTypeNoConstraints0Context;
import x10.parserGen.X10Parser.NamedTypeNoConstraints1Context;
import x10.parserGen.X10Parser.NonExpressionStatemen0Context;
import x10.parserGen.X10Parser.NonExpressionStatemen10Context;
import x10.parserGen.X10Parser.NonExpressionStatemen11Context;
import x10.parserGen.X10Parser.NonExpressionStatemen12Context;
import x10.parserGen.X10Parser.NonExpressionStatemen13Context;
import x10.parserGen.X10Parser.NonExpressionStatemen14Context;
import x10.parserGen.X10Parser.NonExpressionStatemen15Context;
import x10.parserGen.X10Parser.NonExpressionStatemen16Context;
import x10.parserGen.X10Parser.NonExpressionStatemen17Context;
import x10.parserGen.X10Parser.NonExpressionStatemen18Context;
import x10.parserGen.X10Parser.NonExpressionStatemen19Context;
import x10.parserGen.X10Parser.NonExpressionStatemen1Context;
import x10.parserGen.X10Parser.NonExpressionStatemen20Context;
import x10.parserGen.X10Parser.NonExpressionStatemen21Context;
import x10.parserGen.X10Parser.NonExpressionStatemen22Context;
import x10.parserGen.X10Parser.NonExpressionStatemen2Context;
import x10.parserGen.X10Parser.NonExpressionStatemen3Context;
import x10.parserGen.X10Parser.NonExpressionStatemen4Context;
import x10.parserGen.X10Parser.NonExpressionStatemen5Context;
import x10.parserGen.X10Parser.NonExpressionStatemen6Context;
import x10.parserGen.X10Parser.NonExpressionStatemen7Context;
import x10.parserGen.X10Parser.NonExpressionStatemen8Context;
import x10.parserGen.X10Parser.NonExpressionStatemen9Context;
import x10.parserGen.X10Parser.OBSOLETE_FinishExpressionContext;
import x10.parserGen.X10Parser.OBSOLETE_OfferStatementContext;
import x10.parserGen.X10Parser.OBSOLETE_TypeParamWithVariance0Context;
import x10.parserGen.X10Parser.OBSOLETE_TypeParamWithVariance1Context;
import x10.parserGen.X10Parser.PackageDeclarationContext;
import x10.parserGen.X10Parser.PackageName0Context;
import x10.parserGen.X10Parser.PackageName1Context;
import x10.parserGen.X10Parser.PackageNameContext;
import x10.parserGen.X10Parser.ParameterizedNamedType0Context;
import x10.parserGen.X10Parser.ParameterizedNamedType1Context;
import x10.parserGen.X10Parser.ParameterizedNamedType2Context;
import x10.parserGen.X10Parser.PrefixOperatorDeclContext;
import x10.parserGen.X10Parser.PrefixOperatorDeclThisContext;
import x10.parserGen.X10Parser.PropertiesoptContext;
import x10.parserGen.X10Parser.PropertyContext;
import x10.parserGen.X10Parser.PropertyMethodDecl0Context;
import x10.parserGen.X10Parser.PropertyMethodDecl1Context;
import x10.parserGen.X10Parser.ReturnStatementContext;
import x10.parserGen.X10Parser.SetOperatorDeclarationContext;
import x10.parserGen.X10Parser.SimpleNamedType0Context;
import x10.parserGen.X10Parser.SimpleNamedType1Context;
import x10.parserGen.X10Parser.SimpleNamedType2Context;
import x10.parserGen.X10Parser.Statement0Context;
import x10.parserGen.X10Parser.Statement1Context;
import x10.parserGen.X10Parser.StatementExpressionListContext;
import x10.parserGen.X10Parser.StructDeclarationContext;
import x10.parserGen.X10Parser.SubtypeConstraint0Context;
import x10.parserGen.X10Parser.SubtypeConstraint1Context;
import x10.parserGen.X10Parser.SuperExtendsoptContext;
import x10.parserGen.X10Parser.SwitchBlockContext;
import x10.parserGen.X10Parser.SwitchBlockStatementGroupContext;
import x10.parserGen.X10Parser.SwitchBlockStatementGroupsoptContext;
import x10.parserGen.X10Parser.SwitchLabel0Context;
import x10.parserGen.X10Parser.SwitchLabel1Context;
import x10.parserGen.X10Parser.SwitchLabelContext;
import x10.parserGen.X10Parser.SwitchLabelsContext;
import x10.parserGen.X10Parser.SwitchLabelsoptContext;
import x10.parserGen.X10Parser.SwitchStatementContext;
import x10.parserGen.X10Parser.ThrowStatementContext;
import x10.parserGen.X10Parser.TryStatement0Context;
import x10.parserGen.X10Parser.TryStatement1Context;
import x10.parserGen.X10Parser.TypeAnnotationsContext;
import x10.parserGen.X10Parser.TypeArgumentsContext;
import x10.parserGen.X10Parser.TypeConstrainedTypeContext;
import x10.parserGen.X10Parser.TypeContext;
import x10.parserGen.X10Parser.TypeDeclarationContext;
import x10.parserGen.X10Parser.TypeDeclarationsoptContext;
import x10.parserGen.X10Parser.TypeDefDeclarationContext;
import x10.parserGen.X10Parser.TypeFunctionTypeContext;
import x10.parserGen.X10Parser.TypeName0Context;
import x10.parserGen.X10Parser.TypeName1Context;
import x10.parserGen.X10Parser.TypeParamWithVarianceList0Context;
import x10.parserGen.X10Parser.TypeParamWithVarianceList1Context;
import x10.parserGen.X10Parser.TypeParamWithVarianceList2Context;
import x10.parserGen.X10Parser.TypeParamWithVarianceList3Context;
import x10.parserGen.X10Parser.TypeParameterContext;
import x10.parserGen.X10Parser.TypeParameterListContext;
import x10.parserGen.X10Parser.TypeParametersoptContext;
import x10.parserGen.X10Parser.TypeParamsWithVarianceoptContext;
import x10.parserGen.X10Parser.TypeVoidContext;
import x10.parserGen.X10Parser.VarKeyword0Context;
import x10.parserGen.X10Parser.VarKeyword1Context;
import x10.parserGen.X10Parser.Void_Context;
import x10.parserGen.X10Parser.WhenStatementContext;
import x10.parserGen.X10Parser.WhereClauseoptContext;
import x10.parserGen.X10Parser.WhileStatementContext;
import x10.types.ParameterType;
import x10.types.checker.Converter;

public class ASTBuilder extends X10BaseListener implements X10Listener, polyglot.frontend.Parser {

    private X10Parser p;
    private X10Lexer lexer;

    private X10CompilerOptions compilerOpts;
    private ErrorQueue eq;
    private ParserErrorListener err;
    private TypeSystem ts;
    private NodeFactory nf;
    private FileSource srce;

    public ASTBuilder(X10CompilerOptions opts, TypeSystem t, NodeFactory n, FileSource source, ErrorQueue q) {
        compilerOpts = opts;
        ts = t;
        nf = n;
        srce = source;
        eq = q;

        String fileName = source.name();
        ANTLRInputStream input;
        try {
            input = new ANTLRInputStream(new FileInputStream(fileName));
            lexer = new X10Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            p = new X10Parser(tokens);
            p.removeErrorListeners();
            err = new ParserErrorListener(eq, fileName);
            p.addErrorListener(err);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        } catch (IOException e) {
            input = null;
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Node parse() {
        CompilationUnitContext tree = p.compilationUnit();
        if (compilerOpts.x10_config.DISPLAY_PARSE_TREE) {
            Future<JDialog> dialogHdl = tree.inspect(p);
            try {
                JDialog dialog = dialogHdl.get();
                dialog.setTitle(srce.toString());
                Utils.waitForClose(dialog);
            } catch (Exception e) {
                eq.enqueue(ErrorInfo.WARNING, srce + ": unable to display the parse tree.");
            }
        }
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
        SourceFile sf = tree.ast;
        return sf.source(srce);
    }

    // Utility functions

    /** Returns the position of a given parse tree node. */
    private Position pos(ParserRuleContext ctx) {
        if (ctx.getStop() == null) {
            return new Position(null, srce.path(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        } else {
            return new Position(null, srce.path(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
        }
    }

    /** Returns the position of a given token. */
    private Position pos(Token t) {
        return new Position(null, srce.path(), t.getLine(), t.getCharPositionInLine());
    }

    private String comment(Position pos) {
        // IToken[] adjuncts = prsStream.getTokenAt(i).getPrecedingAdjuncts();
        // String s = null;
        // for (IToken a : adjuncts) {
        // String c = a.toString();
        // if (c.startsWith("/**") && c.endsWith("*/")) {
        // s = c;
        // }
        // }
        // return s;
        return null;
    }

    private void checkTypeName(Id identifier) {
        String filename = srce.name();
        String idname = identifier.id().toString();
        int dot = filename.lastIndexOf('.'), slash = filename.lastIndexOf('/', dot);
        if (slash == -1)
            slash = filename.lastIndexOf('\\', dot);
        String clean_filename = (slash >= 0 && dot >= 0 ? filename.substring(slash + 1, dot) : "");
        if ((!clean_filename.equals(idname)) && clean_filename.equalsIgnoreCase(idname))
            err.syntaxError("This type name does not match the name of the containing file: " + filename.substring(slash + 1), identifier.position());
    }

    // Temporary classes used to wrap modifiers.

    public static class Modifier {
    }

    public static class FlagModifier extends Modifier {
        public static int ABSTRACT = 0;
        public static int ATOMIC = 1;
        // public static int EXTERN = 2;
        public static int FINAL = 3;
        // public static int GLOBAL = 4;
        // public static int INCOMPLETE = 5;
        public static int NATIVE = 6;
        // public static int NON_BLOCKING = 7;
        public static int PRIVATE = 8;
        public static int PROPERTY = 9;
        public static int PROTECTED = 10;
        public static int PUBLIC = 11;
        // public static int SAFE = 12;
        // public static int SEQUENTIAL = 13;
        public static int CLOCKED = 14;
        public static int STATIC = 15;
        public static int TRANSIENT = 16;
        public static int NUM_FLAGS = TRANSIENT + 1;

        private Position pos;
        private int flag;

        public Position position() {
            return pos;
        }

        public int flag() {
            return flag;
        }

        public Flags flags() {
            if (flag == ABSTRACT)
                return Flags.ABSTRACT;
            if (flag == ATOMIC)
                return Flags.ATOMIC;
            // if (flag == EXTERN) return X10Flags.EXTERN;
            if (flag == FINAL)
                return Flags.FINAL;
            // if (flag == GLOBAL) return X10Flags.GLOBAL;
            // if (flag == INCOMPLETE) return X10Flags.INCOMPLETE;
            if (flag == NATIVE)
                return Flags.NATIVE;
            // if (flag == NON_BLOCKING) return X10Flags.NON_BLOCKING;
            if (flag == PRIVATE)
                return Flags.PRIVATE;
            if (flag == PROPERTY)
                return Flags.PROPERTY;
            if (flag == PROTECTED)
                return Flags.PROTECTED;
            if (flag == PUBLIC)
                return Flags.PUBLIC;
            // if (flag == SAFE) return X10Flags.SAFE;
            // if (flag == SEQUENTIAL) return X10Flags.SEQUENTIAL;
            if (flag == CLOCKED)
                return Flags.CLOCKED;
            if (flag == TRANSIENT)
                return Flags.TRANSIENT;
            if (flag == STATIC)
                return Flags.STATIC;
            assert (false);
            return null;
        }

        public String name() {
            if (flag == ABSTRACT)
                return "abstract";
            if (flag == ATOMIC)
                return "atomic";
            // if (flag == EXTERN) return "extern";
            if (flag == FINAL)
                return "final";
            // if (flag == GLOBAL) return "global";
            // if (flag == INCOMPLETE) return "incomplete";
            if (flag == NATIVE)
                return "native";
            // if (flag == NON_BLOCKING) return "nonblocking";
            if (flag == PRIVATE)
                return "private";
            if (flag == PROPERTY)
                return "property";
            if (flag == PROTECTED)
                return "protected";
            if (flag == PUBLIC)
                return "public";
            // if (flag == SAFE) return "safe";
            // if (flag == SEQUENTIAL) return "sequential";
            if (flag == CLOCKED)
                return "clocked";
            if (flag == STATIC)
                return "static";
            if (flag == TRANSIENT)
                return "transient";
            assert (false);
            return "?";
        }

        public static boolean classModifiers[] = new boolean[NUM_FLAGS];
        static {
            classModifiers[ABSTRACT] = true;
            classModifiers[FINAL] = true;
            classModifiers[PRIVATE] = true;
            classModifiers[PROTECTED] = true;
            classModifiers[PUBLIC] = true;
            // classModifiers[SAFE] = true;
            classModifiers[STATIC] = true;
            classModifiers[CLOCKED] = true;
            // classModifiers[GLOBAL] = true;
        }

        public boolean isClassModifier(int flag) {
            return classModifiers[flag];
        }

        public static boolean typeDefModifiers[] = new boolean[NUM_FLAGS];
        static {
            typeDefModifiers[ABSTRACT] = true;
            typeDefModifiers[FINAL] = true;
            typeDefModifiers[PRIVATE] = true;
            typeDefModifiers[PROTECTED] = true;
            typeDefModifiers[PUBLIC] = true;
            typeDefModifiers[STATIC] = true;
        }

        public boolean isTypeDefModifier(int flag) {
            return typeDefModifiers[flag];
        }

        public static boolean fieldModifiers[] = new boolean[NUM_FLAGS];
        static {
            fieldModifiers[TRANSIENT] = true;
            // fieldModifiers[GLOBAL] = true;
            fieldModifiers[CLOCKED] = true;
            fieldModifiers[PRIVATE] = true;
            fieldModifiers[PROTECTED] = true;
            fieldModifiers[PROPERTY] = true;
            fieldModifiers[PUBLIC] = true;
            fieldModifiers[STATIC] = true;
        }

        public boolean isFieldModifier(int flag) {
            return fieldModifiers[flag];
        }

        public static boolean variableModifiers[] = new boolean[NUM_FLAGS];
        static {
            variableModifiers[CLOCKED] = true;
        }

        public boolean isVariableModifier(int flag) {
            return variableModifiers[flag];
        }

        public static boolean methodModifiers[] = new boolean[NUM_FLAGS];
        static {
            methodModifiers[ABSTRACT] = true;
            methodModifiers[ATOMIC] = true;
            // methodModifiers[EXTERN] = true;
            methodModifiers[FINAL] = true;
            // methodModifiers[GLOBAL] = true;
            // methodModifiers[INCOMPLETE] = true;
            methodModifiers[NATIVE] = true;
            // methodModifiers[NON_BLOCKING] = true;
            methodModifiers[PRIVATE] = true;
            methodModifiers[PROPERTY] = true;
            methodModifiers[PROTECTED] = true;
            methodModifiers[PUBLIC] = true;
            // methodModifiers[SAFE] = true;
            // methodModifiers[SEQUENTIAL] = true;
            methodModifiers[STATIC] = true;
            // methodModifiers[CLOCKED] = true;
        }

        public boolean isMethodModifier(int flag) {
            return methodModifiers[flag];
        }

        public static boolean constructorModifiers[] = new boolean[NUM_FLAGS];
        static {
            constructorModifiers[NATIVE] = true;
            constructorModifiers[PRIVATE] = true;
            constructorModifiers[PROTECTED] = true;
            constructorModifiers[PUBLIC] = true;
        }

        public boolean isConstructorModifier(int flag) {
            return constructorModifiers[flag];
        }

        public static boolean interfaceModifiers[] = new boolean[NUM_FLAGS];
        static {
            interfaceModifiers[ABSTRACT] = true;
            interfaceModifiers[PRIVATE] = true;
            interfaceModifiers[PROTECTED] = true;
            interfaceModifiers[PUBLIC] = true;
            interfaceModifiers[STATIC] = true;
            interfaceModifiers[CLOCKED] = true;

        }

        public boolean isInterfaceModifier(int flag) {
            return interfaceModifiers[flag];
        }

        public FlagModifier(Position pos, int flag) {
            this.pos = pos;
            this.flag = flag;
        }
    }

    private static class AnnotationModifier extends Modifier {
        private AnnotationNode annotation;

        public AnnotationNode annotation() {
            return annotation;
        }

        public AnnotationModifier(AnnotationNode annotation) {
            this.annotation = annotation;
        }
    }

    //
    // TODO: Say something!
    //
    private List<Node> checkModifiers(String kind, List<Modifier> modifiers, boolean legal_flags[]) {
        List<Node> l = new LinkedList<Node>();

        assert (modifiers.size() > 0);

        boolean flags[] = new boolean[FlagModifier.NUM_FLAGS]; // initialized to
                                                               // false
        for (int i = 0; i < modifiers.size(); i++) {
            Object element = modifiers.get(i);
            if (element instanceof FlagModifier) {
                FlagModifier modifier = (FlagModifier) element;
                l.addAll(Collections.singletonList(nf.FlagsNode(modifier.position(), modifier.flags())));

                if (!flags[modifier.flag()]) {
                    flags[modifier.flag()] = true;
                } else {
                    err.syntaxError("Duplicate specification of modifier: " + modifier.name(), modifier.position());
                }

                if (!legal_flags[modifier.flag()]) {
                    err.syntaxError("\"" + modifier.name() + "\" is not a valid " + kind + " modifier", modifier.position());
                }
            } else {
                AnnotationModifier modifier = (AnnotationModifier) element;
                l.addAll(Collections.singletonList(modifier.annotation()));
            }
        }

        return l;
    }

    private List<Node> checkClassModifiers(List<Modifier> modifiers) {
        return (modifiers.size() == 0 ? Collections.<Node> singletonList(nf.FlagsNode(Position.COMPILER_GENERATED, Flags.NONE)) : checkModifiers("class", modifiers,
                FlagModifier.classModifiers));
    }

    private List<Node> checkTypeDefModifiers(List<Modifier> modifiers) {
        return (modifiers.size() == 0 ? Collections.<Node> singletonList(nf.FlagsNode(Position.COMPILER_GENERATED, Flags.NONE)) : checkModifiers("typedef", modifiers,
                FlagModifier.typeDefModifiers));
    }

    private List<Node> checkFieldModifiers(List<Modifier> modifiers) {
        return (modifiers.size() == 0 ? Collections.<Node> emptyList() : checkModifiers("field", modifiers, FlagModifier.fieldModifiers));
    }

    private List<Node> checkVariableModifiers(List<Modifier> modifiers) {
        return (modifiers.size() == 0 ? Collections.<Node> emptyList() : checkModifiers("variable", modifiers, FlagModifier.variableModifiers));
    }

    private List<Node> checkMethodModifiers(List<Modifier> modifiers) {
        return (modifiers.size() == 0 ? Collections.<Node> emptyList() : checkModifiers("method", modifiers, FlagModifier.methodModifiers));
    }

    private List<Node> checkConstructorModifiers(List<Modifier> modifiers) {
        return (modifiers.size() == 0 ? Collections.<Node> emptyList() : checkModifiers("constructor", modifiers, FlagModifier.constructorModifiers));
    }

    private List<Node> checkInterfaceModifiers(List<Modifier> modifiers) {
        return (modifiers.size() == 0 ? Collections.<Node> emptyList() : checkModifiers("interface", modifiers, FlagModifier.interfaceModifiers));
    }

    private List<AnnotationNode> extractAnnotations(List<? extends Node> l) {
        List<AnnotationNode> l2 = new LinkedList<AnnotationNode>();
        for (Node n : l) {
            if (n instanceof AnnotationNode) {
                l2.add((AnnotationNode) n);
            }
        }
        return l2;
    }

    private FlagsNode extractFlags(List<? extends Node> l, Flags f) {
        FlagsNode fn = extractFlags(l);
        fn = fn.flags(fn.flags().set(f));
        return fn;
    }

    private FlagsNode extractFlags(List<? extends Node> l1, List<? extends Node> l2) {
        List<Node> l = new ArrayList<Node>();
        l.addAll(l1);
        l.addAll(l2);
        return extractFlags(l);
    }

    private FlagsNode extractFlags(List<? extends Node> l) {
        Position pos = null;
        Flags xf = Flags.NONE;
        for (Node n : l) {
            if (n instanceof FlagsNode) {
                FlagsNode fn = (FlagsNode) n;
                pos = pos == null ? fn.position() : new Position(pos, fn.position());
                Flags f = fn.flags();
                xf = xf.set(f);
            }
        }
        return nf.FlagsNode(pos == null ? Position.COMPILER_GENERATED : pos, xf);
    }

    // Grammar actions

    @Override
    public void exitCompilationUnit(CompilationUnitContext ctx) {
        List<Import> importDeclarationsopt = ctx.importDeclarationsopt().ast == null ? new TypedList<Import>(new LinkedList<Import>(), Import.class, false) : ctx
                .importDeclarationsopt().ast;
        List<TopLevelDecl> typeDeclarationsopt = ctx.typeDeclarationsopt().ast == null ? new TypedList<TopLevelDecl>(new LinkedList<TopLevelDecl>(), TopLevelDecl.class, false)
                : ctx.typeDeclarationsopt().ast;

        PackageNode packageDeclaration = ctx.packageDeclaration() == null ? null : ctx.packageDeclaration().ast;
        ctx.ast = nf.SourceFile(pos(ctx), packageDeclaration, importDeclarationsopt, typeDeclarationsopt);

    }

    @Override
    public void exitImportDeclarationsopt(ImportDeclarationsoptContext ctx) {
        List<Import> l = new TypedList<Import>(new LinkedList<Import>(), Import.class, false);
        for (ImportDeclarationContext importDeclaration : ctx.importDeclaration()) {
            l.add(importDeclaration.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitTypeDeclarationsopt(TypeDeclarationsoptContext ctx) {
        List<TopLevelDecl> l = new TypedList<TopLevelDecl>(new LinkedList<TopLevelDecl>(), TopLevelDecl.class, false);
        for (TypeDeclarationContext typeDecl : ctx.typeDeclaration()) {
            l.add(typeDecl.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitPackageDeclaration(PackageDeclarationContext ctx) {
        List<AnnotationNode> Annotationsopt = ctx.annotationsopt().ast;
        ParsedName PackageName = ctx.packageName().ast;
        PackageNode pn = PackageName.toPackage();
        pn = (PackageNode) ((X10Ext) pn.ext()).annotations(Annotationsopt);
        ctx.ast = pn;
    }

    @Override
    public void exitModifiersopt(ModifiersoptContext ctx) {
        List<Modifier> l = new LinkedList<Modifier>();
        for (ModifierContext m : ctx.modifier()) {
            l.add(m.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitModifierAnnotation(ModifierAnnotationContext ctx) {
        ctx.ast = new AnnotationModifier(ctx.annotation().ast);
    }

    @Override
    public void exitModifierPrivate(ModifierPrivateContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.PRIVATE);
    }

    @Override
    public void exitModifierNative(ModifierNativeContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.NATIVE);
    }

    @Override
    public void exitModifierTransient(ModifierTransientContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.TRANSIENT);
    }

    @Override
    public void exitModifierClocked(ModifierClockedContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.CLOCKED);
    }

    @Override
    public void exitModifierFinal(ModifierFinalContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.FINAL);
    }

    @Override
    public void exitModifierAbstract(ModifierAbstractContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.ABSTRACT);
    }

    @Override
    public void exitModifierAtomic(ModifierAtomicContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.ATOMIC);
    }

    @Override
    public void exitModifierStatic(ModifierStaticContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.STATIC);
    }

    @Override
    public void exitModifierProtected(ModifierProtectedContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.PROTECTED);
    }

    @Override
    public void exitModifierPublic(ModifierPublicContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.PUBLIC);
    }

    @Override
    public void exitMethodModifiersopt(MethodModifiersoptContext ctx) {
        List<Modifier> l = new LinkedList<Modifier>();
        for (MethodModifierContext m : ctx.methodModifier()) {
            l.add(m.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitMethodModifierModifier(MethodModifierModifierContext ctx) {
        ctx.ast = ctx.modifier().ast;
    }

    @Override
    public void exitMethodModifierProperty(MethodModifierPropertyContext ctx) {
        ctx.ast = new FlagModifier(pos(ctx), FlagModifier.PROPERTY);
    }

    @Override
    public void exitTypeDefDeclaration(TypeDefDeclarationContext ctx) {
        List<Modifier> Modifiersopt = ctx.modifiersopt().ast;
        Id Identifier = ctx.identifier().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        List<Formal> FormalParameterList = ctx.formalParameterList() == null ? new ArrayList<Formal>() : ctx.formalParameterList().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode Type = ctx.type().ast;
        List<Node> modifiers = checkTypeDefModifiers(Modifiersopt);
        FlagsNode f = extractFlags(modifiers);
        List<AnnotationNode> annotations = extractAnnotations(modifiers);
        List<Formal> formals = new ArrayList<Formal>();
        for (Formal v : FormalParameterList) {
            FlagsNode flags = v.flags();
            if (!flags.flags().isFinal()) {
                err.syntaxError("Type definition parameters must be final.", v.position());
                v = v.flags(flags.flags(flags.flags().Final()));
            }
            formals.add(v);
        }
        TypeDecl cd = nf.TypeDecl(pos(ctx), f, Identifier, TypeParametersopt, formals, WhereClauseopt, Type);
        cd = (TypeDecl) ((X10Ext) cd.ext()).annotations(annotations);
        ctx.ast = cd;
    }

    @Override
    public void exitPropertiesopt(PropertiesoptContext ctx) {
        List<PropertyDecl> l = new TypedList<PropertyDecl>(new LinkedList<PropertyDecl>(), PropertyDecl.class, false);
        for (PropertyContext e : ctx.property()) {
            l.add(e.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitProperty(PropertyContext ctx) {
        List<AnnotationNode> Annotationsopt = ctx.annotationsopt().ast;
        Id Identifier = ctx.identifier().ast;
        TypeNode ResultType = ctx.resultType().ast;
        List<AnnotationNode> annotations = extractAnnotations(Annotationsopt);
        PropertyDecl cd = nf.PropertyDecl(pos(ctx), nf.FlagsNode(pos(ctx), Flags.PUBLIC.Final()), ResultType, Identifier);
        cd = (PropertyDecl) ((X10Ext) cd.ext()).annotations(annotations);
        ctx.ast = cd;
    }

    @Override
    public void exitMethodDeclarationSetOp(MethodDeclarationSetOpContext ctx) {
        ctx.ast = ctx.setOperatorDeclaration().ast;
    }

    @Override
    public void exitMethodDeclarationConversionOp(MethodDeclarationConversionOpContext ctx) {
        ctx.ast = ctx.conversionOperatorDeclaration().ast;
    }

    @Override
    public void exitMethodDeclarationBinaryOp(MethodDeclarationBinaryOpContext ctx) {
        ctx.ast = ctx.binaryOperatorDeclaration().ast;
    }

    @Override
    public void exitMethodDeclarationApplyOp(MethodDeclarationApplyOpContext ctx) {
        ctx.ast = ctx.applyOperatorDeclaration().ast;
    }

    @Override
    public void exitMethodDeclarationMethod(MethodDeclarationMethodContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        Id Identifier = ctx.identifier().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        List<Formal> FormalParameters = ctx.formalParameters().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> Throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        ProcedureDecl pd = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                Identifier, TypeParametersopt, FormalParameters, WhereClauseopt, OBSOLETE_Offersopt, Throwsopt, MethodBody);
        pd = (ProcedureDecl) ((X10Ext) pd.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = pd;
    }

    @Override
    public void exitMethodDeclarationPrefixOp(MethodDeclarationPrefixOpContext ctx) {
        ctx.ast = ctx.prefixOperatorDeclaration().ast;
    }

    @Override
    public void exitBinaryOperatorDecl(BinaryOperatorDeclContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        X10Formal fp1 = ctx.fp1.ast;
        Binary.Operator BinOp = ctx.binOp().ast;
        X10Formal fp2 = ctx.fp2.ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Name opName = X10Binary_c.binaryMethodName(BinOp);
        if (opName == null) {
            err.syntaxError("Cannot override binary operator '" + BinOp + "'.", pos(ctx));
            opName = Name.make("invalid operator");
        }
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx.binOp()), opName), TypeParametersopt, Arrays.<Formal> asList(fp1, fp2), WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (!flags.flags().isStatic()) {
            err.syntaxError("Binary operator with two parameters must be static.", md.position());
            md = md.flags(flags.flags(flags.flags().Static()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitBinaryOperatorDeclThisLeft(BinaryOperatorDeclThisLeftContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        Binary.Operator BinOp = ctx.binOp().ast;
        X10Formal fp2 = ctx.fp2.ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Name opName = X10Binary_c.binaryMethodName(BinOp);
        if (opName == null) {
            err.syntaxError("Cannot override binary operator '" + BinOp + "'.", pos(ctx));
            opName = Name.make("invalid operator");
        }
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx.binOp()), opName), TypeParametersopt, Collections.<Formal> singletonList(fp2), WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (flags.flags().isStatic()) {
            err.syntaxError("Binary operator with this parameter cannot be static.", md.position());
            md = md.flags(flags.flags(flags.flags().clearStatic()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitBinaryOperatorDeclThisRight(BinaryOperatorDeclThisRightContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        X10Formal fp1 = ctx.fp1.ast;
        Binary.Operator BinOp = ctx.binOp().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Name opName = X10Binary_c.invBinaryMethodName(BinOp);
        if (opName == null) {
            // [DC] doesn't look like this can ever happen?
            err.syntaxError("Cannot override binary operator '" + BinOp + "'.", pos(ctx));
            opName = Name.make("invalid operator");
        }
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx.binOp()), opName), TypeParametersopt, Collections.<Formal> singletonList(fp1), WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (flags.flags().isStatic()) {
            err.syntaxError("Binary operator with this parameter cannot be static.", md.position());
            md = md.flags(flags.flags(flags.flags().clearStatic()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitPrefixOperatorDecl(PrefixOperatorDeclContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        Unary.Operator PrefixOp = ctx.prefixOp().ast;
        X10Formal fp2 = ctx.formalParameter().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Name opName = X10Unary_c.unaryMethodName(PrefixOp);
        if (opName == null) {
            err.syntaxError("Cannot override unary operator '" + PrefixOp + "'.", pos(ctx));
            opName = Name.make("invalid operator");
        }
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx.prefixOp()), opName), TypeParametersopt, Collections.<Formal> singletonList(fp2), WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (!flags.flags().isStatic()) {
            err.syntaxError("Unary operator with one parameter must be static.", md.position());
            md = md.flags(flags.flags(flags.flags().Static()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitPrefixOperatorDeclThis(PrefixOperatorDeclThisContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        Unary.Operator PrefixOp = ctx.prefixOp().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Name opName = X10Unary_c.unaryMethodName(PrefixOp);
        if (opName == null) {
            err.syntaxError("Cannot override unary operator '" + PrefixOp + "'.", pos(ctx));
            opName = Name.make("invalid operator");
        }
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx.prefixOp()), opName), TypeParametersopt, Collections.<Formal> emptyList(), WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (flags.flags().isStatic()) {
            err.syntaxError("Unary operator with this parameter cannot be static.", md.position());
            md = md.flags(flags.flags(flags.flags().clearStatic()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitApplyOperatorDeclaration(ApplyOperatorDeclarationContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        List<Formal> FormalParameters = ctx.formalParameters().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx), ClosureCall.APPLY), TypeParametersopt, FormalParameters, WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (flags.flags().isStatic()) {
            err.syntaxError("operator() cannot be static.", md.position());
            md = md.flags(flags.flags(flags.flags().clearStatic()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitSetOperatorDeclaration(SetOperatorDeclarationContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        List<Formal> FormalParameters = ctx.formalParameters().ast;
        X10Formal fp2 = ctx.formalParameter().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx), SettableAssign.SET), TypeParametersopt, CollectionUtil.append(FormalParameters, Collections.singletonList(fp2)), WhereClauseopt,
                OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (flags.flags().isStatic()) {
            err.syntaxError("Set operator cannot be static.", md.position());
            md = md.flags(flags.flags(flags.flags().clearStatic()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitConversionOperatorDeclarationExplicit(ConversionOperatorDeclarationExplicitContext ctx) {
        ctx.ast = ctx.explicitConversionOperatorDeclaration().ast;
    }

    @Override
    public void exitConversionOperatorDeclarationImplicit(ConversionOperatorDeclarationImplicitContext ctx) {
        ctx.ast = ctx.implicitConversionOperatorDeclaration().ast;
    }

    @Override
    public void exitExplicitConversionOperatorDecl0(ExplicitConversionOperatorDecl0Context ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        X10Formal fp1 = ctx.formalParameter().ast;
        TypeNode Type = ctx.type().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), Type, nf.Id(pos(ctx), Converter.operator_as), TypeParametersopt,
                Collections.<Formal> singletonList(fp1), WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (!flags.flags().isStatic()) {
            err.syntaxError("Conversion operator must be static.", md.position());
            md = md.flags(flags.flags(flags.flags().Static()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitExplicitConversionOperatorDecl1(ExplicitConversionOperatorDecl1Context ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        X10Formal fp1 = ctx.formalParameter().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx), Converter.operator_as), TypeParametersopt, Collections.<Formal> singletonList(fp1), WhereClauseopt, OBSOLETE_Offersopt, throwsopt, MethodBody);
        FlagsNode flags = md.flags();
        if (!flags.flags().isStatic()) {
            err.syntaxError("Conversion operator must be static.", md.position());
            md = md.flags(flags.flags(flags.flags().Static()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitImplicitConversionOperatorDeclaration(ImplicitConversionOperatorDeclarationContext ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        X10Formal fp1 = ctx.formalParameter().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> throwsopt = ctx.throwsopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        Position bodyStart = MethodBody == null ? pos(ctx).endOf() : MethodBody.position().startOf();
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers), HasResultTypeopt == null ? nf.UnknownTypeNode(bodyStart.markCompilerGenerated()) : HasResultTypeopt,
                nf.Id(pos(ctx), Converter.implicit_operator_as), TypeParametersopt, Collections.<Formal> singletonList(fp1), WhereClauseopt, OBSOLETE_Offersopt, throwsopt,
                MethodBody);
        FlagsNode flags = md.flags();
        if (!flags.flags().isStatic()) {
            err.syntaxError("Conversion operator must be static.", md.position());
            md = md.flags(flags.flags(flags.flags().Static()));
        }
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitPropertyMethodDecl0(PropertyMethodDecl0Context ctx) {
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        Id Identifier = ctx.identifier().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        List<Formal> FormalParameters = ctx.formalParameters().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers, Flags.PROPERTY), HasResultTypeopt == null ? nf.UnknownTypeNode(pos(ctx).markCompilerGenerated())
                : HasResultTypeopt, Identifier, TypeParametersopt, FormalParameters, WhereClauseopt, null, // offersOpt
                Collections.<TypeNode> emptyList(), MethodBody);
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitPropertyMethodDecl1(PropertyMethodDecl1Context ctx) {
        err.syntaxError("This syntax is no longer supported. You must supply the property method formals, and if there are none, you can use an empty parenthesis '()'.", pos(ctx));
        List<Modifier> MethodModifiersopt = ctx.methodModifiersopt().ast;
        Id Identifier = ctx.identifier().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        Block MethodBody = ctx.methodBody().ast;
        List<Node> modifiers = checkMethodModifiers(MethodModifiersopt);
        MethodDecl md = nf.X10MethodDecl(pos(ctx), extractFlags(modifiers, Flags.PROPERTY), HasResultTypeopt == null ? nf.UnknownTypeNode(pos(ctx).markCompilerGenerated())
                : HasResultTypeopt, Identifier, Collections.<TypeParamNode> emptyList(), Collections.<Formal> emptyList(), WhereClauseopt, null, // offersOpt
                Collections.<TypeNode> emptyList(), MethodBody);
        md = (MethodDecl) ((X10Ext) md.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = md;
    }

    @Override
    public void exitExplicitConstructorInvocationThis(ExplicitConstructorInvocationThisContext ctx) {
        List<TypeNode> TypeArgumentsopt = ctx.typeArgumentsopt().ast;
        List<Expr> ArgumentListopt = ctx.argumentListopt().ast;
        ctx.ast = nf.X10ThisCall(pos(ctx), TypeArgumentsopt, ArgumentListopt);
    }

    @Override
    public void exitExplicitConstructorInvocationPrimaryThis(ExplicitConstructorInvocationPrimaryThisContext ctx) {
        Expr Primary = ctx.primary().ast;
        List<TypeNode> TypeArgumentsopt = ctx.typeArgumentsopt().ast;
        List<Expr> ArgumentListopt = ctx.argumentListopt().ast;
        ctx.ast = nf.X10ThisCall(pos(ctx), Primary, TypeArgumentsopt, ArgumentListopt);
    }

    @Override
    public void exitExplicitConstructorInvocationSuper(ExplicitConstructorInvocationSuperContext ctx) {
        List<TypeNode> TypeArgumentsopt = ctx.typeArgumentsopt().ast;
        List<Expr> ArgumentListopt = ctx.argumentListopt().ast;
        ctx.ast = nf.X10SuperCall(pos(ctx), TypeArgumentsopt, ArgumentListopt);
    }

    @Override
    public void exitExplicitConstructorInvocationPrimarySuper(ExplicitConstructorInvocationPrimarySuperContext ctx) {
        Expr Primary = ctx.primary().ast;
        List<TypeNode> TypeArgumentsopt = ctx.typeArgumentsopt().ast;
        List<Expr> ArgumentListopt = ctx.argumentListopt().ast;
        ctx.ast = nf.X10SuperCall(pos(ctx), Primary, TypeArgumentsopt, ArgumentListopt);
    }

    @Override
    public void exitInterfaceDeclaration(InterfaceDeclarationContext ctx) {
        List<Modifier> Modifiersopt = ctx.modifiersopt().ast;
        Id Identifier = ctx.identifier().ast;
        List<TypeParamNode> TypeParamsWithVarianceopt = ctx.typeParamsWithVarianceopt().ast;
        List<PropertyDecl> Propertiesopt = ctx.propertiesopt().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        List<TypeNode> ExtendsInterfacesopt = ctx.extendsInterfacesopt().ast;
        ClassBody InterfaceBody = ctx.interfaceBody().ast;
        List<Node> modifiers = checkInterfaceModifiers(Modifiersopt);
        checkTypeName(Identifier);
        List<TypeParamNode> TypeParametersopt = TypeParamsWithVarianceopt;
        List<PropertyDecl> props = Propertiesopt;
        // we use the property syntax for annotation-interfaces:
        // public interface Pragma(pragma:Int) extends StatementAnnotation { ...
        // }
        DepParameterExpr ci = WhereClauseopt;
        FlagsNode fn = extractFlags(modifiers, Flags.INTERFACE);
        ClassDecl cd = nf.X10ClassDecl(pos(ctx), fn, Identifier, TypeParametersopt, props, ci, null, ExtendsInterfacesopt, InterfaceBody);
        cd = (ClassDecl) ((X10Ext) cd.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = cd;
    }

    @Override
    public void exitAssignPropertyCall(AssignPropertyCallContext ctx) {
        List<TypeNode> TypeArgumentsopt = ctx.typeArgumentsopt().ast;
        List<Expr> ArgumentListopt = ctx.argumentListopt().ast;
        ctx.ast = nf.AssignPropertyCall(pos(ctx), TypeArgumentsopt, ArgumentListopt);
    }

    @Override
    public void exitTypeFunctionType(TypeFunctionTypeContext ctx) {
        ctx.ast = ctx.functionType().ast;
    }

    @Override
    public void exitTypeConstrainedType(TypeConstrainedTypeContext ctx) {
        ctx.ast = ctx.constrainedType().ast;
    }

    @Override
    public void exitTypeVoid(TypeVoidContext ctx) {
        ctx.ast = ctx.void_().ast;
    }

    @Override
    public void exitTypeAnnotations(TypeAnnotationsContext ctx) {
        TypeNode Type = ctx.type().ast;
        List<AnnotationNode> Annotations = ctx.annotations().ast;
        TypeNode tn = Type;
        tn = (TypeNode) ((X10Ext) tn.ext()).annotations((List<AnnotationNode>) Annotations);
        ctx.ast = (TypeNode) tn.position(pos(ctx));
    }

    @Override
    public void exitFunctionType(FunctionTypeContext ctx) {
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        List<Formal> FormalParameterListopt = ctx.formalParameterList().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        TypeNode Type = ctx.type().ast;
        ctx.ast = nf.FunctionTypeNode(pos(ctx), TypeParametersopt, FormalParameterListopt, WhereClauseopt, Type, OBSOLETE_Offersopt);
    }

    @Override
    public void exitClassType(ClassTypeContext ctx) {
        ctx.ast = ctx.namedType().ast;
    }

    @Override
    public void exitConstrainedType(ConstrainedTypeContext ctx) {
        ctx.ast = ctx.namedType().ast;
    }

    @Override
    public void exitVoid_(Void_Context ctx) {
        ctx.ast = nf.CanonicalTypeNode(pos(ctx), ts.Void());
    }

    @Override
    public void exitSimpleNamedType0(SimpleNamedType0Context ctx) {
        ParsedName TypeName = ctx.typeName().ast;
        ctx.ast = (AmbTypeNode) TypeName.toType();
    }

    @Override
    public void exitSimpleNamedType1(SimpleNamedType1Context ctx) {
        Expr Primary = ctx.primary().ast;
        Id Identifier = ctx.identifier().ast;
        ctx.ast = nf.AmbTypeNode(pos(ctx), Primary, Identifier);
    }

    @Override
    public void exitSimpleNamedType2(SimpleNamedType2Context ctx) {
        AmbTypeNode SimpleNamedType = ctx.simpleNamedType().ast;
        List<TypeNode> TypeArguments = ctx.typeArgumentsopt().ast;
        List<Expr> Arguments = ctx.argumentsopt().ast;
        TypeNode qualifier;
        if (ctx.depParameters() == null) {
            qualifier = nf.AmbMacroTypeNode(pos(ctx), SimpleNamedType.prefix(), SimpleNamedType.name(), TypeArguments, Arguments);
        } else {
            DepParameterExpr DepParameters = ctx.depParameters().ast;
            qualifier = nf.AmbDepTypeNode(pos(ctx), SimpleNamedType.prefix(), SimpleNamedType.name(), TypeArguments, Arguments, DepParameters);
        }
        Id Identifier = ctx.identifier().ast;
        ctx.ast = nf.AmbTypeNode(pos(ctx), qualifier, Identifier);
    }

    @Override
    public void exitParameterizedNamedType0(ParameterizedNamedType0Context ctx) {
        AmbTypeNode SimpleNamedType = ctx.simpleNamedType().ast;
        TypedList<TypeNode> typeArguments = new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false);
        List<Expr> Arguments = ctx.arguments().ast;
        AmbMacroTypeNode type = nf.AmbMacroTypeNode(pos(ctx), SimpleNamedType.prefix(), SimpleNamedType.name(), typeArguments, Arguments);
        ctx.ast = type;
    }

    @Override
    public void exitParameterizedNamedType1(ParameterizedNamedType1Context ctx) {
        AmbTypeNode SimpleNamedType = ctx.simpleNamedType().ast;
        List<TypeNode> TypeArguments = ctx.typeArguments().ast;
        TypedList<Expr> Arguments = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
        AmbMacroTypeNode type = nf.AmbMacroTypeNode(pos(ctx), SimpleNamedType.prefix(), SimpleNamedType.name(), TypeArguments, Arguments);
        ctx.ast = type;
    }

    @Override
    public void exitParameterizedNamedType2(ParameterizedNamedType2Context ctx) {
        AmbTypeNode SimpleNamedType = ctx.simpleNamedType().ast;
        List<TypeNode> TypeArguments = ctx.typeArguments().ast;
        List<Expr> Arguments = ctx.arguments().ast;
        AmbMacroTypeNode type = nf.AmbMacroTypeNode(pos(ctx), SimpleNamedType.prefix(), SimpleNamedType.name(), TypeArguments, Arguments);
        ctx.ast = type;
    }

    @Override
    public void exitDepNamedType0(DepNamedType0Context ctx) {
        AmbTypeNode SimpleNamedType = ctx.simpleNamedType().ast;
        TypedList<TypeNode> TypeArguments = new TypedList<TypeNode>(new LinkedList<TypeNode>(), TypeNode.class, false);
        TypedList<Expr> Arguments = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
        DepParameterExpr DepParameters = ctx.depParameters().ast;
        TypeNode type = nf.AmbDepTypeNode(pos(ctx), SimpleNamedType.prefix(), SimpleNamedType.name(), TypeArguments, Arguments, DepParameters);
        ctx.ast = type;
    }

    @Override
    public void exitDepNamedType1(DepNamedType1Context ctx) {
        AmbMacroTypeNode ParameterizedNamedType = ctx.parameterizedNamedType().ast;
        DepParameterExpr DepParameters = ctx.depParameters().ast;
        TypeNode type = nf.AmbDepTypeNode(pos(ctx), ParameterizedNamedType, DepParameters);
        ctx.ast = type;
    }

    @Override
    public void exitNamedTypeNoConstraints0(NamedTypeNoConstraints0Context ctx) {
        ctx.ast = ctx.simpleNamedType().ast;
    }

    @Override
    public void exitNamedTypeNoConstraints1(NamedTypeNoConstraints1Context ctx) {
        ctx.ast = ctx.parameterizedNamedType().ast;
    }

    @Override
    public void exitNamedType0(NamedType0Context ctx) {
        ctx.ast = ctx.namedTypeNoConstraints().ast;
    }

    @Override
    public void exitNamedType1(NamedType1Context ctx) {
        ctx.ast = ctx.depNamedType().ast;
    }

    @Override
    public void exitDepParameters(DepParametersContext ctx) {
        List<Formal> FUTURE_ExistentialListopt = new ArrayList<Formal>();
        List<Expr> ConstraintConjunctionopt = ctx.constraintConjunctionopt().ast;
        ctx.ast = nf.DepParameterExpr(pos(ctx), FUTURE_ExistentialListopt, ConstraintConjunctionopt);
    }

    @Override
    public void exitTypeParamsWithVarianceopt(TypeParamsWithVarianceoptContext ctx) {
        if (ctx.typeParamWithVarianceList() == null) {
            ctx.ast = new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false);
        } else {
            ctx.ast = ctx.typeParamWithVarianceList().ast;
        }
    }

    @Override
    public void exitTypeParametersopt(TypeParametersoptContext ctx) {
        if (ctx.typeParameterList() == null) {
            ctx.ast = new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false);
        } else {
            ctx.ast = ctx.typeParameterList().ast;
        }
    }

    @Override
    public void exitFormalParameters(FormalParametersContext ctx) {
        ctx.ast = ctx.formalParameterList().ast;
    }

    @Override
    public void exitConstraintConjunctionopt(ConstraintConjunctionoptContext ctx) {
        List<Expr> l = new ArrayList<Expr>();
        for (ExpressionContext e : ctx.expression()) {
            l.add(e.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitHasZeroConstraint(HasZeroConstraintContext ctx) {
        TypeNode t1 = ctx.type().ast;
        ctx.ast = nf.HasZeroTest(pos(ctx), t1);
    }

    @Override
    public void exitIsRefConstraint(IsRefConstraintContext ctx) {
        TypeNode t1 = ctx.type().ast;
        ctx.ast = nf.IsRefTest(pos(ctx), t1);
    }

    @Override
    public void exitSubtypeConstraint0(SubtypeConstraint0Context ctx) {
        TypeNode t1 = ctx.t1.ast;
        TypeNode t2 = ctx.t2.ast;
        ctx.ast = nf.SubtypeTest(pos(ctx), t1, t2, false);
    }

    @Override
    public void exitSubtypeConstraint1(SubtypeConstraint1Context ctx) {
        TypeNode t1 = ctx.t1.ast;
        TypeNode t2 = ctx.t2.ast;
        ctx.ast = nf.SubtypeTest(pos(ctx), t2, t1, false);
    }

    @Override
    public void exitWhereClauseopt(WhereClauseoptContext ctx) {
        if (ctx.depParameters() == null) {
            ctx.ast = null;
        } else {
            DepParameterExpr DepParameters = ctx.depParameters().ast;
            ctx.ast = DepParameters;
        }
    }

    @Override
    public void exitClassDeclaration(ClassDeclarationContext ctx) {
        List<Modifier> Modifiersopt = ctx.modifiersopt().ast;
        Id Identifier = ctx.identifier().ast;
        List<TypeParamNode> TypeParamsWithVarianceopt = ctx.typeParamsWithVarianceopt().ast;
        List<PropertyDecl> Propertiesopt = ctx.propertiesopt().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode Superopt = ctx.superExtendsopt().ast;
        List<TypeNode> Interfacesopt = ctx.interfacesopt().ast;
        ClassBody ClassBody = ctx.classBody().ast;
        List<Node> modifiers = checkClassModifiers(Modifiersopt);
        checkTypeName(Identifier);
        List<TypeParamNode> TypeParametersopt = TypeParamsWithVarianceopt;
        List<PropertyDecl> props = Propertiesopt;
        DepParameterExpr ci = WhereClauseopt;
        FlagsNode f = extractFlags(modifiers);
        List<AnnotationNode> annotations = extractAnnotations(modifiers);
        ClassDecl cd = nf.X10ClassDecl(pos(ctx), f, Identifier, TypeParametersopt, props, ci, Superopt, Interfacesopt, ClassBody);
        cd = (ClassDecl) ((X10Ext) cd.ext()).annotations(annotations);
        ctx.ast = cd;
    }

    @Override
    public void exitStructDeclaration(StructDeclarationContext ctx) {
        List<Modifier> Modifiersopt = ctx.modifiersopt().ast;
        Id Identifier = ctx.identifier().ast;
        List<TypeParamNode> TypeParamsWithVarianceopt = ctx.typeParamsWithVarianceopt().ast;
        List<PropertyDecl> Propertiesopt = ctx.propertiesopt().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        List<TypeNode> Interfacesopt = ctx.interfacesopt().ast;
        ClassBody ClassBody = ctx.classBody().ast;
        List<Node> modifiers = checkClassModifiers(Modifiersopt);
        checkTypeName(Identifier);
        List<TypeParamNode> TypeParametersopt = TypeParamsWithVarianceopt;
        List<PropertyDecl> props = Propertiesopt;
        DepParameterExpr ci = WhereClauseopt;
        ClassDecl cd = nf.X10ClassDecl(pos(ctx), extractFlags(modifiers, Flags.STRUCT), Identifier, TypeParametersopt, props, ci, null, Interfacesopt, ClassBody);
        cd = (ClassDecl) ((X10Ext) cd.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = cd;
    }

    @Override
    public void exitConstructorDeclaration(ConstructorDeclarationContext ctx) {
        List<Modifier> Modifiersopt = ctx.modifiersopt().ast;
        List<TypeParamNode> TypeParametersopt = ctx.typeParametersopt().ast;
        List<Formal> FormalParameters = ctx.formalParameters().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        List<TypeNode> Throwsopt = ctx.throwsopt().ast;
        Block ConstructorBody = ctx.constructorBody().ast;
        List<Node> modifiers = checkConstructorModifiers(Modifiersopt);
        ConstructorDecl cd = nf.X10ConstructorDecl(pos(ctx), extractFlags(modifiers), nf.Id(pos(ctx.id), TypeSystem.CONSTRUCTOR_NAME), HasResultTypeopt, TypeParametersopt,
                FormalParameters, WhereClauseopt, OBSOLETE_Offersopt, Throwsopt, ConstructorBody);
        cd = (ConstructorDecl) ((X10Ext) cd.ext()).annotations(extractAnnotations(modifiers));
        ctx.ast = cd;
    }

    @Override
    public void exitSuperExtendsopt(SuperExtendsoptContext ctx) {
        if (ctx.classType() == null) {
            ctx.ast = null;
        } else {
            ctx.ast = ctx.classType().ast;
        }
    }

    @Override
    public void exitVarKeyword0(VarKeyword0Context ctx) {
        ctx.ast = Collections.singletonList(nf.FlagsNode(pos(ctx), Flags.FINAL));
    }

    @Override
    public void exitVarKeyword1(VarKeyword1Context ctx) {
        ctx.ast = Collections.singletonList(nf.FlagsNode(pos(ctx), Flags.NONE));
    }

    @Override
    public void exitFieldDeclaration(FieldDeclarationContext ctx) {
        List<Modifier> Modifiersopt = ctx.modifiersopt().ast;
        List<FlagsNode> FieldKeyword = ctx.varKeyword() == null ? Collections.singletonList(nf.FlagsNode(pos(ctx), Flags.FINAL)) : ctx.varKeyword().ast;
        List<Object[]> FieldDeclarators = ctx.fieldDeclarators().ast;
        List<Node> modifiers = checkFieldModifiers(Modifiersopt);
        FlagsNode fn = extractFlags(modifiers, FieldKeyword);
        List<ClassMember> l = new TypedList<ClassMember>(new LinkedList<ClassMember>(), ClassMember.class, false);
        for (Object[] o : FieldDeclarators) {
            Position pos = (Position) o[0];
            Id name = (Id) o[1];
            if (name == null)
                name = nf.Id(pos, Name.makeFresh());
            TypeNode type = (TypeNode) o[3];
            if (type == null)
                type = nf.UnknownTypeNode(name.position().markCompilerGenerated());
            Expr init = (Expr) o[4];
            FieldDecl fd = nf.FieldDecl(pos, fn, type, name, init);
            fd = (FieldDecl) ((X10Ext) fd.ext()).annotations(extractAnnotations(modifiers));
            fd = (FieldDecl) ((X10Ext) fd.ext()).setComment(comment(pos(ctx))); // TODO
            l.add(fd);
        }
        ctx.ast = l;
    }

    @Override
    public void exitStatement0(Statement0Context ctx) {
        ctx.ast = ctx.annotationStatement().ast;
    }

    @Override
    public void exitStatement1(Statement1Context ctx) {
        ctx.ast = ctx.expressionStatement().ast;
    }

    @Override
    public void exitAnnotationStatement(AnnotationStatementContext ctx) {
        List<AnnotationNode> Annotationsopt = ctx.annotationsopt().ast;
        Stmt NonExpressionStatement = ctx.nonExpressionStatement().ast;
        if (NonExpressionStatement.ext() instanceof X10Ext) {
            NonExpressionStatement = (Stmt) ((X10Ext) NonExpressionStatement.ext()).annotations(Annotationsopt);
        }
        ctx.ast = (Stmt) NonExpressionStatement.position(pos(ctx));
    }

    @Override
    public void exitNonExpressionStatemen0(NonExpressionStatemen0Context ctx) {
        ctx.ast = ctx.block().ast;
    }

    @Override
    public void exitNonExpressionStatemen1(NonExpressionStatemen1Context ctx) {
        ctx.ast = ctx.emptyStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen2(NonExpressionStatemen2Context ctx) {
        ctx.ast = ctx.assertStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen3(NonExpressionStatemen3Context ctx) {
        ctx.ast = ctx.switchStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen4(NonExpressionStatemen4Context ctx) {
        ctx.ast = ctx.doStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen5(NonExpressionStatemen5Context ctx) {
        ctx.ast = ctx.breakStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen6(NonExpressionStatemen6Context ctx) {
        ctx.ast = ctx.continueStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen7(NonExpressionStatemen7Context ctx) {
        ctx.ast = ctx.returnStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen8(NonExpressionStatemen8Context ctx) {
        ctx.ast = ctx.throwStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen9(NonExpressionStatemen9Context ctx) {
        ctx.ast = ctx.tryStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen10(NonExpressionStatemen10Context ctx) {
        ctx.ast = ctx.labeledStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen11(NonExpressionStatemen11Context ctx) {
        ctx.ast = ctx.ifThenStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen12(NonExpressionStatemen12Context ctx) {
        ctx.ast = ctx.ifThenElseStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen13(NonExpressionStatemen13Context ctx) {
        ctx.ast = ctx.whileStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen14(NonExpressionStatemen14Context ctx) {
        ctx.ast = ctx.forStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen15(NonExpressionStatemen15Context ctx) {
        ctx.ast = ctx.asyncStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen16(NonExpressionStatemen16Context ctx) {
        ctx.ast = ctx.atStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen17(NonExpressionStatemen17Context ctx) {
        ctx.ast = ctx.atomicStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen18(NonExpressionStatemen18Context ctx) {
        ctx.ast = ctx.whenStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen19(NonExpressionStatemen19Context ctx) {
        ctx.ast = ctx.atEachStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen20(NonExpressionStatemen20Context ctx) {
        ctx.ast = ctx.finishStatement().ast;
    }

    @Override
    public void exitNonExpressionStatemen21(NonExpressionStatemen21Context ctx) {
        ctx.ast = ctx.assignPropertyCall().ast;
    }

    @Override
    public void exitNonExpressionStatemen22(NonExpressionStatemen22Context ctx) {
        ctx.ast = ctx.oBSOLETE_OfferStatement().ast;
    }

    @Override
    public void exitOBSOLETE_OfferStatement(OBSOLETE_OfferStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        ctx.ast = nf.Offer(pos(ctx), Expression);
    }

    @Override
    public void exitIfThenStatement(IfThenStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.If(pos(ctx), Expression, Statement);
    }

    @Override
    public void exitIfThenElseStatement(IfThenElseStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        Stmt s1 = ctx.s1.ast;
        Stmt s2 = ctx.s2.ast;
        ctx.ast = nf.If(pos(ctx), Expression, s1, s2);
    }

    @Override
    public void exitEmptyStatement(EmptyStatementContext ctx) {
        ctx.ast = nf.Empty(pos(ctx));
    }

    @Override
    public void exitLabeledStatement(LabeledStatementContext ctx) {
        Id Identifier = ctx.identifier().ast;
        Stmt LoopStatement = ctx.loopStatement().ast;
        ctx.ast = nf.Labeled(pos(ctx), Identifier, LoopStatement);
    }

    @Override
    public void exitLoopStatement0(LoopStatement0Context ctx) {
        ctx.ast = ctx.forStatement().ast;
    }

    @Override
    public void exitLoopStatement1(LoopStatement1Context ctx) {
        ctx.ast = ctx.whileStatement().ast;
    }

    @Override
    public void exitLoopStatement2(LoopStatement2Context ctx) {
        ctx.ast = ctx.doStatement().ast;
    }

    @Override
    public void exitLoopStatement3(LoopStatement3Context ctx) {
        ctx.ast = ctx.atEachStatement().ast;
    }

    @Override
    public void exitExpressionStatement(ExpressionStatementContext ctx) {
        Expr StatementExpression = ctx.expression().ast;
        ctx.ast = nf.Eval(pos(ctx), StatementExpression);
    }

    @Override
    public void exitAssertStatement0(AssertStatement0Context ctx) {
        Expr Expression = ctx.expression().ast;
        ctx.ast = nf.Assert(pos(ctx), Expression);
    }

    @Override
    public void exitAssertStatement1(AssertStatement1Context ctx) {
        Expr expr1 = ctx.e1.ast;
        Expr expr2 = ctx.e2.ast;
        ctx.ast = nf.Assert(pos(ctx), expr1, expr2);
    }

    @Override
    public void exitSwitchStatement(SwitchStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        List<SwitchElement> SwitchBlock = ctx.switchBlock().ast;
        ctx.ast = nf.Switch(pos(ctx), Expression, SwitchBlock);
    }

    @Override
    public void exitSwitchBlock(SwitchBlockContext ctx) {
        List<SwitchElement> SwitchBlockStatementGroupsopt = ctx.switchBlockStatementGroupsopt().ast;
        List<Case> SwitchLabelsopt = ctx.switchLabelsopt().ast;
        SwitchBlockStatementGroupsopt.addAll(SwitchLabelsopt);
        ctx.ast = SwitchBlockStatementGroupsopt;
    }

    @Override
    public void exitSwitchBlockStatementGroup(SwitchBlockStatementGroupContext ctx) {
        List<Case> SwitchLabels = ctx.switchLabels().ast;
        List<Stmt> BlockStatements = ctx.blockStatements().ast;
        List<SwitchElement> l = new TypedList<SwitchElement>(new LinkedList<SwitchElement>(), SwitchElement.class, false);
        l.addAll(SwitchLabels);
        l.add(nf.SwitchBlock(pos(ctx), BlockStatements));
        ctx.ast = l;
    }

    @Override
    public void exitSwitchBlockStatementGroupsopt(SwitchBlockStatementGroupsoptContext ctx) {
        List<SwitchElement> l = new TypedList<SwitchElement>(new LinkedList<SwitchElement>(), SwitchElement.class, false);
        for (SwitchBlockStatementGroupContext e : ctx.switchBlockStatementGroup()) {
            l.addAll(e.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitSwitchLabel0(SwitchLabel0Context ctx) {
        Expr ConstantExpression = ctx.constantExpression().ast;
        ctx.ast = nf.Case(pos(ctx), ConstantExpression);
    }

    @Override
    public void exitSwitchLabel1(SwitchLabel1Context ctx) {
        ctx.ast = nf.Default(pos(ctx));
    }

    @Override
    public void exitSwitchLabelsopt(SwitchLabelsoptContext ctx) {
        if (ctx.switchLabels() == null) {
            ctx.ast = new TypedList<Case>(new LinkedList<Case>(), Case.class, false);
        } else {
            ctx.ast = ctx.switchLabels().ast;
        }
    }

    @Override
    public void exitWhileStatement(WhileStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.While(pos(ctx), Expression, Statement);
    }

    @Override
    public void exitDoStatement(DoStatementContext ctx) {
        Stmt Statement = ctx.statement().ast;
        Expr Expression = ctx.expression().ast;
        ctx.ast = nf.Do(pos(ctx), Statement, Expression);
    }

    @Override
    public void exitForStatement0(ForStatement0Context ctx) {
        ctx.ast = ctx.basicForStatement().ast;
    }

    @Override
    public void exitForStatement1(ForStatement1Context ctx) {
        ctx.ast = ctx.enhancedForStatement().ast;
    }

    @Override
    public void exitBasicForStatement(BasicForStatementContext ctx) {
        List<ForInit> ForInitopt = ctx.forInitopt().ast;
        Expr Expressionopt = ctx.expressionopt().ast;
        List<ForUpdate> ForUpdateopt = ctx.forUpdateopt().ast;
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.For(pos(ctx), ForInitopt, Expressionopt, ForUpdateopt, Statement);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void exitForInit0(ForInit0Context ctx) {
        ctx.ast = (List<ForInit>) ctx.statementExpressionList().ast;
    }

    @Override
    public void exitForInit1(ForInit1Context ctx) {
        List<LocalDecl> LocalVariableDeclaration = ctx.localVariableDeclaration().ast;
        List<ForInit> l = new TypedList<ForInit>(new LinkedList<ForInit>(), ForInit.class, false);
        l.addAll(LocalVariableDeclaration);
        ctx.ast = l;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void exitForUpdate(ForUpdateContext ctx) {
        ctx.ast = (List<ForUpdate>) ctx.statementExpressionList().ast;
    }

    @Override
    public void exitStatementExpressionList(StatementExpressionListContext ctx) {
        List<Eval> l = new TypedList<Eval>(new LinkedList<Eval>(), Eval.class, false);
        for (ExpressionContext e : ctx.expression()) {
            l.add(nf.Eval(pos(e), e.ast));
        }
        ctx.ast = l;
    }

    @Override
    public void exitBreakStatement(BreakStatementContext ctx) {
        Id Identifieropt = ctx.identifieropt().ast;
        ctx.ast = nf.Break(pos(ctx), Identifieropt);
    }

    @Override
    public void exitContinueStatement(ContinueStatementContext ctx) {
        Id Identifieropt = ctx.identifieropt().ast;
        ctx.ast = nf.Continue(pos(ctx), Identifieropt);
    }

    @Override
    public void exitReturnStatement(ReturnStatementContext ctx) {
        Expr Expressionopt = ctx.expressionopt().ast;
        ctx.ast = nf.Return(pos(ctx), Expressionopt);
    }

    @Override
    public void exitThrowStatement(ThrowStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        ctx.ast = nf.Throw(pos(ctx), Expression);
    }

    @Override
    public void exitTryStatement0(TryStatement0Context ctx) {
        Block Block = ctx.block().ast;
        List<Catch> Catches = ctx.catches().ast;
        ctx.ast = nf.Try(pos(ctx), Block, Catches);
    }

    @Override
    public void exitTryStatement1(TryStatement1Context ctx) {
        Block Block = ctx.block().ast;
        List<Catch> Catchesopt = ctx.catchesopt().ast;
        Block Finally = ctx.finallyBlock().ast;
        ctx.ast = nf.Try(pos(ctx), Block, Catchesopt, Finally);
    }

    @Override
    public void exitCatches(CatchesContext ctx) {
        List<Catch> l = new TypedList<Catch>(new LinkedList<Catch>(), Catch.class, false);
        for (CatchClauseContext CatchClause : ctx.catchClause()) {
            l.add(CatchClause.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitCatchClause(CatchClauseContext ctx) {
        X10Formal FormalParameter = ctx.formalParameter().ast;
        Block Block = ctx.block().ast;
        ctx.ast = nf.Catch(pos(ctx), FormalParameter, Block);
    }

    @Override
    public void exitFinallyBlock(FinallyBlockContext ctx) {
        Block Block = ctx.block().ast;
        ctx.ast = Block;
    }

    @Override
    public void exitClockedClauseopt(ClockedClauseoptContext ctx) {
        List<Expr> Arguments;
        if (ctx.arguments() == null) {
            Arguments = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
        } else {
            Arguments = ctx.arguments().ast;
        }
        ctx.ast = Arguments;
    }

    @Override
    public void exitAsyncStatement0(AsyncStatement0Context ctx) {
        List<Expr> ClockedClauseopt = ctx.clockedClauseopt().ast;
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.Async(pos(ctx), ClockedClauseopt, Statement);
    }

    @Override
    public void exitAsyncStatement1(AsyncStatement1Context ctx) {
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.Async(pos(ctx), Statement, true);
    }

    @Override
    public void exitAtStatement(AtStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.AtStmt(pos(ctx), Expression, Statement);
    }

    @Override
    public void exitAtomicStatement(AtomicStatementContext ctx) {
        Stmt Statement = ctx.statement().ast;
        // Position of here might be wrong
        ctx.ast = nf.Atomic(pos(ctx), nf.Here(pos(ctx)), Statement);
    }

    @Override
    public void exitWhenStatement(WhenStatementContext ctx) {
        Expr Expression = ctx.expression().ast;
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.When(pos(ctx), Expression, Statement);
    }

    @Override
    public void exitAtEachStatement0(AtEachStatement0Context ctx) {
        X10Formal LoopIndex = ctx.loopIndex().ast;
        Expr Expression = ctx.expression().ast;
        List<Expr> ClockedClauseopt = ctx.clockedClauseopt().ast;
        Stmt Statement = ctx.statement().ast;
        FlagsNode fn = LoopIndex.flags();
        if (!fn.flags().isFinal()) {
            err.syntaxError("Enhanced ateach loop may not have var loop index. " + LoopIndex, LoopIndex.position());
            fn = fn.flags(fn.flags().Final());
            LoopIndex = LoopIndex.flags(fn);
        }
        ctx.ast = nf.AtEach(pos(ctx), LoopIndex, Expression, ClockedClauseopt, Statement);
    }

    @Override
    public void exitAtEachStatement1(AtEachStatement1Context ctx) {
        Expr Expression = ctx.expression().ast;
        Stmt Statement = ctx.statement().ast;
        Id name = nf.Id(pos(ctx), Name.makeFresh());
        TypeNode type = nf.UnknownTypeNode(pos(ctx).markCompilerGenerated());
        X10Formal LoopIndex = nf.X10Formal(pos(ctx), nf.FlagsNode(pos(ctx), Flags.FINAL), type, name, null, true);
        TypedList<Expr> ClockedClauseopt = new TypedList<Expr>(new LinkedList<Expr>(), Expr.class, false);
        ctx.ast = nf.AtEach(pos(ctx), LoopIndex, Expression, ClockedClauseopt, Statement);
    }

    @Override
    public void exitEnhancedForStatement0(EnhancedForStatement0Context ctx) {
        X10Formal LoopIndex = ctx.loopIndex().ast;
        Expr Expression = ctx.expression().ast;
        Stmt Statement = ctx.statement().ast;
        FlagsNode fn = LoopIndex.flags();
        if (!fn.flags().isFinal()) {
            err.syntaxError("Enhanced for loop may not have var loop index. " + LoopIndex, LoopIndex.position());
            fn = fn.flags(fn.flags().Final());
            LoopIndex = LoopIndex.flags(fn);
        }
        ctx.ast = nf.ForLoop(pos(ctx), LoopIndex, Expression, Statement);
    }

    @Override
    public void exitEnhancedForStatement1(EnhancedForStatement1Context ctx) {
        Expr Expression = ctx.expression().ast;
        Stmt Statement = ctx.statement().ast;
        Id name = nf.Id(pos(ctx), Name.makeFresh());
        TypeNode type = nf.UnknownTypeNode(pos(ctx).markCompilerGenerated());
        Formal LoopIndex = nf.X10Formal(pos(ctx), nf.FlagsNode(pos(ctx), Flags.FINAL), type, name, null, true);
        ctx.ast = nf.ForLoop(pos(ctx), LoopIndex, Expression, Statement);
    }

    @Override
    public void exitFinishStatement0(FinishStatement0Context ctx) {
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.Finish(pos(ctx), Statement, false);
    }

    @Override
    public void exitFinishStatement1(FinishStatement1Context ctx) {
        Stmt Statement = ctx.statement().ast;
        ctx.ast = nf.Finish(pos(ctx), Statement, true);
    }

    @Override
    public void exitCastExpression0(CastExpression0Context ctx) {
        ctx.ast = ctx.primary().ast;
    }

    @Override
    public void exitCastExpression1(CastExpression1Context ctx) {
        ParsedName ExpressionName = ctx.expressionName().ast;
        ctx.ast = ExpressionName.toExpr();
    }

    @Override
    public void exitCastExpression2(CastExpression2Context ctx) {
        Expr CastExpression = ctx.castExpression().ast;
        TypeNode Type = ctx.type().ast;
        ctx.ast = nf.X10Cast(pos(ctx), Type, CastExpression);
    }

    @Override
    public void exitTypeParamWithVarianceList0(TypeParamWithVarianceList0Context ctx) {
        TypeParamNode TypeParameter = ctx.typeParameter().ast;
        List<TypeParamNode> l = new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false);
        l.add(TypeParameter);
        ctx.ast = l;
    }

    @Override
    public void exitTypeParamWithVarianceList1(TypeParamWithVarianceList1Context ctx) {
        TypeParamNode OBSOLETE_TypeParamWithVariance = ctx.oBSOLETE_TypeParamWithVariance().ast;
        List<TypeParamNode> l = new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false);
        l.add(OBSOLETE_TypeParamWithVariance);
        ctx.ast = l;
    }

    @Override
    public void exitTypeParamWithVarianceList2(TypeParamWithVarianceList2Context ctx) {
        List<TypeParamNode> TypeParamWithVarianceList = ctx.typeParamWithVarianceList().ast;
        TypeParamNode TypeParameter = ctx.typeParameter().ast;
        TypeParamWithVarianceList.add(TypeParameter);
        ctx.ast = TypeParamWithVarianceList;
    }

    @Override
    public void exitTypeParamWithVarianceList3(TypeParamWithVarianceList3Context ctx) {
        List<TypeParamNode> TypeParamWithVarianceList = ctx.typeParamWithVarianceList().ast;
        TypeParamNode OBSOLETE_TypeParamWithVariance = ctx.oBSOLETE_TypeParamWithVariance().ast;
        TypeParamWithVarianceList.add(OBSOLETE_TypeParamWithVariance);
        ctx.ast = TypeParamWithVarianceList;
    }

    @Override
    public void exitTypeParameterList(TypeParameterListContext ctx) {
        List<TypeParamNode> l = new TypedList<TypeParamNode>(new LinkedList<TypeParamNode>(), TypeParamNode.class, false);
        for (TypeParameterContext TypeParameter : ctx.typeParameter()) {
            l.add(TypeParameter.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitOBSOLETE_TypeParamWithVariance0(OBSOLETE_TypeParamWithVariance0Context ctx) {
        TypeParamNode TypeParameter = ctx.typeParameter().ast;
        err.syntaxError("Covariance is no longer supported.", pos(ctx));
        ctx.ast = (TypeParamNode) TypeParameter.variance(ParameterType.Variance.COVARIANT).position(pos(ctx));
    }

    @Override
    public void exitOBSOLETE_TypeParamWithVariance1(OBSOLETE_TypeParamWithVariance1Context ctx) {
        TypeParamNode TypeParameter = ctx.typeParameter().ast;
        err.syntaxError("Contravariance is no longer supported.", pos(ctx));
        ctx.ast = (TypeParamNode) TypeParameter.variance(ParameterType.Variance.CONTRAVARIANT).position(pos(ctx));
    }

    @Override
    public void exitTypeParameter(TypeParameterContext ctx) {
        Id Identifier = ctx.identifier().ast;
        ctx.ast = nf.TypeParamNode(pos(ctx), Identifier);
    }


    @Override
    public void exitClosureExpression(ClosureExpressionContext ctx) {
        List<Formal> FormalParameters = ctx.formalParameters().ast;
        DepParameterExpr WhereClauseopt = ctx.whereClauseopt().ast;
        TypeNode HasResultTypeopt = ctx.hasResultTypeopt().ast;
        TypeNode HasResultType = HasResultTypeopt == null ? nf.UnknownTypeNode(Position.COMPILER_GENERATED) : HasResultTypeopt;
        TypeNode OBSOLETE_Offersopt = ctx.oBSOLETE_Offersopt().ast;
        Block ClosureBody = ctx.closureBody().ast;
        ctx.ast = nf.Closure(pos(ctx), FormalParameters, WhereClauseopt, HasResultType, ClosureBody);
    }

    @Override
    public void exitLastExpression(LastExpressionContext ctx) {
        Expr Expression = ctx.expression().ast;
        ctx.ast = nf.X10Return(pos(ctx), Expression, true);
    }

    @Override
    public void exitClosureBody0(ClosureBody0Context ctx) {
        Expr ConditionalExpression = ctx.expression().ast;
        ctx.ast = nf.Block(pos(ctx), nf.X10Return(pos(ctx), ConditionalExpression, true));
    }

    @Override
    public void exitClosureBody1(ClosureBody1Context ctx) {
        List<AnnotationNode> Annotationsopt = ctx.annotationsopt().ast;
        List<Stmt> BlockStatementsopt = ctx.blockStatements().ast;
        Stmt LastExpression = ctx.lastExpression().ast;
        List<Stmt> l = new ArrayList<Stmt>();
        l.addAll(BlockStatementsopt);
        l.add(LastExpression);
        Block b = nf.Block(pos(ctx), l);
        b = (Block) ((X10Ext) b.ext()).annotations(Annotationsopt);
        ctx.ast = b;
    }

    @Override
    public void exitClosureBody2(ClosureBody2Context ctx) {
        List<AnnotationNode> Annotationsopt = ctx.annotationsopt().ast;
        Block Block = ctx.block().ast;
        Block b = Block;
        b = (Block) ((X10Ext) b.ext()).annotations(Annotationsopt);
        ctx.ast = (polyglot.ast.Block) b.position(pos(ctx));
    }

    @Override
    public void exitAtExpression(AtExpressionContext ctx) {
        List<AnnotationNode> Annotationsopt = ctx.annotationsopt().ast;
        Expr Expression = ctx.expression().ast;
        Block ClosureBody = ctx.closureBody().ast;
        AtExpr r = nf.AtExpr(pos(ctx), Expression, ClosureBody);
        r = (AtExpr) ((X10Ext) r.ext()).annotations(Annotationsopt);
        ctx.ast = r;
    }

    @Override
    public void exitOBSOLETE_FinishExpression(OBSOLETE_FinishExpressionContext ctx) {
        Expr Expression = ctx.expression().ast;
        Block Block = ctx.block().ast;
        ctx.ast = nf.FinishExpr(pos(ctx), Expression, Block);
    }

    @Override
    public void exitTypeName0(TypeName0Context ctx) {
        Id Identifier = ctx.identifier().ast;
        ctx.ast = new ParsedName(nf, ts, pos(ctx), Identifier);
    }

    @Override
    public void exitTypeName1(TypeName1Context ctx) {
        ParsedName TypeName = ctx.typeName().ast;
        Id Identifier = ctx.identifier().ast;
        // Position might be wrong
        ctx.ast = new ParsedName(nf, ts, pos(ctx), TypeName, Identifier);
    }

    @Override
    public void exitClassName(ClassNameContext ctx) {
        ctx.ast = ctx.typeName().ast;
    }

    @Override
    public void exitTypeArguments(TypeArgumentsContext ctx) {
        List<TypeNode> l = new ArrayList<TypeNode>();
        for (TypeContext Type : ctx.type()) {
            l.add(Type.ast);
        }
        ctx.ast = l;
    }

    @Override
    public void exitPackageName0(PackageName0Context ctx) {
        ctx.ast = new ParsedName(nf, ts, pos(ctx), ctx.identifier().ast);
    }

    @Override
    public void exitPackageName1(PackageName1Context ctx) {
        ctx.ast = new ParsedName(nf, ts, pos(ctx), ctx.packageName().ast, ctx.identifier().ast);
    }
















































}