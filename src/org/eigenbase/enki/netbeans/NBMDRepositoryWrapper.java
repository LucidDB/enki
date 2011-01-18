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
package org.eigenbase.enki.netbeans;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;
import javax.management.*;

import org.eigenbase.enki.mbean.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.mdr.ClassLoaderProvider;
import org.eigenbase.enki.netbeans.mbean.*;
import org.eigenbase.enki.util.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;
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
        Properties storageProps,
        ClassLoaderProvider classLoaderProvider)
    {
        this.impl = loadRepository(storageProps, classLoaderProvider);
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
    
    private static NBMDRepositoryImpl loadRepository(
        Properties storagePropsInit, ClassLoaderProvider classLoaderProvider)
    {
        if (classLoaderProvider != null) {
            BaseObjectHandler.setClassLoaderProvider(classLoaderProvider);
        }
        
        Properties sysProps = System.getProperties();
        
        Map<Object, Object> savedProps = new HashMap<Object, Object>();

        String storagePrefix = NETBEANS_MDR_STORAGE_PROP_PREFIX;

        // may be specified as a property
        String storageFactoryClassName = 
            storagePropsInit.getProperty(NETBEANS_MDR_CLASS_NAME_PROP);

        if (storageFactoryClassName == null) {
            // use default
            storageFactoryClassName = BtreeFactory.class.getName();
        }

        if (storageFactoryClassName.equals(BtreeFactory.class.getName())) {
            // special case
            storagePrefix = "";
        }

        // save existing system properties first
        savedProps.put(
            NETBEANS_MDR_CLASS_NAME_PROP,
            sysProps.get(NETBEANS_MDR_CLASS_NAME_PROP));
        for(Object key: storagePropsInit.keySet()) {
            String propName = applyPrefix(storagePrefix, key.toString());
            savedProps.put(propName, sysProps.get(propName));
        }

        try {
            // set desired properties
            sysProps.put(
                NETBEANS_MDR_CLASS_NAME_PROP, storageFactoryClassName);
            for (Map.Entry<Object, Object> entry : storagePropsInit.entrySet()) {
                sysProps.put(
                    applyPrefix(
                        storagePrefix,
                        entry.getKey().toString()),
                    entry.getValue());
            }

            // load repository
            return new NBMDRepositoryImpl();
        } finally {
            // restore saved system properties
            for (Map.Entry<Object, Object> entry : savedProps.entrySet()) {
                if (entry.getValue() == null) {
                    sysProps.remove(entry.getKey());
                } else {
                    sysProps.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private static String applyPrefix(String prefix, String propertyName)
    {
        if (propertyName.startsWith(prefix)) {
            return propertyName;
        }
        
        return prefix + propertyName;
    }
    

    public void finalize()
    {
        if (mbeanName != null) {
            EnkiMBeanUtil.unregisterRepositoryMBean(mbeanName);
        }
    }
    
    // implement EnkiMDRepository
    public boolean isWeak()
    {
        return false;
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
        GenericBackupRestore.backupExtent(this, extentName, stream);
    }
    
    // Implement EnkiMDRepository
    public void restoreExtent(
        String extentName,
        String metaModelExtentName,
        String metaPackageName,
        InputStream stream) 
    throws EnkiRestoreFailedException
    {
        GenericBackupRestore.restoreExtent(
            this, 
            filterStreamCls,
            extentName,
            metaModelExtentName,
            metaPackageName,
            stream);
    }

    @Deprecated
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
