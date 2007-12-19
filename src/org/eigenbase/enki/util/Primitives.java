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
package org.eigenbase.enki.util;

import java.util.*;

/**
 * Primitives provides utility methods related to Java primitive types and
 * their wrapper classes.
 * 
 * @author Stephan Zuercher
 */
public class Primitives
{
    private static final Class<?>[][] classes = {
        { Integer.class,   int.class },
        { Long.class,      long.class },
        { Float.class,     float.class },
        { Double.class,    double.class },
        { Short.class,     short.class },
        { Character.class, char.class },
        { Boolean.class,   boolean.class },
    };
    
    private static final Map<Class<?>, Class<?>> primitiveToWrapperMap;
    private static final Map<Class<?>, Class<?>> wrapperToPrimitiveMap;
    
    // conversion table between object type names and primitive type names
    private static final Map<String, String> primitiveTypeNameMap;

    static {
        HashMap<Class<?>, Class<?>> toPrimitiveMap = 
            new HashMap<Class<?>, Class<?>>();
        HashMap<Class<?>, Class<?>> toWrapperMap = 
            new HashMap<Class<?>, Class<?>>();
        HashMap<String, String> typeNameMap = new HashMap<String, String>();
        
        for(Class<?>[] entry: classes) {
            Class<?> wrapper = entry[0];
            Class<?> primitive = entry[1];

            toPrimitiveMap.put(wrapper, primitive);
            toWrapperMap.put(primitive, wrapper);
            
            // REVIEW: SWZ: 11/7/2007: See review comment in 
            // GeneratorBase.getTypeName(ModelElement, String).
            typeNameMap.put(wrapper.getName(), primitive.getName());
            typeNameMap.put(wrapper.getSimpleName(), primitive.getName());
        }

        primitiveToWrapperMap = Collections.unmodifiableMap(toWrapperMap);
        wrapperToPrimitiveMap = Collections.unmodifiableMap(toPrimitiveMap);
        
        primitiveTypeNameMap = Collections.unmodifiableMap(typeNameMap);
    }
    
    private Primitives()
    {
    }
    
    /**
     * Gets the wrapper class associated with the given primitive type.
     * 
     * @param primitiveType a primitive type
     * @return the wrapper associated with primitiveType or null if 
     *         primitiveType is not primitive
     */
    public static Class<?> getWrapper(Class<?> primitiveType)
    {
        return primitiveToWrapperMap.get(primitiveType);
    }

    /**
     * Gets the primitive class associated with the given wrapper type.
     * 
     * @param wrapperType a wrapper type
     * @return the primitive type associated with wrapperType or null if 
     *         wrapperType is not a wrapper
     */
    public static Class<?> getPrimitive(Class<?> wrapperType)
    {
        return wrapperToPrimitiveMap.get(wrapperType);
    }
    
    /**
     * Gets the Java keyword identifying the primitive type associated with
     * the given wrapper type name.  Wrapper type names need not be fully
     * qualified.  For example, passing "Integer" produces the same result
     * as passing "java.lang.Integer".
     * 
     * @param wrapperTypeName wrapper type name (e.g. "java.lang.Short")
     * @return the Java keyword representing the primitive type corresponding
     *         to the wrapper type name.
     */
    public static String convertTypeNameToPrimitive(String wrapperTypeName)
    {
        return primitiveTypeNameMap.get(wrapperTypeName);
    }
}

// End Primitives.java
