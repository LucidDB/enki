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
package org.eigenbase.enki.netbeans;

import java.io.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;
import org.netbeans.mdr.*;

/**
 * NBMDRepositoryWrapper wraps the Netbeans MDR implementation of 
 * {@link MDRepository} to provide an implementation of 
 * {@link EnkiMDRepository}. 
 * 
 * @author Stephan Zuercher
 */
public class NBMDRepositoryWrapper implements EnkiMDRepository
{
    private final NBMDRepositoryImpl impl;
    private final String jdbcUrl;
    
    public NBMDRepositoryWrapper(NBMDRepositoryImpl impl)
    {
        this.impl = impl;
        
        this.jdbcUrl = 
            System.getProperty(
                "MDRStorageProperty.org.netbeans.mdr.persistence.jdbcimpl.url");
        
    }

    // implement EnkiMDRepository
    public void dropExtentStorage(String extentName)
    {
        impl.unmountStorage(extentName);
        
        // REVIEW: SWZ: 12/12/07: This is pretty ugly.  Perhaps better to
        // just have the build script delete the catalog files, even if it
        // means having two code paths (since the Hibernate impl will 
        // implement this properly).  Could require more information to
        // be passed in (e.g., the catalog dir and HSQLDB file prefix, or in
        // Ant terms ${catalog.dir}/FooCatalog)
        if (jdbcUrl != null) {
            if (jdbcUrl.startsWith("jdbc:hsqldb:")) {
                String dbName = jdbcUrl.substring(13).split(";")[0];
                
                File dbFile = new File(dbName);
                File catalogDir = dbFile.getParentFile();
                if (catalogDir.exists() && catalogDir.isDirectory()) {
                    for(File file: catalogDir.listFiles()) {
                       if (file.getName().startsWith(dbFile.getName())) {
                           file.delete();
                       }
                    }
                }
            }
        }
    }
    
    public void addListener(MDRChangeListener arg0, int arg1)
    {
        impl.addListener(arg0, arg1);
    }

    public void addListener(MDRChangeListener arg0)
    {
        impl.addListener(arg0);
    }

    public void beginTrans(boolean arg0)
    {
        impl.beginTrans(arg0);
    }

    public RefPackage createExtent(
        String arg0,
        RefObject arg1,
        RefPackage[] arg2)
        throws CreationFailedException
    {
        return impl.createExtent(arg0, arg1, arg2);
    }

    public RefPackage createExtent(String arg0, RefObject arg1)
        throws CreationFailedException
    {
        return impl.createExtent(arg0, arg1);
    }

    public RefPackage createExtent(String arg0)
        throws CreationFailedException
    {
        return impl.createExtent(arg0);
    }

    public void endTrans()
    {
        impl.endTrans();
    }

    public void endTrans(boolean arg0)
    {
        impl.endTrans(arg0);
    }

    public RefBaseObject getByMofId(String arg0)
    {
        return impl.getByMofId(arg0);
    }

    public RefPackage getExtent(String arg0)
    {
        return impl.getExtent(arg0);
    }

    public String[] getExtentNames()
    {
        return impl.getExtentNames();
    }

    public void removeListener(MDRChangeListener arg0, int arg1)
    {
        impl.removeListener(arg0, arg1);
    }

    public void removeListener(MDRChangeListener arg0)
    {
        impl.removeListener(arg0);
    }

    public void shutdown()
    {
        impl.shutdown();
    }
    
    
}

// End NBMDRepositoryWrapper.java
