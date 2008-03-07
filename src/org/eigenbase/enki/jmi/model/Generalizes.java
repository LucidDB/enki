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

import java.util.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * Implements MOF's Generalizes association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class Generalizes
    extends RefAssociationBase
    implements javax.jmi.model.Generalizes
{
    public Generalizes(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "supertype",
            Multiplicity.UNIQUE_ORDERED_COLLECTION,
            "subtype",
            Multiplicity.UNIQUE_COLLECTION);

    }

    public boolean exists(
        javax.jmi.model.GeneralizableElement supertype,
        javax.jmi.model.GeneralizableElement subtype)
    {
        return super.refLinkExists(supertype, subtype);
    }

    @SuppressWarnings("unchecked")
    public List/*<javax.jmi.model.GeneralizableElement>*/ getSupertype(
        javax.jmi.model.GeneralizableElement subtype)
    {
        return (List)super.refQuery(
            "subtype", subtype);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.GeneralizableElement>*/ getSubtype(
        javax.jmi.model.GeneralizableElement supertype)
    {
        return super.refQuery(
            "supertype", supertype);
    }

    public boolean add(
        javax.jmi.model.GeneralizableElement supertype,
        javax.jmi.model.GeneralizableElement subtype)
    {
        return super.refAddLink(supertype, subtype);
    }

    public boolean remove(
        javax.jmi.model.GeneralizableElement supertype,
        javax.jmi.model.GeneralizableElement subtype)
    {
        return super.refRemoveLink(supertype, subtype);
    }
}

// End Generalizes.java
