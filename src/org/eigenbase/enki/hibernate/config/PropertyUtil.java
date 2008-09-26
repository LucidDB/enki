/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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
package org.eigenbase.enki.hibernate.config;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

/**
 * PropertyUtil provides static utilities methods for manipulating 
 * {@link Properties} objects.
 * 
 * @author Stephan Zuercher
 */
public class PropertyUtil
{
    private PropertyUtil()
    {
    }
    
    public static <T> T readStorageProperty(
        Properties storageProperties,
        Logger log,
        String name,
        T defaultValue,
        Class<T> cls)
    {
        String stringValue = storageProperties.getProperty(name);
        if (stringValue == null) {
            return defaultValue;
        }
        
        try {
            Constructor<T> cons = cls.getConstructor(String.class);
            
            return cons.newInstance(stringValue);
        } catch (Exception e) {
            log.log(
                Level.SEVERE, 
                "Error parsing storage property (" + name + "=[" + stringValue
                + "], " + cls.getName() + ")",
                e);
            return defaultValue;
        }
    }    
}

// End PropertyUtil.java
