/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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
package org.eigenbase.enki.hibernate.codegen;

import javax.jmi.model.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.codegen.Generator.*;
import org.eigenbase.enki.util.*;

/**
 * ComponentInfo implements {@link ReferenceInfo} so as to model component
 * attributes as pseudo-references.
 * 
 * @author Stephan Zuercher
 */
public class ComponentInfo implements ReferenceInfo
{
    private final MofClass cls;
    private final Attribute attrib;
    
    private final String baseName;
    private final String[] types;
    private final int exposedEndIndex;
    private final int referencedEndIndex;
    private final String accessorName;
    private final String fieldName;
    private final String referencedEndBaseName;
    
    public ComponentInfo(
        Generator generator,
        MofClass cls,
        Attribute attrib,
        boolean componentReferenced)
    {
        assert(
            attrib.getType() instanceof MofClass ||
            (attrib.getType() instanceof AliasType &&
                ((AliasType)attrib.getType()).getType() instanceof MofClass));

        this.cls = cls;
        this.attrib = attrib;
        
        Classifier attribBaseType = attrib.getType();
        if (attribBaseType instanceof CollectionType) {
            attribBaseType = ((CollectionType)attribBaseType).getType();
        }
        
        this.types = new String[] {
            generator.getTypeName(cls),
            generator.getTypeName(attribBaseType)
        };
        
        this.baseName = 
            StringUtil.toInitialUpper(generator.getSimpleTypeName(attrib))
            + "$Comp"
            + "$" + generator.getSimpleTypeName(attrib.getContainer());
        
        if (componentReferenced) {
            this.exposedEndIndex = 0;
            this.referencedEndIndex = 1;
        } else {
            this.exposedEndIndex = 1;
            this.referencedEndIndex = 0;
        }
        
        this.accessorName = 
            generator.getAccessorName(attrib) + HibernateJavaHandler.IMPL_SUFFIX;
        this.fieldName = StringUtil.toInitialLower(baseName);
        this.referencedEndBaseName = baseName;
    }

    public MofClass getOwnerType()
    {
        return cls;
    }
    
    public Attribute getOwnerAttribute()
    {
        return attrib;
    }
    
    public Association getAssoc()
    {
        throw new UnsupportedOperationException();
    }

    public String getAssocInterfaceName()
    {
        throw new UnsupportedOperationException();
    }

    public String getBaseName()
    {
        return baseName;
    }

    public AssociationEnd getEnd(int end)
    {
        throw new UnsupportedOperationException();
    }

    public String getEndName(int end)
    {
        throw new UnsupportedOperationException();
    }

    public String getEndName(int end, boolean forceInitCaps)
    {
        String name = getEndName(end);
        if (forceInitCaps) {
            name = StringUtil.toInitialUpper(name);
        }
        return name;
    }

    public String[] getEndNames()
    {
        throw new UnsupportedOperationException();
    }

    public String getEndType(int end)
    {
        return types[end];
    }

    public String[] getEndTypes()
    {
        return types;
    }

    public AssociationKindEnum getKind()
    {
        if (isSingle(1)) {
            return AssociationKindEnum.ONE_TO_ONE;
        } else {
            return AssociationKindEnum.ONE_TO_MANY;
        }
    }

    public boolean isChangeable(int end)
    {
        return true;
    }

    public boolean isComposite(int end)
    {
        return (end == 1);
    }

    public boolean isOrdered(int end)
    {
        if (end == 1) {
            return attrib.getMultiplicity().isOrdered();
        }

        return false;
    }

    public boolean isSingle(int end)
    {
        if (end == 1) {
            return attrib.getMultiplicity().getUpper() == 1;
        }

        return true;
    }

    public String getAccessorName()
    {
        return accessorName;
    }

    public int getExposedEndIndex()
    {
        return exposedEndIndex;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public Reference getReference()
    {
        return null;
    }

    public String getReferencedEndBaseName()
    {
        return referencedEndBaseName;
    }

    public int getReferencedEndIndex()
    {
        return referencedEndIndex;
    }

    public Classifier getReferencedType()
    {
        if (referencedEndIndex == 0) {
            return cls;
        } else {
            return attrib.getType();
        }
    }

    public String getReferencedTypeName()
    {
        return types[referencedEndIndex];
    }

    public boolean isChangeable()
    {
        return isChangeable(referencedEndIndex);
    }

    public boolean isComposite()
    {
        return isComposite(referencedEndIndex);
    }

    public boolean isExposedEndFirst()
    {
        return exposedEndIndex == 0;
    }

    public boolean isOrdered()
    {
        return isOrdered(referencedEndIndex);
    }

    public boolean isReferencedEndFirst()
    {
        return referencedEndIndex == 0;
    }

    public boolean isSingle()
    {
        return isSingle(referencedEndIndex);
    }
}

// End ComponentReferenceInfo.java
