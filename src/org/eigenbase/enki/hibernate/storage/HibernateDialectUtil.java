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

import org.hibernate.dialect.*;

/**
 * HibernateDialectUtil provides static utility methods for use with 
 * Hibernate's Dialect class.
 * 
 * @author Stephan Zuercher
 */
public class HibernateDialectUtil
{
    private HibernateDialectUtil()
    {
    }
    
    /**
     * Quote the given identifier using a Hibernate {@link Dialect} object.
     * Unlike, {@link Dialect#quote(String)} this method does not require
     * that the string passed be quoted in Hibernate-style backticks.
     * 
     * @param dialect Hibernate dialect
     * @param identifier identifier to quote
     * @return quoted identifier
     */
    public static String quote(Dialect dialect, String identifier)
    {
        StringBuilder result = new StringBuilder();
        result
            .append(dialect.openQuote())
            .append(identifier)
            .append(dialect.closeQuote());
        return result.toString();
    }
}

// End HibernateDialectUtil.java
