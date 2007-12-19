/*
//  $Id:
//  //open/enki/src/org/eigenbase/enki/codegen/MofImplementationGenerator.java#1
//  $
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
 * Implements MOF's IsOfType association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class IsOfType
    extends RefAssociationBase
    implements javax.jmi.model.IsOfType
{
    public IsOfType(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "type",
            Multiplicity.SINGLE,
            "typedElements",
            Multiplicity.UNIQUE_COLLECTION);
    }

    public boolean exists(
        javax.jmi.model.Classifier type,
        javax.jmi.model.TypedElement typedElements)
    {
        return super.refLinkExists(type, typedElements);
    }

    public javax.jmi.model.Classifier getType(
        javax.jmi.model.TypedElement typedElements)
    {
        Collection<?> result = super.refQuery(
            "typedElements", typedElements);
        Iterator<?> iter = result.iterator();
        if (iter.hasNext()) {
            return (javax.jmi.model.Classifier)iter.next();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.TypedElement>*/ getTypedElements(
        javax.jmi.model.Classifier type)
    {
        return super.refQuery(
            "type", type);
    }

    public boolean add(
        javax.jmi.model.Classifier type,
        javax.jmi.model.TypedElement typedElements)
    {
        return super.refAddLink(type, typedElements);
    }

    public boolean remove(
        javax.jmi.model.Classifier type,
        javax.jmi.model.TypedElement typedElements)
    {
        return super.refRemoveLink(type, typedElements);
    }
}

// End IsOfType.java
