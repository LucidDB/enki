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
import java.util.*;
import org.hibernate.*;
import org.hibernate.dialect.*;
import org.hibernate.engine.*;
import org.hibernate.id.*;
import org.hibernate.util.*;
import org.hibernate.type.*;

/**
 * MofIdGenerator extends Hibernate's {@link TableGenerator} to provide a 
 * source of unique MOF ID values. 
 * 
 * @author Stephan Zuercher
 */
public class MofIdGenerator extends TableGenerator
{
    public static final String INTERVAL_PARAM = "interval";

    private static long high = -1L;
    private static int interval = -1;
    private static int subInterval = -1;
    private static Class<?> resultType;

    public void configure(Type type, Properties params, Dialect d)
    {
        super.configure(type, params, d);

        synchronized (MofIdGenerator.class) {
            if (interval <= 0) {
                interval = PropertiesHelper.getInt(
                    INTERVAL_PARAM,
                    params,
                    Short.MAX_VALUE);
                resultType = type.getReturnedClass();
                if (resultType != Long.class) {
                    throw new MappingException(
                        "MofIdGenerator only generates long (64-bit) values");
                }
                
                // REVIEW: SWZ: Oct 29, 2007: The implication here is that if
                // interval is set to its maximum value, up to 2^32 processes
                // may generate MofIds in a particular database before we run
                // out of MofId space because each process gets up to 2^31 ids
                // from 2^32 ranges (for a total of 2^63 MofIds, assuming no
                // waste and that no one process uses more than 2^31 before
                // exiting). The processes may be started concurrently or
                // sequentially, but processes that don't cause a MofId to be
                // generated don't count. Another issue is that any concurrent
                // processes must be configured with the same interval and
                // that the interval can never be reduced. Therefore the
                // interval should probably be stored in the table and
                // validated against the parameter's value before use.
                final int max = Integer.MAX_VALUE;
                if (interval < 2 || interval > max) {
                    throw new MappingException(INTERVAL_PARAM
                        + " parameter must be between 2 and " + max
                        + ", inclusive");
                }
                subInterval = -1;
                high = 0;
            }
        }
    }

    public synchronized Serializable generate(
        SessionImplementor session,
        Object obj)
    throws HibernateException
    {
        synchronized (MofIdGenerator.class) {
            if (subInterval >= interval || subInterval < 0) {
                int nextHigh = (Integer) super.generate(session, obj);

                subInterval = 0;
                high = nextHigh * interval;

                // Skip zero.
                if (high == 0L && subInterval == 0) {
                    subInterval = 1;
                }
            }

            Number result = IdentifierGeneratorFactory.createNumber(high
                + subInterval++, resultType);

            return result;
        }
    }
}

// End MofIdGenerator.java
