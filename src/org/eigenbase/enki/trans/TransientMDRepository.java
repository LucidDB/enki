/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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
package org.eigenbase.enki.trans;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.jmi.model.init.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;

/**
 * TransientMDRepository implements a memory-only, non-persistent repository.
 * All objects are lost upon repository shutdown.  This repository 
 * implementation only partially supports the MDR APIs.  In particular, it 
 * doesn't generate MDR Events and does not implement actual transactions (e.g.,
 * all changes are immediately visible to all callers and changes cannot be
 * rolled back).
 * 
 * <p>Storage properties.  Set the 
 * <code>org.eigenbase.enki.implementationType</code> storage property to 
 * {@link MdrProvider#ENKI_TRANSIENT} to enable the transient
 * repository implementation.  Additional storage properties of note are listed
 * in the following table.
 *
 * <table border="1">
 *   <caption><b>Transient-specific Storage Properties</b></caption>
 *   <tr>
 *     <th align="left">Name</th>
 *     <th align="left">Description</th>
 *   </tr>
 *   <tr>
 *     <td align="left">{@value #PROPERTY_WEAK}</td>
 *     <td align="left">
 *       Controls whether or not this is a weak reference repository.
 *       Defaults to {@value DEFAULT_WEAK}.
 *     </td>
 *   </tr>
 * </table>
 *
 * @author Stephan Zuercher
 */
public class TransientMDRepository implements EnkiMDRepository
{
    /**
     * Configuration file property that contains the name of the 
     * {@link MetamodelInitializer} class used to initialize the metamodel.
     */
    public static final String PROPERTY_MODEL_INITIALIZER = 
        "enki.model.initializer";

    /**
     * Configuration file property that indicates whether this model is a 
     * plug-in or a base model.  Valid values are true or false.
     */
    public static final String PROPERTY_MODEL_PLUGIN = 
        "enki.model.plugin";

    /**
     * Configuration file property that specifies a packaging version for
     * the model's schema.  This value may change from release to release.
     * Compatibility between versions will be documented elsewhere.  
     */
    public static final String PROPERTY_MODEL_PACKAGE_VERSION =
        "enki.model.packageVersion";
    
    /**
     * Current packaging version.  The current packaging version is {@value}.
     */
    public static final String PACKAGE_VERSION = "1.0";

    /**
     * Storage property that configures whether this is a weak reference
     * repository.  Values are converted to boolean via {@link
     * Boolean#valueOf(String)}.
     */
    public static final String PROPERTY_WEAK =
        "org.eigenbase.enki.trans.weak";

    /**
     * Contains the default value for the 
     * {@link #PROPERTY_WEAK} storage property.
     * The default is {@value}.
     */
    public static final boolean DEFAULT_WEAK = false;

    private static final String MOF_EXTENT = "MOF";
    private static final Logger log = 
        Logger.getLogger(TransientMDRepository.class.getName());

    private static final MdrSessionStack sessionStack = new MdrSessionStack();
    
    private final List<Properties> modelPropertiesList;
    private final Properties storageProps;
    private final ClassLoader classLoader;
    private final ReadWriteLock txnLock;
    private final AtomicInteger sessionCount;
    private final Map<String, ModelDescriptor> modelMap;
    private final Map<String, ExtentDescriptor> extentMap;

    private Map<Long, RefObject> byMofIdMap;
    
    private AtomicLong nextMofId = new AtomicLong(1);

    private boolean isWeak;
    
    public TransientMDRepository(
        List<Properties> modelPropertiesList,
        Properties storageProps, 
        ClassLoader classLoader)
    {
        this.modelPropertiesList = modelPropertiesList;
        this.storageProps = storageProps;
        this.classLoader = classLoader;
        this.txnLock = new ReentrantReadWriteLock();
        this.sessionCount = new AtomicInteger(0);
        this.modelMap = new HashMap<String, ModelDescriptor>();
        this.extentMap = new HashMap<String, ExtentDescriptor>();
        
        this.byMofIdMap = new ConcurrentHashMap<Long, RefObject>();

        String weakProp = storageProps.getProperty(PROPERTY_WEAK);
        if (weakProp == null) {
            isWeak = DEFAULT_WEAK;
        } else {
            isWeak = Boolean.valueOf(weakProp);
        }

        initModelMap();
        initModelExtent(MOF_EXTENT, false);

        // TODO: MBean support
    }
    
