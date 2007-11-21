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
package org.eigenbase.enki.hibernate.jmi;

import java.util.*;

import javax.jmi.reflect.*;

/**
 * HibernateRefClass is a base class for implementations of {@link RefClass}.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateRefClass implements RefClass
{
    public Collection<?> refAllOfClass()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<?> refAllOfType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public RefObject refCreateInstance(List arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(RefObject arg0, List arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(String arg0, List arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RefEnum refGetEnum(RefObject arg0, String arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RefEnum refGetEnum(String arg0, String arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object refGetValue(RefObject arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object refGetValue(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object refInvokeOperation(RefObject arg0, List arg1)
        throws RefException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object refInvokeOperation(String arg0, List arg1)
        throws RefException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void refSetValue(RefObject arg0, Object arg1)
    {
        // TODO Auto-generated method stub

    }

    public void refSetValue(String arg0, Object arg1)
    {
        // TODO Auto-generated method stub

    }

    public RefPackage refImmediatePackage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RefObject refMetaObject()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String refMofId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RefPackage refOutermostPackage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<?> refVerifyConstraints(boolean arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
