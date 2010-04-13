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

import org.apache.tools.ant.*;
import org.eigenbase.enki.ant.EnkiTask.*;
import org.eigenbase.enki.mdr.*;

/**
 * DropExtentSubTask is responsible for dropping MDR extents.
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
 *   <td>Existing model extent to drop.</td>
 *   <td>Yes</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class DropExtentSubTask
    extends SubTask
{
    private String extentName;
    private boolean failOnError;
    
    protected DropExtentSubTask(String name)
    {
        super(name);
        
        this.failOnError = true;
    }

    public void setExtent(String extentName)
    {
        this.extentName = extentName;
    }
    
    public void setFailOnError(boolean failOnError)
    {
        this.failOnError = failOnError;
    }
    
    @Override
    protected void execute() throws BuildException
    {
        if (extentName == null) {
            throw new BuildException("Missing extent attribute");
        }
        
        EnkiMDRepository repos = getMDRepository(true);

        try {
            repos.beginSession();
            repos.dropExtentStorage(extentName);
            repos.endSession();
        } catch (EnkiDropFailedException e) {
            if (failOnError) {
                throw new BuildException(e);
            } else {
                e.printStackTrace(System.out);
            }
        }
    }
}

// End DropExtentSubTask.java
