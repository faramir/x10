/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package x10.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.types.DerefTransform;
import polyglot.types.Named;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.Use;
import polyglot.util.TransformingList;

public class X10TypeObjectMixin {
    
    public static List<Type> annotations(X10Def def) {
        return new TransformingList<Ref<? extends Type>, Type>(def.defAnnotations(), new DerefTransform<Type>());
    }
    
    public static List<Type> annotationsMatching(X10Def o, Type t) {
        return annotationsMatching(annotations(o), t);
    }
    
    public static List<Type> annotationsNamed(X10Def o, QName fullName) {
        return annotationsNamed(annotations(o), fullName);
    }
    
    public static List<Type> annotations(X10Use<? extends X10Def> o) {
        return annotations(o.x10Def());
    }

    public static List<Type> annotationsMatching(X10Use<? extends X10Def> o, Type t) {
        return annotationsMatching(annotations(o), t);
    }
    
    public static List<Type> annotationsNamed(X10Use<? extends X10Def> o, QName fullName) {
        return annotationsNamed(annotations(o), fullName);
    }

    public static List<Type> annotationsMatching(List<Type> annotations, Type t) {
        List<Type> l = new ArrayList<Type>();
        for (Iterator<Type> i = annotations.iterator(); i.hasNext(); ) {
            Type ct = i.next();
            if (ct.isSubtype(t, t.typeSystem().emptyContext())) {
                l.add(ct);
            }
        }
        return l;
    }

    public static List<Type> annotationsNamed(List<Type> annotations, QName fullName) {
        List<Type> l = new ArrayList<Type>();
        for (Iterator<Type> i = annotations.iterator(); i.hasNext(); ) {
            Type ct = i.next();
            if (ct instanceof Named && ((Named) ct).fullName().equals(fullName)) {
                l.add(ct);
            }
        }
        return l;
    }

}
