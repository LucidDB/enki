/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009-2009 The Eigenbase Project
// Copyright (C) 2009-2009 Disruptive Tech
// Copyright (C) 2009-2009 LucidEra, Inc.
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
package org.apache.commons.dbcp;

/**
 * This is a terrible terrible thing to do.  See {@link
 * org.eigenbase.enki.hibernate.config.OptimizedPoolableConnectionFactory} for
 * why it is necessary.
 *
 * @author John Sichi
 * @version $Id$
 */
public abstract class DbcpTrojan
{
    public static void passivateDelegatingConnection(Object obj)
        throws Exception
    {
        if (obj instanceof DelegatingConnection) {
            ((DelegatingConnection) obj).passivate();
        }
    }
}

// End DbcpTrojan.java
