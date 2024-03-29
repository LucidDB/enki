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
package org.eigenbase.enki.codegen;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.jmi.reflect.*;
import javax.jmi.xmi.*;

import org.netbeans.api.mdr.*;
import org.netbeans.api.xmi.*;

/**
 * MdrGenerator loads an MDR model and invokes an abstract method to configure
 * the desired {@link Handler} implementations.
 * 
 * @author Stephan Zuercher
 */
public abstract class MdrGenerator
    extends GeneratorBase
{
    public static final String DEFAULT_ENKI_MODEL_EXTENT_NAME = 
        "EnkiModelExtent";

    private static final String NBMDR_STORAGE_FACTORY_PROPERTY =
        "org.netbeans.mdr.storagemodel.StorageFactoryClassName";
    private static final String NBMDR_MEM_STORAGE_FACTORY_VALUE =
        "org.netbeans.mdr.persistence.memoryimpl.StorageFactoryImpl";
    
    private MDRManager mdrMgr;
    private MDRepository mdr;
    private String extentName;
    private String savedStorageFactory;
    
    public MdrGenerator()
    {
        super();
    }

    public void setExtentName(String extentName)
    {
        this.extentName = extentName;
    }
    
    public String getExtentName()
    {
        return extentName;
    }
    
    /**
     * Configures a concrete base class's desired handlers.  Must make
     * one or more calls to {@link #addHandler(Handler)}.
     */
    protected abstract void configureHandlers();
    
    public final void execute() throws GenerationException
    {
        boolean configuredMemoryRepos = false;
        
        try {
            if (!xmiFile.exists()) {
                throw new FileNotFoundException(xmiFile.toString());
            }
            
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                
                throw new IOException(
                    "Cannot create directory '" + outputDir + "'");
            }
            
            if (extentName == null) {
                throw new GenerationException("Extent name not set");
            }
        
            // Fix ENK-6: Netbean's BTree repository implementation has a bug
            // which can cause ordered multi-value associations to be 
            // incorrectly re-ordered during XMI import.  This causes 
            // Attributes and References to be intermittently re-ordered.
            // Memory repository seems to avoid this problem.
            configureMemoryRepos(true);
            configuredMemoryRepos = true;
            
            configureHandlers();
            
            mdrMgr = getDefaultMdrManager();
            mdr = mdrMgr.getDefaultRepository();
        
            instantiate();
            readXmi();
            generateCode();
        } catch(GenerationException e) {
            throw e;
        } catch(Throwable t) {
            throw new GenerationException(t);
        } finally {
            if (mdr != null) {
                mdr.shutdown();
            }
            
            if (configuredMemoryRepos) {
                configureMemoryRepos(false);
            }
        }
    }

    /**
     * Provides processing for executing a concrete subclass from the command
     * line.
     * 
     * @param args XMI file name, output directory
     * @param useGenerics if true, use Java generic types
     */
    protected void doMain(String[] args, boolean useGenerics)
    {
        try {
            String xmiFileName = args[0];
            String outputDirName = args[1];
    
            Pattern optPattern = Pattern.compile("([^=]+)=(.+)");
            Map<String, String> options = new HashMap<String, String>();
            for(int i = 2; i < args.length; i++) {
                Matcher m = optPattern.matcher(args[i]);
                if (!m.matches()) {
                    System.err.println(
                        "Argument [" 
                        + args[i] 
                        + "] does not match the option regex");
                    return;
                }
                
                String option = m.group(1);
                String value = m.group(2);

                if (value.startsWith("'")) {
                    if (!value.endsWith("'")) {
                        System.err.println(
                            "Missing quote in [" + args[i] + "]");
                        return;
                    }
                    
                    value = value.substring(1, value.length() - 1);
                    value = value.replaceAll("''", "'");
                }
                
                options.put(option, value);
            }
            
            setXmiFile(new File(xmiFileName));
            setOutputDirectory(new File(outputDirName));
            setExtentName(DEFAULT_ENKI_MODEL_EXTENT_NAME);
            CodeGenUtils.setEnableGenerics(useGenerics);
            if (!options.isEmpty()) {
                setOptions(options);
            }
            execute();
            System.out.println("Done");
        }
        catch(Exception e) {
            System.err.println(
                "Usage: java " + getClass().toString() + 
                " <xmi-file> <out-dir> [name=value ...]");
            System.err.println();
            e.printStackTrace();
        }        
    }
    
    private void configureMemoryRepos(boolean enable)
    {
        Properties sysProps = System.getProperties();
        if (enable) {
            savedStorageFactory = 
                sysProps.getProperty(NBMDR_STORAGE_FACTORY_PROPERTY);
            sysProps.setProperty(
                NBMDR_STORAGE_FACTORY_PROPERTY, 
                NBMDR_MEM_STORAGE_FACTORY_VALUE);
        } else {
            sysProps.remove(NBMDR_STORAGE_FACTORY_PROPERTY);
            if (savedStorageFactory != null) {
                sysProps.setProperty(
                    NBMDR_STORAGE_FACTORY_PROPERTY, 
                    savedStorageFactory);
            }
        }
    }

    private MDRManager getDefaultMdrManager()
    {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();                
        Thread.currentThread().setContextClassLoader( 
            this.getClass().getClassLoader() );
        
        MDRManager newMdrMgr = MDRManager.getDefault();
        
        Thread.currentThread().setContextClassLoader( oldLoader );

        return newMdrMgr;
    }
    
    private void begin()
    {
        mdr.beginTrans(true);
    }
    
    private void rollback()
    {
        mdr.endTrans(true);
    }
    
    private void commit()
    {
        mdr.endTrans(false);
    }
    
    private void instantiate() throws CreationFailedException 
    {
        begin();
        boolean rollback = true;
        try {
            mdr.createExtent(extentName);
            
            rollback = false;
        }
        finally {
            if (rollback) {
                rollback();
            } else {
                commit();
            }
        }
    }
    
    private void readXmi() throws IOException, MalformedXMIException
    {
        begin();
        boolean rollback = true;
        try {
            XMIReader xmiReader = 
                XMIReaderFactory.getDefault().createXMIReader();
            xmiReader.read( 
                xmiFile.toURL().toString(), 
                getExtent());
            
            rollback = false;
        }
        finally {
            if (rollback) {
                rollback();
            } else {
                commit();
            }
        }
    }
    
    private void generateCode() throws GenerationException
    {
        begin();
        boolean rollback = true;
        try {
            RefBaseObject obj = getExtent(); 
            
            visitRefBaseObject(obj);
            
            rollback = false;
        }
        finally {
            if (rollback) {
                rollback();
            } else {
                commit();
            }
        }        
    }

    public RefPackage getExtent(String name)
    {
        return mdr.getExtent(name);
    }
    
    private RefPackage getExtent()
    {
        return mdr.getExtent(extentName);
    }
    
}
