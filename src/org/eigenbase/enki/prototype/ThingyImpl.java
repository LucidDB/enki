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

public class ThingyImpl extends AssociationBase implements Thingy
{
    private long id;
    private String name;
    
    public ThingyImpl()
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

    public OneToManyAssociation getBarAssociation()
    {
        return getAssociation("barAssociation", OneToManyAssociation.class);
    }

    public void setBarAssociation(OneToManyAssociation barAssociation)
    {
        setAssociation("barAssociation", barAssociation);
    }

    public BarCmd getParent()
    {
        return (BarCmd)getBarAssociation().getParent();
    }

    public void setParent(BarCmd parent)
    {
        // Is there an old parent?
        if (getBarAssociation() == null) {
            // No.  Does the new parent have any children?
            if (parent.getThingyAssociation() == null) {
                // No.  Set the chlidren list to just this.
                ArrayList<Thingy> thingies = new ArrayList<Thingy>();
                thingies.add(this);
                parent.setThingies(thingies);
            } else {
                // Yes.  Add ourselves as a child.
                parent.getThingyAssociation().addChild(this);
            }

            // Note child-to-parent association.
            setBarAssociation(parent.getThingyAssociation());
        } else {
            // Yes.
            BarCmd oldParent = getParent();

            // Old same as new?
            if (oldParent == parent) {
                // Do nothing.
                return;
            }

            // Remove this from the old parent.
            oldParent.getThingyAssociation().removeChild(this);

            // Set new child-to-parent association.
            setBarAssociation(parent.getThingyAssociation());

            // Add ourselves to the association.
            getBarAssociation().addChild(this);
        }
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b
            .append("Thingy@")
            .append(getId())
            .append('(') 
            .append(name)
            .append(')');
        return b.toString();
    }
}
