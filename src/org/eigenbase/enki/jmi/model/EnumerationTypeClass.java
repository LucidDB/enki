/*
//  $Id$
//  Enki generates and implements the JMI and MDR APIs for MOF metamodels.
//  Copyright (C) 2007-2008 The Eigenbase Project
//  Copyright (C) 2007-2008 Disruptive Tech
//  Copyright (C) 2007-2008 LucidEra, Inc.
// 
//  This library is free software; you can redistribute it and/or modify it
//  under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation; either version 2.1 of the License, or (at
//  your option) any later version.
// 
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
//  USA
*/

package org.eigenbase.enki.jmi.model;

import javax.jmi.reflect.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * Implements MOF's EnumerationType class proxy interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class EnumerationTypeClass
    extends RefClassBase
    implements javax.jmi.model.EnumerationTypeClass
{
    EnumerationTypeClass(
        RefPackage container)
    {
        super(container);

    }

    public EnumerationType createEnumerationType()
    {
        return new org.eigenbase.enki.jmi.model.EnumerationType(this);
    }

    @SuppressWarnings("unchecked")
    public EnumerationType createEnumerationType(
        String name,
        String annotation,
        boolean isRoot,
        boolean isLeaf,
        boolean isAbstract,
        javax.jmi.model.VisibilityKind visibility,
        java.util.List/*<String>*/ labels)
    {
        return new org.eigenbase.enki.jmi.model.EnumerationType(
            this,
            name,
            annotation,
            isRoot,
            isLeaf,
            isAbstract,
            visibility,
            labels);
    }

    protected void checkConstraints(java.util.List<javax.jmi.reflect.JmiException> errors, boolean deepVerify)
    {
    }
}

// End EnumerationTypeClass.java
