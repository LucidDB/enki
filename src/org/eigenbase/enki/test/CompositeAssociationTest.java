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
import org.junit.runner.*;

import eem.sample.special.*;

/**
 * CompositeAssociationTest tests aspects of associations that represent
 * composite aggregation.  In particular, it tests 
 * {@link RefObject#refImmediateComposite()} and 
 * {@link RefObject#refOutermostComposite()} for simple associations, 
 * situations where an entity is a component of multiple classes, and
 * situations where a {@link javax.jmi.model.Attribute} is used to specify 
 * composition.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class CompositeAssociationTest
    extends SampleModelTestBase
{
    @BeforeClass
    public static void setupAssociations()
    {
        createCompositeBuildingAssociations();
        createCompositeLogCabinAssociations();
        createCompositeLogRaftAssociations();
        createCompositePhoneAssociations();
    }
    
    public static String createCompositeBuildingAssociations()
    {
        return createCompositeBuildingAssociations(false);
    }
    
    public static String createCompositeBuildingAssociations(
        boolean createCompanies)
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
            
            Company companyA = null;
            Company companyB = null;
            if (createCompanies) {
                companyA = 
                    getSpecialPackage().getCompany().createCompany(
                        "Qwik-E-Mart");
                companyB = 
                    getSpecialPackage().getCompany().createCompany("Buy N Large");                
            }
            
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
            
            if (createCompanies) {
                room100.setCompany(companyA);
                room200.setCompany(companyB);
                room201.setCompany(companyB);
            }
            rollback = false;
            
            return building.refMofId();
        }
        finally {
            getRepository().endTrans(rollback);
        }
    }
    
    public static String createCompositeLogCabinAssociations()
    {
        getRepository().beginTrans(true);
        
        boolean rollback = true;
        try {
            LogCabin cabin = 
                getSpecialPackage().getLogCabin().createLogCabin();
            Log cabinLog = getSpecialPackage().getLog().createLog();
            cabin.getLogs().add(cabinLog);
            
            rollback = false;
            
            return cabin.refMofId();
        }
        finally {
            getRepository().endTrans(rollback);
        }
    }
            
    public static String createCompositeLogRaftAssociations()
    {
        getRepository().beginTrans(true);
        
        boolean rollback = true;
        try {
            LogRaft raft = getSpecialPackage().getLogRaft().createLogRaft();
            Log raftLog = getSpecialPackage().getLog().createLog();
            raft.getLogs().add(raftLog);
            
            rollback = false;
            
            return raft.refMofId();
        }
        finally {
            getRepository().endTrans(rollback);
        }
    }

    public static String createCompositePhoneAssociations()
    {
        getRepository().beginTrans(true);
        
        boolean rollback = true;
        try {
            AreaCode areaCode = 
                getSpecialPackage().getAreaCode().createAreaCode("619", true);
            
            // Yes, they'd be annoyed if you called.
            PhoneNumber phoneNumber =
                getSpecialPackage().getPhoneNumber().createPhoneNumber(
                    areaCode, "440-5428");
            
            rollback = false;
            
            return phoneNumber.refMofId();
        }
        finally {
            getRepository().endTrans(rollback);
        }
    }
    
    @Test
    public void testRefImmediateComposite()
    {
        getRepository().beginTrans(false);        
        try {
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
                
                Assert.assertEquals(
                    expectedFloorNumber, floor.getFloorNumber());
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
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testRefOutermostComposite()
    {
        getRepository().beginTrans(false);        
        try {
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
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testMultipleComposition()
    {
        // TODO: test that a single Log cannot simultaneously be a component
        // of a LogCabin and LogRaft (not currently enforced)
        getRepository().beginTrans(false);        
        try {
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
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testAttributeComposition()
    {
        getRepository().beginTrans(false);        
        try {
            AreaCode areaCode = 
                (AreaCode)getSpecialPackage().getAreaCode().refAllOfClass().iterator().next();
            
            RefFeatured immediateComposite = areaCode.refImmediateComposite();
            Assert.assertNotNull(immediateComposite);
            Assert.assertTrue(immediateComposite instanceof PhoneNumber);
            
            PhoneNumber phoneNumber = (PhoneNumber)immediateComposite;
            
            Assert.assertEquals(areaCode, phoneNumber.getAreaCode());
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testAttributeCompositionDeleteCascade()
    {
        String phoneNumberMofId = createCompositePhoneAssociations();

        // Delete phone number and expect area code is deleted as well.
        String areaCodeMofId;
        getRepository().beginTrans(true);
        try {
            PhoneNumber phoneNumber = 
                (PhoneNumber)getRepository().getByMofId(phoneNumberMofId);
            
            areaCodeMofId = phoneNumber.getAreaCode().refMofId();
            
            phoneNumber.refDelete();
        }
        finally {
            getRepository().endTrans(false);
        }
        
        getRepository().beginTrans(false);
        try {
            Assert.assertNull(
                getRepository().getByMofId(phoneNumberMofId));
            Assert.assertNull(
                getRepository().getByMofId(areaCodeMofId));
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testReferenceCompositionDeleteCascade()
    {
        String buildingMofId = createCompositeBuildingAssociations(true);

        // Delete build and expect floors are deleted as well.
        List<String> floorMofIds = new ArrayList<String>();
        List<String> roomMofIds = new ArrayList<String>();
        Set<String> companyMofIds = new HashSet<String>();
        
        getRepository().beginTrans(true);
        try {
            Building building = 
                (Building)getRepository().getByMofId(buildingMofId);
            
            for(Floor floor: building.getFloors()) {
                floorMofIds.add(floor.refMofId());
                
                for(Room room: floor.getRooms()) {
                    roomMofIds.add(room.refMofId());
                    
                    companyMofIds.add(room.getCompany().refMofId());
                }
            }
            
            building.refDelete();
        }
        finally {
            getRepository().endTrans(false);
        }
        
        getRepository().beginTrans(false);
        try {
            Assert.assertNull(
                getRepository().getByMofId(buildingMofId));

            for(String floorMofId: floorMofIds) {
                Assert.assertNull(
                    getRepository().getByMofId(floorMofId));
            }
            
            for(String roomMofId: roomMofIds) {
                Assert.assertNull(
                    getRepository().getByMofId(roomMofId));
            }
            
            for(String companyMofId: companyMofIds) {
                Assert.assertNotNull(
                    getRepository().getByMofId(companyMofId));
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testNullifiedComposite()
    {
        String phoneNumberMofId = createCompositePhoneAssociations();

        // Null the phone number's area code and see if it's deleted or not
        String areaCodeMofId;
        getRepository().beginTrans(true);
        try {
            PhoneNumber phoneNumber = 
                (PhoneNumber)getRepository().getByMofId(phoneNumberMofId);
            
            areaCodeMofId = phoneNumber.getAreaCode().refMofId();
            
            phoneNumber.setAreaCode(null);
        }
        finally {
            getRepository().endTrans(false);
        }
        
        getRepository().beginTrans(false);
        try {
            Assert.assertNotNull(
                getRepository().getByMofId(phoneNumberMofId));
            Assert.assertNotNull(
                getRepository().getByMofId(areaCodeMofId));
        }
        finally {
            getRepository().endTrans();
        }
    }
}

// End CompositeAssociationTest.java
