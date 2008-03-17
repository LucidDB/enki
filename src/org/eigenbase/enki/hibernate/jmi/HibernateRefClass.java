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

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.jmi.impl.*;
import org.hibernate.*;

/**
 * HibernateRefClass provides a Hibernate-based implementation of 
 * {@link RefClass}.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateRefClass 
    extends RefClassBase 
    implements RefClass
{
    private final String allOfClassQueryName;
    private final String allOfTypeQueryName;
    
    protected HibernateRefClass(
        RefPackage container,
        Class<?> classImplementationName,
        Class<?> classInterfaceName)
    {
        super(container);
        
        if (classImplementationName != null) {
            this.allOfClassQueryName = 
                classImplementationName.getName() + "." + 
                HibernateMappingHandler.QUERY_NAME_ALLOFCLASS;
        } else {
            this.allOfClassQueryName = null;
        }
        
        this.allOfTypeQueryName = 
            classInterfaceName.getName() + "." + 
            HibernateMappingHandler.QUERY_NAME_ALLOFTYPE;
        
        getHibernateRepository().registerRefClass(
            getClassIdentifier(),
            this);
    }

    protected abstract String getClassIdentifier();
    
    @Override
    @SuppressWarnings("unchecked")
    public Collection<?> refAllOfClass()
    {
        if (allOfClassQueryName != null) {
            HibernateMDRepository repos = getHibernateRepository();
            
            repos.checkTransaction(false);
            
            Collection<?> cacheResult = repos.lookupAllOfClassResult(this);
            if (cacheResult != null) {
                return cacheResult;
            }
            
            Session session = repos.getCurrentSession();
            
            Query query = session.getNamedQuery(allOfClassQueryName);

            Collection<?> result = Collections.unmodifiableList(query.list());

            repos.storeAllOfClassResult(this, result);
            
            return result;
        }
        
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<?> refAllOfType()
    {
        HibernateMDRepository repos = getHibernateRepository();

        repos.checkTransaction(false);
        
        Collection<?> cacheResult = repos.lookupAllOfTypeResult(this);
        if (cacheResult != null) {
            return cacheResult;
        }
        
        Session session = repos.getCurrentSession();
        
        Query query = session.getNamedQuery(allOfTypeQueryName);
        
        Collection<?> result = Collections.unmodifiableList(query.list());
        
        repos.storeAllOfTypeResult(this, result);
        
        return result;
    }
    
    
    protected HibernateMDRepository getHibernateRepository()
    {
        return (HibernateMDRepository)getRepository();
    }
}
