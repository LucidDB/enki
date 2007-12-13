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
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.netbeans.codegen.*;

/**
 * MapJavaSubTask generates code for a given metamodel.
 * 
 * <p>Attributes:
 * <table>
 * <tr>
 *   <th>Name</th>
 *   <th>Description</th>
 *   <th>Required?</th>
 * </tr>
 * <tr>
 *   <td>extent</td>
 *   <td>Extent into which the metamodel will be imported.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>XMI metamodel that needs code generation</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>dir</td>
 *   <td>Directory into which code will be generated (sub directories will be
 *       created as necessary).</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>generatorClass</td>
 *   <td>Fully qualified name of the generator class to use.  For example,
 *       {@link HibernateGenerator} or {@link NetbeansGenerator}.</td>
 *   <td>Yes</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class MapJavaSubTask extends EnkiTask.SubTask
{
    private String xmiFile;
    private String outputDir;
    private String extent;
    private String generatorClsName;
    
    public MapJavaSubTask(String name)
    {
        super(name);
    }
    
    void execute() throws BuildException
    {
        if (xmiFile == null) {
            throw new BuildException("missing file attribute");
        }
        
        if (outputDir == null) {
            throw new BuildException("missing dir attribute");
        }
        
        if (extent == null) {
            throw new BuildException("missing extent attribute");
        }
        
        if (generatorClsName == null) {
            throw new BuildException("missing generatorClass attribute");
        }
        
        Class<? extends MdrGenerator> generatorCls;
        try {
            generatorCls = 
                Class.forName(generatorClsName).asSubclass(MdrGenerator.class);
        } catch (ClassNotFoundException e) {
            throw new BuildException(
                "Generator class must be a subclass of MdrGenerator", e);
        }
        
        MdrGenerator generator;
        try {
            generator = generatorCls.newInstance();
        } catch (Exception e) {
            throw new BuildException(e);
        }
        
        File xmi = new File(xmiFile);
        File dir = new File(outputDir);
        
        generator.setOutputDirectory(dir);
        generator.setXmiFile(xmi);
        generator.setExtentName(extent);
        generator.setUseGenerics(true);
        
        try {
            generator.execute();
        } catch (GenerationException e) {
            throw new BuildException(e);
        }
    }
    
    boolean isCombinableWith(EnkiTask.SubTask subTask)
    {
        // Can't combine since another task might instantiate the MDR 
        // subsystem with a different configuration.
        return false;
    }

    public void setFile(String xmiFile)
    {
        this.xmiFile = xmiFile;
    }
    
    public void setDir(String outputDir)
    {
        this.outputDir = outputDir;
    }

    public void setExtent(String extent)
    {
        this.extent = extent;
    }
    
    public void setGeneratorClass(String generatorClsName)
    {
        this.generatorClsName = generatorClsName;
    }
}

// End MapJavaTask.java
