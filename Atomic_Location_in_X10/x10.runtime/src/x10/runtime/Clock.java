package x10.runtime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import x10.lang.ClockUseException;
import x10.lang.Runtime;
import x10.lang.clock;
import x10.runtime.distributed.RemoteClock;

/**
 * Implementation of Clock.  Technically the spec says that this is
 * a value class, but of course the implementation of clocks needs to
 * keep mutable state (somewhere).  Clocks are also integrated with the
 * Runtime and with places since they need to interact with activity
 * creation.  
 
 * 
 <p>Let each activity A track the phase of every clock c it is registered
 with, call this variable A_C. 
 
 <p>Let each clock c track its phase, call it Cp. 
 
 <p>A clock is said to be in whole phase if A_C = Cp for every activity A. 
 
 <p>It is in split phase if A_C = Cp-1 for some activities, and A_C = Cp
 for some others.
 
 <p>In the algorithm we dont actually need to track Cp and A_C, though it
 is helpful to think as if we are.
 
 <p>The clock tracks three sets of activities: activities_, resumed_, and
 nextResumed_. The clock also has a boolean variable, splitPhase, which
 is initialy false.
 
 <p>Initially the clock starts out in whole phase (splitPhase=false).
 
 <p>We will assume that activities always do a resume before doing a next. 
 
 <p>Say that an activity A in activities_ is 
 <ul>
 <li> activeNow: clock is in wholephase, A is not in resumed_.
 <li> quiescedNow: A is in resumed_.
 <li> activeNext: clock is in splitphase, A is not in resumed_ || nextResumed_.
 <li> quiescedNext: A is in nextResumed_.
 
 <li> active: activeNow or activeNext
 <li>quiesced: quiescedNow or quiescedNext
 <li> registered: active or quiesced
 <li> unregistered: not in activities_, resumed_ or nextResumed_
 </ul>
 
 <p>Invariants on the state of the sytems:
 <ul>
 <li>  (1) A clock is either in splitPhase or wholePhase.
 <li>  (2) activity_ superset resumed_ u nextResumed_
 <li>  (3) nextResumed_ and resumed_ are disjoint
 <li>  (4) In wholePhase, nextResumed_ = empty.
 <li>  (5) Whenever next is performed by an activity, activity is quiesced.
 <li>  (6) An activity is either active or quiesced.
 </ul>
 
 <P>Transition from wholePhase to splitPhase: when |activity_| = |resumed_|
 <p>Transition from splitPhase to wholePhase: when |resumed_| = 0
 
 <p>When an activity does a resume:
 <ul>
 <li>unregistered -> throw CUE
 <li>quiescedNow -> quiescedNow, return.    
 <li>quiescedNext -> quiescedNext, return.
 <li>activeNow -> quiescedNow, tryMoveToSplit, return.
 <li>activeNext -> quiescedNext, return.
 </ul>
 
 <p>When an activity does a next: {assumed to be quiesced by (5)}
 <ul>
 <li>unregistered -> throw CUE
 <li>quiescedNow, splitPhase -> activeNext, tryMoveToWhole, return.
 <li>quiescedNow, wholePhase -> block until splitPhase, move to activeNow on wakeup.
 <li> quiescedNext, splitPhase -> block until next splitPhase, move to activeNow on wakeup
 <li> quiescedNext, wholePhase : impossible start state by (4).
 </ul>
 
 <p>When an activity terminates:
 <ul>
 <li> unregistered -> throw CUE
 <li> registered -> unregistered, tryMoveToSplit, return.
 </ul>
 
 <p>When an activity B spawns another A, giving it the clock c:
 <ul>
 <li> B quiesced  -> throw CUE
 <li> B unregistered -> throw CUE
 <li> A unregistered -> A registered, return.
 </ul>
 
 <p>tryMoveToSplit: 
 If |activities_| == |resumed_|, move clock to splitPhase, wake up all
 suspended activities.
 
 <p>tryMoveToWhole:
 If |resumed_| == 0, move clock to wholePhase, move nextResumed_ to resumed_,
 set nextResumed_ to empty. {This causes all activities in quiescedNext state
 to move to the quiescedNow state.}
 
 * </code>
 * @author Christian Grothoff, Christoph von Praun
 * @author vj
 */

