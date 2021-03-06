/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.rascalmpl.value;

import java.util.Arrays;
import java.util.function.Supplier;

import org.rascalmpl.value.io.binary.message.IValueReader;
import org.rascalmpl.value.type.TypeStore;

public class Setup {

  public static Iterable<? extends Object> valueFactories() {
    return Arrays.asList(org.rascalmpl.value.impl.reference.ValueFactory.getInstance(),
        org.rascalmpl.value.impl.fast.ValueFactory.getInstance(),
        org.rascalmpl.value.impl.persistent.ValueFactory.getInstance());
  }

  /**
   * Allocates new {@link TypeStore} environments that are used within {@link IValueReader}.
   *
   * Type stores are used as encapsulated namespaces for types. The supplier creates a fresh type
   * store environment, to avoid name clashes when nesting types / values.
   */
  public static final Supplier<TypeStore> TYPE_STORE_SUPPLIER = () -> {
    TypeStore typeStore = new TypeStore();
    // typeStore.declareAbstractDataType(RascalValueFactory.Type);
    // typeStore.declareConstructor(RascalValueFactory.Type_Reified);
    // typeStore.declareAbstractDataType(RascalValueFactory.ADTforType);
    return typeStore;
  };

}
