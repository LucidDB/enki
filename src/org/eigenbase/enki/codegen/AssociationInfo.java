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

import org.eigenbase.enki.codegen.Generator.*;
import org.eigenbase.enki.util.*;

public class AssociationInfo
{
    private final Association assoc;
    private final String assocInterfaceName;
    private final AssociationKindEnum kind;
    private final AssociationEnd[] ends;
    private final String[] types;
    private final String[] names;
    private final String baseName;
    
    public AssociationInfo(Generator generator, Association assoc)
    {
        this.assoc = assoc;
        this.assocInterfaceName = generator.getTypeName(assoc);
        this.kind = generator.getAssociationKind(assoc);
        this.ends = generator.getAssociationEnds(assoc);

        this.names = new String[] {
            generator.getSimpleTypeName(ends[0]),
            generator.getSimpleTypeName(ends[1]),
        };
        
        this.types = new String[] {
            generator.getTypeName(ends[0].getType()),
            generator.getTypeName(ends[1].getType()),
        };
        
        this.baseName = generator.getSimpleTypeName(assoc);
    }
    
    public final Association getAssoc()
    {
        return assoc;
    }
    
    public final String getAssocInterfaceName()
    {
        return assocInterfaceName;
    }
    
    public final AssociationKindEnum getKind()
    {
        return kind;
    }
    
    public final String getBaseName()
    {
        return baseName;
    }
    
    public final AssociationEnd getEnd(int end)
    {
        return ends[end];
    }
    
    public final String getEndType(int end)
    {
        return types[end];
    }
    
    public final String[] getEndTypes()
    {
        return types;
    }
    
    public final String getEndName(int end)
    {
        return names[end];
    }
    
    public final String getEndName(int end, boolean forceInitCaps)
    {
        String name = getEndName(end);
        if (forceInitCaps) {
            name = StringUtil.toInitialUpper(name);
        }
        return name;
    }
    
    public final String[] getEndNames()
    {
        return names;
    }
    
    public final boolean isSingle(int end)
    {
        return ends[end].getMultiplicity().getUpper() == 1;
    }
    
    public final boolean isOrdered(int end)
    {
        return ends[end].getMultiplicity().isOrdered();
    }
    
    public final boolean isChangeable(int end)
    {
        return ends[end].isChangeable();
    }
    
    public final boolean isComposite(int end)
    {
        return AggregationKindEnum.COMPOSITE.equals(
            ends[end].getAggregation());
    }
}

// End AssociationInfo.java
