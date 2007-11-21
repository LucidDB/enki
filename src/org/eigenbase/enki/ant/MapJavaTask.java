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
package org.eigenbase.enki.ant;

import java.io.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.codegen.*;

/**
 * @author Stephan Zuercher
 */
public class MapJavaTask extends Task
{
    private String xmiFile;
    private String outputDir;
    private String generatorClsName;
    
    public void execute() throws BuildException
    {
        if (xmiFile == null) {
            throw new BuildException("missing file attribute");
        }
        
        if (outputDir == null) {
            throw new BuildException("missing dir attribute");
        }
        
        if (generatorClsName == null) {
            throw new BuildException("missing generatorClass attribute");
        }
        
        Class<? extends Generator> generatorCls;
        try {
            generatorCls = Class.forName(generatorClsName).asSubclass(Generator.class);
        } catch (ClassNotFoundException e) {
            throw new BuildException(e);
        }
        
        Generator generator;
        try {
            generator = generatorCls.newInstance();
        } catch (Exception e) {
            throw new BuildException(e);
        }
        
        File xmi = new File(xmiFile);
        File dir = new File(outputDir);
        
        generator.setOutputDirectory(dir);
        generator.setXmiFile(xmi);
        generator.setUseGenerics(true);
        
        try {
            generator.execute();
        } catch (GenerationException e) {
            throw new BuildException(e);
        }
    }

    public void setFile(String xmiFile)
    {
        this.xmiFile = xmiFile;
    }
    
    public void setDir(String outputDir)
    {
        this.outputDir = outputDir;
    }

    public void setGeneratorClass(String generatorClsName)
    {
        this.generatorClsName = generatorClsName;
    }
}

// End MapJavaTask.java
