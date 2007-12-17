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

/**
 * Handler represents a class that generated code for one or more types of
 * JMI model element.  See the sub-interfaces for more details.
 * 
 * @author Stephan Zuercher
 */
public interface Handler
{
    /**
     * Configures the Generator driving this Handler.
     * 
     * @param generator a {@link Generator}
     */
    public void setGenerator(Generator generator);
    
    /**
     * Configures the output directory.  Called automatically by 
     * {@link Generator#addHandler(Handler)}.  Do not call 
     * this method on an instance of {@link HandlerBase} that has
     * already been added to a {@link Generator}.
     * 
     * @param outputDir directory in which to store output files.
     */
    public void setOutputDir(File outputDir);
    
    /**
     * Indicates that code generation is about to begin.  Called once after
     * all handlers have been configured, but before any Handler sub-interface
     * is asked to generate any code.  
     * 
     * @throws GenerationException for any error condition
     */
    public void beginGeneration() throws GenerationException;
    
    /**
     * Indicates that code generation had ended.  Called once after all 
     * handlers have finished generating code.  Called even if generation is
     * ending abnormally.
     * 
     * @param throwing true if generation is ending abnormally due to an
     *                 exception (handler should not throw a new exception
     *                 in this case to avoid masking an earlier error)
     * @throws GenerationException for any error condition
     */
    public void endGeneration(boolean throwing) throws GenerationException;
}
