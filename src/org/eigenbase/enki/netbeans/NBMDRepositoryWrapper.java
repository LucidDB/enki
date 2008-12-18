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
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;
import javax.jmi.xmi.*;
import javax.management.*;

import org.eigenbase.enki.mbean.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.netbeans.mbean.*;
import org.eigenbase.enki.util.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;
import org.netbeans.api.xmi.*;
import org.netbeans.mdr.*;
import org.netbeans.mdr.handlers.*;
import org.netbeans.mdr.persistence.*;
import org.netbeans.mdr.persistence.btreeimpl.btreestorage.*;

/**
 * NBMDRepositoryWrapper wraps the Netbeans MDR implementation of 
 * {@link MDRepository} to provide an implementation of 
 * {@link EnkiMDRepository}. 
 * 
 * <p>Storage properties.  Set the 
 * <code>org.eigenbase.enki.implementationType</code> storage property to 
 * {@link MdrProvider#NETBEANS_MDR NETBEANS_MDR} to enable the Netbeans MDR 
 * implementation. Additional storage properties of note are listed in the 
 * following table.
 * 
 * <table border="1">
 *   <caption><b>Netbeans-specific Storage Properties</b></caption>
 *   <tr>
 *     <th align="left">Name</th>
 *     <th align="left">Description</th>
 *   </tr>
 *   <tr>
 *     <td align="left">{@value #NETBEANS_MDR_CLASS_NAME_PROP}</td>
 *     <td align="left">
 *       Sets the storage factory used internally by Netbeans MDR.  Defaults
 *       to {@link BtreeFactory}.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td align="left">*</td>
 *     <td align="left">
 *       All other storage properties are passed through to Netbeans MDR.  If
 *       the storage factory is <b>not</b> set to {@link BtreeFactory},
 *       they are automatically prefixed with 
 *       {@value #NETBEANS_MDR_STORAGE_PROP_PREFIX} (unless the prefix is 
 *       already present on a property by property basis).
 *     </td>
 *   </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class NBMDRepositoryWrapper implements EnkiMDRepository
{
    public static final String NETBEANS_MDR_CLASS_NAME_PROP =
        MDRepositoryFactory.NETBEANS_MDR_CLASS_NAME_PROP;
    
    public static final String NETBEANS_MDR_STORAGE_PROP_PREFIX =
        MDRepositoryFactory.NETBEANS_MDR_STORAGE_PROP_PREFIX;
    
    private static int NEXT_ID = 1;
    
    private final Logger log = 
        Logger.getLogger(NBMDRepositoryWrapper.class.getName());

    private final ThreadLocalSessionContext tls = 
        new ThreadLocalSessionContext();

    public final int id;
    
    private final NBMDRepositoryImpl impl;
    private final Properties storageProps;
    
    private Class<? extends InputStream> filterStreamCls;
    
    private ObjectName mbeanName;
    
    public NBMDRepositoryWrapper(
        NBMDRepositoryImpl impl, 
        Properties storageProps)
    {
        this.impl = impl;
        this.storageProps = storageProps;
        this.id = NEXT_ID++;
        
        try {
            this.mbeanName = 
                EnkiMBeanUtil.registerRepositoryMBean(
                    NBMDRepositoryMBeanFactory.create(this));
        } catch(JMException e) {
            throw new ProviderInstantiationException(
                "Unable to register mbean", e);
        }
    }

    public void finalize()
    {
        if (mbeanName != null) {
            EnkiMBeanUtil.unregisterRepositoryMBean(mbeanName);
        }
    }
    
    // implement EnkiMDRepository
    public MdrProvider getProviderType()
    {
        return MdrProvider.NETBEANS_MDR;
    }
    
    // implement EnkiMDRepository
    public void dropExtentStorage(String extentName)
        throws EnkiDropFailedException
    {
        RefPackage pkg = impl.getExtent(extentName);
        
        if (pkg == null) {
            // Doesn't exist, do nothing.
            return;
        }
        
        dropExtentStorage(pkg);
    }
    
    public void dropExtentStorage(RefPackage refPackage)
        throws EnkiDropFailedException
    {
        // From Farrago: grotty internals for dropping physical repos storage
        String mofIdString = refPackage.refMofId();
        MOFID mofId = MOFID.fromString(mofIdString);

        Storage storage = impl.getMdrStorage().getStorageByMofId(mofId);
        try {
            storage.close();
            storage.delete();
        } catch(StorageException e) {
            throw new EnkiDropFailedException(
                "Error dropping Netbeans MDR storage", e);
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
    
    public void beginSession()
    {
        SessionContext context = tls.get();
        beginSessionImpl(context, false);
    }
    
    private void beginSessionImpl(SessionContext context, boolean implicit)
    {
        context.refCount++;
        context.implicit = implicit;

        if (context.implicit) {
            log.fine("begin implicit repository session");
        }
    }

    private void checkBeginImplicitSession()
    {
        SessionContext context = tls.get();
        if (context.refCount == 0) {
            beginSessionImpl(context, true);
        }
    }

    private void checkEndImplicitSession()
    {
        SessionContext context = tls.get();
        if (context.implicit == true && context.refCount == 1) {
            endSessionImpl(context);
        }
    }

    public void beginTrans(boolean write)
    {
        checkBeginImplicitSession();
        impl.beginTrans(write);
    }

    public RefPackage createExtent(
        String name,
        RefObject metaPackage,
        RefPackage[] existingInstances)
        throws CreationFailedException
    {
        return impl.createExtent(name, metaPackage, existingInstances);
    }

    public RefPackage createExtent(String name, RefObject metaPackage)
        throws CreationFailedException
    {
        return impl.createExtent(name, metaPackage);
    }

    public RefPackage createExtent(String name)
        throws CreationFailedException
    {
        return impl.createExtent(name);
    }
    
    public void endTrans()
    {
        impl.endTrans();
        checkEndImplicitSession();
    }

    public void endTrans(boolean rollback)
    {
        impl.endTrans(rollback);
        checkEndImplicitSession();
    }

    public void endSession()
    {
        SessionContext context = tls.get();
        endSessionImpl(context);
    }

    private void endSessionImpl(SessionContext context)
    {
        if (context.refCount == 0) {
            throw new RuntimeException("session never opened/already closed");
        } else {
            context.refCount--;
        }
    }

    public EnkiMDSession detachSession()
    {
        SessionContext session = tls.get();
        if (session.refCount == 0) {
            return null;
        }
        
        tls.set(new SessionContext());
        return session;
    }
    
    public void reattachSession(EnkiMDSession session)
    {
        SessionContext threadSession = tls.get();
        if (threadSession.refCount != 0) {
            throw new NBMDRepositoryWrapperException(
                "must end current session before re-attach");
        }
        
        if (session == null) {
            // nothing to do
            return;
        }
        
        if (!(session instanceof SessionContext)) {
            throw new NBMDRepositoryWrapperException(
                "invalid session object; wrong type");
        }
        
        tls.set((SessionContext)session);
        
    }

    public RefBaseObject getByMofId(String mofId)
    {
        return impl.getByMofId(mofId);
    }
    
    public RefObject getByMofId(String mofId, RefClass cls)
    {
        RefBaseObject baseObj = impl.getByMofId(mofId);
        if (baseObj == null) {
            return null;
        }
        
        if (!(baseObj instanceof RefObject)) {
            return null;
        }
        
        RefObject obj = (RefObject)baseObj;
        
        if (!obj.refClass().equals(cls)) {
            return null;
        }
        
        return obj;
    }
    
    public void delete(Collection<RefObject> objects)
    {
        for(RefObject object: objects)
        {
            object.refDelete();
        }
    }

    public void previewRefDelete(RefObject obj)
    {
        throw new UnsupportedOperationException();
    }
    
    public boolean supportsPreviewRefDelete()
    {
        return false;
    }
    
    public RefPackage getExtent(String name)
    {
        return impl.getExtent(name);
    }

    public String[] getExtentNames()
    {
        return impl.getExtentNames();
    }

    public void removeListener(MDRChangeListener listener, int mask)
    {
        impl.removeListener(listener, mask);
    }

    public void removeListener(MDRChangeListener listener)
    {
        impl.removeListener(listener);
    }

    public void shutdown()
    {
        EnkiMBeanUtil.unregisterRepositoryMBean(mbeanName);
        mbeanName = null;
        
        // Make it possible to start a new instance of NBMDRepositoryImpl
        org.netbeans.jmiimpl.mof.model.NamespaceImpl.clearContains();
        
        impl.shutdown();
    }
    
    // Implement EnkiMDRepository
    public boolean isExtentBuiltIn(String name)
    {
        return false;
    }
    
    // Implement EnkiMDRepository
    public ClassLoader getDefaultClassLoader()
    {
        return BaseObjectHandler.getDefaultClassLoader();
    }
    
    private static class ThreadLocalSessionContext 
        extends ThreadLocal<SessionContext>
    {
        @Override
        protected SessionContext initialValue()
        {
            return new SessionContext();
        }
    }
    
    // Implement EnkiMDRepository
    public String getAnnotation(String extentName)
    {
        MofPackage pkg = (MofPackage)getExtent(extentName).refMetaObject();
        if (pkg == null) {
            return null;
        }
        
        return pkg.getAnnotation();
    }
    
    // Implement EnkiMDRepository
    public void setAnnotation(String extentName, String annotation)
    {
        MofPackage pkg = (MofPackage)getExtent(extentName).refMetaObject();

        pkg.setAnnotation(annotation);
    }
    
    // Implement EnkiMDRepository
    public void backupExtent(String extentName, OutputStream stream) 
        throws EnkiBackupFailedException
    {
        try {
            RefPackage refPackage = getExtent(extentName);
            XmiWriter xmiWriter = XMIWriterFactory.getDefault().createXMIWriter();
            xmiWriter.write(stream, refPackage, "1.2");
        } catch(IOException e) {
            throw new EnkiBackupFailedException(e);
        }
    }
    
    // Implement EnkiMDRepository
    public void restoreExtent(
        String extentName,
        String metaPackageExtentName,
        String metaPackageName,
        InputStream stream) 
    throws EnkiRestoreFailedException
    {
        beginTrans(true);
        boolean rollback = true;
        try {
            RefPackage extent = getExtent(extentName);
            if (extent != null) {
                delete(extent);
                extent.refDelete();
                extent = null;
            }
            
            ModelPackage modelPackage =
                (ModelPackage)getExtent(metaPackageExtentName);
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
                extent = createExtent(extentName, metaPackage);
                
                XmiReader xmiReader = 
                    XMIReaderFactory.getDefault().createXMIReader();
        
                InputStream filter;
                if (filterStreamCls == null) {
                    filter = stream;
                } else {
                    filter = makeXmiFilterStream(stream);
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
            endTrans(rollback);
        }        
    }
    
    private void delete(RefPackage pkg)
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
    
    private InputStream makeXmiFilterStream(InputStream stream) 
        throws Exception
    {
        Constructor<? extends InputStream> cons = 
            filterStreamCls.getConstructor(InputStream.class);
        
        InputStream filterStream = cons.newInstance(stream);
        
        return filterStream;
    }
    
    public void setRestoreExtentXmiFilter(Class<? extends InputStream> cls)
    {
        this.filterStreamCls = cls;
    }
    
    public Properties getStorageProperties()
    {
        Properties copy = new Properties();
        copy.putAll(storageProps);
        return copy;
    }
    
    private static class SessionContext implements EnkiMDSession
    {
        public int refCount = 0;
        public boolean implicit = false;
    }
}

// End NBMDRepositoryWrapper.java
