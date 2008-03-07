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

import org.eigenbase.enki.mdr.*;
import org.junit.*;

import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * OneToManyAssociationTest tests one-to-many associations.
 * 
 * @author Stephan Zuercher
 */
public class OneToManyAssociationTest extends SampleModelTestBase
{
    private static final int N = 8;
    
    @Test
    public void testUnorderedCreate()
    {
        String e10RefMofId;
        Set<String> e11RefMofIds = new HashSet<String>();
        
        e10RefMofId = createEntity10(N, e11RefMofIds, false);
        
        traverseHasEntity11(e10RefMofId, e11RefMofIds);
    }

    @Test
    public void testUnorderedReverseCreate()
    {
        String e10RefMofId;
        Set<String> e11RefMofIds = new HashSet<String>();
        
        e10RefMofId = createEntity10(N, e11RefMofIds, true);
        
        traverseHasEntity11(e10RefMofId, e11RefMofIds);
    }
    
    @Test
    public void testOrderedCreate()
    {
        String e16RefMofId;
        List<String> e17RefMofIds = new ArrayList<String>();
        
        e16RefMofId = createEntity16(N, e17RefMofIds, false);
        
        traverseHasEntity17(e16RefMofId, e17RefMofIds);
    }

    @Test
    public void testOrderedReverseCreate()
    {
        String e16RefMofId;
        List<String> e17RefMofIds = new ArrayList<String>();
        
        e16RefMofId = createEntity16(N, e17RefMofIds, true);
        
        traverseHasEntity17(e16RefMofId, e17RefMofIds);
        
        traverseHasEntity17(e16RefMofId, e17RefMofIds);
    }
    
    @Test
    public void testUnorderedReassociateByFirstEnd()
    {
        testUnorderedReassociate(true);
    }
    
    @Test
    public void testUnorderedReassociateBySecondEnd()
    {
        testUnorderedReassociate(false);
    }
    
    private void testUnorderedReassociate(boolean byFirstEnd)
    {
        Set<String> e11RefMofIds = new HashSet<String>();
        
        String e10aRefMofId = createEntity10(N, e11RefMofIds, false);
        
        String e10bRefMofId = createEntity10(0, null, false);
        
        int pick = N / 2;
        String e11RefMofId = null;
        for(Iterator<String> iter = e11RefMofIds.iterator(); iter.hasNext(); )
        {
            e11RefMofId = iter.next();
            
            if (pick-- == 0) {
                iter.remove();
                break;
            }
        }

        getRepository().beginTrans(true);
        
        try {
            Entity11 e11 = findEntity(e11RefMofId, Entity11.class);
            
            Entity10 e10b = findEntity(e10bRefMofId, Entity10.class);
            
            if (byFirstEnd) {
                Entity10 e10a = findEntity(e10aRefMofId, Entity10.class);

                // REVIEW: SWZ: 12/26/07: Netbeans/Enki-Hibernate discrepancy.
                // Netbeans requires the remove(e11) on the old Entity10.  I
                // contend that since the e11 can only refer to one Entity10,
                // it should automatically be removed.  (Or else it should be
                // a constraint violation exception.)
                if (getMdrProvider() == MdrProvider.NETBEANS_MDR) {
                    e10a.getEntity11().remove(e11);
                }
                e10b.getEntity11().add(e11);
            } else {
                e11.setEntity10(e10b);
            }
            
            traverseHasEntity11(e10aRefMofId, e11RefMofIds, false);
            traverseHasEntity11(
                e10bRefMofId, Collections.singleton(e11RefMofId), false);
        }
        finally {
            getRepository().endTrans();
        }
        
        // Verify in separate txn
        traverseHasEntity11(e10aRefMofId, e11RefMofIds);
        traverseHasEntity11(e10bRefMofId, Collections.singleton(e11RefMofId));
    }
    
    @Test
    public void testOrderedReassociateByFirstEnd()
    {
        testOrderedReassociate(true);
    }
    
    @Test
    public void testOrderedReassociateBySecondEnd()
    {
        testOrderedReassociate(false);
    }
    
