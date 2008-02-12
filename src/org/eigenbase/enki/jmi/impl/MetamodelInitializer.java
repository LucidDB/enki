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

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;

/**
 * MetamodelInitializer is an abstract base class used for initializing 
 * metamodels.
 * 
 * @author Stephan Zuercher
 */
public abstract class MetamodelInitializer
{
    private static final ThreadLocal<MetamodelInitializer> initializerTls =
        new ThreadLocal<MetamodelInitializer>();
    
    public static long METAMODEL_MOF_ID_MASK = 0x4000000000000000L; 
    
    private static long nextMofId = METAMODEL_MOF_ID_MASK;
    
    private final String metaModelExtent;
    
    private final Map<RefClassBase, Collection<RefObjectBase>> objectMap;
    private final Map<String, Object> propertyMap;
    
    private ModelPackage modelPackage;
    private ModelPackage metaModelPackage;
    
    private EnkiMDRepository owningRepository;
    
    protected Logger log = 
        Logger.getLogger(MetamodelInitializer.class.getName());
    
    public MetamodelInitializer(String metaModelExtent)
    {
        this.metaModelExtent = metaModelExtent;
        this.objectMap = 
            new TreeMap<RefClassBase, Collection<RefObjectBase>>(
                RefBaseObjectComparator.instance);
        this.propertyMap = new HashMap<String, Object>();
    }
    
    public void setOwningRepository(EnkiMDRepository repos)
    {
        this.owningRepository = repos;
    }
    
