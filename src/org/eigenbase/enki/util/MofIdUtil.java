/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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

/**
 * MofIdUtil provides utilities for manipulating Enki-style MOF ID strings.
 * 
 * @author Stephan Zuercher
 */
public class MofIdUtil
{
    private static final char[] HEX_DIGITS = { 
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
    };
    
    private MofIdUtil()
    {
    }
    
    /** Width of a formatted MOF ID. */
    private static final int MOFID_WIDTH = 18;

    /** Constant prefix for formatted MOF IDs. */
    private static final String MOFID_PREFIX = "j:";

    /**
     * Generates a formatted MOF ID from the given long value.  The formatted 
     * MOF ID is always exactly {@link #MOFID_WIDTH} characters long and 
     * always begins with {@link #MOFID_PREFIX}.
     * 
     * @param mofId MOF ID value
     */
    public static String makeMofIdStr(long mofId)
    {
        char[] buf = new char[16];
        int pos = 15;
        do {
            buf[pos--] = HEX_DIGITS[(int)(mofId & 0xF)];
            mofId >>>= 4;
        } while(mofId != 0);
        
        while(pos >= 0) {
            buf[pos--] = HEX_DIGITS[0];
        }
                
        StringBuilder b = 
            new StringBuilder(MOFID_WIDTH)
            .append(MOFID_PREFIX)
            .append(buf);
        
        return b.toString();
    }
    
    /**
     * Parses a formatted MOF ID into a long value.  The formatted 
     * MOF ID must be exactly {@link #MOFID_WIDTH} characters long and 
     * must begin with {@link #MOFID_PREFIX}.
     * 
     * @param mofIdStr a formatted MOF ID
     * @return the long representation of the formatted MOF ID
     */
    public static long parseMofIdStr(String mofIdStr)
    {
        if (mofIdStr == null) {
            throw new NullPointerException("null MOF ID");
        }

        if (mofIdStr.length() != MOFID_WIDTH || 
            !mofIdStr.startsWith(MOFID_PREFIX))
        {
            throw new IllegalArgumentException("Invalid MOF ID: " + mofIdStr);
        }

        try {
            return Long.parseLong(mofIdStr.substring(2), 16);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid MOF ID: " + mofIdStr, e);
        }
    }

}

// End MofIdUtil.java
