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

/**
 * EnumerationClassHandler represents a class that can generate code related
 * to JMI enumeration classes.  An enumeration class is a concrete, final 
 * implementation of an enumeration interface.  
 * 
 * @author Stephan Zuercher
 */
public interface EnumerationClassHandler extends Handler
{
    /** Suffix for all enumeration class type names. */
    public static final String ENUM_CLASS_SUFFIX = "Enum";

    /**
     * Generates enumeration class code for instances of an EnumerationType.
     * 
     * @param enumType the {@link EnumerationType} to generate
     * @throws GenerationException if there is an error
     */
    public void generateEnumerationClass(EnumerationType enumType)
        throws GenerationException;
}
