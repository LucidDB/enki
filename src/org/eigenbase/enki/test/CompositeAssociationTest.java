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

import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;
import org.junit.*;

import eem.sample.special.*;

/**
 * CompositeAssociationTest tests aspects of associations that represent
 * composite aggregation.  In particular, it tests 
 * {@link RefObject#refImmediateComposite()} and 
 * {@link RefObject#refOutermostComposite()} for simple associations, 
 * situations where an entity is a component of multiple classes, and
 * situations where an {@link Attribute} is used to specify composition.
 * 
 * @author Stephan Zuercher
 */
public class CompositeAssociationTest
    extends SampleModelTestBase
{
    @BeforeClass
    public static void createCompositeAssociations()
    {
        getRepository().beginTrans(true);
        
        boolean rollback = true;
        try {
            Building building = 
                getSpecialPackage().getBuilding().createBuilding(
                    "123 Main Street",
                    "San Francisco",
                    "CA",
                    "94117");
            
            Floor firstFloor = 
                getSpecialPackage().getFloor().createFloor(1, 20);
            Floor secondFloor =
                getSpecialPackage().getFloor().createFloor(2, 10);
            
            building.getFloors().add(firstFloor);
            building.getFloors().add(secondFloor);
            
            Room room100 = 
                getSpecialPackage().getRoom().createRoom(100, 100, 100);
            Room room200 =
                getSpecialPackage().getRoom().createRoom(200, 100, 50);
            Room room201 =
                getSpecialPackage().getRoom().createRoom(201, 100, 50);
            
            firstFloor.getRooms().add(room100);
            secondFloor.getRooms().add(room200);
            secondFloor.getRooms().add(room201);
            
            LogCabin cabin = 
                getSpecialPackage().getLogCabin().createLogCabin();
            Log cabinLog = getSpecialPackage().getLog().createLog();
            cabin.getLogs().add(cabinLog);
            
            LogRaft raft = getSpecialPackage().getLogRaft().createLogRaft();
            Log raftLog = getSpecialPackage().getLog().createLog();
            raft.getLogs().add(raftLog);
    
            AreaCode areaCode = 
                getSpecialPackage().getAreaCode().createAreaCode("619", true);
            
            // Yes, they'd be annoyed if you called.
            getSpecialPackage().getPhoneNumber().createPhoneNumber(
                areaCode, "440-5428");
            
            rollback = false;
        }
        finally {
            getRepository().endTrans(rollback);
        }
    }
    
    @Before
    public void startReadTxn()
    {
        getRepository().beginTrans(false);        
    }
    
    @After
    public void endReadTxn()
    {
        getRepository().endTrans();
    }
    
    @Test
    public void testRefImmediateComposite()
    {
        Collection<Room> rooms =
            GenericCollections.asTypedCollection(
                getSpecialPackage().getRoom().refAllOfClass(),
                Room.class);
        Assert.assertFalse(rooms.isEmpty());
        
        // refImmediateComposite of each room is a floor
        for(Room room: rooms) {
            int expectedFloorNumber = room.getRoomNumber() / 100;
            
            RefFeatured immediateComposite = room.refImmediateComposite();
            
            Assert.assertNotNull(immediateComposite);
            Assert.assertTrue(immediateComposite instanceof Floor);
            
            Floor floor = (Floor)immediateComposite;
            
            Assert.assertEquals(expectedFloorNumber, floor.getFloorNumber());
        }
        
        Building expectedBuilding = 
            (Building)getSpecialPackage().getBuilding().refAllOfClass().iterator().next();
        
        Collection<Floor> floors =
            GenericCollections.asTypedCollection(
                getSpecialPackage().getFloor().refAllOfClass(),
                Floor.class);
        Assert.assertFalse(floors.isEmpty());
        
        // refImmediateComposite of each floor is a building
        for(Floor floor: floors) {
            RefFeatured immediateComposite = floor.refImmediateComposite();
            
            Assert.assertNotNull(immediateComposite);
            Assert.assertTrue(immediateComposite instanceof Building);
            
            Building building = (Building)immediateComposite;
            
            Assert.assertEquals(expectedBuilding, building);
        }
    }
    
    @Test
    public void testRefOutermostComposite()
    {
        Building expectedBuilding = 
            (Building)getSpecialPackage().getBuilding().refAllOfClass().iterator().next();
        
        Collection<Room> rooms =
            GenericCollections.asTypedCollection(
                getSpecialPackage().getRoom().refAllOfClass(),
                Room.class);
        Assert.assertFalse(rooms.isEmpty());

        // refOutermostComposite of each room is a building
        for(Room room: rooms) {
            RefFeatured outermostComposite = room.refOutermostComposite();
            
            Assert.assertNotNull(outermostComposite);
            Assert.assertTrue(outermostComposite instanceof Building);

            Building building = (Building)outermostComposite;
            
            Assert.assertEquals(expectedBuilding, building);                
        }
        
        Collection<Floor> floors =
            GenericCollections.asTypedCollection(
                getSpecialPackage().getFloor().refAllOfClass(),
                Floor.class);
        Assert.assertFalse(floors.isEmpty());
        
        // refOutermostComposite of each floor is a building
        for(Floor floor: floors) {
            RefFeatured outermostComposite = floor.refOutermostComposite();
            
            Assert.assertNotNull(outermostComposite);
            Assert.assertTrue(outermostComposite instanceof Building);
            
            Building building = (Building)outermostComposite;
            
            Assert.assertEquals(expectedBuilding, building);
        }
    }
    
    @Test
    public void testMultipleComposition()
    {
        // TODO: test that a single Log cannot simultaneously be a component
        // of a LogCabin and LogRaft (not current enforced)
        
        Collection<Log> logs =
            GenericCollections.asTypedCollection(
                getSpecialPackage().getLog().refAllOfClass(),
                Log.class);
        Assert.assertFalse(logs.isEmpty());

        LogCabin expectedLogCabin = 
            (LogCabin)getSpecialPackage().getLogCabin().refAllOfClass().iterator().next();

        LogRaft expectedLogRaft= 
            (LogRaft)getSpecialPackage().getLogRaft().refAllOfClass().iterator().next();
        
        for(Log log: logs) {
            RefFeatured immediateComposite = log.refImmediateComposite();
            
            Assert.assertNotNull(immediateComposite);
            
            if (immediateComposite instanceof LogCabin) {
                LogCabin logCabin = (LogCabin)immediateComposite;
                
                Assert.assertEquals(expectedLogCabin, logCabin);
            } else if (immediateComposite instanceof LogRaft) {
                LogRaft logRaft = (LogRaft)immediateComposite;
                
                Assert.assertEquals(expectedLogRaft, logRaft);
            } else {
                Assert.fail("unexpected type for Log's composite");
            }
        }
    }
    
    @Test
    public void testAttributeComposition()
    {
        AreaCode areaCode = 
            (AreaCode)getSpecialPackage().getAreaCode().refAllOfClass().iterator().next();
        
        RefFeatured immediateComposite = areaCode.refImmediateComposite();
        Assert.assertNotNull(immediateComposite);
        Assert.assertTrue(immediateComposite instanceof PhoneNumber);
        
        PhoneNumber phoneNumber = (PhoneNumber)immediateComposite;
        
        Assert.assertEquals(areaCode, phoneNumber.getAreaCode());
    }
}

// End CompositeAssociationTest.java
