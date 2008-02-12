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

/**
 * ReferenceInfo contains commonly used information about {@link Reference}
 * instances and the referenced {@link Association}.
 * 
 * Note: This class also supports situations where an Association exists 
 * between two classes, but there is no corresponding Reference.  The name
 * of this class is, therefore, misleading.  
 * (See {@link ReferenceInfoImpl#ReferenceInfoImpl(
 *                 Generator, Association, AssociationEnd)}.)
 * 
 * @author Stephan Zuercher
 */
public interface ReferenceInfo extends AssociationInfo
{
    /**
     * Retrieves the described {@link Reference} object.
     */
    public Reference getReference();

    /**
     * Retrieves the type referenced by the described {@link Reference}.
     */
    public Classifier getReferencedType();

    /**
     * Retrieves the name of the class instance interface for the type 
     * referenced by the described {@link Reference}.
     */
    public String getReferencedTypeName();
    
    /**
     * Retrieves the name of the field used to store information about the
     * association of the described {@link Reference}.
     */
    public String getFieldName();
    
    /**
     * Retrieves the {@link AssociationInfo#getBaseName() base name} for the
     * referenced end.  If this object describes a reference-less Association,
     * the name is prefixed with the Association's name to insure uniqueness.
     */
    public String getReferencedEndBaseName();
    
    /**
     * Retrieves the name of the accessor (getter) method used to retrieve
     * the reference's value(s) from a 
     * {@link javax.jmi.reflect.RefAssociation}.
     */
    public String getAccessorName();
    
    /**
     * Tests whether the referenced end of the association has an upper
     * multiplicity bound of exactly 1.
     */
    public boolean isSingle();
    
    /**
     * Tests whether the referenced end of the association has ordered
     * multiplicity.
     */
    public boolean isOrdered();
    
    /**
     * Tests whether the referenced end of the association is changeable.
     */
    public boolean isChangeable();
    
    /**
     * Tests whether the referenced end of the association has composite
     * aggregation semantics.
     */    
    public boolean isComposite();
    
    /**
     * Tests whether the referenced end of the association is the first 
     * (index 0) {@link AssociationEnd} of the underlying {@link Association}.
     */
    public boolean isReferencedEndFirst();
    
    /**
     * Tests whether the exposed end of the association is the first 
     * (index 0) {@link AssociationEnd} of the underlying {@link Association}.
     */
    public boolean isExposedEndFirst();
    
    /**
     * Retrieves the 0-based index of the {@link AssociationEnd} in the
     * underlying {@link Association} which represents the referenced end.
     */
    public int getReferencedEndIndex();
    
    /**
     * Retrieves the 0-based index of the {@link AssociationEnd} in the
     * underlying {@link Association} which represents the exposed end.
     */
    public int getExposedEndIndex();
}

// End ReferenceInfo.java