    private void testOrderedReassociate(boolean byFirstEnd)
    {
        List<String> e17RefMofIds = new ArrayList<String>();
        
        String e16aRefMofId = createEntity16(N, e17RefMofIds, false);
        
        String e16bRefMofId = createEntity16(0, null, false);
        
        String e17RefMofId = e17RefMofIds.get(N / 2);
        e17RefMofIds.remove(N / 2);

        getRepository().beginTrans(true);
        
        try {
            Entity17 e17 = findEntity(e17RefMofId, Entity17.class);
            
            Entity16 e16b = findEntity(e16bRefMofId, Entity16.class);
            
            if (byFirstEnd) {
                Entity16 e16a = findEntity(e16aRefMofId, Entity16.class);
                
                // REVIEW: SWZ: 12/26/07: Netbeans/Enki-Hibernate discrepancy.
                // Netbeans requires the remove(e17) on the old Entity16.  I
                // contend that since the e17 can only refer to one Entity16,
                // it should automatically be removed.  (Or else it should be
                // a constraint violation exception.)
                if (getMdrProvider() == MdrProvider.NETBEANS_MDR) {
                    e16a.getEntity17().remove(e17);
                }
                e16b.getEntity17().add(e17);
            } else {
                e17.setEntity16(e16b);
            }
            
            traverseHasEntity17(e16aRefMofId, e17RefMofIds, false);
            traverseHasEntity17(
                e16bRefMofId, Collections.singletonList(e17RefMofId), false);
        }
        finally {
            getRepository().endTrans();
        }
        
        // Verify in separate txn
        traverseHasEntity17(e16aRefMofId, e17RefMofIds);
        traverseHasEntity17(
            e16bRefMofId, Collections.singletonList(e17RefMofId));
    }

    @Test
    public void testUnorderedRemove()
    {
        Set<String> e11RefMofIds = new HashSet<String>();
        
        String e10RefMofId = createEntity10(N, e11RefMofIds, false);
        
        getRepository().beginTrans(true);
        
        try {
            Entity10 e10 = findEntity(e10RefMofId, Entity10.class);
            
            Collection<Entity11> entities11 = e10.getEntity11();
            
            Iterator<Entity11> iter = entities11.iterator();
            for(int i = 0; i < N / 2; i++) {
                Assert.assertTrue(iter.hasNext());
                Entity11 e11 = iter.next();
                iter.remove();
                
                e11RefMofIds.remove(e11.refMofId());
            }
            
            traverseHasEntity11(e10RefMofId, e11RefMofIds, false);
        }
        finally {
            getRepository().endTrans();
        }

        // Again in a new txn
        traverseHasEntity11(e10RefMofId, e11RefMofIds);

        // Remove the rest
        getRepository().beginTrans(true);
        
        try {
            Entity10 e10 = findEntity(e10RefMofId, Entity10.class);
            
            Collection<Entity11> entities11 = e10.getEntity11();
            
            entities11.clear();
            e11RefMofIds.clear();
            
            traverseHasEntity11(e10RefMofId, e11RefMofIds, false);
        }
        finally {
            getRepository().endTrans();
        }

        // Again in a new txn
        traverseHasEntity11(e10RefMofId, e11RefMofIds);
    }
    
