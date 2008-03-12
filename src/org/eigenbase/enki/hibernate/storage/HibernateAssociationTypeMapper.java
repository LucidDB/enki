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
package org.eigenbase.enki.hibernate.storage;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.hibernate.*;
import org.hibernate.usertype.*;

/**
 * HibernateAssociationTypeMapper is a base class for Hibernate meta-type
 * mapping.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateAssociationTypeMapper implements UserType
{
    /**
     * Map a concrete subclass of HibernateAssociationTypeMapper to a 
     * bi-directional mapping of classes and type names.
     */
    private static 
        Map<Class<? extends HibernateAssociationTypeMapper>, Mapper> 
        metamodelToMappings =
            new HashMap<
                Class<? extends HibernateAssociationTypeMapper>, Mapper>();
    
    private static final int[] TYPES = { Types.VARCHAR }; 
    
    private final Mapper mapper;
    
    protected HibernateAssociationTypeMapper()
    {
        this.mapper = initializeMapper(getClass());
    }
    
    private static Mapper initializeMapper(
        Class<? extends HibernateAssociationTypeMapper> cls)
    {
        Mapper mapper = metamodelToMappings.get(cls);
        if (mapper != null) {
            return mapper;
        }

        mapper = new Mapper();
        
        metamodelToMappings.put(cls, mapper);

        return mapper;
    }

    public void register(Class<? extends RefObject> cls, String mappedName)
    {
        String clsName = cls.getName();
        String oldMappedName = mapper.clsToNameMap.put(clsName, mappedName);
        String oldClsName = mapper.nameToClsMap.put(mappedName, cls.getName());
        
        if (oldClsName == null && oldMappedName == null) {
            return;
        }
        
        if (oldClsName == null) {
            throw new EnkiHibernateException(
                "Mapping of " + clsName + " to " + mappedName + " failed: " + 
                clsName + " was already mapped to " + oldMappedName);
        }
        
        if (oldMappedName == null) {
            throw new EnkiHibernateException(
                "Mapping of " + clsName + " to " + mappedName + " failed: " + 
                mappedName + " was already reversed mapped to " + oldClsName);
        }
        
        if (clsName.equals(oldClsName) && mappedName.equals(oldMappedName)) {
            // Equality means that the same mapping was applied twice, which
            // is fine.
            return;
        }
        
        throw new EnkiHibernateException(
            "Mapping of " + clsName + " to " + mappedName + " failed: " + 
            clsName + " was already mapped to " + oldMappedName + "; " +
            mappedName + " was already reversed mapped to " + oldClsName);
    }
    
    // implement UserType
    public Object assemble(Serializable mappedName, Object owner)
        throws HibernateException
    {
        return mapper.findClassName((String)mappedName);
    }

    // implement UserType
    public Object deepCopy(Object value) throws HibernateException
    {
        return value;
    }

    // implement UserType
    @SuppressWarnings("unchecked")
    public Serializable disassemble(Object clsName) throws HibernateException
    {
        return mapper.findMappedName((String)clsName);
    }

    // implement UserType
    public boolean equals(Object mappedName1, Object mappedName2)
        throws HibernateException
    {
        if (mappedName1 == mappedName2) {
            return true;
        }
        
        if (mappedName1 == null || mappedName2 == null) {
            return false;
        }
        
        return mappedName1.equals(mappedName2);
    }

    // implement UserType
    public int hashCode(Object mappedName) throws HibernateException
    {
        return mappedName.hashCode();
    }

    // implement UserType
    public boolean isMutable()
    {
        return false;
    }

    // implement UserType
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String mappedName = (String)Hibernate.STRING.nullSafeGet(rs, names[0]);
        String clsName = null;
        if (mappedName != null) { 
            clsName = mapper.findClassName(mappedName);
        }
        
        return clsName;
    }

    // implement UserType
    @SuppressWarnings("unchecked")
    public void nullSafeSet(PreparedStatement stmt, Object value, int index)
        throws HibernateException, SQLException
    {
        String clsName = (String)value;
        String mappedName = null;
        if (clsName != null) {
            mappedName = mapper.findMappedName(clsName);
        }
        
        Hibernate.STRING.nullSafeSet(stmt, mappedName, index); 
    }

    // implement UserType
    public Object replace(Object original, Object target, Object owner)
        throws HibernateException
    {
        return original;
    }

    // implement UserType
    public Class<?> returnedClass()
    {
        return String.class;
    }

    // implement UserType
    public int[] sqlTypes()
    {
        return TYPES;
    }

    private static class Mapper
    {
        private final Map<String, String> clsToNameMap;
        private final Map<String, String> nameToClsMap;
        
        private Mapper()
        {
            this.clsToNameMap = new HashMap<String, String>();
            this.nameToClsMap = new HashMap<String, String>();
        }
        
        public String findClassName(String name)
        {
            return nameToClsMap.get(name);
        }
        
        public String findMappedName(String cls) 
        {
            return clsToNameMap.get(cls);
        }
    }
}

// End HibernateAssociationTypeMapper.java
