/*
============================================================================
 Name        : Sys.h
 Author      : Rajkishore Barik
 Version     :
 Copyright   : IBM Corporation 2007
 Description : Exe source file
============================================================================
*/
#ifndef x10lib_Sys_h
#define x10lib_Sys_h

/*I am hacking this file to include XLC PowerPC intrinsics on
 * rlsecomp1. I am not sure how this would work on other machines, and
 * if there exists any difference even within the powerpc
 * processors. Those would be worries for some other time.
 * @author Sriram.K
 */

#include <assert.h>
#include <time.h>

#ifdef __xlC__
#include <sys/atomic_op.h>
#endif

namespace x10lib_cws {
#ifndef __POWERPC__ 
#define __POWERPC__

#if 1  
#define MEM_BARRIER()  __asm__ __volatile__ ("sync" : : : "memory")
#define READ_BARRIER()  __asm__ __volatile__ ("lwsync" : : : "memory")
#define WRITE_BARRIER()  __asm__ __volatile__ ("lwsync" : : : "memory")
#else
#define MEM_BARRIER()  __sync()
#define READ_BARRIER()  __lwsync()
#define WRITE_BARRIER()  __lwsync()
#endif

static __inline__ int atomic_exchange(volatile int *ptr, int x)
{
  int result=0;
  
#if 1
  __asm__ __volatile__ (
			"lwarx %0,0,%1\n stwcx. %2,0,%1\n .long 0x40a2fff8 \n isync\n" :
			"=&r"(result) : 
			"r"(ptr), "r"(x) :
			"cr0");
#else
#warning "Fix atomic_exchange before using. Commented now"
  assert(0);
#endif
  
  return result;
}


static  int
compare_exchange(int *p, int  old_value, int new_value)
{
#if defined(__xlC__)
  int *old = &old_value;	
  return compare_and_swap(p, old, new_value);

#elif defined(__GNUC__)
  int prev;                                        
  __asm__ __volatile__ (                           
			"\n"
        		"l1:\n\t"
			"lwarx   %0,0,%2\n\t"
        		"cmpw    0,%0,%3\n\t"
        		"bne-    l2 \n\t"
        		"stwcx.  %4,0,%2\n\t"   
        		"bne-    l1 \n\t"
        		"isync\n"
        		"l2:"
        		: "=&r" (prev), "=m" (*p)
        		: "r" (p), "r" (old_value),
			"r" (new_value), "m" (*p)
        		: "cc", "memory");
  return prev;


#else
#error "Fix compare_exchange before running. Commented now"
  int prev;
  __asm__ __volatile__ (
			
        		"1:     lwarx   %0,0,%2\n\
        				cmpw    0,%0,%3\n\
        				bne-    2f\n\
        				stwcx.  %4,0,%2\n\
        				bne-    1b\
        				isync\n\
        		2:"	
        		: "=&r" (prev), "=m" (*p)
        		: "r" (p), "r" (old_value), "r" (new_value), "m" (*p)
        		: "cc", "memory");
  return prev;
#endif
}

static void atomic_add(volatile int* mem, int val)
{
    int tmp;

#if defined(__xlC__)
    fetch_and_add((int *)mem, val); //ignore return value
#elif defined(__GNUC__)

  __asm__ __volatile__ (                                      
			" #Inline atomic add\n"  
			"l1:\n\t"
			"lwarx    %0,0,%2 \n\t"
			"add%I3   %0,%0,%3 \n\t"
			"stwcx.   %0,0,%2 \n\t"
			"bne-     l1 \n\t"  
			"isync \n\t"
			: "=&b"(tmp), "=m" (*mem)
			: "r" (mem), "Ir"(val), "m" (*mem) 
			: "cr0");

#else
#error "Fix atomic_add before running. Commented now"

    __asm__ __volatile__ (
			  "/* Inline atomic add */\n"
			  "0:\t"
				  "lwarx    %0,0,%2 \n\t"
				  "add%I3   %0,%0,%3 \n\t"
				  "stwcx.   %0,0,%2 \n\t"
				  "bne-     0b \n\t"
   		  	"isync \n\t"
			  : "=&b"(tmp), "=m" (*mem)
			  : "r" (mem), "Ir"(val), "m" (*mem)
			  : "cr0");
  
#endif
}
#endif


static __inline__ int atomic_fetch(volatile int* mem)
{
	
#if defined (__xlC__)
	//return fetch_and_nop((int *)mem);
	return *mem;
#else
#warning "Fix atomic_fetch before running. Commented now"
	assert(0);
#endif
}
/*Simple portable timers for now. Could add accurate system-specific
  timers later*/

inline long long nanoTime() {
  struct timespec ts;
  // clock_gettime is POSIX!
  ::clock_gettime(CLOCK_REALTIME, &ts);
  return (long long)(ts.tv_sec * 1000000000LL + ts.tv_nsec);
}


}
#endif
