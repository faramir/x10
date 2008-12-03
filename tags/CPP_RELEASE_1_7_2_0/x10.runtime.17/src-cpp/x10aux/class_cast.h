#ifndef X10AUX_CLASS_CAST_H
#define X10AUX_CLASS_CAST_H

#include <x10aux/config.h>
#include <x10aux/throw.h>

#include <x10aux/RTT.h>
#include <x10aux/ref.h>

#include <x10/lang/Object.h>
#include <x10/lang/Box.h>

#include <x10/lang/ClassCastException.h>
#include <x10/lang/BadPlaceException.h>

namespace x10aux {

    template<class T> struct CAST_TRACER {
        CAST_TRACER(T val_) : val(val_) { }
        T val;
        T get() { return val; }
    };
    template<class T> struct CAST_TRACER<ref<T> > {
        CAST_TRACER(ref<T> val_) : val(val_) { }
        ref<T> val;
        T* get() { return val.get(); }
    };
    template<class T> std::ostream& operator<<(std::ostream& o, CAST_TRACER<T> t) {
        return o << t.get();
    }

    template<class T> ref<x10::lang::Box<T> > box(T obj) {
        _CAST_("boxed: "<<CAST_TRACER<T>(obj)<<" of type "<<TYPENAME(T));
        return X10NEW(x10::lang::Box<T>)(obj);
    }

    template<class T> T unbox(ref<x10::lang::Box<T> > obj) {
        _CAST_("unboxed: "<<CAST_TRACER<ref<x10::lang::Box<T> > >(obj)<<" of type "<<TYPENAME(T));
        return obj->get();
    }

    // T stands for "to"
    // F stands for "from"

    template<class T, class F> struct ClassCastNotBothRef {
        // All possibilities accounted for, if you got here something has gone wrong
    };

