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
 * Implements MOF's DataType class proxy interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class DataTypeClass
    extends RefClassBase
    implements javax.jmi.model.DataTypeClass
{
    DataTypeClass(
        RefPackage container)
    {
        super(container);

    }

    protected void checkConstraints(java.util.List<javax.jmi.reflect.JmiException> errors, boolean deepVerify)
    {
    }

    public Class<?> getInstanceClass()
    {
        return null;
    }
}

// End DataTypeClass.java
