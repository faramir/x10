/*
 * Created on Sep 29, 2004
 */
package polyglot.ext.x10.ast;

import polyglot.ast.Stmt;
import polyglot.ast.Block;

/**
 * @author Christian Grothoff
 */
public interface Atomic extends Stmt, RemoteActivityInvocation {
    
    /** Set the Atomic's body */
    Atomic body(Block body);

    /** Get the body of the Atomic. */
    Block body();
}
