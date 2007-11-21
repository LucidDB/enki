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
package org.eigenbase.enki.codegen;

import java.io.*;

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
    private static final String ENKI_MODEL_EXTENT_NAME = "EnkiModelExtent";
    private MDRManager mdrMgr;
    private MDRepository mdr;
    
    public MdrGenerator()
    {
        super();
    }

    /**
     * Configures a concrete base class's desired handlers.  Must make
     * one or more calls to {@link #addHandler(Handler)}.
     */
    protected abstract void configureHandlers();
    
    public final void execute() throws GenerationException
    {
        try {
            if (!xmiFile.exists()) {
                throw new FileNotFoundException(xmiFile.toString());
            }
            
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                
                throw new IOException(
                    "Cannot create directory '" + outputDir + "'");
            }
            
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
        }
    }

    private MDRManager getDefaultMdrManager()
    {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();                
        Thread.currentThread().setContextClassLoader( 
            this.getClass().getClassLoader() );
        
        MDRManager mdrMgr = MDRManager.getDefault();
        
        Thread.currentThread().setContextClassLoader( oldLoader );

        return mdrMgr;
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
            mdr.createExtent(ENKI_MODEL_EXTENT_NAME);
            
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

    private RefPackage getExtent()
    {
        return mdr.getExtent(ENKI_MODEL_EXTENT_NAME);
    }
    
}
