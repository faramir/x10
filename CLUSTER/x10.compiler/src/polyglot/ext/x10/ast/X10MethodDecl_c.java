package polyglot.ext.x10.ast;

import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.MethodDecl_c;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.Translator;

/** A representation of a method declaration. Includes an extra field to represent the where clause
 * in the method definition.
 * 
 * @author VijaySaraswat
 *
 */
public class X10MethodDecl_c extends MethodDecl_c {
    // The representation of this( DepParameterExpr ) in the production.
    TypeNode thisClause;
    // The reprsentation of the : Constraint in the parameter list.
    Expr whereClause;
     /*   public X10MethodDecl_c(Position pos, 
                Flags flags, TypeNode returnType,
                String name, List formals, List throwTypes, Block body) {
                super(pos, flags, returnType, name, formals, throwTypes, body);
        }*/
        public X10MethodDecl_c(Position pos, TypeNode thisClause, 
                Flags flags, TypeNode returnType,
                String name, List formals, Expr e, List throwTypes, Block body) {
        super(pos, flags, returnType, name, formals, throwTypes, body);
        whereClause = e;
}
        public void translate(CodeWriter w, Translator tr) {
                Context c = tr.context();
                Flags flags = flags();

                if (c.currentClass().flags().isInterface()) {
                    flags = flags.clearPublic();
                    flags = flags.clearAbstract();
                }
                this.del().prettyPrint(w, tr);
        }
}