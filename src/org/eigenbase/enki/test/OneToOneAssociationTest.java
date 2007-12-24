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

import javax.jmi.reflect.*;

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

        String e1RefMofId, e2RefMofId;
        
        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Entity1 e1 = 
                simplePkg.getEntity1().createEntity1();
            e1RefMofId = e1.refMofId();
            
            Entity2 e2 =
                simplePkg.getEntity2().createEntity2();
            e2RefMofId = e2.refMofId();
            
            e1.setEntity2(e2);
            
            scheduleForDelete(e1);
            scheduleForDelete(e2);
        } finally {
            getRepository().endTrans();
        }
        
        traverseHasEntity2(e1RefMofId, e2RefMofId);
    }
    
    @Test
    public void testReverseCreate()
    {
        getRepository().beginTrans(true);

        String e1RefMofId, e2RefMofId;

        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Entity1 e1 = 
                simplePkg.getEntity1().createEntity1();
            e1RefMofId = e1.refMofId();
            
            Entity2 e2 =
                simplePkg.getEntity2().createEntity2();
            e2RefMofId = e2.refMofId();
            
            e2.setEntity1(e1);

            scheduleForDelete(e1);
            scheduleForDelete(e2);
        } finally {
            getRepository().endTrans();
        }
        
        traverseHasEntity2(e1RefMofId, e2RefMofId);
    }

    @Test
    public void testReassociateByFirstEnd()
    {
        getRepository().beginTrans(true);

        String e1aRefMofId, e1bRefMofId, e2RefMofId;
        
        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Entity1 e1a = 
                simplePkg.getEntity1().createEntity1();
            e1aRefMofId = e1a.refMofId();
            
            Entity1 e1b = 
                simplePkg.getEntity1().createEntity1();
            e1bRefMofId = e1b.refMofId();

            Entity2 e2 =
                simplePkg.getEntity2().createEntity2();
            e2RefMofId = e2.refMofId();
            
            e1a.setEntity2(e2);
            
            scheduleForDelete(e1a);
            scheduleForDelete(e1b);
            scheduleForDelete(e2);
        } finally {
            getRepository().endTrans();
        }
        
        traverseHasEntity2(e1aRefMofId, e2RefMofId);

        // Re-associate
        getRepository().beginTrans(true);

        try {
            Entity1 e1a = findEntity(e1aRefMofId, Entity1.class); 
            Entity1 e1b = findEntity(e1bRefMofId, Entity1.class);
            Entity2 e2 = findEntity(e2RefMofId, Entity2.class);

            // TODO: Fix Netbeans/Enki-Hibernate discrepancy.
            // Netbeans requires the set(null) on the old Entity1
            e1a.setEntity2(null);
            e1b.setEntity2(e2);
            
            // Verify in transaction
            Assert.assertEquals(e2, e1b.getEntity2());
            Assert.assertEquals(e1b, e2.getEntity1());
            Assert.assertNull(e1a.getEntity2());
        } finally {
            getRepository().endTrans();
        }

        traverseHasEntity2(e1bRefMofId, e2RefMofId);

        // Check e1a again from another transaction
        getRepository().beginTrans(false);
        try {
            Entity1 e1a = findEntity(e1aRefMofId, Entity1.class); 

            Assert.assertNull(e1a.getEntity2());
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testReassociateBySecondEnd()
    {
        getRepository().beginTrans(true);

        String e1aRefMofId, e1bRefMofId, e2RefMofId;
        
        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Entity1 e1a = 
                simplePkg.getEntity1().createEntity1();
            e1aRefMofId = e1a.refMofId();
            
            Entity1 e1b = 
                simplePkg.getEntity1().createEntity1();
            e1bRefMofId = e1b.refMofId();

            Entity2 e2 =
                simplePkg.getEntity2().createEntity2();
            e2RefMofId = e2.refMofId();
            
            e1a.setEntity2(e2);
            
            scheduleForDelete(e1a);
            scheduleForDelete(e1b);
            scheduleForDelete(e2);
        } finally {
            getRepository().endTrans();
        }
        
        traverseHasEntity2(e1aRefMofId, e2RefMofId);

        // Re-associate
        getRepository().beginTrans(true);

        try {
            Entity1 e1a = findEntity(e1aRefMofId, Entity1.class); 
            Entity1 e1b = findEntity(e1bRefMofId, Entity1.class);
            Entity2 e2 = findEntity(e2RefMofId, Entity2.class);
            
            e2.setEntity1(e1b);
            
            // Verify in transaction
            Assert.assertNull(e1a.getEntity2());
            Assert.assertEquals(e2, e1b.getEntity2());
            Assert.assertEquals(e1b, e2.getEntity1());
        } finally {
            getRepository().endTrans();
        }

        traverseHasEntity2(e1bRefMofId, e2RefMofId);

        // Check e1a again from another transaction
        getRepository().beginTrans(false);
        try {
            Entity1 e1a = findEntity(e1aRefMofId, Entity1.class); 

            Assert.assertNull(e1a.getEntity2());
        }
        finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testRemove()
    {
        // Create association
        getRepository().beginTrans(true);

        String e1RefMofId, e2RefMofId;
        
        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            
            Entity1 e1 = 
                simplePkg.getEntity1().createEntity1();
            e1RefMofId = e1.refMofId();
            
            Entity2 e2 =
                simplePkg.getEntity2().createEntity2();
            e2RefMofId = e2.refMofId();
            
            e1.setEntity2(e2);
            
            scheduleForDelete(e1);
            scheduleForDelete(e2);
        } finally {
            getRepository().endTrans();
        }
        
        traverseHasEntity2(e1RefMofId, e2RefMofId);
        
        // Remove association
        getRepository().beginTrans(true);
        
        try {
            Entity1 e1 = findEntity(e1RefMofId, Entity1.class);
            Entity2 e2 = findEntity(e2RefMofId, Entity2.class);
            
            RefAssociation hasEntity2Assoc = 
                getSimplePackage().getHasAnEntity2();
            
            hasEntity2Assoc.refRemoveLink(e1, e2);
            
            Assert.assertNull(e1.getEntity2());
            Assert.assertNull(e2.getEntity1());
        } finally {
            getRepository().endTrans();
        }

        // Verify association was removed
        getRepository().beginTrans(false);

        try {
            Entity1 e1 = findEntity(e1RefMofId, Entity1.class);
            Entity2 e2 = findEntity(e2RefMofId, Entity2.class);
            
            Assert.assertNull(e1.getEntity2());
            Assert.assertNull(e2.getEntity1());
        } finally {
            getRepository().endTrans();
        }
    }
    
    private void traverseHasEntity2(String e1RefMofId, String e2RefMofId)
    {
        getRepository().beginTrans(false);
        
        try {
            Entity1 e1 = findEntity(e1RefMofId, Entity1.class);
            
            Assert.assertEquals(e1RefMofId, e1.refMofId());
            
            Assert.assertNotNull(e1.getEntity2());
            
            Assert.assertEquals(e2RefMofId, e1.getEntity2().refMofId());
        } 
        finally {
            getRepository().endTrans();
        }
    }
}

// End AssociationTest.java
