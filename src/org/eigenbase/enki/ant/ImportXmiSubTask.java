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
package org.eigenbase.enki.ant;

import java.io.*;

import javax.jmi.reflect.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.ant.EnkiTask.*;
import org.eigenbase.enki.mdr.*;
import org.netbeans.api.xmi.*;

/**
 * ImportXmi task imports the given XMI file into a newly 
 * {@link CreateExtentSubTask created extent}.
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
 *   <td>Extent into which the metamodel should be imported.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>XMI metamodel to import.</td>
 *   <td>Yes</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class ImportXmiSubTask extends SubTask
{
    private String file;
    private String extent;
    
    public ImportXmiSubTask(String name)
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
        
        File xmiFile = new File(file);
        
        EnkiMDRepository repos = getMDRepository(true);
        repos.beginSession();
        try {
            boolean builtIn = repos.isExtentBuiltIn(extent);
            if (builtIn) {
                System.out.println(
                    "Ignoring import step for built-in extent '" + extent + "'");
                return;
            }
    
            RefPackage refPackage = repos.getExtent(extent);
            if (refPackage == null) {
                throw new BuildException(
                    "Extent '" + extent + "' does not exist");
            }
            
            XMIReader xmiReader = 
                XMIReaderFactory.getDefault().createXMIReader();
    
            repos.beginTrans(true);
            boolean rollback = true;
            try {
                xmiReader.read(xmiFile.toURL().toString(), refPackage);
                rollback = false;
            } catch (Exception e) {
                throw new BuildException(e);
            } finally {
                repos.endTrans(rollback);
            }
        } finally {
            repos.endSession();
        }
    }
}

// End ImportXmiSubTask.java
