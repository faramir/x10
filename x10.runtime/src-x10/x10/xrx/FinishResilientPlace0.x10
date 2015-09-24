/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2015.
 */
package x10.xrx;

import x10.compiler.*;

import x10.array.Array_2;
import x10.array.Array_3;
import x10.io.Serializer;
import x10.io.Deserializer;
import x10.util.concurrent.SimpleLatch;

/**
 * Place0-based Resilient Finish
 * This version is optimized and does not use ResilientStorePlace0
 */
class FinishResilientPlace0 extends FinishResilient {
    private static val verbose = FinishResilient.verbose;
    private static val place0 = Place.FIRST_PLACE;

    private static val AT = 0;
    private static val ASYNC = 1;
    private static val AT_AND_ASYNC = AT..ASYNC;

    private static class State { // data stored at Place0
        val NUM_PLACES = Place.numPlaces();
        val transit = new Array_3[Int](2, NUM_PLACES, NUM_PLACES);
        val transitAdopted = new Array_3[Int](2, NUM_PLACES, NUM_PLACES);
        val live = new Array_2[Int](2, NUM_PLACES);
        val liveAdopted = new Array_2[Int](2, NUM_PLACES);
        val excs = new x10.util.GrowableRail[CheckedThrowable](); // exceptions to report
        val children = new x10.util.GrowableRail[Long](); // children
        var adopterId:Long = -1; // adopter (if adopted)
        def isAdopted() = (adopterId != -1);
        var numDead:Long = 0;
        
        val parentId:Long; // parent (or -1)
        val gLatch:GlobalRef[SimpleLatch]; // latch to be released
        private def this(parentId:Long, gLatch:GlobalRef[SimpleLatch]) {
            this.parentId = parentId; this.gLatch = gLatch;
        }
        
        def dump(msg:Any) {
            val s = new x10.util.StringBuilder(); s.add(msg); s.add('\n');
            s.add("           live:"); s.add(live.toString(1024)); s.add('\n');
            s.add("    liveAdopted:"); s.add(liveAdopted.toString(1024)); s.add('\n');
            s.add("        transit:"); s.add(transit.toString(1024)); s.add('\n');
            s.add(" transitAdopted:"); s.add(transitAdopted.toString(1024)); s.add('\n');
            s.add("  children.size: " + children.size()); s.add('\n'); s.add('\n');
            s.add("      adopterId: " + adopterId); s.add('\n');
            s.add("       parentId: " + parentId);
            debug(s.toString());
        }
    }
    
    // TODO: freelist to reuse ids (maybe also states)
    //       or perhaps switch to HashMap[Long,State] instead of GrowableRail
    private static val states = (here.id==0) ? new x10.util.GrowableRail[State]() : null;

    private static val lock = (here.id==0) ? new x10.util.concurrent.Lock() : null;
    
    private val id:Long;
    private var hasRemote:Boolean = false;

    public def toString():String = "FinishResilientPlace0(id="+id+")";
    private def this(id:Long) { this.id = id; }
    
    static def make(parent:FinishState, latch:SimpleLatch):FinishResilient {
        if (verbose>=1) debug(">>>> make called, parent="+parent + " latch="+latch);
        val parentId = (parent instanceof FinishResilientPlace0) ? (parent as FinishResilientPlace0).id : -1; // ok to ignore other cases?
        val gLatch = GlobalRef[SimpleLatch](latch);
        val id = Runtime.evalImmediateAt[Long](place0, ()=>{ 
            try {
                lock.lock();
                val id = states.size();
                val state = new State(parentId, gLatch);
                states.add(state);
                state.live(ASYNC,gLatch.home.id) = 1n; // for myself, will be decremented in waitForFinish
                if (parentId != -1) states(parentId).children.add(id);
                return id;
            } finally {
                lock.unlock();
            }
        });
        val fs = new FinishResilientPlace0(id);
        if (verbose>=1) debug("<<<< make returning fs="+fs);
        return fs;
    }

