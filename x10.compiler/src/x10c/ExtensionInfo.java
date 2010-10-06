/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

package x10c;

import java.util.List;

import polyglot.ast.NodeFactory;
import polyglot.frontend.Goal;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.types.TypeSystem;
import x10.visit.SharedBoxer;
import x10c.ast.X10CNodeFactory_c;
import x10c.types.X10CTypeSystem_c;
import x10c.visit.AsyncInitializer;
import x10c.visit.CastRemover;
import x10c.visit.ClosuresToStaticMethods;
import x10c.visit.Desugarer;
import x10c.visit.ExpressionFlattenerForAtExpr;
import x10c.visit.InlineHelper;
import x10c.visit.JavaCaster;
import x10c.visit.RailInLoopOptimizer;
import x10c.visit.VarsBoxer;

public class ExtensionInfo extends x10.ExtensionInfo {
    @Override
    protected Scheduler createScheduler() {
        return new X10CScheduler(this);
    }

    @Override
    protected NodeFactory createNodeFactory() {
        return new X10CNodeFactory_c(this);
    }

    @Override
    protected TypeSystem createTypeSystem() {
        return new X10CTypeSystem_c();
    }

//    public static final boolean PREPARE_FOR_INLINING = x10.Configuration.INLINE_OPTIMIZATIONS;
    public static final boolean PREPARE_FOR_INLINING = true;

    static class X10CScheduler extends X10Scheduler {
        public X10CScheduler(ExtensionInfo extInfo) {
            super(extInfo);
        }

        @Override
        public List<Goal> goals(Job job) {
            List<Goal> goals = super.goals(job);
            ExpressionFlattenerForAtExpr(job).addPrereq(TypeCheckBarrier());
            VarsBoxer(job).addPrereq(ExpressionFlattenerForAtExpr(job));
            Desugarer(job).addPrereq(VarsBoxer(job));
            ClosuresToStaticMethods(job).addPrereq(Desugarer(job));
            JavaCaster(job).addPrereq(ClosuresToStaticMethods(job));
            CastsRemoved(job).addPrereq(JavaCaster(job));
            RailInLoopOptimizer(job).addPrereq(CastsRemoved(job));
//            SharedBoxed(job).addPrereq(RailInLoopOptimizer(job));
            AsyncInitializer(job).addPrereq(RailInLoopOptimizer(job));
            if (PREPARE_FOR_INLINING) {
                InlineHelped(job).addPrereq(AsyncInitializer(job));
            }
            CodeGenerated(job).addPrereq(ExpressionFlattenerForAtExpr(job));
            CodeGenerated(job).addPrereq(VarsBoxer(job));
            CodeGenerated(job).addPrereq(Desugarer(job));
            CodeGenerated(job).addPrereq(ClosuresToStaticMethods(job));
            CodeGenerated(job).addPrereq(JavaCaster(job));
            CodeGenerated(job).addPrereq(CastsRemoved(job));
            CodeGenerated(job).addPrereq(RailInLoopOptimizer(job));
//            CodeGenerated(job).addPrereq(SharedBoxed(job));
            CodeGenerated(job).addPrereq(AsyncInitializer(job));
            if (PREPARE_FOR_INLINING) {
                CodeGenerated(job).addPrereq(InlineHelped(job));
            }
            return goals;
        }
        
        @Override
        public Goal Desugarer(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("Desugarer", job, new Desugarer(job, ts, nf)).intern(this);
        }

        public Goal ClosuresToStaticMethods(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("ClosuresToStaticMethods", job, new ClosuresToStaticMethods(job, ts, nf)).intern(this);
        }

        private Goal RailInLoopOptimizer(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("RailInLoopOptimized", job, new RailInLoopOptimizer(job, ts, nf)).intern(this);
        }
        
        private Goal JavaCaster(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("JavaCasted", job, new JavaCaster(job, ts, nf)).intern(this);
        }

        private Goal CastsRemoved(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("CastsRemoved", job, new CastRemover(job, ts, nf)).intern(this);
        }
        
        private Goal SharedBoxed(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("SharedBoxed", job, new SharedBoxer(job, ts, nf)).intern(this);
        }
        
        private Goal InlineHelped(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("InlineHelped", job, new InlineHelper(job, ts, nf)).intern(this);
        }

        private Goal AsyncInitializer(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("AsyncInitialized", job, new AsyncInitializer(job, ts, nf)).intern(this);
        }
        
        private Goal VarsBoxer(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("VarsBoxed", job, new VarsBoxer(job, ts, nf)).intern(this);
        }
        
        private Goal ExpressionFlattenerForAtExpr(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new ValidatingVisitorGoal("ExpressionFlattenerForAtExpr", job, new ExpressionFlattenerForAtExpr(job, ts, nf)).intern(this);
        }
    }
}
