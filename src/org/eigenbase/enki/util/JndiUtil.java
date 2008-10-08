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
package org.eigenbase.enki.util;

import java.util.*;
import java.util.logging.*;

import javax.naming.*;

import org.osjava.sj.memory.*;

/**
 * JndiUtil provides various utility methods for accessing JNDI
 * resources.
 * 
 * <p>JndiUtil instances provide conversion of application properties into
 * JNDI properties.  For example, you may have many configuration properties, 
 * but only some (with a common prefix) should be passed to JNDI's
 * InitialContext.
 * 
 * <p>Application properties are converted to JNDI properties automatically.
 * All properties that begin with the application property prefix, but are not 
 * the initial context factory property or provider URL property, have the
 * prefix stripped and are copied to the JNDI properties.  If present, the 
 * initial context factory property and/or provider URL properties are copied 
 * as {@link Context#INITIAL_CONTEXT_FACTORY} and {@link Context#PROVIDER_URL},
 * respectively.
 * 
 * <p>The two special properties can have the same prefix as the other 
 * properties, but it is not necessary. 
 * 
 * @author Stephan Zuercher
 */
public class JndiUtil
{
    private static final Logger log = 
        Logger.getLogger(JndiUtil.class.getName());
    
    private final String appPropertyPrefix;
    private final String initialContextFactoryProperty;
    private final String providerUrlProperty;
    
    /**
     * Constructs a JndiUtil instance.  This is only necessary if you wish
     * to use JndiUtil convert application properties into JNDI environment
     * properties. See above.
     * 
     * @param appPropertyPrefix JNDI property prefix
     * @param initialContextFactoryProperty name of the application property
     *            that specifies the initial context factory (e.g.,
     *            {@link Context#INITIAL_CONTEXT_FACTORY})
     * @param providerUrlProperty name of the application property
     *            that specifies the provider URL (e.g., 
     *            {@link Context#PROVIDER_URL})
     */
    public JndiUtil(
        String appPropertyPrefix,
        String initialContextFactoryProperty,
        String providerUrlProperty)
    {
        this.appPropertyPrefix = appPropertyPrefix;
        this.initialContextFactoryProperty = initialContextFactoryProperty;
        this.providerUrlProperty = providerUrlProperty;
    }
    
    /**
     * Tests whether a JNDI provider is configured.  If yes, it returns false
     * and the given properties may be passed directly to 
     * {@link #newInitialContext(Properties)}. This method will return false 
     * if it detects an existing JNDI provider configuration, even if that 
     * configuration does not result in a valid {@link InitialContext}.  In
     * other words, a misconfigured JNDI provider passes the test.
     * 
     * <p>If no JNDI provider is detected, it returns true and modifies the
     * given properties with additional settings to allow the use of a basic 
     * JNDI provider implemented by the Simple-JNDI project.  
     * 
     * @param storageProps application properties
     * @return true if the properties were modified; false otherwise
     * @see MemoryContextFactory
     */
    public boolean initJndi(Properties storageProps)
    {
        if (checkInitialContext(storageProps)) {
            return false;
        }
        
        // Up to us to configure things.  Use the Simple-JNDI memory context
        // and update the storage properties with the necessary JNDI
        // environment.
        storageProps.setProperty(
            initialContextFactoryProperty, 
            MemoryContextFactory.class.getName());
        
        // Enable sharing else each InitialContext gets its own storage and
        // they can't see each other's objects.
        storageProps.setProperty(
            appPropertyPrefix + "org.osjava.sj.jndi.shared",
            "true");
        
        return true;
    }
    
    private boolean checkInitialContext(Properties props)
    {
        try {
            InitialContext initialContext = newInitialContext(props);

            // Requesting the environment forces the actual context to be 
            // constructed, and fails when it's not configured. Even if it 
            // didn't fail, the presence of the initial factory key indicates 
            // whether JNDI is configured.
            return initialContext.getEnvironment().containsKey(
                Context.INITIAL_CONTEXT_FACTORY);
        } catch(NoInitialContextException e) {
            // Happens when java.naming.factory.initial is not set
            return false;
        } catch(NamingException e) {
            // Probably mis-configured, but let it go -- assume the caller
            // will log this when it tries to create an InitialContext for
            // actual use.
            return true;
        }
    }
    
    /**
     * Constructs a new InitialContext from the given application properties.
     * 
     * @param props application properties
     * @return a JNDI {@link InitialContext}
     * @throws NamingException if the InitialContext cannot be constructed
     *             (invalid configuration, etc.)
     */
    public InitialContext newInitialContext(Properties props)
        throws NamingException
    {
        Properties jndiProps = getJndiProperties(props);
        
        if (jndiProps.isEmpty()) {
            return new InitialContext();
        } else {
            return new InitialContext(jndiProps);
        }
    }
    
