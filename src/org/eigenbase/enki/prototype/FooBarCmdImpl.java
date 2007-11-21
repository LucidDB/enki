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

public class FooBarCmdImpl extends AssociationBase implements FooBarCmd
{
    private long id;
    private String name;
    private int foo;
    private Long bar;
    private boolean foobar;
    
    public FooBarCmdImpl()
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
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getFoo()
    {
        return foo;
    }

    public void setFoo(int foo)
    {
        this.foo = foo;
    }

    public Long getBar()
    {
        return bar;
    }

    public void setBar(Long bar)
    {
        this.bar = bar;
    }

    public boolean getFoobar()
    {
        return foobar;
    }

    public void setFoobar(boolean foobar)
    {
        this.foobar = foobar;
    }
    
    public OneToManyAssociation getThingyAssociation()
    {
        return getAssociation(
            BarCmdImpl.THINGY_ASSOCIATION_NAME, OneToManyAssociation.class);
    }

    public void setThingyAssociation(OneToManyAssociation thingyAssociation)
    {
        setAssociation(BarCmdImpl.THINGY_ASSOCIATION_NAME, thingyAssociation);
    }

    public List<Thingy> getThingies()
    {
        return OneToManyAssociationHelper.getChildren(
            BarCmdImpl.THINGY_ASSOCIATION_NAME, this, Thingy.class);
    }

    public void setThingies(List<Thingy> thingies)
    {
        OneToManyAssociationHelper.replaceChildren(
            BarCmdImpl.THINGY_ASSOCIATION_NAME, this, thingies);
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b
            .append("FooBarCmd@")
            .append(getId())
            .append('(') 
            .append(getName()).append("; ") 
            .append(getFoo() + "; ") 
            .append(getBar() + "; ") 
            .append(getFoobar() + ") [");
        boolean first = true;
        for(Thingy thingy: getThingies()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append(thingy);
        }
        b.append(']');
        return b.toString();
    }
}

// End FooBarCmd.java
