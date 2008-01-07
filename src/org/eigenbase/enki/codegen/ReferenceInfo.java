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
 * ReferenceInfo contains commonly used information about {@link Reference}
 * instances and the referenced {@link Association}.
 * 
 * @author Stephan Zuercher
 */
public class ReferenceInfo extends AssociationInfo
{
    private final AssociationEnd referencedEnd;
    
    private final Classifier referencedType;
    
    private final String referencedTypeName;
    
    private final String referenceEndBaseName;
    private final String fieldName;
    private final String accessorName;
    
    private final boolean isReferenceEndFirst;
    
    public ReferenceInfo(Generator generator, Reference ref)
            throws GenerationException
    {
        this(
            generator,
            (Association)ref.getExposedEnd().getContainer(),
            ref.getReferencedEnd(),
            false);
    }
    
    public ReferenceInfo(
        Generator generator, Association assoc, AssociationEnd referencedEnd)
    {
        this(generator, assoc, referencedEnd, true);
    }

    private ReferenceInfo(
        Generator generator, Association assoc, AssociationEnd referencedEnd,
        boolean prefixWithAssocName)
    {
        super(generator, assoc);
        
        this.referencedEnd = referencedEnd;
        this.referencedType = referencedEnd.getType();
        this.referencedTypeName = 
            generator.getTypeName(referencedType);
        
        String baseName;
        if (prefixWithAssocName) {
            baseName = 
                StringUtil.toInitialUpper(assoc.getName()) + "_" +
                StringUtil.toInitialUpper(referencedEnd.getName());
        } else {
            baseName = StringUtil.toInitialUpper(referencedEnd.getName());
        }
        
        String fieldName;
        if (prefixWithAssocName) {
            fieldName = 
                StringUtil.toInitialLower(assoc.getName()) + "_" +
                StringUtil.toInitialUpper(referencedEnd.getName());
        } else {
            fieldName = StringUtil.toInitialLower(referencedEnd.getName());
        }

        
        this.referenceEndBaseName = baseName;
        this.fieldName = fieldName;
        this.accessorName = generator.getAccessorName(referencedEnd, null);
        
        this.isReferenceEndFirst = (getEnd(0) == referencedEnd);
        
    }
    
    public AssociationEnd getReferencedEnd()
    {
        return referencedEnd;
    }
    
    public Classifier getReferencedType()
    {
        return referencedType;
    }
    
    public String getReferencedTypeName()
    {
        return referencedTypeName;
    }
    
    public String getFieldName()
    {
        return fieldName;
    }
    
    public String getReferencedEndBaseName()
    {
        return referenceEndBaseName;
    }
    
    public String getAccessorName()
    {
        return accessorName;
    }
    
    public boolean isSingle()
    {
        return referencedEnd.getMultiplicity().getUpper() == 1;
    }
    
    public boolean isOrdered()
    {
        return referencedEnd.getMultiplicity().isOrdered();
    }
    
    public boolean isChangeable()
    {
        return referencedEnd.isChangeable();
    }
    
    public boolean isReferencedEndFirst()
    {
        return isReferenceEndFirst;
    }
    
    public boolean isExposedEndFirst()
    {
        return !isReferenceEndFirst;
    }
    
    public int getReferencedEndIndex()
    {
        return isReferenceEndFirst ? 0 : 1;
    }
    
    public int getExposedEndIndex()
    {
        return isReferenceEndFirst ? 1 : 0;
    }
}

// End ReferenceInfo.java