    @Test
    public void testOrderedRemove()
    {
        List<String> e17RefMofIds = new ArrayList<String>();
        
        String e16RefMofId = createEntity16(N, e17RefMofIds, false);
        
        // Remove two via iterator
        getRepository().beginTrans(true);
        
        try {
            Entity16 e16 = findEntity(e16RefMofId, Entity16.class);
            
            List<Entity17> entities17 = e16.getEntity17();
            
            Iterator<Entity17> iter = entities17.iterator();
            for(int i = 0; i < N / 4; i++) {
                Assert.assertTrue(iter.hasNext());
                Entity17 e17 = iter.next();
                iter.remove();
                
                e17RefMofIds.remove(e17.refMofId());
            }
            
            traverseHasEntity17(e16RefMofId, e17RefMofIds, false);
        }
        finally {
            getRepository().endTrans();
        }

        traverseHasEntity17(e16RefMofId, e17RefMofIds);

        // Remove two via index
        getRepository().beginTrans(true);
        
        try {
            Entity16 e16 = findEntity(e16RefMofId, Entity16.class);
            
            List<Entity17> entities17 = e16.getEntity17();
            
            for(int i = N / 4; i < N / 2; i++) {
                Entity17 e17 = entities17.remove(i);

                e17RefMofIds.remove(e17.refMofId());
            }
            
            traverseHasEntity17(e16RefMofId, e17RefMofIds, false);
        }
        finally {
            getRepository().endTrans();
        }

        traverseHasEntity17(e16RefMofId, e17RefMofIds);
        
        if (getMdrProvider() != MdrProvider.NETBEANS_MDR) {
            // Remove two via sublist
            getRepository().beginTrans(true);
            
            try {
                Entity16 e16 = findEntity(e16RefMofId, Entity16.class);
                
                List<Entity17> entities17 = e16.getEntity17();
            
                List<Entity17> subList = entities17.subList(0, N / 4);
                subList.clear();
                
                List<String> mofIdSubList = e17RefMofIds.subList(0, N / 4);
                mofIdSubList.clear();
                
                traverseHasEntity17(e16RefMofId, e17RefMofIds, false);
            }
            finally {
                getRepository().endTrans();
            }
    
            traverseHasEntity17(e16RefMofId, e17RefMofIds);
        } else {
            System.out.println(
                "skipping remove by subList -- not support in Netbeans");
        }
        
        // Remove the rest via clear
        getRepository().beginTrans(true);
        
        try {
            Entity16 e16 = findEntity(e16RefMofId, Entity16.class);
            
            Collection<Entity17> entities17 = e16.getEntity17();
            
            entities17.clear();
            e17RefMofIds.clear();
            
            traverseHasEntity17(e16RefMofId, e17RefMofIds, false);
        }
        finally {
            getRepository().endTrans();
        }

        traverseHasEntity17(e16RefMofId, e17RefMofIds);
    }
    
    @Test
    public void testReorder()
    {
        List<String> e17RefMofIds = new ArrayList<String>();
        
        String e16RefMofId = createEntity16(N, e17RefMofIds, false);

        final int seed = 20050612;
        Random rng = new Random();

        getRepository().beginTrans(true);
        
        try {
            Entity16 e16 = findEntity(e16RefMofId, Entity16.class);
            
            List<Entity17> entities17 = e16.getEntity17();
            
            // In theory we should be able to shuffle entites17 directly,
            // but ListProxy doesn't handle the necessary arbitrary 
            // set(int,Object) calls that (and if the association
            // were marked unique it shouldn't work anyway since shuffle
            // might temporarily place the same object in the list in two
            // separate locations).
            rng.setSeed(seed);
            ArrayList<Entity17> copy = new ArrayList<Entity17>(entities17);            
            Collections.shuffle(copy, rng);
            entities17.clear();
            entities17.addAll(copy);
            
            rng.setSeed(seed);
            Collections.shuffle(e17RefMofIds, rng);
            
            traverseHasEntity17(e16RefMofId, e17RefMofIds, false);
        }
        finally {
            getRepository().endTrans();
        }

        traverseHasEntity17(e16RefMofId, e17RefMofIds);
    }

    @Test
    public void testOrderedInsert()
    {
        List<String> e17RefMofIds = new ArrayList<String>();
        
        String e16RefMofId = createEntity16(N, e17RefMofIds, false);

        getRepository().beginTrans(true);
        
        try {
            Entity16 e16 = findEntity(e16RefMofId, Entity16.class);
            
            Entity17 e17a = getSimplePackage().getEntity17().createEntity17();
            Entity17 e17b = getSimplePackage().getEntity17().createEntity17();

            List<Entity17> entities17 = e16.getEntity17();

            entities17.add(N / 4, e17b);
            entities17.add(0, e17a);
            
            e17RefMofIds.add(N / 4, e17b.refMofId());
            e17RefMofIds.add(0, e17a.refMofId());
            
            traverseHasEntity17(e16RefMofId, e17RefMofIds, false);
        }
        finally {
            getRepository().endTrans();
        }

        traverseHasEntity17(e16RefMofId, e17RefMofIds);
    }
    
