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
package org.eigenbase.enki.mbean;

import java.lang.management.*;
import java.util.*;
import java.util.logging.*;

import javax.management.*;
import javax.management.openmbean.*;

import org.eigenbase.enki.mdr.*;

/**
 * EnkiMBeanUtil provides utilities for implementing the JMX MBeans
 * for Enki management/monitoring. It provides utility methods for
 * registering and unregistering {@link EnkiRepository} instances with JMX.
 * It also provides utility methods for generating {@link TabularData}
 * objects.
 * 
 * @author Stephan Zuercher
 */
public class EnkiMBeanUtil
{
    private static final String DOMAIN="org.eigenbase.enki.mbean";

    private static final Logger log = 
        Logger.getLogger(EnkiMBeanUtil.class.getName());
    
    private EnkiMBeanUtil()
    {
    }
    
    /**
     * Registers the given MBean with JMX.  The {@link ObjectName} used to
     * register the MBean is returned (and may be used in a subsequent call
     * to {@link #unregisterRepositoryMBean(ObjectName)}).  The object name
     * is taken with from the repository's storage properties (from the
     * {@link MDRepositoryFactory#ENKI_REPOS_NAME} property) or is generated
     * from the repository's type and identity hash code.
     * 
     * @param repositoryMBean the MBean to register
     * @return the name given to the MBean
     * @throws JMException on duplicate registration, malformed name, or other
     *                     JMX error
     */
    public static ObjectName registerRepositoryMBean(
        EnkiRepository repositoryMBean)
    throws JMException
    {
        MBeanServer mbeanServer = getMBeanServer();
        
        ObjectName objectName = 
            new ObjectName(
                DOMAIN + 
                ":type=" + EnkiRepository.class.getSimpleName() +
                ",name=" + getName(repositoryMBean.getRepos()));
        
        mbeanServer.registerMBean(repositoryMBean, objectName);
        
        return objectName;
    }

    private static MBeanServer getMBeanServer()
    {
        // REVIEW: SWZ: 2008-12-17: Is there a better way?

        // Under Sun JVMs, there is not (by default) an mbean server so (for
        // testing or other non-container invocations) the platform server will
        // be created on the first call to this method.  Under JRockit, the
        // platform server is always there.  If we're running inside, say,
        // JBoss, under the Sun JVM we'll see only JBoss' server and will use
        // it (as desired).  Under JBoss+JRockit, we'll see two servers and
        // want to chose the one that isn't the platform server. It is
        // possible, however, that there could be even more servers registered,
        // in which case we may choose the wrong one.
        List<?> mbeanServers = MBeanServerFactory.findMBeanServer(null);
        
        // Under Sun JVMs, outside of any container (e.g., junit tests or 
        // other stand alone operation), there will not be any MBeanServers.
        // Cause the platform server to be created.
        if (mbeanServers.isEmpty()) {
            return ManagementFactory.getPlatformMBeanServer();
        }
        
        // Under JRockit, outside of any container, there will always be a
        // platform MBeanServer.  Under Sun JVMs, inside a container (such
        // as JBoss) there may already be a server configured.  Either way,
        // use it.
        if (mbeanServers.size() == 1) {
            return (MBeanServer)mbeanServers.get(0);
        }
        
        // Under JRockit, inside a container (such as JBoss), there will be
        // two servers: the platform server and JBoss' server.  Use JBoss'
        // server.  However, it may be that there are 2 or more non-platform
        // servers already registered (perhaps by the container and an 
        // application running in the container), so track how many of those
        // we see and log if it's more than 1.
        MBeanServer platforMBeanServer = 
            ManagementFactory.getPlatformMBeanServer();
        
        MBeanServer result = null;
        int count = 0;
        for(Object o: mbeanServers) {
            MBeanServer mbeanServer = (MBeanServer)o;
            
            if (!mbeanServer.equals(platforMBeanServer)) {
                if (result == null) {
                    result = mbeanServer;
                }
                count++;
            }
        }
        
        if (result == null) {
            // Shouldn't be possible unless the platform server somehow appears
            // in the list multiple times.
            log.warning(
                "Found " + 
                mbeanServers.size() + 
                " platform MBeanServers (expected at most 1)");
            return platforMBeanServer;
        }
        
        if (count > 1) {
            // Log that we chose the first non-platform server of 2 or more.
            log.warning(
                "Found "
                + count
                + " MBeanServers (excluding platform server, if any); chose "
                + result);
        } else {
            log.fine("Chose MBeanServer: " + result);
        }

        return result;
    }
    
