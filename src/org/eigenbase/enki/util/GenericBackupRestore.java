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
package org.eigenbase.enki.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;
import javax.jmi.xmi.*;

import org.eigenbase.enki.mdr.*;
import org.netbeans.api.xmi.*;

/**
 * GenericBackupRestore implements repository backup and restore using XMI
 * export and import.
 * 
 * @author Stephan Zuercher
 */
public class GenericBackupRestore
{
    private GenericBackupRestore()
    {
    }
    
    public static void backupExtent(
        EnkiMDRepository repos,
        String extentName,
        OutputStream stream)
    throws EnkiBackupFailedException
    {
        try {
            RefPackage refPackage = repos.getExtent(extentName);
            XmiWriter xmiWriter = 
                XMIWriterFactory.getDefault().createXMIWriter();
            xmiWriter.write(stream, refPackage, "1.2");
        } catch(IOException e) {
            throw new EnkiBackupFailedException(e);
        }
    }
    
    public static void restoreExtent(
        EnkiMDRepository repos,
        Class<? extends InputStream> filterStreamCls,
        String extentName,
        String metaPackageExtentName,
        String metaPackageName,
        InputStream stream) 
    throws EnkiRestoreFailedException
    {
        repos.beginTrans(true);
        boolean rollback = true;
        try {
            RefPackage extent = repos.getExtent(extentName);
            if (extent != null) {
                delete(extent);
                extent.refDelete();
                extent = null;
            }
            
            ModelPackage modelPackage =
                (ModelPackage)repos.getExtent(metaPackageExtentName);
            if (modelPackage == null) {
                throw new EnkiRestoreFailedException(
                    "Must create metamodel extent '" 
                    + metaPackageExtentName 
                    + "' before restoring backup");
            }
            
            MofPackage metaPackage = null;
            for (Object o: modelPackage.getMofPackage().refAllOfClass()) {
                MofPackage result = (MofPackage) o;
                if (result.getName().equals(metaPackageName)) {
                    metaPackage = result;
                    break;
                }
            }
            
            if (metaPackage == null) {
                throw new EnkiRestoreFailedException(
                    "Could not find metamodel package '" 
                    + metaPackageName 
                    + "' in extent '" 
                    + metaPackageExtentName 
                    + "'");
            }

            try {
                extent = repos.createExtent(extentName, metaPackage);
                
                XmiReader xmiReader = 
                    XMIReaderFactory.getDefault().createXMIReader();
        
                InputStream filter;
                if (filterStreamCls == null) {
                    filter = stream;
                } else {
                    filter = makeXmiFilterStream(stream, filterStreamCls);
                }
                
                xmiReader.read(
                    filter,
                    null,
                    extent);
            } catch(Exception e) {
                throw new EnkiRestoreFailedException(e);
            }
            
            rollback = false;
        } finally {
            repos.endTrans(rollback);
        }                
    }
    
    private static void delete(RefPackage pkg)
    {
        for(RefClass cls: 
            GenericCollections.asTypedCollection(
                pkg.refAllClasses(),
                RefClass.class))
        {
            Collection<RefObject> allOfClass = 
                new ArrayList<RefObject>(
                    GenericCollections.asTypedCollection(
                        cls.refAllOfClass(),
                        RefObject.class));
        
            Iterator<RefObject> iter = allOfClass.iterator();
            while(iter.hasNext()) {
                RefFeatured owner = iter.next().refImmediateComposite();
                if (owner != null && owner instanceof RefObject)
                {
                    // If we delete this object before we happen
                    // to have deleted its composite owner, the owner will
                    // throw an exception (because this object was already
                    // deleted). Can happen depending on iteration order of
                    // pkg.refAllClasses(). So remove this object from the
                    // collection and let the owner delete it.
                    iter.remove();
                }
            }
        
            for(RefObject obj: allOfClass) {
                obj.refDelete();
            }
        }
        
        for(RefPackage p: 
                GenericCollections.asTypedCollection(
                    pkg.refAllPackages(),
                    RefPackage.class))
        {
            delete(p);
        }
    }

    
    private static InputStream makeXmiFilterStream(
        InputStream stream,
        Class<? extends InputStream> filterStreamCls)
    throws Exception
    {
        Constructor<? extends InputStream> cons = 
            filterStreamCls.getConstructor(InputStream.class);
        
        InputStream filterStream = cons.newInstance(stream);
        
        return filterStream;
}

}

// End GenericBackupRestore.java