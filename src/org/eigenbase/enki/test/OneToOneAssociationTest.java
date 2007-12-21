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
package org.eigenbase.enki.test;

import java.util.*;

import org.eigenbase.enki.util.*;
import org.junit.*;

import eem.sample.simple.*;

/**
 * OneToOneAssociationTest tests one-to-one associations.
 * 
 * @author Stephan Zuercher
 */
public class OneToOneAssociationTest extends SampleModelTestBase
{
    @Test
    public void testCreate()
    {
        getRepository().beginTrans(true);

        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Entity1 e1 = 
                simplePkg.getEntity1().createEntity1();
            
            Entity2 e2 =
                simplePkg.getEntity2().createEntity2();
            
            e1.setEntity2(e2);
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testReverseCreate()
    {
        getRepository().beginTrans(true);

        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Entity1 e1 = 
                simplePkg.getEntity1().createEntity1();
            
            Entity2 e2 =
                simplePkg.getEntity2().createEntity2();
            
            e2.setEntity1(e1);
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testTraverse()
    {
        try {
            getRepository().beginTrans(false);
    
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Collection<Entity1> all =
                GenericCollections.asTypedCollection(
                    simplePkg.getEntity1().refAllOfClass(),
                    Entity1.class);
            Assert.assertTrue(all.size() >= 1);
            
            for(Entity1 e1: all) {
                Entity2 e2 = e1.getEntity2();
                Assert.assertNotNull(e2);
                
                System.out.println(
                    "e1(" + e1.refMofId() + ") -> e2(" + e2.refMofId() + ")");
            }
        } finally {
            getRepository().endTrans();
        }        
    }
}

// End AssociationTest.java
