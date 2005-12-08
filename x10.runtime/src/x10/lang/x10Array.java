package x10.lang;
import x10.lang.place;

/* root for all array classes */
abstract public class x10Array extends x10.lang.Object implements Indexable, Unsafe {

	// used to associate a partitioned global array with a
	// particular place
        private x10.lang.place _owningPlace; 
	public place getOwningPlace() {assert (_owningPlace != null);return _owningPlace;}
	public void setOwningPlace(place id) { _owningPlace = id;}

	// copy src onto dest over regions on the this place
	abstract public dist getDistribution();

	// For distributed arrays, return vm-unique long to identify
	// it.  Used to create fatpointer for global objects
	public long generateUniqueId(){ return -1;}

}
