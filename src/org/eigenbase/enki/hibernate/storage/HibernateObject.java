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

import java.util.logging.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.jmi.impl.*;
import org.hibernate.*;

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
     * Saves this object to the current Hibernate 
     * {@link org.hibernate.Session}.
     * 
     * @throws IllegalStateException if no write transaction is in progress
     */
    public void save()
    {
        save(getHibernateRepository());
    }
    
    protected void save(HibernateMDRepository repos)
    {
        if (deleted) {
            throw new IllegalStateException("Saving deleted object");
        }
        
        if (saved) {
            log.finer(
                "Double save on '" + getClass().getName() + "':" + refMofId());
            return;
        }

        repos.checkTransaction(true);
        
        Session session = repos.getCurrentSession();
        if (getMofId() == 0L) {
            long mofId = repos.getMofIdGenerator().nextMofId();
            
            setMofId(mofId);
        
            if (!(this instanceof HibernateAssociation)) {
                MofIdTypeMapping mapping = new MofIdTypeMapping();
                mapping.setMofId(mofId);
                mapping.setTypeName(this.getClass().getName());
                session.save(mapping);
            }
        }
        
        session.save(this);
        
        log.finer(
            "Save on '" + getClass().getName() + "':" + refMofId());
        
        saved = true;
    }
    
    
    /**
     * Deletes this object from the current Hibernate 
     * {@link org.hibernate.Session}.
     * 
     * @throws IllegalStateException if no write transaction is in progress
     */
    public void delete()
    {
        delete(getHibernateRepository());
    }
    
    protected void delete(HibernateMDRepository repos)
    {
        if (deleted) {
            return;
        }
        
        repos.checkTransaction(true);
        
        Session session = repos.getCurrentSession();
        session.delete(this);
        
        if (!(this instanceof HibernateAssociation)) {
            Query query = session.getNamedQuery("TypeMappingByMofId");
            query.setLong("mofId", getMofId());
            
            MofIdTypeMapping mapping = (MofIdTypeMapping)query.uniqueResult();
            session.delete(mapping);
        }
        
        deleted = true;        
    }
    
    /**
     * Returns the {@link HibernateMDRepository} that stores this object.
     * This method is a convenience method that simply casts the result of 
     * {@link #getRepository()} to HibernateMDRepository.
     * 
     * @return the HibernateMDRepository that stores this object.
     */
    public HibernateMDRepository getHibernateRepository()
    {
        return (HibernateMDRepository)getRepository();
    }
}

// End HibernateObject.java
