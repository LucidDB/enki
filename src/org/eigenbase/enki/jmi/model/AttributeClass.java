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
 * Implements MOF's Attribute class proxy interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class AttributeClass
    extends RefClassBase
    implements javax.jmi.model.AttributeClass
{
    AttributeClass(
        RefPackage container)
    {
        super(container);

    }

    public Attribute createAttribute()
    {
        return new org.eigenbase.enki.jmi.model.Attribute(this);
    }

    public Attribute createAttribute(
        String name,
        String annotation,
        javax.jmi.model.ScopeKind scope,
        javax.jmi.model.VisibilityKind visibility,
        javax.jmi.model.MultiplicityType multiplicity,
        boolean isChangeable,
        boolean isDerived)
    {
        return new org.eigenbase.enki.jmi.model.Attribute(
            this,
            name,
            annotation,
            scope,
            visibility,
            multiplicity,
            isChangeable,
            isDerived);
    }
}

// End AttributeClass.java
