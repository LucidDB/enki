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

import javax.jmi.model.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.util.*;
import org.netbeans.api.mdr.*;

/**
 * CreateExtentSubTask uses the given configuration information to create a
 * metadata repository extent.
 * 
 * <p>Attributes:
 * <table>
 * <tr>
 *   <th>Name</th>
 *   <th>Description</th>
 *   <th>Required?</th>
 * </tr>
 * <tr>
 *   <td>name</td>
 *   <td>Name of the extent to create.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>extent</td>
 *   <td>Existing metamodel extent to instantiate.</td>
 *   <td>No</td>
 * </tr>
 * <tr>
 *   <td>package</td>
 *   <td>Package to instantiate.</td>
 *   <td>Yes, if extent is specified</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class CreateExtentSubTask extends EnkiTask.SubTask
{
    private String extentName;
    private String modelExtentName;
    private String packageName;
    
    public CreateExtentSubTask(String name)
    {
        super(name);
    }
    
    public void setName(String name)
    {
        this.extentName = name;
    }
    
    public void setExtent(String extent)
    {
        this.modelExtentName = extent;
    }
    
    public void setPackage(String packageName)
    {
        this.packageName = packageName;
    }
    
    @Override
    void execute() throws BuildException
    {
        if (extentName == null) {
            throw new BuildException(
                "A name for new extent must be specified using the \"name\" attribute");
        }
        
        MDRepository repos = task.getMDRepository(true);
        
        try {
            if (modelExtentName == null && packageName == null) {
                repos.createExtent(extentName);
            } else if (modelExtentName == null) {
                throw new BuildException(
                    "An extent containing the package must be specified using the \"extent\" attribute");
            } else if (packageName == null) {
                throw new BuildException(
                    "A package name must be specified using the \"package\" attribute");
            } else {
                ModelPackage modelPackage =
                    (ModelPackage)repos.getExtent(modelExtentName);
                if (modelPackage == null) {
                    throw new BuildException(
                        "Extent '" + modelExtentName + "' does not exist");
                }
                
                MofPackage extentPackage = null;
                for(MofPackage mofPkg: 
                        GenericCollections.asTypedCollection(
                            modelPackage.getMofPackage().refAllOfClass(),
                            MofPackage.class))
                {
                    if (mofPkg.getName().equals(packageName)) {
                        extentPackage = mofPkg;
                        break;
                    }
                }
                
                if (extentPackage == null) {
                    throw new BuildException(
                        "Package '" + packageName + 
                        "' does not exist in the extent '" + modelExtentName + 
                        "'");
                }
                
                repos.createExtent(extentName, extentPackage);
            }
        } catch(CreationFailedException ex) {
            throw new BuildException(ex);
        }
    }
}

// End CreateExtentSubTask.java
