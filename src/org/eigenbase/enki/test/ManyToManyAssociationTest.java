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

import org.junit.*;

import eem.sample.simple.*;

/**
 * ManyToManyAssociationTest tests many-to-many associations.
 * 
 * @author Stephan Zuercher
 */
public class ManyToManyAssociationTest extends SampleModelTestBase
{
    private final int N = 5;
    
    @Test
    public void testOrderedCreate()
    {
        List<String> e23RefMofIds = new ArrayList<String>();
        List<String> e22RefMofIds = createEntities22(N, N, e23RefMofIds, false);
        
        for(String e22RefMofId: e22RefMofIds) {
            traverseEntity22to23(e22RefMofId, e23RefMofIds);
        }
        
        for(String e23RefMofId: e23RefMofIds) {
            traverseEntity23to22(e23RefMofId, e22RefMofIds);
        }
    }
    
    @Test
    public void testOrderedReverseCreate()
    {
        List<String> e23RefMofIds = new ArrayList<String>();
        List<String> e22RefMofIds = createEntities22(N, N, e23RefMofIds, true);
        
        for(String e22RefMofId: e22RefMofIds) {
            traverseEntity22to23(e22RefMofId, e23RefMofIds);
        }
        
        for(String e23RefMofId: e23RefMofIds) {
            traverseEntity23to22(e23RefMofId, e22RefMofIds);
        }
    }
    
    @Test
    public void testUnorderedCreate()
    {
        Set<String> e21RefMofIds = new HashSet<String>();
        Set<String> e20RefMofIds = createEntities20(N, N, e21RefMofIds, false);
        
        for(String e20RefMofId: e20RefMofIds) {
            traverseEntity20to21(e20RefMofId, e21RefMofIds);
        }
        
        for(String e21RefMofId: e21RefMofIds) {
            traverseEntity21to20(e21RefMofId, e20RefMofIds);
        }
    }
    
    @Test
    public void testUnorderedReverseCreate()
    {
        Set<String> e21RefMofIds = new HashSet<String>();
        Set<String> e20RefMofIds = createEntities20(N, N, e21RefMofIds, true);
        
        for(String e20RefMofId: e20RefMofIds) {
            traverseEntity20to21(e20RefMofId, e21RefMofIds);
        }
        
        for(String e21RefMofId: e21RefMofIds) {
            traverseEntity21to20(e21RefMofId, e20RefMofIds);
        }
    }
    
    @Test
    public void testOrderedRemove()
    {
        List<String> e23RefMofIds = new ArrayList<String>();
        List<String> e22RefMofIds = createEntities22(N, N, e23RefMofIds, false);

        // Map e22 to e23 to remove
        Map<String, String> e23RemoveRefMofIds = new TreeMap<String, String>();
        
        // Map e22 to set of e23 that will remain
        Map<String, List<String>> e23RemainingRefMofIds = 
            new TreeMap<String, List<String>>();
        
        for(
            Iterator<String> 
                i1 = e22RefMofIds.iterator(), 
                i2 = e23RefMofIds.iterator();
            i1.hasNext() && i2.hasNext(); )
        {
            String e22RefMofId = i1.next();
            String e23RemoveRefMofId = i2.next();

            e23RemoveRefMofIds.put(e22RefMofId, e23RemoveRefMofId);
            
            List<String> remaining = new ArrayList<String>(e23RefMofIds);
            remaining.remove(e23RemoveRefMofId);
            e23RemainingRefMofIds.put(e22RefMofId, remaining);
        }

        getRepository().beginTrans(true);
        try {
            for(Map.Entry<String, String> entry: e23RemoveRefMofIds.entrySet())
            {
                String e22RefMofId = entry.getKey();
                String e23RemoveRefMofId = entry.getValue();
                
                Entity22 e22 = findEntity(e22RefMofId, Entity22.class);

                List<Entity23> entities23 = e22.getEntity23();
                Iterator<Entity23> iter = entities23.iterator();
                while(iter.hasNext()) {
                    Entity23 e23 = iter.next();
                    if (e23.refMofId().equals(e23RemoveRefMofId)) {
                        iter.remove();
                    }
                }
                
                List<String> remaining = e23RemainingRefMofIds.get(e22RefMofId);
                traverseEntity22to23(e22RefMofId, remaining, false);
            }
        } finally {
            getRepository().endTrans();
        }

        for(Map.Entry<String, List<String>> entry: 
                e23RemainingRefMofIds.entrySet())
        {
            String e22RefMofId = entry.getKey();
            List<String> remaining = entry.getValue();
            traverseEntity22to23(e22RefMofId, remaining);
        }
    }
    