    public void init(ModelPackage metaModelPackage)
    {
        this.metaModelPackage = metaModelPackage;
        
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
    
    public EnkiMDRepository getRepository()
    {
        return owningRepository;
    }
    
    protected void setModelPackage(ModelPackage modelPackage)
    {
        if (this.modelPackage != null) {
            throw new InternalJmiError("multiple ModelPackages");
        }
        
        this.modelPackage = modelPackage;
    }
    
    protected ModelPackage getMetaModelPackage()
    {
        return metaModelPackage;
    }
    
    public Object getProperty(String name)
    {
        return propertyMap.get(name);
    }
    
    public Object setProperty(String name, Object value)
    {
        return propertyMap.put(name, value);
    }
    
    public static MetamodelInitializer getCurrentInitializer()
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
    
    Collection<? extends RefObject> getAllInstancesOf(
        RefClassBase refClass, boolean includeSubtypes)
    {
        log.finest(
            "Looking up all instances of " + 
            refClass.getClass() + "/" + refClass.refMofId());
        
        Collection<RefObjectBase> instances = objectMap.get(refClass);
        
        if (!includeSubtypes) {
            if (instances != null) {
                return Collections.unmodifiableCollection(instances);
            } else {
                return Collections.emptySet();
            }
        }

        ArrayList<RefObject> allInstances = new ArrayList<RefObject>();
        if (instances != null) {
            allInstances.addAll(instances);
        }
        
        MofClass mofCls = (MofClass)refClass.refMetaObject();
        Generalizes generalizesAssoc = getModelPackage().getGeneralizes();
        
        Collection<MofClass> subClasses = 
            GenericCollections.asTypedCollection(
                generalizesAssoc.getSubtype(mofCls), MofClass.class);
        for(MofClass subClass: subClasses) {
            RefClassBase refSubClass = 
                (RefClassBase)refClass.refImmediatePackage().refClass(
                    subClass.getName());
            
            allInstances.addAll(getAllInstancesOf(refSubClass, true));
        }

        return Collections.unmodifiableCollection(allInstances);
    }
    
    Collection<RefClassBase> getAllRefClasses()
    {
        return Collections.unmodifiableCollection(objectMap.keySet());
    }
    
    public RefBaseObject getByMofId(String mofId)
    {
        ModelPackage modelPkg = getModelPackage();
        
        return getByMofId(modelPkg, mofId);
    }

    private RefBaseObject getByMofId(RefPackage pkg, String mofId)
    {
        for(RefAssociation assoc:
                GenericCollections.asTypedCollection(
                    pkg.refAllAssociations(), RefAssociation.class))
        {
            if (assoc.refMofId().equals(mofId)) {
                return assoc;
            }
        }
        
        for(RefPackage subPkg: 
                GenericCollections.asTypedCollection(
                    pkg.refAllPackages(), RefPackage.class))
        {
            if (subPkg.refMofId().equals(mofId)) {
                return subPkg;
            }
            
            RefBaseObject recursiveResult = getByMofId(subPkg, mofId);
            if (recursiveResult != null) {
                return recursiveResult;
            }
        }
        
        for(RefClass cls:
                GenericCollections.asTypedCollection(
                    pkg.refAllClasses(), RefClass.class))
        {
            if (cls.refMofId().equals(mofId)) {
                return cls;
            }
            
            for(RefObject obj: 
                    GenericCollections.asTypedCollection(
                        cls.refAllOfClass(), RefObject.class))
            {
                if (obj.refMofId().equals(mofId)) {
                    return obj;
                }
            }
        }
        
        return null;
    }
    
    void register(RefObjectBase refObject)
    {
        RefClassBase refClass = (RefClassBase)refObject.refClass();
        
        log.finer(
            "Registering " + refClass.getClass() + "/" + refObject.refMofId() + 
            " with " + getClass());
        
        Collection<RefObjectBase> instances = objectMap.get(refClass);
        if (instances == null) {
            instances = 
                new TreeSet<RefObjectBase>(RefBaseObjectComparator.instance);
            objectMap.put(refClass, instances);
        }
        
        if (!instances.add(refObject)) {
            throw new InternalJmiError(
                "multiple objects with same mofId: " + refObject.refMofId());
        }
    }
    
    protected RefObject findMofClassByName(
        String name, boolean searchMetaModel)
    {
        ModelPackage mp = getModelPackage();
        if (searchMetaModel && metaModelPackage != null) {
            mp = metaModelPackage;
        }
        
        for(MofClass mofClass:         
            GenericCollections.asTypedCollection(
                mp.getMofClass().refAllOfClass(), MofClass.class)) 
        {
            if (mofClass.getName().equals(name)) {
                return mofClass;
            }
        }
        
        throw new NoSuchElementException(name);
    }

    protected RefObject findMofClassByName(String name)
    {
        return findMofClassByName(name, false);
    }
    
    protected void setRefMetaObject(RefBaseObject base, RefObject meta)
    {
        ((RefBaseObjectBase)base).setRefMetaObject(meta);
    }
    
    protected RefObject findMofPackageByName(String name)
    {
        ModelPackage mp = getModelPackage();

        for(MofPackage mofPackage:         
            GenericCollections.asTypedCollection(
                mp.getMofPackage().refAllOfClass(), MofPackage.class)) 
        {
            if (mofPackage.getName().equals(name)) {
                return mofPackage;
            }
        }
        
        throw new NoSuchElementException(name);
    }
    
    public void setRefMetaObject(RefPackageBase pkg, String pkgName)
    {
        RefObject metaObj = findMofPackageByName(pkgName);

        pkg.setRefMetaObject(metaObj);
    }

    public void setRefMetaObject(RefClassBase cls, String clsName)
    {
        RefObject metaObj = findMofClassByName(clsName);

        cls.setRefMetaObject(metaObj);
    }

    protected RefObject findAssociationByName(String name)
    {
        ModelPackage mp = getModelPackage();

        for(Association association:         
            GenericCollections.asTypedCollection(
                mp.getAssociation().refAllOfClass(), Association.class)) 
        {
            if (association.getName().equals(name)) {
                return association;
            }
        }
        
        throw new NoSuchElementException(name);
    }
    
    public void setRefMetaObject(RefAssociationBase assoc, String assocName)
    {
        RefObject metaObj = findAssociationByName(assocName);

        assoc.setRefMetaObject(metaObj);
    }
}

// End MetamodelInitializer.java
