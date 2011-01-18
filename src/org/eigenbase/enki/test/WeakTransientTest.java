/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2011 The Eigenbase Project
// Copyright (C) 2011 SQLstream, Inc.
// Copyright (C) 2011 Dynamo BI Corporation
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
package org.eigenbase.enki.test;

import java.util.*;

import org.junit.*;
import org.junit.runner.*;

import org.eigenbase.enki.trans.*;

import eem.sample.*;
import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * WeakTransientTest tests the weak reference mode of the transient repository.
 *
 * @author John Sichi
 * @version $Id$
 */
@RunWith(TransientOnlyTestRunner.class)
public class WeakTransientTest extends JmiTestBase
{
    static {
        getExtraProps().put(
            TransientMDRepository.PROPERTY_WEAK,
            Boolean.toString(true));
    }

    @Test
    public void testDisallowedMethods()
    {
        createEntities();
        Entity1 e1 = (Entity1) getRepository().getByMofId(e1MofId);
        Assert.assertNull(e1);
        Assert.assertEquals(
            0, getSimplePackage().getEntity1().refAllOfClass().size());
        Assert.assertEquals(
            0, getSimplePackage().getEntity1().refAllOfType().size());
        Assert.assertEquals(
            0, getSimplePackage().getHasAnEntity2().refAllLinks().size());
    }
}

// End WeakTransientTest.java
