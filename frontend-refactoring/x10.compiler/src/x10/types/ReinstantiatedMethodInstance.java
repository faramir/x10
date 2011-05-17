/**
 * 
 */
package x10.types;

import java.util.List;

import polyglot.types.*;
import polyglot.util.Position;
import x10.types.constraints.CConstraint;
import x10.types.constraints.TypeConstraint;

final class ReinstantiatedMethodInstance extends X10MethodInstance_c {
	private static final long serialVersionUID = -1235025903277125182L;

	private final TypeParamSubst typeParamSubst;
	private final X10MethodInstance fi;

	ReinstantiatedMethodInstance(TypeParamSubst typeParamSubst, TypeSystem ts, Position pos,
			Ref<? extends X10MethodDef> def, X10MethodInstance fi) {
		super(ts, pos, def);
		this.typeParamSubst = typeParamSubst;
		this.fi = fi;
	}

	@Override
	public Ref<? extends Type> returnTypeRef() {
		if (returnType == null)
			return this.typeParamSubst.reinstantiate(fi.returnTypeRef());
		return returnType;
	}

	@Override
	public Type returnType() {
		if (returnType == null)
			return this.typeParamSubst.reinstantiate(fi.returnType());
		return returnType.get();
	}

	@Override
	public List<LocalInstance> formalNames() {
		if (formalNames == null) 
			return this.typeParamSubst.reinstantiate(fi.formalNames());
		return formalNames;
	}
	@Override
	public List<Type> formalTypes() {
		if (formalTypes == null)
			return this.typeParamSubst.reinstantiate(fi.formalTypes());
		return formalTypes;
	}


	@Override
	public Ref<? extends Type> offerType() {
	    final Ref<? extends Type> ref = fi.offerType();
	    if (ref==null) return null;
	    return new Ref_c<Type>(this.typeParamSubst.reinstantiate(ref.get()));
	}

	@Override
	public CConstraint guard() {
	    if (guard == null)
	        return this.typeParamSubst.reinstantiate(fi.guard());
	    return guard;
	}

	@Override
	public TypeConstraint typeGuard() {
	    if (typeGuard == null)
	        return this.typeParamSubst.reinstantiate(fi.typeGuard());
	    return typeGuard;
	}

	@Override
	public StructType container() {
		if (container == null)
			return this.typeParamSubst.reinstantiate(fi.container());
		return container;
	}
}