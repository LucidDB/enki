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
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;
import javax.naming.*;
import javax.sql.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.config.*;
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
import org.hibernate.dialect.*;
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
    public static final String CONFIG_PROPERTIES = "configurator.properties";

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
     * Configuration file property that indicates whether this model is a 
     * plug-in or a base model.  Valid values are true or false.
     */
    public static final String PROPERTY_MODEL_PLUGIN = 
        "enki.model.plugin";

    /**
     * Configuration file property that specifies the table name prefix for
     * a given model's schema.
     */
    public static final String PROPERTY_MODEL_TABLE_PREFIX =
        "enki.model.tablePrefix";
    
    /**
     * Storage property that configures the JNDI name of the {@link DataSource}
     * that Enki/Hibernate should use.  If no DataSource exists with this
     * name, Enki/Hibernate will attempt to create and bind one using the
     * {@link #PROPERTY_STORAGE_CONNECTION_DRIVER_CLASS}, 
     * {@link #PROPERTY_STORAGE_CONNECTION_URL}, 
     * {@link #PROPERTY_STORAGE_CONNECTION_USERNAME},and 
     * {@link #PROPERTY_STORAGE_CONNECTION_PASSWORD} properties. If the driver
     * class and URLproperties are not set, Enki/Hibernate will assume that 
     * Hibernate-specific properties have been used to configure a database
     * connection.
     * 
     * Note that in the event that Enki/Hibernate creates and binds its own
     * DataSource, following Hibernate properties will be modified or cleared:
     * <ul>
     * <li>hibernate.connection.datasource</li>
     * <li>hibernate.connection.driver_class</li>
     * <li>hibernate.connection.url</li>
     * <li>hibernate.connection.username</li>
     * <li>hibernate.connection.password</li>
     * <li>hibernate.connection.pool_size</li>
     * </ul>
     */
    public static final String PROPERTY_STORAGE_CONNECTION_DATASOURCE =
        "org.eigenbase.enki.hibernate.connection.datasource";
    
    /**
     * The default value for the 
     * {@link #PROPERTY_STORAGE_CONNECTION_DATASOURCE} property is {@value}.
     */
    public static final String PROPERTY_STORAGE_DEFAULT_CONNECTION_DATASOURCE =
        "java:ENKI_DATASOURCE";
    
    /**
     * Storage property containing the name of JDBC driver class to use for 
     * creating a {@link DataSource} object.  
     * See {@link #PROPERTY_STORAGE_CONNECTION_DATASOURCE}.
     */
    public static final String PROPERTY_STORAGE_CONNECTION_DRIVER_CLASS =
        "org.eigenbase.enki.hibernate.connection.driver_class";

    /**
     * Storage property containing the name of JDBC URL to use for 
     * creating a {@link DataSource} object.  
     * See {@link #PROPERTY_STORAGE_CONNECTION_DATASOURCE}.
     */
    public static final String PROPERTY_STORAGE_CONNECTION_URL =
        "org.eigenbase.enki.hibernate.connection.url";

    /**
     * Storage property containing the name of JDBC username to use for 
     * creating a {@link DataSource} object.  
     * See {@link #PROPERTY_STORAGE_CONNECTION_DATASOURCE}.
     */
    public static final String PROPERTY_STORAGE_CONNECTION_USERNAME =
        "org.eigenbase.enki.hibernate.connection.username";

    /**
     * Storage property containing the name of JDBC password to use for 
     * creating a {@link DataSource} object.  
     * See {@link #PROPERTY_STORAGE_CONNECTION_DATASOURCE}.
     */
    public static final String PROPERTY_STORAGE_CONNECTION_PASSWORD =
        "org.eigenbase.enki.hibernate.connection.password";

    /**
     * Storage property containing the maximum number of idle connections to
     * keep open if Enki/Hibernate constructs its own DataSource.
     * See {@link #PROPERTY_STORAGE_CONNECTION_DATASOURCE}.
     */
    public static final String PROPERTY_STORAGE_CONNECTION_MAX_IDLE =
        "org.eigenbase.enki.hibernate.connection.max_idle";

    /**
     * Storage property JNDI prefix.  When creating a {@link DataSource} any 
     * storage properties starting with this string are used to construct
     * a JNDI {@link InitialContext}.  These properties (excluding both
     * {@link #PROPERTY_STORAGE_JNDI_INITIAL_CONTEXT_FACTORY_CLASS} and
     * {@link #PROPERTY_STORAGE_JNDI_PROVIDER_URL}) are passed to the
     * InitialContext constructor with the prefix stripped.
     * 
     * Note: The prefix value does not contain the substring "hibernate.jndi"
     * because Hibernate 3.1 mistakenly uses those values, while incorrectly 
     * assuming that "hibernate.jndi" appears at the start of the string.
     */
    public static final String PROPERTY_STORAGE_JNDI_PREFIX =
        "org.eigenbase.enki.hibernatejndi.";
    
    /**
     * Storage property specifying the name of the JNDI initial context factory
     * class.  Passed to the JNDI {@link InitialContext} constructor using
     * the property name {@value javax.naming.Context#INITIAL_CONTEXT_FACTORY}.
     */
    public static final String PROPERTY_STORAGE_JNDI_INITIAL_CONTEXT_FACTORY_CLASS =
        PROPERTY_STORAGE_JNDI_PREFIX  + "initial_context_factory_class";

    /**
     * Storage property specifying the name of the JNDI provider URL. Passed 
     * to the JNDI {@link InitialContext} constructor using the property name 
     * {@value javax.naming.Context#PROVIDER_URL}.
     */
    public static final String PROPERTY_STORAGE_JNDI_PROVIDER_URL =
        PROPERTY_STORAGE_JNDI_PREFIX  + "provider_url";

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
    public static final String PROPERTY_STORAGE_PERIODIC_STATS =
        "org.eigenbase.enki.hibernate.periodicStats";
    
    /**
     * Contains the default value for {@link #PROPERTY_STORAGE_PERIODIC_STATS}
     * storage property.  The default is {@value}.
     */
    public static final int DEFAULT_PERIODIC_STATS_INTERVAL = 0;
    
    /**
     * Storage property that configures periodic session factory statistics
     * logging to include information about the size (in bytes) of Hibernate
     * caches.  Computing cache memory size requires serializing all cached
     * objects, which can be very slow.  Valid values are true or false.
     * Defaults to {@link #DEFAULT_MEM_STATS}. 
     * {@link #PROPERTY_STORAGE_PERIODIC_STATS} must be enabled for memory 
     * statistics logging to occur.
     */
    public static final String PROPERTY_STORAGE_MEM_STATS =
        "org.eigenbase.enki.hibernate.periodicStats.memStats";
    
    /**
     * Contains the default value of {@link #PROPERTY_STORAGE_MEM_STATS}.
     * The default is {@value}.
     */
    public static final boolean DEFAULT_MEM_STATS = false;
    
    /**
     * Storage property that controls whether Enki/Hibernate will create the
     * necessary schema in its database.  The following values are allowed:
     * 
     * <table>
     * <tr>
     *   <td>Value</td>
     *   <td>Description</td>
     * </tr>
     * <tr>
     *   <td>{@value #CREATE_SCHEMA_AUTO}</td>
     *   <td>Enki/Hibernate will validate the schema and then create or update 
     *       it as necessary to match its model definitions.</td>
     * </tr>
     * <tr>
     *   <td>{@value #CREATE_SCHEMA_AUTO_VIEW}</td>
     *   <td>Like AUTO, but all-of-class and all-of-type views are created
     *       in addition to the storage tables.</td> 
     * </tr>
     * <tr>
     *   <td>{@value #CREATE_SCHEMA_VIEW}</td>
     *   <td>Causes all-of-class and all-of-type views to be created.</td>
     * </tr>
     * <tr>
     *   <td>{@value #CREATE_SCHEMA_DISABLED}</td>
     *   <td>Enki/Hibernate will validate the schema, and if it does not match
     *       the model definition (extraneous tables and views are ignored), 
     *       it will throw an exception.</td>
     * </tr>
     * <table>
     * 
     * Defaults to {@link #DEFAULT_CREATE_SCHEMA}. 
     */
    public static final String PROPERTY_STORAGE_CREATE_SCHEMA =
        "org.eigenbase.enki.hibernate.createSchema";
    
    public static final String CREATE_SCHEMA_AUTO = "AUTO";
    public static final String CREATE_SCHEMA_AUTO_VIEW = "AUTO_VIEW";
    public static final String CREATE_SCHEMA_VIEW = "VIEW";
    public static final String CREATE_SCHEMA_DISABLED = "DISABLED";

    /**
     * The default value of {@link #PROPERTY_STORAGE_CREATE_SCHEMA}.
     * The default is {@value}.
     */
    public static final String DEFAULT_CREATE_SCHEMA = "DISABLED";
    
    /**
     * Identifier for the built-in MOF extent.
     */
    public static final String MOF_EXTENT = "MOF";
    
    private static final MdrSessionStack sessionStack = new MdrSessionStack();
    
    private final ReadWriteLock txnLock;
    
    private final AtomicInteger sessionCount;
    
    private final AtomicInteger sessionIdGenerator;
    
    /** Storage configuration properties. */
    private final Properties storageProperties;

    /** Configuration generator. */
    private final HibernateConfigurator configurator;
    
    /** Given default class loader, if any. */
    private final ClassLoader classLoader;
    
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
    private final boolean createSchema;
    private final boolean createViews;
    
    private final DataSourceConfigurator dataSourceConfigurator;
    
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
    
    /** The SQL dialect in use by the configured database. */
    private Dialect sqlDialect;
    
    final Logger log;

    private boolean previewDelete;

    public HibernateMDRepository(
        List<Properties> modelProperties,
        Properties storageProperties,
        ClassLoader classLoader)
    {
        this.log = Logger.getLogger(HibernateMDRepository.class.getName());
        this.txnLock = new ReentrantReadWriteLock();
        this.storageProperties = storageProperties;
        this.classLoader = classLoader;
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
                PROPERTY_STORAGE_PERIODIC_STATS, 
                DEFAULT_PERIODIC_STATS_INTERVAL, 
                Integer.class);
        this.logMemStats =
            readStorageProperty(
                PROPERTY_STORAGE_MEM_STATS, 
                DEFAULT_MEM_STATS, 
                Boolean.class);
        
        String createSchemaSetting = 
            readStorageProperty(
                PROPERTY_STORAGE_CREATE_SCHEMA,
                DEFAULT_CREATE_SCHEMA,
                String.class).toUpperCase();
        if (createSchemaSetting.equals(CREATE_SCHEMA_AUTO)) {
            this.createSchema = true;
            this.createViews = false;
        } else if (createSchemaSetting.equals(CREATE_SCHEMA_AUTO_VIEW)) {
            this.createSchema = true;
            this.createViews = true;
        } else if (createSchemaSetting.equals(CREATE_SCHEMA_VIEW)) {
            this.createSchema = false;
            this.createViews = true;
        } else {
            if (!createSchemaSetting.equals(CREATE_SCHEMA_DISABLED)) {
                log.warning(
                    "Value '" + createSchemaSetting + "' for property " 
                    + PROPERTY_STORAGE_CREATE_SCHEMA 
                    + " is invalid; defaulting to " + CREATE_SCHEMA_DISABLED);
            }
            
            this.createSchema = false;
            this.createViews = false;
        }
        
        // Initialize our data source as necessary.
        this.dataSourceConfigurator = 
            new DataSourceConfigurator(storageProperties);
        this.dataSourceConfigurator.initDataSource();
        
        this.configurator = 
            new HibernateConfigurator(storageProperties, modelProperties);
        
        initModelExtent(MOF_EXTENT, false);
        initStorage();
        
        this.sessionCount = new AtomicInteger(0);  
        this.sessionIdGenerator = new AtomicInteger(0);
    }
    
    private <T> T readStorageProperty(
        String name, T defaultValue, Class<T> cls)
    {
        return PropertyUtil.readStorageProperty(
            storageProperties, 
            log,
            name,
            defaultValue,
            cls);
    }
    
    public MdrProvider getProviderType()
    {
        return MdrProvider.ENKI_HIBERNATE;
    }
    
    public EnkiMDSession detachSession()
    {
        if (sessionStack.isEmpty()) {
            return null;
        }
        
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
        // commits/rollsback.  So bar writes nested in explicit reads.
        boolean isCommitter = false;
        boolean beginTrans = false;
        if (contexts.isEmpty()) {
            isCommitter = true;
            beginTrans = true;
        } else {
            isCommitter = contexts.getLast().isImplicit;
        }
        
        if (write && !isCommitter && !isNestedWriteTransaction(mdrSession)) {
            throw new HibernateException(
                "cannot start write transaction within read transaction");
        }
        
        Transaction trans;
        if (beginTrans) {
            if (write) {
                mdrSession.obtainWriteLock();
            } else {
                mdrSession.obtainReadLock();
            }
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
            // If any txn in the nested stack rolled back, then the outermost
            // txn rolls back even if the user requests commit.
            if (rollback || context.forceRollback) {
                contexts.getLast().forceRollback = true;
            }
            
            return mdrSession;
        }
        
        Transaction txn = context.transaction;

        if (context.isWrite) {
            enqueueEndTransEvent(mdrSession);
        }
        
        try {
            // Note that even if "commit" is requested, we'll rollback if no 
            // writing was possible.
            if (rollback || context.forceRollback) {
                txn.rollback();
                
                fireCanceledChanges(mdrSession);
                
                if (mdrSession.containsWrites) {
                    mdrSession.session.clear();
                    mdrSession.reset();
                }
                
                // Throw this after the rollback has happened.
                if (rollback && !context.isWrite) {
                    throw new EnkiHibernateException(
                        "Cannot rollback read transactions");
                }
            } else {
                try {
                    Session session = mdrSession.session;
                    if (mdrSession.containsWrites) {
                        session.flush();
                        
                        if (!mdrSession.mofIdDeleteSet.isEmpty()) {
                            // Update enki type mapping
                            Query query = 
                                session.getNamedQuery("TypeMappingDeleteByMofId");
                            
                            // Do this in chunks or else feel the wrath of
                            // http://opensource.atlassian.com/projects/hibernate/browse/HHH-766
                            int flushSize = typeLookupFlushSize;
                            if (flushSize <= 0) {
                                flushSize = 100;
                            }
                            List<Long> chunk = new ArrayList<Long>();
                            for(Long mofId: mdrSession.mofIdDeleteSet) {
                                chunk.add(mofId);
                                if (chunk.size() >= flushSize) {
                                    query.setParameterList("mofIds", chunk);
                                    query.executeUpdate();
                                    chunk.clear();
                                }
                            }
                            if (!chunk.isEmpty()) {
                                query.setParameterList("mofIds", chunk);
                                query.executeUpdate();                            
                                chunk.clear();
                            }
                            
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
        } finally {
            mdrSession.releaseLock();
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
    
    public Collection<RefObject> getByMofId(List<Long> mofIds, RefClass cls)
    {
        if (mofIds.isEmpty()) {
            return Collections.emptyList();
        } else if (mofIds.size() == 1) {
            RefObject obj = getByMofId(mofIds.get(0), cls);
            if (obj == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(obj);
        }
        
        HibernateRefClass hibRefCls = (HibernateRefClass)cls;

        Class<? extends RefObject> instanceClass = 
            hibRefCls.getInstanceClass();
        
        Criteria criteria = 
            getCurrentSession().createCriteria(instanceClass)
                .add(Restrictions.in("id", mofIds))
                .setCacheable(true);

        MdrSession mdrSession = getMdrSession();
        
        // We use a set here because certain types of objects (those with
        // an attribute of type collection-of-primitive) cause Hibernate to
        // return duplicate results. It performs a left outer join to load
        // the objects with their collections and fails to return each
        // instance only once.  Probably a Hibernate bug.
        
        Set<RefObject> result = new HashSet<RefObject>();
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
    private RefObject getByMofId(Long mofIdLong, RefClass cls, String mofId)
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
        
        Class<? extends RefObject> instanceClass = 
            ((HibernateRefClass)cls).getInstanceClass();

        return getByMofId(mdrSession, mofIdLong, instanceClass);
    }

    private RefObject getByMofId(
        MdrSession mdrSession,
        Long mofIdLong,
        Class<? extends RefObject> instanceClass)
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
        
        Long mofIdLong = MofIdUtil.parseMofIdStr(mofId); 

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
    
    public void delete(Collection<RefObject> objects)
    {
        if (objects.isEmpty()) {
            return;
        }
        
        // TODO: should also check for attribute modifications
        
        MdrSession session = getMdrSession();
        if (!session.mofIdCreateMap.isEmpty() ||
            !session.mofIdDeleteSet.isEmpty())
        {
            throw new EnkiHibernateException(
                "mass deletion API may not be used in transaction with pending modifications");
        }
        
        new HibernateMassDeletionUtil(this).massDelete(objects);
    }

    public void previewRefDelete(RefObject obj)
    {
        previewDelete = true;
        try {
            obj.refDelete();
        } finally {
            previewDelete = false;
        }
    }

    public boolean supportsPreviewRefDelete()
    {
        return true;
    }
    
    public boolean inPreviewDelete()
    {
        return previewDelete;
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

    public Dialect getSqlDialect()
    {
        return sqlDialect;
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
                
                try {
                    sessionFactory.close();
                    sessionFactory = null;
                    
                    classRegistry.clear();
                    assocRegistry.clear();
                } finally {
                    dataSourceConfigurator.close();
                }
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
            MdrSession session = getMdrSession();
            if (!isNestedWriteTransaction(session)) {
                throw new EnkiHibernateException(
                    "Operation requires write transaction");
            }
        } else {
            // Make sure a txn exists.
            getContext();
        }
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
        
        Class<? extends RefObject> instanceClass = cls.getInstanceClass();
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
        if (previewDelete) {
            return;
        }
        
        MdrSession mdrSession = getMdrSession();
        
        long mofId = object.getMofId();

        mdrSession.byMofIdCache.remove(mofId);
        mdrSession.mofIdDeleteSet.add(mofId);
    }

    void recordObjectDeletions(Collection<Long> mofIds)
    {
        MdrSession mdrSession = getMdrSession();
        
        for(Long mofId: mofIds) {
            mdrSession.byMofIdCache.remove(mofId);
            mdrSession.mofIdDeleteSet.add(mofId);
        }
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
        // there are no listeners now.)  For deletion preview, do not
        // enqueue anything, since there's no actual effect to be committed
        // or rolled back.
        if (!previewDelete) {
            mdrSession.queuedEvents.add(event);
        }
        
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
                ModelDescriptor modelDesc = 
                    configurator.getModelMap().get(modelExtentName);
    
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
                        modelDesc = 
                            configurator.getModelMap().get(extentDesc.name);
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

        initModelViews(modelDesc, extentDesc.extent);
        
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
        Configuration config = configurator.newConfiguration(true);

        initProviderStorage(config);
        
        configurator.addModelConfigurations(config);
        
        sessionFactory = config.buildSessionFactory();
        
        startPeriodicStats();
        
        mofIdGenerator = 
            new MofIdGenerator(sessionFactory, config, storageProperties);
        mofIdGenerator.configureTable(createSchema);

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

    private void initProviderStorage(Configuration config)
    {
        SessionFactory tempSessionFactory = config.buildSessionFactory();

        Session session = tempSessionFactory.getCurrentSession();

        boolean exists = false;
        Transaction trans = session.beginTransaction();
        try {
            try {
                DatabaseMetaData dbMetadata = 
                    session.connection().getMetaData();
                this.sqlDialect = 
                    DialectFactory.buildDialect(
                        config.getProperties(),
                        dbMetadata.getDatabaseProductName(),
                        dbMetadata.getDatabaseMajorVersion());
            } catch(Exception e) {
                throw new ProviderInstantiationException(
                    "Unable to determine appropriate SQL dialect", e);
            }

            try {
                // Execute the query
                session.getNamedQuery("AllExtents").list();
                exists = true;
            } catch(HibernateException e) {
                // Presume that table doesn't exist.
                
                // REVIEW: SWZ: 3/12/08: If it's a connection error, and
                // suddenly starts working (startup race?) we could 
                // conceivably destroy the tables, which would be bad.
                log.log(Level.FINE, "Extent Query Error", e);
            }
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
                if (createSchema) {
                    log.log(
                        Level.WARNING, 
                        "Enki Hibernate provider database schema validation failed", 
                        e);
                    requiresUpdate = true;
                } else {
                    throw new ProviderInstantiationException(
                        "Enki Hibernate provider database schema validation failed",
                        e);
                }
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
        } else if (!createSchema) {
            throw new ProviderInstantiationException(
                "Unable to query extents from database: "
                + "invalid schema or bad connection");
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
            Configuration config = 
                configurator.newModelConfiguration(modelDesc, true);
    
            log.info("Validating schema for model '" + modelDesc.name + "'");
    
            SchemaValidator validator = new SchemaValidator(config);
            try {
                validator.validate();
                
                return;
            } catch(HibernateException e) {
                if (createSchema) {
                    log.log(
                        Level.FINE,
                        "Schema validation error for model '" 
                        + modelDesc.name + "'",
                        e);
                } else {
                    throw new ProviderInstantiationException(
                        "Schema validation error for model '" 
                        + modelDesc.name + "'",
                        e);
                }
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
            
            config = 
                configurator.newModelIndexMappingConfiguration(
                    modelDesc, false);
            
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
    
    private void initModelViews(ModelDescriptor modelDesc, RefPackage pkg)
    {
        if (!createViews) {
            return;
        }
        
        CodeGenXmlOutputStringBuilder mappingOutput = 
            new CodeGenXmlOutputStringBuilder();
        
        HibernateViewMappingUtil viewUtil = 
            new HibernateViewMappingUtil(
                mappingOutput,
                new Dialect[][] { { sqlDialect }},
                modelDesc.properties.getProperty(PROPERTY_MODEL_TABLE_PREFIX));

        Set<Classifier> classes = new HashSet<Classifier>();
        LinkedList<RefPackage> pkgs = new LinkedList<RefPackage>();
        pkgs.add(pkg);
        while(!pkgs.isEmpty()) {
            RefPackage p = pkgs.removeFirst();
            
            if (CodeGenUtils.isTransient((MofPackage)p.refMetaObject())) {
                continue;
            }
            
            for(RefClass c: 
                    GenericCollections.asTypedCollection(
                        p.refAllClasses(), RefClass.class))
            {
                classes.add((Classifier)c.refMetaObject());
            }
            Collection<RefPackage> subPkgs = 
                GenericCollections.asTypedCollection(
                    p.refAllPackages(), RefPackage.class);
            pkgs.addAll(subPkgs);
        }
        
        mappingOutput.writeXmlDecl();
        mappingOutput.writeDocType(
            "hibernate-mapping",
            "-//Hibernate/Hibernate Mapping DTD 3.0//EN",
            "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd");
        mappingOutput.newLine();

        try {
            mappingOutput.startElem("hibernate-mapping");
            viewUtil.generateViews(classes);
            mappingOutput.endElem("hibernate-mapping");
        } catch(GenerationException e) {
            throw new HibernateException("Could not generate view mapping", e);
        }
        
        String mapping = mappingOutput.getOutput();
        
        Configuration config = configurator.newConfiguration(false);
        
        config.addXML(mapping);
        
        log.info("Updating views for model '" + modelDesc.name + "'");
        
        SchemaExport export = new SchemaExport(config);
        // execute params are:
        //   script:     false (don't write DDL to stdout)
        //   export:     true  (send DDL to DB)
        //   justDrop:   true  (run only drop statements)
        //   justCreate: false (don't run create statements)
        export.execute(false, true, true, false);
        
        List<?> exceptions = export.getExceptions();
        if (exceptions != null && !exceptions.isEmpty()) {
            // Log drop errors, but don't abort (they'll always fail if
            // this is the initial schema creation).
            logDdlExceptions(exceptions, Level.FINE, "view drop error");
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
            throw new HibernateException(
                "View creation for model '" + modelDesc.name + 
                "' failed (see log for errors)");
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
            Configuration config = 
                configurator.newModelConfiguration(modelDesc, false);
    
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
    
    private void initModelExtent(String name, boolean isNew)
    {
        boolean isMof = name.equals(MOF_EXTENT);

        ModelDescriptor modelDesc = configurator.getModelMap().get(name);
        if (modelDesc == null) {
            throw new InternalMdrError(
                "Unknown metamodel extent '" + name + "'");
        }
        
        ModelDescriptor mofDesc = 
            isMof ? null : configurator.getModelMap().get(MOF_EXTENT);
        
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
        protected boolean builtIn;
        
        public ExtentDescriptor(String name)
        {
            this.name = name;
            this.pluginInitializers = new ArrayList<MetamodelInitializer>();
        }
    }
    
    private class MdrSession implements EnkiMDSession
    {
        private Session session;
        private Lock lock;
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
        
        private void obtainWriteLock()
        {
            if (lock != null) {
                throw new EnkiHibernateException("already locked");
            }
            
            lock = txnLock.writeLock();
            lock.lock();
        }
        
        private void obtainReadLock()
        {
            if (lock != null) {
                throw new EnkiHibernateException("already locked");
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
        
        public boolean isEmpty()
        {
            return tls.get().isEmpty();
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
        private boolean forceRollback;
        
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
            this.forceRollback = false;
        }
    }    
}

// End HibernateMDRepository.java
