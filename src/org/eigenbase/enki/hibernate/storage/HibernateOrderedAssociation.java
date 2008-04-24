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
package org.eigenbase.enki.hibernate.storage;

import java.util.*;

/**
 * HibernateOrderedAssociation represents an ordered association storage
 * object.
 * 
 * @author Stephan Zuercher
 */
public interface HibernateOrderedAssociation extends HibernateAssociation
{
    /**
     * Generically add an association between left and right at the given
     * index.  One of the parameters left or right must be part of this 
     * association already.
     *  
     * @param index position of new item
     * @param left left-side of association
     * @param right right-side of association
     */
    public void add(
        int index, HibernateAssociable left, HibernateAssociable right);
    
    /**
     * Generically remove an association between left and right.  The
     * parameters left and right must be part of this association already.
     *  
     * @param index position of item to remove
     * @param left left-side of association
     * @param right right-side of association
     * @return true if the association was found and removed, false otherwise
     */
    public boolean remove(
        int index, HibernateAssociable left, HibernateAssociable right);


    /**
     * Get a List of the remote end(s) of the association for the given item.
     * 
     * @return List of items associated with item
     */
    public List<HibernateAssociable> getOrdered(HibernateAssociable item);
}

// End HibernateOrderedAssociation.java
