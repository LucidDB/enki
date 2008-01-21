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

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.hibernate.*;
import org.hibernate.criterion.*;

/**
 * HibernateRefObject provides a Hibernate-based implementation of 
 * {@link RefObject}.
 *
 * @author Stephan Zuercher
 */
public abstract class HibernateRefObject
    extends HibernateObject
    implements RefObject
{
    @Override
    public RefClass refClass()
    {
        return HibernateRefClassRegistry.instance().findRefClass(
            getClassIdentifier());
    }
    
    @Override
    public void refDelete()
    {
        removeAssociations();
        
        super.delete();
    }
    
    /**
     * Looks up the entity with the given type whose given property matches
     * this instance's MOF ID.
     * 
     * @param ownerClass composite owner class (e.g., a class whose instances
     *                   may contain this instance as an attribute value)
     * @param ownerPropertyName property in the owner class that may refer to
     *                          this instance
     * @return the unique instance of the owner class that refers to this 
     *         instance as a component via the given property, or null if no
     *         such instance exists
     */
    protected final RefObject findCompositeOwner(
        Class<?> ownerClass, String ownerPropertyName)
    {
        // REVIEW: SWZ: 1/11/08: Consider a way of representing attributes
        // with MofClass types that would remove the need for this query.
        // For instance, an attribute with MofClass type could be modeled
        // as a 1-to-1 association, which would give the component instance
        // a back-link back to the owner.  The down-side is an additional
        // row to represent the association and still more complex code
        // generation.
        assert(RefObject.class.isAssignableFrom(ownerClass));
        
        Session session = HibernateMDRepository.getCurrentSession();

        Criteria criteria = 
            session.createCriteria(ownerClass)
            .add(Restrictions.eq(ownerPropertyName, this));
        criteria.setCacheable(true);
        
        return (RefObject)criteria.uniqueResult();
    }
    
    @Override
    public RefFeatured refOutermostComposite()
    {
        RefFeatured immediateComposite = refImmediateComposite();
        if (immediateComposite == null) {
            return this;
        } else if (immediateComposite instanceof RefObject) {
            return ((RefObject)immediateComposite).refOutermostComposite();
        }
        
        // must be a RefClass
        return immediateComposite;
    }
    
    protected abstract void removeAssociations();
    
    protected abstract String getClassIdentifier();
}
