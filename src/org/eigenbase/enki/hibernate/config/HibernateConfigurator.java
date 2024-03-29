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
package org.eigenbase.enki.hibernate.config;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mdr.*;
import org.hibernate.cfg.*;

/**
 * HibernateConfigurator creates Hibernate Configuration objects for use in
 * configuring Hibernate session factories and its schema export tools.
 * 
 * @author Stephan Zuercher
 */
public final class HibernateConfigurator
{
    private final Logger log = Logger.getLogger(getClass().getName());

    private final Properties storageProperties;
    private final List<Properties> modelPropertiesList;
    private final boolean lenient;
    
    /** Map of metamodel extent names to ModelDescriptor instances. */
    private Map<String, ModelDescriptor> modelMap;
    
    /** 
     * Constructs a strict HibernateConfigurator with the given storage and 
     * model properties objects.
     * 
     * @param storageProperties repository storage properties
     * @param modelPropertiesList list of available model configurations
     */
    public HibernateConfigurator(
        Properties storageProperties,
        List<Properties> modelPropertiesList)
    {
        this(storageProperties, modelPropertiesList, false);
    }
    
    /** 
     * Constructs a HibernateConfigurator with the given storage and model
     * properties objects and leniency.
     * 
     * @param storageProperties repository storage properties
     * @param modelPropertiesList list of available model configurations
     * @param lenient if true, ignores missing top level package
     */
    public HibernateConfigurator(
        Properties storageProperties,
        List<Properties> modelPropertiesList,
        boolean lenient)
    {
        this.storageProperties = storageProperties;
        this.modelPropertiesList = modelPropertiesList;
        this.lenient = lenient;
    }
    
    public Configuration newConfiguration(boolean includeProviderMappings)
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
    
        if (includeProviderMappings) {
            URL internalConfigIUrl = 
                getClass().getResource(
                    HibernateMDRepository.HIBERNATE_STORAGE_MAPPING_XML);
            addConfig(config, internalConfigIUrl);
        }
        
