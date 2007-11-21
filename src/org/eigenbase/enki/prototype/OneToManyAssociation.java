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
package org.eigenbase.enki.prototype;

import java.util.*;

public class OneToManyAssociation implements Association
{
    private long id;
    private String type;
    private Object parent;
    
    // TODO: wrap children in a list impl that automatically adds.removes the 
    // children from the association when add(Object)/remove(int) is called.
    private List<Associable> children;

    public OneToManyAssociation()
    {
    }
    
    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }
    
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
    
    public Object getParent()
    {
        return parent;
    }

    public void setParent(Object parent)
    {
        this.parent = parent;
    }

    public List<Associable> getChildren()
    {
        return children;
    }

    /**
     * @throws ClassCastException if any child is not a <tt>cls</tt>
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> getChildren(Class<E> cls)
    {
        for(Object child: children) {
            cls.cast(child);
        }

        return (List<E>)children;
    }

    public void setChildren(List<Associable> children)
    {
        System.out.println("notify-special-case");
        this.children = new NotifyingArrayList<Associable>(children);
    }

    public <E extends Associable> void addChild(E child)
    {
        if (children == null) {
            children = 
                new NotifyingArrayList<Associable>(new ArrayList<Associable>());
        }

        children.add(child);
    }

    public boolean removeChild(Associable child)
    {
        if (children == null) {
            return false;
        }

        return children.remove(child);
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder();

        b
            .append("One-To-Many@")
            .append(id)
            .append("(")
            .append(getParent())
            .append("->[");
        boolean first = true;
        for(Object child: getChildren()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append(child);
        }
        b.append(']');
        return b.toString();
    }
}

// End OneToManyAssociation.java