    @Test
    public void testUnorderedRemove()
    {
        Set<String> e21RefMofIds = new HashSet<String>();
        Set<String> e20RefMofIds = createEntities20(N, N, e21RefMofIds, false);

        // Map e20 to e21 to remove
        Map<String, String> e21RemoveRefMofIds = new TreeMap<String, String>();
        
        // Map e20 to set of e21 that will remain
        Map<String, Set<String>> e21RemainingRefMofIds = 
            new TreeMap<String, Set<String>>();
        
        for(
            Iterator<String> 
                i1 = e20RefMofIds.iterator(), 
                i2 = e21RefMofIds.iterator();
            i1.hasNext() && i2.hasNext(); )
        {
            String e20RefMofId = i1.next();
            String e21RemoveRefMofId = i2.next();

            e21RemoveRefMofIds.put(e20RefMofId, e21RemoveRefMofId);
            
            HashSet<String> remaining = new HashSet<String>(e21RefMofIds);
            remaining.remove(e21RemoveRefMofId);
            e21RemainingRefMofIds.put(e20RefMofId, remaining);
        }

        getRepository().beginTrans(true);
        try {
            for(Map.Entry<String, String> entry: e21RemoveRefMofIds.entrySet())
            {
                String e20RefMofId = entry.getKey();
                String e21RemoveRefMofId = entry.getValue();
                
                Entity20 e20 = findEntity(e20RefMofId, Entity20.class);

                Collection<Entity21> entities21 = e20.getEntity21();
                Iterator<Entity21> iter = entities21.iterator();
                while(iter.hasNext()) {
                    Entity21 e21 = iter.next();
                    if (e21.refMofId().equals(e21RemoveRefMofId)) {
                        iter.remove();
                    }
                }
                
                Set<String> remaining = e21RemainingRefMofIds.get(e20RefMofId);
                traverseEntity20to21(e20RefMofId, remaining, false);
            }
        } finally {
            getRepository().endTrans();
        }

        for(Map.Entry<String, Set<String>> entry: 
                e21RemainingRefMofIds.entrySet())
        {
            String e20RefMofId = entry.getKey();
            Set<String> remaining = entry.getValue();
            traverseEntity20to21(e20RefMofId, remaining);
        }
    }
    
    @Test
    public void testOrderedReassociationByFirstEnd()
    {
        testOrderedReassociation(true);
    }
    
    @Test
    public void testOrderedReassociationBySecondEnd()
    {
        testOrderedReassociation(false);
    }
    
    private void testOrderedReassociation(boolean byFirstEnd)
    {
        
    }
    
    @Test
    public void testUnorderedReassociationByFirstEnd()
    {
        testUnorderedReassociation(true);
    }
    
    @Test
    public void testUnorderedReassociationBySecondEnd()
    {
        testUnorderedReassociation(false);
    }
    
    private void testUnorderedReassociation(boolean byFirstEnd)
    {
        
    }
    
    @Test
    public void testReorder()
    {
        
    }
    
    @Test
    public void testOrderedInsert()
    {
        
    }
    
    private Set<String> createEntities20(
        int numEntities20, 
        int numEntities21, 
        Set<String> e21RefMofIds,
        boolean reverse)
    {
        getRepository().beginTrans(true);
        
        HashSet<String> e20RefMofIds = new HashSet<String>();
        
        Set<Entity21> entities21 = new HashSet<Entity21>();
        
        try {
            for(int i = 0; i < numEntities21; i++) {
                Entity21 e21 = 
                    getSimplePackage().getEntity21().createEntity21();
                String e21RefMofId = e21.refMofId();
                scheduleForDelete(e21);
                
                e21RefMofIds.add(e21RefMofId);
                
                entities21.add(e21);
            }
            
            for(int i = 0; i < numEntities20; i++) {
                Entity20 e20 =
                    getSimplePackage().getEntity20().createEntity20();
                e20RefMofIds.add(e20.refMofId());
                scheduleForDelete(e20);
             
                if (reverse) {
                    for(Entity21 e21: entities21) {
                        e21.getEntity20().add(e20);
                    }
                } else {
                    e20.getEntity21().addAll(entities21);
                }
            }
        } finally {
            getRepository().endTrans();
        }
        
        return e20RefMofIds;
    }
    
    private void traverseEntity20to21(
        String e20RefMofId, Set<String> e21RefMofIds)
    {
        traverseEntity20to21(e20RefMofId, e21RefMofIds, true);
    }

    private void traverseEntity20to21(
        String e20RefMofId, Set<String> e21RefMofIds, boolean startTxn)
    {
        if (startTxn) {
            getRepository().beginTrans(false);
        }
        
        // Defensive copy
        Set<String> expectedRefMofIds = new HashSet<String>(e21RefMofIds);

        try {
            Entity20 e20 = findEntity(e20RefMofId, Entity20.class);
            
            Collection<Entity21> entities21 = e20.getEntity21();
            
            Assert.assertEquals(expectedRefMofIds.size(), entities21.size());
            
            for(Entity21 e21: entities21) {
                String e21RefMofId = e21.refMofId();
                
                boolean removed = expectedRefMofIds.remove(e21RefMofId);
                Assert.assertTrue(removed);
            }
        } finally {
            if (startTxn) {
                getRepository().endTrans();
            }
        }
    }

