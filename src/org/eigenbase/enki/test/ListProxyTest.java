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
package org.eigenbase.enki.test;

import java.util.*;

import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.simple.*;

/**
 * Tests {@link org.eigenbase.enki.hibernate.storage.ListProxy}.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class ListProxyTest extends SampleModelTestBase
{
    private static final int N = 8;
    private static String e16RefMofId;
    
    @BeforeClass
    public static void createTestObjects()
    {
        getRepository().beginTrans(true);
        
        e16RefMofId = null;
        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            Entity16 e16 = simplePkg.getEntity16().createEntity16();
            
            for(int i = 0; i < N; i++) {
                Entity17 e17 = simplePkg.getEntity17().createEntity17();

                e16.getEntity17().add(e17);
            }
            
            e16RefMofId = e16.refMofId();
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testSize()
    {
        getRepository().beginTrans(false);
        
        try {
            Entity16 e16 = findTestObject();
            
            int size = e16.getEntity17().size();
            
            Assert.assertEquals(N, size);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    private Entity16 findTestObject()
    {
        Collection<Entity16> entities16 =
            GenericCollections.asTypedCollection(
                getSimplePackage().getEntity16().refAllOfClass(),
                Entity16.class);
        for(Entity16 e: entities16) {
            if (e.refMofId().equals(e16RefMofId)) {
                return e;
            }
        }

        Assert.fail("couldn't find Entity16 / " + e16RefMofId);
        return null;
    }
}

// End ListProxyTest.java
