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
package org.eigenbase.enki.hibernate;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.jmi.model.init.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.hibernate.event.*;
import org.hibernate.event.def.*;
import org.hibernate.tool.hbm2ddl.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;

/**
 * HibernateMDRepository implements {@link MDRepository} for Hibernate-based
 * metamodel storage.
 * 
 * @author Stephan Zuercher
 */
public class HibernateMDRepository
    implements MDRepository, EnkiMDRepository, 
               EnkiChangeEventThread.ListenerSource
{
    public static final String CONFIG_PROPERTIES = "config.properties";
    public static final String MAPPING_XML = "mapping.xml";
    public static final String HIBERNATE_STORAGE_MAPPING_XML = 
        "/org/eigenbase/enki/hibernate/storage/hibernate-storage-mapping.xml";
    
    public static final String PROPERTY_MODEL_INITIALIZER = 
        "enki.model.initializer";
    
    private static final String MOF_EXTENT = "--mof--";
    
    private static final ThreadLocal<Context> tls = new ThreadLocal<Context>();
    private static HibernateMDRepository repos;
    
    private final List<Properties> modelPropertiesList;
    private final Properties storageProperties;
    private final ClassLoader classLoader;
    private final Map<String, ModelDescriptor> modelMap;
    private final Map<String, ExtentDescriptor> extentMap;
    
    private SessionFactory sessionFactory;
    private MofIdGenerator mofIdGenerator;
    
    private EnkiChangeEventThread thread;
    private Map<MDRChangeListener, EnkiMaskedMDRChangeListener> listeners;
    
    private final Logger log = 
        Logger.getLogger(HibernateMDRepository.class.getName());

    public HibernateMDRepository(
        List<Properties> modelProperties,
        Properties storageProperties,
        ClassLoader classLoader)
    {
        if (repos != null) {
            // TODO: better exception type
            throw new IllegalStateException(
                "Cannot instantiate multiple repositories");
        }
        this.modelPropertiesList = modelProperties;
        this.storageProperties = storageProperties;
        this.classLoader = classLoader;
        this.modelMap = new HashMap<String, ModelDescriptor>();
        this.extentMap = new HashMap<String, ExtentDescriptor>();
        this.thread = null;
        this.listeners = 
            new IdentityHashMap<MDRChangeListener, EnkiMaskedMDRChangeListener>();
        
        initModelMap();
        initModelExtent(MOF_EXTENT);
        
        // REVIEW: SWZ: 2008-02-01: Strictly speaking, multiple instance of
        // this class should be acceptable (may even be required).  However,
        // we need a way to find the repository instance for implicit read
        // transactions.  Hibernate instantiates objects as necessary, so
        // it's hard to know when/where it'll happen.  Could burn the repos 
        // instance into the RefPackage/RefClass/etc instances at init time 
        // and then set it on the objects loaded by them (have to check every 
        // object every time, which is ugly).
        repos = this;
    }
    
    public void beginTrans(boolean write)
    {
        beginTrans(write, false);
    }
    
    private Context beginTrans(boolean write, boolean implicit)
    {
        assert(!write || !implicit): "Cannot support implicit write txn";
        
        Context ancestor = null;
        Context implicitContext = null;
        if (tls.get() != null) {
            Context context = tls.get();

            if (context.isImplicit) {
                if (!write) {
                    // Nested read.  May upgrade implicit to explicit read txn.
                    context.isImplicit = implicit;
                    return context;
                }

                // Replace implicit read with a write txn by falling through 
                // with null ancestor.
                implicitContext = context;
            } else {
                // Allow certain nested read transactions.
                if (context.isWrite && write) {
                    // TODO: better exception type
                    throw new IllegalStateException(
                        "Cannot nest write transactions");
                }
                
                ancestor = context;
            }
        }

        boolean explicitlyEnableFlush = false;
        boolean explicitlyDisableFlush = false;
        
        Session session;
        Transaction txn;
        if (ancestor != null) {
            session = ancestor.session;
            txn = ancestor.transaction;
            
            if (write && !isWriteTransaction(ancestor, true)) {
                explicitlyEnableFlush = true;
            }
        } else if (implicitContext != null) {
            // Don't open a new session/transaction.
            session = implicitContext.session;
            txn = implicitContext.transaction;
            
            // N.B.: We know this is a write txn because of the logic that
            // set implicitContext.
            explicitlyEnableFlush = true;
        } else {
            session = sessionFactory.getCurrentSession();
            txn = session.beginTransaction();
            
            explicitlyEnableFlush = write;
            explicitlyDisableFlush = !write;
        }

        if (explicitlyDisableFlush) {
            assert(!explicitlyEnableFlush);
            
            // Disable auto-flush to improve performance.  No need to flush 
            // since this MDR transaction is read-only.
            session.setFlushMode(FlushMode.COMMIT);
        } else if (explicitlyEnableFlush) {
            session.setFlushMode(FlushMode.AUTO);
        }
        
        Context context = new Context(session, txn, write, implicit, ancestor);
        tls.set(context);

        if (write) {
            context.getEldestAncestor().mayContainWrites = true;
        }
        
        if (write) {
            enqueueBeginTransEvent(context);
        }
        
        return context;
    }

    public void endTrans()
    {
        endTrans(false);
    }

    public void endTrans(boolean rollback)
    {
        Context context = tls.get();
        if (context == null) {
            // TODO: better exception type
            throw new IllegalStateException(
                "No current transaction on this thread");
        }
        
        if (context.ancestor != null) {
            tls.set(context.ancestor);
            return;
        }
        
        tls.set(null);
        
        Transaction txn = context.transaction;

        if (context.isWrite) {
            enqueueEndTransEvent(context);
        }
        
        // No need to close the session, the txn commit or rollback took care 
        // of that already.  Note that even if "commit" is requested, we'll
        // rollback if no writing was possible.
        if (rollback) {
            txn.rollback();
            
            fireCanceledChanges(context);
        } else {
            // TODO: check for constraint violations
            if (false) {
                ArrayList<String> constraintErrors = new ArrayList<String>();
                boolean foundConstraintError = false;
    
                if (foundConstraintError) {
                    txn.rollback();
                    
                    throw new HibernateConstraintViolationException(
                        constraintErrors);
                }
            }
            
            try {
                if (!context.mayContainWrites) {
                    txn.rollback();
                } else {
                    txn.commit();
                }
                
                fireChanges(context);
            } catch(HibernateException e) {
                fireCanceledChanges(context);
                throw e;
            }
        }
        
        
    }

    public RefPackage createExtent(String name)
        throws CreationFailedException
    {
        return createExtent(name, null, null);
    }

    public RefPackage createExtent(String name, RefObject metaPackage)
        throws CreationFailedException
    {
        return createExtent(name, metaPackage, null);
    }

    public RefPackage createExtent(
        String name,
        RefObject metaPackage,
        RefPackage[] existingInstances)
    throws CreationFailedException
    {
        initStorage();

        ExtentDescriptor extentDesc = extentMap.get(name);
        if (extentDesc != null) {
            throw new EnkiCreationFailedException(
                "Extent '" + name + "' already exists");
        }
        
        enqueueEvent(
            getContext(), 
            new ExtentEvent(
                this, 
                ExtentEvent.EVENT_EXTENT_CREATE, 
                name, 
                metaPackage,
                Collections.unmodifiableCollection(extentMap.keySet()), 
                true));
        
        try {
            extentDesc = 
                createExtentStorage(name, metaPackage, existingInstances);
            
            return extentDesc.extent;
        }
        catch(ProviderInstantiationException e) {
            throw new EnkiCreationFailedException(
                    "could not create extent '" + name + "'", e);
        }
    }

    public void dropExtentStorage(String extent) throws EnkiDropFailedException
    {
        // TODO: locking
        initStorage();
        
        ExtentDescriptor extentDesc = extentMap.get(extent);
        if (extentDesc == null) {
            return;
        }
        
        dropExtentStorage(extentDesc);
    }

    public void dropExtentStorage(RefPackage refPackage) 
        throws EnkiDropFailedException
    {
        // TODO: locking
        initStorage();
        
        for(ExtentDescriptor extentDesc: extentMap.values()) {
            if (extentDesc.extent.equals(refPackage)) {
                dropExtentStorage(extentDesc);
                return;
            }
        }
    }
    
    private void dropExtentStorage(ExtentDescriptor extentDesc)
        throws EnkiDropFailedException
    {
        enqueueEvent(
            getContext(),
            new ExtentEvent(
                this,
                ExtentEvent.EVENT_EXTENT_DELETE,
                extentDesc.name,
                null,
                Collections.unmodifiableCollection(extentMap.keySet())));
        
        extentMap.remove(extentDesc.name);
        
        Session session = sessionFactory.getCurrentSession();
        Transaction trans = session.beginTransaction();
        boolean rollback = true;
        try {
            Query query = session.getNamedQuery("ExtentByName");
            query.setString(0, extentDesc.name);
            
            Extent dbExtent = (Extent)query.uniqueResult();
            session.delete(dbExtent);
            
            trans.commit();
            rollback = false;
        } catch(HibernateException e) {
            throw new EnkiDropFailedException(
                "Could not delete extent table entry", e);
        } finally {
            if (rollback) {
                trans.rollback();
            }
        }
        
        dropModelStorage(extentDesc.modelDescriptor);
    }
    
    public RefBaseObject getByMofId(String mofId)
    {
        long mofIdLong = MofIdUtil.parseMofIdStr(mofId); 

        if ((mofIdLong & MetamodelInitializer.METAMODEL_MOF_ID_MASK) != 0) {
            for(ExtentDescriptor extentDesc: extentMap.values()) {
                // Only search in metamodels
                if (extentDesc.modelDescriptor == null ||
                    extentDesc.modelDescriptor.name.equals(MOF_EXTENT))
                {
                    RefBaseObject result = 
                        extentDesc.initializer.getByMofId(mofId);
                    if (result != null) {
                        return result;
                    }
                }
            }
            
            return null;
        } else {
            Session session = getCurrentSession();
            
            // TODO: convert to named query
            Query query = 
                session.createQuery(
                    "from " + RefBaseObject.class.getName() 
                    + " where mofId = ?");
            query.setCacheable(true);
            query.setLong(0, mofIdLong);
            
            return (RefBaseObject)query.uniqueResult();
        }
    }

    public RefPackage getExtent(String name)
    {
        initStorage();
        
        ExtentDescriptor extentDesc = extentMap.get(name);
        if (extentDesc == null) {
            return null;
        }
        
        assert(extentDesc.modelDescriptor != null);
        
        return extentDesc.extent;
    }

    public String[] getExtentNames()
    {
        return extentMap.keySet().toArray(new String[extentMap.size()]);
    }

    public void shutdown()
    {
        // TODO: locking
        
        if (sessionFactory != null) {
            try {
                thread.shutdown();
            }
            catch(InterruptedException e) {
                log.log(
                    Level.SEVERE, 
                    "EnkiChangeEventThread interrupted on shutdown",
                    e);
            }
            
            if (sessionFactory.getStatistics().isStatisticsEnabled()) {
                sessionFactory.getStatistics().logSummary();
            }
            
            sessionFactory.close();
        }

        // Clear the repository used for implicit read transactions
        repos = null;
    }

    public void addListener(MDRChangeListener listener)
    {
        addListener(listener, MDRChangeEvent.EVENTMASK_ALL);
    }

    public void addListener(MDRChangeListener listener, int mask)
    {
        synchronized(listeners) {
            EnkiMaskedMDRChangeListener maskedListener = 
                listeners.get(listener);
            if (maskedListener != null) {
                maskedListener.add(mask);
                return;
            }
            
            maskedListener = new EnkiMaskedMDRChangeListener(listener, mask);
            listeners.put(listener, maskedListener);
        }
    }

    public void removeListener(MDRChangeListener listener)
    {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    public void removeListener(MDRChangeListener listener, int mask)
    {
        synchronized(listeners) {
            EnkiMaskedMDRChangeListener maskedListener = 
                listeners.get(listener);
            if (maskedListener != null) {
                boolean removedAll = maskedListener.remove(mask);
                
                if (removedAll) {
                    listeners.remove(listener);
                }
            }
        }
    }
    
    // Implement EnkiChangeEventThread.ListenerSource
    public void getListeners(
        Collection<EnkiMaskedMDRChangeListener> listenersCopy)
    {
        listenersCopy.clear();
        
        synchronized(listeners) {
            listenersCopy.addAll(listeners.values());
        }
    }
    
    // Implement EnkiMDRepository
    public boolean isExtentBuiltIn(String name)
    {
        initStorage();
        
        ExtentDescriptor extentDesc = extentMap.get(name);
        if (extentDesc == null) {
            return false;
        }
        
        assert(extentDesc.modelDescriptor != null);
        
        return extentDesc.builtIn;        
    }
    
    // Implement EnkiMDRepository
    public ClassLoader getDefaultClassLoader()
    {
        return classLoader;
    }
    
    public SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }
    
    public static HibernateMDRepository getRepository()
    {
        return repos;
    }

    public static Session getCurrentSession()
    {
        return getContext().session;
    }
    
    
    public static boolean isWriteTransaction()
    {
        return isWriteTransaction(getContext(), false);
    }
    
    private static boolean isWriteTransaction(
        Context context, boolean checkNested)
    {
        if (!checkNested) {
            return context.isWrite;
        }
        
        do {
            if (context.isWrite) {
                return true;
            }
            
            context = context.ancestor;
        } while(context != null);
        
        return false;
    }

    public static MofIdGenerator getMofIdGenerator()
    {
        return getContext().getMofIdGenerator();
    }

    public static Collection<?> lookupAllOfTypeResult(HibernateRefClass cls)
    {
        return getContext().allOfTypeCache.get(cls);
    }
    
    public static void storeAllOfTypeResult(
        HibernateRefClass cls, Collection<?> allOfType)
    {
        getContext().allOfTypeCache.put(cls, allOfType);
    }
    
    public static Collection<?> lookupAllOfClassResult(HibernateRefClass cls)
    {
        return getContext().allOfClassCache.get(cls);
    }
    
    public static void storeAllOfClassResult(
        HibernateRefClass cls, Collection<?> allOfClass)
    {
        getContext().allOfClassCache.put(cls, allOfClass);
    }

    public static void enqueueEvent(MDRChangeEvent event)
    {
        Context context = getContext();
        if (!isWriteTransaction(context, true)) {
            throw new IllegalStateException("Not in write transaction");
        }

        while(!context.isWrite) {
            context = context.ancestor;
        }
        
        repos.enqueueEvent(context, event);
    }
    
    /**
     * Helper method for generating the extent deletion even from a RefPackage.
     * 
     * @param pkg top-level RefPackage of the extent being deleted
     */
    public static void enqueueExtentDeleteEvent(RefPackage pkg)
    {
        Map<String, ExtentDescriptor> extentMap = repos.extentMap;
        String extentName = null;
        for(Map.Entry<String, ExtentDescriptor> entry: extentMap.entrySet()) {
            if (entry.getValue().extent.equals(pkg)) {
                extentName = entry.getKey();
                break;
            }
        }
        
        if (extentName == null) {
            throw new InternalMdrError(
                "Extent delete event only valid on top-level package");
        }
        
        enqueueEvent(
            new ExtentEvent(
                pkg,
                ExtentEvent.EVENT_EXTENT_DELETE,
                extentName,
                pkg.refMetaObject(),
                Collections.unmodifiableCollection(extentMap.keySet())));
    }

    private void enqueueEvent(Context context, MDRChangeEvent event)
    {
        // Cache event in Context (we'll need to fire it upon commit even if
        // there are no listeners now.)
        context.queuedEvents.add(event);
        
        // Fire as planned change immediately (and from this thread).
        synchronized(listeners) {
            for(EnkiMaskedMDRChangeListener listener: listeners.values()) {
                // Note: EnkiMaskedMDRChangeListener automatically squelches
                // RuntimeExceptions as required by the API.
                listener.plannedChange(event);
            }
        }
    }

    private void enqueueBeginTransEvent(Context context)
    {
        enqueueEvent(
            context, 
            new TransactionEvent(
                this, TransactionEvent.EVENT_TRANSACTION_START));
    }

    private void enqueueEndTransEvent(Context context)
    {
        enqueueEvent(
            context, 
            new TransactionEvent(
                this, TransactionEvent.EVENT_TRANSACTION_END));
    }

    private static Context getContext()
    {
        Context context = tls.get();
        if (context != null) {
            return context;
        }

        // begin an implicit read transaction for this thread
        return repos.beginTrans(false, true);
    }
    
    private void fireCanceledChanges(Context context)
    {
        synchronized(listeners) {
            for(MDRChangeEvent event: context.queuedEvents) {
                for(EnkiMaskedMDRChangeListener listener: listeners.values()) {
                    listener.changeCancelled(event);
                }
            }
            context.queuedEvents.clear();
        }
    }
    
    private void fireChanges(Context context)
    {
        synchronized(listeners) {
            for(MDRChangeEvent event: context.queuedEvents) {
                thread.enqueueEvent(event);
            }
            context.queuedEvents.clear();
        }
    }
    
    private void loadExistingExtents(List<Extent> extents)
    {
        for(Extent extent: extents) {
            String extentName = extent.getExtentName();
            String modelExtentName = extent.getModelExtentName();
            
            ModelDescriptor modelDesc = modelMap.get(modelExtentName);

            if (modelDesc == null) {
                throw new ProviderInstantiationException(
                    "Missing model extent '" + modelExtentName + 
                    "' for extent '" + extentName + "'");
            }
            
            if (!extentMap.containsKey(modelExtentName)) {
                initModelExtent(modelExtentName);
            }
            
            ExtentDescriptor modelExtentDesc = 
                extentMap.get(modelExtentName);
            
            if (modelExtentDesc.initializer == null) {
                throw new ProviderInstantiationException(
                    "Missing initializer for metamodel extent '" + 
                    modelExtentName + "'");
            }
            
            ExtentDescriptor extentDesc = new ExtentDescriptor(extentName);
            extentDesc.modelDescriptor = modelDesc;

            MetamodelInitializer.setCurrentInitializer(
                modelExtentDesc.initializer);
            try {
                extentDesc.extent =
                    modelDesc.topLevelPkgCons.newInstance(
                        (Object)null);
            } catch (Exception e) {
                throw new ProviderInstantiationException(
                    "Cannot load extent '" + extentName + "'", e);
            } finally {
                MetamodelInitializer.setCurrentInitializer(null);
            }
            
            extentMap.put(extentName, extentDesc);
        }
    }
    
    private ExtentDescriptor createExtentStorage(
        String name,
        RefObject metaPackage, 
        RefPackage[] existingInstances)
    throws EnkiCreationFailedException
    {
        if (metaPackage == null) {
            initModelExtent(name);
            return extentMap.get(name);
        }
        
        ModelDescriptor modelDesc = null;

        EXTENT_SEARCH:
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
                        modelDesc = modelMap.get(extentDesc.name);
                        break EXTENT_SEARCH;
                    }
                }
            }
        }        
        
        if (modelDesc == null) {
            throw new EnkiCreationFailedException(
                "Unknown metapackage");
        }
        
        initModelStorage(modelDesc);
        
        ExtentDescriptor modelExtentDesc = extentMap.get(modelDesc.name);
        
        ExtentDescriptor extentDesc = new ExtentDescriptor(name);

        extentDesc.modelDescriptor = modelDesc;
        MetamodelInitializer.setCurrentInitializer(
            modelExtentDesc.initializer);
        try {
            if (modelDesc.topLevelPkgCons != null) {
                extentDesc.extent =
                    modelDesc.topLevelPkgCons.newInstance((Object)null);
            } else {
                extentDesc.extent = modelDesc.topLevelPkgCls.newInstance();
            }
        } catch (Exception e) {
            throw new ProviderInstantiationException(
                "Cannot load extent '" + name + "'", e);
        } finally {
            MetamodelInitializer.setCurrentInitializer(null);
        }

        Session session = sessionFactory.getCurrentSession();
        Transaction trans = session.beginTransaction();
        
        boolean rollback = true;
        try {
            Extent extentDbObj = new Extent();
            extentDbObj.setExtentName(extentDesc.name);
            extentDbObj.setModelExtentName(modelDesc.name);
            
            session.save(extentDbObj);
            rollback = false;
        } finally {
            if (rollback) {
                trans.rollback();
            } else {
                trans.commit();
            }
        }
        
        extentMap.put(name, extentDesc);

        return extentDesc;
    }

    private void initStorage()
    {
        // TODO: locking (don't want to double-init the session factory)
        if (sessionFactory == null) {
            Configuration config = newConfiguration();

            initProviderStorage(config);
            
            for(ModelDescriptor modelDesc: modelMap.values()) {
                if (MOF_EXTENT.equals(modelDesc.name)) {
                    continue;
                }

                // Load mappings.
                URL mappingUrl = getModelMappingUrl(modelDesc);
                
                config.addURL(mappingUrl);
            }
            
            sessionFactory = config.buildSessionFactory();
            
            mofIdGenerator = 
                new MofIdGenerator(sessionFactory, config, storageProperties);
            mofIdGenerator.configureTable();
            
            List<Extent> extents = null;
            Session session = sessionFactory.getCurrentSession();
                
            Transaction trans = session.beginTransaction();
            try {
                Query query = session.getNamedQuery("AllExtents");
                extents = 
                    GenericCollections.asTypedList(query.list(), Extent.class);
            } finally {
                trans.commit();
            }
            
            loadExistingExtents(extents);
            
            thread = new EnkiChangeEventThread(this);
            thread.start();
        }
    }

    private Configuration newConfiguration()
    {
        return newConfiguration(true);
    }
    
    private Configuration newConfiguration(boolean includeProviderMapping)
    {
        Configuration config = new Configuration();

        // Load basic configuration.
        config.configure(
            "org/eigenbase/enki/hibernate/hibernate-base-config.xml");

        // Override it with storage properties
        final String keyPrefix = "hibernate.";
        
        for(Map.Entry<Object, Object> entry: storageProperties.entrySet())
        {
            String key = entry.getKey().toString();
            String value = 
                entry.getValue() == null 
                    ? null 
                    : entry.getValue().toString();
            
            if (key.startsWith(keyPrefix)) {
                config.setProperty(key, value);
            }
        }
    
        if (includeProviderMapping) {
            URL internalConfigIUrl = 
                getClass().getResource(HIBERNATE_STORAGE_MAPPING_XML);
            config.addURL(internalConfigIUrl);
            
            EventListener listener = new EventListener();
            
            FlushEventListener[] flushListeners = { 
                listener,
                new DefaultFlushEventListener()
            };
            AutoFlushEventListener[] autoFlushListeners = { 
                listener,
                new DefaultAutoFlushEventListener()
            };
            
            config.getEventListeners().setFlushEventListeners(flushListeners);
            config.getEventListeners().setAutoFlushEventListeners(
                autoFlushListeners);
        }
        
        return config;
    }

    private void initProviderStorage(Configuration config)
    {
        SessionFactory tempSessionFactory = config.buildSessionFactory();

        Session session = tempSessionFactory.getCurrentSession();
        
        boolean exists = false;
        Transaction trans = session.beginTransaction();
        try {
            // Execute the query
            session.getNamedQuery("AllExtents").list();
            exists = true;
        } catch(HibernateException e) {
            // Presume that table doesn't exist (if it's a connection error,
            // we can't cause any damage).
            log.log(Level.FINE, "Extent Query Error", e);
        } finally {
            trans.commit();
            
            tempSessionFactory.close();
        }
        
        if (exists) {
            log.info("Validating Enki Hibernate provider schema");
            
            SchemaValidator validator = new SchemaValidator(config);
            
            try {
                validator.validate();
                return;
            } catch(HibernateException e) {
                log.log(
                    Level.WARNING, 
                    "Enki Hibernate provider schema validation failed", 
                    e);
            }
            
            log.info("Updating Enki Hibernate provider schema");
            
            SchemaUpdate update = new SchemaUpdate(config);
            
            try {
                update.execute(false, true);
            } catch(HibernateException e) {
                throw new ProviderInstantiationException(
                    "Unable to update Enki Hibernate provider schema", e);
            }
        } else {
            log.info("Creating Enki Hibernate Provider schema");
        
            SchemaExport export = new SchemaExport(config);
            try {
                export.create(false, true);
            } catch(HibernateException e) {
                throw new ProviderInstantiationException(
                    "Unable to create Enki Hibernate provider schema", e);
            }
        }        
    }
    
    private void initModelStorage(ModelDescriptor modelDesc)
    throws EnkiCreationFailedException
    {
        Configuration config = newConfiguration();
        
        URL mappingUrl = getModelMappingUrl(modelDesc);
        
        config.addURL(mappingUrl);

        SchemaValidator validator = new SchemaValidator(config);
        try {
            validator.validate();
            
            return;
        } catch(HibernateException e) {
            log.log(
                Level.FINE,
                "Schema validation error for model '" + modelDesc.name + "'",
                e);
        }
        
        log.info("Updating schema for model '" + modelDesc.name + "'");
        
        SchemaUpdate update = new SchemaUpdate(config);
        update.execute(false, true);
        
        List<?> exceptions = update.getExceptions();
        if (exceptions != null && !exceptions.isEmpty()) {
            throw new EnkiCreationFailedException(
                "Schema update for model '" + modelDesc.name + 
                "' failed (cause is first exception)",
                (Throwable)exceptions.get(0));

        }
    }
    
    private void dropModelStorage(ModelDescriptor modelDesc)
        throws EnkiDropFailedException
    {
        Configuration config = newConfiguration(false);
        
        URL mappingUrl = getModelMappingUrl(modelDesc);
        
        config.addURL(mappingUrl);

        log.info("Dropping schema for model '" + modelDesc.name + "'");
        
        SchemaExport export = new SchemaExport(config);
        export.drop(false, true);        
        List<?> exceptions = export.getExceptions();
        if (exceptions != null && !exceptions.isEmpty()) {
            throw new EnkiDropFailedException(
                "Schema drop for model '" + modelDesc.name + 
                "' failed (cause is first exception)",
                (Throwable)exceptions.get(0));
        }
        
        SessionFactory tempSessionFactory = config.buildSessionFactory();

        try {
            MofIdGenerator mofIdGenerator = 
                new MofIdGenerator(
                    tempSessionFactory, config, storageProperties);
            mofIdGenerator.dropTable();
        }
        finally {
            tempSessionFactory.close();
        }
    }
    
    private URL getModelMappingUrl(ModelDescriptor modelDesc)
    {
        String configUrlStr = 
            modelDesc.properties.getProperty(
                MDRepositoryFactory.PROPERTY_ENKI_RUNTIME_CONFIG_URL);
        
        log.info(
            "Model: " + modelDesc.name + 
            ", Config URL: " + configUrlStr);
        
        URL mappingUrl;
        try {
            URL configUrl = new URL(configUrlStr);
            mappingUrl = new URL(configUrl, MAPPING_XML);
        } catch (MalformedURLException e) {
            throw new ProviderInstantiationException(
                "Cannot compute mapping.xml location", e);
        }
        return mappingUrl;
    }
    
    private void initModelMap()
    {
        Class<? extends RefPackage> mofPkgCls =
            org.eigenbase.enki.jmi.model.ModelPackage.class;
        
        ModelDescriptor mofModelDesc =
            new ModelDescriptor(
                MOF_EXTENT, mofPkgCls, null, new Properties());
        modelMap.put(MOF_EXTENT, mofModelDesc);
        
        log.info("Initializing Model Descriptor: " + MOF_EXTENT);
        
        for(Properties modelProperties: modelPropertiesList) {
            String topLevelPkg = 
                modelProperties.getProperty(
                    MDRepositoryFactory.PROPERTY_ENKI_TOP_LEVEL_PKG);
            if (topLevelPkg == null) {
                throw new ProviderInstantiationException(
                    "Top-level package name missing from model properties");
            }

            Class<?> cls;
            try {
                cls = 
                    Class.forName(
                        topLevelPkg,
                        true, 
                        Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new ProviderInstantiationException(
                    "Top-level package '" + topLevelPkg + "' not found", e);
            }

            Class<? extends RefPackage> topLevelPkgCls =
                cls.asSubclass(RefPackage.class);
                
            Constructor<? extends RefPackage> topLevelPkgCons;
            try {
                topLevelPkgCons = 
                    topLevelPkgCls.getConstructor(RefPackage.class);
            } catch (NoSuchMethodException e) {
                throw new ProviderInstantiationException(
                    "Cannot find constructor for top-level package class '" + 
                    topLevelPkgCls.getName() + "'", e);
            }
            
            String name = 
                modelProperties.getProperty(
                    MDRepositoryFactory.PROPERTY_ENKI_EXTENT);
            if (name == null) {
                throw new ProviderInstantiationException(
                    "Extent name missing from model properties");
            }

            ModelDescriptor modelDesc =
                new ModelDescriptor(
                    name, topLevelPkgCls, topLevelPkgCons, modelProperties);
            
            modelMap.put(name, modelDesc);
            
            log.info("Initialized Model Descriptor: " + name);
        }
    }
    
    private void initModelExtent(String name)
    {
        ModelDescriptor modelDesc = modelMap.get(name);
        ModelDescriptor mofDesc = 
            name.equals(MOF_EXTENT) ? null : modelMap.get(MOF_EXTENT);
        
        log.info("Initialize Extent Descriptor: " + name);
        
        ExtentDescriptor extentDesc = new ExtentDescriptor(name);
        
        extentDesc.modelDescriptor = mofDesc;
        
        MetamodelInitializer init;
        if (name.equals(MOF_EXTENT)) {
            init = new Initializer(MOF_EXTENT);
        } else {
            String initializerName = 
                modelDesc.properties.getProperty(PROPERTY_MODEL_INITIALIZER);
            if (initializerName == null) {
                throw new ProviderInstantiationException(
                    "Initializer name missing from '" + name + 
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
                
                init = cons.newInstance(name);
            } catch (Exception e) {
                throw new ProviderInstantiationException(
                    "Initializer class '" + initializerName + 
                    "' from '" + name +
                    "' model JAR could not be instantiated", e);                    
            }
        }
        
        ModelPackage metaModelPackage = null;
        if (mofDesc != null) {
            ExtentDescriptor mofExtentDesc = extentMap.get(MOF_EXTENT);
            
            metaModelPackage = mofExtentDesc.initializer.getModelPackage();
        }
        init.init(metaModelPackage);
        
        extentDesc.extent = init.getModelPackage();
        extentDesc.initializer = init;
        extentDesc.builtIn = true;
        
        extentMap.put(name, extentDesc);
    }
        
    public void onFlush(FlushEvent flushEvent) throws HibernateException
    {
        Context context = tls.get();
        if (context == null) {
            // Not in a transaction (e.g. startup, shutdown)
            return;
        }
        
        context.allOfTypeCache.clear();
        context.allOfClassCache.clear();
    }

    public void onAutoFlush(AutoFlushEvent autoFlushEvent) 
        throws HibernateException
    {
        Context context = tls.get();
        if (context == null) {
            // Not in a transaction (e.g. startup, shutdown)
            return;
        }

        if (!isWriteTransaction(context, true)) {
            // Ignore auto-flush on read-only transactions.
            return;
        }
        
        context.allOfTypeCache.clear();
        context.allOfClassCache.clear();
    }

    private static class ModelDescriptor
    {
        private final String name;
        private final Class<? extends RefPackage> topLevelPkgCls;
        private final Constructor<? extends RefPackage> topLevelPkgCons;
        private final Properties properties;
        
        private ModelDescriptor(
            String name,
            Class<? extends RefPackage> topLevelPkgCls,
            Constructor<? extends RefPackage> topLevelPkgCons,
            Properties properties)
        {
            this.name = name;
            this.topLevelPkgCls = topLevelPkgCls;
            this.topLevelPkgCons = topLevelPkgCons;
            this.properties = properties;
        }
    }    
    
    private static class ExtentDescriptor
    {
        private final String name;
        private ModelDescriptor modelDescriptor;
        private RefPackage extent;
        private MetamodelInitializer initializer;
        private boolean builtIn;
        
        private ExtentDescriptor(String name)
        {
            this.name = name;
        }
    }
    
    private class Context
    {
        private Session session;
        private Transaction transaction;
        private boolean isWrite;
        private boolean isImplicit;
        private final Context ancestor;
        private Map<HibernateRefClass, Collection<?>> allOfTypeCache;
        private Map<HibernateRefClass, Collection<?>> allOfClassCache;
        private boolean mayContainWrites;
        private List<MDRChangeEvent> queuedEvents;
        
        private Context(
            Session sesson,
            Transaction transaction, 
            boolean isWrite, 
            boolean isImplicit,
            Context ancestor)
        {
            this.session = sesson;
            this.transaction = transaction;
            this.isWrite = isWrite;
            this.isImplicit = isImplicit;
            this.ancestor = ancestor;
            this.allOfTypeCache = 
                new HashMap<HibernateRefClass, Collection<?>>();
            this.allOfClassCache =
                new HashMap<HibernateRefClass, Collection<?>>();
            this.mayContainWrites = false;
            this.queuedEvents = new LinkedList<MDRChangeEvent>();
        }
        
        private MofIdGenerator getMofIdGenerator()
        {
            return HibernateMDRepository.this.mofIdGenerator;
        }
        
        private Context getEldestAncestor()
        {
            Context context = this;
            while(context.ancestor != null) {
                context = context.ancestor;
            }
            
            return context;
        }
    }
    
    private class EventListener 
        implements FlushEventListener, AutoFlushEventListener
    {
        private static final long serialVersionUID = 5884573353539473470L;

        public void onFlush(FlushEvent flushEvent)
            throws HibernateException
        {
            HibernateMDRepository.this.onFlush(flushEvent);
        }

        public void onAutoFlush(AutoFlushEvent autoFlushEvent)
            throws HibernateException
        {
            HibernateMDRepository.this.onAutoFlush(autoFlushEvent);
        }
    }
}

// End HibernateMDRepository.java
