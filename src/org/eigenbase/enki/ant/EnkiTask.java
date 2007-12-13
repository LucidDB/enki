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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.eigenbase.enki.mdr.*;
import org.netbeans.api.mdr.*;

/**
 * EnkiTask is an Ant task with several sub-tasks that can be combined
 * in various orders to produce useful combinations.
 * 
 * <p>Attributes:
 * <table>
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
    
    private List<SubTask> subTasks;

    private MDRepository mdr;
    
    public EnkiTask()
    {
        this.subTasks = new ArrayList<SubTask>();
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
                
                oldClassLoader = Thread.currentThread().getContextClassLoader();
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
                    Thread.currentThread().setContextClassLoader(oldClassLoader);                
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
    
    void setMDRepository(MDRepository mdr)
    {
        this.mdr = mdr;
    }
    
    MDRepository getMDRepository()
    {
        return getMDRepository(false);
    }
    
    MDRepository getMDRepository(boolean create)
    {
        if (mdr != null) {
            return mdr;
        }
        
        if (!create) {
            throw new BuildException("MDR repository not instantiated");
        }
        
        Properties props = getProperties();
        
        mdr = MDRepositoryFactory.newMDRepository(props);
        
        return mdr;
    }
    
    Properties getProperties() throws BuildException
    {
        if (propertiesFile == null) {
            throw new BuildException(
                "Configuration properties for repository storage must be passed using the \"propertiesFile\" attribute");
        }
        
        File propsFile = new File(propertiesFile);
        
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(propsFile));
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
        
        return props;
    }
    
    abstract static class SubTask
    {
        private final String name;
        protected EnkiTask task;
        
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
            Properties props = task.getProperties();
            String enkiImplType = 
                props.getProperty(MDRepositoryFactory.ENKI_IMPL_TYPE);
            MdrProvider implType = MdrProvider.valueOf(enkiImplType);
            return implType;
        }
    }
}

// End EnkiTask.java
