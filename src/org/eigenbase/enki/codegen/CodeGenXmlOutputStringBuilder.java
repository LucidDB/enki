/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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
 * CodeGenXmlOutputStringBuilder implements {@link CodeGenXmlOutput} and
 * stores the generated XML output as a string.
 * 
 * @author Stephan Zuercher
 */
public class CodeGenXmlOutputStringBuilder
    extends AbstractCodeGenXmlOutput
    implements CodeGenXmlOutput
{
    private final StringWriter stringWriter;
    
    public CodeGenXmlOutputStringBuilder()
    {
        super();
        
        this.stringWriter = new StringWriter();
        
        setOutput(new PrintWriter(stringWriter));
        setEncoding("UTF-8");
    }
    
    public String getOutput()
    {
        return stringWriter.toString();
    }
    
    public void clearOutput()
    {
        stringWriter.getBuffer().setLength(0);
    }
}

// End CodeGenXmlOutputStringBuilder.java