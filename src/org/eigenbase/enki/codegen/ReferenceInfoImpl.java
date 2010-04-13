/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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
 * ReferenceInfoImpl contains commonly used information about {@link Reference}
 * instances and the referenced {@link Association}.
 * 
 * Note: This class also supports situations where an Association exists 
 * between two classes, but there is no corresponding Reference.  The name
 * of this class is, therefore, misleading.  
 * (See {@link #ReferenceInfoImpl(Association, AssociationEnd)}.)
 * 
 * @author Stephan Zuercher
 */
public class ReferenceInfoImpl 
    extends AssociationInfoImpl 
    implements ReferenceInfo
{
    private final Reference ref;
    
    private final AssociationEnd referencedEnd;
    
    private final Classifier referencedType;
    
    private final String referencedTypeName;
    
    private final String referenceEndBaseName;
    private final String fieldName;
    
    private final boolean isReferenceEndFirst;
    
    public ReferenceInfoImpl(Reference ref)
            throws GenerationException
    {
        this(
            ref,
            (Association)ref.getExposedEnd().getContainer(),
            ref.getReferencedEnd(),
            false);
    }
    
    /**
     * Constructs a Reference-less ReferenceInfo.  Useful when two classes
     * are associated, but do not reference each other.  When this
     * constructor is used, {@link #getReference()} returns null.
     * 
     * @param assoc the association
     * @param referencedEnd the end of the association that should be treated
     *                      as the referenced end
     */
    public ReferenceInfoImpl(Association assoc, AssociationEnd referencedEnd)
    {
        this(null, assoc, referencedEnd, true);
    }

    private ReferenceInfoImpl(
        Reference ref,
        Association assoc,
        AssociationEnd referencedEnd,
        boolean prefixWithAssocName)
    {
        super(assoc);
        
        this.ref = ref;
        this.referencedEnd = referencedEnd;
        this.referencedType = referencedEnd.getType();
        this.referencedTypeName = 
            CodeGenUtils.getTypeName(referencedType);
        
        String baseName;
        if (prefixWithAssocName) {
            baseName = 
                StringUtil.toInitialUpper(assoc.getName()) + "_" +
                StringUtil.toInitialUpper(referencedEnd.getName());
        } else {
            baseName = StringUtil.toInitialUpper(referencedEnd.getName());
        }
        
        String fieldNameInit;
        if (prefixWithAssocName) {
            fieldNameInit = 
                StringUtil.toInitialLower(assoc.getName()) + "_" +
                StringUtil.toInitialUpper(referencedEnd.getName());
        } else {
            fieldNameInit = StringUtil.toInitialLower(referencedEnd.getName());
        }

        this.referenceEndBaseName = baseName;
        this.fieldName = fieldNameInit;
        
        this.isReferenceEndFirst = (getEnd(0) == referencedEnd);
        
    }
    
    /**
     * Retrieves the described {@link Reference} object.
     */
    public final Reference getReference()
    {
        return ref;
    }
    
    /**
     * Retrieves the type referenced by the described {@link Reference}.
     */
    public final Classifier getReferencedType()
    {
        return referencedType;
    }
    
    /**
     * Retrieves the name of the class instance interface for the type 
     * referenced by the described {@link Reference}.
     */
    public final String getReferencedTypeName()
    {
        return referencedTypeName;
    }
    
    /**
     * Retrieves the name of the field used to store information about the
     * association of the described {@link Reference}.
     */
    public final String getFieldName()
    {
        return fieldName;
    }
    
    /**
     * Retrieves the {@link AssociationInfo#getBaseName() base name} for the
     * referenced end.  If this object describes a reference-less Association,
     * the name is prefixed with the Association's name to insure uniqueness.
     */
    public final String getReferencedEndBaseName()
    {
        return referenceEndBaseName;
    }
    
    /**
     * Retrieves the name of the accessor (getter) method used to retrieve
     * the reference's value(s) from a 
     * {@link javax.jmi.reflect.RefAssociation}.
     */
    public final String getAccessorName(Generator generator)
    {
        return CodeGenUtils.getAccessorName(generator, referencedEnd, null);
    }
    
    /**
     * Tests whether the referenced end of the association has an upper
     * multiplicity bound of exactly 1.
     */
    public final boolean isSingle()
    {
        return referencedEnd.getMultiplicity().getUpper() == 1;
    }
    
    /**
     * Tests whether the referenced end of the association has ordered
     * multiplicity.
     */
    public final boolean isOrdered()
    {
        return referencedEnd.getMultiplicity().isOrdered();
    }
    
    /**
     * Tests whether the referenced end of the association is changeable.
     */
    public final boolean isChangeable()
    {
        return referencedEnd.isChangeable();
    }
    
    /**
     * Tests whether the referenced end of the association has composite
     * aggregation semantics.
     */    
    public final boolean isComposite()
    {
        return AggregationKindEnum.COMPOSITE.equals(
            referencedEnd.getAggregation());
    }
    
    /**
     * Tests whether the referenced end of the association is the first 
     * (index 0) {@link AssociationEnd} of the underlying {@link Association}.
     */
    public final boolean isReferencedEndFirst()
    {
        return isReferenceEndFirst;
    }
    
    /**
     * Tests whether the exposed end of the association is the first 
     * (index 0) {@link AssociationEnd} of the underlying {@link Association}.
     */
    public final boolean isExposedEndFirst()
    {
        return !isReferenceEndFirst;
    }
    
    /**
     * Retrieves the 0-based index of the {@link AssociationEnd} in the
     * underlying {@link Association} which represents the referenced end.
     */
    public final int getReferencedEndIndex()
    {
        return isReferenceEndFirst ? 0 : 1;
    }
    
    /**
     * Retrieves the 0-based index of the {@link AssociationEnd} in the
     * underlying {@link Association} which represents the exposed end.
     */
    public final int getExposedEndIndex()
    {
        return isReferenceEndFirst ? 1 : 0;
    }
}

// End ReferenceInfo.java
