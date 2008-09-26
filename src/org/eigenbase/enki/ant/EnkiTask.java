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
    private static final String ENKI_COMPAT_STORAGE_PROPS_FILE =
        "enki.compatibility.storagePropertiesFile";
    
    private static final String ENKI_COMPAT_SUBST_PROP_NAME = 
        "enki.compatibility.substPropertyName";
    
    private static final String ENKI_COMPAT_SUBST_PROP_VALUE = 
        "enki.compatibility.substPropertyValue";

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
        } catch(RuntimeException e) {
            Logger.getLogger("org.eigenbase.enki.ant").log(
                Level.SEVERE, "Build error", e);
            throw e;
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
    
    // REVIEW: SWZ: 2008-03-26: Compatibility of old-style red-zone scripts.
    // Remove when no longer needed.
    @Deprecated
    public ImportXmiSubTask createReadXMI()
    {
        return createImportXmi();
    }
    
    public ExportXmiSubTask createExportXmi()
    {
        ExportXmiSubTask exportXmiSubTask = new ExportXmiSubTask("exportXmi");
        subTasks.add(exportXmiSubTask);
        exportXmiSubTask.setTask(this);
        return exportXmiSubTask;
    }

    // REVIEW: SWZ: 2008-03-26: Compatibility of old-style red-zone scripts.
    // Remove when no longer needed.
    @Deprecated
    public ExportXmiSubTask createWriteXMI()
    {
        return createExportXmi();
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

    public void add(EnkiSubTask enkiSubTask)
    {
        SubTask providerSpecificSubTask = (SubTask)enkiSubTask;
        subTasks.add(providerSpecificSubTask);
        providerSpecificSubTask.setTask(this);
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
        
        Properties props = getStorageProperties();
        
        mdr = MDRepositoryFactory.newMDRepository(props);
        
        return mdr;
    }
    
    MdrProvider getMdrProvider() throws BuildException
    {
        Properties props = getStorageProperties(false);
        if (props == null) {
            return null;
        }
        
        String enkiImplType = 
            props.getProperty(MDRepositoryFactory.ENKI_IMPL_TYPE);
        if (enkiImplType == null) {
            return null;
        }
        
        MdrProvider implType = MdrProvider.valueOf(enkiImplType);
        return implType;
    }
    
    Properties getStorageProperties() throws BuildException
    {
        return getStorageProperties(true);
    }
    
    Properties getStorageProperties(boolean failOnNotFound) 
        throws BuildException
    {
        if (storageProperties == null) {
            storageProperties = 
                loadProperties(
                    propertiesFile,
                    propertySet,
                    storagePropertyElements);
            
            if (storageProperties == null && failOnNotFound) {
                throw new BuildException("Unable to find storage properties");
            }
        }
        
        return storageProperties;
    }
    
    private static Properties loadProperties(
        String propertiesFile,
        PropertySet substPropertySet,
        List<StorageProperty> storagePropertyElements) 
    throws BuildException
    {
        Properties substituionProperties = null;
        if (substPropertySet != null) {
            substituionProperties = substPropertySet.getProperties();
        }
        
        return loadProperties(
            propertiesFile,
            substituionProperties, 
            storagePropertyElements,
            true);
    }
    
    private static Properties loadProperties(
        String propertiesFile,
        Properties substProps,
        List<StorageProperty> storagePropertyElements,
        boolean recurse) 
    throws BuildException
    {
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

        // Use substProps to do substitution on values in props.
        if (substProps != null) {
            Pattern propRegex = 
                Pattern.compile("\\$(\\{([^$}]+)\\}|\\$)");
            
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
                        } else {
                            // Escape backslashes so the propMatcher doesn't
                            // remove them.
                            replacement = replacement.replace("\\", "\\\\");
                        }
                    }
                    
                    propMatcher.appendReplacement(newValue, replacement);
                }
                propMatcher.appendTail(newValue);
                
                entry.setValue(newValue.toString());
            }
        }
        
        // REVIEW: SWZ: 2008-03-26: Remove this block when no longer needed.
        // You can also collapse this method in to the no-argument version
        // of loadProperties().

        // Provide backwards compatibility to pre-Enki scripts, which assume
        // that Netbeans MDR storage properties are provided as system
        // properties by Ant scripts.  This is normally accomplished in some
        // shared custom Ant task/macro -- assume the macro is modified to
        // pass the storage properties file via a system property.
        if (propertiesFile == null && props.isEmpty()) {
            if (recurse) {
                String compatPropsFile = 
                    System.getProperty(ENKI_COMPAT_STORAGE_PROPS_FILE);

                // Provide the one substitution most storage properties need.
                Properties compatSubstProps = new Properties();
                
                String substPropName = 
                    System.getProperty(ENKI_COMPAT_SUBST_PROP_NAME);
                if (substPropName != null) {
                    String substPropValue = 
                        System.getProperty(ENKI_COMPAT_SUBST_PROP_VALUE);
                    
                    compatSubstProps.put(substPropName, substPropValue);
                }
                
                // Try again.
                props = 
                    loadProperties(
                        compatPropsFile,
                        compatSubstProps,
                        storagePropertyElements, 
                        false);
            } else {
                // Failed on recursion, perhaps the compatibility properties
                // file wasn't set.
                return null;
            }

            if (props != null &&
                !props.containsKey(MDRepositoryFactory.ENKI_IMPL_TYPE))
            {
                final MdrProvider defaultProvider = MdrProvider.NETBEANS_MDR;
                props.put(
                    MDRepositoryFactory.ENKI_IMPL_TYPE,
                    defaultProvider.toString());
                System.out.println(
                    "Forcing MDR provider '" + defaultProvider + "'");
            }
        }
        
        return props;
    }
    
    /**
     * SubTask is an abstract base class for sub-tasks nested with an outer
     * {@link EnkiTask}.
     */
    public abstract static class SubTask implements EnkiSubTask
    {
        private final String name;
        private EnkiTask task;
        
        protected SubTask(String name)
        {
            this.name = name;
        }
        
        protected abstract void execute() throws BuildException;
        
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
        
        protected Properties getStorageProperties()
        {
            return task.getStorageProperties(true);
        }
        
        protected void verbose(String msg)
        {
            task.log(msg, Project.MSG_VERBOSE);
        }
    }
    
    public static class DuplicateProviderSpecificSubTaskException 
        extends RuntimeException
    {
        private static final long serialVersionUID = -1087481338333794155L;

        private final String name;
        private final Class<? extends SubTask> oldType;
        private final Class<? extends SubTask> newType;
        
        public DuplicateProviderSpecificSubTaskException(
            String name, 
            Class<? extends SubTask> oldType, 
            Class<? extends SubTask> newType)
        {
            this.name = name;
            this.oldType = oldType;
            this.newType = newType;
        }
        
        public String getMessage()
        {
            return 
                "duplicate provider-specific sub-task named '" + name 
                + "'; old class: " + oldType.getName() 
                + ", new class: " + newType.getName();
        }
    }
}

// End EnkiTask.java
