/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation, 2009-2015 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    jurgen@vinju.org

 *******************************************************************************/
package org.rascalmpl.value.type;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IList;
import org.rascalmpl.value.IListWriter;
import org.rascalmpl.value.ISetWriter;
import org.rascalmpl.value.IString;
import org.rascalmpl.value.IValue;
import org.rascalmpl.value.IValueFactory;
import org.rascalmpl.value.exceptions.FactTypeUseException;
import org.rascalmpl.value.exceptions.UndeclaredAnnotationException;
import org.rascalmpl.value.type.TypeFactory.TypeReifier;

/**
 * A tree type is a type of tree node, defined by its name, the types of
 * its children and the type it produces. Example tree types would be:
 * 
 * Address ::= dutchAddress(Street, City, Postcode)
 * Address ::= usAddress(Street, City, State, PostalCode)
 * 
 * Here Address is the AbstractDataType, the type a tree produces. dutchAddress
 * and usAddress are the names of the node types and the other capitalized names
 * are the types of the children.
 * 
 * Children types can also be named as in:
 * Boolean ::= and(Boolean lhs, Boolean rhs)
 * Boolean ::= or(Boolean lhs, Boolean rhs)
 * 
 */
/*package*/ final class ConstructorType extends AbstractDataType {
	private final Type fChildrenTypes;
	private final Type fADT;
	private final String fName;

	/* package */ ConstructorType(String name, Type childrenTypes, Type adt) {
		super(adt.getName(), adt.getTypeParameters());
		fName = name.intern(); // TODO remove this expensive trick
		fChildrenTypes = childrenTypes;
		fADT = adt;
	}

	public static class Info implements TypeReifier {
		@Override
		public Type getSymbolConstructorType() {
			return symbols().typeSymbolConstructor("cons", symbols().symbolADT(), "adt", tf().stringType(), "name", tf().listType(symbols().symbolADT()), "parameters");
		}

		public Type getProductionConstructorType() {
			return symbols().typeProductionConstructor("cons", symbols().symbolADT(), "def", TF.listType(symbols().symbolADT()), "symbols", TF.listType(symbols().symbolADT()), "kwTypes", tf().setType(symbols().attrADT()), "attributes");
		}
		
		@Override
		public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
				Set<IConstructor> done) {
			IListWriter w = vf.listWriter();

			if (type.hasFieldNames()) {
				for (int i = 0; i < type.getArity(); i++) {
					w.append(symbols().labelSymbol(vf, type.getFieldType(i).asSymbol(vf, store, grammar, done), type.getFieldName(i)));
				}
			}
			else {
				for (Type field : type.getFieldTypes()) {
					w.append(field.asSymbol(vf, store, grammar, done));
				}
			}

			IConstructor adt = type.getAbstractDataType().asSymbol(vf, store, grammar, done);
			IConstructor cons = vf.constructor(getSymbolConstructorType(),  adt, vf.string(type.getName()), w.done());

			return cons;
		}
		
		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
			Type adt = symbols().fromSymbol((IConstructor) symbol.get("adt"), store, grammar);
			IList parameters = (IList) symbol.get("parameters");
			String name = ((IString) symbol.get("name")).getValue();
			return TF.constructorFromTuple(store, adt, name, symbols().fromSymbols(parameters, store, grammar));
		}

		/*package*/ Type fromProduction(IConstructor prod, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
		    if (prod.getConstructorType() == getSymbolConstructorType()) {
		        // TODO this should not be necessary after standardizing cons representations
		        return fromAlternativeProduction(prod, store, grammar);
		    }
		    
		    // TODO remove double field name after bootstrap
			IConstructor adt = (IConstructor)  (prod.has("def") ? prod.get("def") : prod.get("adt"));
			String name = symbols().getLabel(adt);
			IConstructor sym = symbols().getLabeledSymbol(adt);
			IList parameters = (IList) prod.get("symbols");
			IList kwtypes = (IList) prod.get("kwTypes");

			Type cons = TF.constructorFromTuple(store, symbols().fromSymbol(sym, store, grammar), name, symbols().fromSymbols(parameters, store, grammar));

			for (IValue kwType : kwtypes) {
				store.declareKeywordParameter(cons, symbols().getLabel(kwType), symbols().fromSymbol(symbols().getLabeledSymbol(kwType), store, grammar));
			}

			return cons;
		}
		
		private Type fromAlternativeProduction(IConstructor prod, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
		    IConstructor adt = (IConstructor)  prod.get("adt");
            String name = ((IString) prod.get("name")).getValue();
            IList parameters = (IList) prod.get("parameters");

            return TF.constructorFromTuple(store, symbols().fromSymbol(adt, store, grammar), name, symbols().fromSymbols(parameters, store, grammar));
        }

        @Override
		public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
				Set<IConstructor> done) {
			IConstructor adt = type.getAbstractDataType().asSymbol(vf, store, grammar, done);

			IListWriter w = vf.listWriter();
			if (type.hasFieldNames()) {
				for (int i = 0; i < type.getArity(); i++) {
					w.append(symbols().labelSymbol(vf, type.getFieldType(i).asSymbol(vf, store, grammar, done), type.getFieldName(i)));
				}
			}
			else {
				for (Type field : type.getFieldTypes()) {
					w.append(field.asSymbol(vf, store, grammar, done));
				}
			}

			IListWriter kwTypes = vf.listWriter();
			Map<String,Type> keywordParameters = store.getKeywordParameters(type);

			for (String label : keywordParameters.keySet()) {
				kwTypes.insert(symbols().labelSymbol(vf, keywordParameters.get(label).asSymbol(vf, store, grammar, done), label));
			}

			IConstructor cons = vf.constructor(getProductionConstructorType(), symbols().labelSymbol(vf, adt, type.getName()), w.done(), kwTypes.done(), vf.set());

			grammar.insert(vf.tuple(adt, cons));
		}
        
        @Override
        public boolean isRecursive() {
            return true;
        }
        
        @Override
        public Type randomInstance(Supplier<Type> next, TypeStore store, Random rnd) {
            return tf().constructorFromTuple(store, new AbstractDataType.Info().randomInstance(next, store, rnd), randomLabel(rnd), randomTuple(next, store, rnd));
        }
	}

	@Override
    public TypeReifier getTypeReifier() {
    	return new Info();
    }
	
	@Override
	public Type carrier() {
		return fChildrenTypes.carrier();
	}

	@Override
	public int hashCode() {
		return 21 + 44927 * ((fName != null) ? fName.hashCode() : 1) + 
				181 * fChildrenTypes.hashCode() + 
				354767453 * fADT.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ConstructorType) {
			ConstructorType other = (ConstructorType) o;

			if (fName != other.fName) { // fName is interned, change to equals when intern() is removed
				return false;
			}

			if (fChildrenTypes != other.fChildrenTypes) {
				return false;
			}

			if (fADT != other.fADT) {
				return false;
			}


			// nothing is different
			return true;
		}

		// not a constructor type
		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(fADT);
		builder.append(" = ");
		builder.append(fName);
		builder.append("(");

		Iterator<Type> iter = fChildrenTypes.iterator();
		while(iter.hasNext()) {
			builder.append(iter.next());

			if (iter.hasNext()) {
				builder.append(',');
			}
		}

		builder.append(")");

		return builder.toString();
	}

	@Override
	public int getArity() {
		return fChildrenTypes.getArity();
	}

	@Override
	public int getFieldIndex(String fieldName) throws FactTypeUseException {
		return fChildrenTypes.getFieldIndex(fieldName);
	}

	@Override
	public boolean hasField(String fieldName) {
		return fChildrenTypes.hasField(fieldName);
	}

	@Override
	public boolean hasField(String fieldName, TypeStore store) {
		return hasField(fieldName) ;
	}

	@Override
	public boolean hasKeywordField(String fieldName, TypeStore store) {
		return store.getKeywordParameterType(this, fieldName) != null;
	}

	@Override
	public Type getFieldTypes() {
		return fChildrenTypes;
	}


	@Override
	public String getName() {
		return fName;
	}

	@Override
	public Type getAbstractDataType() {
		return fADT;
	}

	@Override
	public Type getFieldType(int i) {
		return fChildrenTypes.getFieldType(i);
	}

	@Override
	public Type getFieldType(String fieldName) throws FactTypeUseException {
		return fChildrenTypes.getFieldType(fieldName);
	}

	@Override
	public String getFieldName(int i) {
		return fChildrenTypes.getFieldName(i);
	}

	@Override
	public String[] getFieldNames() {
		return fChildrenTypes.getFieldNames();
	}

	@Override
	public boolean hasFieldNames() {
		return fChildrenTypes.hasFieldNames();
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitConstructor(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
		return type.isSubtypeOfConstructor(this);
	}

	@Override
	public Type lub(Type type) {
		return type.lubWithConstructor(this);
	}

	@Override
	public Type glb(Type type) {
		return type.glbWithConstructor(this);
	}

	@Override
	protected boolean isSubtypeOfConstructor(Type type) {
		if (type.getName().equals(getName())) {
			return getAbstractDataType().isSubtypeOf(type.getAbstractDataType())
					&& getFieldTypes().isSubtypeOf(type.getFieldTypes());
		}
		else {
			return false;
		}
	}

	@Override
	protected boolean isSubtypeOfAbstractData(Type type) {
		return getAbstractDataType().isSubtypeOfAbstractData(type);
	}

	@Override
	protected Type lubWithConstructor(Type type) {
		if(this == type) {
			return this;
		}
		return getAbstractDataType().lubWithAbstractData(type.getAbstractDataType());
	}

	@Override
	protected Type glbWithConstructor(Type type) {
		if (type.isSubtypeOf(this)) {
			return type;
		}
		else if (isSubtypeOf(type)) {
			return this;
		}
		else {
			return TF.voidType();
		}
	}

	@Override
	protected Type glbWithAbstractData(Type type) {
		if (isSubtypeOf(type)) {
			return this;
		}

		return TF.voidType();
	}

	@Override
	protected Type glbWithNode(Type type) {
		return this;
	}

	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		return super.match(matched, bindings)
				&& fADT.match(matched.getAbstractDataType(), bindings)
				&& getFieldTypes().match(matched.getFieldTypes(), bindings);
	}

	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		if (bindings.isEmpty()) {
			return this;
		}
		Type adt = fADT.instantiate(bindings);
		Type fields = getFieldTypes().instantiate(bindings);

		TypeStore store = new TypeStore();
		store.declareAbstractDataType(fADT);
		store.declareConstructor(this);

		return TypeFactory.getInstance().constructorFromTuple(store, adt, getName(), fields);
	}

	@Override
	public boolean declaresAnnotation(TypeStore store, String label) {
		return store.getAnnotationType(this, label) != null;
	}

	@Override
	public Type getAnnotationType(TypeStore store, String label) throws FactTypeUseException {
		Type type = store.getAnnotationType(this, label);

		if (type == null) {
			throw new UndeclaredAnnotationException(getAbstractDataType(), label);
		}

		return type;
	}

	@Override
	public boolean isParameterized() {
		return fADT.isParameterized();
	}
}
