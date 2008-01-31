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

import java.util.*;
import java.util.logging.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateObject is a base class for all stored model entities.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateObject extends RefObjectBase
{
    private static final Logger log = 
        Logger.getLogger(HibernateObject.class.getName());
    
    private boolean saved;
    private boolean deleted;
    
    protected HibernateObject()
    {
        super((MetamodelInitializer)null);
    }
    
    /**
     * Save this object to the current Hibernate {@link Session}.
     * 
     * @throws IllegalStateException if no write transaction is in progress
     */
    public void save()
    {
        if (deleted) {
            throw new IllegalStateException("Saving deleted object");
        }
        
        if (saved) {
            log.finer(
                "Double save on '" + getClass().getName() + "':" + refMofId());
            return;
        }

        if (!HibernateMDRepository.isWriteTransaction()) {
            throw new IllegalStateException("Not in write transaction");
        }
        
        if (getMofId() == 0L) {
            long mofId = HibernateMDRepository.getMofIdGenerator().nextMofId();
            
            setMofId(mofId);
        }
        
        HibernateMDRepository.getCurrentSession().save(this);
        
        log.finer(
            "Save on '" + getClass().getName() + "':" + refMofId());
        
        saved = true;
    }
    
    public void delete()
    {
        if (deleted) {
            return;
        }
        
        if (!HibernateMDRepository.isWriteTransaction()) {
            throw new IllegalStateException("Not in write transaction");
        }
        
        HibernateMDRepository.getCurrentSession().delete(this);
        
        deleted = true;        
    }
    
    /**
     * Test if the constraints on this object have been met.  Called just
     * before the object is persisted to the database.
     * 
     * @param errors list of errors that the object may add to
     * @return true if constraints are met, false otherwise
     */
    public abstract boolean checkConstraints(List<String> errors);
}

// End HibernateObject.java
