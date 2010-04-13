/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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


/**
 * CodeGenXmlOutputWrapper delegates {@link CodeGenOutput} methods to a given
 * CodeGenOutput instance and provides an implementation of 
 * {@link CodeGenXmlOutput}.  This is useful for mixing 
 * {@link CodeGenXmlOutput} into subclass that already extends a
 * {@link CodeGenOutput} implementation.  
 * 
 * @author Stephan Zuercher
 */
public class CodeGenXmlOutputWrapper
    extends AbstractCodeGenXmlOutput
    implements CodeGenOutput, CodeGenXmlOutput
{
    private final CodeGenOutput output;
    
    public CodeGenXmlOutputWrapper(CodeGenOutput output)
    {
        super();
        
        this.output = output;
    }

    public String getEncoding()
    {
        return output.getEncoding();
    }

    public void increaseIndent()
    {
        output.increaseIndent();
    }

    public void decreaseIndent()
    {
        output.decreaseIndent();
    }

    public void resetIndent()
    {
        output.resetIndent();
    }

    public int getIndentLevel()
    {
        return output.getIndentLevel();
    }

    public void indent()
    {
        output.indent();
    }

    public void newLine()
    {
        output.newLine();
    }

    public void write(Object... strings)
    {
        output.write(strings);
    }

    public void writeWrapped(String prefix, Object... strings)
    {
        output.writeWrapped(prefix, strings);
    }

    public void writeln(Object... strings)
    {
        output.writeln(strings);
    }
}

// End CodeGenXmlOutputWrapper.java
