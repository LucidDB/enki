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
 * Implements MOF's Contains association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class Contains
    extends RefAssociationBase
    implements javax.jmi.model.Contains
{
    public Contains(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "container",
            javax.jmi.model.Namespace.class,
            new Multiplicity(0, 1, false, false),
            "containedElement",
            javax.jmi.model.ModelElement.class,
            new Multiplicity(0, -1, true, true));

    }

    public boolean exists(
        javax.jmi.model.Namespace container,
        javax.jmi.model.ModelElement containedElement)
    {
        return super.refLinkExists(container, containedElement);
    }

    public javax.jmi.model.Namespace getContainer(
        javax.jmi.model.ModelElement containedElement)
    {
        Collection<?> result = super.refQuery(
            "containedElement", containedElement);
        Iterator<?> iter = result.iterator();
        if (iter.hasNext()) {
            return (javax.jmi.model.Namespace)iter.next();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List/*<javax.jmi.model.ModelElement>*/ getContainedElement(
        javax.jmi.model.Namespace container)
    {
        return (List)super.refQuery(
            "container", container);
    }

    public boolean add(
        javax.jmi.model.Namespace container,
        javax.jmi.model.ModelElement containedElement)
    {
        return super.refAddLink(container, containedElement);
    }

    public boolean remove(
        javax.jmi.model.Namespace container,
        javax.jmi.model.ModelElement containedElement)
    {
        return super.refRemoveLink(container, containedElement);
    }
}

// End Contains.java
