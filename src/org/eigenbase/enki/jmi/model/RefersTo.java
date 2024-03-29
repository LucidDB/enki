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

import java.util.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * Implements MOF's RefersTo association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class RefersTo
    extends RefAssociationBase
    implements javax.jmi.model.RefersTo
{
    public RefersTo(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "referent",
            javax.jmi.model.Reference.class,
            new Multiplicity(0, -1, false, true),
            "referencedEnd",
            javax.jmi.model.AssociationEnd.class,
            new Multiplicity(1, 1, false, false));

    }

    public boolean exists(
        javax.jmi.model.Reference referent,
        javax.jmi.model.AssociationEnd referencedEnd)
    {
        return super.refLinkExists(referent, referencedEnd);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.Reference>*/ getReferent(
        javax.jmi.model.AssociationEnd referencedEnd)
    {
        return super.refQuery(
            "referencedEnd", referencedEnd);
    }

    public javax.jmi.model.AssociationEnd getReferencedEnd(
        javax.jmi.model.Reference referent)
    {
        Collection<?> result = super.refQuery(
            "referent", referent);
        Iterator<?> iter = result.iterator();
        if (iter.hasNext()) {
            return (javax.jmi.model.AssociationEnd)iter.next();
        }
        return null;
    }

    public boolean add(
        javax.jmi.model.Reference referent,
        javax.jmi.model.AssociationEnd referencedEnd)
    {
        return super.refAddLink(referent, referencedEnd);
    }

    public boolean remove(
        javax.jmi.model.Reference referent,
        javax.jmi.model.AssociationEnd referencedEnd)
    {
        return super.refRemoveLink(referent, referencedEnd);
    }
}

// End RefersTo.java
