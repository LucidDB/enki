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
package org.eigenbase.enki.mdr;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.netbeans.*;
import org.eigenbase.enki.trans.*;

/**
 * MDRepositoryFactory is a factory class for {@link EnkiMDRepository}
 * instances.
 * 
 * <p>The storage properties given to the factory method are used to determine
 * which provider to instantiate and to configure the provider itself.  The
 * storage properties must contain the a property with the key
 * {@value #ENKI_IMPL_TYPE} and the name of an Enki MDR Provider (as defined 
 * by the enumeration {@link MdrProvider}. In addition, the storage properties
 * may specify a unique repository name by providing a property with the key
 * {@value #ENKI_REPOS_NAME}. The name is used for JMX MBean identification.
 * All other storage properties are specific to the provider.
 *  
 * @author Stephan Zuercher
 */
public class MDRepositoryFactory
{
    public static final String PROPERTY_ENKI_RUNTIME_CONFIG_URL = 
        "enki.runtime.config.url";
    
    public static final String META_INF_DIR_NAME = "META-INF";
    
    public static final String ENKI_DIR_NAME = "enki";

    /** The name of the META-INF/enki directory. */
    public static final String META_INF_ENKI_DIR = 
        META_INF_DIR_NAME + "/" + ENKI_DIR_NAME;

    /** 
     * The name of the metamodel configuration properties file. Stored in 
     * the <code>META-INF/enki</code> directory of an Enki model JAR file.
     */
    public static final String CONFIG_PROPERTIES = "configurator.properties";

    public static final String ENKI_IMPL_TYPE = 
        "org.eigenbase.enki.implementationType";
    
    public static final String ENKI_REPOS_NAME = 
        "org.eigenbase.enki.repositoryName";
    
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
        MdrProvider provider = getProvider(storageProps);
        
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
            
        case ENKI_TRANSIENT:
            return newTransientRepository(
                storageProps, modelProperties, classLoader);
            
        default:
            throw new UnknownProviderException(
                "unimplemented provider: " + provider.toString());
        }
    }

    private static MdrProvider getProvider(Properties storageProperties)
    {
        String providerName = storageProperties.getProperty(ENKI_IMPL_TYPE);
        if (providerName == null) {
            throw new UnknownProviderException(
                "repository storage properties is missing key '" +
                ENKI_IMPL_TYPE + "'");
        }

        MdrProvider provider = 
            MdrProvider.valueOf(providerName.toUpperCase(Locale.US));
        if (provider == null) {
            throw new UnknownProviderException(
                "unknown provider: " + providerName);
        }

        return provider;
    }
    
    private static EnkiMDRepository newNetbeansMDRepository(
        Properties storageProps)
    {
        return new NBMDRepositoryWrapper(storageProps, classLoaderProvider);
    }
    
    private static EnkiMDRepository newHibernateRepository(
        Properties storageProps, 
        List<Properties> modelProperties,
        ClassLoader classLoader)
    {
        ClassLoader contextClassLoader = null;
        if (classLoader != null) {
            contextClassLoader = 
                Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        try {
            return new HibernateMDRepository(
                modelProperties, storageProps, classLoader);
        }
        finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(
                    contextClassLoader);                
            }
        }
    }
    
    private static EnkiMDRepository newTransientRepository(
        Properties storageProps, 
        List<Properties> modelProperties,
        ClassLoader classLoader)
    {
        ClassLoader contextClassLoader = null;
        if (classLoader != null) {
            contextClassLoader = 
                Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        try {
            return new TransientMDRepository(
                modelProperties, storageProps, classLoader);
        }
        finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(
                    contextClassLoader);                
            }
        }
    }
    
    public static List<Properties> getRepositoryProperties(
        Properties storageProperties)
    {
        MdrProvider provider = getProvider(storageProperties);
        
        // Special case for delegation to Netbeans MDR implementation.
        if (provider == MdrProvider.NETBEANS_MDR) {
            return Collections.emptyList();
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

        return modelProperties;
    }

    private static List<Properties> loadRepositoryProperties(
        MdrProvider provider, ClassLoader classLoader) 
    throws IOException
    {
        ArrayList<Properties> modelProps = new ArrayList<Properties>();
        
        Enumeration<URL> configUrls = 
            classLoader.getResources(
                META_INF_ENKI_DIR + "/" + CONFIG_PROPERTIES);
        
        String ignoreExtent = 
            System.getProperty(SYS_PROPERTY_ENKI_IGNORE_EXTENT);
        
        while(configUrls.hasMoreElements()) {
            URL configUrl = configUrls.nextElement();
            
            log.info("Config URL: " + configUrl.toString());

            Properties props = new Properties();
            props.load(configUrl.openStream());
        
            if (ignoreExtent != null &&
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
    
    public static void setClassLoaderProvider(ClassLoaderProvider provider)
    {
        classLoaderProvider = provider;
    }
    
    // Use EnkiMDRepository.getDefaultClassLoader() instead
    @Deprecated
    public static ClassLoader getDefaultClassLoader()
    {
        if (classLoaderProvider == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return classLoaderProvider.getClassLoader();
        }
    }
}

// End MDRepositoryFactory.java
