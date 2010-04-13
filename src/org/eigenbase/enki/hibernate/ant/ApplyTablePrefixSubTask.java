/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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
package org.eigenbase.enki.hibernate.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.eigenbase.enki.ant.*;
import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.config.*;

/**
 * ApplyTablePrefixSubTask parses arbitrary files containing the Enki/Hibernate
 * table prefix reference and replaces the references with the given table
 * prefix.  Note that no escaping of the table prefix reference is possible.
 * The table prefix reference is defined in 
 * {@link HibernateMappingHandler#TABLE_REF} and its value is obtained from
 * the storage properties passed to {@link EnkiTask}.
 * 
 * <p>Attributes:
 * <table border="1">
 * <tr>
 *   <th>Name</th>
 *   <th>Description</th>
 *   <th>Required?</th>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>Selects a file to parse</td>
 *   <td>Yes, unless one or more filesets nested</td>
 * </tr>
 * <tr>
 *   <td>output</td>
 *   <td>Sets the output directory for the file(s).
 *   <td>Yes</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class ApplyTablePrefixSubTask extends EnkiTask.SubTask
{
    private String file;
    private List<FileSet> fileSets;
    private String output;
    
    public ApplyTablePrefixSubTask()
    {
        super("applyTablePrefix/Hibernate");
        
        this.fileSets = new ArrayList<FileSet>();
    }

    public void setFile(String file)
    {
        this.file = file;
    }
    
    public void addFileSet(FileSet fileSet)
    {
        this.fileSets.add(fileSet);
    }
    
    public void setOutput(String output)
    {
        this.output = output;
    }
    
    @Override
    protected void execute()
        throws BuildException
    {
        if (file == null && fileSets.isEmpty()) {
            throw new BuildException("Missing file or fileset(s)");
        }
        
        if (output == null) {
            throw new BuildException("Missing output directory");
        }

        String tablePrefix = 
            getStorageProperties().getProperty(
                HibernateMDRepository.PROPERTY_STORAGE_TABLE_PREFIX);
        if (tablePrefix == null) {
            tablePrefix = "";
        }

        verbose("Table Prefix: [" + tablePrefix + "]");
        
        List<File> files = new ArrayList<File>();
        
        if (file != null) {
            files.add(new File(file));
        }
        
        for(FileSet fileSet: fileSets) {
            DirectoryScanner scanner = 
                fileSet.getDirectoryScanner(getProject());
            String[] names = scanner.getIncludedFiles();
            File baseDir = scanner.getBasedir();
            for(String name: names) {
                files.add(new File(baseDir, name));
            }
        }

        for(File f: files) {
            if (!f.exists()) {
                throw new BuildException("File not found: " + f);
            }
            
            if (!f.canRead()) {
                throw new BuildException("Cannot read file: " + f);
            }
        }

        byte[] buffer = new byte[8192];
        for(File f: files) {
            File outputFile = new File(output, f.getName());
            
            verbose(f.toString() + " - > " + outputFile.toString());
            try {
                TablePrefixInputStream in = 
                    new TablePrefixInputStream(
                        new FileInputStream(f),
                        tablePrefix);
                
                FileOutputStream out = new FileOutputStream(outputFile);
                
                int len;
                while((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                out.close();
                in.close();
            } catch(IOException e) {
                throw new BuildException("i/o error on " + f, e);
            }
        }
    }

}

// End ApplyTablePrefixSubTask.java
