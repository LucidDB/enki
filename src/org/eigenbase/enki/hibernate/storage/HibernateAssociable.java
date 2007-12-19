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
package org.eigenbase.enki.hibernate.storage;

import javax.jmi.reflect.*;

/**
 * HibernateAssociable represents a stored class that will participate in an 
 * association.
 * 
 * @author Stephan Zuercher
 */
public interface HibernateAssociable extends RefObject
{
    /**
     * Retrieves the {@link HibernateAssociation} instance associated with
     * the given type of association for this instance.
     *  
     * @param type name of the type of association to retrieve
     * @return the HibernateAssociation for the given name or null if not set
     */
    public HibernateAssociation getAssociation(String type);

    /**
     * Set the {@link HibernateAssociation} associated with the given type of 
     * association for this instance. 
     *  
     * @param type name of the type of association to set
     * @param assoc the association to store
     */
    public void setAssociation(String type, HibernateAssociation assoc);

    /**
     * Retrieves or creates the {@link HibernateAssociation} instance 
     * associated with the given type association for this instance.
     * 
     * @param type name of the type of association to get or create
     * @return the HibernateAssociation for the given name
     */
    public HibernateAssociation getOrCreateAssociation(String type);
}

// End HibernateAssociable.java
