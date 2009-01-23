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
 * RefFeaturedBase is a base class for {@link RefFeatured} implementations.
 * 
 * @author Stephan Zuercher
 */
public abstract class RefFeaturedBase 
    extends RefBaseObjectBase 
    implements RefFeatured
{
    /** Maximum number of classes for which reflective methods are cached. */
    private static final int MAX_CLASSES = 25;
    
    /** 
     * Maximum number of methods per class for which reflective methods
     * are cached.
     */
    private static final int MAX_METHODS_PER_CLASS = 50;
    
    private static final Map<Class<?>, Map<String, Method>> refMethodCache =
        new LRUHashMap<Class<?>, Map<String, Method>>(MAX_CLASSES);
    
    protected RefFeaturedBase()
    {
        super();
    }
    
    protected RefFeaturedBase(MetamodelInitializer initializer)
    {
        super(initializer);
    }
    
    public Object refGetValue(RefObject type)
    {
        logJmi("refGetValue(ByRefObject)");
        
        Method method = findMethod(type, null, true, 0);
        
        if (method != null) {
            return invokeMethod(Object.class, this, method);
        }
        
        throw new InvalidCallException(this, type);
    }

    public Object refGetValue(String typeName)
    {
        logJmi("refGetValue(ByName)");
        
        Method method = findMethod(null, typeName, true, 0);
        
        if (method != null) {
            return invokeMethod(Object.class, this, method);
        }
        
        throw new InvalidNameException(typeName);
    }

    @SuppressWarnings("unchecked")
    public Object refInvokeOperation(RefObject operation, List params)
        throws RefException
    {
        logJmi("refInvokeOperation(ByRefObject)");
        
        Method method = findOperationMethod(operation, null, params);
        
        if (method != null) {
            return invokeOperationMethod(method, params.toArray());
        }
        
        throw new InvalidCallException(this, operation);
    }

    @SuppressWarnings("unchecked")
    public Object refInvokeOperation(String operationName, List params)
        throws RefException
    {
        logJmi("refInvokeOperation(ByName)");
        
        Method method = findOperationMethod(null, operationName, params);
        
        if (method != null) {
            return invokeOperationMethod(method, params.toArray());
        }
        
        throw new InvalidNameException(operationName);
    }

    public void refSetValue(RefObject type, Object value)
    {
        logJmi("refSetValue(ByRefObject)");
        
        Method method = findMethod(type, null, false, 1);
        
        if (method != null && isMutable(method, value.getClass())) {
            invokeMethod(Void.class, this, method, value);
            return;
        }
        
        throw new InvalidCallException(this, type);
    }

    public void refSetValue(String typeName, Object value)
    {
        logJmi("refSetValue(ByName)");

        Method method = findMethod(null, typeName, false, 1);
        
        if (method != null && isMutable(method, value.getClass())) {
            invokeMethod(Void.class, this, method, value);
            return;
        }
        
        throw new InvalidNameException(typeName);
    }

    private Method findMethod(
        RefObject type, String typeName, boolean isGetter, int numParams)
    {
        if ((type == null && typeName == null) ||
            (type != null && typeName != null))
        {
            throw new InternalJmiError(
                "bad call: either type or typeName must be non-null");
        }
        
        if (typeName == null) {
            ModelElement typeElement = (ModelElement)type;
            typeName = typeElement.getName();
        }

        Class<?> cls;
        if (this instanceof RefClass) {
            cls = getClass();
        } else {
            RefClass refCls = ((RefObject)this).refClass();
            cls = ((RefClassBase)refCls).getInstanceClass();
        }
        
        String cacheTypeName = (isGetter ? "GET$$" : "SET$$") + typeName;
        
        synchronized(refMethodCache) {
            Map<String, Method> classMethodCache = refMethodCache.get(cls);
            if (classMethodCache != null) {
                Method method = classMethodCache.get(cacheTypeName);
                if (method != null) {
                    return method;
                }
            }
        
            boolean startsWithIs = 
                typeName.length() > 2 && 
                typeName.startsWith("is") && 
                Character.isUpperCase(typeName.charAt(2));
    
            String typeNameInitialUpper = StringUtil.toInitialUpper(typeName);
            
            String primaryMethodName;
            String secondaryMethodName;
            if (isGetter) {
                primaryMethodName =
                    "get" + typeNameInitialUpper;
                if (startsWithIs) {
                    secondaryMethodName = typeName;
                } else {
                    secondaryMethodName = "is" + typeNameInitialUpper;
                }
            } else {
                primaryMethodName = "set" + typeNameInitialUpper;
                if (startsWithIs) {
                    secondaryMethodName = "set" + typeName.substring(2);
                } else {
                    secondaryMethodName = null;
                }
            }
                
            Method[] methods = cls.getMethods();
    
            for(Method method: methods) {
                String methodName = method.getName();
                if (!methodName.equals(primaryMethodName)) {
                    if (secondaryMethodName == null || 
                        !methodName.equals(secondaryMethodName))
                    {
                        continue;
                    }
                }
                
                if (method.getParameterTypes().length == numParams) {
                    if (classMethodCache == null) {
                        classMethodCache = 
                            new LRUHashMap<String, Method>(
                                MAX_METHODS_PER_CLASS);
                        refMethodCache.put(cls, classMethodCache);
                    }
                    
                    classMethodCache.put(cacheTypeName, method);
                    return method;
                }
            }
        }
        
        return null;
    }

    private boolean isMutable(Method method, Class<?> valueType)
    {
        Class<?>[] paramTypes = method.getParameterTypes();

        // Value type must by assignable to the method's single parameter
        Class<?> paramType = paramTypes[0];
        
        if (paramType.isPrimitive()) {
            paramType = Primitives.getWrapper(paramType);
        }
        
        return 
            paramType.isAssignableFrom(valueType);
    }
    
    private Method findOperationMethod(
        RefObject operation, String operationName, List<?> params)
    {
        if ((operation == null && operationName == null) ||
            (operation != null && operationName != null))
        {
            throw new InternalJmiError(
            "bad call: either operation or operationName must be non-null");
        }
        
        if (params == null) {
            params = Collections.EMPTY_LIST;
        }
        
        if (operationName == null) {
            ModelElement operationElement = (ModelElement)operation;
            operationName = operationElement.getName();
        }
     
        Method[] methods = getClass().getMethods();
        for(Method method: methods) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (operationName.equals(method.getName())) {
                if (paramTypes.length != params.size()) {
                    throw new WrongSizeException(operation);
                }
                
                for(int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = paramTypes[i];
                    Class<?> callerParamType = params.get(i).getClass();
                    
                    if (paramType.isPrimitive()) {
                        paramType = Primitives.getWrapper(paramType);
                    }
                    
                    if (!paramType.isAssignableFrom(callerParamType)) {
                        throw new TypeMismatchException(
                            paramType, 
                            params.get(i),
                            operation, 
                            operationName);
                    }
                }
                
                return method;
            }
        }

        return null;
    }

    private Object invokeOperationMethod(Method method, Object[] params)
    {
        try {
            return method.invoke(this, params);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static class LRUHashMap<K, V> extends LinkedHashMap<K, V>
    {
        private static final long serialVersionUID = -1017977801627995410L;

        private final int limit;
        
        public LRUHashMap(int limit)
        {
            super(16, 0.75f, true);
            
            this.limit = limit;
        }
        
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
        {
            return size() > limit;
        }
    }
}

// End RefFeaturedBase.java
