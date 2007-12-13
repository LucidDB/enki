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


/**
 * HibernateOneToOneAssociation extends HibernateAssociation and stores
 * one-to-one associations. 
 * 
 * @author Stephan Zuercher
 */
public class HibernateOneToOneAssociation
    extends HibernateAssociation
{
    private HibernateAssociable left;
    private HibernateAssociable right;
    
    public HibernateOneToOneAssociation()
    {
    }
    
    public HibernateAssociable getLeft()
    {
        return left;
    }
    
    public void setLeft(HibernateAssociable left)
    {
        this.left = left;
    }
    
    public <E> E getLeft(Class<E> cls)
    {
        return cls.cast(left);
    }

    public HibernateAssociable getRight()
    {
        return right;
    }

    public void setRight(HibernateAssociable right)
    {
        this.right = right;
    }
    
    public <E> E getRight(Class<E> cls)
    {
        return cls.cast(right);
    }

    @Override
    public void add(HibernateAssociable newLeft, HibernateAssociable newRight)
    {
        final String type = getType();
        
        if (equals(left, newLeft) && equals(right, newRight)) {
            // Nothing to do.
            assert(this.equals(newLeft.getAssociation(type)));
            assert(this.equals(newRight.getAssociation(type)));
            return;
        }
        
        if (equals(getLeft(), newLeft)) {
            assert(this.equals(newLeft.getAssociation(type)));
            
            // Remove association from the old right, if any.
            if (getRight() != null) {
                getRight().setAssociation(type, null);
            }

            // Remove any previous association to the new right. 
            HibernateOneToOneAssociation newRightAssoc = 
                (HibernateOneToOneAssociation)newRight.getAssociation(type);
            if (newRightAssoc != null) {
                newRightAssoc.getLeft().setAssociation(type, null);
            }
            
            setRight(newRight);
            newRight.setAssociation(type, this);
        } else {
            assert(equals(getRight(), newRight));
            assert(this.equals(newRight.getAssociation(type)));
            
            // Remove association from the old left, if any.
            if (getLeft() != null) {
                getLeft().setAssociation(type, null);
            }
            
            // Remove any previous association to the new left.
            HibernateOneToOneAssociation newLeftAssoc =
                (HibernateOneToOneAssociation)newLeft.getAssociation(type);
            if (newLeftAssoc != null) {
                newLeftAssoc.getRight().setAssociation(type, null);
            }
            
            setLeft(newLeft);
            newLeft.setAssociation(type, this);
        }

        // REVIEW: SWZ: 11/14/2007: get Hibernate session and delete dangling
        // associations? Or does that happen auto-magically?
    }

    @Override
    public void add(
        int index, HibernateAssociable left, HibernateAssociable right)
    {
        add(left, right);
    }
    
    @Override
    public boolean remove(HibernateAssociable left, HibernateAssociable right)
    {
        final String type = getType();

        assert(equals(getLeft(), left) && equals(getRight(), right));
        assert(this.equals(left.getAssociation(type)));
        assert(this.equals(right.getAssociation(type)));
        
        getLeft().setAssociation(type, null);
        getRight().setAssociation(type, null);
        
        // REVIEW: SWZ: 11/14/2007: get Hibernate session and delete ourselves?
        // Or does that happen auto-magically?
        
        return true;
    }
    
    @Override
    public void clear(HibernateAssociable item)
    {
        if (equals(getLeft(), item) || equals(getRight(), item)) {
            remove(getLeft(), getRight());
        }
    }
    
    @Override
    public List<HibernateAssociable> get(HibernateAssociable item)
    {
        if (equals(getLeft(), item)) {
            return Collections.singletonList(getRight());
        } else if (equals(getRight(), item)) {
            return Collections.singletonList(getLeft());
        } else {
            return Collections.emptyList();
        }
    }
}

// End HibernateOneToOneAssociation.java