    // Overrides 
    public boolean isWeak()
    {
        if (extentMap.size() < 2) {
            return false;
        }
        return isWeak;
    }
    
    // Overrides 
    public MdrProvider getProviderType()
    {
        return MdrProvider.ENKI_TRANSIENT;
    }

    // Overrides 
    public Properties getStorageProperties()
    {
        return new Properties(storageProps);
    }

    // Overrides 
    public ClassLoader getDefaultClassLoader()
    {
        return classLoader;
    }

    // Overrides 
    public void shutdown()
    {
        // TODO: unregister mbean
        
        synchronized(extentMap) {
            extentMap.clear();
            modelMap.clear();
        }
        
        log.info("shutdown");
    }

    // Overrides 
    public void beginSession()
    {
        MdrSession mdrSession = sessionStack.peek(this);
        if (mdrSession != null) {
            log.log(Level.FINE, "begin re-entrant repository session");
            mdrSession.refCount++;
            return;
        }
        
        beginSessionImpl(false);
    }

    private MdrSession beginSessionImpl(boolean implicit)
    {
        if (implicit) {
            log.log(Level.FINE, "begin implicit repository session");            
        } else {
            log.log(Level.FINE, "begin repository session");
        }

        MdrSession mdrSession = new MdrSession(implicit);
        mdrSession.refCount++;
        
        sessionStack.push(mdrSession);
        int count = sessionCount.incrementAndGet();
        assert(count > 0);
        
        return mdrSession;
    }
    
    // Overrides 
    public EnkiMDSession detachSession()
    {
        if (sessionStack.isEmpty()) {
            return null;
        }
        
        MdrSession mdrSession = sessionStack.pop();
        return mdrSession;
    }

    // Overrides 
    public void reattachSession(EnkiMDSession session)
    {
        MdrSession existingSession = sessionStack.peek(this);
        if (existingSession != null) {
            throw new InternalMdrError(
                "must end current session before re-attach");
        }
        
        if (session == null) {
            // nothing to do
            return;
        }
        
        if (!(session instanceof MdrSession)) {
            throw new InternalMdrError(
                "invalid session object; wrong type");
        }
        
        sessionStack.push((MdrSession)session);
    }

    // Overrides 
    public void endSession()
    {
        MdrSession mdrSession = sessionStack.peek(this);
        if (mdrSession == null) {
            throw new InternalMdrError(
                "session never opened/already closed");
        }
        
        if (--mdrSession.refCount != 0) {
            log.log(Level.FINE, "end re-entrant repository session");
            return;
        }
        
        endSessionImpl(mdrSession);
    }
    
    private void endSessionImpl(MdrSession mdrSession)
    {
        if (mdrSession.refCount != 0) {
            throw new InternalMdrError(
                "bad ref count: " + mdrSession.refCount);
        }
        
        LinkedList<Context> contexts = mdrSession.context;
        if (!contexts.isEmpty()) {
            // More than 1 txn context implies at least one explicit txn.
            throw new InternalMdrError(
                "attempted to close session while txn remains open: " 
                + contexts.size());
        }
        
        if (mdrSession.implicit) {
            log.log(Level.WARNING, "end implicit repository session");
        } else {
            log.log(Level.FINE, "end repository session");
        }
        
        mdrSession.close();
        sessionStack.pop();
        int count = sessionCount.decrementAndGet();
        assert(count >= 0);
    }

