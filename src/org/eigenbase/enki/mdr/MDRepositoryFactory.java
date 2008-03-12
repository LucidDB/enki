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
package org.eigenbase.enki.mdr;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.netbeans.*;
import org.netbeans.mdr.*;
import org.netbeans.mdr.handlers.*;
import org.netbeans.mdr.persistence.btreeimpl.btreestorage.*;

/**
 * MDRepositoryFactory is a factory class for {@link EnkiMDRepository}
 * instances.
 * 
 * <p>The storage properties given to the factory method are used to determine
 * which provider to instantiate and to configure the provider itself.  The
 * storage properties must contain the a property with the key
 * {@value #ENKI_IMPL_TYPE} and the name of an Enki MDR Provider (as defined 
 * in {@link MdrProvider}.  All other storage properties are specific to the 
 * provider.
 *  
 * @author Stephan Zuercher
 */
public class MDRepositoryFactory
{
    public static final String PROPERTY_ENKI_RUNTIME_CONFIG_URL = 
        "enki.runtime.config.url";
    
    public static final String META_INF_DIR_NAME = "META-INF";
    
    public static final String ENKI_DIR_NAME = "enki";
    
    public static final String META_INF_ENKI_DIR = 
        META_INF_DIR_NAME + "/" + ENKI_DIR_NAME;

    public static final String ENKI_IMPL_TYPE = 
        "org.eigenbase.enki.implementationType";
    
    public static final String PROPERTY_ENKI_IMPLEMENTATION = 
        "enki.implementation";

    public static final String PROPERTY_ENKI_EXTENT = "enki.extent";

    public static final String PROPERTY_ENKI_TOP_LEVEL_PKG = 
        "enki.topLevelPkg";
    
    private static final String SYS_PROPERTY_ENKI_IGNORE_EXTENT = 
        "enki.ignore.extent";

    public static final String NETBEANS_MDR_CLASS_NAME_PROP = 
        "org.netbeans.mdr.storagemodel.StorageFactoryClassName";
    
    public static final String NETBEANS_MDR_STORAGE_PROP_PREFIX = 
        "MDRStorageProperty.";

    private static final Logger log = 
        Logger.getLogger(MDRepositoryFactory.class.getName());

    private static ClassLoaderProvider classLoaderProvider;
    
    private MDRepositoryFactory()
    {
    }
    
    /**
     * Constructs a new {@link EnkiMDRepository} from the given storage
     * properties.
     * 
     * @param storageProps storage properties (see {@link MDRepositoryFactory}.
     * @return a new EnkiMDRepostiory
     * @throws UnknownProviderException if the provider is not known
     * @throws ProviderInstantiationException if the provider cannot be
     *                                        instantiated
     */
    public static EnkiMDRepository newMDRepository(Properties storageProps)
    {
        String providerName = storageProps.getProperty(ENKI_IMPL_TYPE);
        MdrProvider provider = 
            MdrProvider.valueOf(providerName.toUpperCase(Locale.US));
        if (provider == null) {
            throw new UnknownProviderException(
                "unknown provider: " + providerName);
        }

        // Special case for delegation to Netbeans MDR implementation.
        if (provider == MdrProvider.NETBEANS_MDR) {
            return newNetbeansMDRepository(storageProps);
        }

        ClassLoader classLoader;
        if (classLoaderProvider != null) {
            classLoader = classLoaderProvider.getClassLoader();
        } else {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        
        List<Properties> modelProperties;
        try {
            modelProperties = loadRepositoryProperties(provider, classLoader);
        } catch (IOException e) {
            throw new ProviderInstantiationException(
                "Could not load model properties for " + provider, e);
        }
        
        switch(provider) {
        case ENKI_HIBERNATE:
            return newHibernateRepository(
                storageProps, modelProperties, classLoader);
            
        default:
            throw new UnknownProviderException(
                "unimplemented provider: " + provider.toString());
        }
    }

    private static EnkiMDRepository newNetbeansMDRepository(
        Properties storageProps)
    {
        // TODO: move these brains to the netbeans package 
        
        if (classLoaderProvider != null) {
            BaseObjectHandler.setClassLoaderProvider(classLoaderProvider);
        }
        
        Properties sysProps = System.getProperties();
        
        Map<Object, Object> savedProps = new HashMap<Object, Object>();

        String storagePrefix = NETBEANS_MDR_STORAGE_PROP_PREFIX;

        // may be specified as a property
        String storageFactoryClassName = 
            storageProps.getProperty(NETBEANS_MDR_CLASS_NAME_PROP);

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
        for(Object key: storageProps.keySet()) {
            String propName = applyPrefix(storagePrefix, key.toString());
            savedProps.put(propName, sysProps.get(propName));
        }

        try {
            // set desired properties
            sysProps.put(
                NETBEANS_MDR_CLASS_NAME_PROP, storageFactoryClassName);
            for (Map.Entry<Object, Object> entry : storageProps.entrySet()) {
                sysProps.put(
                    applyPrefix(
                        storagePrefix,
                        entry.getKey().toString()),
                    entry.getValue());
            }

            // load repository
            return new NBMDRepositoryWrapper(new NBMDRepositoryImpl());
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
    
    private static EnkiMDRepository newHibernateRepository(
        Properties storageProps, 
        List<Properties> modelProperties,
        ClassLoader classLoader)
    {
        return new HibernateMDRepository(
            modelProperties, storageProps, classLoader);
    }

    private static List<Properties> loadRepositoryProperties(
        MdrProvider provider, ClassLoader classLoader) 
    throws IOException
    {
        ArrayList<Properties> modelProps = new ArrayList<Properties>();
        
        Enumeration<URL> configUrls = 
            classLoader.getResources(
                META_INF_ENKI_DIR + "/" + 
                HibernateMDRepository.CONFIG_PROPERTIES);
        
        String ignoreExtent = 
            System.getProperty(SYS_PROPERTY_ENKI_IGNORE_EXTENT);
        
        while(configUrls.hasMoreElements()) {
            URL configUrl = configUrls.nextElement();
            
            log.info("Config URL: " + configUrl.toString());

            Properties props = new Properties();
            props.load(configUrl.openStream());
        
            if (ignoreExtent!= null &&
                ignoreExtent.equals(props.get(PROPERTY_ENKI_EXTENT)))
            {
                log.info("Ignore Config URL: " + configUrl.toString());
                continue;
            }
            
            assert(!props.containsKey(PROPERTY_ENKI_RUNTIME_CONFIG_URL));
            
            props.setProperty(
                PROPERTY_ENKI_RUNTIME_CONFIG_URL, configUrl.toString());
            
            String providerName = 
                props.getProperty(PROPERTY_ENKI_IMPLEMENTATION);
            if (MdrProvider.valueOf(providerName) == provider) {
                modelProps.add(props);
            }
        }
        
        if (modelProps.isEmpty()) {
            log.warning("No model config resources found.");
        }
        
        return modelProps;
    }
    
    private static String applyPrefix(String prefix, String propertyName)
    {
        if (propertyName.startsWith(prefix)) {
            return propertyName;
        }
        
        return prefix + propertyName;
    }
    
    public static void setClassLoaderProvider(ClassLoaderProvider provider)
    {
        classLoaderProvider = provider;
    }
}

// End MDRepositoryFactory.java
