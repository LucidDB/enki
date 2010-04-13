/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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

import java.util.*;

/**
 * RefBaseObjectComparator compares {@link RefBaseObjectBase} instances by
 * MOF ID.  
 * 
 * @author Stephan Zuercher
 */
class RefBaseObjectComparator implements Comparator<RefBaseObjectBase>
{
    public static final RefBaseObjectComparator instance = 
        new RefBaseObjectComparator();
    
    public int compare(RefBaseObjectBase o1, RefBaseObjectBase o2)
    {
        if (o1 == o2) {
            return 0;
        }
        
        long mofId1 = o1.getMofId();
        long mofId2 = o2.getMofId();

        if (mofId1 < mofId2) {
            return -1;
        } else if (mofId1 > mofId2) {
            return 1;
        }

        return 0;
    }
}

// End RefBaseObjectComparator.java
