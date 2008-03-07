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
 * AssociationInfo contains commonly used information about a MOF 
 * {@link Association} and and its {@link AssociationEnd} instances.
 * 
 * @author Stephan Zuercher
 */
public interface AssociationInfo
{
    /**
     * Retrieves the {@link Association} this object describes.
     */
    public Association getAssoc();
    
    /**
     * Retrieves the name of the association proxy interface for the described
     * {@link Association}. 
     */
    public String getAssocInterfaceName();
    
    /**
     * Retrieves simplified multiplicity of the described {@link Association}. 
     */
    public AssociationKindEnum getKind();
    
    /**
     * Retrieves the base name for the described {@link Association}.  The
     * base name is the association's simple name.
     */
    public String getBaseName();
    
    /**
     * Retrieves the specified {@link AssociationEnd} of the described
     * {@link Association}.
     * 
     * @param end 0-based index of the end to retrieve
     * @return the selected AssociationEnd
     */
    public AssociationEnd getEnd(int end);
    
    /**
     * Retrieves the name of the class instance interface for the given
     * end of the described {@link Association}.
     */
    public String getEndType(int end);
    
    /**
     * Retrieves the names of the class instance interfaces for both ends of 
     * the described {@link Association} as a String array.
     */
    public String[] getEndTypes();
    
    /**
     * Retrieves an identifier for the given {@link AssociationEnd} of the 
     * described {@link Association}.  The identifier is the end's name 
     * mangled to be a valid Java identifier.
     * 
     * @param end 0-based end index
     */
    public String getEndIdentifier(int end);
    
    /**
     * Retrieves identifiers for the {@link AssociationEnd ends} of the 
     * described {@link Association} as a String array.  The identifiers are
     * the end names mangled to be valid Java identifiers.
     */
    public String[] getEndIdentifiers();
    
    /**
     * Retrieves the name of the given {@link AssociationEnd} of the
     * describe {@link Association}.
     * 
     * @param end 0-based end index
     */
    public String getEndName(int end);
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * has an upper multiplicity bound of exactly 1.
     */
    public boolean isSingle(int end);
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * has a ordered multiplicity.
     */
    public boolean isOrdered(int end);
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * is changeable.
     */
    public boolean isChangeable(int end);
    
    /**
     * Tests whether the selected end of the described {@link Association}
     * has composite aggregation semantics.
     */
    public boolean isComposite(int end);
}

// End AssociationInfo.java
