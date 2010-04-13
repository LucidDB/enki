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
import javax.jmi.reflect.*;

/**
 * PackageHandler represents a class that generates code for JMI model
 * MofPackage instances.
 * 
 * @author Stephan Zuercher
 */
public interface PackageHandler extends Handler
{
    /** Suffix for all generated instances of MofPackage. */
    public static final String PACKAGE_SUFFIX = "Package";
    
    /** The name of the base class for all MofPackage implementations. */
    public static final JavaClassReference REF_PACKAGE_CLASS = 
        new JavaClassReference(RefPackage.class);

    /**
     * Generates code for instances of a MofPackage.
     * 
     * @param pkg the {@link MofPackage} to generate
     * @throws GenerationException if there is an error
     */    
    public void generatePackage(MofPackage pkg) 
        throws GenerationException;
}