    // Overrides 
    public void beginTrans(boolean write)
    {
        if (log.isLoggable(Level.FINEST)) {
            log.log(
                Level.FINEST, 
                "begin txn; "
                + (write ? "write; " : "read; "));
        }
        
        MdrSession mdrSession = sessionStack.peek(this);
        if (mdrSession == null) {
            mdrSession = beginSessionImpl(true);
        }
        
        log.fine("begin repository transaction");

        LinkedList<Context> contexts = mdrSession.context;
        
        // Nested txns are okay, but the outermost txn is the only one that
        // commits/rollsback.  So bar writes nested in reads.
        boolean isOutermost = contexts.isEmpty();
        
        if (write && !isOutermost) {
            if (!contexts.getFirst().isWrite) {
                throw new InternalMdrError(
                    "cannot start write transaction within read transaction");
            }
        }
        
        if (isOutermost) {
            if (write) {
                mdrSession.obtainWriteLock();
            } else {
                mdrSession.obtainReadLock();
            }
        }
        
        Context context = new Context(write);

        contexts.add(context);

        // TODO: begin trans event
    }

    // Overrides 
    public void endTrans()
    {
        endTrans(false);
    }

    // Overrides 
    public void endTrans(boolean rollback)
    {
        if (log.isLoggable(Level.FINEST)) {
            log.log(
                Level.FINEST, 
                "end txn; " + (rollback ? "rollback" : "commit"));
        }

        MdrSession mdrSession = sessionStack.peek(this);
        if (mdrSession == null) {
            throw new InternalMdrError(
                "No repository session associated with this thread");
        }
        
        LinkedList<Context> contexts = mdrSession.context;
        if (contexts.isEmpty()) {
            throw new InternalMdrError(
                "No repository transactions associated with this thread");
        }

        Context context = contexts.removeLast();

        if (rollback && !context.isWrite) {
            throw new InternalMdrError(
                "Cannot rollback read transactions");
        }

        log.fine("end repository transaction");

        if (contexts.isEmpty()) {
            try {
                // TODO: end trans event
                // TODO: fire change events
            } finally {                
                mdrSession.releaseLock();
            }
        
            if (mdrSession.implicit) {
                mdrSession.refCount--;
                endSessionImpl(mdrSession);
            }
        }
    }

    // Overrides 
    public void delete(Collection<RefObject> objects)
    {
        for(RefObject o: objects) {
            o.refDelete();
        }
    }

    // Overrides 
    public boolean supportsPreviewRefDelete()
    {
        return false;
    }

    // Overrides 
    public void previewRefDelete(RefObject obj)
    {
        throw new UnsupportedOperationException();
    }

    // Overrides 
    public RefPackage createExtent(String name)
        throws CreationFailedException
    {
        return createExtent(name, null, null);
    }

    // Overrides 
    public RefPackage createExtent(String name, RefObject metaPackage)
        throws CreationFailedException
    {
        return createExtent(name, metaPackage, null);
    }

