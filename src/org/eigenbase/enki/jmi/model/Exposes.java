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
 * Implements MOF's Exposes association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class Exposes
    extends RefAssociationBase
    implements javax.jmi.model.Exposes
{
    public Exposes(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "Referrer",
            Multiplicity.UNIQUE_COLLECTION,
            "ExposedEnd",
            Multiplicity.SINGLE);

    }

    public boolean exists(
        javax.jmi.model.Reference referrer,
        javax.jmi.model.AssociationEnd exposedEnd)
    {
        return super.refLinkExists(referrer, exposedEnd);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.Reference>*/ getReferrer(
        javax.jmi.model.AssociationEnd exposedEnd)
    {
        return super.refQuery(
            "ExposedEnd", exposedEnd);
    }

    public javax.jmi.model.AssociationEnd getExposedEnd(
        javax.jmi.model.Reference referrer)
    {
        Collection<?> result = super.refQuery(
            "Referrer", referrer);
        Iterator<?> iter = result.iterator();
        if (iter.hasNext()) {
            return (javax.jmi.model.AssociationEnd)iter.next();
        }
        return null;
    }

    public boolean add(
        javax.jmi.model.Reference referrer,
        javax.jmi.model.AssociationEnd exposedEnd)
    {
        return super.refAddLink(referrer, exposedEnd);
    }

    public boolean remove(
        javax.jmi.model.Reference referrer,
        javax.jmi.model.AssociationEnd exposedEnd)
    {
        return super.refRemoveLink(referrer, exposedEnd);
    }
}

// End Exposes.java
