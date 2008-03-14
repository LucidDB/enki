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

import org.eigenbase.enki.codegen.*;

/**
 * Multiplicity mirrors {@link MultiplicityType}, but is only used internally 
 * to initialize {@link RefAssociationBase} instances.
 * 
 * @author Stephan Zuercher
 */
public class Multiplicity
{
    public static final int UNBOUNDED = -1;
    
    private final int lower;
    private final int upper;
    private final boolean ordered;
    private final boolean unique;
    
    public Multiplicity(int lower, int upper, boolean ordered, boolean unique)
    {
        this.lower = lower;
        this.upper = upper;
        this.ordered = ordered;
        this.unique = unique;
        
        // lower cannot be negative or unbounded
        assert(lower >= 0);
        
        // upper may not be zero and must be >= lower or unbounded
        assert(upper != 0);
        assert(upper >= lower || upper == UNBOUNDED);
        
        if (upper == 1) {
            assert(!ordered);
            assert(!unique);
        }
    }
    
    public boolean isSingle()
    {
        return upper == 1;
    }
    
    public boolean isRequired()
    {
        return lower != 0;
    }
    
    public boolean isOrdered()
    {
        return ordered;
    }
    
    public boolean isUnique()
    {
        return unique;
    }
    
    public int getLower()
    {
        return lower;
    }
    
    public int getUpper()
    {
        return upper;
    }
    
    public boolean isUpperBounded()
    {
        return upper != UNBOUNDED;
    }
    
    public String toInstantiationString()
    {
        return toInstantiationString(false);
    }
    
    public String toInstantiationString(boolean assumeImport)
    {
        String className = 
            assumeImport ? getClass().getSimpleName() : getClass().getName();
        
        StringBuilder b = new StringBuilder();
        b
            .append("new ")
            .append(className)
            .append("(")
            .append(lower)
            .append(", ")
            .append(upper)
            .append(", ")
            .append(ordered)
            .append(", ")
            .append(unique)
            .append(")");
        return b.toString();
    }
    
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b
            .append("[")
            .append(lower)
            .append(", ")
            .append(upper == UNBOUNDED ? "unbounded": upper)
            .append("]; ")
            .append(ordered ? "ordered" : "unordered")
            .append("; ")
            .append(unique ? "unique" : "allow-dups");
        return b.toString();
    }
    
    public boolean equals(Object other)
    {
        Multiplicity that = (Multiplicity)other;
        
        return 
            this.lower == that.lower &&
            this.upper == that.upper &&
            this.ordered == that.ordered &&
            this.unique == that.unique;
    }
    
    public int hashCode()
    {
        return 
            ((lower << 2) ^ (upper << 10)) | 
            (ordered ? 2 : 0) | 
            (unique ? 1 : 0);
    }
    
    public static Multiplicity fromMultiplicityType(
        MultiplicityType multiplicityType) throws GenerationException
    {
        int lower = multiplicityType.getLower();
        int upper = multiplicityType.getUpper();
        boolean ordered = multiplicityType.isOrdered();
        boolean unique = multiplicityType.isUnique();

        if (upper == 1) {
            // MOF requires this to be true, but XMI from ArgoUML has unique
            // set sometimes and I don't see how to fix that.  So silently
            // modify these to keep the constructor's assertions happy.
            ordered = false;
            unique = false;
        }
        
        return new Multiplicity(lower, upper, ordered, unique);
    }
}

// End Multiplicity.java
