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
package org.eigenbase.enki.netbeans.mbean;

import org.eigenbase.enki.mbean.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.netbeans.*;

/**
 * NBMDRepositoryMBean extends {@link EnkiRepository} to support
 * management of Enki/Netbeans repositories.  Note that this implementation
 * does not override {@link #getPerformanceStatistics()} as the Netbeans
 * repository does not collect any performance statistics.
 * 
 * @author Stephan Zuercher
 */
public class NBMDRepositoryMBean extends EnkiRepository
{
    private final NBMDRepositoryWrapper repos;
    
    NBMDRepositoryMBean(NBMDRepositoryWrapper repos)
    {
        this.repos = repos;
    }
    
    @Override
    protected EnkiMDRepository getRepos()
    {
        return repos;
    }
}

// End NBMDRepositoryMBean.java