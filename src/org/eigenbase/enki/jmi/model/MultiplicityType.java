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
 * Implements MOF's MultiplicityType interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class MultiplicityType
    extends RefStructBase
    implements javax.jmi.model.MultiplicityType
{
    // Fields
    public int lower;
    public int upper;
    public boolean isOrdered;
    public boolean isUnique;

    MultiplicityType(
        int lower,
        int upper,
        boolean isOrdered,
        boolean isUnique)
    {
        super();

        this.lower = lower;
        this.upper = upper;
        this.isOrdered = isOrdered;
        this.isUnique = isUnique;
    }

    public int getLower() throws JmiException
    {
        return lower;
    }

    public int getUpper() throws JmiException
    {
        return upper;
    }

    public boolean isOrdered() throws JmiException
    {
        return isOrdered;
    }

    public boolean isUnique() throws JmiException
    {
        return isUnique;
    }

    private static final long serialVersionUID = -1630324975055117959L;

    protected void checkConstraints(java.util.List<javax.jmi.reflect.JmiException> errors, boolean deepVerify)
    {
    }
}

// End MultiplicityType.java
