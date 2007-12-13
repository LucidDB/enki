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

/**
 * @author Stephan Zuercher
 */
public abstract class RefBaseObjectBase implements RefBaseObject
{
    private final MetamodelInitializer initializer;
    
    private long mofId;
    private String refMofId;
    private RefPackage immediatePackage;
    private RefObject metaObj;
    
    protected RefBaseObjectBase()
    {
        initializer = MetamodelInitializer.getCurrentInitializer();
        
        if (initializer != null) {
            setMofId(initializer.nextMofId());
        }
    }
    
    public RefObject refMetaObject()
    {
        return metaObj;
    }

    public String refMofId()
    {
        return refMofId;
    }

    public RefPackage refOutermostPackage()
    {
        if (immediatePackage == null) {
            assert(this instanceof RefPackage);
            return (RefPackage)this;
        }
        
        return immediatePackage.refOutermostPackage();
    }

    @SuppressWarnings("unchecked")
    public Collection refVerifyConstraints(boolean arg0)
    {
        // TODO: implement refVerifyConstraints
        return null;
    }

    public long getMofId()
    {
        return mofId;
    }
    
    public void setMofId(long refMofId)
    {
        this.mofId = refMofId;
        this.refMofId= MetamodelInitializer.makeMofIdStr(refMofId);
    }
    
    public void setImmediatePackage(RefPackage pkg)
    {
        this.immediatePackage = pkg;
    }
    
    public void setMetaObject(RefObject metaObj)
    {
        // TODO: this method needs to be called
        this.metaObj = metaObj;
    }

    public boolean equals(Object other)
    {
        if (other instanceof RefBaseObjectBase) {
            RefBaseObjectBase that = (RefBaseObjectBase)other;
            return this.mofId == that.mofId;
        } else {
            RefBaseObject that = (RefBaseObject)other;
            
            return this.refMofId.equals(that.refMofId());
        }
    }
    
    /**
     * Returns the hash code for this model entity.  The hash code is based
     * on the MOF ID.
     * 
     * @return the hash code for this model entity.
     */
    public int hashCode()
    {
        return (int)(mofId ^ (mofId >>> 32));
    }
        
    protected <E> E createInstance(
        RefObject type,
        String typeName,
        List<?> params,
        Class<E> resultType)
    {
        assert(this instanceof RefClass);
        
        if ((type == null && typeName == null) ||
            (type != null && typeName != null)) 
        {
            throw new InternalJmiError(
                "bad call: either type or typeName must be non-null");
        }
        
        if (type != null) {
            ModelElement modelElem = (ModelElement)type;
            typeName = modelElem.getName();
        }
        
        if (params == null) {
            params = Collections.emptyList();
        }
        
        Method[] methods = getClass().getMethods();
        
        for(Method method: methods) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (RefStruct.class.isAssignableFrom(method.getReturnType()) &&
                method.getName().startsWith("create")) {
    
                if (paramTypes.length != params.size()) {
                    throw new WrongSizeException(type, typeName);
                }
    
                for(int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = paramTypes[i];
                    Class<?> callerParamType = params.get(i).getClass();
                    
                    if (!paramType.isAssignableFrom(callerParamType)) {
                        throw new TypeMismatchException(
                            paramType, params.get(i), type, typeName);
                    }
                }
                
                // All param types are assignable, so this must by the
                // factory method we want to call.
                return invokeMethod(
                    resultType, this, method, params.toArray());
            }
        }
        
        if (type != null) {
            throw new InvalidCallException(this, type, typeName);
        } else {
            throw new InvalidNameException(typeName);
        }
    }

    protected RefEnum getEnum(RefObject enumType, String enumTypeName, String name)
    {
        assert(this instanceof RefPackage || this instanceof RefClass);
        
        if ((enumType == null && enumTypeName == null) ||
            (enumType != null && enumTypeName != null)) 
        {
            throw new InternalJmiError(
                "bad call: either enumType or enumTypeName must be non-null");
        }
        
        StringBuilder className = new StringBuilder();
        if (enumType != null) {
            ModelElement modelElem = (ModelElement)enumType;

            // REVIEW: Should this differ for a call from a class proxy?
            List<?> qualifiedName = modelElem.getQualifiedName();
            for(Object o: qualifiedName) {
                if (className.length() > 0) {
                    className.append('.');
                }
                className.append(o.toString());
            }
            className.append("Enum");
        } else {
            // REVIEW: Should this differ for a call from a class proxy?
            className
                .append(getClass().getPackage().getName())
                .append('.')
                .append(enumTypeName)
                .append("Enum");
        }
        
        
        try {
            return getEnum(className.toString(), name);
        }
        catch (Exception e) {
            // fall through
        }
        
        if (enumTypeName == null) {
            throw new InvalidCallException(this, enumType);
        } else {
            throw new InvalidNameException(enumTypeName);
        }
    }

    private RefEnum getEnum(String className, String literal) 
    throws 
        ClassNotFoundException, NoSuchMethodException, IllegalAccessException, 
        InvocationTargetException
    {
        Class<? extends RefEnum> cls =
            Class.forName(className.toString()).asSubclass(RefEnum.class);
        
        Method method = cls.getMethod("forName", String.class);

        RefEnum refEnum = cls.cast(method.invoke(null, literal));
        return refEnum;
    }

    protected <E> E invokeMethod(Class<E> cls, Method method)
    {
        return invokeMethod(cls, this, method);
    }
    
    protected <E> E invokeMethod(
        Class<E> cls, Object instance, Method method, Object... params)
    {
        try {
            Object result = method.invoke(instance, params);
            if (cls == Void.class) {
                assert(result == null);
                return null;
            }
            
            return cls.cast(result);
        }
        catch(Exception e) {
            throw new InternalJmiError(e);
        }
    }
    
    public final MetamodelInitializer getInitializer()
    {
        return initializer;
    }
    
    protected final MetamodelInitializer getCurrentInitializer()
    {
        MetamodelInitializer tlsInitializer =
            MetamodelInitializer.getCurrentInitializer();
        if (tlsInitializer != null) {
            return tlsInitializer;
        } else {
            return initializer;
        }
    }
}

// End RefBaseObjectBase.java
