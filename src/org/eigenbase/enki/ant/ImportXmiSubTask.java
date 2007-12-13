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
import org.netbeans.api.xmi.*;

/**
 * ImportXmi task imports the given XMI file into a newly 
 * {@link CreateExtentSubTask created extent}.
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
    void execute() throws BuildException
    {
        if (file == null) {
            throw new BuildException("Missing \"file\" attribute");
        }

        if (extent == null) {
            throw new BuildException("Missing \"extent\" attribute");
        }
        
        File xmiFile = new File(file);
        
        MdrProvider implType = getMdrProvider();
        if (implType != MdrProvider.NETBEANS_MDR) {            
            // TODO: perform import for Hibernate implementation
            System.out.println(
                "Ignoring import step for '" + implType + "' provider");
            return;
        }

        task.getMDRepository().beginTrans(true);
        boolean rollback = true;
        try {
            RefPackage refPackage = 
                task.getMDRepository().getExtent(extent);
            if (refPackage == null) {
                throw new BuildException(
                    "Extent '" + extent + "' does not exist");
            }
            
            XMIReader xmiReader = 
                XMIReaderFactory.getDefault().createXMIReader();
            try {
                xmiReader.read(xmiFile.toURL().toString(), refPackage);
            } catch (Exception e) {
                throw new BuildException(e);
            }

            rollback = false;
        }
        finally {
            task.getMDRepository().endTrans(!rollback);
        }
    }
}

// End ImportXmiSubTask.java
