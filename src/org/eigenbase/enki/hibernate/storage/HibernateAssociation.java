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

/**
 * HibernateAssociation represents an association storage class. 
 * 
 * @author Stephan Zuercher
 */
public interface HibernateAssociation extends HibernateIdentifiable
{
    public String getType();
    
    /**
     * Generically add an association between left and right.  One of the
     * parameters left or right must be part of this association already
     * unless this association contains no members at all (e.g., it is newly
     * constructed).
     *  
     * @param left left-side of association
     * @param right right-side of association
     */
    public boolean add(HibernateAssociable left, HibernateAssociable right);
    
    /**
     * Generically remove an association between left and right.  The
     * parameters left and right must be part of this association already.
     *  
     * @param left left-side of association
     * @param right right-side of association
     * @return true if the association was found and removed, false otherwise
     */
    public boolean remove(HibernateAssociable left, HibernateAssociable right);
    
    /**
     * Fix up association after one end has been removed via an operation
     * on the collection returned via {@link #get(HibernateAssociable)}.
     * This is supported only for unique collections to simplify 
     * {@link CollectionProxy.IteratorProxy}.
     *
     * @param left left-side of association
     * @param right right-side of association
     */
    public void postRemove(
        HibernateAssociable left, HibernateAssociable right);
    
    /**
     * Generically removes all associations of this type for the given 
     * association end.  The item may be at either end of the association.
     * The cascade delete option is useful when this association represents
     * a component attribute.
     * 
     * @param item an association end
     * @param isFirstEnd if item is end 1 of the association vs. end 2
     * @param cascadeDelete if true, call {@link RefObject#refDelete()} on 
     *                      each removed item
     */
    public void removeAll(
        HibernateAssociable item, boolean isFirstEnd, boolean cascadeDelete);
    
    /**
     * Generically remove all associations related to the given item, which
     * must be the first end of the association.
     */
    public void clear(HibernateAssociable item);
    
    public Collection<RefAssociationLink> getLinks();
    
    public Iterator<RefAssociationLink> linkIterator();
    
    /**
     * Query this association and return the objects of the requested end.
     * 
     * @param returnSecondEnd if true return the end2 object(s)
     * @return collection of objects for the requested end
     */
    public Collection<? extends RefObject> query(boolean returnSecondEnd);
    
    /**
     * Get a Collection of the remote end(s) of the association for the given 
     * item.
     * 
     * @return Collection of items associated with item
     */
    public Collection<HibernateAssociable> get(HibernateAssociable item);
    
    /**
     * Retrieve the class that represents the type of this instance.  Separate
     * from {@link Object#getClass()} because Hibernate can and will proxy the
     * class and we don't want to accidentally retrieve the wrong type.
     * 
     * @return class that represents this object's type
     */
    public abstract Class<? extends HibernateAssociation> getInstanceClass();
    
    /**
     * Get basic association type.
     * 
     * @return the {@link Kind} that represents this association
     */
    public abstract HibernateAssociation.Kind getKind();
    
    public abstract String getTable();
    
    public abstract String getCollectionTable();

    public abstract String getCollectionName();
    
    /**
     * Kind represents the basic multiplicity of an association. It
     * is constant for a given type of association.
     */
    public enum Kind {
        ONE_TO_ONE,
        ONE_TO_MANY,
        ONE_TO_MANY_HIGH_CARDINALITY,
        ONE_TO_MANY_ORDERED,
        MANY_TO_MANY,
        MANY_TO_MANY_ORDERED;
    }
}

// End HibernateAssociation.java