    // Box primitives on casting to interfaces
    #define PRIMITIVE_INTERFACE_CAST(T,F) \
    template<> struct ClassCastNotBothRef<ref<T>,F> { \
        static ref<T> _ (F obj) { \
            _CAST_(TYPENAME(F) <<" converted to "<<TYPENAME(ref<T>)); \
            return ref<T>(box(obj)); \
        } \
    }
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_boolean);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_byte);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_char);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_short);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_int);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_long);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_float);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Object, x10_double);
    /* TODO
    PRIMITIVE_INTERFACE_CAST(x10::lang::Integer, x10_byte);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Integer, x10_short);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Integer, x10_int);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Integer, x10_long);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Signed, x10_byte);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Signed, x10_short);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Signed, x10_int);
    PRIMITIVE_INTERFACE_CAST(x10::lang::Signed, x10_long);
    */

    template<class T, class F> struct ClassCastNotBothRef<ref<T>,F*> {
        static ref<T> _(F* obj) {
            return ref<T>(ref<F>(obj));
        }
    };

    // Boxing of primitives
    template<class T> struct ClassCastNotBothRef<ref<x10::lang::Box<T> >,T> {
        static ref<x10::lang::Box<T> > _(T obj) {
            return box(obj);
        }
    };

    // Unboxing of primitives
    template<class T> struct ClassCastNotBothRef<T,ref<x10::lang::Box<T> > > {
        static T _(ref<x10::lang::Box<T> > obj) {
            return unbox(obj);
        }
    };

    // ClassCastBothRef
    template<class T, class F> struct ClassCastBothRef { static ref<T> _(ref<F> obj) {
        if (obj == x10aux::null) {
            // NULL passes any class cast check and remains NULL
            _CAST_("Special case: null gets cast to "<<TYPENAME(ref<T>));
            return obj;
        }
        if (obj.isRemote()) {
            //compare static types as we can't get at the dynamic type
            const RuntimeType *from = getRTT<ref<F> >();
            const RuntimeType *to = getRTT<ref<T> >();
            #ifndef NO_EXCEPTIONS
            if (!from->subtypeOf(to)) {
                // can only upcast remote refs
                throwException<x10::lang::BadPlaceException>();
            }
            #endif
            _CAST_("Special case: remote reference gets upcast to "<<TYPENAME(ref<T>));
            return ref<T>(reinterpret_cast<T*>(obj.get()));
        }
            
        const RuntimeType *from = obj->_type();
        const RuntimeType *to = getRTT<ref<T> >();
        #ifndef NO_EXCEPTIONS
        _CAST_(from->name()<<" to "<<to->name());
        if (!from->subtypeOf(to)) {
            throwException<x10::lang::ClassCastException>();
        }
        #else
        _CAST_("UNCHECKED! "<<from->name()<<" to "<<to->name());
        #endif
        return static_cast<ref<T> >(obj);
    } };

    // Boxing of ref types
    template<class T> struct ClassCastBothRef<x10::lang::Box<ref<T> >,T> {
        static ref<x10::lang::Box<ref<T> > > _(ref<T> obj) {
            return box(obj);
        }
    };

    // Unboxing of ref types
    template<class T> struct ClassCastBothRef<T,x10::lang::Box<ref<T> > > {
        static ref<T> _(ref<x10::lang::Box<ref<T> > > obj) {
            return unbox(obj);
        }
    };


    // ClassCastNotPrimitive
    template<class T, class F> struct ClassCastNotPrimitive { static T _(F obj) {
        return ClassCastNotBothRef<T,F>::_(obj);
    } };

    template<class T, class F> struct ClassCastNotPrimitive<ref<T>,ref<F> > {
        static ref<T> _(ref<F> obj) {
            _CAST_("Ref to ref cast "<<TYPENAME(T)<<" to "<<TYPENAME(T));
            return ClassCastBothRef<T,F>::_(obj);
        }
    };

    // This is the second level that recognises primitive casts
    template<class T, class F> struct ClassCastPrimitive { static T _(F obj) {
        // if we get here it's not a primitive cast
        _CAST_("Not a primitive cast "<<TYPENAME(T)<<" to "<<TYPENAME(T));
        return ClassCastNotPrimitive<T,F>::_(obj);
    } };

    #define PRIMITIVE_CAST(T,F) \
    template<> struct ClassCastPrimitive<T,F> { \
        static T _ (F obj) { \
            _CAST_(TYPENAME(F) <<" converted to "<<TYPENAME(T)); \
            return static_cast<T>(obj); \
        } \
    }

    // make reflexive
    #define PRIMITIVE_CAST2(T,F) PRIMITIVE_CAST(T,F) ; PRIMITIVE_CAST(F,T)

    // boolean can't be cast to anything except itself (handled below)
    // everything else is totally connected

    PRIMITIVE_CAST2(x10_byte,x10_char);
    PRIMITIVE_CAST2(x10_byte,x10_short);
    PRIMITIVE_CAST2(x10_byte,x10_int);
    PRIMITIVE_CAST2(x10_byte,x10_long);
    PRIMITIVE_CAST2(x10_byte,x10_float);
    PRIMITIVE_CAST2(x10_byte,x10_double);

    PRIMITIVE_CAST2(x10_char,x10_short);
    PRIMITIVE_CAST2(x10_char,x10_int);
    PRIMITIVE_CAST2(x10_char,x10_long);
    PRIMITIVE_CAST2(x10_char,x10_float);
    PRIMITIVE_CAST2(x10_char,x10_double);

    PRIMITIVE_CAST2(x10_short,x10_int);
    PRIMITIVE_CAST2(x10_short,x10_long);
    PRIMITIVE_CAST2(x10_short,x10_float);
    PRIMITIVE_CAST2(x10_short,x10_double);

    PRIMITIVE_CAST2(x10_int,x10_long);
    PRIMITIVE_CAST2(x10_int,x10_float);
    PRIMITIVE_CAST2(x10_int,x10_double);

    PRIMITIVE_CAST2(x10_long,x10_float);
    PRIMITIVE_CAST2(x10_long,x10_double);

    PRIMITIVE_CAST2(x10_float,x10_double);


    // first level of template specialisation that recognises <T,T>
    // (needed because generic classes can be instantiated in ways that make casts redundant)
    template<class T, class F> struct ClassCast { static T _ (F obj) {
        return ClassCastPrimitive<T,F>::_(obj);
    } };
    template<class T> struct ClassCast<T,T> { static T _ (T obj) {
        // nothing to do (until we have constraints)
        _CAST_("Identity cast to/from "<<TYPENAME(T));
        return obj;
    } };

    template<typename T, typename F> T class_cast(F obj) {
        return ClassCast<T,F>::_(obj);
    }

}

    

#endif
// vim:tabstop=4:shiftwidth=4:expandtab
