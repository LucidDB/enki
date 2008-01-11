/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007-2007 The Eigenbase Project
// Copyright (C) 2007-2007 Disruptive Tech
// Copyright (C) 2007-2007 LucidEra, Inc.
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 2.1 of the License, or (at
// your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
*/
package org.eigenbase.enki.hibernate.jmi;

import javax.jmi.reflect.*;
import javax.jmi.reflect.RefAssociationLink;

import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;

/**
 * HibernateRefPackage provides a Hibernate-based implementations of 
 * {@link RefPackage}.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateRefPackage 
    extends RefPackageBase 
    implements RefPackage
{
    protected HibernateRefPackage(RefPackage container)
    {
        super(container);
    }
    
    public void refDelete()
    {
        deleteObjectsRecursively();
    }
    
    private void deleteObjectsRecursively()
    {
        for(RefPackage refPackage: 
                GenericCollections.asTypedCollection(
                    refAllPackages(), RefPackage.class))
        {
            if (refPackage instanceof HibernateRefPackage) {
                ((HibernateRefPackage)refPackage).deleteObjectsRecursively();
            }
            
            // TODO: transient packages deletion
        }
        
        for(RefAssociation refAssociation:
                GenericCollections.asTypedCollection(
                    refAllAssociations(), RefAssociation.class))
        {
            for(RefAssociationLink link: 
                    GenericCollections.asTypedCollection(
                        refAssociation.refAllLinks(), RefAssociationLink.class))
            {
                refAssociation.refRemoveLink(
                    link.refFirstEnd(), link.refSecondEnd());
            }
        }
        
        for(RefClass refClass: 
                GenericCollections.asTypedCollection(
                    refAllClasses(), RefClass.class))
        {
            for(RefObject refObject:
                    GenericCollections.asTypedCollection(
                        refClass.refAllOfClass(), RefObject.class))
            {
                refObject.refDelete();
            }
        }        
    }
}
