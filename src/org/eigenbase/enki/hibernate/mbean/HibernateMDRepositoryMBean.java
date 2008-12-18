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
package org.eigenbase.enki.hibernate.mbean;

import java.util.*;

import javax.management.openmbean.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mbean.*;
import org.eigenbase.enki.mdr.*;
import org.hibernate.*;
import org.hibernate.stat.*;

/**
 * HibernateMDRepositoryMBean extents {@link EnkiRepository} to support
 * management of Enki/Hibernate repositories.
 * 
 * @author Stephan Zuercher
 */
class HibernateMDRepositoryMBean extends EnkiRepository
{
    private static final String statsKeyHeader = "statistic";
    private static final String statsValueHeader = "value";
    private static final String statsDesc = 
        "repository performance statistics";
    
    private final HibernateMDRepository repos;
    
    HibernateMDRepositoryMBean(HibernateMDRepository repos)
    {
        this.repos = repos;
    }
    
    @Override
    protected EnkiMDRepository getRepos()
    {
        return repos;
    }

    public void enablePerformanceStatistics()
    {
        SessionFactory sessionFactory = repos.getSessionFactory();

        sessionFactory.getStatistics().setStatisticsEnabled(true);
    }
    
    public void disablePerformanceStatistics()
    {
        SessionFactory sessionFactory = repos.getSessionFactory();

        sessionFactory.getStatistics().setStatisticsEnabled(false);
    }
    
    public TabularData getPerformanceStatistics() throws Exception
    {
        SessionFactory sessionFactory = repos.getSessionFactory();
        
        Statistics stats = sessionFactory.getStatistics();
        
        LinkedHashMap<String, Object> data = loadStats(stats);

        return EnkiMBeanUtil.tabularDataFromMap(
            data,
            statsDesc,
            statsKeyHeader,
            statsValueHeader);
    }

    private LinkedHashMap<String, Object> loadStats(Statistics stats)
    {
        LinkedHashMap<String, Object> data = 
            new LinkedHashMap<String, Object>();
        data.put("start time", stats.getStartTime());
        data.put("sessions opened", stats.getSessionOpenCount());
        data.put("sessions closed", stats.getSessionCloseCount());
        data.put("transactions", stats.getTransactionCount());
        data.put(
            "successful transactions", stats.getSuccessfulTransactionCount());
        data.put(
            "optimistic lock failures", stats.getOptimisticFailureCount());
        data.put("flushes", stats.getFlushCount());
        data.put("statements prepared", stats.getPrepareStatementCount());
        data.put("statements closed", stats.getCloseStatementCount());
        data.put(
            "second level cache puts", stats.getSecondLevelCachePutCount());
        data.put(
            "second level cache hits", stats.getSecondLevelCacheHitCount());
        data.put(
            "second level cache misses", stats.getSecondLevelCacheMissCount());
        data.put("entities loaded", stats.getEntityLoadCount());
        data.put("entities updated", stats.getEntityUpdateCount());
        data.put("entities inserted", stats.getEntityInsertCount());
        data.put("entities deleted", stats.getEntityDeleteCount());
        data.put(
            "entities fetched (minimize this)", stats.getEntityFetchCount());
        data.put("collections loaded", stats.getCollectionLoadCount());
        data.put("collections updated", stats.getCollectionUpdateCount());
        data.put("collections removed", stats.getCollectionRemoveCount());
        data.put("collections recreated", stats.getCollectionRecreateCount());
        data.put(
            "collections fetched (minimize this)", 
            stats.getCollectionFetchCount());
        data.put(
            "queries executed to database", stats.getQueryExecutionCount());
        data.put("query cache puts", stats.getQueryCachePutCount());
        data.put("query cache hits", stats.getQueryCacheHitCount());
        data.put("query cache misses", stats.getQueryCacheMissCount());
        data.put("max query time (ms)", stats.getQueryExecutionMaxTime());
        
        for(String cacheRegion: stats.getSecondLevelCacheRegionNames()) {
            SecondLevelCacheStatistics cacheStats = 
                stats.getSecondLevelCacheStatistics(cacheRegion);
            data.put(
                "cache region: " + cacheRegion + ": elements",
                cacheStats.getElementCountInMemory());
        }
        
        return data;
    }
}

// End HibernateMDRepositoryMBean.java
