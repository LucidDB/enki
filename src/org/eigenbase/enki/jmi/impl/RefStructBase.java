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

import javax.jmi.reflect.*;

/**
 * RefStructBase implements the JMI interface {@link RefStruct}.
 * 
 * @author Stephan Zuercher
 */
public abstract class RefStructBase implements RefStruct
{
    @SuppressWarnings("unchecked")
    public List refFieldNames()
    {
        Field[] fields = getClass().getDeclaredFields();
        ArrayList<String> fieldNames = new ArrayList<String>();
        for(Field field: fields) {
            fieldNames.add(field.getName());
        }
        
        return fieldNames;
    }

    public Object refGetValue(String fieldName)
    {
        Field field;
        try {
            field = getClass().getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new InvalidNameException(fieldName);
        }
        
        try {
            return field.get(this);
        } catch (IllegalAccessException e) {
            throw new InvalidNameException(fieldName);
        }
    }

    @SuppressWarnings("unchecked")
    public List refTypeName()
    {
        // REVIEW: Is this an acceptable implementation?
        String fullyQualifiedName = getClass().getName();
        
        String[] parts = fullyQualifiedName.split("\\.");
        
        return Arrays.asList(parts);        
    }
}

// End RefStructBase.java