    static def notifyPlaceDeath():void {
        if (verbose>=1) debug(">>>> notifyPlaceDeath called");
        if (here != place0) {
            if (verbose>=2) debug("not place0, returning");
            return;
        }
        try {
            lock.lock();
            for (id in 0..(states.size()-1)) {
                if (quiescent(id)) releaseLatch(id);
            }
        } finally {
            lock.unlock();
        }
        if (verbose>=1) debug("<<<< notifyPlaceDeath returning");
    }

    def notifySubActivitySpawn(place:Place):void {
        notifySubActivitySpawn(place, ASYNC);
    }
    def notifyShiftedActivitySpawn(place:Place):void {
        notifySubActivitySpawn(place, AT);
    }
    def notifySubActivitySpawn(place:Place, kind:long):void {
        val srcId = here.id, dstId = place.id;
        if (dstId != srcId) hasRemote = true;
        if (verbose>=1) debug(">>>> notifySubActivitySpawn(id="+id+") called, srcId="+srcId + " dstId="+dstId+" kind="+kind);
        Runtime.runImmediateAt(place0, ()=>{
            try {
                lock.lock();
                val state = states(id);
                if (!state.isAdopted()) {
                    state.transit(kind, srcId, dstId)++;
                } else {
                    val adopterId = getCurrentAdopterId(id);
                    val adopterState = states(adopterId);
                    adopterState.transitAdopted(kind, srcId, dstId)++;
                }
                if (verbose>=3) state.dump("DUMP id="+id);
            } finally {
                lock.unlock();
            }
        });
        if (verbose>=1) debug("<<<< notifySubActivitySpawn(id="+id+") returning");
    }

    def notifyRemoteContinuationCreated():void { 
        hasRemote = true;
    }

    /*
     * This method can't block because it may run on an @Immediate worker.  
     * Therefore it can't use Runtime.runImmediateAsync.
     * Instead sequence @Immediate messages to do the nac to place0 and
     * then come back and submit the pending activity.
     * Because place0 can't fail, we know that if the first message gets
     * to place0, the message back to push the activity will eventually
     * be received (unless dstId's place fails, in which case it doesn't matter).
     */
    def notifyActivityCreation(srcPlace:Place, activity:Activity):Boolean {
        return notifyActivityCreation(srcPlace, activity, ASYNC);
    }
    def notifyActivityCreation(srcPlace:Place, activity:Activity, kind:long):Boolean {
        val srcId = srcPlace.id; 
        val dstId = here.id;
        if (verbose>=1) debug(">>>> notifyActivityCreation(id="+id+") called, srcId="+srcId + " dstId="+dstId+" kind="+kind);
        if (srcPlace.isDead()) {
            if (verbose>=1) debug("<<<< notifyActivityCreation(id="+id+") returning false");
            return false;
        }

        val pendingActivity = GlobalRef(activity); 
        at (place0) @Immediate("notifyActivityCreation_to_zero") async {
            try {
                lock.lock();
                val state = states(id);
                if (!state.isAdopted()) {
                    state.live(kind, dstId)++;
                    state.transit(kind, srcId, dstId)--;
                } else {
                    val adopterId = getCurrentAdopterId(id);
                    val adopterState = states(adopterId);
                    adopterState.liveAdopted(kind, dstId)++;
                    adopterState.transitAdopted(kind, srcId, dstId)--;
                }
                if (verbose>=3) state.dump("DUMP id="+id);
            } finally {
                lock.unlock();
            }
            at (pendingActivity) @Immediate("notifyActivityCreation_push_activity") async {
                val pa = pendingActivity();
                if (pa != null && pa.epoch == Runtime.epoch()) {
                    if (verbose>=1) debug("<<<< notifyActivityCreation(id="+id+") finally submitting activity");
                    Runtime.worker().push(pa);
                }
                pendingActivity.forget();
            }
        };

        // Return false because we want to defer pushing the activity.
        return false;                
    }

