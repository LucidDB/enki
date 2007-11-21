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
 * @author Stephan Zuercher
 */
public abstract class HibernateRefAssociation
    implements RefAssociation
{

    public boolean refAddLink(RefObject arg0, RefObject arg1)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection<?> refAllLinks()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean refLinkExists(RefObject arg0, RefObject arg1)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection<?> refQuery(RefObject arg0, RefObject arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<?> refQuery(String arg0, RefObject arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean refRemoveLink(RefObject arg0, RefObject arg1)
    {
        // TODO Auto-generated method stub
        return false;
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
