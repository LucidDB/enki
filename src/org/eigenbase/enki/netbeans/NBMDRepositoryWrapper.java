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
package org.eigenbase.enki.netbeans;

import java.util.logging.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;
import org.netbeans.mdr.*;
import org.netbeans.mdr.handlers.*;
import org.netbeans.mdr.persistence.*;

/**
 * NBMDRepositoryWrapper wraps the Netbeans MDR implementation of 
 * {@link MDRepository} to provide an implementation of 
 * {@link EnkiMDRepository}. 
 * 
 * @author Stephan Zuercher
 */
public class NBMDRepositoryWrapper implements EnkiMDRepository
{
    private final Logger log = 
        Logger.getLogger(NBMDRepositoryWrapper.class.getName());

    private static final ThreadLocalSessionContext tls = 
        new ThreadLocalSessionContext();

    private final NBMDRepositoryImpl impl;
    
    public NBMDRepositoryWrapper(NBMDRepositoryImpl impl)
    {
        this.impl = impl;
    }

    // implement EnkiMDRepository
    public void dropExtentStorage(String extentName)
        throws EnkiDropFailedException
    {
        RefPackage pkg = impl.getExtent(extentName);
        
        dropExtentStorage(pkg);
    }
    
    public void dropExtentStorage(RefPackage refPackage)
        throws EnkiDropFailedException
    {
        // From Farrago: grotty internals for dropping physical repos storage
        String mofIdString = refPackage.refMofId();
        MOFID mofId = MOFID.fromString(mofIdString);

        Storage storage = impl.getMdrStorage().getStorageByMofId(mofId);
        try {
            storage.close();
            storage.delete();
        } catch(StorageException e) {
            throw new EnkiDropFailedException(
                "Error dropping Netbeans MDR storage", e);
        }
    }
    
    public void addListener(MDRChangeListener arg0, int arg1)
    {
        impl.addListener(arg0, arg1);
    }

    public void addListener(MDRChangeListener arg0)
    {
        impl.addListener(arg0);
    }
    
    public void beginSession()
    {
        SessionContext context = tls.get();
        beginSessionImpl(context, false);
    }
    
    private void beginSessionImpl(SessionContext context, boolean implicit)
    {
        context.refCount++;
        context.implicit = implicit;

        if (context.implicit) {
            log.warning("begin implicit repository session");
        }
    }

    private void checkBeginImplicitSession()
    {
        SessionContext context = tls.get();
        if (context.refCount == 0) {
            beginSessionImpl(context, true);
        }
    }

    private void checkEndImplicitSession()
    {
        SessionContext context = tls.get();
        if (context.implicit == true && context.refCount == 1) {
            endSessionImpl(context);
        }
    }

    public void beginTrans(boolean write)
    {
        checkBeginImplicitSession();
        impl.beginTrans(write);
    }

    public RefPackage createExtent(
        String name,
        RefObject metaPackage,
        RefPackage[] existingInstances)
        throws CreationFailedException
    {
        return impl.createExtent(name, metaPackage, existingInstances);
    }

    public RefPackage createExtent(String name, RefObject metaPackage)
        throws CreationFailedException
    {
        return impl.createExtent(name, metaPackage);
    }

    public RefPackage createExtent(String name)
        throws CreationFailedException
    {
        return impl.createExtent(name);
    }
    
    public void endSession()
    {
        SessionContext context = tls.get();
        endSessionImpl(context);
    }

    private void endSessionImpl(SessionContext context)
    {
        if (context.refCount == 0) {
            throw new RuntimeException("session never opened/already closed");
        } else {
            context.refCount--;
        }
    }

    public void endTrans()
    {
        impl.endTrans();
        checkEndImplicitSession();
    }

    public void endTrans(boolean rollback)
    {
        impl.endTrans(rollback);
        checkEndImplicitSession();
    }

    public RefBaseObject getByMofId(String mofId)
    {
        return impl.getByMofId(mofId);
    }

    public RefPackage getExtent(String name)
    {
        return impl.getExtent(name);
    }

    public String[] getExtentNames()
    {
        return impl.getExtentNames();
    }

    public void removeListener(MDRChangeListener listener, int mask)
    {
        impl.removeListener(listener, mask);
    }

    public void removeListener(MDRChangeListener listener)
    {
        impl.removeListener(listener);
    }

    public void shutdown()
    {
        // Make it possible to start a new instance of NBMDRepositoryImpl
        org.netbeans.jmiimpl.mof.model.NamespaceImpl.clearContains();
        
        impl.shutdown();
    }
    
    // Implement EnkiMDRepository
    public boolean isExtentBuiltIn(String name)
    {
        return false;
    }
    
    // Implement EnkiMDRepository
    public ClassLoader getDefaultClassLoader()
    {
        return BaseObjectHandler.getDefaultClassLoader();
    }
    
    private static class ThreadLocalSessionContext 
        extends ThreadLocal<SessionContext>
    {
        @Override
        protected SessionContext initialValue()
        {
            return new SessionContext();
        }
    }
    
    private static class SessionContext
    {
        public int refCount = 0;
        public boolean implicit = false;
    }
}

// End NBMDRepositoryWrapper.java
