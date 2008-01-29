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

import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import javax.jmi.reflect.*;

import org.hibernate.*;
import org.hibernate.usertype.*;

/**
 * EnumUserType is a Hibernate {@link UserType} that converts JMI enumerations
 * to string value for storage via Hibernate.
 * 
 * @author Stephan Zuercher
 */
public class EnumUserType 
    implements UserType, org.hibernate.usertype.ParameterizedType
{
    public static final String ENUM_CLASS_PARAM = "class";
    
    private static final int[] TYPES = new int[] { Types.VARCHAR };

    private Class<? extends RefEnum> returnedClass;
    private Method forNameMethod;

    private String returnedClassName;
    
    public Object assemble(Serializable cached, Object owner)
        throws HibernateException
    {
        return typedAssemble((String)cached, owner);
    }
    
    private RefEnum typedAssemble(String cached, Object owner)
        throws HibernateException
    {
        if (cached == null) {
            return null;
        }

        String literal = (String)cached;
        
        int pos = literal.indexOf('#');
        if (pos < 0 || pos + 1 >= literal.length()) {
            throw new HibernateException(
                "Cannot parse enum literal '" + literal + "'");
        }

        if (returnedClassName.length() != pos ||
            !returnedClassName.regionMatches(0, literal, 0, pos))
        {
            throw new HibernateException(
                "Wrong enumeration: expected '" + returnedClassName + 
                "', got '" + literal.substring(0, pos) + "'");
        }
        
        literal = literal.substring(pos + 1);

        try {
            return returnedClass.cast(forNameMethod.invoke(null, literal));
        } catch (Exception e) {
            throw new HibernateException(e);
        }
    }

    public Serializable disassemble(Object value) throws HibernateException
    {
        return typedDisassemble((RefEnum)value);
    }
    
    private String typedDisassemble(RefEnum value) throws HibernateException
    {
        if (value == null) {
            return null;
        }
        
        StringBuilder b = new StringBuilder();
        b.append(returnedClassName).append('#').append(value.toString());
        return b.toString();
    }

    public Object deepCopy(Object value)
        throws HibernateException
    {
        return value;
    }

    public boolean equals(Object x, Object y)
        throws HibernateException
    {
        if (x == null || y == null) {
            return x == y;
        }
        
        return x.equals(y);
    }

    public int hashCode(Object x)
        throws HibernateException
    {
        if (x == null) {
            return 0;
        }
        
        return x.hashCode();
    }

    public boolean isMutable()
    {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String persisted = rs.getString(names[0]);
        
        return typedAssemble(persisted, owner);
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
        throws HibernateException, SQLException
    {
        String persist = typedDisassemble((RefEnum)value);
        
        st.setString(index, persist);
    }

    public Object replace(Object original, Object target, Object owner)
        throws HibernateException
    {
        return original;
    }

    public Class<?> returnedClass()
    {
        return returnedClass;
    }

    public int[] sqlTypes()
    {
        return TYPES;
    }

    public void setParameterValues(Properties parameters)
    {
        String enumClassName = parameters.getProperty(ENUM_CLASS_PARAM);
        
        try {
            returnedClass = 
                Class.forName(enumClassName).asSubclass(RefEnum.class);
            returnedClassName = returnedClass.getName();

            forNameMethod = returnedClass.getMethod("forName", String.class);
        } catch (Exception e) {
            throw new HibernateException(e);
        }
        
        if (!forNameMethod.getReturnType().isAssignableFrom(returnedClass)) {
            throw new HibernateException(
                "Cannot assign '" + returnedClassName + 
                "' to '" + forNameMethod.getReturnType().getName() + 
                "' (return type of '" + forNameMethod.getDeclaringClass() + 
                "." + forNameMethod.getName() + "')");
        }
    }
}

// End EnumUserType.java
