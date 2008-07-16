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
import java.util.concurrent.atomic.*;
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
import org.hibernate.criterion.*;
import org.hibernate.stat.*;
import org.hibernate.tool.hbm2ddl.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;

/**
 * HibernateMDRepository implements {@link MDRepository} and 
 * {@link EnkiMDRepository} for Hibernate-based metamodel storage.  In
 * addition, it acts as a {@link ListenerSource source} of 
 * {@link MDRChangeEvent} instances.
 * 
 * <p>Storage properties.  Set the 
 * <code>org.eigenbase.enki.implementationType</code> storage property to 
 * {@link MdrProvider#ENKI_HIBERNATE} to enable the Hibernate 
 * MDR implementation.  Additional storage properties of note are listed
 * in the following table.
 * 
 * <table border="1">
 *   <caption><b>Hibernate-specific Storage Properties</b></caption>
 *   <tr>
 *     <th align="left">Name</th>
 *     <th align="left">Description</th>
 *   </tr>
 *   <tr>
 *     <td align="left">{@value #PROPERTY_STORAGE_ALLOW_IMPLICIT_SESSIONS}</td>
 *     <td align="left">
 *       Controls whether or not implicit sessions are allowed.  Defaults to
 *       {@value DEFAULT_ALLOW_IMPLICIT_SESSIONS}.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td align="left">{@value #PROPERTY_STORAGE_TRACK_SESSIONS}</td>
 *     <td align="left">
 *       Controls whether session begin/end pairs are tracked with a unique
 *       identifier.  Useful for determining where a particular session begins
 *       and ends when sessions are unexpectedly nested.  Note that the 
 *       generated identifiers are only unique within a repository instance.
 *       Defaults to {@value #DEFAULT_TRACK_SESSIONS}.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td align="left">{@value #PROPERTY_STORAGE_TYPE_LOOKUP_FLUSH_SIZE}</td>
 *     <td align="left">
 *       Controls whether or not and how frequently insertions into the MOF
 *       ID/type lookup table are flushed. Defaults to the value of
 *       {@value #PROPERTY_STORAGE_HIBERNATE_JDBC_BATCH_SIZE}.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td align="left">hibernate.*</td>
 *     <td align="left">
 *       All properties are passed to Hibernate's {@link Configuration} without
 *       modification. Note that the property
 *       {@value #PROPERTY_STORAGE_HIBERNATE_DEFAULT_BATCH_FETCH_SIZE} controls
 *       batch fetch size for lazy associations.
 *     </td>
 *   </tr>
 * </table>
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
     * The name of a metamodel-specific Hibernate mapping file that contains
     * only &lt;database-object&gt; entries for special indexes.
     */
    public static final String INDEX_MAPPING_XML = "indexMapping.xml";
    
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
     * Configuration file property indicates whether this model is a plug-in
     * or a base model.  Valid values are true or false.
     */
    public static final String PROPERTY_MODEL_PLUGIN = 
        "enki.model.plugin";

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
     * Storage property that configures whether sessions are tracked.  Session
     * tracking allows matching of the begin/end session API calls in the 
     * log file.  If the value evaluates to true, sessions are tracked with
     * identifiers that are unique within this repository instance.
     */
    public static final String PROPERTY_STORAGE_TRACK_SESSIONS =
        "org.eigenbase.enki.hibernate.trackSessions";
    
    /**
     * Contains the default value for the 
     * {@link #PROPERTY_STORAGE_TRACK_SESSIONS} storage property.  
     * The default is {@value}. 
     */
    public static final boolean DEFAULT_TRACK_SESSIONS = false;

    /**
     * Storage property that controls whether and how frequently the Hibernate
     * session is flushed while inserting entires into the MOF ID/type lookup
     * table.  Defaults to the value of the
     * {@value #PROPERTY_STORAGE_HIBERNATE_JDBC_BATCH_SIZE} property.  Values
     * less than or equal to 0 disable flushing.
     */
    public static final String PROPERTY_STORAGE_TYPE_LOOKUP_FLUSH_SIZE =
        "org.eigenbase.enki.hibernate.typeLookupFlushSize";

    /**
     * Hibernate property re-used as a storage property to control the default
     * size of MOF ID/type mapping flushes during transaction commit in
     * addition to its normal Hibernate behavior.
     */
    public static final String PROPERTY_STORAGE_HIBERNATE_JDBC_BATCH_SIZE =
        "hibernate.jdbc.batch_size";

    /**
     * Hibernate property re-used as a storage property to control lazy
     * association batch size in addition to its normal Hibernate behavior.
     */
    public static final String PROPERTY_STORAGE_HIBERNATE_DEFAULT_BATCH_FETCH_SIZE =
        "hibernate.default_batch_fetch_size";

    /**
     * Storage property that configures whether session factory statistics
     * are periodically logged.  Values of zero or less disable statistics
     * logging.  Larger values indicate the number of seconds between log
     * messages.  Default value is {@link #DEFAULT_PERIODIC_STATS_INTERVAL}.
     */
    public static final String PROPERTY_PERIODIC_STATS =
        "org.eigenbase.enki.hibernate.periodicStats";
    
    /**
     * Contains the default value for {@link #PROPERTY_PERIODIC_STATS}
     * storage property.  The default is {@value}.
     */
    public static final int DEFAULT_PERIODIC_STATS_INTERVAL = 0;
    
    /**
     * Storage property that configures periodic session factory statistics
     * logging to include information about the size (in bytes) of Hibernate
     * caches.  Computing cache memory size requires serializing all cached
     * objects, which can be very slow.  Valid values are true or false.
     * Defaults to {@link #DEFAULT_MEM_STATS}. {@link #PROPERTY_PERIODIC_STATS}
     * must be enabled for memory statistics logging to occur.
     */
    public static final String PROPERTY_MEM_STATS =
        "org.eigenbase.enki.hibernate.periodicStats.memStats";
    
    /**
     * Contains the default value of {@link #PROPERTY_MEM_STATS}.
     * The default is {@value}.
     */
    public static final boolean DEFAULT_MEM_STATS = false;
    
    /**
     * Identifier for the built-in MOF extent.
     */
    private static final String MOF_EXTENT = "MOF";
    
    private static final MdrSessionStack sessionStack = new MdrSessionStack();
    
    private final AtomicInteger sessionCount;
    
    private final AtomicInteger sessionIdGenerator;
    
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
    
    /** Value of {@link #PROPERTY_STORAGE_TYPE_LOOKUP_FLUSH_SIZE}. */
    private final int typeLookupFlushSize;

    /** Value of hibernate.default.batch_fetch_size. */
    private final int defaultBatchFetchSize;
    
    private final boolean allowImplicitSessions;
    
    private final boolean trackSessions;
    
    private final int periodicStatsInterval;
    private final boolean logMemStats;
    
    private Timer periodicStatsTimer;
    
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
    
    /** Map of unique class identifier to HibernateRefClass. */
    private final HashMap<String, HibernateRefClass> classRegistry;
    
    /** Map of unique association identifier to HibernateRefAssociation. */
    private final HashMap<String, HibernateRefAssociation> assocRegistry;
    
    private final Logger log;

    public HibernateMDRepository(
        List<Properties> modelProperties,
        Properties storageProperties,
        ClassLoader classLoader)
    {
        this.log = Logger.getLogger(HibernateMDRepository.class.getName());
        this.modelPropertiesList = modelProperties;
        this.storageProperties = storageProperties;
        this.classLoader = classLoader;
        this.modelMap = new HashMap<String, ModelDescriptor>();
        this.extentMap = new HashMap<String, ExtentDescriptor>();
        this.thread = null;
        this.listeners = 
            new IdentityHashMap<MDRChangeListener, EnkiMaskedMDRChangeListener>();
        this.classRegistry = new HashMap<String, HibernateRefClass>();
        this.assocRegistry = new HashMap<String, HibernateRefAssociation>();
        
        int jdbcBatchSize = 
            readStorageProperty(
                PROPERTY_STORAGE_HIBERNATE_JDBC_BATCH_SIZE, -1, Integer.class);
        
        this.typeLookupFlushSize =
            readStorageProperty(
                PROPERTY_STORAGE_TYPE_LOOKUP_FLUSH_SIZE,
                jdbcBatchSize,
                Integer.class);

        this.defaultBatchFetchSize = 
            readStorageProperty(
                PROPERTY_STORAGE_HIBERNATE_DEFAULT_BATCH_FETCH_SIZE,
                -1,
                Integer.class);

        this.allowImplicitSessions = 
            readStorageProperty(
                PROPERTY_STORAGE_ALLOW_IMPLICIT_SESSIONS, 
                DEFAULT_ALLOW_IMPLICIT_SESSIONS,
                Boolean.class);
        this.trackSessions =
            readStorageProperty(
                PROPERTY_STORAGE_TRACK_SESSIONS,
                DEFAULT_TRACK_SESSIONS,
                Boolean.class);
        this.periodicStatsInterval = 
            readStorageProperty(
                PROPERTY_PERIODIC_STATS, 
                DEFAULT_PERIODIC_STATS_INTERVAL, 
                Integer.class);
        this.logMemStats =
            readStorageProperty(
                PROPERTY_MEM_STATS, 
                DEFAULT_MEM_STATS, 
                Boolean.class);
        
        initModelMap();
        initModelExtent(MOF_EXTENT, false);
        initStorage();
        
        this.sessionCount = new AtomicInteger(0);  
        this.sessionIdGenerator = new AtomicInteger(0);
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
    
    public EnkiMDSession detachSession()
    {
        MdrSession mdrSession = sessionStack.pop();
        return mdrSession;
    }
    
    public void reattachSession(EnkiMDSession session)
    {
        MdrSession existingSession = sessionStack.peek(this);
        if (existingSession != null) {
            throw new EnkiHibernateException(
                "must end current session before re-attach");
        }
        
        if (session == null) {
            // nothing to do
            return;
        }
        
        if (!(session instanceof MdrSession)) {
            throw new EnkiHibernateException(
                "invalid session object; wrong type");
        }
        
        sessionStack.push((MdrSession)session);
    }

    public void endDetachedSession(EnkiMDSession session)
    {
        reattachSession(session);
        endSession();
    }

    public void beginSession()
    {
        MdrSession mdrSession = sessionStack.peek(this);
        if (mdrSession != null) {
            logStack(
                Level.FINE, 
                "begin re-entrant repository session", 
                mdrSession.sessionId);
            mdrSession.refCount++;
            return;
        }
        
        beginSessionImpl(false);
    }
    
    private MdrSession beginSessionImpl(boolean implicit)
    {
        int sessionId = -1;
        if (trackSessions) {
            sessionId = sessionIdGenerator.incrementAndGet();
        }
        
        if (implicit) {
            if (!allowImplicitSessions) {
                throw new InternalMdrError("implicit session");
            }
            
            logStack(
                Level.WARNING,
                "begin implicit repository session", 
                sessionId);
        } else {
            logStack(Level.FINE, "begin repository session", sessionId);
        }
        
        Session session = sessionFactory.openSession();
        session.setFlushMode(FlushMode.COMMIT);
        
        MdrSession mdrSession = new MdrSession(session, implicit, sessionId);
        mdrSession.refCount++;
        
        sessionStack.push(mdrSession);
        int count = sessionCount.incrementAndGet();
        assert(count > 0);
        
        return mdrSession;
    }
    
    public void endSession()
    {
        MdrSession mdrSession = sessionStack.peek(this);
        if (mdrSession == null) {
            throw new EnkiHibernateException(
                "session never opened/already closed");
        }
        
        if (--mdrSession.refCount != 0) {
            logStack(
                Level.FINE, 
                "end re-entrant repository session", 
                mdrSession.sessionId);
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
            logStack(
                Level.WARNING, 
                "end implicit repository session", 
                mdrSession.sessionId);
        } else {
            logStack(
                Level.FINE, "end repository session", mdrSession.sessionId);
        }
        
        mdrSession.close();
        sessionStack.pop();
        int count = sessionCount.decrementAndGet();
        assert(count >= 0);
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
        
        MdrSession mdrSession = sessionStack.peek(this);
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
        boolean beginTrans = false;
        if (contexts.isEmpty()) {
            isCommitter = true;
            beginTrans = true;
        } else {
            isCommitter = contexts.getLast().isImplicit;
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

        MdrSession mdrSession = sessionStack.peek(this);
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
                mdrSession.session.clear();
                mdrSession.reset();
            }
        } else {
            try {
                Session session = mdrSession.session;
                if (mdrSession.containsWrites) {
                    if (!mdrSession.mofIdDeleteSet.isEmpty()) {
                        // Update enki type mapping
                        Query query = 
                            session.getNamedQuery("TypeMappingDeleteByMofId");
                        query.setParameterList(
                            "mofIds", mdrSession.mofIdDeleteSet);
                        query.executeUpdate();
                        
                        mdrSession.mofIdCreateMap.keySet().removeAll(
                            mdrSession.mofIdDeleteSet);
                        
                        mdrSession.mofIdDeleteSet.clear();
                    }
                    
                    if (!mdrSession.mofIdCreateMap.isEmpty()) {
                        int i = 0;
                        int flushSize = typeLookupFlushSize;
                        boolean flush = flushSize > 0;
                        for(Map.Entry<Long, Class<? extends RefObject>> entry:
                                mdrSession.mofIdCreateMap.entrySet())
                        {
                            if (flush && i++ % flushSize == 0) {
                                session.flush();
                                session.clear();
                            }
                            MofIdTypeMapping mapping = new MofIdTypeMapping();
                            mapping.setMofId(entry.getKey());
                            mapping.setTypeName(entry.getValue().getName());
                            mdrSession.session.save(mapping);
                        }
                        
                        mdrSession.mofIdCreateMap.clear();
                    }
                }
                
                // TODO: check for constraint violations
                if (false) {
                    ArrayList<String> constraintErrors = 
                        new ArrayList<String>();
                    boolean foundConstraintError = false;
        
                    if (foundConstraintError) {
                        txn.rollback();
                        
                        throw new HibernateConstraintViolationException(
                            constraintErrors);
                    }
                }
            
                if (!mdrSession.containsWrites) {
                    txn.rollback();
                } else {
                    txn.commit();
                }

                fireChanges(mdrSession);
            } catch(HibernateException e) {
                fireCanceledChanges(mdrSession);
                throw e;
            } finally {
                mdrSession.reset();
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
    
    public int getBatchSize()
    {
        return defaultBatchFetchSize;
    }
    
    public RefObject getByMofId(String mofId, RefClass cls)
    {
        Long mofIdLong = MofIdUtil.parseMofIdStr(mofId); 

        return getByMofId(mofIdLong, cls, mofId);
    }
    
    public RefObject getByMofId(long mofId, RefClass cls)
    {
        return getByMofId(mofId, cls, null);
    }
    
    public List<RefObject> getByMofId(List<Long> mofIds, RefClass cls)
    {
        if (mofIds.isEmpty()) {
            return Collections.emptyList();
        } else if (mofIds.size() == 1) {
            return Collections.singletonList(getByMofId(mofIds.get(0), cls));
        }
        
        HibernateRefClass hibRefCls = (HibernateRefClass)cls;

        Class<?> instanceClass = hibRefCls.getInstanceClass();
        
        Criteria criteria = 
            getCurrentSession().createCriteria(instanceClass)
                .add(Restrictions.in("id", mofIds))
                .setCacheable(true);

        MdrSession mdrSession = getMdrSession();
        
        ArrayList<RefObject> result = new ArrayList<RefObject>();
        for(RefObjectBase refObj: 
                GenericCollections.asTypedList(
                    criteria.list(), RefObjectBase.class))
        {
            if (!mdrSession.mofIdDeleteSet.contains(refObj.getMofId())) {
                result.add(refObj);
            }
        }

        if (mofIds.size() > result.size()) {
            for(Long queryMofId: mofIds) {
                Class<? extends RefObject> c = 
                    mdrSession.mofIdCreateMap.get(queryMofId);
                if (c != null) {
                    result.add(getByMofId(mdrSession, queryMofId, c));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Loads a RefObject by MOF ID.  The string version of the MOF ID is
     * optional, but if non-null must match <code>mofIdLong</code>.  If
     * null, mofIdLong is converted to a string if needed (it usually is not).
     * 
     * @param mofIdLong MOF ID of the object to load
     * @param cls {@link RefClass} representing the object's concrete type
     * @param mofId optional string version of MOF ID
     * @return the object requested or null if not found or already deleted
     */
    private RefObject getByMofId(long mofIdLong, RefClass cls, String mofId)
    {
        MdrSession mdrSession = getMdrSession();
        
        RefBaseObject cachedResult = 
            (RefBaseObject)lookupByMofId(mdrSession, mofIdLong);
        if (cachedResult != null) {
            return convertToRefObject(cls, cachedResult);
        }
        
        if (isMetamodelMofId(mofIdLong)) {
            if (mofId == null) {
                mofId = MofIdUtil.makeMofIdStr(mofIdLong);
            }
            RefBaseObject result = findMetaByMofId(mofId);
            return convertToRefObject(cls, result);
        }

        if (mdrSession.mofIdDeleteSet.contains(mofIdLong)) {
            return null;
        }
        
        Class<?> instanceClass = ((HibernateRefClass)cls).getInstanceClass();

        return getByMofId(mdrSession, mofIdLong, instanceClass);
    }

    private RefObject getByMofId(
        MdrSession mdrSession,
        Long mofIdLong,
        Class<?> instanceClass)
    {
        Session session = mdrSession.session;
        
        RefObject result = (RefObject)session.get(instanceClass, mofIdLong);
            
        if (result != null) {
            storeByMofId(mdrSession, mofIdLong, result);
        }
        
        return result;
    }
    
    private boolean isMetamodelMofId(long mofId)
    {
        return (mofId & MetamodelInitializer.METAMODEL_MOF_ID_MASK) != 0;
    }

    private RefObject convertToRefObject(
        RefClass cls,
        RefBaseObject cachedResult)
    {
        if (!(cachedResult instanceof RefObject)) {
            return null;
        }
        
        RefObject cachedResultObj = (RefObject)cachedResult;
        
        if (!cachedResultObj.refClass().equals(cls)) {
            return null;
        }

        return cachedResultObj;
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
        if (isMetamodelMofId(mofIdLong)) {
            result = findMetaByMofId(mofId);
        } else {
            if (mdrSession.mofIdDeleteSet.contains(mofIdLong)) {
                return null;
            }
            
            if (mdrSession.mofIdCreateMap.containsKey(mofIdLong)) {
                Class<? extends RefObject> cls = 
                    mdrSession.mofIdCreateMap.get(mofIdLong);
                return getByMofId(mdrSession, mofIdLong, cls);
            }
            
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
    
    private RefBaseObject findMetaByMofId(String mofId)
    {
        synchronized(extentMap) {
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
        }
        
        return null;
    }

    private RefBaseObject lookupByMofId(MdrSession mdrSession, Long mofId)
    {
        return softCacheLookup(mdrSession.byMofIdCache, mofId);
    }
    
    private <V, K> V softCacheLookup(Map<K, SoftReference<V>> cache, K key)
    {
        SoftReference<V> ref = cache.get(key);
        if (ref == null) {
            return null;
        }
        
        V value = ref.get();
        if (value == null) {
            cache.remove(key);
        }
        
        return value;
    }

    private <K, V> void softCacheStore(
        Map<K, SoftReference<V>> cache, K key, V value)
    {
        cache.put(key, new SoftReference<V>(value));
    }
    
    private void storeByMofId(
        MdrSession mdrSession, Long mofId, RefBaseObject obj)
    {
        softCacheStore(mdrSession.byMofIdCache, mofId, obj);
    }
    
    public RefObject findAllOfType(
        RefClass cls, String featureName, Object value)
    {
        HibernateRefClass hibRefCls = (HibernateRefClass)cls;
        
        String propertyName = 
            StringUtil.mangleIdentifier(
                featureName, StringUtil.IdentifierType.CAMELCASE_INIT_LOWER)
            + HibernateJavaHandler.IMPL_SUFFIX;
        
        Criteria criteria = 
            getCurrentSession().createCriteria(hibRefCls.getInterfaceClass())
                .add(Restrictions.eq(propertyName, value))
                .setCacheable(true);
        
        return (RefObject)criteria.uniqueResult();
    }
    
    public RefObject findAllOfType(
        RefClass cls, RefObject feature, Object value)
    {
        ModelElement featureElem = (ModelElement)feature;
        return findAllOfType(cls, featureElem.getName(), value);
    }
    
    public RefObject findAllOfClass(
        RefClass cls, String featureName, Object value)
    {
        HibernateRefClass hibRefCls = (HibernateRefClass)cls;
        
        String propertyName = 
            StringUtil.mangleIdentifier(
                featureName, StringUtil.IdentifierType.CAMELCASE_INIT_LOWER)
            + HibernateJavaHandler.IMPL_SUFFIX;
        
        Criteria criteria = 
            getCurrentSession().createCriteria(hibRefCls.getInstanceClass())
                .add(Restrictions.eq(propertyName, value))
                .setCacheable(true);
        
        return (RefObject)criteria.uniqueResult();
    }
    
    public RefObject findAllOfClass(
        RefClass cls, RefObject feature, Object value)
    {
        ModelElement featureElem = (ModelElement)feature;
        return findAllOfClass(cls, featureElem.getName(), value);
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
        log.info("repository shut down");

        int count = sessionCount.get();
        if (count != 0) {
            throw new EnkiHibernateException(
                "cannot shutdown while " + count + " session(s) are open");
        }
        
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
                
                stopPeriodicStats();
                
                if (sessionFactory.getStatistics().isStatisticsEnabled()) {
                    sessionFactory.getStatistics().logSummary();
                }
                
                sessionFactory.close();
                sessionFactory = null;
                
                classRegistry.clear();
                assocRegistry.clear();
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
    public Collection<EnkiMaskedMDRChangeListener> getListeners()
    {
        synchronized(listeners) {
            return new ArrayList<EnkiMaskedMDRChangeListener>(
                listeners.values());
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
        MdrSession session = sessionStack.peek(this);
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
    
    public static HibernateMDRepository getCurrentRepository()
    {
        MdrSession session = sessionStack.peek();
        if (session == null) {
            throw new EnkiHibernateException(
                "No current session on this thread");
        }
        
        return session.getRepos();
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
    
    @SuppressWarnings("unchecked")
    public Collection<?> allOfType(HibernateRefClass cls, String queryName)
    {
        MdrSession mdrSession = getMdrSession();
        checkTransaction(false);
        
        Collection<?> simpleResult = mdrSession.allOfTypeCache.get(cls);
        if (simpleResult == null) {
            Session session = getCurrentSession();
            
            Query query = session.getNamedQuery(queryName);
            
            simpleResult = query.list();
            
            mdrSession.allOfTypeCache.put(cls, simpleResult);
        }
        
        Set<RefObject> result = new HashSet<RefObject>();
        for(Object obj: simpleResult) {
            RefObjectBase b = (RefObjectBase)obj;
            if (!mdrSession.mofIdDeleteSet.contains(b.getMofId())) {
                result.add(b);
            }
        }
        
        Class<?> ifaceClass = cls.getInterfaceClass();
        for(Map.Entry<Long, Class<? extends RefObject>> e: 
                mdrSession.mofIdCreateMap.entrySet())
        {
            Class<? extends RefObject> createdCls = e.getValue();
            if (ifaceClass.isAssignableFrom(createdCls)) {
                result.add(getByMofId(mdrSession, e.getKey(), createdCls));
            }
        }
        
        return Collections.unmodifiableSet(result);
    }
    
    @SuppressWarnings("unchecked")
    public Collection<?> allOfClass(HibernateRefClass cls, String queryName)
    {
        MdrSession mdrSession = getMdrSession();
        
        checkTransaction(false);
        
        Collection<?> simpleResult= mdrSession.allOfClassCache.get(cls);
        if (simpleResult == null) {
            Session session = getCurrentSession();
            
            Query query = session.getNamedQuery(queryName);
    
            simpleResult = query.list();
    
            mdrSession.allOfClassCache.put(cls, simpleResult);
        }
        
        Set<RefObject> result = new HashSet<RefObject>();
        for(Object obj: simpleResult) {
            RefObjectBase b = (RefObjectBase)obj;
            if (!mdrSession.mofIdDeleteSet.contains(b.getMofId())) {
                result.add(b);
            }
        }
        
        Class<?> instanceClass = cls.getInstanceClass();
        for(Map.Entry<Long, Class<? extends RefObject>> e: 
                mdrSession.mofIdCreateMap.entrySet())
        {
            if (instanceClass.isAssignableFrom(e.getValue())) {
                result.add(getByMofId(e.getKey(), cls));
            }
        }
        
        return Collections.unmodifiableSet(result);
    }
    
    /**
     * Find the identified {@link HibernateRefClass}.
     * 
     * @param uid unique {@link HibernateRefClass} identifier
     * @return {@link HibernateRefClass} associated with UID
     * @throws InternalJmiError if the class is not found
     */
    public HibernateRefClass findRefClass(String uid)
    {
        HibernateRefClass refClass = classRegistry.get(uid);
        if (refClass == null) {
            throw new InternalJmiError(
                "Cannot find HibernateRefClass identified by '" + uid + "'");
        }
        
        return refClass;
    }
    
    /**
     * Register the given {@link HibernateRefClass}.
     * @param uid unique identifier for the given {@link HibernateRefClass} 
     * @param refClass a {@link HibernateRefClass} 
     * @throws InternalJmiError on duplicate uid
     * @throws NullPointerException if either parameter is null
     */
    public void registerRefClass(String uid, HibernateRefClass refClass)
    {
        if (uid == null) {
            throw new NullPointerException("uid == null");
        }
        if (refClass == null) {
            throw new NullPointerException("refClass == null");
        }
        
        HibernateRefClass prev = classRegistry.put(uid, refClass);
        if (prev != null) {
            throw new InternalJmiError(
                "HibernateRefClass (mofId " + prev.refMofId() + "; class " + 
                prev.getClass().getName() + ") already identified by '" + uid +
                "'; Cannot replace it with HibernateRefClass (mofId " + 
                refClass.refMofId() + "; class " + 
                refClass.getClass().getName() + ")"); 
        }
        
        log.finer(
            "Registered class " + refClass.getClass().getName() + 
            ", identified by '" + uid + "'");
    }
    
    /**
     * Unregister a previously 
     * {@link #registerRefClass(String, HibernateRefClass) registered} 
     * {@link HibernateRefClass}.
     * 
     * @param uid unique identifier for the HibernateRefClass
     */
    public void unregisterRefClass(String uid)
    {
        if (uid == null) {
            throw new NullPointerException("uid == null");
        }
        
        HibernateRefClass old = classRegistry.remove(uid);
        if (old == null) {
            throw new InternalJmiError(
                "HibernateRefClass (uid " + uid + ") was never registered");
        }
        
        log.finer(
            "Unregistered class " + old.getClass().getName() + 
            ", identified by '" + uid + "'");
    }
    
    /**
     * Find the identified {@link HibernateRefAssociation}.
     * 
     * @param uid unique {@link HibernateRefAssociation} identifier
     * @return {@link HibernateRefAssociation} associated with UID
     * @throws InternalJmiError if the class is not found
     */
    public HibernateRefAssociation findRefAssociation(String uid)
    {
        HibernateRefAssociation refAssoc = assocRegistry.get(uid);
        if (refAssoc == null) {
            throw new InternalJmiError(
                "Cannot find HibernateRefAssociation identified by '" 
                + uid + "'");
        }
        
        return refAssoc;
    }
    
    /**
     * Register the given {@link HibernateRefAssociation}.
     * @param uid unique identifier for the given 
     *            {@link HibernateRefAssociation} 
     * @param refAssoc a {@link HibernateRefAssociation} 
     * @throws InternalJmiError on duplicate uid
     * @throws NullPointerException if either parameter is null
     */
    public void registerRefAssociation(
        String uid, HibernateRefAssociation refAssoc)
    {
        if (uid == null) {
            throw new NullPointerException("uid == null");
        }
        if (refAssoc == null) {
            throw new NullPointerException("refAssoc == null");
        }
        
        HibernateRefAssociation prev = assocRegistry.put(uid, refAssoc);
        if (prev != null) {
            throw new InternalJmiError(
                "HibernateRefAssociation (mofId " + prev.refMofId() +
                "; class " + prev.getClass().getName() +
                ") already identified by '" + uid +
                "'; Cannot replace it with HibernateRefAssociation (mofId " + 
                refAssoc.refMofId() + "; class " + 
                refAssoc.getClass().getName() + ")"); 
        }

        log.finer(
            "Registered assoc " + refAssoc.getClass().getName() + 
            ", identified by '" + uid + "'");
    }
    
    /**
     * Unregister a previously 
     * {@link #registerRefAssociation(String, HibernateRefAssociation) registered} 
     * {@link HibernateRefAssociation}.
     * 
     * @param uid unique identifier for the HibernateRefAssociation
     */
    public void unregisterRefAssociation(String uid)
    {
        if (uid == null) {
            throw new NullPointerException("uid == null");
        }
        
        HibernateRefAssociation old = assocRegistry.remove(uid);
        if (old == null) {
            throw new InternalJmiError(
                "HibernateRefAssociation (uid " + uid + 
                ") was never registered");
        }
        
        log.finer(
            "Unregistered assoc " + old.getClass().getName() + 
            ", identified by '" + uid + "'");
    }
    
    public void enqueueEvent(MDRChangeEvent event)
    {
        MdrSession mdrSession = getMdrSession();
        if (!isNestedWriteTransaction(mdrSession)) {
            throw new IllegalStateException("Not in write transaction");
        }

        enqueueEvent(mdrSession, event);
    }
    
    public void recordObjectCreation(HibernateObject object)
    {
        MdrSession mdrSession = getMdrSession();

        long mofId = object.getMofId();

        mdrSession.mofIdCreateMap.put(mofId, object.getClass());
    }
    
    public void recordObjectDeletion(HibernateObject object)
    {
        MdrSession mdrSession = getMdrSession();
        
        long mofId = object.getMofId();
        mdrSession.byMofIdCache.remove(mofId);
        mdrSession.mofIdDeleteSet.add(mofId);
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
                    
                    for(MetamodelInitializer init: 
                            modelExtentDesc.pluginInitializers)
                    {
                        init.stitchPackages(extentDesc.extent);
                    }
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
            
            for(MetamodelInitializer init: extentDesc.pluginInitializers) {
                init.stitchPackages(extentDesc.extent);
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
    
    private void initStorage()
    {
        Configuration config = newConfiguration();

        initProviderStorage(config);
        
        for(ModelDescriptor modelDesc: modelMap.values()) {
            if (MOF_EXTENT.equals(modelDesc.name)) {
                continue;
            }

            configureMappings(config, modelDesc);
        }
        
        sessionFactory = config.buildSessionFactory();
        
        startPeriodicStats();
        
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
        for(Map.Entry<Object, Object> entry: storageProperties.entrySet())
        {
            String key = entry.getKey().toString();
            String value = 
                entry.getValue() == null 
                    ? null 
                    : entry.getValue().toString();
            
            config.setProperty(key, value);
        }
    
        if (includeProviderMapping) {
            URL internalConfigIUrl = 
                getClass().getResource(HIBERNATE_STORAGE_MAPPING_XML);
            config.addURL(internalConfigIUrl);
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
            // Presume that table doesn't exist.
            
            // REVIEW: SWZ: 3/12/08: If it's a connection error, and suddenly
            // starts working (startup race?) we could conceivably destroy the
            // tables, which would be bad.
            log.log(Level.FINE, "Extent Query Error", e);
        } finally {
            trans.commit();
            
            tempSessionFactory.close();
        }
        
        if (exists) {
            log.info("Validating Enki Hibernate provider schema");
            
            SchemaValidator validator = new SchemaValidator(config);
            
            boolean requiresUpdate = false;
            try {
                validator.validate();
            } catch(HibernateException e) {
                log.log(
                    Level.WARNING, 
                    "Enki Hibernate provider schema validation failed", 
                    e);
                requiresUpdate = true;
            }
        
            if (requiresUpdate) {
                log.info("Updating Enki Hibernate provider schema");
                
                SchemaUpdate update = new SchemaUpdate(config);
                
                try {
                    update.execute(false, true);
                } catch(HibernateException e) {
                    throw new ProviderInstantiationException(
                        "Unable to update Enki Hibernate provider schema", e);
                }
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
        ClassLoader contextClassLoader = null;
        if (classLoader != null) {
            contextClassLoader = 
                Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        try {
            Configuration config = newConfiguration();
            
            configureMappings(config, modelDesc);
    
            log.info("Validating schema for model '" + modelDesc.name + "'");
    
            SchemaValidator validator = new SchemaValidator(config);
            try {
                validator.validate();
                
                return;
            } catch(HibernateException e) {
                log.log(
                    Level.FINE,
                    "Schema validation error for model '" 
                    + modelDesc.name + "'",
                    e);
            }
            
            log.info("Updating schema for model '" + modelDesc.name + "'");
            
            SchemaUpdate update = new SchemaUpdate(config);
            update.execute(false, true);
            
            List<?> exceptions = update.getExceptions();
            if (exceptions != null && !exceptions.isEmpty()) {
                logDdlExceptions(
                    exceptions, Level.SEVERE, "schema update error");

                throw new EnkiCreationFailedException(
                    "Schema update for model '" + modelDesc.name + 
                    "' failed (see log for errors)");
            }
            
            config = newConfiguration(false);
            configureIndexMappings(config, modelDesc);
            
            log.info("Updating indexes for model '" + modelDesc.name + "'");
            
            SchemaExport export = new SchemaExport(config);
            // execute params are:
            //   script:     false (don't write DDL to stdout)
            //   export:     true  (send DDL to DB)
            //   justDrop:   true  (run only drop statements)
            //   justCreate: false (don't run create statements)
            export.execute(false, true, true, false);
            
            exceptions = export.getExceptions();
            if (exceptions != null && !exceptions.isEmpty()) {
                // Log drop errors, but don't abort (they'll always fail if
                // this is the initial schema creation).
                logDdlExceptions(exceptions, Level.FINE, "index drop error");
            }

            // execute params are:
            //   script:     false (don't write DDL to stdout)
            //   export:     true  (send DDL to DB)
            //   justDrop:   false (don't run drop statements)
            //   justCreate: true  (run only create statements)
            export.execute(false, true, false, true);
            
            exceptions = export.getExceptions();
            if (exceptions != null && !exceptions.isEmpty()) {
                logDdlExceptions(
                    exceptions, Level.SEVERE, "index create error");
                throw new EnkiCreationFailedException(
                    "Index creation for model '" + modelDesc.name + 
                    "' failed (see log for errors)");
            }
        }
        finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(
                    contextClassLoader);                
            }
        }            
    }

    private void logDdlExceptions(List<?> exceptions, Level level, String msg)
    {
        int i = 1;
        for(Object o: exceptions) {
            Throwable t = (Throwable)o;
            log.log(
                level,
                msg + " (" + i++ + " of " + exceptions.size() + ")",
                t);
        }
    }
    
    private void dropModelStorage(ModelDescriptor modelDesc)
        throws EnkiDropFailedException
    {
        ClassLoader contextClassLoader = null;
        if (classLoader != null) {
            contextClassLoader = 
                Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        try {
            Configuration config = newConfiguration(false);
            
            configureMappings(config, modelDesc);
    
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
        finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(
                    contextClassLoader);                
            }
        }            
    }
    
    private void configureMappings(
        Configuration config, ModelDescriptor modelDesc)
    {
        URL mappingUrl = getModelMappingUrl(modelDesc);        
        config.addURL(mappingUrl);

        for(ModelPluginDescriptor pluginDesc: modelDesc.plugins) {
            URL pluginMappingUrl = getModelMappingUrl(pluginDesc);
            config.addURL(pluginMappingUrl);
        }
    }
    
    private void configureIndexMappings(
        Configuration config, ModelDescriptor modelDesc)
    {
        URL indexMappingUrl = getIndexMappingUrl(modelDesc);
        config.addURL(indexMappingUrl);
    }
    
    private URL getModelMappingUrl(AbstractModelDescriptor modelDesc)
    {
        return getMappingUrl(modelDesc, false);
    }
    
    private URL getIndexMappingUrl(AbstractModelDescriptor modelDesc)
    {
        return getMappingUrl(modelDesc, true);
    }
    
    private URL getMappingUrl(
        AbstractModelDescriptor modelDesc, boolean getIndexMapping)
    {
        if (modelDesc.mappingUrl == null) {
            String configUrlStr = 
                modelDesc.properties.getProperty(
                    MDRepositoryFactory.PROPERTY_ENKI_RUNTIME_CONFIG_URL);
            
            log.config(
                "Model" 
                + (modelDesc.isPlugin() ? " Plugin" : "")
                + ": "
                + modelDesc.name
                + ", Mapping URL: "
                + configUrlStr);
            
            URL configUrl;
            try {
                configUrl = new URL(configUrlStr);
            } catch(MalformedURLException e) {
                throw new ProviderInstantiationException(
                    "Cannot parse configuration URL", e);
            }
            
            URL mappingUrl;
            try {
                mappingUrl = new URL(configUrl, MAPPING_XML);
            } catch (MalformedURLException e) {
                throw new ProviderInstantiationException(
                    "Cannot parse mapping URL", e);
            }
    
            URL indexMappingUrl;
            try {
                indexMappingUrl = new URL(configUrl, INDEX_MAPPING_XML);
            } catch (MalformedURLException e) {
                throw new ProviderInstantiationException(
                    "Cannot parse index mapping URL", e);
            }
            
            modelDesc.mappingUrl = mappingUrl;
            modelDesc.indexMappingUrl = indexMappingUrl;
        }
        
        if (getIndexMapping) {
            return modelDesc.indexMappingUrl;
        } else {
            return modelDesc.mappingUrl;
        }
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
            
            if (isPlugin(modelProperties)) {
                String pluginName = 
                    modelProperties.getProperty(PROPERTY_MODEL_INITIALIZER);
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

                Class<?> cls;
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
                    
                Constructor<? extends RefPackage> topLevelPkgCons;
                try {
                    topLevelPkgCons = 
                        topLevelPkgCls.getConstructor(RefPackage.class);
                } catch (NoSuchMethodException e) {
                    throw new ProviderInstantiationException(
                        "Cannot find constructor for top-level package class '"
                        + topLevelPkgCls.getName() + "'",
                        e);
                }
                
                ModelDescriptor modelDesc =
                    new ModelDescriptor(
                        name, 
                        topLevelPkgCls,
                        topLevelPkgCons, 
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
        ModelDescriptor mofDesc = 
            isMof ? null : modelMap.get(MOF_EXTENT);
        
        log.info("Initializing Extent Descriptor: " + name);
        
        ExtentDescriptor extentDesc = new ExtentDescriptor(name);
        
        extentDesc.modelDescriptor = mofDesc;
        
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
        extentDesc.builtIn = true;
        
        if (isNew && !isMof) {
            createExtentRecord(extentDesc.name, MOF_EXTENT);
        }
        
        extentMap.put(name, extentDesc);
        
        log.fine("Initialized Extent Descriptor: " + name);
    }

    private MetamodelInitializer getInitializer(
        AbstractModelDescriptor modelDesc)
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

    private void logStack(Level level, String msg)
    {
        logStack(level, msg, -1);
    }
    
    private void logStack(Level level, String msg, int sessionId)
    {
        Throwable t = null;
        if (log.isLoggable(Level.FINEST)) {
            t = new RuntimeException("SHOW STACK");
        }
     
        if (sessionId != -1) {
            StringBuilder b = new StringBuilder(msg);
            b.append(" (id: ").append(sessionId).append(')');
            msg = b.toString();
        }
        
        log.log(level, msg, t);
    }
    
    private static boolean isPlugin(Properties modelProps)
    {
        return Boolean.parseBoolean(
            modelProps.getProperty(PROPERTY_MODEL_PLUGIN, "false"));
    }
    
    private void startPeriodicStats()
    {
        if (periodicStatsInterval <= 0) {
            return;
        }
        
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        
        long delay = (long)periodicStatsInterval * 1000L;
        
        periodicStatsTimer = new Timer("Enki Hibernate Stats Timer", true);
        periodicStatsTimer.schedule(
            new TimerTask() {
                @Override
                public void run()
                {
                    logPeriodicStats();
                }                
            }, 
            delay,
            delay);
    }
    
    private void logPeriodicStats()
    {
        Statistics stats = sessionFactory.getStatistics();
        
        stats.logSummary();
        
        StringBuilder b = new StringBuilder();
        for(String cacheRegion: stats.getSecondLevelCacheRegionNames()) {
            SecondLevelCacheStatistics cacheStats = 
                stats.getSecondLevelCacheStatistics(cacheRegion);
            b.setLength(0);
            b
                .append("stats: cache region: ")
                .append(cacheRegion)
                .append(": elements: ")
                .append(cacheStats.getElementCountInMemory());
            if (logMemStats) {
                b
                    .append(" size in memory: ")
                    .append(cacheStats.getSizeInMemory());
            }
            log.info(b.toString());
        }
    }
    
    private void stopPeriodicStats()
    {
        if (periodicStatsTimer == null) {
            return;
        }
        
        periodicStatsTimer.cancel();
    }
    
    protected static abstract class AbstractModelDescriptor
    {
        protected final String name;
        protected final Properties properties;        
        protected URL mappingUrl;
        protected URL indexMappingUrl;
        
        protected AbstractModelDescriptor(String name, Properties properties)
        {
            this.name = name;
            this.properties = properties;
        }
        
        protected abstract boolean isPlugin();
    }
    
    /**
     * ModelDescriptor describes a meta-model.
     */
    protected static class ModelDescriptor extends AbstractModelDescriptor
    {
        protected final Class<? extends RefPackage> topLevelPkgCls;
        protected final Constructor<? extends RefPackage> topLevelPkgCons;
        protected final List<ModelPluginDescriptor> plugins;
        
        protected ModelDescriptor(
            String name,
            Class<? extends RefPackage> topLevelPkgCls,
            Constructor<? extends RefPackage> topLevelPkgCons,
            Properties properties)
        {
            super(name, properties);
            this.topLevelPkgCls = topLevelPkgCls;
            this.topLevelPkgCons = topLevelPkgCons;
            this.plugins = new ArrayList<ModelPluginDescriptor>();
        }
        
        protected boolean isPlugin()
        {
            return false;
        }
    }    
    
    /**
     * ModelPluginDescriptor describes a meta-model plug-in
     */
    protected static class ModelPluginDescriptor extends AbstractModelDescriptor
    {
        protected ModelPluginDescriptor(String name, Properties properties)
        {
            super(name, properties);
        }
        
        protected boolean isPlugin()
        {
            return true;
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
        private List<MetamodelInitializer> pluginInitializers;
        private boolean builtIn;
        
        private ExtentDescriptor(String name)
        {
            this.name = name;
            this.pluginInitializers = new ArrayList<MetamodelInitializer>();
        }
    }
    
    private class MdrSession implements EnkiMDSession
    {
        private Session session;
        private boolean containsWrites;
        private boolean isImplicit;
        private int refCount;
        private final int sessionId;
        
        private final LinkedList<Context> context;
        private final Map<HibernateRefClass, Collection<?>> allOfTypeCache;
        private final Map<HibernateRefClass, Collection<?>> allOfClassCache;
        private final Map<Long, SoftReference<RefBaseObject>> byMofIdCache;
        private final Set<Long> mofIdDeleteSet;
        private final Map<Long, Class<? extends RefObject>> mofIdCreateMap;
        
        private final List<MDRChangeEvent> queuedEvents;

        private MdrSession(Session session, boolean isImplicit, int sessionId)
        {
            this.session = session;
            this.context = new LinkedList<Context>();
            this.allOfTypeCache = 
                new HashMap<HibernateRefClass, Collection<?>>();
            this.allOfClassCache =
                new HashMap<HibernateRefClass, Collection<?>>();
            this.byMofIdCache =
                new HashMap<Long, SoftReference<RefBaseObject>>();
            this.mofIdDeleteSet = new HashSet<Long>();
            this.mofIdCreateMap = 
                new HashMap<Long, Class<? extends RefObject>>();
            this.containsWrites = false;
            this.queuedEvents = new LinkedList<MDRChangeEvent>();
            this.isImplicit = isImplicit;
            this.refCount = 0;
            this.sessionId = sessionId;
        }
        
        private MofIdGenerator getMofIdGenerator()
        {
            return HibernateMDRepository.this.mofIdGenerator;
        }
        
        private HibernateMDRepository getRepos()
        {
            return HibernateMDRepository.this;
        }
        
        public void reset()
        {
            allOfTypeCache.clear();
            allOfClassCache.clear();
            byMofIdCache.clear();
            mofIdDeleteSet.clear();
            mofIdCreateMap.clear();
        }
        
        public void close()
        {
            if (!context.isEmpty()) {
                throw new InternalMdrError("open txns on session");
            }
            context.clear();
            
            if (!queuedEvents.isEmpty()) {
                throw new InternalMdrError("unfired events on session");
            }
            queuedEvents.clear();

            reset();
            
            session.close();
            session = null;
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
        
        public MdrSession peek(HibernateMDRepository repos)
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
     * ModelPropertiesComparator sorts model {@link Properties} objects by
     * their plug-in flag.  All non-plug-in model property sets come first, 
     * followed by those for plug-in models.  The relative ordering of the
     * two groups remains stable if the sorting algorithm is stable.
     */
    private static class ModelPropertiesComparator
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
}

// End HibernateMDRepository.java
