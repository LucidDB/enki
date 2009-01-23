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
package org.eigenbase.enki.jmi.impl;

import javax.jmi.reflect.*;

/**
 * RefAssociationLinkImpl implements {@link RefAssociationLink}.
 * 
 * @author Stephan Zuercher
 */
public class RefAssociationLinkImpl implements RefAssociationLink
{
    private final RefObject firstEnd;
    private final RefObject secondEnd;
    
    private final long firstEndMofId;
    private final long secondEndMofId;
    
    public RefAssociationLinkImpl(RefObject firstEnd, RefObject secondEnd)
    {
        this.firstEnd = firstEnd;
        this.secondEnd = secondEnd;
        
        this.firstEndMofId = ((RefBaseObjectBase)firstEnd).getMofId();
        this.secondEndMofId = ((RefBaseObjectBase)secondEnd).getMofId();        
    }
    
    public RefObject refFirstEnd()
    {
        return firstEnd;
    }

    public RefObject refSecondEnd()
    {
        return secondEnd;
    }

    public boolean equals(Object other)
    {
        RefAssociationLinkImpl that = (RefAssociationLinkImpl)other;
        
        return 
            this.firstEndMofId == that.firstEndMofId &&
            this.secondEndMofId == that.secondEndMofId;
    }
    
    public int hashCode()
    {
        return (int)firstEndMofId ^ (int)secondEndMofId;
    }
}

// End RefAssociationLink.java
