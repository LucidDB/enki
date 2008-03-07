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
package org.eigenbase.enki.ant;

import java.io.*;

import javax.jmi.reflect.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.mdr.*;
import org.netbeans.api.xmi.*;

/**
 * ImportXmi task imports the named extent into the given XMI file.
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
 *   <td>Extent from which to export an XMI file.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>XMI export file</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>xmiVersion</td>
 *   <td>XMI version to emit</td>
 *   <td>No, defaults to {@link #DEFAULT_XMI_VERSION}</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class ExportXmiSubTask extends EnkiTask.SubTask
{
    /**
     * The default XMI version to use for exporting models. Currently {@value}.
     */
    public static final String DEFAULT_XMI_VERSION = "1.2";
    
    private String file;
    private String extent;
    private String xmiVersion;
    
    public ExportXmiSubTask(String name)
    {
        super(name);
        
        this.xmiVersion = DEFAULT_XMI_VERSION;
    }

    public void setFile(String file)
    {
        this.file = file;
    }
    
    public void setExtent(String extent)
    {
        this.extent = extent;
    }
    
    public void setXmiVersion(String xmiVersion)
    {
        this.xmiVersion = xmiVersion;
    }
    
    @Override
    void execute()
    {
        if (file == null) {
            throw new BuildException("Missing \"file\" attribute");
        }

        if (extent == null) {
            throw new BuildException("Missing \"extent\" attribute");
        }
        
        if (xmiVersion == null || xmiVersion.equals("")) {
            throw new BuildException("Invalid \"xmiVersion\" attribute");
        }
        
        EnkiMDRepository repos = getMDRepository(true);
        repos.beginSession();
        
        try {
            RefPackage pkg = repos.getExtent(extent);
            FileOutputStream out;
            try {
                out = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                throw new BuildException(e);
            }
            
            XMIWriter writer = XMIWriterFactory.getDefault().createXMIWriter();
    
            try {
                writer.write(out, pkg, xmiVersion);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        } finally {
            repos.endSession();
        }
    }
}

// End ExportXmiSubTask.java