    private String createEntity10(
        int numEntities11, Set<String> e11RefMofIds, boolean reverse)
    {
        getRepository().beginTrans(true);
        
        String e10RefMofId;
        try {
            SimplePackage simplePkg = getSimplePackage();
            Entity10 e10 = simplePkg.getEntity10().createEntity10();
            e10RefMofId = e10.refMofId();
            
            for(int i = 0; i < numEntities11; i++) {
                Entity11 e11 = simplePkg.getEntity11().createEntity11();

                e11RefMofIds.add(e11.refMofId());
     
                if (reverse) {
                    e11.setEntity10(e10);
                } else {
                    e10.getEntity11().add(e11);
                }
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        return e10RefMofId;
    }

    private void traverseHasEntity11(
        String e10RefMofId, Set<String> e11RefMofIds)
    {
        traverseHasEntity11(e10RefMofId, e11RefMofIds, true);
    }
    
    private void traverseHasEntity11(
        String e10RefMofId, Set<String> e11RefMofIds, boolean startTxn)
    {
        if (startTxn) {
            getRepository().beginTrans(false);
        }
        
        // Defensive copy
        Set<String> expectedRefMofIds = new HashSet<String>(e11RefMofIds);
        
        try {
            Entity10 e10 = findEntity(e10RefMofId, Entity10.class);
            Assert.assertEquals(e10RefMofId, e10.refMofId());
            
            Collection<Entity11> entities11 = e10.getEntity11();
            
            Assert.assertEquals(expectedRefMofIds.size(), entities11.size());
            
            for(Entity11 e11: entities11) {
                String e11RefMofId = e11.refMofId();
                
                boolean removed = expectedRefMofIds.remove(e11RefMofId);
                Assert.assertTrue(removed);
            }
        }
        finally {
            if (startTxn) {
                getRepository().endTrans();
            }
        }
    }

    private String createEntity16(
        int numEntities17, List<String> e17RefMofIds, boolean reverse)
    {
        getRepository().beginTrans(true);
        
        String e16RefMofId;
        try {
            SimplePackage simplePkg = getSamplePackage().getSimple();
            Entity16 e16 = simplePkg.getEntity16().createEntity16();
            e16RefMofId = e16.refMofId();
            
            for(int i = 0; i < numEntities17; i++) {
                Entity17 e17 = simplePkg.getEntity17().createEntity17();

                e17RefMofIds.add(e17.refMofId());
     
                if (reverse) {
                    e17.setEntity16(e16);
                } else {
                    e16.getEntity17().add(e17);
                }
            }
        }
        finally {
            getRepository().endTrans();
        }
        return e16RefMofId;
    }

    private void traverseHasEntity17(
        String e16RefMofId, List<String> e17RefMofIds)
    {
        traverseHasEntity17(e16RefMofId, e17RefMofIds, true);
    }
    
    private void traverseHasEntity17(
        String e16RefMofId, List<String> e17RefMofIds, boolean startTxn)
    {
        if (startTxn) {
            getRepository().beginTrans(false);
        }
        
        try {
            Entity16 e16 = findEntity(e16RefMofId, Entity16.class);
            Assert.assertEquals(e16RefMofId, e16.refMofId());
            
            List<Entity17> entities17 = e16.getEntity17();

            Iterator<Entity17> entities17Iter = entities17.iterator();
            Iterator<String> e17RefMofIdsIter = e17RefMofIds.iterator();
            while(entities17Iter.hasNext() && e17RefMofIdsIter.hasNext()) {
                Assert.assertEquals(
                    e17RefMofIdsIter.next(), 
                    entities17Iter.next().refMofId());
            }
            
            // Assert that lists are the same length.
            Assert.assertEquals(
                entities17Iter.hasNext(), e17RefMofIdsIter.hasNext());
        }
        finally {
            if (startTxn) {
                getRepository().endTrans();
            }
        }
    }

    @Test
    public void testManyToOneAssociation()
    {
        // Some associations have their single end as end 2 and the many
        // end as end 1.  Basic tests of that orientation here.
        
        String bMofId;
        Set<String> pnMofIds = new HashSet<String>();
        
        // Create
        getRepository().beginTrans(true);
        try {
            AreaCode ac1 = 
                getSpecialPackage().getAreaCode().createAreaCode("415", true);
            
            PhoneNumber pn1 = 
                getSpecialPackage().getPhoneNumber().createPhoneNumber(
                    ac1, "555-1212");
            
            AreaCode ac2 = 
                getSpecialPackage().getAreaCode().createAreaCode("415", true);

            PhoneNumber pn2 =
                getSpecialPackage().getPhoneNumber().createPhoneNumber(
                    ac2, "767-1234");
            
            Building b =
                getSpecialPackage().getBuilding().createBuilding(
                    "123 Main Street", "San Francisco", "CA", "94117");

            b.getPhoneNumber().add(pn1);
            b.getPhoneNumber().add(pn2);
            
            bMofId = b.refMofId();
            pnMofIds.add(pn1.refMofId());
            pnMofIds.add(pn2.refMofId());
            
        } finally {
            getRepository().endTrans();
        }
        
        // Read them back
        getRepository().beginTrans(false);
        try {
            Building b = findEntity(bMofId, Building.class);
            
            Assert.assertEquals("123 Main Street", b.getAddress());
            Assert.assertEquals("San Francisco", b.getCity());
            Assert.assertEquals("CA", b.getState());
            Assert.assertEquals("94117", b.getZipcode());
            
            Collection<PhoneNumber> phoneNumbers = b.getPhoneNumber();
            Assert.assertEquals(2, phoneNumbers.size());
            for(PhoneNumber pn: phoneNumbers) {
                boolean contained = pnMofIds.remove(pn.refMofId());
                Assert.assertTrue(contained);
            }
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testUnorderedDuplicates()
    {
        final int N = 4;
        final int EN = 1;
        
        String e12MofId;
        String e13MofId;
        
        // Create Entity12 and duplicate Entity13s
        getRepository().beginTrans(true);
        try {
            Entity12 e12 = getSimplePackage().getEntity12().createEntity12();
            e12MofId = e12.refMofId();
            
            Entity13 e13 = getSimplePackage().getEntity13().createEntity13();
            e13MofId = e13.refMofId();
            
            for(int i = 0; i < N; i++) {
                e12.getEntity13().add(e13);
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        // Read it back
        getRepository().beginTrans(false);
        try {
            Entity12 e12 = (Entity12)getRepository().getByMofId(e12MofId);
            
            Collection<Entity13> entities13 = e12.getEntity13();
            
            Assert.assertEquals(EN, entities13.size());
            for(Entity13 e13: entities13) {
                Assert.assertEquals(e13MofId, e13.refMofId());
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testOrderedDuplicates()
    {
        final int N = 4;
        
        String e16MofId;
        String e17MofId;
        
        // Create Entity16 and duplicate Entity17s
        getRepository().beginTrans(true);
        try {
            Entity16 e16 = getSimplePackage().getEntity16().createEntity16();
            e16MofId = e16.refMofId();
            
            Entity17 e17 = getSimplePackage().getEntity17().createEntity17();
            e17MofId = e17.refMofId();
            
            for(int i = 0; i < N; i++) {
                e16.getEntity17().add(e17);
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        // Read it back
        getRepository().beginTrans(false);
        try {
            Entity16 e16 = (Entity16)getRepository().getByMofId(e16MofId);
            
            List<Entity17> entities17 = e16.getEntity17();
            
            Assert.assertEquals(N, entities17.size());
            for(Entity17 e17: entities17) {
                Assert.assertEquals(e17MofId, e17.refMofId());
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        // Remove by index, one at a time
        for(int repeat = 0; repeat <= N; repeat++) {
            getRepository().beginTrans(true);
            try {
                Entity16 e16 = (Entity16)getRepository().getByMofId(e16MofId);
                
                List<Entity17> entities17 = e16.getEntity17();
                
                Assert.assertEquals(N - repeat, entities17.size());
                for(Entity17 e17: entities17) {
                    Assert.assertEquals(e17MofId, e17.refMofId());
                }
                
                if (entities17.size() > 0) {
                    entities17.remove(0);
                }
            }
            finally {
                getRepository().endTrans();
            }
        }
    }
}

// End OneToManyAssociationTest.java
