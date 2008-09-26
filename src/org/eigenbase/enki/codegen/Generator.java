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

import java.io.*;
import java.util.*;

import javax.jmi.reflect.*;

/**
 * Generator represents a class that generates JMI code for a UML model.
 * 
 * @author Stephan Zuercher
 */
public interface Generator
{
    /**
     * Configures the XMI file containing the model for which code will be
     * generated.
     * 
     * @param xmiFile a filename
     */
    public void setXmiFile(File xmiFile);

    /**
     * Retrieves the XMI file containing the model for which code is being
     * generated.
     * 
     * @return the XMI file's name
     */
    public File getXmiFile();
    
    /**
     * Configures the directory where code generated for the model will be
     * written.
     * 
     * @param outputDir an existing directory
     */
    public void setOutputDirectory(File outputDir);

    /**
     * Enables or disables generic types.  If enabled, collections include 
     * generic type specifications.  If disabled, the generic types are 
     * commented out (e.g., <tt>List/*&lt;SomeType&gt;*&#x2f;</tt>) 
     * 
     * @param enable controls whether generic types are enabled (true) or not 
     *               (false)
     * @return the previous value of the setting
     */
    public boolean setUseGenerics(boolean enable);

    /**
     * Configures implementation-specific options for this Generator.  Unknown
     * options should be ignored.
     * 
     * @param options map of option name to value
     */
    public void setOptions(Map<String, String> options);
    
    /**
     * Adds a {@link Handler} implementation to the list of handlers for this
     * generator.  All calls to this method must occur <b>before</b> 
     * {@link #execute()} or as part of a callback initiated by that method.
     * The Generator implementation must call 
     * {@link Handler#setGenerator(Generator)} before invoking any method
     * on the handler.
     * 
     * @param handler a {@link Handler} implementation.  In practice, the
     *                object given must also implement one of Handler's
     *                sub-interfaces.
     */
    public void addHandler(Handler handler);

    /**
     * Generates code with the given configuration.
     * 
     * @throws GenerationException if there is a file or other error during
     *                             code generation
     */
    public void execute() throws GenerationException;

    /**
     * Retrieves the RefBaseObject for the current metamodel.
     * 
     * @return the RefBaseObject for the current metamodel.
     */
    public RefBaseObject getRefBaseObject();
        
    /**
     * Applies any transformations required by this generator to an identifier.
     * Example transformations are truncation and mangling.
     *
     * @param identifier identifier to be transformed
     *
     * @return transformed identifier
     */
    public String transformIdentifier(String identifier);
}