    /**
     * Generic helper method that binds the given object to the given name
     * and automatically constructs any necessary sub-contexts.  This method
     * does not require a JndiUtil instance.
     * 
     * @param context base context for the bind operation
     * @param name value's name
     * @param value the value
     * @throws NamingException if a sub-context cannot be created, or there
     *             is an error binding the value to the terminal sub-context,
     *             or the name is already bound
     */
    public static void bind(Context context, String name, Object value)
        throws NamingException
    {
        try {
            context.bind(name, value);
        } catch(NamingException e) {
            Name nameObj = context.getNameParser("").parse(name);
            while(nameObj.size() > 1) {
                String contextName = nameObj.get(0);
                
                Context subContext = null;
                try {
                    subContext = (Context)context.lookup(contextName);
                } catch(NameNotFoundException e2) {
                    // ignored
                }
                
                if (subContext == null) {
                    context = context.createSubcontext(contextName);
                } else {
                    context = subContext;
                }
                
                nameObj = nameObj.getSuffix(1);
            }
            
            context.bind(nameObj, value);
        }
    }
    
    /**
     * Generic helper method that unbinds the given name.  It automatically
     * looks up sub-contexts as necessary.  This method does not require a 
     * JndiUtil instance.
     * 
     * @param context base context for the unbind operation
     * @param name value's name
     * @throws NamingException if a sub-context cannot be found, or there
     *             is an error unbinding the value to the terminal sub-context
     */
    public static void unbind(Context context, String name)
        throws NamingException
    {
        try {
            context.unbind(name);
        } catch(NamingException e) {
            Name nameObj = context.getNameParser("").parse(name);
            while(nameObj.size() > 1) {
                String contextName = nameObj.get(0);
                
                // Let the exception escape if the sub-context's name is not
                // found.
                context = (Context)context.lookup(contextName);
                
                nameObj = nameObj.getSuffix(1);
            }
            
            context.unbind(nameObj);
        }
    }

    /**
     * Generic helper method that looks up the object bound to the given name.
     * It automatically looks up sub-contexts as necessary.  This method does 
     * not require a JndiUtil instance.
     *
     * @param <T> the expected object type
     * @param context base context for the lookup operation
     * @param name value's name
     * @param cls Class representing the expected object type
     * @return the bound object, or null if it cannot be cast to T
     * @throws NamingException
     */
    public static <T> T lookup(Context context, String name, Class<T> cls)
        throws NamingException
    {
        Object o = null;
        try {
            o = context.lookup(name);
        } catch(NamingException e) {
            Name nameObj = context.getNameParser("").parse(name);
            while(nameObj.size() > 1) {
                String contextName = nameObj.get(0);

                // Let the exception escape if the sub-context's name is not
                // found.
                context = (Context)context.lookup(contextName);
                                
                nameObj = nameObj.getSuffix(1);
            }
            
            o = context.lookup(nameObj);
        }
        
        if (cls.isInstance(o)) {
            return cls.cast(o);
        }
        
        return null;
    }
    
    /**
     * Retrieves the JNDI properties computed from the given application
     * properties, including the initial context factory and provider URL
     * properties.  If the special properties are not set, they are not 
     * included in the result. Equivalent to 
     * {@link
     *      #getJndiProperties(Properties, boolean) 
     *      getJndiProperties(props, true)}.
     * 
     * @param props application properties
     * @return JNDI properties
     */
    public Properties getJndiProperties(Properties props)
    {
        return getJndiProperties(props, true);
    }
    
    /**
     * Retrieves the JNDI properties computed from the given application
     * properties, optionally including the initial context factory and 
     * provider URL properties.  If the special properties are not set,
     * they are not included in the result.
     * 
     * @param props application properties
     * @param includeSpecialProps whether or not to include the special
     *                            properties
     * @return JNDI properties
     */
    public Properties getJndiProperties(
        Properties props, boolean includeSpecialProps)
    {
        Properties jndiProps = new Properties();
        
        final int len = appPropertyPrefix.length();
        Iterator<Object> iter = props.keySet().iterator();
        while(iter.hasNext()) {
            String key = (String)iter.next();
            
            if (key.startsWith(appPropertyPrefix) &&
                !key.equals(initialContextFactoryProperty) &&
                !key.equals(providerUrlProperty))
            {
                String strippedKey = key.substring(len);
                Object value = props.get(key);
                log.fine("JNDI prop: " + strippedKey + " = " + value);
                jndiProps.put(strippedKey, value);
            }
        }
        
        if (includeSpecialProps) {
            String jndiClass = 
                props.getProperty(initialContextFactoryProperty);
            if (jndiClass != null) {
                log.fine(
                    "JNDI prop: " 
                    + Context.INITIAL_CONTEXT_FACTORY 
                    + " = " 
                    + jndiClass);
                jndiProps.setProperty(
                    Context.INITIAL_CONTEXT_FACTORY, jndiClass);
            }
            
            String jndiUrl = props.getProperty(providerUrlProperty);
            if (jndiUrl != null) {
                log.fine(
                    "JNDI prop: " 
                    + Context.PROVIDER_URL 
                    + " = " 
                    + jndiUrl);
                jndiProps.setProperty(Context.PROVIDER_URL, jndiUrl);
            }
        }
        
        return jndiProps;
    }
    
    /**
     * Returns the initial context factory name property's value from the
     * given application properties.
     * 
     * @param props application properties
     * @return initial context factory name, or null if not set
     */
    public String getInitialContextFactoryName(Properties props)
    {
        return props.getProperty(initialContextFactoryProperty);
    }
    
    /**
     * Returns the provider URL property's value from the given application 
     * properties. 
     * 
     * @param props application properties
     * @return provider URL, or null if not set
     */
    public String getProviderUrl(Properties props)
    {
        return props.getProperty(providerUrlProperty);
    }
}

// End JndiUtil.java
