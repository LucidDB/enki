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
package org.eigenbase.enki.mdr;

import javax.jmi.reflect.*;

import org.netbeans.api.mdr.*;

/**
 * EnkiMDRepository extends {@link MDRepository} to provide a mechanism for
 * dropping an extent's storage.
 * 
 * @author Stephan Zuercher
 */
public interface EnkiMDRepository extends MDRepository
{
    /**
     * Drops the extent associated with the given top-level package.  If
     * {@link EnkiDropFailedException} is thrown, the extent's storage is
     * left in an indeterminate state.  Do not assume it is fully deleted
     * or that it can still be used.
     * 
     * @param refPackage top-level {@link RefPackage}
     * @throws EnkiDropFailedException if there's an error dropping the extent
     */
    public void dropExtentStorage(RefPackage refPackage)
        throws EnkiDropFailedException;
    
    
    /**
     * Drops the named extent.  If {@link EnkiDropFailedException} is thrown, 
     * the extent's storage is left in an indeterminate state.  Do not assume 
     * it is fully deleted or that it can still be used.
     * 
     * @param extentName name of the extent whose storage is to be dropped 
     * @throws EnkiDropFailedException if there's an error dropping the extent
     */
    public void dropExtentStorage(String extentName) 
        throws EnkiDropFailedException;
    
    /**
     * Tests whether the named extent is built into this repository.  Built-in
     * extents cannot be imported or deleted.
     * 
     * @param extentName extent name to test
     * @return true if the extent is built-in, false otherwise
     */
    public boolean isExtentBuiltIn(String extentName);
    
    /**
     * Retrieves the default {@link ClassLoader} for this repository.
     */
    public ClassLoader getDefaultClassLoader();
}

// End EnkiMDRepository.java
