/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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

import javax.management.openmbean.*;

import org.eigenbase.enki.mdr.*;

/**
 * EnkiRepositoryMBean represents a metadata repository instance.
 * 
 * @author Stephan Zuercher
 */
public interface EnkiRepositoryMBean
{
    /**
     * Retrieve the name of this repository's underlying implementation.
     * This value is the string representation of an {@link MdrProvider} 
     * value.
     * 
     * @return the name of this repository's underlying implementation
     */
    public String getProviderType();
    
    /**
     * Retrieve this repository's storage properties as tabular data.
     * 
     * @return this repository's storage properties
     * @throws Exception on error
     */
    public TabularData getStorageProperties() throws Exception;
    
    /**
     * Enables performance statistics collection.
     */
    public void enablePerformanceStatistics();
    
    /**
     * Disables performance statistics collection.
     */
    public void disablePerformanceStatistics();
    
    /**
     * Retrieve this repository's statistics as tabular data.  Actual contents
     * depend on the underlying repository implementation.
     *  
     * @return this repository's statistics
     * @throws Exception on error
     */
    public TabularData getPerformanceStatistics() throws Exception;
    
    /**
     * Retrieve the repository's extent names.
     * 
     * @return the repository's extent names.
     */
    public String[] getExtentNames();
    
    /**
     * Retrieve the given extent's annotation.
     * 
     * @param extentName an extent name (see {@link #getExtentNames()}
     * @return the given extent's annotation
     */
    public String getExtentAnnotation(String extentName);
}

// End EnkiRepositoryMBean.java