        return config;
    }
    
    public Configuration newModelConfiguration(
        ModelDescriptor modelDescriptor, boolean includeProviderMappings)
    {
        Configuration config = newConfiguration(includeProviderMappings);
        
        configureMappings(config, modelDescriptor);
        
        return config;
    }
    
    /**
     * Returns a {@link Configuration} for the given model extent name.
     * 
     * @param modelExtentName name of a model extent (a key into the 
     *                        model map)
     * @param includeProviderMappings flag indicating whether the provider
     *                                mappings should be included in the
     *                                configuration
     * @return a {@link Configuration}
     * @throws NoSuchElementException if modelExtentName is not found
     */
    public Configuration newModelConfiguration(
        String modelExtentName, boolean includeProviderMappings)
    {
        ModelDescriptor modelDesc = getModelMap().get(modelExtentName);
        if (modelDesc == null) {
            throw new NoSuchElementException();
        }
        
        return newModelConfiguration(modelDesc, includeProviderMappings);
    }
    
    public Configuration newModelIndexMappingConfiguration(
        ModelDescriptor modelDescriptor, boolean includeProviderMappings)
    {
        Configuration config = newConfiguration(includeProviderMappings);
        
        configureIndexMappings(config, modelDescriptor);
        
        return config;
    }
   
    public void addModelIndexConfiguration(
        Configuration config, String modelExtentName)
    {
        ModelDescriptor modelDesc = getModelMap().get(modelExtentName);
        if (modelDesc == null) {
            throw new NoSuchElementException();
        }
        
        configureIndexMappings(config, modelDesc);
    }
    
    public void addModelConfigurations(Configuration config)
    {
        for(ModelDescriptor modelDesc: getModelMap().values()) {
            if (HibernateMDRepository.MOF_EXTENT.equals(modelDesc.name)) {
                continue;
            }

            configureMappings(config, modelDesc);
        }
    }
    
    public Map<String, ModelDescriptor> getModelMap()
    {
        if (modelMap == null) {
            modelMap = new HashMap<String, ModelDescriptor>();
            initModelMap();
        }
        
        return modelMap;
    }
    
    private void initModelMap()
    {
        Class<? extends RefPackage> mofPkgCls =
            org.eigenbase.enki.jmi.model.ModelPackage.class;
        
        ModelDescriptor mofModelDesc =
            new ModelDescriptor(
                HibernateMDRepository.MOF_EXTENT, 
                mofPkgCls, 
                null, 
                new Properties());
        modelMap.put(HibernateMDRepository.MOF_EXTENT, mofModelDesc);
        
        log.info(
            "Initializing Model Descriptor: " + 
            HibernateMDRepository.MOF_EXTENT);
        
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
                modelProperties.getProperty(
                    HibernateMDRepository.PROPERTY_MODEL_PACKAGE_VERSION);
            if (!packageVersion.equals(HibernateMDRepository.PACKAGE_VERSION))
            {
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
                        HibernateMDRepository.PROPERTY_MODEL_INITIALIZER);
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

                boolean failed = false;
                Class<?> cls = null;
                try {
                    cls = 
                        Class.forName(
                            topLevelPkg,
                            true, 
                            Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    if (!lenient) {
                        throw new ProviderInstantiationException(
                            "Top-level package '" + topLevelPkg + "' not found",
                            e);
                    }
                    
                    failed = true;
                }

                Class<? extends RefPackage> topLevelPkgCls = null;
                Constructor<? extends RefPackage> topLevelPkgCons = null;
                if (!failed) {
                    topLevelPkgCls = cls.asSubclass(RefPackage.class);
                        
                    try {
                        topLevelPkgCons = 
                            topLevelPkgCls.getConstructor(RefPackage.class);
                    } catch (NoSuchMethodException e) {
                        throw new ProviderInstantiationException(
                            "Cannot find constructor for top-level package class '"
                            + topLevelPkgCls.getName() + "'",
                            e);
                    }
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
    
    private boolean isPlugin(Properties modelProps)
    {
        return Boolean.parseBoolean(
            modelProps.getProperty(
                HibernateMDRepository.PROPERTY_MODEL_PLUGIN,
                "false"));
    }
    
    private void configureMappings(
        Configuration config, ModelDescriptor modelDesc)
    {
        URL mappingUrl = getModelMappingUrl(modelDesc);        
        addConfig(config, mappingUrl);

        for(ModelPluginDescriptor pluginDesc: modelDesc.plugins) {
            URL pluginMappingUrl = getModelMappingUrl(pluginDesc);
            addConfig(config, pluginMappingUrl);
        }
    }

    private void configureIndexMappings(
        Configuration config, ModelDescriptor modelDesc)
    {
        URL indexMappingUrl = getIndexMappingUrl(modelDesc);
        addConfig(config, indexMappingUrl);
    }
    
    private void addConfig(Configuration config, URL mappingUrl)
    {
        String tablePrefix = 
            PropertyUtil.readStorageProperty(
                storageProperties,
                null,
                HibernateMDRepository.PROPERTY_STORAGE_TABLE_PREFIX,
                "",
                String.class);
        
        InputStream urlStream;
        try {
            urlStream = mappingUrl.openStream();
        } catch (IOException e) {
            throw new ProviderInstantiationException(
                "Cannot read mapping document @ " + mappingUrl, e);
        }
        
        TablePrefixInputStream in = 
            new TablePrefixInputStream(urlStream, tablePrefix);
        config.addInputStream(in);
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
            
            URL mappingUrl = 
                relativeURL(
                    configUrl,
                    HibernateMDRepository.MAPPING_XML, 
                    "mapping URL");
    
            URL indexMappingUrl =
                relativeURL(
                    configUrl,
                    HibernateMDRepository.INDEX_MAPPING_XML,
                    "index mapping URL");

            URL createSqlUrl = 
                relativeURL(
                    configUrl,
                    HibernateMDRepository.MODEL_CREATE_DDL,
                    "create SQL URL");
            
            URL dropSqlUrl = 
                relativeURL(
                    configUrl,
                    HibernateMDRepository.MODEL_DROP_DDL,
                    "drop SQL URL");

            URL providerSqlUrl = 
                relativeURL(
                    configUrl,
                    HibernateMDRepository.PROVIDER_DDL,
                    "provider SQL URL");

            modelDesc.mappingUrl = mappingUrl;
            modelDesc.indexMappingUrl = indexMappingUrl;
            modelDesc.createDdl = createSqlUrl;
            modelDesc.dropDdl = dropSqlUrl;
            modelDesc.providerDdl = providerSqlUrl;
        }
        
        if (getIndexMapping) {
            return modelDesc.indexMappingUrl;
        } else {
            return modelDesc.mappingUrl;
        }
    }

    private URL relativeURL(URL base, String name, String type)
    {
        try {
            return new URL(base, name);
        } catch (MalformedURLException e) {
            throw new ProviderInstantiationException(
                "Cannot parse " + type, e);
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
}

// End HibernateConfigurator.java
