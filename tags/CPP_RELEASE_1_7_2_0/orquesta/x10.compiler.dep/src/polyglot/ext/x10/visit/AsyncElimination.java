/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Language.
 *
 */
/*
 * Created on Mar 20, 2005
 *
 */
package polyglot.ext.x10.visit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.Block;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Assign;
import polyglot.ast.Receiver;
import polyglot.ast.Call;
import polyglot.ast.ProcedureCall;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.visit.NodeVisitor;
import polyglot.ext.x10.ast.Async;
import polyglot.ext.x10.ast.Future;

/**
 * The <code>AsyncElimination</code> runs over the AST and 
 * 'inlines' the Async blocks in the current activity in case 
 * the body of the async performs only a simple remote read / or write.
 * 
 * This optimization can render the results of the sampling incorrect
 * because remote accesses may disappear in the code and look like 
 * local accesses! 
 *
 * Note that this optimization is not legal in real X10 programs!
 * We use it just for the simple shared memory variant of the X10
 * prototype system to reduce the overheads of the very general 
 * implementation of async and future. 
 */
public class AsyncElimination extends NodeVisitor {
    private final boolean DEBUG_ = false;
    
    public Node leave(Node old, Node n, NodeVisitor v) {
        Node ret = n;

        // If we have a labeled block consisting of just one statement, then
        // flatten the block and label the statement instead. We also flatten
        // labeled blocks when there is no reference to the label within the
        // block.
        if (n instanceof Async) {
            Async as = (Async) n;
            Stmt as_body = as.body(); 
            Stmt simple_stmt = checkIfSimpleBlock_(as_body);
            if (simple_stmt != null && isOptimizableStmt_(simple_stmt)) {
                // replace the whole sync stmt with the simple smt;
                if (DEBUG_)
                    System.out.println("AsyncElimination - for async=" + n);
                ret = simple_stmt;
            }
        } else if (n instanceof Call) { // Futures that are forced
            Call c = (Call) n;
            List args = c.arguments();
            Receiver r = c.target();
            if ("force".equals(c.id().id()) && args.size() == 0 && r instanceof Future) {
                Future f = (Future) r;
                Expr f_expr = f.body();
                if (isOptimizableExpr_(f_expr)) {
                    if (DEBUG_)
                        System.out.println("AsyncElimination - for future=" + n);
                    ret = f_expr;
                }
            }
        }
        return ret;
    }

    /**
     * returns the single statement that may be replaced for the async 
     * */
    private Stmt checkIfSimpleBlock_(Stmt s) {
        Stmt the_one_stmt = null;
        if (s instanceof Block) {
            Block b = (Block) s;
            List l = b.statements();
            if (l.size() == 1) 
                the_one_stmt = (Stmt) l.get(0);
        } else {
            the_one_stmt = s;
        }
        return the_one_stmt;
    }
    
    /**
     * The stmt in the body of an async is optimizable if it is 
     * a simple assignment and the rhs meets the criteraia of
     * method isOptimizableExpr_.
     */
    private boolean isOptimizableStmt_(Stmt s) {
        boolean ret = false;
        if (s instanceof Eval) {
            Eval e = (Eval) s;
            Expr e_expr = e.expr();
            if (e_expr instanceof Assign) {
                Assign a_expr = (Assign) e_expr;
                ret = isOptimizableExpr_(a_expr.right());
            }
        } 
        return ret;
    }
    
    /**
     * Traverses an expression and determines if it does not
     *  - invoke another method
     *  - is future
     *  - does not allocate other objects / arrays
     **/
    private boolean isOptimizableExpr_(Expr e) {
        final Set critical = new HashSet();
        e.visit( new NodeVisitor() {
            public Node leave( Node old, Node n, NodeVisitor v) {
                if ( n instanceof ProcedureCall ||
                     n instanceof Future ||
                     n instanceof New ||    // implies call to constructor
                     n instanceof NewArray) // implies execution of initializer
                {
                    critical.add(n);
                }
                return n;
            }
        });        
        return critical.isEmpty();
    }

}
