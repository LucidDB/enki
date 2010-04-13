/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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
package org.eigenbase.enki.hibernate.storage;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mdr.*;

/**
 * HibernateAssociationBase is an abstract base class for association storage
 * classes. 
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateAssociationBase 
    extends HibernateObject
    implements HibernateAssociation
{
    private final HibernateMDRepository repos;
    
    public HibernateAssociationBase()
    {
        this.repos = HibernateMDRepository.getCurrentRepository();
    }
    
    /** Name of the association type. */
    private String type;

    public String getType()
    {
        return type;
    }
    
    public void setType(String type)
    {
        this.type = type;
    }
    
    /**
     * Test two objects for equality.  Equality is based on 
     * {@link Object#equals(Object)}.  Null references are handled without
     * error.
     * 
     * @param a1 an object
     * @param a2 an object
     * @return true if a1.equals(a2) or a1 == null and a2 == null
     */
    protected static boolean equals(
        Object a1, Object a2)
    {
        if (a1 == null) {
            return a2 == null;
        } else if (a2 == null) {
            return false;
        } else {
            return a1.equals(a2);
        }
    }
    
    public Iterator<RefAssociationLink> linkIterator()
    {
        return getLinks().iterator();
    }
    
    
    /**
     * Retrieves a {@link HibernateMDRepository} instance.

     * @return the HibernateMDRepository that stores this association
     */
    @Override
    public HibernateMDRepository getHibernateRepository()
    {
        return repos;
    }
 
    /**
     * Retrieves this association's {@link HibernateMDRepository} as a
     * {@link EnkiMDRepository}.
     * 
     * @return the EnkiMDRepository that stores this association
     */
    @Override
    public EnkiMDRepository getRepository()
    {
        return repos;
    }
  
    /**
     * Retrieves a {@link HibernateMDRepository} from an element of either
     * end of the association.
     * 
     * @param end one of this association's ends
     * @return the HibernateMDRepository that stores the end, and hence this
     *         association
     */
    protected HibernateMDRepository getHibernateRepository(
        HibernateAssociable end)
    {
        return ((HibernateObject)end).getHibernateRepository();
    }
    
    @Override
    protected final void checkConstraints(
        List<JmiException> errors, boolean deepVerify)
    {
        // HibernateAssociation is a HibernateObject, but really isn't meant
        // to be a HibernateObject, and so this method should not be called.
        throw new IllegalStateException();
    }
}

// End HibernateAssociationBase.java