    def notifyShiftedActivityCreation(srcPlace:Place):Boolean {
        val kind = AT;
        val srcId = srcPlace.id; 
        val dstId = here.id;
        if (verbose>=1) debug(">>>> notifyShiftedActivityCreation(id="+id+") called, srcId="+srcId + " dstId="+dstId+" kind="+kind);
        if (srcPlace.isDead()) {
            if (verbose>=1) debug("<<<< notifyShiftedActivityCreation(id="+id+") returning false");
            return false;
        }

        Runtime.runImmediateAt(place0, ()=> {
            try {
                lock.lock();
                val state = states(id);
                if (!state.isAdopted()) {
                    state.live(kind, dstId)++;
                    state.transit(kind, srcId, dstId)--;
                } else {
                    val adopterId = getCurrentAdopterId(id);
                    val adopterState = states(adopterId);
                    adopterState.liveAdopted(kind, dstId)++;
                    adopterState.transitAdopted(kind, srcId, dstId)--;
                }
                if (verbose>=3) state.dump("DUMP id="+id);
            } finally {
                lock.unlock();
            }
        });

        return true;
    }

    def notifyActivityCreationFailed(srcPlace:Place, t:CheckedThrowable):void { 
        notifyActivityCreationFailed(srcPlace, t, ASYNC);
    }
    def notifyActivityCreationFailed(srcPlace:Place, t:CheckedThrowable, kind:long):void { 
        val srcId = srcPlace.id; 
        val dstId = here.id;
        if (verbose>=1) debug(">>>> notifyActivityCreationFailed(id="+id+") called, srcId="+srcId + " dstId="+dstId+" kind="+kind);

        at (place0) @Immediate("notifyActivityCreationFailed_to_zero") async {
            try {
                lock.lock();
                if (verbose>=1) debug(">>>> notifyActivityCreationFailed(id="+id+") message running at place0");
                val state = states(id);
                if (!state.isAdopted()) {
                    state.transit(kind, srcId, dstId)--;
                    state.excs.add(t);
                    if (quiescent(id)) releaseLatch(id);
                } else {
                    val adopterId = getCurrentAdopterId(id);
                    val adopterState = states(adopterId);
                    adopterState.transitAdopted(kind, srcId, dstId)--;
                    adopterState.excs.add(t);
                    if (quiescent(adopterId)) releaseLatch(adopterId);
                }
            } finally {
                lock.unlock();
            }
       };

       if (verbose>=1) debug("<<<< notifyActivityCreationFailed(id="+id+") returning, srcId="+srcId + " dstId="+dstId);
    }

    def notifyActivityCreatedAndTerminated(srcPlace:Place) {
        notifyActivityCreatedAndTerminated(srcPlace, ASYNC);
    }
    def notifyActivityCreatedAndTerminated(srcPlace:Place, kind:long) {
        val srcId = srcPlace.id; 
        val dstId = here.id;
        if (verbose>=1) debug(">>>> notifyActivityCreatedAndTerminated(id="+id+") called, srcId="+srcId + " dstId="+dstId+" kind="+kind);

        at (place0) @Immediate("notifyActivityCreatedAndTerminated_to_zero") async {
            try {
                lock.lock();
                if (verbose>=1) debug(">>>> notifyActivityCreatedAndTerminated(id="+id+") message running at place0");
                val state = states(id);
                if (!state.isAdopted()) {
                    state.transit(kind, srcId, dstId)--;
                    if (quiescent(id)) releaseLatch(id);
                } else {
                    val adopterId = getCurrentAdopterId(id);
                    val adopterState = states(adopterId);
                    adopterState.transitAdopted(kind, srcId, dstId)--;
                    if (quiescent(adopterId)) releaseLatch(adopterId);
                }
            } finally {
                lock.unlock();
            }
       };

       if (verbose>=1) debug(">>>> notifyActivityCreatedAndTerminated(id="+id+") returning, srcId="+srcId + " dstId="+dstId);
    }

