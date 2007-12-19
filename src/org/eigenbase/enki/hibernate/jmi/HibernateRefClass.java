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
    private final String classImplementationName;
    private final String classInterfaceName;
    
    protected HibernateRefClass(
        RefPackage container,
        Class<?> classImplementationName,
        Class<?> classInterfaceName)
    {
        super(container);
        
        this.classImplementationName = classImplementationName.getName();
        this.classInterfaceName = classInterfaceName.getName();
        
        HibernateRefClassRegistry.instance().registerRefClass(
            getClassIdentifier(),
            this);
        
        // TODO: 
        // x1. Create RefClass registry class (singleton or all-static)
        // x2. Insert [getClassIdentifier(), this] into the registry here.
        // 3. Burn a class identifier (meta-model specific) into each 
        //    class proxy (returned by getClassIdentifier()) 
        // 4. Burn a static class identifier into each instance class and
        //    use it in HibernateObject to look up the RefClass
    }

    protected abstract String getClassIdentifier();
    
    @Override
    @SuppressWarnings("unchecked")
    public Collection<?> refAllOfClass()
    {
        Session session = HibernateMDRepository.getCurrentSession();
        
        // Polymorphic query against implementation type will return only
        // instances of this exact class.
        Query query = session.createQuery("from " + classImplementationName);

        return Collections.unmodifiableList(query.list());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<?> refAllOfType()
    {
        Session session = HibernateMDRepository.getCurrentSession();
        
        // Polymorphic query against interface type will return instances
        // of this class and any other implementation of the interface (e.g.,
        // all subclasses).
        Query query = session.createQuery("from " + classInterfaceName);
        
        return Collections.unmodifiableList(query.list());
    }
    
    
}
