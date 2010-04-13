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
package org.eigenbase.enki.jmi.impl;

import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;

/**
 * RefPackageBase is a base class for {@link RefPackage} implementations.
 * 
 * @author Stephan Zuercher
 */
public abstract class RefPackageBase 
    extends RefBaseObjectBase 
    implements RefPackage
{
    private final RefPackage container;
    private final EnkiMDRepository repos;

    private final Map<String, RefClass> classMap;
    private final Map<String, RefAssociation> associationMap;
    private final Map<String, RefPackage> packageMap;
    
    protected RefPackageBase(RefPackage container)
    {
        this.container = container;
        this.repos = getCurrentInitializer().getRepository();
        this.classMap = new HashMap<String, RefClass>();
        this.associationMap = new HashMap<String, RefAssociation>();
        this.packageMap = new HashMap<String, RefPackage>();
    }
    
    protected void addClass(String name, RefClass refClass)
    {
        classMap.put(name, refClass);
    }

    protected void addAssociation(String name, RefAssociation refAssoc)
    {
        associationMap.put(name, refAssoc);
    }

    public void addPackage(String name, RefPackage refPackage)
    {
        packageMap.put(name, refPackage);
    }

    /**
     * Registers any clustered import packages which are being treated as
     * aliases.  Subclasses override this based on the way
     * they have been generated.
     */
    public void addAliasPackages()
    {
    }
    
    // Implement RefBaseObject
    public RefPackage refImmediatePackage()
    {
        return container;
    }

    @SuppressWarnings("unchecked")
    public Collection refAllAssociations()
    {
        return associationMap.values();
    }

    @SuppressWarnings("unchecked")
    public Collection refAllClasses()
    {
        return classMap.values();
    }

    @SuppressWarnings("unchecked")
    public Collection refAllPackages()
    {
        return packageMap.values();
    }

    public RefAssociation refAssociation(RefObject association)
    {
        return getByType(RefAssociation.class, association, associationMap);
    }

    public RefAssociation refAssociation(String associationName)
    {
        return get(RefAssociation.class, associationName, associationMap);
    }

    public RefClass refClass(RefObject type)
    {
        return getByType(RefClass.class, type, classMap);
    }

    public RefClass refClass(String typeName)
    {
        return get(RefClass.class, typeName, classMap);
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(RefObject struct, List params)
    {
        logJmi("refCreateStruct(ByRefObject)");
        
        return createInstance(struct, null, params, RefStruct.class);
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(String structName, List params)
    {
        logJmi("refCreateStruct(ByName)");

        return createInstance(null, structName, params, RefStruct.class);
    }

    public void refDelete()
    {
        throw new UnsupportedOperationException();
    }

    public RefEnum refGetEnum(RefObject enumType, String literalName)
    {
        return getEnum(enumType, null, literalName);
    }

    public RefEnum refGetEnum(String enumTypeName, String literalName)
    {
        return getEnum(null, enumTypeName, literalName);
    }
    

    public RefPackage refPackage(RefObject pkg)
    {
        return getByType(RefPackage.class, pkg, packageMap);
    }

    public RefPackage refPackage(String pkgName)
    {
        return get(RefPackage.class, pkgName, packageMap);
    }

    private <E> E get(
        Class<E> cls, String typeName, Map<String, E> accessorMap)
    {
        E entity = accessorMap.get(typeName);
        if (entity == null) {
            throw new InvalidNameException(typeName);
        }
        
        return entity;
    }

    private <E> E getByType(
        Class<E> cls, RefObject refObj, Map<String, E> accessorMap)
    {
        ModelElement modelElem = (ModelElement)refObj;
        
        E entity = accessorMap.get(modelElem.getName());
        if (entity == null) {
            throw new InvalidCallException(this, refObj);
        }

        return entity;
    }
    
    @Override
    public EnkiMDRepository getRepository()
    {
        return repos;
    }
}

// End RefPackageBase.java
