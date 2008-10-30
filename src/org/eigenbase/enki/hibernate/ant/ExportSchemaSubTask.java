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
package org.eigenbase.enki.hibernate.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.ant.*;
import org.eigenbase.enki.hibernate.config.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.mdr.*;
import org.hibernate.cfg.*;
import org.hibernate.dialect.*;
import org.hibernate.tool.hbm2ddl.*;

/**
 * ExportSchemaSubTask implements exporting a schema creation script.
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
 *   <td>Generates DDL for this name extent.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>Location of output DDL script.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>delimiter</td>
 *   <td>Sets the delimiter used, in addition to a newline, at the end of each 
 *       DDL statement. Defaults to ";" (semicolon).</td>
 *   <td>No</td>
 * </tr>
 * <tr>
 *   <td>includeProviderSchema</td>
 *   <td>Boolean flag controlling the generation of DDL for the Enki/Hibernate 
 *       provider. Defaults to <code>true</code>.</td>
 *   <td>No</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class ExportSchemaSubTask
    extends EnkiTask.SubTask
{
    public static final String DEFAULT_DELIMITER = ";";
    
    private String extent;
    private String file;
    private String delimiter;
    private boolean includeProviderSchema;
    private boolean update;
    
    public ExportSchemaSubTask()
    {
        super("exportSchema/Hibernate");
        
        this.delimiter = DEFAULT_DELIMITER;
        this.includeProviderSchema = true;
        this.update = false;
    }

    public void setExtent(String extent)
    {
        this.extent = extent;
    }
    
    public void setFile(File file)
    {
        this.file = file.getPath();
    }
    
    public void setDelimiter(String delimiter)
    {
        this.delimiter = delimiter;
    }
    
    public void setIncludeProviderSchema(boolean includeProviderSchema)
    {
        this.includeProviderSchema = includeProviderSchema;
    }
    
    public void setUpdate(boolean update)
    {
        this.update = update;
    }
    
    @Override
    protected void execute() throws BuildException
    {
        if (extent == null) {
            throw new BuildException("Missing extent attribute");
        }
        
        if (file == null) {
            throw new BuildException("Missing file attribute");
        }
        
        Properties storageProps = getStorageProperties();
        
        List<Properties> modelProperties = 
            MDRepositoryFactory.getRepositoryProperties(storageProps);
        if (modelProperties.isEmpty()) {
            throw new BuildException("No model configurations found");
        }

        DataSourceConfigurator dsConfigurator = 
            new DataSourceConfigurator(storageProps);
        dsConfigurator.initDataSource();
        try {
            HibernateConfigurator configurator = 
                new HibernateConfigurator(storageProps, modelProperties);
            
            Configuration config;
            try {
                config = 
                    configurator.newModelConfiguration(
                        extent, includeProviderSchema);
            } catch(NoSuchElementException e) {
                throw new BuildException(
                    "Model extent '" + extent + "' not found");
            }
            
            verbose("Exporting DDL for: " + extent);
            verbose("Exporting DDL to: " + file);
            verbose("Exporting DDL: " + (update ? "updates-only" : "full"));
            
            if (update) {
                // Brutal hack: SchemaUpdate doesn't support file output.
                // Redirect System.out and do our own delimiter insertion.
                SchemaUpdate updater = new SchemaUpdate(config);
                
                DdlPrintStream ps = new DdlPrintStream(file, delimiter);
                
                PrintStream originalPs = redirectSystemOut(ps);
                try {
                    updater.execute(true, false);
                } finally  {
                    redirectSystemOut(originalPs);
                }
            } else {
                SchemaExport exporter = new SchemaExport(config);
                exporter.setDelimiter(delimiter);
                exporter.setOutputFile(file);
                
                exporter.execute(false, false, false, true);
            }
            
            // Append the MOF ID generator table, if we're writing provider's
            // schema as well.
            
            // TODO: This is not strictly correct.  If we're asked to update
            // a completely empty schema we should generate this table.
            // For now, assume that update means the provider tables exist.
            if (includeProviderSchema && !update) {
                BufferedWriter writer = 
                    new BufferedWriter(new FileWriter(file, true));
                writer.write(
                    MofIdGenerator.generateCreateDdl(
                        storageProps, 
                        Dialect.getDialect(config.getProperties())));
                writer.write(delimiter);
                writer.newLine();
                writer.write(
                    MofIdGenerator.generateInsertDml(
                        storageProps, 
                        Dialect.getDialect(config.getProperties())));
                writer.write(delimiter);
                writer.newLine();
                writer.flush();
                writer.close();
            }
        } catch(Exception e) {
            throw new BuildException("error generating schema", e);
        } finally {
            dsConfigurator.close();
        }
    }
    
    private PrintStream redirectSystemOut(PrintStream ps)
    {
        PrintStream originalPs = System.out;
        
        System.setOut(ps);
        
        return originalPs;
    }
    
    static class DdlPrintStream extends PrintStream
    {
        private final String delimiter;
        
        public DdlPrintStream(String file, String delimiter)
        throws FileNotFoundException
        {
            super(file);
            
            this.delimiter = delimiter;
        }
        
        @Override
        public void println(String s)
        {
            super.print(s);
            super.println(delimiter);
        }
        
        @Override
        public void println()
        {
            super.println(delimiter);
        }
    }
}

// End ExportSchemaSubTask.java