    private void traverseEntity21to20(
        String e21RefMofId, Set<String> e20RefMofIds)
    {
        traverseEntity21to20(e21RefMofId, e20RefMofIds, true);
    }

    private void traverseEntity21to20(
        String e21RefMofId, Set<String> e20RefMofIds, boolean startTxn)
    {
        if (startTxn) {
            getRepository().beginTrans(false);
        }
        
        // Defensive copy
        Set<String> expectedRefMofIds = new HashSet<String>(e20RefMofIds);

        try {
            Entity21 e21 = findEntity(e21RefMofId, Entity21.class);
            
            Collection<Entity20> entities20 = e21.getEntity20();
            
            Assert.assertEquals(expectedRefMofIds.size(), entities20.size());
            
            for(Entity20 e20: entities20) {
                String e20RefMofId = e20.refMofId();
                
                boolean removed = expectedRefMofIds.remove(e20RefMofId);
                Assert.assertTrue(removed);
            }
        } finally {
            if (startTxn) {
                getRepository().endTrans();
            }
        }
    }

    private List<String> createEntities22(
        int numEntities22, 
        int numEntities23, 
        List<String> e23RefMofIds,
        boolean reverse)
    {
        getRepository().beginTrans(true);
        
        List<String> e22RefMofIds = new ArrayList<String>();
        
        List<Entity23> entities23 = new ArrayList<Entity23>();
        
        try {
            for(int i = 0; i < numEntities23; i++) {
                Entity23 e23 = 
                    getSimplePackage().getEntity23().createEntity23();
                String e23RefMofId = e23.refMofId();
                scheduleForDelete(e23);
                
                e23RefMofIds.add(e23RefMofId);
                
                entities23.add(e23);
            }
            
            for(int i = 0; i < numEntities22; i++) {
                Entity22 e22 =
                    getSimplePackage().getEntity22().createEntity22();
                e22RefMofIds.add(e22.refMofId());
                scheduleForDelete(e22);
             
                if (reverse) {
                    for(Entity23 e23: entities23) {
                        e23.getEntity22().add(e22);
                    }
                } else {
                    e22.getEntity23().addAll(entities23);
                }
            }
        } finally {
            getRepository().endTrans();
        }
        
        return e22RefMofIds;
    }
    
    private void traverseEntity22to23(
        String e22RefMofId, List<String> e23RefMofIds)
    {
        traverseEntity22to23(e22RefMofId, e23RefMofIds, true);
    }

    private void traverseEntity22to23(
        String e22RefMofId, List<String> e23RefMofIds, boolean startTxn)
    {
        if (startTxn) {
            getRepository().beginTrans(false);
        }
        
        try {
            Entity22 e22 = findEntity(e22RefMofId, Entity22.class);
            
            Collection<Entity23> entities23 = e22.getEntity23();

            Iterator<Entity23> entities23Iter = entities23.iterator();
            Iterator<String> e23RefMofIdsIter = e23RefMofIds.iterator();
            while(entities23Iter.hasNext() && e23RefMofIdsIter.hasNext()) {
                Entity23 e23 = entities23Iter.next();
                String e23RefMofId = e23RefMofIdsIter.next();
                
                Assert.assertEquals(e23RefMofId, e23.refMofId());
            }
            
            Assert.assertEquals(
                e23RefMofIdsIter.hasNext(), entities23Iter.hasNext());
        } finally {
            if (startTxn) {
                getRepository().endTrans();
            }
        }
    }

    private void traverseEntity23to22(
        String e23RefMofId, List<String> e22RefMofIds)
    {
        traverseEntity23to22(e23RefMofId, e22RefMofIds, true);
    }

    private void traverseEntity23to22(
        String e23RefMofId, List<String> e22RefMofIds, boolean startTxn)
    {
        if (startTxn) {
            getRepository().beginTrans(false);
        }
        
        try {
            Entity23 e23 = findEntity(e23RefMofId, Entity23.class);
            
            Collection<Entity22> entities22 = e23.getEntity22();
            
            Iterator<Entity22> entities22Iter = entities22.iterator();
            Iterator<String> e22RefMofIdsIter = e22RefMofIds.iterator();
            while(entities22Iter.hasNext() && e22RefMofIdsIter.hasNext()) {
                Entity22 e22 = entities22Iter.next();
                String e22RefMofId = e22RefMofIdsIter.next();
                
                Assert.assertEquals(e22RefMofId, e22.refMofId());
            }
            
            Assert.assertEquals(
                e22RefMofIdsIter.hasNext(), entities22Iter.hasNext());
        } finally {
            if (startTxn) {
                getRepository().endTrans();
            }
        }
    }
}

// End ManyToManyAssociation.java
