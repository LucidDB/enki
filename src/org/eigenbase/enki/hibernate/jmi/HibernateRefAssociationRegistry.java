/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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

import java.util.*;

import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateRefAssociationRegistry provides a mechanism for 
 * {@link org.eigenbase.enki.hibernate.storage.HibernateAssociation} instances 
 * to lookup their {@link HibernateRefAssociation}.  This is necessary because 
 * Hibernate allows for lazy initialization of objects through associations, 
 * so it's possible that a newly loaded {@link HibernateAssociation} will we 
 * loaded at a time when it's not possible to know which 
 * {@link HibernateRefAssociation} to associate with it.  
 * 
 * @author Stephan Zuercher
 */
public class HibernateRefAssociationRegistry
{
    private static HibernateRefAssociationRegistry instance = 
        new HibernateRefAssociationRegistry();
    
    private final HashMap<String, HibernateRefAssociation> registry;
    
    private HibernateRefAssociationRegistry()
    {
        this.registry = new HashMap<String, HibernateRefAssociation>();
    }
    
    public static HibernateRefAssociationRegistry instance()
    {
        return instance;
    }
    
    /**
     * Find the identified {@link HibernateRefAssociation}.
     * 
     * @param uid unique {@link HibernateRefAssociation} identifier
     * @return {@link HibernateRefAssociation} associated with UID
     * @throws InternalJmiError if the class is not found
     */
    public HibernateRefAssociation findRefAssociation(String uid)
    {
        HibernateRefAssociation refAssoc = registry.get(uid);
        if (refAssoc == null) {
            throw new InternalJmiError(
                "Cannot find HibernateRefAssociation identified by '" 
                + uid + "'");
        }
        
        return refAssoc;
    }
    
    /**
     * Register the given {@link HibernateRefAssociation}.
     * @param uid unique identifier for the given 
     *            {@link HibernateRefAssociation} 
     * @param refAssoc a {@link HibernateRefAssociation} 
     * @throws InternalJmiError on duplicate uid
     * @throws NullPointerException if either parameter is null
     */
    public void registerRefAssociation(
        String uid, HibernateRefAssociation refAssoc)
    {
        if (uid == null) {
            throw new NullPointerException("uid == null");
        }
        if (refAssoc == null) {
            throw new NullPointerException("refAssoc == null");
        }
        
        HibernateRefAssociation prev = registry.put(uid, refAssoc);
        if (prev != null) {
            throw new InternalJmiError(
                "HibernateRefAssociation (mofId " + prev.refMofId() +
                "; class " + prev.getClass().getName() +
                ") already identified by '" + uid +
                "'; Cannot replace it with HibernateRefAssociation (mofId " + 
                refAssoc.refMofId() + "; class " + 
                refAssoc.getClass().getName() + ")"); 
        }
    }
    
    /**
     * Unregister a previously 
     * {@link #registerRefAssociation(String, HibernateRefAssociation) registered} 
     * {@link HibernateRefAssociation}.
     * 
     * @param uid unique identifier for the HibernateRefAssociation
     */
    public void unregisterRefAssociation(String uid)
    {
        if (uid == null) {
            throw new NullPointerException("uid == null");
        }
        
        HibernateRefAssociation old = registry.remove(uid);
        if (old == null) {
            throw new InternalJmiError(
                "HibernateRefAssociation (uid " + uid + 
                ") was never registered");
        }
    }
}

// End HibernateRefAssociationRegistry.java
