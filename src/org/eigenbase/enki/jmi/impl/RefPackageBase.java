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

import java.lang.reflect.*;
import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;

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
    
    private final Map<String, Method> accessorMap;
    
    protected RefPackageBase(RefPackage container)
    {
        this.container = container;
        this.accessorMap = new HashMap<String, Method>();
        
        Method[] methods = getClass().getDeclaredMethods();
        for(Method method: methods) {
            String name = method.getName();
            int modifiers = method.getModifiers();
            if (name.startsWith("get") && 
                method.getParameterTypes().length == 0 &&
                RefBaseObject.class.isAssignableFrom(method.getReturnType()) &&
                Modifier.isPublic(modifiers) &&
                !Modifier.isStatic(modifiers) &&
                !Modifier.isAbstract(modifiers))
            {
                name = name.substring(3);
                
                accessorMap.put(name, method);
            }
        }
    }

    // Implement RefBaseObject
    public RefPackage refImmediatePackage()
    {
        return container;
    }

    @SuppressWarnings("unchecked")
    public Collection refAllAssociations()
    {
        return getAllOfTypeByReflection(RefAssociation.class);
    }

    @SuppressWarnings("unchecked")
    public Collection refAllClasses()
    {
        return getAllOfTypeByReflection(RefClass.class);
    }

    @SuppressWarnings("unchecked")
    public Collection refAllPackages()
    {
        return getAllOfTypeByReflection(RefPackage.class);
    }

    public RefAssociation refAssociation(RefObject association)
    {
        return invokeAccessorByType(RefAssociation.class, association);
    }

    public RefAssociation refAssociation(String associationName)
    {
        return invokeAccessor(RefAssociation.class, associationName);
    }

    public RefClass refClass(RefObject type)
    {
        return invokeAccessorByType(RefClass.class, type);
    }

    public RefClass refClass(String typeName)
    {
        return invokeAccessor(RefClass.class, typeName);
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(RefObject struct, List params)
    {
        return createInstance(struct, null, params, RefStruct.class);
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(String structName, List params)
    {
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
    

    public RefPackage refPackage(RefObject nestedPackage)
    {
        return invokeAccessorByType(RefPackage.class, nestedPackage);
    }

    public RefPackage refPackage(String nestedPackageName)
    {
        String mangledName = 
            StringUtil.mangleIdentifier(
                nestedPackageName, StringUtil.IdentifierType.ALL_LOWER);
        return invokeAccessor(
            RefPackage.class, 
            StringUtil.toInitialUpper(mangledName));
    }

    private <E> Collection<E> getAllOfTypeByReflection(Class<E> cls)
    {
        Method[] methods = getClass().getMethods();
        
        ArrayList<E> types = new ArrayList<E>();
        
        for(Method method: methods) {
            if (cls.isAssignableFrom(method.getReturnType()) &&
                method.getParameterTypes().length == 0 &&
                method.getName().startsWith("get"))
            {
                E type = invokeMethod(cls, method);

                types.add(type);
            }
        }
        
        return Collections.unmodifiableCollection(types);
    }
    
    private <E> E invokeAccessor(Class<E> cls, String typeName)
    {
        Method method = accessorMap.get(typeName);
        if (method == null) {
            throw new InvalidNameException(typeName);
        }
        
        return invokeMethod(cls, method);
    }

    private <E> E invokeAccessorByType(Class<E> cls, RefObject refObj)
    {
        ModelElement modelElem = (ModelElement)refObj;
        String typeName = getTypeName(modelElem);
        
        Method method = accessorMap.get(typeName);
        if (method == null) {
            throw new InvalidCallException(this, refObj);
        }
        
        return invokeMethod(cls, method);
    }
    
    private String getTypeName(ModelElement modelElem)
    {
        String baseName = modelElem.getName();

        String substName = getTag(modelElem, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (substName != null) {
            baseName = substName;
        }

        return StringUtil.mangleIdentifier(
            baseName, StringUtil.IdentifierType.CAMELCASE_INIT_UPPER);
    }
}

// End RefPackageBase.java
