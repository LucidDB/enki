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
     * Starts a repository session, which may be comprised of zero or more
     * repository transactions.  If this method is not invoked before the
     * {@link #beginTrans(boolean)} method on a particular thread, a session 
     * is started automatically and ended when the transaction is ended.
     * 
     * @see MDRepository#beginTrans(boolean)
     */
    public void beginSession();

    /**
     * Ends a repository session.
     * 
     * @see #beginSession()
     */
    public void endSession();
    
    /**
     * Detaches the current session, if any, from this repository.  If there is
     * no currently active session or the repository implementation does not
     * support sessions, null is returned.
     * 
     * @return detached repository session
     */
    public EnkiMDSession detachSession();

    /**
     * Re-attaches a repository session previously
     * {@link #detachSession() detached} from this repository.  Sessions
     * <b>are not</b> transferable across repositories.  This repository
     * must not have any currently active repository sessions.  The session
     * parameter may be null, which produces no operation.  This simplifies
     * usage since callers of {@link #detachSession()} need not distinguish
     * between the no current session and sessions not supported cases. 
     * 
     * @param session an {@link EnkiMDSession} previously detached via
     *                {@link #detachSession()}
     */
    public void reattachSession(EnkiMDSession session);

    /**
     * Ends a detached repository session.
     *
     * <p>The effect is similar to calling {@link #reattachSession} followed
     * by {@link #endSession}, except that there is not an error if there is
     * a current session.
     *
     * <p>Useful when the session to be closed was detached by a different
     * thread and the current thread already has a session open.
     *
     * @param session an {@link EnkiMDSession} previously detached via
     *                {@link #detachSession()}
     */
    public void endDetachedSession(EnkiMDSession session);

    /**
     * Looks up a {@link RefObject} with the given MOF ID and type.  Some
     * repositories may perform this operation more efficiently than the
     * less specific {@link MDRepository#getByMofId(String)}.
     * 
     * @param mofId MOF ID of an object with the given {@link RefClass}
     * @param cls non-abstract {@link RefClass} that has an instance with
     *            the given MOF ID
     * @return a RefObject or null if not found
     */
    public RefObject getByMofId(String mofId, RefClass cls);
    
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
