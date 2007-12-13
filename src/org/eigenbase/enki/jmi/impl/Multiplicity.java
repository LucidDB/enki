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
package org.eigenbase.enki.jmi.impl;

import javax.jmi.model.*;

/**
 * Multiplicity mirrors {@link MultiplicityType}, but is only used internally 
 * to initialize {@link RefAssociationBase} instances.
 * 
 * @author Stephan Zuercher
 */
public enum Multiplicity
{
    SINGLE(true, false, false),
    COLLECTION(false, false, false),
    ORDERED_COLLECTION(false, true, false),
    UNIQUE_COLLECTION(false, false, true),
    UNIQUE_ORDERED_COLLECTION(false, true, true);
    
    private final boolean isSingle;
    private final boolean isOrdered;
    private final boolean isUnique;
    
    private Multiplicity(boolean isSingle, boolean isOrdered, boolean isUnique)
    {
        this.isSingle = isSingle;
        this.isOrdered = isOrdered;
        this.isUnique = isUnique;
    }
    
    public boolean isSingle()
    {
        return isSingle;
    }
    
    public boolean isOrdered()
    {
        return isOrdered;
    }
    
    public boolean isUnique()
    {
        return isUnique;
    }
    
    public static Multiplicity fromMultiplicityType(
        MultiplicityType multiplicityType)
    {
        if (multiplicityType.getUpper() == 1) {
            return SINGLE;
        }
        
        if (multiplicityType.isOrdered()) {
            if (multiplicityType.isUnique()) {
                return UNIQUE_ORDERED_COLLECTION;
            } else {
                return ORDERED_COLLECTION;
            }
        } else {
            if (multiplicityType.isUnique()) {
                return UNIQUE_COLLECTION;
            } else {
                return COLLECTION;
            }
        }
    }
}

// End Multiplicity.java
