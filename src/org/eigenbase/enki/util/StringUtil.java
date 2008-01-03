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

/**
 * StringUtil provides static String utility methods.
 * 
 * @author Stephan Zuercher
 */
public class StringUtil
{
    private StringUtil()
    {
    }

    /**
     * Returns the given string with its first character converted to lower
     * case.
     * 
     * @param str any string
     * @return the given string with its first character converted to lower
     *         case.
     */
    public static String toInitialLower(String str)
    {
        if (str == null) {
            return null;
        }
        
        StringBuilder buf = new StringBuilder();
        int len = str.length();
        if (len > 0) {
            buf.append(Character.toLowerCase(str.charAt(0)));
            if (len > 1) {
                buf.append(str.substring(1));
            }
        }
        
        return buf.toString();
    }

    /**
     * Returns the given string with its first character converted to upper
     * case.
     * 
     * @param str any string
     * @return the given string with its first character converted to upper
     *         case.
     */
    public static String toInitialUpper(String str)
    {
        if (str == null) {
            return null;
        }
        
        StringBuilder buf = new StringBuilder();
        int len = str.length();
        if (len > 0) {
            buf.append(Character.toUpperCase(str.charAt(0)));
            if (len > 1) {
                buf.append(str.substring(1));
            }
        }
        
        return buf.toString();
    }
}

// End StringUtil.java
