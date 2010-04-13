/*
//  $Id$
//  Enki generates and implements the JMI and MDR APIs for MOF metamodels.
//  Copyright (C) 2007 The Eigenbase Project
//  Copyright (C) 2007 SQLstream, Inc.
//  Copyright (C) 2007 Dynamo BI Corporation
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
 * Implements MOF's Parameter class proxy interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class ParameterClass
    extends RefClassBase
    implements javax.jmi.model.ParameterClass
{
    ParameterClass(
        RefPackage container)
    {
        super(container);

    }

    public Parameter createParameter()
    {
        org.eigenbase.enki.jmi.model.Parameter instance = 
            new org.eigenbase.enki.jmi.model.Parameter(this);
        register(instance);
        return instance;
    }

    public Parameter createParameter(
        String name,
        String annotation,
        javax.jmi.model.DirectionKind direction,
        javax.jmi.model.MultiplicityType multiplicity)
    {
        org.eigenbase.enki.jmi.model.Parameter instance =
            new org.eigenbase.enki.jmi.model.Parameter(
                this,
                name,
                annotation,
                direction,
                multiplicity);
        register(instance);
        return instance;
    }

    protected void checkConstraints(java.util.List<javax.jmi.reflect.JmiException> errors, boolean deepVerify)
    {
    }

    public Class<?> getInstanceClass()
    {
        return org.eigenbase.enki.jmi.model.Parameter.class;
    }
}

// End ParameterClass.java
