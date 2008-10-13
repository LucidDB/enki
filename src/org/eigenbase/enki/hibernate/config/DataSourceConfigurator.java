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
package org.eigenbase.enki.hibernate.config;

import java.sql.*;
import java.util.*;
import java.util.logging.*;

import javax.naming.*;
import javax.sql.*;

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.impl.*;
import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;
import org.hibernate.cfg.*;

/**
 * DataSourceConfigurator manages the configuration of Enki/Hibernate's
 * {@link DataSource} object.  It manages the use (and possibly configuration)
 * of a JNDI provider and automatically binds a locally create DataSource if
 * one is not provided externally.
 * 
 * @author Stephan Zuercher
 */
public class DataSourceConfigurator
{
    /**
     * Hibernate configuration property for JNDI initial context factory.
     */
    private static final String HIBERNATE_JNDI_CLASS = Environment.JNDI_CLASS;

    /**
     * Hibernate configuration property for JNDI provider URL.
     */
    private static final String HIBERNATE_JNDI_URL = Environment.JNDI_URL;

    /**
     * Hibernate configuration property prefix for generic JNDI properties.
     * Guaranteed to end with a period.
     */
    private static final String HIBERNATE_JNDI_PREFIX;
    static {
        String prefix = Environment.JNDI_PREFIX;
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }
        HIBERNATE_JNDI_PREFIX = prefix;
    }
    
    /**
     * These Hibernate properties are removed from the given storage properties
     * so they do not interfere with our configuration of Hibernate.  Note that
     * some properties, like the hibernate.connection.release_mode, are left 
     * unchanged.
     */
    private static final List<String> FILTERED_HIBERNATE_CONNECTION_PROPS =
        Collections.unmodifiableList(
            Arrays.asList(
                new String[] {
                    "hibernate.connection.driver_class",
                    "hibernate.connection.url",
                    "hibernate.connection.username",
                    "hibernate.connection.password",
                    "hibernate.connection.pool_size",
                }));
    
    private static final Logger log = 
        Logger.getLogger(DataSourceConfigurator.class.getName());
    
    private final Properties storageProperties;
    private GenericObjectPool connectionPool;
    
    /**
     * Constructs a DataSourceConfigurator.  Warning: the 
     * {@link #initDataSource()} method modified these storage properties.
     * 
     * @param storageProperties storage properties that define the data source
     */
    public DataSourceConfigurator(Properties storageProperties)
    {
        this.storageProperties = storageProperties;
    }
    

    /**
     * Initializes the configured data source as necessary.  Warning: this 
     * method modifies the storage properties given in the constructor.  In
     * particular, JNDI storage properties may be set if no JNDI provider
     * is found and <code>hibernate.connection</code> and 
     * <code>hibernate.jndi</code> properties are deleted or replaced.
     * 
     * @see JndiUtil#initJndi(Properties)
     */
    public void initDataSource()
    {
        if (connectionPool != null) {
            throw new HibernateException("data source already initialized");
        }
        
        JndiUtil jndiUtil = newJndiUtil();
        try {
            // First check if JNDI is configured
            boolean internalJndi = jndiUtil.initJndi(storageProperties);
            
            String datasourceName = getDataSourceName();
            String driverClass = getDriverClass();
            String jdbcUrl = getJdbcUrl();
            
            InitialContext context = 
                jndiUtil.newInitialContext(storageProperties);
            
            log.fine(
                "Datasource Name: " + 
                datasourceName + 
                "; self-configured JNDI: " + 
                (internalJndi ? "yes" : "no"));

            DataSource datasource;
            try {
                datasource =
                    JndiUtil.lookup(
                        context, datasourceName, DataSource.class);
            } catch(NamingException e) {
                log.log(
                    Level.WARNING, 
                    "Could not find datasource '" + datasourceName + "'", 
                    e);
                dumpProperties("JNDI props during error: ", storageProperties);

                datasource = null;
            }
            
            if (datasource == null) {
                if (driverClass == null || jdbcUrl == null) {
                    // We assume hibernate has been configured via its own 
                    // properties.
                    log.fine("No Enki/Hibernate data source configured");
                    return;
                }
            
                datasource = createDataSource();

                JndiUtil.bind(context, datasourceName, datasource);
            }
            
            // Force removal of hibernate connection properties that may 
            // interfere with our data source.
            for(String prop: FILTERED_HIBERNATE_CONNECTION_PROPS) {
                storageProperties.remove(prop);
            }

            // Force removal of hibernate JNDI properties that may interfere
            // with our data source
            Iterator<Object> iter = storageProperties.keySet().iterator();
            while(iter.hasNext()) {
                String key = (String)iter.next();
                
                if (key.startsWith(HIBERNATE_JNDI_PREFIX)) {
                    iter.remove();
                }
            }
        
            // Apply our settings to Hibernate's configuration properties.
            storageProperties.setProperty(
                "hibernate.connection.datasource", datasourceName);
            
            String initial = 
                jndiUtil.getInitialContextFactoryName(storageProperties);
            if (initial != null) {
                storageProperties.setProperty(HIBERNATE_JNDI_CLASS, initial);
            }
            
            String provider = jndiUtil.getProviderUrl(storageProperties);
            if (provider != null) {
                storageProperties.setProperty(HIBERNATE_JNDI_URL, provider);
            }
            
            Properties jndiProps =
                jndiUtil.getJndiProperties(storageProperties, false);
            iter = jndiProps.keySet().iterator();
            while(iter.hasNext()) {
                String key = (String)iter.next();
                
                storageProperties.setProperty(
                    HIBERNATE_JNDI_PREFIX + key, jndiProps.getProperty(key));
            }
            
            dumpProperties("final storage props: ", storageProperties);
        }
        catch(NamingException e) {
            throw new EnkiHibernateException(e);
        }
    }

    public Properties getStorageProperties()
    {
        return storageProperties;
    }
    
    public void close()
    {
        if (connectionPool == null) {
            return;
        }
        
        String datasourceName = 
            getDataSourceName();

        JndiUtil jndiUtil = newJndiUtil();
        
        try {
            dumpProperties("close storage props:", storageProperties);
            
            InitialContext context = 
                jndiUtil.newInitialContext(storageProperties);
            
            JndiUtil.unbind(context, datasourceName);
        } catch(NamingException e) {
            log.log(Level.SEVERE, "Unable to unbind data source", e);
        } finally {
            try {
                connectionPool.close();
            } catch(Exception e) {
                log.log(Level.SEVERE, "Unable to close connection pool", e);
            } finally {
                connectionPool = null;
            }
        }
    }
    
    private JndiUtil newJndiUtil()
    {
        return new JndiUtil(
            HibernateMDRepository.PROPERTY_STORAGE_JNDI_PREFIX,
            HibernateMDRepository.PROPERTY_STORAGE_JNDI_INITIAL_CONTEXT_FACTORY_CLASS,
            HibernateMDRepository.PROPERTY_STORAGE_JNDI_PROVIDER_URL);
    }
    
    private DataSource createDataSource()
    {
        String driverClass = getDriverClass();
        String jdbcUrl = getJdbcUrl();
        String username = 
            readStorageProperty(
                HibernateMDRepository.PROPERTY_STORAGE_CONNECTION_USERNAME,
                "",
                String.class);
        String password = 
            readStorageProperty(
                HibernateMDRepository.PROPERTY_STORAGE_CONNECTION_PASSWORD,
                "",
                String.class);
        int maxIdle =
            readStorageProperty(
                HibernateMDRepository.PROPERTY_STORAGE_CONNECTION_MAX_IDLE,
                1,
                Integer.class);
        if (maxIdle < 1) {
            maxIdle = 1;
        }
        
        try {
            Class.forName(driverClass);
        } catch(ClassNotFoundException e) {
            throw new ProviderInstantiationException(
                "Cannot find JDBC driver class", e);
        }
        
        // Don't use DBCP's DriverManagerConnectionFactory: DriverManager's
        // methods are synchronized and deadlock can result (if Enki is used
        // inside a JDBC Driver, such as Farrago's engine driver).  Here we
        // touch DriverManager once (during startup, which should be be
        // externally synchronized) and then never again.
        Driver driver;
        try {
            driver = DriverManager.getDriver(jdbcUrl);
        } catch(SQLException e) {
            throw new ProviderInstantiationException(
                "Cannot find JDBC driver", e);
        }
        
        Properties connProps = new Properties();
        connProps.setProperty("user", username);
        connProps.setProperty("password", password);
        
        ConnectionFactory connectionFactory =
            new DriverConnectionFactory(
                driver, jdbcUrl, connProps);

        connectionPool = new GenericObjectPool();
        connectionPool.setWhenExhaustedAction(
            GenericObjectPool.WHEN_EXHAUSTED_GROW);
        connectionPool.setMaxActive(-1);        
        connectionPool.setMaxIdle(maxIdle);
        
        new PoolableConnectionFactory(
            connectionFactory,
            connectionPool,
            null,
            null,
            false,
            false);

        return new PoolingDataSource(connectionPool);
    }
    
    private String getDataSourceName()
    {
        return readStorageProperty(
            HibernateMDRepository.PROPERTY_STORAGE_CONNECTION_DATASOURCE, 
            HibernateMDRepository.PROPERTY_STORAGE_DEFAULT_CONNECTION_DATASOURCE, 
            String.class);
    }

    private String getDriverClass()
    {
        return readStorageProperty(
            HibernateMDRepository.PROPERTY_STORAGE_CONNECTION_DRIVER_CLASS, 
            null, 
            String.class);
    }

    private String getJdbcUrl()
    {
        return readStorageProperty(
            HibernateMDRepository.PROPERTY_STORAGE_CONNECTION_URL,
            null,
            String.class);
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
    
    private void dumpProperties(String prefix, Properties props)
    {
        dumpProperties(Level.FINER, prefix, props);
    }
    
    private void dumpProperties(Level level, String prefix, Properties props)
    {
        if (!log.isLoggable(level)) {
            return;
        }
        
        StringBuilder b = new StringBuilder();
        for(Object key: props.keySet()) {
            if (b.length() > 0) {
                b.append('\n');
            }
            b.append(prefix + key + " = " + props.get(key));
        }
        
        log.log(level, b.toString());
    }
}

// End DataSourceConfigurator.java
