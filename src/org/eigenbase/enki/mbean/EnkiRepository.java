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
package org.eigenbase.enki.mbean;

import java.util.*;

import javax.management.openmbean.*;

import org.eigenbase.enki.mdr.*;

/**
 * EnkiRepository represents an {@link EnkiMDRepository} in JMX.
 * 
 * @author Stephan Zuercher
 */
public abstract class EnkiRepository implements EnkiRepositoryMBean
{
    protected abstract EnkiMDRepository getRepos();

    /**
     * Obtains the given extent's annotation.
     * 
     * @return the extent's annotation, or null if the extent has no annotation
     *         of if the extent does not exist
     */
    public String getExtentAnnotation(String extent)
    {
        EnkiMDRepository repos = getRepos();
        repos.beginSession();
        try {
            repos.beginTrans(false);
            try {
                return repos.getAnnotation(extent);
            } finally {
                repos.endTrans();
            }
        } finally {
            repos.endSession();
        }
    }

    /**
     * Obtains the repository's extent names.
     * 
     * @return an array of repository extent names
     */
    public String[] getExtentNames()
    {
        EnkiMDRepository repos = getRepos();
        repos.beginSession();
        try {
            repos.beginTrans(false);
            try {
                return repos.getExtentNames();
            } finally {
                repos.endTrans();
            }
        } finally {
            repos.endSession();
        }
    }

    /**
     * Returns the repository's provider type.
     * 
     * @return the {@link MdrProvider#toString()} for the repository's 
     *         provider
     */
    public String getProviderType()
    {
        return getRepos().getProviderType().toString();
    }
    
    /**
     * Returns the repository's storage properties as {@link TabularData}.
     * 
     * @return TabularData instance containing all repository storage 
     *         properties
     */
    public TabularData getStorageProperties() throws Exception
    {
        return EnkiMBeanUtil.tabularDataFromStorageProperties(
            getRepos().getStorageProperties());
    }
    
    /**
     * Enables collection of repository performance statistics.  The default
     * implementation is a no-op.
     */
    public void enablePerformanceStatistics()
    {
    }
    
    /**
     * Disables collection of repository performance statistics.  The default
     * implementation is a no-op.
     */
    public void disablePerformanceStatistics()
    {
    }
    
    /**
     * Returns the repository's current performance statistics as 
     * {@link TabularData}.  The default implementation assumes the repository
     * cannot or does not collection performance statistics and returns no 
     * data.
     * 
     * @return empty TablularData instance
     */
    public TabularData getPerformanceStatistics() throws Exception
    {
        return EnkiMBeanUtil.tabularDataFromMap(
            new HashMap<Object, Object>(),
            "statistics values",
            "statistic",
            "value");
    }
}

// End EnkiRepository.java