    /**
     * Unregisters the named MBean from JMX.  No checking is done to insure
     * that this object name is an EnkiRepository instance.
     * 
     * @param objectName the object name to unregister
     */
    public static void unregisterRepositoryMBean(ObjectName objectName)
    {
        MBeanServer mbeanServer = getMBeanServer();
        
        try {
            mbeanServer.unregisterMBean(objectName);
        } catch (JMException e) {
            log.log(
                Level.SEVERE, 
                "Unable to unregister '" + objectName.toString() + "'", 
                e);
        }
    }
    
    /**
     * Converts the given {@link Properties} object into a {@link TabularData}
     * instance with column names referencing storage properties.  This method
     * delegates to {@link #tabularDataFromMap(Map, String, String, String)}.
     * 
     * @param props storage properties to convert into TabularData
     * @return a TabularData object representing the Properties
     * @throws OpenDataException due to internal programming errors
     */
    public static TabularData tabularDataFromStorageProperties(
        Properties props)
    throws OpenDataException
    {
        return tabularDataFromMap(
            props,
            "storage property names and values",
            "storage property name",
            "storage property value");
    }
    
    /**
     * Converts the given {@link Map} object into a {@link TabularData} 
     * instance with the given description and column names.  The TabularData's
     * entries are added in the Map's iteration order.  The columns of the
     * TabularData are always String values and the map's keys and values
     * are converted to Strings via {@link String#valueOf(Object)}.
     * 
     * @param props the map to convert
     * @param desc description of the data
     * @param keyHeader header for the column containing map keys
     * @param valueHeader header for the column containing map values
     * @return a TabularData instance representing the map's data
     * @throws IllegalArgumentException if desc, keyHeader, or valueHeader are 
     *                                  null or empty strings; 
     * @throws OpenDataException if keyHeader equals valueHeader
     */
    public static TabularData tabularDataFromMap(
        Map<?, ?> props,
        String desc, 
        String keyHeader,
        String valueHeader)
    throws OpenDataException
    {
        String[] headers = 
            new String[] { keyHeader, valueHeader };

        CompositeType compType = 
            new CompositeType(
                desc,
                desc,
                headers,
                headers,
                new OpenType[] {
                    SimpleType.STRING,
                    SimpleType.STRING,
                });

        ArrayList<String> propNames = new ArrayList<String>();
        ArrayList<String> propValues = new ArrayList<String>();
        for(Map.Entry<?, ?> entry: props.entrySet()) {
            String name = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());
            propNames.add(name);
            propValues.add(value);
        }
        
        TabularType tabularType = 
            new TabularType(
                desc,
                desc,
                compType,
                headers);
        TabularData tabularData = new TabularDataSupport(tabularType);
        
        for(int i = 0; i < propNames.size(); i++) {
            String name = propNames.get(i);
            String value = propValues.get(i);
        
            CompositeData entry = 
                new CompositeDataSupport(
                    compType, 
                    headers, 
                    new Object[] { 
                        name,
                        value,
                    });
            
            tabularData.put(entry);
        }
        
        return tabularData;
    }
    
    private static String getName(EnkiMDRepository repos)
    {
        Properties storageProps = repos.getStorageProperties();
        
        String name = 
            storageProps.getProperty(MDRepositoryFactory.ENKI_REPOS_NAME);
        if (name != null) {
            return name;
        }
        
        // Invent a unique name
        return 
            repos.getProviderType().toString() + 
            "#" + 
            Long.toHexString(System.identityHashCode(repos));
    }
}

// End EnkiMBeanUtil.java
