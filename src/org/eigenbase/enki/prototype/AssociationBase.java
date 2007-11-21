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

/**
 * AssociationBase serves as a base class for any end-point of any type of
 * association.  It manages multiple associations by name.  Names are
 * typically related to the name of the accessor and mutator that modify
 * a particular association.  This base class allows various association
 * helper classes to operate one the parent and child of an association
 * without reflection.
 *  
 * @author Stephan Zuercher
 */
public abstract class AssociationBase implements Associable
{
    private final HashMap<String, Association> assocMap;
    
    protected AssociationBase()
    {
        this.assocMap = new HashMap<String, Association>();
    }
    
    public void setAssociation(String name, Association assoc)
    {
        assocMap.put(name, assoc);
    }
    
    public Association getAssociation(String name)
    {
        return assocMap.get(name);
    }
    
    public <E extends Association> E getAssociation(String name, Class<E> cls)
    {
        Association assoc = assocMap.get(name);
        if (assoc == null) {
            return null;
        }
        
        return cls.cast(assoc);
    }
}