/* This really should be a final class.  However, for the time being,
   I allow RemoteClock to extend this class. */
public /* final */ class Clock extends clock {
	
	private static int nextId_ = 0;
	private int id_;
	private final String name_;

	
	/**
	 * A Set of all activities registered with this clock.
	 */
	protected final Set activities_ = new HashSet(); // <Activity>
	protected int activityCount_ = 0;
        public int activityCount() { return activityCount_; }
	
	/** A clock is split iff all activities have resumed, the clock has advanced,
	 * and there is at least one activity that has resumed that has not yet performed
	 * a next.
	 * In split phase, activities are executing in two phases: the current phase
	 * and the next phase. 
	 */
	private boolean splitPhase_ = false;
	
	/** The set of all activities that have executed a resume in the current clock cycle.
	 * 
	 */
	private final Set resumed_ = new HashSet(); // <Activity>
	private int resumedCount_ = 0;
	
	/**
	 * The Set of all activities that have executed a resume in the next phase of a 
	 * split clock.
	 */
	private final Set nextResumed_ = new HashSet(); // <Activity>
	private int nextResumedCount_ = 0;
	

	/**
	 * The first registered advance listener.  null if none is
	 * registered.
	 */
	private AdvanceListener listener1_;
	
	/**
	 * Lazy initialization (since we typically only have at most one
	 * listener).
	 */
	private ArrayList listeners_; // <AdvanceListener>
	
	/**
	 * The current phase of the Clock.  Incremented by one in each
	 * phase (may wrap around!).
	 */
	private int phase_;
	
    
	/**
	 * Create a new Clock.  Registers the current activity with
	 * the clock as a side-effect (see X10 Report).
	 */
	public Clock() {
		this( "");
	}
	public Clock( String name) {
		this.name_ = name;
		synchronized (getClass()) {
			id_ = nextId_++;
		}
                if (this instanceof RemoteClock) {
                   // arrrgg... have to create activity first!
                } else {
                    Activity a = Runtime.getCurrentActivity();
                    a.addClock(this);
                    activities_.add(a);
                    activityCount_++;
                    if (Report.should_report("clock",3)) {
			Report.report(3, PoolRunner.logString() + " " + this + " created by " + a +".");
                    }
                }
	}
	
	/**
	 * An activity a is inactive on a clock if it is not registered with it
	 * or it is registered with it, but has already resumed it in the current phase.
	 * @param a
	 * @return
	 */
	private boolean inactive( Activity a) {
		return ( (! activities_.contains(a)) || quiescent(a));
	}
	/**
	 * Register the current activity with this clock.
	 */
	public synchronized void register(Activity a ) {
                /* TODO ... a better authorizer for multi-VM case */
		Activity authorizer =
                   a.activityAsSeenByInvokingVM==Activity.thisActivityIsLocal ?
                   Runtime.getCurrentActivity() : null;
                if (authorizer != null)
                    if (Report.should_report("clock", 5)) {
			Report.report(5, PoolRunner.logString() + " " + this + ".register:" + authorizer + " registering " + a);
                    }
		synchronized (this) {
                   if (authorizer != null && inactive(authorizer))	
				throw new ClockUseException(authorizer + "is not active on " + this + "; cannot transmit.");
			if (activities_.contains(a)) return;
			activities_.add(a);
			activityCount_++;
		}
		if (Report.should_report("clock", 3)) {
			Report.report(3, PoolRunner.logString() + " " + this + "...done.(activityCount_=" + activityCount_+").");
		}
	}
	
	    
	private boolean quiescent( Activity a) {
		return resumed_.contains(a) || nextResumed_.contains(a);
		
	}
	/**
	 * Notify the clock that this activity has completed its
	 * work in this phase of the clock.
	 */
	public void resume() {
           resume(Runtime.getCurrentActivity());
        }
	public void resume(Activity a) {
		// do not lock earlier - see comment in doNext
		if (Report.should_report("clock", 5)) {
			Report.report(5, PoolRunner.logString() + " " + this + ".resume(" + a  +")");
		}
		synchronized (this) {
			if (! activities_.contains(a))
				throw new ClockUseException(a + "is not registered with " + this +"; cannot execute 'resume'.");
			
			if (quiescent(a)) {
				if (Report.should_report("clock", 5)) {
					Report.report(5, PoolRunner.logString() + " " + this + "...returned (noop).");
				}
				return; 
			}
			if (splitPhase_) {
				nextResumed_.add(a);
				nextResumedCount_++;
				if (Report.should_report("clock", 5)) {
					Report.report(5, PoolRunner.logString() + " " + this + "...added to nextResumed.");
				}
				return;
			}
			resumed_.add(a);
			resumedCount_++;
			if (Report.should_report("clock", 3)) {
				Report.report(3, PoolRunner.logString() + " " + this + "...added to resumed.");
			}
			tryMoveToSplit_();
		}
	}
	
	
	
	public boolean registered() {
		Activity a = Runtime.getCurrentActivity();	
		// do not lock earlier - see comment in doNow       
		synchronized (this) {
			return activities_.contains(a);
		}
	}
	public boolean dropped() {
		return ! registered();
	}
	
	public boolean quiescent() {
		Activity a = Runtime.getCurrentActivity();
		synchronized (this) {
			return quiescent(a);
		}
		
	}
	/**
	 * Drop this activity from the clock.  Afterwards the
	 * activity may no longer use continue or now on this clock.
	 * Other activities will no longer be blocked waiting for 
	 * the current activity to complete the phase.  
	 * 
	 */
	public void drop() {
		Activity a = Runtime.getCurrentActivity();
		if (drop(a))
			a.dropClock( this );
	}
	
	/**
	 * Drop the given activity from the clock.  Afterwards the
	 * activity may no longer use continue or now on this clock.
	 * Other activities will no longer be blocked waiting for 
	 * the current activity to complete the phase.<p>  
	 * 
	 * Note that this method is used internally (by clock.drop or
	 * by an activity exiting) and should never be called directly
	 * by clients (hence package-scoped). Note that this method
	 * does not unregister the clock with the list of clocks kept
	 * by the activity.
	 * 
	 * @return true if the activity has already dropped this
	 *   clock (or if it never was registered).
	 */
	public synchronized boolean drop(Activity a) {
	   
		boolean ret = activities_.remove(a);
		if (ret) activityCount_--;
		if (resumed_.remove(a)) resumedCount_--;
		if (nextResumed_.remove(a)) nextResumedCount_--;
		tryMoveToSplit_();
		 if (Report.should_report("clock", 3)) {
			Report.report(3, this + " drops " + a +").");
		 }
		return ret;        
	}
	
	/**
	 * Advance to the next phase.  Increments the phase counter,
	 * calls all advance listeners and then signals the activities
	 * that are waiting to get them going again.
	 */
	synchronized boolean tryMoveToSplit_() {
	    if (Report.should_report("clock", 3)) {
			Report.report(3, PoolRunner.logString() + " " + this + ".tryMoveToSplit_()");
			}
		
		if (! (activityCount_ == resumedCount_ )) {
		    if (Report.should_report("clock", 3)) {
				Report.report(3, "...fails");
				}
			return false;
		}
		if (Report.should_report("clock", 3)) {
			Report.report(3, "...succeeds");
			}
		splitPhase_ = true;
		this.phase_++;
		if (Sampling.SINGLETON != null)
			Sampling.SINGLETON.signalEvent(Sampling.EVENT_ID_CLOCK_ADVANCE, id_);
		// first notify everyone
		if (this.listener1_ != null) {
			this.listener1_.notifyAdvance();
			if (this.listeners_ != null) {
				int size = listeners_.size();
				for (int i=0;i<size;i++)
					((AdvanceListener)listeners_.get(i)).notifyAdvance();
			}
		}
		this.notifyAll();
		return true;
		
	}
	private synchronized boolean tryMoveToWhole_() {
	    if (Report.should_report("clock", 3)) {
			Report.report(3, PoolRunner.logString() + " " + this+".tryMoveToWhole_()");
			}
		if (resumedCount_ != 0) {
		    if (Report.should_report("clock", 3)) {
				Report.report(3, this+"...fails.");
				}
			return false;
		}
		if (Report.should_report("clock", 3)) {
			Report.report(3, this+"...succeeds.");
			}
		splitPhase_ = false;
		resumed_.addAll(nextResumed_);
		nextResumed_.clear();
		resumedCount_ = nextResumedCount_;
		nextResumedCount_ = 0;
		return true;
	}
	
	
	private void block_() {
		int start = phase_;
		while (start == phase_) { // signal might be random in Java, check!
			try {
				LoadMonitored.blocked(Sampling.CAUSE_CLOCK, id_, null);
				this.wait(); // wait for signal
			} catch (InterruptedException ie) {
				throw new Error(ie); // that was unexpected...
			} finally {
				LoadMonitored.unblocked(Sampling.CAUSE_CLOCK, id_, null);
			}
		}
	}
	
	public void doNext() {
            doNext(Runtime.getCurrentActivity());
        };

        /* An activity on a remote VM has done a next on this clock */
        public void doNextForRemoteClock(final Activity a, final long lapi_target, final long lapi_target_addr) {
            // do not stall LAPI threads
            new Thread() {
                public void run() {
                    doNext(a);
                    RemoteClock.completeClockOp(lapi_target, lapi_target_addr);
                }
            }.start();
        }
    
	public void doNext(Activity a) {
		// do not acquire lock earlier - otherwise deadlock can happen
		// because the lock used to protected aip_.getCurrentActivity
		// is also held when terminating activities drop locks ...
		if (Report.should_report("clock", 5)) {
			Report.report(5, PoolRunner.logString() + " " + this+".doNext(" + a + ") called.");
		}
		synchronized (this) {
			assert activities_.contains(a);
			assert nextResumed_.contains(a) || resumed_.contains(a);
			if (!splitPhase_ || nextResumed_.contains(a)){
				if (Report.should_report("clock", 3)) {
					Report.report(3, PoolRunner.logString() + " " + this+".doNext(" + a + ") blocks.");
				}
				block_();
			}
			if (Report.should_report("clock", 3)) {
				Report.report(3, PoolRunner.logString() + " " + this+".doNext(" + a + ") continues.");
			}
			assert resumed_.contains(a);
			resumed_.remove(a);
			resumedCount_ --;
			tryMoveToWhole_();
			return;
		}
	}

        public native long establishGlobalRefAddr_();
        public void establishGlobalRefAddr() {
            globalRefAddr_ = establishGlobalRefAddr_();
        }
        public void clearGlobalRefAddr() {
            globalRefAddr_ = 0;
        }
        public long getGlobalRefAddr() { return globalRefAddr_; }
        private long globalRefAddr_;
	/**
	 * Register a callback that is to be called whenever the clock
	 * advances into the next phase.
	 * 
	 * @param al the listener to notify
	 
	public synchronized void registerAdvanceListener(AdvanceListener al) {
		if (this.listener1_ == null) {
			this.listener1_ = al;
		} else {
			if (this.listeners_ == null)
				this.listeners_ = new ArrayList();
			this.listeners_.add(al);
		}
	}
	*/
	/**
	 * Callback method used by the Clock to notify all listeners
	 * that the Clock is advancing into the next phase.
	 *
	 * @author Christian Grothoff
	 */
	public interface AdvanceListener {
		public void notifyAdvance();
	}
	public String toString() { 
		return "Clock" + id_ +  name_ 
		+"("  + (splitPhase_ ? "split," : "whole,") + phase_
		+ ":" + activityCount_ 
		+ "," + resumedCount_
		+ "," + nextResumedCount_
                + ((globalRefAddr_ == 0) ? "" : ("," + Long.toHexString(globalRefAddr_)))
		+ ")";
	}
	
} // end of Clock