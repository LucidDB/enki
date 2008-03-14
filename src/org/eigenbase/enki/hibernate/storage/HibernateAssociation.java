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

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mdr.*;

/**
 * HibernateAssociation is an abstract base class for association storage
 * classes. 
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateAssociation extends HibernateObject
{
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
     * Generically add an association between left and right.  One of the
     * parameters left or right must be part of this association already
     * unless this association contains no members at all (e.g., it is newly
     * constructed).
     *  
     * @param left left-side of association
     * @param right right-side of association
     */
    public abstract boolean add(
        HibernateAssociable left, HibernateAssociable right);
    
    /**
     * Generically add an association between left and right at the given
     * index.  One of the parameters left or right must be part of this 
     * association already.
     *  
     * @param index position of new item
     * @param left left-side of association
     * @param right right-side of association
     */
    public abstract void add(
        int index, HibernateAssociable left, HibernateAssociable right);
    
    /**
     * Generically remove an association between left and right.  The
     * parameters left and right must be part of this association already.
     *  
     * @param left left-side of association
     * @param right right-side of association
     * @return true if the association was found and removed, false otherwise
     */
    public abstract boolean remove(
        HibernateAssociable left, HibernateAssociable right);
    
    /**
     * Generically remove an association between left and right.  The
     * parameters left and right must be part of this association already.
     *  
     * @param index position of item to remove
     * @param left left-side of association
     * @param right right-side of association
     * @return true if the association was found and removed, false otherwise
     */
    public abstract boolean remove(
        int index, HibernateAssociable left, HibernateAssociable right);

    /**
     * Generically removes all associations of this type for the given 
     * association end.  The item may be at either end of the association.
     * The cascade delete option is useful when this association represents
     * a component attribute.
     * 
     * @param item an association end
     * @param cascadeDelete if true, call {@link RefObject#refDelete()} on 
     *                      each removed item
     */
    public abstract void removeAll(
        HibernateAssociable item, boolean cascadeDelete);
    
    /**
     * Generically remove all associations related to the given item, which
     * must be the first end of the association.
     */
    public abstract void clear(HibernateAssociable item);
    
    /**
     * Get a List of the remote end(s) of the association for the given item.
     * 
     * @return List of items associated with item
     */
    protected abstract List<HibernateAssociable> get(HibernateAssociable item);
    
    /**
     * Test two HibernateAssociable objects for equality.  Equality is based
     * on {@link Object#equals(Object)}.  Null references are handled without
     * error.
     * 
     * @param a1 a HibernateAssociable
     * @param a2 a HibernateAssociable
     * @return true if a1.equals(a2) or a1 == null and a2 == null
     */
    protected static boolean equals(
        HibernateAssociable a1, HibernateAssociable a2)
    {
        if (a1 == null) {
            return a2 == null;
        } else if (a2 == null) {
            return false;
        } else {
            return a1.equals(a2);
        }
    }

    /**
     * Test two HibernateAssociation objects for equality.  Equality is based
     * on {@link Object#equals(Object)}.  Null references are handled without
     * error.
     * 
     * @param a1 a HibernateAssociation
     * @param a2 a HibernateAssociation
     * @return true if a1.equals(a2) or a1 == null and a2 == null
     */
    protected static boolean equals(
        HibernateAssociation a1, HibernateAssociation a2)
    {
        if (a1 == null) {
            return a2 == null;
        } else if (a2 == null) {
            return false;
        } else {
            return a1.equals(a2);
        }
    }
    
    public abstract Collection<RefAssociationLink> getLinks();
    
    public Iterator<RefAssociationLink> linkIterator()
    {
        return getLinks().iterator();
    }
    
    /**
     * Query this association and return the objects of the requested end.
     * 
     * @param returnSecondEnd if true return the end2 object(s)
     * @return collection of objects for the requested end
     */
    public abstract List<? extends RefObject> query(boolean returnSecondEnd);

    /**
     * Retrieves a {@link HibernateMDRepository} instance via one of this
     * association's ends.  This override is necessary since instances of
     * HibernateAssociation do not have a reference to a RefAssociationBase.
     * Instead the HibernateMDRepository is obtained via one of the members
     * of the association.

     * @return the HibernateMDRepository that stores this association
     */
    @Override
    protected HibernateMDRepository getHibernateRepository()
    {
        // REVIEW: SWZ: 2008-02-12: Could be more efficient to use an abstract
        // method that returns any end from the association.  Here we query
        // the first end, and if no object is found (usually because this
        // association is newly created) we query the other end (where there
        // must be an object even at create time).
        HibernateAssociable end;
        Iterator<? extends RefObject> iter = query(false).iterator();
        if (iter.hasNext()) {
            end = (HibernateAssociable)iter.next();
        } else {
            iter = query(true).iterator();
            if (iter.hasNext()) {
                end = (HibernateAssociable)iter.next();
            } else {
                throw new InternalMdrError("empty association");
            }
        }
        
        return getHibernateRepository(end);
    }

    /**
     * Retrieves this association's {@link HibernateMDRepository} as a
     * {@link EnkiMDRepository}.
     * 
     * @return the EnkiMDRepository that stores this association
     */
    @Override
    protected EnkiMDRepository getRepository()
    {
        return getHibernateRepository();
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

// End HibernateAssociation.java
