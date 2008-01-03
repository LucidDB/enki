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
package org.eigenbase.enki.hibernate.storage;

import java.lang.reflect.*;
import java.util.*;

import org.eigenbase.enki.util.*;
import org.hibernate.*;
import org.hibernate.engine.*;
import org.hibernate.property.*;

/**
 * BooleanPropertyAccessor maps a Hibernate property name to Hibernate Getter
 * and Setting instances that represent the JMI method names.
 * 
 * <p>By default Hibernate assumes that the methods for a boolean property 
 * named "isDefault" are <code>getIsDefault()</code> and 
 * <code>setIsDefault(boolean)</code>, but the JMI specification causes them 
 * to be generated as <code>isDefault()</code> and 
 * <code>setDefault(boolean)</code>.
 *  
 * @author Stephan Zuercher
 */
public class BooleanPropertyAccessor
    implements PropertyAccessor
{
    @SuppressWarnings("unchecked")
    public Getter getGetter(Class cls, String propertyName)
        throws PropertyNotFoundException
    {
        return new BooleanGetter(cls, propertyName);
    }

    @SuppressWarnings("unchecked")
    public Setter getSetter(Class cls, String propertyName)
        throws PropertyNotFoundException
    {
        return new BooleanSetter(cls, propertyName);
    }

    private static class BooleanGetter implements Getter
    {
        private static final long serialVersionUID = 5457463552791297004L;

        private final Class<?> cls;
        private final Method method;
        
        private BooleanGetter(Class<?> cls, String propertyName)
            throws PropertyNotFoundException
        {
            this.cls = cls;
            
            String methodName;
            if (propertyName.startsWith("is")) {
                methodName = propertyName;
            } else {
                methodName = "is" + StringUtil.toInitialUpper(propertyName);
            }
            
            try {
                this.method = cls.getDeclaredMethod(methodName, (Class<?>[])null);
            } catch (NoSuchMethodException e) {
                throw new PropertyNotFoundException(propertyName);
            }
            
            Class<?> returnType = this.method.getReturnType();
            if (returnType != Boolean.class && returnType != boolean.class) {
                throw new PropertyNotFoundException(
                    propertyName + ": wrong type");
            }
        }

        public Object get(Object instance) throws HibernateException
        {
            try {
                return method.invoke(instance, (Object[])null);
            } catch(IllegalArgumentException e) {
                throw new PropertyAccessException(
                    e,
                    "IllegalArgumentException while calling",
                    false,
                    cls,
                    method.getName());
            } catch(IllegalAccessException e) {
                throw new PropertyAccessException(
                    e,
                    "IllegalAccessException while calling",
                    false,
                    cls,
                    method.getName());
            } catch(InvocationTargetException e) {
                throw new PropertyAccessException(
                    e,
                    "Exception while calling",
                    false,
                    cls,
                    method.getName());
            }
        }

        @SuppressWarnings("unchecked")
        public Object getForInsert(
            Object owner, Map mergeMap, SessionImplementor session)
        throws HibernateException
        {
            return get(owner);
        }

        public Method getMethod()
        {
            return method;
        }

        public String getMethodName()
        {
            return method.getName();
        }

        @SuppressWarnings("unchecked")
        public Class getReturnType()
        {
            return method.getReturnType();
        }
    }
    
    private static class BooleanSetter implements Setter
    {
        private static final long serialVersionUID = -2781339158649399642L;

        private final Class<?> cls;
        private final Method method;
        private final String propertyName;

        private BooleanSetter(Class<?> cls, String propertyName)
            throws PropertyNotFoundException
        {
            this.cls = cls;
            this.propertyName = propertyName;
            
            String methodName;
            if (propertyName.startsWith("is")) {
                methodName = "set" + propertyName.substring(2);
            } else {
                methodName = "set" + StringUtil.toInitialUpper(propertyName);
            }
            
            Class<?>[] booleanTypes = { Boolean.class, boolean.class };
            Method method = null;
            for(Class<?> paramType: booleanTypes) {
                try {
                    method = cls.getDeclaredMethod(methodName, paramType);
                } catch (NoSuchMethodException e) {
                    // ignored
                }
            }
            
            if (method == null) {
                throw new PropertyNotFoundException(propertyName);
            }
            
            this.method = method;
        }

        public Method getMethod()
        {
            return method;
        }

        public String getMethodName()
        {
            return method.getName();
        }

        public void set(
            Object instance, 
            Object value,
            SessionFactoryImplementor sessionFactory)
        throws HibernateException
        {
            try {
                method.invoke(instance, value);
            }
            catch(InvocationTargetException e) {
                throw new PropertyAccessException(
                    e,
                    "Exception while calling",
                    true,
                    cls,
                    propertyName);                                        
            }
            catch(Exception e) {
                if (value == null && 
                    method.getParameterTypes()[0].isPrimitive())
                {
                    throw new PropertyAccessException(
                        e,
                        "Null value assigned to property of primitive type",
                        true,
                        cls,
                        propertyName);                        
                } else {
                    throw new PropertyAccessException(
                        e,
                        e.getClass().getSimpleName() + " while calling",
                        true,
                        cls,
                        propertyName);
                }
            }
        }
    }
}

// End BooleanPropertyAccessor.java
