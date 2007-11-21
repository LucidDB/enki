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


/**
 * @author Stephan Zuercher
 */
public abstract class HibernateObject
{
    /** Width of a formatted MOF ID. See {@link #setRefMofId(long)}. */
    private static final int MOFID_WIDTH = 18;

    /** Constant prefix for formatted MOF IDs. */
    private static final String MOFID_PREFIX = "j:";

    /** 
     * Character array of zeroes used to widen the hexadecimal portion of a 
     * MOF ID to the {@link #MOFID_WIDTH}. 
     */
    private static final char[] ZEROES = new char[] {
        '0', '0', '0', '0', '0', '0', '0', '0', 
        '0', '0', '0', '0', '0', '0', '0', '0',
    };
    
    private long mofId;
    private String mofIdStr;

    public long getMofId()
    {
        return mofId;
    }
    
    public void setMofId(long mofId)
    {
        this.mofId = mofId;

        StringBuilder buf = new StringBuilder(18);
        buf.append(MOFID_PREFIX);
        buf.append(Long.toHexString(mofId));
        int len = buf.length();
        assert(len <= MOFID_WIDTH);
        if (len < MOFID_WIDTH) {
            assert(MOFID_PREFIX.length() + ZEROES.length == MOFID_WIDTH);
            buf.insert(MOFID_PREFIX.length(), ZEROES, 0, MOFID_WIDTH - len);
        }
        this.mofIdStr = buf.toString();
    }
    
    public String refMofId()
    {
        return mofIdStr;
    }
    
    public boolean equals(Object other)
    {
        return mofId == ((HibernateObject)other).getMofId();
    }
    
    public int hashCode()
    {
        return (int)(mofId ^ (mofId >>> 32));
    }
}

// End HibernateObject.java
