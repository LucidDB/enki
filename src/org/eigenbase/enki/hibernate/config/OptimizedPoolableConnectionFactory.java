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
package org.eigenbase.enki.hibernate.config;

import org.apache.commons.dbcp.*;
import org.apache.commons.pool.*;

import java.sql.*;

/**
 * OptimizedPoolableConnectionFactory overrides {@link
 * PoolableConnectionFactory} to fix the passivate autocommit issue left
 * unresolved in <a
 * href="http://issues.apache.org/jira/browse/DBCP-102">DBCP-102</a>.
 *
 * @author John Sichi
 * @version $Id$
 */
public class OptimizedPoolableConnectionFactory
    extends PoolableConnectionFactory
{
    public OptimizedPoolableConnectionFactory(
        ConnectionFactory connFactory,
        ObjectPool pool,
        KeyedObjectPoolFactory stmtPoolFactory,
        String validationQuery,
        boolean defaultReadOnly,
        boolean defaultAutoCommit)
    {
        super(
            connFactory, pool, stmtPoolFactory, validationQuery,
            defaultReadOnly, defaultAutoCommit);
    }

    public void passivateObject(Object obj)
        throws Exception
    {
        if (obj instanceof Connection) {
            Connection conn = (Connection) obj;
            if (!conn.getAutoCommit() && !conn.isReadOnly()) {
                conn.rollback();
            }
            conn.clearWarnings();
        }
        DbcpTrojan.passivateDelegatingConnection(obj);
    }
}

// End OptimizedPoolableConnectionFactory.java
