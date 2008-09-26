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
package org.eigenbase.enki.codegen;

import javax.jmi.model.*;

import org.eigenbase.enki.util.*;

/**
 * AssociationInfoImpl contains commonly used information about a MOF 
 * {@link Association} and and its {@link AssociationEnd} instances.
 * 
 * @author Stephan Zuercher
 */
public class AssociationInfoImpl implements AssociationInfo
{
    private final Association assoc;
    private final String assocInterfaceName;
    private final AssociationKindEnum kind;
    private final AssociationEnd[] ends;
    private final String[] types;
    private final String[] identifiers;
    private final String baseName;
    
    public AssociationInfoImpl(Association assoc)
    {
        this.assoc = assoc;
        this.assocInterfaceName = CodeGenUtils.getTypeName(assoc);
        this.kind = CodeGenUtils.getAssociationKind(assoc);
        this.ends = CodeGenUtils.getAssociationEnds(assoc);

        this.identifiers = new String[] {
            CodeGenUtils.getSimpleTypeName(ends[0]),
            CodeGenUtils.getSimpleTypeName(ends[1]),
        };
        
        this.types = new String[] {
            CodeGenUtils.getTypeName(ends[0].getType()),
            CodeGenUtils.getTypeName(ends[1].getType()),
        };
        
        this.baseName = CodeGenUtils.getSimpleTypeName(assoc);
    }
    
    /**
     * Retrieves the {@link Association} this object describes.
     */
    public final Association getAssoc()
    {
        return assoc;
    }
    
    /**
     * Retrieves the name of the association proxy interface for the described
     * {@link Association}. 
     */
    public final String getAssocInterfaceName()
    {
        return assocInterfaceName;
    }
    
    /**
     * Retrieves simplified multiplicity of the described {@link Association}. 
     */
    public final AssociationKindEnum getKind()
    {
        return kind;
    }
    
    /**
     * Retrieves the base name for the described {@link Association}.  The
     * base name is the association's simple name.
     */
    public final String getBaseName()
    {
        return baseName;
    }

    /**
     * Retrieves the specified {@link AssociationEnd} of the described
     * {@link Association}.
     * 
     * @param end 0-based index of the end to retrieve
     * @return the selected AssociationEnd
     */
    public final AssociationEnd getEnd(int end)
    {
        return ends[end];
    }
    
    /**
     * Retrieves the name of the class instance interface for the given
     * end of the described {@link Association}.
     */
    public final String getEndType(int end)
    {
        return types[end];
    }
    
    /**
     * Retrieves the identifiers of the class instance interfaces for both ends of 
     * the described {@link Association} as a String array.
     */
    public final String[] getEndTypes()
    {
        return types;
    }
    
    /**
     * Retrieves an identifier for the given {@link AssociationEnd} of the 
     * described {@link Association}.  The identifier is the end's name 
     * mangled to be a valid Java identifier.
     * 
     * @param end 0-based end index
     */
    public final String getEndIdentifier(int end)
    {
        return identifiers[end];
    }
    
    /**
     * Retrieves identifiers for the {@link AssociationEnd ends} of the 
     * described {@link Association} as a String array.  The identifiers are
     * the end names mangled to be valid Java identifiers.
     */
    public final String[] getEndIdentifiers()
    {
        return identifiers;
    }
    
    /**
     * Retrieves the name of the given {@link AssociationEnd} of the
     * describe {@link Association}.
     * 
     * @param end 0-based end index
     */
    public String getEndName(int end)
    {
        return ends[end].getName();
    }
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * has an upper multiplicity bound of exactly 1.
     */
    public final boolean isSingle(int end)
    {
        return ends[end].getMultiplicity().getUpper() == 1;
    }
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * has ordered multiplicity.
     */
    public final boolean isOrdered(int end)
    {
        return ends[end].getMultiplicity().isOrdered();
    }
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * is changeable.
     */
    public final boolean isChangeable(int end)
    {
        return ends[end].isChangeable();
    }
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * has composite aggregation semantics.
     */
    public final boolean isComposite(int end)
    {
        return AggregationKindEnum.COMPOSITE.equals(
            ends[end].getAggregation());
    }
}

// End AssociationInfo.java
