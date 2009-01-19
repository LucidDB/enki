/*
//  $Id$
//  Enki generates and implements the JMI and MDR APIs for MOF metamodels.
//  Copyright (C) 2007-2009 The Eigenbase Project
//  Copyright (C) 2007-2009 Disruptive Tech
//  Copyright (C) 2007-2009 LucidEra, Inc.
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
 * Implements MOF's AssociationEnd class proxy interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class AssociationEndClass
    extends RefClassBase
    implements javax.jmi.model.AssociationEndClass
{
    AssociationEndClass(
        RefPackage container)
    {
        super(container);

    }

    public AssociationEnd createAssociationEnd()
    {
        org.eigenbase.enki.jmi.model.AssociationEnd instance = 
            new org.eigenbase.enki.jmi.model.AssociationEnd(this);
        register(instance);
        return instance;
    }

    public AssociationEnd createAssociationEnd(
        String name,
        String annotation,
        boolean isNavigable,
        javax.jmi.model.AggregationKind aggregation,
        javax.jmi.model.MultiplicityType multiplicity,
        boolean isChangeable)
    {
        org.eigenbase.enki.jmi.model.AssociationEnd instance =
            new org.eigenbase.enki.jmi.model.AssociationEnd(
                this,
                name,
                annotation,
                isNavigable,
                aggregation,
                multiplicity,
                isChangeable);
        register(instance);
        return instance;
    }

    protected void checkConstraints(java.util.List<javax.jmi.reflect.JmiException> errors, boolean deepVerify)
    {
    }

    public Class<?> getInstanceClass()
    {
        return org.eigenbase.enki.jmi.model.AssociationEnd.class;
    }
}

// End AssociationEndClass.java
