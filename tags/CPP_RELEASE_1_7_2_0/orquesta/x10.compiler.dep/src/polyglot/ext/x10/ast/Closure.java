/*
 * Created on Feb 26, 2007
 */
package polyglot.ext.x10.ast;

import java.util.List;

import polyglot.ast.CodeBlock;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.TypeNode;
import polyglot.ext.x10.types.ClosureDef;

public interface Closure extends Expr, CodeBlock {
    /** The closure's formal parameters.
     * @return A list of {@link polyglot.ast.Formal Formal}
     */
    List<Formal> formals();

    /** The closure's exception throw types.
     * @return A list of {@link polyglot.ast.TypeNode TypeNode}
     */
    List<TypeNode> throwTypes();

    /**
     * @return the closure's return type
     */
    TypeNode returnType();
    
    ClosureDef closureDef();
}