    // Overrides 
    public RefPackage createExtent(
        String name,
        RefObject metaPackage,
        RefPackage[] existingInstances)
        throws CreationFailedException
    {
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(name);
            if (extentDesc != null) {
                throw new EnkiCreationFailedException(
                    "Extent '" + name + "' already exists");
            }

            // TODO: create event
        
            try {
                extentDesc = initExtent(name, metaPackage, existingInstances);

                return extentDesc.extent;
            }
            catch(ProviderInstantiationException e) {
                throw new EnkiCreationFailedException(
                        "could not create extent '" + name + "'", e);
            }
        }
    }

    // Overrides 
    public boolean isExtentBuiltIn(String name)
    {
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(name);
            if (extentDesc == null) {
                return false;
            }

            assert(extentDesc.modelDescriptor != null);

            return extentDesc.builtin;
        }
    }

    // Overrides 
    public RefPackage getExtent(String name)
    {
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(name);
            if (extentDesc == null) {
                return null;
            }
            
            assert(
                extentDesc.modelDescriptor != null || 
                extentDesc.name.equals(MOF_EXTENT));
            
            return extentDesc.extent;
        }
    }

    // Overrides 
    public String[] getExtentNames()
    {
        synchronized(extentMap) {
            return extentMap.keySet().toArray(new String[extentMap.size()]);
        }
    }

    // Overrides 
    public String getAnnotation(String extentName)
    {
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(extentName);
            if (extentDesc == null) {
                return null;
            }
            
            return extentDesc.annotation;
        }
    }

    // Overrides 
    public void setAnnotation(String extentName, String annotation)
    {
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(extentName);
            if (extentDesc == null) {
                return;
            }
            
            extentDesc.annotation = annotation;
        }
    }

    // Overrides 
    public void dropExtentStorage(RefPackage refPackage)
        throws EnkiDropFailedException
    {
        synchronized(extentMap) {
            for(ExtentDescriptor extentDesc: extentMap.values()) {
                if (extentDesc.extent.equals(refPackage)) {
                    removeExtent(extentDesc);
                    return;
                }
            }
        }
    }

    // Overrides 
    public void dropExtentStorage(String extentName)
        throws EnkiDropFailedException
    {
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(extentName);
            if (extentDesc == null) {
                return;
            }
            
            removeExtent(extentDesc);
        }
    }

    private void removeExtent(ExtentDescriptor extentDesc)
    {
        extentMap.remove(extentDesc.name);

        // REVIEW: Delete objects from somewhere?
    }
    
    // Overrides 
    public RefBaseObject getByMofId(String mofId)
    {
        // TODO: optimize this path (see what initializer does)
        Long mofIdLong = MofIdUtil.parseMofIdStr(mofId);
        
        return byMofIdMap.get(mofIdLong);
    }

    // Overrides 
    public RefObject getByMofId(String mofId, RefClass cls)
    {
        RefBaseObject base = getByMofId(mofId);
        
        if (base instanceof RefObject) {
            RefObject obj = (RefObject)base;
            
            if (obj.refClass().equals(cls)) {
                return obj;
            }
        }
        
        return null;
    }

    // Overrides 
    public void addListener(MDRChangeListener listener)
    {
        // TODO: implement
    }

    // Overrides 
    public void addListener(MDRChangeListener listener, int mask)
    {
        // TODO: implement
    }

    // Overrides 
    public void removeListener(MDRChangeListener listener)
    {
        // TODO: implement
    }

    // Overrides 
    public void removeListener(MDRChangeListener listener, int mask)
    {
        // TODO: implement
    }


    // Overrides 
    public void backupExtent(String extentName, OutputStream stream)
        throws EnkiBackupFailedException
    {
        GenericBackupRestore.backupExtent(this, extentName, stream);
    }

    // Overrides 
    public void restoreExtent(
        String extentName,
        String metaModelExtentName,
        String metaPackageName,
        InputStream stream)
        throws EnkiRestoreFailedException
    {
        GenericBackupRestore.restoreExtent(
            this,
            null,
            extentName,
            metaModelExtentName,
            metaPackageName,
            stream);
    }

    // Overrides 
    @Deprecated
    public void setRestoreExtentXmiFilter(Class<? extends InputStream> cls)
    {
        throw new UnsupportedOperationException();
    }
    
    private void initModelMap()
    {
        Class<? extends RefPackage> mofPkgCls =
            org.eigenbase.enki.jmi.model.ModelPackage.class;
        
        ModelDescriptor mofModelDesc =
            new ModelDescriptor(
                MOF_EXTENT, 
                mofPkgCls, 
                new Properties());
        modelMap.put(MOF_EXTENT, mofModelDesc);
        
        log.info("Initializing Model Descriptor: " + MOF_EXTENT);
        
        List<Properties> sortedModelPropertiesList = 
            new ArrayList<Properties>(modelPropertiesList);
        Collections.sort(
            sortedModelPropertiesList, new ModelPropertiesComparator());
        
        for(Properties modelProperties: sortedModelPropertiesList) {
            String name = 
                modelProperties.getProperty(
                    MDRepositoryFactory.PROPERTY_ENKI_EXTENT);
            if (name == null) {
                throw new ProviderInstantiationException(
                    "Extent name missing from model properties");
            }
            
            String packageVersion = 
                modelProperties.getProperty(PROPERTY_MODEL_PACKAGE_VERSION);
            if (!packageVersion.equals(PACKAGE_VERSION)) {
                log.severe(
                    "Ignoring model descriptor '" 
                    + name 
                    + "': unsupported package version '" 
                    + packageVersion 
                    + "'");
                continue;
            }
            
            if (isPlugin(modelProperties)) {
                String pluginName = 
                    modelProperties.getProperty(
                        TransientMDRepository.PROPERTY_MODEL_INITIALIZER);
                if (pluginName != null) {
                    int pos = pluginName.indexOf(".init");
                    if (pos > 0) {
                        pluginName = pluginName.substring(0, pos);
                    }
                } else {
                    pluginName = name;
                }
                ModelPluginDescriptor modelPluginDesc =
                    new ModelPluginDescriptor(pluginName, modelProperties);

                ModelDescriptor modelDesc = modelMap.get(name);
                if (modelDesc == null) {
                    log.severe(
                        "Ignoring plugin model '" 
                        + pluginName 
                        + "': missing model '" 
                        + name 
                        + "'");
                    continue;
                }
                
                modelDesc.plugins.add(modelPluginDesc);
                
                log.info(
                    "Initialized Model Plugin Descriptor: " + pluginName + 
                    " (" + name + ")");
            } else {
                String topLevelPkg = 
                    modelProperties.getProperty(
                        MDRepositoryFactory.PROPERTY_ENKI_TOP_LEVEL_PKG);
                if (topLevelPkg == null) {
                    throw new ProviderInstantiationException(
                        "Top-level package name missing from model properties");
                }
    
                Class<?> cls = null;
                try {
                    cls = 
                        Class.forName(
                            topLevelPkg,
                            true, 
                            Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new ProviderInstantiationException(
                        "Top-level package '" + topLevelPkg + "' not found",
                        e);
                }
    
                Class<? extends RefPackage> topLevelPkgCls = 
                    cls.asSubclass(RefPackage.class);
    
                ModelDescriptor modelDesc =
                    new ModelDescriptor(
                        name, 
                        topLevelPkgCls,
                        modelProperties);
                
                modelMap.put(name, modelDesc);
                
                log.fine("Initialized Model Descriptor: " + name);
            }
        }
    }
    
    private void initModelExtent(String name, boolean isNew)
    {
        boolean isMof = name.equals(MOF_EXTENT);

        ModelDescriptor modelDesc = modelMap.get(name);
        if (modelDesc == null) {
            throw new InternalMdrError(
                "Unknown metamodel extent '" + name + "'");
        }
        
        ModelDescriptor mofDesc = isMof ? null : modelMap.get(MOF_EXTENT);
        
        log.info("Initializing Extent Descriptor: " + name);
        
        ExtentDescriptor extentDesc = new ExtentDescriptor(name);
        
        extentDesc.modelDescriptor = mofDesc;
        extentDesc.builtin = true;
        
        MetamodelInitializer init;
        ModelPackage metaModelPackage = null;
        if (isMof) {
            init = new Initializer(MOF_EXTENT);
        } else {
            init = getInitializer(modelDesc);

            ExtentDescriptor mofExtentDesc = extentMap.get(MOF_EXTENT);
            
            metaModelPackage = mofExtentDesc.initializer.getModelPackage();
        }
        
        init.setOwningRepository(this);
        init.init(metaModelPackage);
        for(ModelPluginDescriptor pluginDesc: modelDesc.plugins) {
            MetamodelInitializer pluginInit = getInitializer(pluginDesc);
            
            pluginInit.setOwningRepository(this);
            pluginInit.initPlugin(metaModelPackage, init);
            
            extentDesc.pluginInitializers.add(pluginInit);
        }

        extentDesc.extent = init.getModelPackage();
        extentDesc.initializer = init;
        
        extentMap.put(name, extentDesc);
        
        log.fine("Initialized Extent Descriptor: " + name);
        
    }
    
    private MetamodelInitializer getInitializer(ModelDescriptor modelDesc)
    {
        String initializerName = 
            modelDesc.properties.getProperty(PROPERTY_MODEL_INITIALIZER);
        if (initializerName == null) {
            throw new ProviderInstantiationException(
                "Initializer name missing from '" + modelDesc.name + 
                "' model properties");
        }
       
        try {
            Class<? extends MetamodelInitializer> initCls =
                Class.forName(
                    initializerName,
                    true,
                    Thread.currentThread().getContextClassLoader())
                .asSubclass(MetamodelInitializer.class);
            
            Constructor<? extends MetamodelInitializer> cons =
                initCls.getConstructor(String.class);
            
            return cons.newInstance(modelDesc.name);
        } catch (Exception e) {
            throw new ProviderInstantiationException(
                "Initializer class '" + initializerName + 
                "' from '" + modelDesc.name +
                "' model JAR could not be instantiated", e);                    
        }
    }
    
    private ExtentDescriptor initExtent(
        String name, RefObject metaPackage, RefPackage[] existingInstances)
    throws EnkiCreationFailedException
    {
        if (metaPackage == null) {
            initModelExtent(name, true);
            return extentMap.get(name);
        }
        
        ModelDescriptor modelDesc = findModelDescriptor(metaPackage);

        ExtentDescriptor modelExtentDesc = extentMap.get(modelDesc.name);
        
        ExtentDescriptor extentDesc = new ExtentDescriptor(name);

        extentDesc.modelDescriptor = modelDesc;
        MetamodelInitializer.setCurrentInitializer(
            modelExtentDesc.initializer);
        try {
            extentDesc.extent = modelDesc.topLevelPkgCls.newInstance();

            for(MetamodelInitializer init: 
                    modelExtentDesc.pluginInitializers)
            {
                init.stitchPackages(extentDesc.extent);
            }
        } catch (Exception e) {
            throw new ProviderInstantiationException(
                "Cannot load extent '" + name + "'", e);
        } finally {
            MetamodelInitializer.setCurrentInitializer(null);
        }

        extentMap.put(name, extentDesc);

        return extentDesc;
    }
    
    private ModelDescriptor findModelDescriptor(RefObject metaPackage)
        throws EnkiCreationFailedException
    {
        for(Map.Entry<String, ExtentDescriptor> entry: extentMap.entrySet()) {
            ExtentDescriptor extentDesc = entry.getValue();
            
            RefPackage extent = extentDesc.extent;
            if (extent instanceof ModelPackage) {
                ModelPackage extentModelPkg = (ModelPackage)extent;
                
                for(MofPackage extentMofPkg: 
                        GenericCollections.asTypedCollection(
                            extentModelPkg.getMofPackage().refAllOfClass(),
                            MofPackage.class))
                {
                    if (extentMofPkg == metaPackage) {
                        return modelMap.get(extentDesc.name);
                    }
                }
            }
        }
        throw new EnkiCreationFailedException(
            "Unknown metapackage");
    }
    
    private static boolean isPlugin(Properties modelProperties)
    {
        return Boolean.parseBoolean(
            modelProperties.getProperty(
                TransientMDRepository.PROPERTY_MODEL_PLUGIN,
                "false"));
    }

    public void register(RefClass cls, RefObject instance)
    {
        long mofId = nextMofId.getAndIncrement();

        ((RefObjectBase)instance).setMofId(mofId);
        if (!isWeak) {
            byMofIdMap.put(mofId, instance);
        }
    }
    
    public void unregister(RefClass cls, RefObject instance)
    {
        long mofId = ((RefObjectBase)instance).getMofId();
        if (!isWeak) {
            byMofIdMap.remove(mofId);
        }
    }
    
    /**
     * ExtentDescriptor describes an instantiated model extent.
     */
    protected static class ExtentDescriptor
    {
        protected final String name;
        protected ModelDescriptor modelDescriptor;
        protected RefPackage extent;
        protected MetamodelInitializer initializer;
        protected List<MetamodelInitializer> pluginInitializers;
        protected String annotation;
        protected boolean builtin;
        
        public ExtentDescriptor(String name)
        {
            this.name = name;
            this.pluginInitializers = new ArrayList<MetamodelInitializer>();
        }
    }
    
    protected static class ModelDescriptor
    {
        protected final String name;
        protected final Class<? extends RefPackage> topLevelPkgCls;
        protected final Properties properties;
        public final List<ModelPluginDescriptor> plugins;

        public ModelDescriptor(
            String name,
            Class<? extends RefPackage> topLevelPkgCls,
            Properties properties)
        {
            this.name = name;
            this.topLevelPkgCls = topLevelPkgCls;
            this.properties = properties;
            this.plugins = new ArrayList<ModelPluginDescriptor>();
        }
        
        public boolean isPlugin()
        {
            return false;
        }
    }
    
    protected static class ModelPluginDescriptor extends ModelDescriptor
    {
        public ModelPluginDescriptor(
            String name,
            Properties properties)
        {
            super(name, null, properties);
        }

        @Override
        public boolean isPlugin()
        {
            return true;
        }
    }
    
    /**
     * ModelPropertiesComparator sorts model {@link Properties} objects by
     * their plug-in flag.  All non-plug-in model property sets come first, 
     * followed by those for plug-in models.  The relative ordering of the
     * two groups remains stable if the sorting algorithm is stable.
     */
    private class ModelPropertiesComparator
        implements Comparator<Properties>
    {
        public int compare(Properties modelProps1, Properties modelProps2)
        {
            boolean isPlugin1 = isPlugin(modelProps1);
            boolean isPlugin2 = isPlugin(modelProps2);
            
            if (isPlugin1 == isPlugin2) {
                return 0;
            }
            
            if (isPlugin1) {
                return 1;
            }
            
            return -1;
        }
    }

    /**
     * MdrSessionStack maintains a thread-local stack of {@link MdrSession}
     * instances.  The stack should only ever contain a single session from
     * any HibernateMDRepository.  It exists to support a single thread
     * accessing multiple repositories.
     */
    private static class MdrSessionStack
    {
        /** Thread-local storage for MDR session contexts. */
        private final ThreadLocal<LinkedList<MdrSession>> tls =
            new ThreadLocal<LinkedList<MdrSession>>() {
                @Override
                protected LinkedList<MdrSession> initialValue()
                {
                    return new LinkedList<MdrSession>();
                }
            };
        
        public MdrSession peek()
        {
            LinkedList<MdrSession> stack = tls.get();
            if (stack.isEmpty()) {
                return null;
            }

            return stack.getFirst();
        }
        
        public MdrSession peek(TransientMDRepository repos)
        {
            LinkedList<MdrSession> stack = tls.get();
            if (stack.isEmpty()) {
                return null;
            }

            MdrSession session = stack.getFirst();
            if (session.getRepos() != repos) {
                return null;
            }
            return session;
        }
        
        public MdrSession pop()
        {
            return tls.get().removeFirst();
        }
        
        public void push(MdrSession session)
        {
            tls.get().addFirst(session);
        }
        
        public boolean isEmpty()
        {
            return tls.get().isEmpty();
        }
    }

    private class MdrSession implements EnkiMDSession
    {
        private final boolean implicit;
        private final LinkedList<Context> context;
        private Lock lock;
        private int refCount;
        

        private MdrSession(boolean implicit)
        {
            this.implicit = implicit;
            this.context = new LinkedList<Context>();
            this.refCount = 0;
        }
        
        private void obtainWriteLock()
        {
            if (lock != null) {
                throw new InternalMdrError("already locked");
            }
            
            lock = txnLock.writeLock();
            lock.lock();
        }
        
        private void obtainReadLock()
        {
            if (lock != null) {
                throw new InternalMdrError("already locked");
            }
            
            Lock l = txnLock.readLock();
            l.lock();
            lock = l;
        }
        
        private void releaseLock()
        {
            if (lock == null) {
                log.warning(
                    "Request to release non-existent transaction lock");
                return;
            }
            
            Lock l = lock;
            lock = null;
            l.unlock();
        }
        
        private TransientMDRepository getRepos()
        {
            return TransientMDRepository.this;
        }
        
        public void close()
        {
            if (!context.isEmpty()) {
                throw new InternalMdrError("open txns on session");
            }
            context.clear();
            
            if (lock != null) {
                throw new InternalMdrError("open locks");
            }
        }
    }
    
    /**
     * Context represents an implicit or explicit MDR transaction.
     */
    private class Context
    {
        private boolean isWrite;
        
        private Context(boolean isWrite)
        {
            this.isWrite = isWrite;
        }
    }
}

// End TransientMDRepository.java
