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

/**
 * MofIdTypeMapping represents a mapping of MOF ID to Java type name.
 * 
 * @author Stephan Zuercher
 */
public class MofIdTypeMapping
{
    private long mofId;
    private String tablePrefix;
    private String typeName;
    
    public MofIdTypeMapping()
    {
    }
    
    public long getMofId()
    {
        return mofId;
    }
    
    public void setMofId(long mofId)
    {
        this.mofId = mofId;
    }
    
    public String getTablePrefix()
    {
        return tablePrefix;
    }
    
    public void setTablePrefix(String tablePrefix)
    {
        this.tablePrefix = tablePrefix;
    }
    
    public String getTypeName()
    {
        return typeName;
    }
    
    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }
    
    public boolean equals(Object other)
    {
        MofIdTypeMapping that = (MofIdTypeMapping)other;
        
        return this.getMofId() == that.getMofId();
    }
    
    public int hashCode()
    {
        long localMofId = this.getMofId();
        return (int)(localMofId >>> 32) ^ (int)localMofId;
    }
}

// End MofIdTypeMapping.java
