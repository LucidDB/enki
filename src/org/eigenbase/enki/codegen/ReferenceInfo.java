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
 * (See {@link #ReferenceInfo(Generator, Association, AssociationEnd)}.)
 * 
 * @author Stephan Zuercher
 */
public interface ReferenceInfo extends AssociationInfo
{
    public Reference getReference();
    
    public Classifier getReferencedType();
    
    public String getReferencedTypeName();
    
    public String getFieldName();
    
    public String getReferencedEndBaseName();
    
    public String getAccessorName();
    
    public boolean isSingle();
    
    public boolean isOrdered();
    
    public boolean isChangeable();
    
    public boolean isComposite();
    
    public boolean isReferencedEndFirst();
    
    public boolean isExposedEndFirst();
    
    public int getReferencedEndIndex();
    
    public int getExposedEndIndex();
}

// End ReferenceInfo.java
