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
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.netbeans.*;
import org.netbeans.api.mdr.*;

/**
 * EnkiTask is an Ant task with several sub-tasks that can be combined
 * in various orders to produce useful combinations.
 * 
 * <p>Attributes:
 * <table border="1">
 * <tr>
 *   <th>Name</th>
 *   <th>Description</th>
 *   <th>Used By</th>
 *   <th>Required?</th>
 * </tr>
 * <tr>
 *   <td>propertiesFile</td>
 *   <td>The path to a properties file containing Enki storage properties.</td>
 *   <td>{@link CreateExtentSubTask}, {@link DropExtentSubTask}, 
 *       {@link ImportXmiSubTask}, {@link WriteDtdSubTask}</td>
 *   <td>Yes, if a using sub-task is invoked</td>
 * </tr>
 * <tr>
 *   <td>modelPathRef</td>
 *   <td>Additional class path reference for model JAR files.</td>
 *   <td>{@link CreateExtentSubTask}, {@link DropExtentSubTask}, 
 *       {@link ImportXmiSubTask}, {@link WriteDtdSubTask}</td>
 *   <td>No</td>
 * </tr>
 * <tr>
 *   <td>logConfigFile</td>
 *   <td>If present, causes the given {@link java.util.logging} configuration
 *       properties to be substituted for the currently configured logging
 *       configuration.  The original configuration is reset when the task
 *       completes.</td>
 *   <td>All</td>
 *   <td>No</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class EnkiTask
    extends Task
{
    private String propertiesFile;
    
    private String logConfigFile;

    private Reference modelPathRef;
    
    private List<StorageProperty> storagePropertyElements;
    private List<SubTask> subTasks;
    private PropertySet propertySet;
    
    private Properties storageProperties;
    private EnkiMDRepository mdr;
    
    public EnkiTask()
    {
        this.subTasks = new ArrayList<SubTask>();
        this.storagePropertyElements = new ArrayList<StorageProperty>();
    }
    
    public void setPropertiesFile(String propertiesFile)
    {
        this.propertiesFile = propertiesFile;
    }
    
    public void setLogConfigFile(String logConfigFile)
    {
        this.logConfigFile = logConfigFile;
    }
    
    public void setModelPathRef(Reference modelPathRef)
    {
        this.modelPathRef = modelPathRef;
    }

    public void execute() throws BuildException
    {
        for(SubTask subTask: subTasks) {
            for(SubTask otherSubTask: subTasks) {
                if (subTask == otherSubTask) {
                    continue;
                }
                
                if (!subTask.isCombinableWith(otherSubTask)) {
                    throw new BuildException(
                        "Sub-task '" + subTask + "' is not combinable with '" 
                        + otherSubTask + "'"); 
                }
            }
        }
        
        modifyLogManager();

        try {
            ClassLoader oldClassLoader = null;
            if (modelPathRef != null) {
                Path path = (Path)modelPathRef.getReferencedObject();
    
                ArrayList<URL> urls = new ArrayList<URL>();
                for(String pathMember: path.list()) {
                    try {
                        urls.add(new File(pathMember).toURL());
                    } catch (MalformedURLException e) {
                        throw new BuildException(e);
                    }
                }
                
                oldClassLoader = 
                    Thread.currentThread().getContextClassLoader();
                URLClassLoader modelClassLoader = 
                    new URLClassLoader(
                        urls.toArray(new URL[urls.size()]),
                        getClass().getClassLoader());
                
                Thread.currentThread().setContextClassLoader(modelClassLoader);
            }
            
            try {
                for(SubTask subTask: subTasks) {
                    System.out.println(subTask.toString());
                    subTask.execute();
                }
            } finally {
                if (oldClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(
                        oldClassLoader);                
                }
            }
            
            if (mdr != null) {
                mdr.shutdown();
            }
        } finally {
            resetLogManager();
        }
    }

    private void modifyLogManager()
    {
        if (logConfigFile != null) {
            FileInputStream logConfigIn;
            try {
                logConfigIn = new FileInputStream(logConfigFile);
            } catch (FileNotFoundException e) {
                throw new BuildException(e);
            }
            
            LogManager.getLogManager().reset();
            try {
                LogManager.getLogManager().readConfiguration(logConfigIn);
                logConfigIn.close();
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
        
        if (getMdrProvider() == MdrProvider.NETBEANS_MDR) {
            Thread thread = Thread.currentThread();
            ClassLoader oldContextClassLoader = thread.getContextClassLoader();
            
            thread.setContextClassLoader(MdrTraceUtil.class.getClassLoader());
            
            try {
                // Configure Netbeans logging
                Logger mdrLogger = Logger.getLogger(getClass().getName());
                MdrTraceUtil.integrateTracing(mdrLogger);
            } finally {
                thread.setContextClassLoader(oldContextClassLoader);
            }
        }
    }
    
    private void resetLogManager()
    {
        if (logConfigFile != null) {
            LogManager.getLogManager().reset();
            try {
                LogManager.getLogManager().readConfiguration();
            } catch(Exception e) {
                throw new BuildException(e);
            }
        }
    }

    public PropertySet createPropertySet()
    {
        PropertySet propSet = new PropertySet();
        propertySet = propSet;
        return propSet;
    }
    
    public StorageProperty createStorageProperty()
    {
        StorageProperty storageProp = new StorageProperty();
        storagePropertyElements.add(storageProp);
        return storageProp;
    }
    
    public CreateExtentSubTask createCreateExtent()
    {
        CreateExtentSubTask createExtentSubTask = 
            new CreateExtentSubTask("createExtent");
        subTasks.add(createExtentSubTask);
        createExtentSubTask.setTask(this);
        return createExtentSubTask;
    }
    
    public DropExtentSubTask createDropExtent()
    {
        DropExtentSubTask dropExtentSubTask = 
            new DropExtentSubTask("dropExtent");
        subTasks.add(dropExtentSubTask);
        dropExtentSubTask.setTask(this);
        return dropExtentSubTask;
    }
    
    public ImportXmiSubTask createImportXmi()
    {
        ImportXmiSubTask importXmiSubTask = new ImportXmiSubTask("importXmi");
        subTasks.add(importXmiSubTask);
        importXmiSubTask.setTask(this);
        return importXmiSubTask;
    }
    
    public ExportXmiSubTask createExportXmi()
    {
        ExportXmiSubTask exportXmiSubTask = new ExportXmiSubTask("exportXmi");
        subTasks.add(exportXmiSubTask);
        exportXmiSubTask.setTask(this);
        return exportXmiSubTask;
    }
    
    public PrintExtentNames createPrintExtentNames()
    {
        PrintExtentNames printExtentNames = 
            new PrintExtentNames("printExtentNames");
        subTasks.add(printExtentNames);
        printExtentNames.setTask(this);
        return printExtentNames;
    }
    
    public WriteDtdSubTask createWriteDtd()
    {
        WriteDtdSubTask writeDtdSubTask = new WriteDtdSubTask("writeDtd");
        subTasks.add(writeDtdSubTask);
        writeDtdSubTask.setTask(this);
        return writeDtdSubTask;
    }
    
    public MapJavaSubTask createMapJava()
    {
        MapJavaSubTask mapJavaSubTask = new MapJavaSubTask("mapJava");
        subTasks.add(mapJavaSubTask);
        mapJavaSubTask.setTask(this);
        return mapJavaSubTask;
    }
    
    void setMDRepository(EnkiMDRepository mdr)
    {
        this.mdr = mdr;
    }
    
    EnkiMDRepository getMDRepository()
    {
        return getMDRepository(false);
    }
    
    EnkiMDRepository getMDRepository(boolean create)
    {
        if (mdr != null) {
            return mdr;
        }
        
        if (!create) {
            throw new BuildException("MDR repository not instantiated");
        }
        
        loadProperties();
        
        mdr = MDRepositoryFactory.newMDRepository(storageProperties);
        
        return mdr;
    }
    
    MdrProvider getMdrProvider()
    {
        loadProperties();
        
        String enkiImplType = 
            storageProperties.getProperty(MDRepositoryFactory.ENKI_IMPL_TYPE);
        if (enkiImplType == null) {
            return null;
        }
        
        MdrProvider implType = MdrProvider.valueOf(enkiImplType);
        return implType;
    }
    
    void loadProperties() throws BuildException
    {
        if (storageProperties != null) {
            return;
        }
        
        Properties props = new Properties();

        if (propertiesFile != null && propertiesFile.length() > 0) {
            // Cannot have properties file AND storageProperty elements.
            if (!storagePropertyElements.isEmpty()) {
                throw new BuildException(
                    "Use of the \"propertiesFile\" attribute and \"storageProperty\" may not be combined");
            }
            
            File propsFile = new File(propertiesFile);
            
            try {
                props.load(new FileInputStream(propsFile));
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
        } else if (!storagePropertyElements.isEmpty()) {
            for(StorageProperty storageProp: storagePropertyElements) {
                props.put(storageProp.getName(), storageProp.getValue());
            }
        }

        if (propertySet != null) {
            Pattern propRegex = 
                Pattern.compile("\\$(\\{([^$}]+)\\}|\\$)");
            
            // Use propertySet to do substitution into props.
            Properties substProps = propertySet.getProperties();
            for(Map.Entry<Object, Object> entry: props.entrySet()) {
                String value = entry.getValue().toString();
                
                StringBuffer newValue = new StringBuffer();
                Matcher propMatcher = propRegex.matcher(value);
                while(propMatcher.find()) {
                    String propName = propMatcher.group(1);
                    String replacement;
                    if (propName.equals("$")) {
                        // found $$, replace with $
                        replacement = "\\$";
                    } else {
                        // found property name
                        propName = propMatcher.group(2);
                        replacement = substProps.getProperty(propName);
                        if (replacement == null) {
                            // leave the unresolved property reference
                            replacement = "$0";
                        }
                    }
                    
                    propMatcher.appendReplacement(newValue, replacement);
                }
                propMatcher.appendTail(newValue);
                
                entry.setValue(newValue.toString());
            }
        }
        
        storageProperties = props;
    }
    
    /**
     * SubTask is an abstract base class for sub-tasks nested with an outer
     * {@link EnkiTask}.
     */
    abstract static class SubTask
    {
        private final String name;
        private EnkiTask task;
        
        protected SubTask(String name)
        {
            this.name = name;
        }
        
        abstract void execute() throws BuildException;
        
        void setTask(EnkiTask task)
        {
            this.task = task;
        }
        
        boolean isCombinableWith(SubTask subTask)
        {
            return true;
        }
        
        public String toString()
        {
            return name;
        }

        protected MdrProvider getMdrProvider()
        {
            return task.getMdrProvider();
        }
        
        protected MDRepository getMDRepository()
        {
            return task.getMDRepository();
        }
        
        protected EnkiMDRepository getMDRepository(boolean create)
        {
            return task.getMDRepository(create);
        }
    }
}

// End EnkiTask.java
