/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

public class StringType extends Type {
    private final static StringType sInstance= new StringType();

    /*package*/ static StringType getInstance() {
        return sInstance;
    }

    private StringType() { }

    @Override
    public boolean isStringType() {
    	return true;
    }
    
    @Override
    public boolean isSubtypeOf(Type other) {
    	return other == this || other.isValueType();
    }

    @Override
    public Type lub(Type other) {
        if (other.isSubtypeOf(this)) {
            return this;
        }
        
        return TypeFactory.getInstance().valueType();
    }

    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof StringType);
    }

    @Override
    public int hashCode() {
        return 94903;
    }

    @Override
    public String toString() {
        return "string";
    }
    
    @Override
    public IValue accept(ITypeVisitor visitor) {
    	return visitor.visitString(this);
    }
    
    @Override
    public IValue make(IValueFactory f, String arg) {
    	return f.string(arg);
    }

}
