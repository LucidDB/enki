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
    
    private final boolean isFirstEnd;
    
    public ReferenceInfo(Generator generator, Reference ref)
            throws GenerationException
    {
        super(generator, (Association)ref.getExposedEnd().getContainer());
        
        this.referencedEnd = ref.getReferencedEnd();
        this.referencedType = referencedEnd.getType();
        this.referencedTypeName = 
            generator.getTypeName(referencedType);
        
        this.referenceEndBaseName = 
            StringUtil.toInitialUpper(referencedEnd.getName());
        this.fieldName = StringUtil.toInitialLower(referencedEnd.getName());
        this.accessorName = generator.getAccessorName(referencedEnd, null);
        
        this.isFirstEnd = (getEnd(0) == referencedEnd);
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
    
    public boolean isFirstEnd()
    {
        return isFirstEnd;
    }
    
    public int getReferencedEndIndex()
    {
        return isFirstEnd ? 0 : 1;
    }
    
    public int getExposedEndIndex()
    {
        return isFirstEnd ? 1 : 0;
    }
}

// End ReferenceInfo.java
