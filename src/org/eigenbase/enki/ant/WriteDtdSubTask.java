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

import javax.jmi.reflect.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.ant.EnkiTask.*;
import org.eigenbase.enki.mdr.*;
import org.netbeans.api.mdr.*;

/**
 * WriteDtdSubTask implements the generation of a DTD for a given metamodel
 * extent.
 * 
 * <p>Attributes:
 * <table border="1">
 * <tr>
 *   <th>Name</th>
 *   <th>Description</th>
 *   <th>Required?</th>
 * </tr>
 * <tr>
 *   <td>extent</td>
 *   <td>Extent to generate a DTD from</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>Path to the generated DTD file.</td>
 *   <td>Yes</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class WriteDtdSubTask extends SubTask
{
    private String file;
    private String extent;
    
    public WriteDtdSubTask(String name)
    {
        super(name);
    }
    
    public void setFile(String file)
    {
        this.file = file;
    }
    
    public void setExtent(String extent)
    {
        this.extent = extent;
    }
    
    @Override
    protected void execute() throws BuildException
    {
        if (file == null) {
            throw new BuildException("Missing \"file\" attribute");
        }

        if (extent == null) {
            throw new BuildException("Missing \"extent\" attribute");
        }

        EnkiMDRepository repos = getMDRepository(true);
        repos.beginSession();
        try {
            RefPackage refPackage = repos.getExtent(extent);
            if (refPackage == null) {
                throw new BuildException("Extent '" + extent + "' does not exist");
            }
            
            FileOutputStream out;
            try {
                out = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                throw new BuildException(e);
            }
            
            DTDProducer.getDefault().generate(out, refPackage);
        } finally {
            repos.endSession();
        }
    }
}

// End WriteDtdSubTask.java
