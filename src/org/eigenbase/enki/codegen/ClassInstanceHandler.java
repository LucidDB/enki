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
import javax.jmi.reflect.*;

/**
 * ClassInstanceHandler represents a class that can generate "class instance"
 * code for a JMI model {@link MofClass}.  "Class instance" code means code
 * related to the implementation of a MofClass, such as an interface defining
 * the methods on that class or an actual concrete implementation.
 * 
 * @author Stephan Zuercher
 */
public interface ClassInstanceHandler extends Handler
{
    /** The name of the base class for all MofClass instance classes. */
    public static final JavaClassReference REF_OBJECT_CLASS = 
        new JavaClassReference(RefObject.class);

    /**
     * Generates code for instances of a MofClass.
     * 
     * @param cls the {@link MofClass} to generate
     * @throws GenerationException if there is an error
     */
    public void generateClassInstance(MofClass cls)  
        throws GenerationException;
}
