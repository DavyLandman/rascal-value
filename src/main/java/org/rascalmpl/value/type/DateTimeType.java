/*******************************************************************************
* Copyright (c) 2009 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Mark Hills (Mark.Hills@cwi.nl) - initial API and implementation
*******************************************************************************/
package org.rascalmpl.value.type;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IValueFactory;

/**
 * @author mhills
 *
 */
public class DateTimeType extends DefaultSubtypeOfValue {
   private static final class InstanceKeeper {
    public final static DateTimeType sInstance= new DateTimeType();
   }

    public static DateTimeType getInstance() {
        return InstanceKeeper.sInstance;
    }
    
    @Override
    protected IConstructor asSymbol(IValueFactory vf) {
      return vf.constructor(reifiedConstructorType);
    }
    
	private DateTimeType() {
		super(TF.constructor(symbolStore, Symbol, "datetime"));
	}

    @Override
    public boolean equals(Object obj) {
        return obj == DateTimeType.getInstance();
    }

    @Override
    public int hashCode() {
        return 63097;
    }

	@Override
	public String toString() {
		return "datetime";
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitDateTime(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
	  return type.isSubtypeOfDateTime(this);
	}
	
	@Override
	public Type lub(Type other) {
	  return other.lubWithDateTime(this);
	}
	
	@Override
	public Type glb(Type type) {
	  return type.glbWithDateTime(this);
	}
	
	@Override
	protected boolean isSubtypeOfDateTime(Type type) {
	  return true;
	}
	
	@Override
	protected Type lubWithDateTime(Type type) {
	  return this;
	}
	
	@Override
	protected Type glbWithDateTime(Type type) {
	  return this;
	}
}
