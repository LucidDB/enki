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
    
    private final Set<RefClassBase> allClasses;
    private final Map<String, Object> propertyMap;
    
    private ModelPackage modelPackage;
    private ModelPackage metaModelPackage;
    
    private EnkiMDRepository owningRepository;
    
    protected Logger log = 
        Logger.getLogger(MetamodelInitializer.class.getName());
    
    public MetamodelInitializer(String metaModelExtent)
    {
        this.metaModelExtent = metaModelExtent;
        this.allClasses = new HashSet<RefClassBase>();
        this.propertyMap = new HashMap<String, Object>();
    }
    
    public void setOwningRepository(EnkiMDRepository repos)
    {
        this.owningRepository = repos;
    }
    
    public final void init(ModelPackage metamodelPackageInit)
    {
        this.metaModelPackage = metamodelPackageInit;
        
        initializerTls.set(this);
        
        initMetamodel();
        
        initializerTls.set(null);
    }
    
    public final void initPlugin(
        ModelPackage metaModelPackageInit,
        MetamodelInitializer parentInitializer)
    {
        this.metaModelPackage = metaModelPackageInit;
        
        initializerTls.set(parentInitializer);
        
        setModelPackage(parentInitializer.getModelPackage());
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
    
    Collection<RefClassBase> getAllRefClasses()
    {
        return Collections.unmodifiableCollection(allClasses);
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
        
        allClasses.add(refClass);
    }
    
    protected MofClass findMofClassByName(
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

    protected MofClass findMofClassByName(String name)
    {
        return findMofClassByName(name, false);
    }
    
    protected void setRefMetaObject(RefBaseObject base, RefObject meta)
    {
        ((RefBaseObjectBase)base).setRefMetaObject(meta);
    }
    
    protected MofPackage findMofPackageByName(String name)
    {
        return findMofPackageByName(name, false);
    }

    protected MofPackage findMofPackageByName(
        String name, boolean searchMetaModel)
    {
        ModelPackage mp = getModelPackage();
        if (searchMetaModel && metaModelPackage != null) {
            mp = metaModelPackage;
        }
        
        return findMofPackageByName(mp, name);
    }

    private MofPackage findMofPackageByName(ModelPackage mp, String name)
    {
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
        RefObject metaObj;
        if (pkg == getModelPackage()) {
            metaObj = findMofPackageByName(getMetaModelPackage(), pkgName);            
        } else {
            metaObj = findMofPackageByName(pkgName);
        }

        pkg.setRefMetaObject(metaObj);
    }

    public void setRefMetaObject(RefClassBase cls, String clsName)
    {
        RefObject metaObj = findMofClassByName(clsName);

        cls.setRefMetaObject(metaObj);
    }

    protected Association findAssociationByName(String name)
    {
        return findAssociationByName(name, false);
    }

    protected Association findAssociationByName(
        String name, boolean searchMetaModel)
    {
        ModelPackage mp = getModelPackage();
        if (searchMetaModel && metaModelPackage != null) {
            mp = metaModelPackage;
        }
        
        return findAssociationByName(mp, name);
    }

    private Association findAssociationByName(ModelPackage mp, String name)
    {
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
    
    protected RefObject findGeneric(String type, String name)
    {
        ModelPackage mp = getModelPackage();

        RefClass cls = mp.refClass(type);
        for(RefObject obj: 
                GenericCollections.asTypedCollection(
                    cls.refAllOfClass(), RefObject.class))
        {
            if (obj.refGetValue("name").equals(name)) {
                return obj;
            }
        }
        
        throw new NoSuchElementException(name);
    }
    
    public void stitchPackages(RefPackage topLevelPkg)
    {
    }
    
    public void setRefMetaObject(RefAssociationBase assoc, String assocName)
    {
        RefObject metaObj = findAssociationByName(assocName);

        assoc.setRefMetaObject(metaObj);
    }
}

// End MetamodelInitializer.java
