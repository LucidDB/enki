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
 * Implements MOF's Aliases association interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class Aliases
    extends RefAssociationBase
    implements javax.jmi.model.Aliases
{
    public Aliases(javax.jmi.reflect.RefPackage container)
    {
        super(
            container,
            "importer",
            Multiplicity.UNIQUE_COLLECTION,
            "imported",
            Multiplicity.SINGLE);
    }

    public boolean exists(
        javax.jmi.model.Import importer,
        javax.jmi.model.Namespace imported)
    {
        return super.refLinkExists(importer, imported);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.Import>*/ getImporter(
        javax.jmi.model.Namespace imported)
    {
        return super.refQuery(
            "importer", imported);
    }

    public javax.jmi.model.Namespace getImported(
        javax.jmi.model.Import importer)
    {
        Collection<?> result = super.refQuery(
            "imported", importer);
        Iterator<?> iter = result.iterator();
        if (iter.hasNext()) {
            return (javax.jmi.model.Namespace)iter.next();
        }
        return null;
    }

    public boolean add(
        javax.jmi.model.Import importer,
        javax.jmi.model.Namespace imported)
    {
        return super.refAddLink(importer, imported);
    }

    public boolean remove(
        javax.jmi.model.Import importer,
        javax.jmi.model.Namespace imported)
    {
        return super.refRemoveLink(importer, imported);
    }
}

// End Aliases.java
