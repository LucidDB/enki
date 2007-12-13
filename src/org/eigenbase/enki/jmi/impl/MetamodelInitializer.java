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
package org.eigenbase.enki.jmi.impl;

import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

/**
 * @author Stephan Zuercher
 */
public abstract class MetamodelInitializer
{
    private static final ThreadLocal<MetamodelInitializer> initializerTls =
        new ThreadLocal<MetamodelInitializer>();
    
    /** Width of a formatted MOF ID. See {@link #setMofId(long)}. */
    private static final int MOFID_WIDTH = 18;

    /** Constant prefix for formatted MOF IDs. */
    private static final String MOFID_PREFIX = "j:";

    private static long nextMofId = 0x4000000000000000L;
    
    private final String metaModelExtent;
    
    private final Map<RefClass, Collection<RefObject>> objectMap;
    private final Map<String, Object> propertyMap;
    
    private ModelPackage modelPackage;
    
    protected Logger log = 
        Logger.getLogger(MetamodelInitializer.class.getName());
    
    public MetamodelInitializer(String metaModelExtent)
    {
        this.metaModelExtent = metaModelExtent;
        this.objectMap = 
            new TreeMap<RefClass, Collection<RefObject>>(
                RefBaseObjectComparator.instance);
        this.propertyMap = new HashMap<String, Object>();
    }
    
    public void init()
    {
        initializerTls.set(this);
        
        initMetamodel();
        
        initializerTls.set(null);
    }
    
    public String getExtent()
    {
        return metaModelExtent;
    }
    
    public ModelPackage getModelPackage()
    {
        return modelPackage;
    }
    
    protected void setModelPackage(ModelPackage modelPackage)
    {
        if (this.modelPackage != null) {
            throw new InternalJmiError("multiple ModelPackages");
        }
        
        this.modelPackage = modelPackage;
    }
    
    public Object getProperty(String name)
    {
        return propertyMap.get(name);
    }
    
    public Object setProperty(String name, Object value)
    {
        return propertyMap.put(name, value);
    }
    
    static MetamodelInitializer getCurrentInitializer()
    {
        return initializerTls.get();
    }
    
    public static void setCurrentInitializer(MetamodelInitializer initializer)
    {
        initializerTls.set(initializer);
    }
    
    protected abstract void initMetamodel();
    
    protected long nextMofId()
    {
        synchronized(MetamodelInitializer.class) {
            return nextMofId++;
        }
    }
    
    /**
     * Updates the unformatted MOF ID for this entity and compute the
     * formatted MOF ID.  The formatted MOF ID is always exactly 
     * {@link #MOFID_WIDTH} characters wide and always begins with
     * {@link #MOFID_PREFIX}.
     * 
     * <p>This method is normally called only by Hibernate when an existing 
     * model entity is loaded from the database.
     * 
     * @param mofId
     */
    protected static String makeMofIdStr(long mofId)
    {
        StringBuilder b = new StringBuilder(MOFID_PREFIX);
        
        String hexMofId = Long.toHexString(mofId);
        
        int padding = MOFID_WIDTH - MOFID_PREFIX.length() - hexMofId.length();
        while(padding-- > 0) {
            b.append('0');
        }
        b.append(hexMofId);
        
        return b.toString();
    }
    
    Collection<RefObject> getAllInstancesOf(
        RefClass refClass, boolean includeSubtypes)
    {
        log.finest(
            "Looking up all instances of " + 
            refClass.getClass() + "/" + refClass.refMofId());
        
        Collection<RefObject> instances = objectMap.get(refClass);
        
        if (!includeSubtypes) {
            if (instances != null) {
                return Collections.unmodifiableCollection(instances);
            } else {
                return Collections.emptySet();
            }
        }
        
        // TODO: finish implementation
        assert(false);
        
        return null;
    }
    
    Collection<RefClass> getAllRefClasses()
    {
        return Collections.unmodifiableCollection(objectMap.keySet());
    }
    
    void register(RefObject refObject)
    {
        RefClass refClass = refObject.refClass();
        
        log.finer(
            "Registering " + refClass.getClass() + "/" + refClass.refMofId() + 
            " with " + getClass());
        
        Collection<RefObject> instances = objectMap.get(refClass);
        if (instances == null) {
            instances = new TreeSet<RefObject>(
                RefBaseObjectComparator.instance);
            objectMap.put(refClass, instances);
        }
        
        if (!instances.add(refObject)) {
            throw new InternalJmiError(
                "multiple objects with same mofId: " + refObject.refMofId());
        }
    }
}

// End MetamodelInitializer.java
