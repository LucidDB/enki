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

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateRefClassRegistry provides a mechanism for 
 * {@link HibernateRefObject} instances to lookup their 
 * {@link HibernateRefClass}.  This is necessary because Hibernate allows
 * for lazy initialization of objects through associations, so it's possible
 * that a newly loaded {@link HibernateRefObject} will we loaded at a time
 * when it's not possible to know what {@link HibernateRefClass} to associate
 * with it.  
 * 
 * @author Stephan Zuercher
 */
public class HibernateRefClassRegistry
{
    private static HibernateRefClassRegistry instance = 
        new HibernateRefClassRegistry();
    
    private final HashMap<String, HibernateRefClass> registry;
    
    private HibernateRefClassRegistry()
    {
        this.registry = new HashMap<String, HibernateRefClass>();
    }
    
    public static HibernateRefClassRegistry instance()
    {
        return instance;
    }
    
    /**
     * Find the identified {@link RefClass}.
     * 
     * @param uid unique {@link HibernateRefClass} identifier
     * @return {@link HibernateRefClass} associated with UID
     * @throws InternalJmiError if the class is not found
     */
    public HibernateRefClass findRefClass(String uid)
    {
        HibernateRefClass refClass = registry.get(uid);
        if (refClass == null) {
            throw new InternalJmiError(
                "Cannot find HibernateRefClass identified by '" + uid + "'");
        }
        
        return refClass;
    }
    
    /**
     * Register the given {@link HibernateRefClass}.
     * @param uid unique identifier for the given {@link HibernateRefClass} 
     * @param refClass a {@link HibernateRefClass} 
     * @throws InternalJmiError on duplicate uid
     * @throws NullPointerException if either parameter is null
     */
    public void registerRefClass(String uid, HibernateRefClass refClass)
    {
        if (uid == null) {
            throw new NullPointerException("uid == null");
        }
        if (refClass == null) {
            throw new NullPointerException("refClass == null");
        }
        
        HibernateRefClass prev = registry.put(uid, refClass);
        if (prev != null) {
            throw new InternalJmiError(
                "HibernateRefClass (mofId " + prev.refMofId() + "; class " + 
                prev.getClass().getName() + ") already identified by '" + uid +
                "'; Cannot replace it with HibernateRefClass (mofId " + 
                refClass.refMofId() + "; class " + 
                refClass.getClass().getName() + ")"); 
        }
    }
}

// End HibernateRefClassRegistry.java
