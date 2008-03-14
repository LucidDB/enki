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
 * Implements MOF's AttachesTo association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class AttachesTo
    extends RefAssociationBase
    implements javax.jmi.model.AttachesTo
{
    public AttachesTo(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "modelElement",
            new Multiplicity(1, -1, false, true),
            "tag",
            new Multiplicity(0, -1, true, true));

    }

    public boolean exists(
        javax.jmi.model.ModelElement modelElement,
        javax.jmi.model.Tag tag)
    {
        return super.refLinkExists(modelElement, tag);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.ModelElement>*/ getModelElement(
        javax.jmi.model.Tag tag)
    {
        return super.refQuery(
            "tag", tag);
    }

    @SuppressWarnings("unchecked")
    public List/*<javax.jmi.model.Tag>*/ getTag(
        javax.jmi.model.ModelElement modelElement)
    {
        return (List)super.refQuery(
            "modelElement", modelElement);
    }

    public boolean add(
        javax.jmi.model.ModelElement modelElement,
        javax.jmi.model.Tag tag)
    {
        return super.refAddLink(modelElement, tag);
    }

    public boolean remove(
        javax.jmi.model.ModelElement modelElement,
        javax.jmi.model.Tag tag)
    {
        return super.refRemoveLink(modelElement, tag);
    }
}

// End AttachesTo.java
