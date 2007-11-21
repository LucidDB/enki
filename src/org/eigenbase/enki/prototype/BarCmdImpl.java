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

public class BarCmdImpl extends AssociationBase implements BarCmd
{
    public static final String THINGY_ASSOCIATION_NAME = "thingyAssociation";
    
    private long id;
    private String name;
    private Long bar;
    
    public BarCmdImpl()
    {
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id )
    {
        this.id = id;
    }

   public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if (Main.getSessionFactory() != null &&
            Main.getSessionFactory().getCurrentSession().getTransaction() != null) {
            System.out.println("set BarCmd.name");
        }
        this.name = name;
    }
    
    public Long getBar()
    {
        return bar;
    }

    public void setBar(Long bar)
    {
        this.bar = bar;
    }
    
    public OneToManyAssociation getThingyAssociation()
    {
        return getAssociation(
            THINGY_ASSOCIATION_NAME, OneToManyAssociation.class);
    }

    public void setThingyAssociation(OneToManyAssociation thingyAssociation)
    {
        setAssociation(THINGY_ASSOCIATION_NAME, thingyAssociation);
    }

    public List<Thingy> getThingies()
    {
        return OneToManyAssociationHelper.getChildren(
            THINGY_ASSOCIATION_NAME, this, Thingy.class);
    }

    public void setThingies(List<Thingy> thingies)
    {
        OneToManyAssociationHelper.replaceChildren(
            THINGY_ASSOCIATION_NAME, this, thingies);
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b
            .append("BarCmd@")
            .append(getId())
            .append('(') 
            .append(getName())
            .append("; ")
            .append(getBar())
            .append("): [");
        boolean first = true;
        List<Thingy> thingies = getThingies();
        if (thingies != null) {
            for(Thingy thingy: thingies) {
                if (first) {
                    first = false;
                } else {
                    b.append(", ");
                }
                b.append(thingy);
            }
        }
        b.append(']');
        
        return b.toString();
    }
}
