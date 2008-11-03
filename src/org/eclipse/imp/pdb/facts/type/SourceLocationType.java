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

import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;


public class SourceLocationType  extends Type {
    private final static SourceLocationType sInstance= new SourceLocationType();

    /*package*/ static SourceLocationType getInstance() {
        return sInstance;
    }

    private SourceLocationType() { }

    @Override
    public boolean isSourceLocationType() {
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
        return (obj instanceof SourceLocationType);
    }

    @Override
    public int hashCode() {
        return 61547;
    }

    @Override
    public String toString() {
        return "sourceLocation";
    }
    
    @Override
    public IValue accept(ITypeVisitor visitor) {
    	return visitor.visitSourceLocation(this);
    }
    
    @Override
    public IValue make(IValueFactory f, String path, ISourceRange range) {
    	return f.sourceLocation(path, range);
    }
    
}
