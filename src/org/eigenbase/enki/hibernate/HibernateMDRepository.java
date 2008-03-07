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

import java.lang.ref.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.jmi.model.init.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.mdr.EnkiChangeEventThread.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.hibernate.event.*;
import org.hibernate.event.def.*;
import org.hibernate.tool.hbm2ddl.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;

/**
 * HibernateMDRepository implements {@link MDRepository} and 
 * {@link EnkiMDRepository} for Hibernate-based metamodel storage.  In
 * addition, it acts as a {@link ListenerSource source} of 
 * {@link MDRChangeEvent} instances.
 * 
 * <p>Logging Notes.  Session and transactions boundaries are logged at level
 * {@link Level#FINE}.  If level is set to {@link Level#FINEST}, stack traces
 * are logged for each session and transaction boundary. 
 * 
 * @author Stephan Zuercher
 */
public class HibernateMDRepository
    implements MDRepository, EnkiMDRepository, 
               EnkiChangeEventThread.ListenerSource
{
    /** 
     * The name of the HibernateMDRepository metamodel configuration properties
     * file. Stored in the <code>META-INF/enki</code> directory of an Enki 
     * model JAR file.
     */
    public static final String CONFIG_PROPERTIES = "config.properties";

    /** 
     * The name of the metamodel-specific Hibernate mapping file. Stored in 
     * the <code>META-INF/enki</code> directory of an Enki model JAR file.
     */
    public static final String MAPPING_XML = "mapping.xml";
    
    /**
     * Path to the resource that contains a Hibernate mapping file for
     * persistent entities used across metamodels. 
     */
    public static final String HIBERNATE_STORAGE_MAPPING_XML = 
        "/org/eigenbase/enki/hibernate/storage/hibernate-storage-mapping.xml";
    
    /**
     * Configuration file property that contains the name of the 
     * {@link MetamodelInitializer} class used to initialize the metamodel.
     */
    public static final String PROPERTY_MODEL_INITIALIZER = 
        "enki.model.initializer";
    
    /**
     * Storage property that configures the behavior of implicit sessions.
     * Values are converted to boolean via {@link Boolean#valueOf(String)}.
     * If the value evaluates to true, implicit sessions are allowed (and are
     * closed when the first transaction within the session is committed or
     * rolled back).
     */
    public static final String PROPERTY_STORAGE_ALLOW_IMPLICIT_SESSIONS =
        "org.eigenbase.enki.hibernate.allowImplicitSessions";
    
    /**
     * Contains the default value for the 
     * {@link #PROPERTY_STORAGE_ALLOW_IMPLICIT_SESSIONS} storage property.
     * The default is {@value}.
     */
    public static final boolean DEFAULT_ALLOW_IMPLICIT_SESSIONS = false;
    
    /**
     * Identifier for the built-in MOF extent.
     */
    private static final String MOF_EXTENT = "MOF";
    
    /** Thread-local storage for MDR session contexts. */
    private static final ThreadLocal<MdrSession> tls =
        new ThreadLocal<MdrSession>();
    
    /** List of all known model configuration properties files. */
    private final List<Properties> modelPropertiesList;
    
    /** Storage configuration properties. */
    private final Properties storageProperties;

    /** Given default class loader, if any. */
    private final ClassLoader classLoader;
    
    /** Map of metamodel extent names to ModelDescriptor instances. */
    private final Map<String, ModelDescriptor> modelMap;
    
    /** Map of extent names to ExtentDescriptor instances. */
    private final Map<String, ExtentDescriptor> extentMap;
    
    private final boolean allowImplicitSessions;
    
    /** The Hibernate {@link SessionFactory} for this repository. */
    private SessionFactory sessionFactory;
    
    /** The {@link MofIdGenerator} for this repository. */
    private MofIdGenerator mofIdGenerator;
    
    /** The {@link EnkiChangeEventThread} for this repository. */
    private EnkiChangeEventThread thread;
    
    /** 
     * Map of {@link MDRChangeListener} instances to 
     * {@link EnkiMaskedMDRChangeListener} instances.
     */
    private Map<MDRChangeListener, EnkiMaskedMDRChangeListener> listeners;
    
    private final Logger log = 
        Logger.getLogger(HibernateMDRepository.class.getName());

    public HibernateMDRepository(
        List<Properties> modelProperties,
        Properties storageProperties,
        ClassLoader classLoader)
    {
        this.modelPropertiesList = modelProperties;
        this.storageProperties = storageProperties;
        this.classLoader = classLoader;
        this.modelMap = new HashMap<String, ModelDescriptor>();
        this.extentMap = new HashMap<String, ExtentDescriptor>();
        this.thread = null;
        this.listeners = 
            new IdentityHashMap<MDRChangeListener, EnkiMaskedMDRChangeListener>();
        
        this.allowImplicitSessions = 
            readStorageProperty(
                PROPERTY_STORAGE_ALLOW_IMPLICIT_SESSIONS, 
                DEFAULT_ALLOW_IMPLICIT_SESSIONS,
                Boolean.class);
        
        initModelMap();
        initModelExtent(MOF_EXTENT, false);
        initStorage();
    }
    
    private <T> T readStorageProperty(
        String name, T defaultValue, Class<T> cls)
    {
        String stringValue = storageProperties.getProperty(name);
        if (stringValue == null) {
            return defaultValue;
        }
        
        try {
            Constructor<T> cons = cls.getConstructor(String.class);
            
            return cons.newInstance(stringValue);
        } catch (Exception e) {
            log.log(
                Level.SEVERE, 
                "Error parsing storage property (" + name + "=[" + stringValue
                + "], " + cls.getName() + ")",
                e);
            return defaultValue;
        }
    }
    
    public void beginSession()
    {
        MdrSession mdrSession = tls.get();
        if (mdrSession != null) {
            logStack(Level.FINE, "begin re-entrant repository session");
            mdrSession.refCount++;
            return;
        }
        
        beginSessionImpl(false);
    }
    
    private MdrSession beginSessionImpl(boolean implicit)
    {
        if (implicit) {
            if (!allowImplicitSessions) {
                throw new InternalMdrError("implicit session");
            }
            
            logStack(Level.WARNING, "begin implicit repository session");
        } else {
            logStack(Level.FINE, "begin repository session");
        }
        
        Session session = sessionFactory.openSession();
        session.setFlushMode(FlushMode.COMMIT);
        
        MdrSession mdrSession = new MdrSession(session, implicit);
        mdrSession.refCount++;
        
        tls.set(mdrSession);
        
        return mdrSession;
    }
    
    public void endSession()
    {
        MdrSession mdrSession = tls.get();
        if (mdrSession == null) {
            throw new EnkiHibernateException(
                "session never opened/already closed");
        }
        
        if (--mdrSession.refCount != 0) {
            logStack(Level.FINE, "end re-entrant repository session");
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
            if (contexts.size() > 1 || !contexts.getFirst().isImplicit) {
                throw new EnkiHibernateException(
                    "attempted to close session while txn remains open: " 
                    + contexts.size());
            }
            
            // End the remaining implicit txn
            endTransImpl(false);
        }
        
        if (mdrSession.isImplicit) {
            log.warning("end implicit repository session");
        } else {
            log.fine("end repository session");
        }
        
        mdrSession.session.close();
        tls.set(null);
    }
    
    public void beginTrans(boolean write)
    {
        beginTransImpl(write, false);
    }
    
    private Context beginTransImpl(boolean write, boolean implicit)
    {
        assert(!write || !implicit): "Cannot support implicit write txn";
        
        if (log.isLoggable(Level.FINEST)) {
            logStack(
                Level.FINEST, 
                "begin txn; "
                + (write ? "write; " : "read; ")
                + (implicit ? "implicit" : "explicit"));
        }
        
        MdrSession mdrSession = tls.get();
        if (mdrSession == null) {
            mdrSession = beginSessionImpl(true);
        }
        
        if (implicit) {
            log.fine("begin implicit repository transaction");
        } else {
            log.fine("begin repository transaction");
        }

        LinkedList<Context> contexts = mdrSession.context;
        
        // Nested txns are okay, but the outermost txn is the only one that
        // commits/rollsback.
        boolean isCommitter = false;
        boolean setFlushMode = false;
        boolean beginTrans = false;
        if (contexts.isEmpty()) {
            isCommitter = true;
            setFlushMode = true;
            beginTrans = true;
        } else {
            isCommitter = contexts.getLast().isImplicit;

            if (!mdrSession.containsWrites && write) {
                setFlushMode = true;
            }
        }
        
        if (setFlushMode) {
            if (write) {
                mdrSession.session.setFlushMode(FlushMode.AUTO);
            } else {
                mdrSession.session.setFlushMode(FlushMode.COMMIT);
            }
        }

        Transaction trans;
        if (beginTrans) {
            trans = mdrSession.session.beginTransaction();
        } else {
            trans = contexts.getLast().transaction;
        }
        
        Context context = new Context(trans, write, implicit, isCommitter);

        contexts.add(context);
        if (write) {
            mdrSession.containsWrites = true;
        }
        
        if (write) {
            enqueueBeginTransEvent(mdrSession);
        }
        
        return context;
    }

    public void endTrans()
    {
        endTrans(false);
    }

    public void endTrans(boolean rollback)
    {
        MdrSession mdrSession = endTransImpl(rollback);
        
        if (mdrSession.isImplicit && mdrSession.context.isEmpty()) {
            mdrSession.refCount--;
            endSessionImpl(mdrSession);
        }
    }
    
    private MdrSession endTransImpl(boolean rollback)
    {
        if (log.isLoggable(Level.FINEST)) {
            logStack(
                Level.FINEST, 
                "end txn; "
                + (rollback ? "rollback" : "commit"));
        }

        MdrSession mdrSession = tls.get();
        if (mdrSession == null) {
            throw new EnkiHibernateException(
                "No repository session associated with this thread");
        }
        
        LinkedList<Context> contexts = mdrSession.context;
        if (contexts.isEmpty()) {
            throw new EnkiHibernateException(
                "No repository transactions associated with this thread");
        }

        Context context = contexts.removeLast();

        if (context.isImplicit) {
            log.fine("end implicit repository transaction");            
        } else {
            log.fine("end repository transaction");
        }

        if (!context.isCommitter) {
            return mdrSession;
        }
        
        Transaction txn = context.transaction;

        if (context.isWrite) {
            enqueueEndTransEvent(mdrSession);
        }
        
        // Note that even if "commit" is requested, we'll rollback if no 
        // writing was possible.
        if (rollback) {
            txn.rollback();
            
            fireCanceledChanges(mdrSession);
            
            if (mdrSession.containsWrites) {
                // Evict all cached and potentially modified objects.
                mdrSession.session.clear();
            }
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
                if (!mdrSession.containsWrites) {
                    txn.rollback();
                } else {
                    txn.commit();
                }
                
                fireChanges(mdrSession);
            } catch(HibernateException e) {
                fireCanceledChanges(mdrSession);
                throw e;
            }
        }

        if (!contexts.isEmpty()) {
            if (contexts.size() != 1) {
                throw new InternalMdrError(
                    "ended nested txn with multiple containers");

            }
                
            Context implicitContext = contexts.getFirst();
            if (!implicitContext.isImplicit) {
                throw new InternalMdrError(
                    "ended nested txn but containing txn is not implicit");
            }
            
            // Start a new transaction for the containing implicit read.
            implicitContext.transaction = 
                mdrSession.session.beginTransaction();
            mdrSession.containsWrites = false;
        }
        
        return mdrSession;
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
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(name);
            if (extentDesc != null) {
                throw new EnkiCreationFailedException(
                    "Extent '" + name + "' already exists");
            }
            
            enqueueEvent(
                getMdrSession(), 
                new ExtentEvent(
                    this, 
                    ExtentEvent.EVENT_EXTENT_CREATE, 
                    name, 
                    metaPackage,
                    Collections.unmodifiableCollection(
                        new ArrayList<String>(extentMap.keySet())), 
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
    }

    public void dropExtentStorage(String extent) throws EnkiDropFailedException
    {
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(extent);
            if (extentDesc == null) {
                return;
            }
            
            dropExtentStorage(extentDesc);
        }
    }

    public void dropExtentStorage(RefPackage refPackage) 
        throws EnkiDropFailedException
    {
        synchronized(extentMap) {
            for(ExtentDescriptor extentDesc: extentMap.values()) {
                if (extentDesc.extent.equals(refPackage)) {
                    dropExtentStorage(extentDesc);
                    return;
                }
            }
        }
    }
    
    private void dropExtentStorage(ExtentDescriptor extentDesc)
        throws EnkiDropFailedException
    {
        enqueueEvent(
            getMdrSession(),
            new ExtentEvent(
                this,
                ExtentEvent.EVENT_EXTENT_DELETE,
                extentDesc.name,
                null,
                Collections.unmodifiableCollection(
                    new ArrayList<String>(extentMap.keySet()))));
        
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
        MdrSession mdrSession = getMdrSession();
        
        long mofIdLong = MofIdUtil.parseMofIdStr(mofId); 

        RefBaseObject cachedResult = lookupByMofId(mdrSession, mofIdLong);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        RefBaseObject result = null;
        if ((mofIdLong & MetamodelInitializer.METAMODEL_MOF_ID_MASK) != 0) {
            synchronized(extentMap) {
                for(ExtentDescriptor extentDesc: extentMap.values()) {
                    // Only search in metamodels
                    if (extentDesc.modelDescriptor == null ||
                        extentDesc.modelDescriptor.name.equals(MOF_EXTENT))
                    {
                        result = extentDesc.initializer.getByMofId(mofId);
                        if (result != null) {
                            break;
                        }
                    }
                }
            }
        } else {
            Session session = mdrSession.session;
            
            Query query = session.getNamedQuery("TypeMappingByMofId");
            query.setLong("mofId", mofIdLong);
            
            MofIdTypeMapping mapping = (MofIdTypeMapping)query.uniqueResult();
            if (mapping != null) {
                query = 
                    session.getNamedQuery(
                        mapping.getTypeName() + "." + 
                        HibernateMappingHandler.QUERY_NAME_BYMOFID);
                query.setLong("mofId", mofIdLong);
                
                result = (RefBaseObject)query.uniqueResult();
            }
        }
        
        if (result != null) {
            storeByMofId(mdrSession, mofIdLong, result);
        }
        
        return result;
    }

    public void deleteExtentDescriptor(RefPackage refPackage)
    {
        synchronized(extentMap) {
            for(ExtentDescriptor extentDesc: extentMap.values()) {
                if (extentDesc.extent.equals(refPackage)) {
                    deleteExtentDescriptor(extentDesc);
                    return;
                }
            }
        }  
    }
    
    private void deleteExtentDescriptor(ExtentDescriptor extentDesc)
    {
        extentMap.remove(extentDesc.name);

        checkTransaction(true);
        
        Session session = getCurrentSession();
        Query query = session.getNamedQuery("ExtentByName");
        query.setString(0, extentDesc.name);
        Extent extent = (Extent)query.uniqueResult();
        session.delete(extent);
    }
    
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

    public String[] getExtentNames()
    {
        synchronized(extentMap) {
            return extentMap.keySet().toArray(new String[extentMap.size()]);
        }
    }

    public void shutdown()
    {                
        synchronized(listeners) {
            listeners.clear();
        }

        synchronized(extentMap) {
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
                sessionFactory = null;
                
                HibernateRefClassRegistry.instance().shutdown();
                HibernateRefAssociationRegistry.instance().shutdown();
            }
        }
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
        synchronized(extentMap) {
            ExtentDescriptor extentDesc = extentMap.get(name);
            if (extentDesc == null) {
                return false;
            }
            
            assert(extentDesc.modelDescriptor != null);
            
            return extentDesc.builtIn;        
        }
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
    
    private MdrSession getMdrSession()
    {
        MdrSession session = tls.get();
        if (session != null) {
            return session;
        }

        return beginSessionImpl(true);
    }
    
    private Context getContext()
    {
        MdrSession session = getMdrSession();
        
        if (session.context.isEmpty()) {
            return beginTransImpl(false, true);
        }
        
        return session.context.getLast();
    }
    
    public Session getCurrentSession()
    {
        return getMdrSession().session;
    }
    
    public void checkTransaction(boolean requireWrite)
    {
        if (requireWrite) {
            if (!isWriteTransaction()) {
                throw new EnkiHibernateException(
                    "Operation required write transaction");
            }
        } else {
            // Make sure a txn exists.
            getContext();
        }
    }
    
    public boolean isWriteTransaction()
    {
        return getContext().isWrite;
    }
    
    private boolean isNestedWriteTransaction(MdrSession session) 
    {
        LinkedList<Context> contexts = session.context;
        ListIterator<Context> iter = contexts.listIterator(contexts.size());
        while(iter.hasPrevious()) {
            Context context = iter.previous();
            
            if (context.isWrite) {
                return true;
            }
        }
        
        return false;
    }

    public MofIdGenerator getMofIdGenerator()
    {
        return getMdrSession().getMofIdGenerator();
    }
    
    private RefBaseObject lookupByMofId(MdrSession mdrSession, long mofId)
    {
        Map<Long, SoftReference<RefBaseObject>> cache = 
            mdrSession.byMofIdCache;

        SoftReference<RefBaseObject> ref = cache.get(mofId);
        if (ref == null) {
            return null;
        }
        
        RefBaseObject obj = ref.get();
        if (obj == null) {
            cache.remove(mofId);
        }
        
        return obj;
    }

    private void storeByMofId(
        MdrSession mdrSession, long mofId, RefBaseObject obj)
    {
        mdrSession.byMofIdCache.put(
            mofId, new SoftReference<RefBaseObject>(obj));
    }
    
    public Collection<?> lookupAllOfTypeResult(HibernateRefClass cls)
    {
        return getMdrSession().allOfTypeCache.get(cls);
    }
    
    public void storeAllOfTypeResult(
        HibernateRefClass cls, Collection<?> allOfType)
    {
        getMdrSession().allOfTypeCache.put(cls, allOfType);
    }
    
    public Collection<?> lookupAllOfClassResult(HibernateRefClass cls)
    {
        return getMdrSession().allOfClassCache.get(cls);
    }
    
    public void storeAllOfClassResult(
        HibernateRefClass cls, Collection<?> allOfClass)
    {
        getMdrSession().allOfClassCache.put(cls, allOfClass);
    }

    public void enqueueEvent(MDRChangeEvent event)
    {
        MdrSession mdrSession = getMdrSession();
        if (!isNestedWriteTransaction(mdrSession)) {
            throw new IllegalStateException("Not in write transaction");
        }

        if (event.isOfType(InstanceEvent.EVENT_INSTANCE_DELETE)) {
            InstanceEvent instanceEvent = (InstanceEvent)event;
            long mofId =
                MofIdUtil.parseMofIdStr(
                    instanceEvent.getInstance().refMofId());
            mdrSession.byMofIdCache.remove(mofId);
        }
        
        enqueueEvent(mdrSession, event);
    }
    
    /**
     * Helper method for generating the extent deletion even from a RefPackage.
     * 
     * @param pkg top-level RefPackage of the extent being deleted
     */
    public void enqueueExtentDeleteEvent(RefPackage pkg)
    {
        synchronized(extentMap) {
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
                    Collections.unmodifiableCollection(
                        new ArrayList<String>(extentMap.keySet()))));
        }
    }

    private void enqueueEvent(MdrSession mdrSession, MDRChangeEvent event)
    {
        // Cache event in Context (we'll need to fire it upon commit even if
        // there are no listeners now.)
        mdrSession.queuedEvents.add(event);
        
        // Fire as planned change immediately (and from this thread).
        synchronized(listeners) {
            for(EnkiMaskedMDRChangeListener listener: listeners.values()) {
                // Note: EnkiMaskedMDRChangeListener automatically squelches
                // RuntimeExceptions as required by the API.
                listener.plannedChange(event);
            }
        }
    }

    private void enqueueBeginTransEvent(MdrSession mdrSession)
    {
        enqueueEvent(
            mdrSession, 
            new TransactionEvent(
                this, TransactionEvent.EVENT_TRANSACTION_START));
    }

    private void enqueueEndTransEvent(MdrSession mdrSession)
    {
        enqueueEvent(
            mdrSession, 
            new TransactionEvent(
                this, TransactionEvent.EVENT_TRANSACTION_END));
    }

    private void fireCanceledChanges(MdrSession mdrSession)
    {
        synchronized(listeners) {
            for(MDRChangeEvent event: mdrSession.queuedEvents) {
                for(EnkiMaskedMDRChangeListener listener: listeners.values()) {
                    listener.changeCancelled(event);
                }
            }
            mdrSession.queuedEvents.clear();
        }
    }
    
    private void fireChanges(MdrSession mdrSession)
    {
        synchronized(listeners) {
            for(MDRChangeEvent event: mdrSession.queuedEvents) {
                thread.enqueueEvent(event);
            }
            mdrSession.queuedEvents.clear();
        }
    }
    
    private void loadExistingExtents(List<Extent> extents)
    {
        // Sort metamodel extents ahead of models.
        Collections.sort(
            extents, 
            new Comparator<Extent>() {
                public int compare(Extent e1, Extent e2)
                {
                    String modelExtentName1 = e1.getModelExtentName();
                    String modelExtentName2 = e2.getModelExtentName();
                    
                    boolean isMof1 = modelExtentName1.equals(MOF_EXTENT);
                    boolean isMof2 = modelExtentName2.equals(MOF_EXTENT);
                    
                    int c;
                    if (isMof1 && isMof2) {
                        // Both metamodels
                        c = 0;
                    } else if (!isMof1 && !isMof2) {
                        c = modelExtentName1.compareTo(modelExtentName2);
                    } else if (isMof1) {
                        return -1;
                    } else {
                        return 1;
                    }
                    
                    if (c != 0) {
                        return c;
                    }
                    
                    return e1.getExtentName().compareTo(e2.getExtentName());
                }
            });
        for(Extent extent: extents) {
            String extentName = extent.getExtentName();
            String modelExtentName = extent.getModelExtentName();

            if (modelExtentName.equals(MOF_EXTENT)) {
                initModelExtent(extentName, false);                
            } else {
                ModelDescriptor modelDesc = modelMap.get(modelExtentName);
    
                if (modelDesc == null) {
                    throw new ProviderInstantiationException(
                        "Missing model extent '" + modelExtentName + 
                        "' for extent '" + extentName + "'");
                }
                
                if (!extentMap.containsKey(modelExtentName)) {
                    // Should have been initialized previously (due to sorting)
                    throw new InternalMdrError(
                        "Missing metamodel extent '" + modelExtentName + "'");
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
    }
    
    private ExtentDescriptor createExtentStorage(
        String name,
        RefObject metaPackage, 
        RefPackage[] existingInstances)
    throws EnkiCreationFailedException
    {
        if (metaPackage == null) {
            initModelExtent(name, true);
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

        createExtentRecord(extentDesc.name, modelDesc.name);
        
        extentMap.put(name, extentDesc);

        return extentDesc;
    }

    private void createExtentRecord(String extentName, String modelExtentName)
    {
        Session session = getCurrentSession();
        
        Extent extentDbObj = new Extent();
        extentDbObj.setExtentName(extentName);
        extentDbObj.setModelExtentName(modelExtentName);
        
        session.save(extentDbObj);
    }
    
    // Must be externally synchronized
    private void initStorage()
    {
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
    }
    
    private URL getModelMappingUrl(ModelDescriptor modelDesc)
    {
        String configUrlStr = 
            modelDesc.properties.getProperty(
                MDRepositoryFactory.PROPERTY_ENKI_RUNTIME_CONFIG_URL);
        
        log.config(
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
            
            log.fine("Initialized Model Descriptor: " + name);
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
        ModelDescriptor mofDesc = 
            isMof ? null : modelMap.get(MOF_EXTENT);
        
        log.info("Initializing Extent Descriptor: " + name);
        
        ExtentDescriptor extentDesc = new ExtentDescriptor(name);
        
        extentDesc.modelDescriptor = mofDesc;
        
        MetamodelInitializer init;
        if (isMof) {
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
        init.setOwningRepository(this);
        init.init(metaModelPackage);
        
        extentDesc.extent = init.getModelPackage();
        extentDesc.initializer = init;
        extentDesc.builtIn = true;
        
        if (isNew && !isMof) {
            createExtentRecord(extentDesc.name, MOF_EXTENT);
        }
        
        extentMap.put(name, extentDesc);
        
        log.fine("Initialized Extent Descriptor: " + name);
    }
        
    public void onFlush(FlushEvent flushEvent) throws HibernateException
    {
        MdrSession mdrSession = tls.get();
        if (mdrSession == null) {
            // Not in a transaction (e.g. startup, shutdown)
            return;
        }
        
        mdrSession.allOfTypeCache.clear();
        mdrSession.allOfClassCache.clear();
    }

    public void onAutoFlush(AutoFlushEvent autoFlushEvent) 
        throws HibernateException
    {
        MdrSession mdrSession = tls.get();
        if (mdrSession== null) {
            // Not in a transaction (e.g. startup, shutdown)
            return;
        }

        if (!isNestedWriteTransaction(mdrSession)) {
            // Ignore auto-flush on read-only transactions.
            return;
        }
        
        mdrSession.allOfTypeCache.clear();
        mdrSession.allOfClassCache.clear();
    }

    private void logStack(Level level, String msg)
    {
        Throwable t = null;
        if (log.isLoggable(Level.FINEST)) {
            t = new RuntimeException("SHOW STACK");
        }
        
        log.log(level, msg, t);
    }
    
    /**
     * ModelDescriptor describes a meta-model.
     */
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
    
    /**
     * ExtentDescriptor describes an instantiated model extent.
     */
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
    
    private class MdrSession
    {
        private Session session;
        private boolean containsWrites;
        private boolean isImplicit;
        private int refCount;
        
        private final LinkedList<Context> context;
        private final Map<HibernateRefClass, Collection<?>> allOfTypeCache;
        private final Map<HibernateRefClass, Collection<?>> allOfClassCache;
        private final Map<Long, SoftReference<RefBaseObject>> byMofIdCache;
        private final List<MDRChangeEvent> queuedEvents;

        private MdrSession(Session session, boolean isImplicit)
        {
            this.session = session;
            this.context = new LinkedList<Context>();
            this.allOfTypeCache = 
                new HashMap<HibernateRefClass, Collection<?>>();
            this.allOfClassCache =
                new HashMap<HibernateRefClass, Collection<?>>();
            this.byMofIdCache =
                new HashMap<Long, SoftReference<RefBaseObject>>();
            this.containsWrites = false;
            this.queuedEvents = new LinkedList<MDRChangeEvent>();
            this.isImplicit = isImplicit;
            this.refCount = 0;
        }
        
        private MofIdGenerator getMofIdGenerator()
        {
            return HibernateMDRepository.this.mofIdGenerator;
        }
    }
    
    /**
     * Context represents an implicit or explicit MDR transaction.
     */
    private class Context
    {
        private Transaction transaction;
        private boolean isWrite;
        private boolean isImplicit;
        private boolean isCommitter;
        
        private Context(
            Transaction transaction, 
            boolean isWrite, 
            boolean isImplicit,
            boolean isCommitter)
        {
            this.transaction = transaction;
            this.isWrite = isWrite;
            this.isImplicit = isImplicit;
            this.isCommitter = isCommitter;
        }
    }
    
    /**
     * EventListener implements Hibernate's {@link FlushEventListener} and 
     * {@link AutoFlushEventListener} and invokes 
     * {@link HibernateMDRepository#onFlush(FlushEvent)} and
     * {@link HibernateMDRepository#onAutoFlush(AutoFlushEvent)} to process
     * those events.
     */
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
