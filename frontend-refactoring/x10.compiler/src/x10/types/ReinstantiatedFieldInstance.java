/**
 * 
 */
package x10.types;

import polyglot.util.Position;
import x10.types.constraints.CConstraint;

final class ReinstantiatedFieldInstance extends X10FieldInstance_c {
	private static final long serialVersionUID = 8234625319808346804L;

	private final TypeParamSubst typeParamSubst;
	private final X10FieldInstance fi;

	ReinstantiatedFieldInstance(TypeParamSubst typeParamSubst, TypeSystem ts, Position pos,
			Ref<? extends X10FieldDef> def, X10FieldInstance fi) {
		super(ts, pos, def);
		this.typeParamSubst = typeParamSubst;
		this.fi = fi;
	}

	@Override
	public Type type() {
		if (type == null)
			return this.typeParamSubst.reinstantiate(fi.type());
		return type;
	}

	@Override
	public StructType container() {
		if (container == null)
			return this.typeParamSubst.reinstantiate(fi.container());
		return container;
	}
}