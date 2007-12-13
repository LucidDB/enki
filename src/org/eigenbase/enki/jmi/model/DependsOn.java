/*
//  $Id$
//  Enki generates and implements the JMI and MDR APIs for MOF metamodels.
//  Copyright (C) 2007-2007 The Eigenbase Project
//  Copyright (C) 2007-2007 Disruptive Tech
//  Copyright (C) 2007-2007 LucidEra, Inc.
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

import java.util.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * Implements MOF's DependsOn association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class DependsOn
    extends DependsOnBase
    implements javax.jmi.model.DependsOn
{
    public DependsOn(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "dependent",
            Multiplicity.UNIQUE_COLLECTION,
            "provider",
            Multiplicity.UNIQUE_COLLECTION);
    }

    public boolean exists(
        javax.jmi.model.ModelElement dependent,
        javax.jmi.model.ModelElement provider)
    {
        return super.refLinkExists(dependent, provider);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.ModelElement>*/ getDependent(
        javax.jmi.model.ModelElement provider)
    {
        return super.refQuery(
            "dependent", provider);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.ModelElement>*/ getProvider(
        javax.jmi.model.ModelElement dependent)
    {
        return super.refQuery(
            "provider", dependent);
    }

}

// End DependsOn.java
