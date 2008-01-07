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
import org.netbeans.api.mdr.*;
import org.netbeans.mdr.*;
import org.netbeans.mdr.persistence.btreeimpl.btreestorage.*;

/**
 * MDRepositoryFactory is a factory class for {@link MDRepository}
 * instances.
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
        "enki.top-level-pkg";
    
    private static final String SYS_PROPERTY_ENKI_IGNORE_EXTENT = 
        "enki.ignore.extent";

    private static final String NETBEANS_MDR_CLASS_NAME_PROP = 
        "org.netbeans.mdr.storagemodel.StorageFactoryClassName";
    
    private static final String NETBEANS_MDR_STORAGE_PROP_PREFIX = 
        "MDRStorageProperty.";

    private static final Logger log = 
        Logger.getLogger(MDRepositoryFactory.class.getName());

    private MDRepositoryFactory()
    {
    }
    
    public static MDRepository newMDRepository(Properties storageProps)
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

        List<Properties> modelProperties;
        try {
            modelProperties = loadRepositoryProperties(provider);
        } catch (IOException e) {
            throw new ProviderInstantiationException(
                "Could not load model properties for " + provider, e);
        }
        
        switch(provider) {
        case ENKI_HIBERNATE:
            return newHibernateRepository(storageProps, modelProperties);
            
        default:
            throw new UnknownProviderException(
                "unimplemented provider: " + provider.toString());
        }
    }

    private static MDRepository newNetbeansMDRepository(
        Properties storageProps)
    {
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
    
    private static MDRepository newHibernateRepository(
        Properties storageProps, List<Properties> modelProperties)
    {
        return new HibernateMDRepository(modelProperties, storageProps);
    }

    private static List<Properties> loadRepositoryProperties(
        MdrProvider provider) 
    throws IOException
    {
        ArrayList<Properties> modelProps = new ArrayList<Properties>();
        
        Enumeration<URL> configUrls = 
            Thread.currentThread().getContextClassLoader().getResources(
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
        
        return modelProps;
    }
    
    private static String applyPrefix(String prefix, String propertyName)
    {
        if (propertyName.startsWith(prefix)) {
            return propertyName;
        }
        
        return prefix + propertyName;
    }
}

// End MDRepositoryFactory.java
