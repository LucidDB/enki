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
public abstract class RefFeaturedBase 
    extends RefBaseObjectBase 
    implements RefFeatured
{
    protected RefFeaturedBase()
    {
        super();
    }
    
    public Object refGetValue(RefObject type)
    {
        Method method = findMethod(type, null, "get", 0);
        
        if (method != null) {
            return invokeMethod(Object.class, this, method);
        }
        
        throw new InvalidCallException(this, type);
    }

    public Object refGetValue(String typeName)
    {
        Method method = findMethod(null, typeName, "get", 0);
        
        if (method != null) {
            return invokeMethod(Object.class, this, method);
        }
        
        throw new InvalidNameException(typeName);
    }

    @SuppressWarnings("unchecked")
    public Object refInvokeOperation(RefObject operation, List params)
        throws RefException
    {
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
        Method method = findOperationMethod(null, operationName, params);
        
        if (method != null) {
            return invokeOperationMethod(method, params.toArray());
        }
        
        throw new InvalidNameException(operationName);
    }

    public void refSetValue(RefObject type, Object value)
    {
        Method method = findMethod(type, null, "set", 1);
        
        if (method != null&& isMutable(method, value.getClass())) {
            invokeMethod(Void.class, this, method, value);
            return;
        }
        
        throw new InvalidCallException(this, type);
    }

    public void refSetValue(String typeName, Object value)
    {
        Method method = findMethod(null, typeName, "set", 1);
        
        if (method != null&& isMutable(method, value.getClass())) {
            invokeMethod(Void.class, this, method, value);
            return;
        }
        
        throw new InvalidNameException(typeName);
    }

    private Method findMethod(
        RefObject type, String typeName, String prefix, int numParams)
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
        
        String methodName = 
            prefix +
            Character.toUpperCase(typeName.charAt(0)) +
            typeName.substring(1);
        
        Class<?> cls = getClass();
        do {
            Method[] methods = cls.getDeclaredMethods();

            for(Method method: methods) {
                if (method.getName().equals(methodName) &&
                    method.getParameterTypes().length == numParams)
                {
                    return method;
                }
            }
            
            cls = cls.getSuperclass();
        } while(cls != RefFeaturedBase.class);

        return null;
    }

    private boolean isMutable(Method method, Class<?> valueType)
    {
        Class<?>[] paramTypes = method.getParameterTypes();
        assert(paramTypes.length == 1);

        // Value type must by assignable to the methods single parameter
        Class<?> paramType = paramTypes[0];
        return 
            paramType.isAssignableFrom(valueType) &&
            !Collection.class.isAssignableFrom(paramType);
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
}

// End RefFeaturedBase.java
