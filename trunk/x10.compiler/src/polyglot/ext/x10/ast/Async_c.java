/*
 * Created on Oct 5, 2004
 */
package polyglot.ext.x10.ast;

import java.util.List;

import polyglot.ast.Stmt;
import polyglot.ast.Expr;
import polyglot.ast.Term;
import polyglot.ext.jl.ast.Stmt_c;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;

/**
 * @author Christian Grothoff
 */
public class Async_c extends Stmt_c 
    implements Async {

    public Stmt body;
    
    public Expr place; 

    public Async_c(Position p, Expr place, Stmt body) {
        super(p);
        this.place = place;
        this.body = body;
    }
    
    public Async_c(Position p) {
        super(p);
    }
    
    /**
     * Return the first (sub)term performed when evaluating this
     * term.
     */
    public Term entry() {
        // TODO:
        return this;
    }

    /**
     * Visit this term in evaluation order.
     */
    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFG(place, body());
        v.visitCFG(body(), this);
        return succs;
    }
    
    /* (non-Javadoc)
     * @see polyglot.ext.x10.ast.Future#body()
     */
    public Stmt body() {
        return body;
    }

    /** Set the body of the statement. */
    public Async body(Stmt body) {
	Async_c n = (Async_c) copy();
	n.body = body;
	return n;
    }

    /** Get the RemoteActivity's place. */
    public Expr place() {
        return place;
    }
    
    /** Set the RemoteActivity's place. */
    public RemoteActivityInvocation place(Expr place) {
        this.place = place;
        return this;
    }
}