    def notifyActivityTermination():void {
        notifyActivityTermination(ASYNC);
    }
    def notifyShiftedActivityCompletion():void {
        notifyActivityTermination(AT);
    }
    def notifyActivityTermination(kind:long):void {
        val dstId = here.id;
        if (verbose>=1) debug(">>>> notifyActivityTermination(id="+id+") called, dstId="+dstId+" kind="+kind);
        at (place0) @Immediate("notifyActivityTermination_to_zero") async {
            try {
                lock.lock();
                if (verbose>=1) debug("<<<< notifyActivityTermination(id="+id+") message running at place0");
                val state = states(id);
                if (!state.isAdopted()) {
                    state.live(kind, dstId)--;
                    if (quiescent(id)) releaseLatch(id);
                } else {
                    val adopterId = getCurrentAdopterId(id);
                    val adopterState = states(adopterId);
                    adopterState.liveAdopted(kind, dstId)--;
                    if (quiescent(adopterId)) releaseLatch(adopterId);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    def pushException(t:CheckedThrowable):void {
        if (verbose>=1) debug(">>>> pushException(id="+id+") called, t="+t);
        Runtime.runImmediateAt(place0, ()=>{ 
            try {
                lock.lock();
                val state = states(id);
                state.excs.add(t); // need not consider the adopter
            } finally {
                lock.unlock();
            }
        });
        if (verbose>=1) debug("<<<< pushException(id="+id+") returning");
    }

    def waitForFinish():void {
        if (verbose>=1) debug(">>>> waitForFinish(id="+id+") called");
        // terminate myself
        notifyActivityTermination(ASYNC); // TOOD: merge this to the following evalImmediateAt

        // get the latch to wait
        val gLatch = Runtime.evalImmediateAt[GlobalRef[SimpleLatch]](place0, ()=>{ 
            try {
                lock.lock();
                val state = states(id);
                return state.gLatch;
            } finally {
                lock.unlock();
            }
        });
        assert gLatch.home==here;
        val lLatch = gLatch.getLocalOrCopy();

        // If we haven't gone remote with this finish yet, see if this worker
        // can execute other asyncs that are governed by the finish before waiting on the latch.
        if ((!Runtime.STRICT_FINISH) && (Runtime.STATIC_THREADS || !hasRemote)) {
            if (verbose>=2) debug("calling worker.join for id="+id);
            Runtime.worker().join(lLatch);
        }

        // wait for the latch release
        if (verbose>=2) debug("calling latch.await for id="+id);
        lLatch.await(); // wait for the termination (latch may already be released)
        if (verbose>=2) debug("returned from latch.await for id="+id);
        
        // get exceptions
        val e = Runtime.evalImmediateAt[MultipleExceptions](place0, ()=> {
            try {
                lock.lock();
                val state = states(id);
                if (!state.isAdopted()) {
                    states(id) = null;
                    return MultipleExceptions.make(state.excs); // may return null
                } else {
                    //TODO: need to remove the state in future
                    return null as MultipleExceptions;
                }
            } finally {
                lock.unlock();
            }
        });
        if (verbose>=1) debug("<<<< waitForFinish(id="+id+") returning, exc="+e);
        if (e != null) throw e;
    }

    /*
     * We have two options for spawning a remote async.
     *
     * The first (indirect) is most appropriate for "fat" asyncs whose
     * serialized form is a very large message.
     *   - notifySubActivitySpawn: src ===> Place0 (increment transit(src,dst)
     *   - x10rtSendAsync:         src ===> dst (send "fat" async body to dst)
     *   - notifyActivityCreation: dst ===> Place0 (decrement transit, increment live)
     *
     * The second (direct) is best for "small" asyncs, since it
     * reduces latency and only interacts with Place0 once, but
     * requires the async body to be bundled with the finish state
     * control messages, and thus sent on the network twice instead of once.
     *
     * We dynamically select the protocol on a per-async basis by comparing
     * the serialized size of body to a size threshold.
     */
    def spawnRemoteActivity(place:Place, body:()=>void, prof:x10.xrx.Runtime.Profile):void {
        val start = prof != null ? System.nanoTime() : 0;
        val ser = new Serializer();
        ser.writeAny(body);
        if (prof != null) {
            val end = System.nanoTime();
            prof.serializationNanos += (end-start);
            prof.bytes += ser.dataBytesWritten();
        }
        val bytes = ser.toRail();

        hasRemote = true;
        val srcId = here.id;
        val dstId = place.id;
        if (bytes.size >= ASYNC_SIZE_THRESHOLD) {
            if (verbose >= 1) debug("==== spawnRemoteActivity(id="+id+") selecting indirect (size="+
                                    bytes.size+") srcId="+srcId + " dstId="+dstId);
            val preSendAction = ()=>{ this.notifySubActivitySpawn(place); };
            val wrappedBody = ()=> @x10.compiler.AsyncClosure {
                val deser = new x10.io.Deserializer(bytes);
                val bodyPrime = deser.readAny() as ()=>void;
                bodyPrime();
            };
            x10.xrx.Runtime.x10rtSendAsync(place.id, wrappedBody, this, prof, preSendAction);
        } else {
            if (verbose >= 1) debug(">>>>  spawnRemoteActivity(id="+id+") selecting direct (size="+
                                    bytes.size+") srcId="+srcId + " dstId="+dstId);
            Runtime.runImmediateAt(place0, ()=>{
                try {
                    lock.lock();
                    val state = states(id);
                    if (!state.isAdopted()) {
                        state.live(ASYNC, dstId)++;
                    } else {
                        val adopterId = getCurrentAdopterId(id);
                        val adopterState = states(adopterId);
                        adopterState.liveAdopted(ASYNC, dstId)++;
                    }                                        
                    if (verbose>=3) state.dump("DUMP id="+id);
                } finally {
                    lock.unlock();
                }
                at (Place(dstId)) @Immediate("spawnRemoteActivity_dstPlace") async {
                    if (verbose >= 1) debug("==== spawnRemoteActivity(id="+id+") submitting activity from "+srcId+" at "+dstId);
                    val wrappedBody = ()=> {
                        // defer deserialization to reduce work on immediate thread
                        val deser = new x10.io.Deserializer(bytes);
                        val bodyPrime = deser.readAny() as ()=>void;
                        bodyPrime();
                    };
                    Runtime.worker().push(new Activity(42, wrappedBody, this));
               }
            });
            if (verbose>=1) debug("<<<< spawnRemoteActivity(id="+id+") returning");
        }
    }
    
    /*
     * Private methods
     */
    private static def getCurrentAdopterId(id:Long):Long {
        assert here==place0;
        var currentId:Long= id;
        while (true) {
            assert currentId!=-1;
            val state = states(currentId);
            if (!state.isAdopted()) break;
            currentId = state.adopterId;
        }
        return currentId;
    }

    private static def releaseLatch(id:Long) { // release the latch for this state
        assert here==place0; // must be called while lock is held and at place0
        if (verbose>=2) debug("releaseLatch(id="+id+") called");
        val state = states(id);
        val gLatch = state.gLatch;
        at (gLatch.home) @Immediate("releaseLatch_gLatch_home") async {
            if (verbose>=2) debug("calling latch.release for id="+id);
            gLatch.getLocalOrCopy().release(); // latch.wait is in waitForFinish
        };
        if (verbose>=2) debug("releaseLatch(id="+id+") returning");
    }

    private static def quiescent(id:Long):Boolean {
        assert here==place0; // must be called while lock is held and at place0
        if (verbose>=2) debug("quiescent(id="+id+") called");
        val state = states(id);
        if (state==null) {
            if (verbose>=2) debug("quiescent(id="+id+") returning false, state==null");
            return false;
        }
        if (state.isAdopted()) {
            if (verbose>=2) debug("quiescent(id="+id+") returning false, already adopted by adopterId=="+state.adopterId);
            return false;
        }
        
        // 1 pull up dead children
        val nd = Place.numDead();
        if (nd != state.numDead) {
            state.numDead = nd;
            val children = state.children;
            for (var chIndex:Long = 0; chIndex < children.size(); ++chIndex) {
                val childId = children(chIndex);
                val childState = states(childId);
                if (childState==null) continue;
                if (!childState.gLatch.home.isDead()) continue;
                val lastChildId = children.removeLast();
                if (chIndex < children.size()) children(chIndex) = lastChildId;
                chIndex--; // don't advance this iteration
                // adopt the child
                if (verbose>=3) debug("adopting childId="+childId);
                assert !childState.isAdopted();
                childState.adopterId = id;
                state.children.addAll(childState.children); // will be checked in the following iteration
		for (k in AT_AND_ASYNC) {
                    for (i in 0..(state.NUM_PLACES-1)) {
                        state.liveAdopted(k,i) += (childState.live(k,i) + childState.liveAdopted(k,i));
                        for (j in 0..(state.NUM_PLACES-1)) {
                            state.transitAdopted(k, i, j) += (childState.transit(k, i, j) + childState.transitAdopted(k, i, j));
                        }
                    }
                }
            } // for (chIndex)
        }

        // 2 delete dead entries
        for (i in 0..(state.NUM_PLACES-1)) {
            if (Place.isDead(i)) {
                for (1..state.live(ASYNC, i)) {
                    if (verbose>=3) debug("adding DPE for live asyncs("+i+")");
                    addDeadPlaceException(state, i);
                }
                state.live(AT, i) = 0n; state.liveAdopted(AT, i) = 0n;
                state.live(ASYNC, i) = 0n; state.liveAdopted(ASYNC, i) = 0n;
                for (j in 0..(state.NUM_PLACES-1)) {
                    state.transit(AT, i,j) = 0n; state.transitAdopted(AT, i, j) = 0n;
                    state.transit(ASYNC, i,j) = 0n; state.transitAdopted(ASYNC, i, j) = 0n;
                    for (1..state.transit(ASYNC, j,i)) {
                        if (verbose>=3) debug("adding DPE for transit asyncs("+j+","+i+")");
                        addDeadPlaceException(state, i);
                    }
                    state.transit(AT, j,i) = 0n; state.transitAdopted(AT, j,i) = 0n;
                    state.transit(ASYNC,j,i) = 0n; state.transitAdopted(ASYNC,j,i) = 0n;
                }
            }
        }
        
        // 3 quiescent check
        if (verbose>=3) state.dump("DUMP id="+id);
        var quiet:Boolean = true;
        outer: for (i in 0..(state.NUM_PLACES-1)) {
            for (k in AT_AND_ASYNC) {
                if (state.live(k, i) > 0) { quiet = false; break outer; }
                if (state.liveAdopted(k,i) > 0) { quiet = false; break outer; }
                for (j in 0..(state.NUM_PLACES-1)) {
                    if (state.transit(k,i,j) > 0) { quiet = false; break outer; }
                    if (state.transitAdopted(k,i,j) > 0) { quiet = false; break outer; }
                }
            }
        }
        if (verbose>=2) debug("quiescent(id="+id+") returning " + quiet);
        return quiet;
    }
    private static def addDeadPlaceException(state:State, placeId:Long) {
        val e = new DeadPlaceException(Place(placeId));
        e.fillInStackTrace(); // meaningless?
        state.excs.add(e);
    }

}
