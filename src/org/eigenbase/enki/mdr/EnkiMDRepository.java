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

import java.io.*;
import java.util.*;

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
     * Retrieves the {@link MdrProvider} enumeration literal that describes
     * this EnkiMDRepository implementation.
     * 
     * @return an MdrProvider enumeration literal
     */
    public MdrProvider getProviderType();
    
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
     * Deletes the given collection of {@link RefObject} instances.  This
     * method is functionally equivalent to iterating over the given collection
     * and invoking {@link RefObject#refDelete()} on each object.  Note, 
     * however, that the actual order of deletion and order of events fired
     * may not correspond to the iterator-and-delete pattern described.
     * Duplication of objects or providing both composite owners and their 
     * owned objects may cause exceptions.
     * 
     * @param objects the objects to delete
     */
    public void delete(Collection<RefObject> objects);

    /**
     * Causes PLANNED events to be fired similar to those from invoking {@link
     * RefObject#refDelete} on <code>obj</code>, but without actually making
     * any changes to repository state.  Not all repository implementations
     * support this call (those that do not will throw {@link
     * UnsupportedOperationException}); {@link #supportsPreviewRefDelete} can
     * be used to check for support.
     *
     *<p>
     *
     * Events will only be delivered to pre-change listeners; no
     * asynchronous post-change listener events will be enqueued
     * since no change is actually taking place.  Nor will
     * change cancellation events be delivered.
     *
     *<p>
     *
     * One other difference from the events delivered by refDelete is that the
     * preview may contain additional "echo" association removal events for
     * composite associations.  If A contains B, and refDelete is called on A,
     * then only a single associational removal event with end references (A,
     * B) will be delivered.  In contrast, for a preview deletion of A,
     * two events will be delivered; one with end references (A, B), and one
     * with end references (B, A).
     *
     *<p>
     *
     * This call requires a write transaction (same as refDelete) even though
     * it does not actually make any changes to the repository.
     *
     * @param obj object for which deletion effect should be previewed
     */
    public void previewRefDelete(RefObject obj);

    /**
     * Checks whether the repository implementation supports
     * {@link #previewRefDelete}.
     *
     * @return true if the repository implementation supports
     * deletion preview, false otherwise
     */
    public boolean supportsPreviewRefDelete();
    
    /**
     * Retrieves the annotation for the given extent.  Must be executed in
     * a repository write transaction.
     * 
     * @param extentName extent name
     * @return the extent's annotation, or null if non was set.
     */
    public String getAnnotation(String extentName);

    /**
     * Annotates the given extent.
     * 
     * @param extentName name of the extent to annotate
     * @param annotation the annotation (may be null)
     */
    public void setAnnotation(String extentName, String annotation);
    
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
    
    /**
     * Backs up the given extent, writing a single file to the given output 
     * stream. The file written may be a multi-file archive, such as a ZIP, 
     * JAR or TAR file. Typically the file is uncompressed, allowing the 
     * caller to apply their choice of compression.
     *
     * @param extentName name of the extent to back up
     * @param stream an OutputStream to which a single file's data is written
     * @throws EnkiBackupFailedException if there is an error writing to the
     *                                   stream or reading from the repository
     */
    public void backupExtent(String extentName, OutputStream stream) 
        throws EnkiBackupFailedException;

    /**
     * Restores the contents of the given input stream to the named extent. 
     * The input stream is assumed to contain a single file's data.  The input 
     * stream's close method is <b>not</b> called, thereby allowing the extent 
     * to be restored from a single entry in a multi-file archive, such as a 
     * ZIP, JAR or TAR file.
     *
     * @param extentName name of the extent to be created and restore to
     * @param metaModelExtentName name of the extent describing the
     *                            new extent's metamodel
     * @param metaPackageName name of the root package for the new extent
     * @throws EnkiRestoreFailedException if there is an error reading from the 
     *                                    stream or writing to the repository
     */
    public void restoreExtent(
        String extentName,
        String metaModelExtentName,
        String metaPackageName,
        InputStream stream) 
    throws EnkiRestoreFailedException;
    
    /**
     * Configures a special {@link InputStream} that modifies XMI-based 
     * repository data as it is being restored.  Longer term, applications
     * should not store binary data (particularly control characters) in 
     * repository string attributes.  In the near term, applications may
     * dynamically detect and modify or filter such characters at the 
     * InputStream (not XML) level.
     * 
     * <p>The given class must have a constructor that takes a single
     * InputStream as a parameter.
     * 
     * <p>Implementations which do not store XMI in their backups may ignore
     * calls to this method.
     * 
     * @param cls class representing a filtering InputStream
     */
    @Deprecated
    public void setRestoreExtentXmiFilter(Class<? extends InputStream> cls);
    
    /**
     * Retrieves a copy of this repository's storage properties.
     * 
     * @return a copy of this repository's storage properties
     */
    public Properties getStorageProperties();
}

// End EnkiMDRepository.java
