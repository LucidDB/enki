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
import java.net.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.ant.*;
import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.config.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.mdr.*;
import org.hibernate.cfg.*;
import org.hibernate.dialect.*;
import org.hibernate.tool.hbm2ddl.*;

/**
 * ExportSchemaSubTask exports schema-related scripts.  It operates in one of
 * three modes.  In model mode it generates the necessary DDL scripts for 
 * Enki/Hibernate backup/restore.  In model-plugin mode it does the same but
 * generates DDL only for model plugins, excluding the model's oridinal DDL.
 * In update mode it generates a script to update the named extent in database
 * specified by the storage properties to the current model's definition,
 * including tables associated with any plugins.
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
 *   <td>mode</td>
 *   <td>Sets the script generation mode.  Valid values are "model", 
 *       "model-plugin", or "update".</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>dir</td>
 *   <td>Model output directory.</td>
 *   <td>If mode is "model" or "model-plugin"</td>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>Location of output DDL script for update mode script generation.</td>
 *   <td>If mode is "update"</td>
 * </tr>
 * <tr>
 *   <td>includeProviderSchema</td>
 *   <td>Boolean flag controlling the generation of DDL for the Enki/Hibernate 
 *       provider in update mode script generation. Ignored in "model" and
 *       "model-plugin" modes.
 *       Defaults to <code>true</code>.</td>
 *   <td>No</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class ExportSchemaSubTask
    extends EnkiTask.SubTask
{
    public static final String DELIMITER = ";";
    
    private String extent;
    private String dir;
    private String file;
    private boolean update;
    private boolean plugin;
    private boolean includeProviderSchema;
    
    public ExportSchemaSubTask()
    {
        super("exportSchema/Hibernate");

        this.update = false;
        this.plugin = false;
        this.includeProviderSchema = true;
    }

    public void setExtent(String extent)
    {
        this.extent = extent;
    }
    
    public void setFile(File file)
    {
        this.file = file.getPath();
    }
    
    public void setDir(File dir)
    {
        this.dir = dir.getPath();
    }
    
    public void setIncludeProviderSchema(boolean includeProviderSchema)
    {
        this.includeProviderSchema = includeProviderSchema;
    }
    
    public void setMode(String mode) throws BuildException
    {
        if ("update".equals(mode)) {
            this.update = true;
            this.plugin = false;
        } else if ("model".equals(mode)) {
            this.update = false;
            this.plugin = false;
        } else if ("model-plugin".equals(mode)) {
            this.update = false;
            this.plugin = true;
        } else {
            throw new BuildException("invalid mode '" + mode + "'");
        }
    }
    
    @Override
    protected void execute() throws BuildException
    {
        if (extent == null) {
            throw new BuildException("Missing extent attribute");
        }
        
        if (update) {
            if (file == null) {
                throw new BuildException("update requires file attribute");
            }
            
            if (dir != null) {
                throw new BuildException(
                    "update is mutually exclusive with dir attribute");                
            }
        } else {
            if (dir == null) {
                throw new BuildException("missing required dir attribute");
            }
            
            if (file != null) {
                throw new BuildException(
                    "file attribute may only be used when update is true");
            }
        }
        
        Properties storageProps = getStorageProperties();
        
        // Emit DDL with the prefix reference as the prefix.
        storageProps.setProperty(
            HibernateMDRepository.PROPERTY_STORAGE_TABLE_PREFIX, 
            HibernateMappingHandler.TABLE_REF);
        
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
                        extent, update && includeProviderSchema);
                
                configurator.addModelIndexConfiguration(config, extent);
            } catch(NoSuchElementException e) {
                throw new BuildException(
                    "Model extent '" + extent + "' not found");
            }
            
            verbose("Export DDL mode: " + (update ? "update" : "model"));
            verbose("Export DDL for: " + extent);
            verbose("Export DDL to: " + (update ? file : dir));
            
            if (update) {
                // Brutal hack: SchemaUpdate doesn't support file output.
                // Redirect System.out and do our own delimiter insertion.
                SchemaUpdate updater = new SchemaUpdate(config);
                
                DdlPrintStream ps = new DdlPrintStream(file, DELIMITER);
                
                PrintStream originalPs = redirectSystemOut(ps);
                try {
                    updater.execute(true, false);
                } finally  {
                    redirectSystemOut(originalPs);
                }
                
                // TODO: If we're asked to update a completely empty schema 
                // we should generate the MOF ID table. For now, assume that 
                // update means the MOF ID table exists.
            } else {
                File metaInfEnkiDir = 
                    new File(dir, MDRepositoryFactory.META_INF_ENKI_DIR);
                File providerFile =
                    new File(
                        metaInfEnkiDir, HibernateMDRepository.PROVIDER_DDL);
                File createFile = 
                    new File(
                        metaInfEnkiDir,
                        HibernateMDRepository.MODEL_CREATE_DDL);
                File dropFile =
                    new File(
                        metaInfEnkiDir, HibernateMDRepository.MODEL_DROP_DDL);
                
                if (plugin) {
                    ModelDescriptor modelDesc = 
                        configurator.getModelMap().get(extent);
                    
                    export(config, createFile, true, modelDesc.createDdl);
                    export(config, dropFile, false, modelDesc.dropDdl);
                } else {
                    export(config, createFile, true);
                    export(config, dropFile, false);
                }
                
                // Generate provider schema
                config = configurator.newConfiguration(true);
                export(config, providerFile, true);
                
                // Append the MOF ID generator table.
                BufferedWriter writer = 
                    new BufferedWriter(new FileWriter(providerFile, true));
                writer.write(
                    MofIdGenerator.generateCreateDdl(
                        storageProps, 
                        Dialect.getDialect(config.getProperties())));
                writer.write(DELIMITER);
                writer.newLine();
                writer.write(
                    MofIdGenerator.generateInsertDml(
                        storageProps, 
                        Dialect.getDialect(config.getProperties())));
                writer.write(DELIMITER);
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

    private void export(Configuration config, File exportFile, boolean create)
    {
        SchemaExport exporter = new SchemaExport(config);
        exporter.setDelimiter(DELIMITER);
        exporter.setOutputFile(exportFile.getPath());
        exporter.execute(false, false, !create, create);
    }
    
    /**
     * Export DDL for the given Hibernate Configuration, stripping out DDL
     * already present in baseDdl.  This is useful for generating DDL to
     * create additional tables for a model plugin.
     * 
     * @param config Hibernate configuration
     * @param exportFile final output file
     * @param create if true generate create statements, else drop
     * @param baseDdl previously created DDL (matching create or drop) to 
     *                filter
     * @throws IOException if there's an error generating the SQL
     */
    private void export(
        Configuration config, 
        File exportFile, 
        boolean create, 
        URL baseDdl)
    throws IOException
    {
        File tempFile = File.createTempFile("enkiCodeGenerator", ".sql");
        
        try {
            // Export all the DDL
            export(config, tempFile, create);
            
            // Process base create SQL and strip these tables out of plugin's
            // SQL.
            List<String> prefixes = new ArrayList<String>();
            BufferedReader rdr = 
                new BufferedReader(
                    new InputStreamReader(
                        baseDdl.openStream(), "UTF-8"));
            try {
                String line;
                boolean startOfStmt = true;
                while((line = rdr.readLine()) != null) {
                    if (startOfStmt) {
                        // N.B.: Sometimes Hibernate re-orders columns.  Assume
                        // everything is okay and ignore lines based on data
                        // up to the first left paren.
                        String prefix = line.split("\\(")[0];
                        
                        prefixes.add(prefix);
                        startOfStmt = false;
                    } 
                    
                    if (line.trim().endsWith(DELIMITER)) {
                        startOfStmt = true;
                    }
                }
            } finally {
                rdr.close();
            }
            
            BufferedWriter out = 
                new BufferedWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(exportFile), "UTF-8"));
            try {
                BufferedReader in =
                    new BufferedReader(
                        new InputStreamReader(
                            new FileInputStream(tempFile), "UTF-8"));
                try {
                    String line;
                    boolean startOfStmt = true;
                    boolean ignoringStmt = false;
                    while((line = in.readLine()) != null) {
                        if (startOfStmt) {
                            Iterator<String> iter = prefixes.iterator();
                            while(iter.hasNext()) {
                                String prefix = iter.next();
                                
                                if (line.startsWith(prefix)) {
                                    // Ignore remainder of statement's lines
                                    // and stop looking for this prefix.
                                    ignoringStmt = true;
                                    iter.remove();
                                    break;
                                }
                            }
                            
                            startOfStmt = false;
                        }
                        
                        if (!ignoringStmt) {
                            out.write(line);
                            out.newLine();
                        }
                        
                        if (line.trim().endsWith(DELIMITER)) {
                            startOfStmt = true;
                            ignoringStmt = false;
                        }
                    }
                    
                    out.flush();
                } finally {
                    in.close();
                }
            } finally {
                out.close();
            }
        } finally {
            tempFile.delete();
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
