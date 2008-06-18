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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.junit.*;
import org.junit.runner.*;

import eem.sample.*;
import eem.sample.special.*;

/**
 * JmiWriteTest tests methods on various JMI interfaces.  As its name implies,
 * the test methods modify the repository.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class JmiWriteTest extends JmiTestBase
{
    private static List<String> carMofIds;
    private static String buildMofId;
    private static String phoneNumberMofId;
    
    @BeforeClass
    public static void initStaticMembers()
    {
        carMofIds = new ArrayList<String>();
        buildMofId = null;
        phoneNumberMofId = null;
    }
    
    @Ignore
    @Test
    public void testRefPackageCreateStruct()
    {
        // TODO: refCreateStruct  (Sample model has no structs)
    }
    
    @Ignore
    @Test
    public void testRefPackageDelete()
    {
        // TODO: refDelete (only on outermost package for Netbeans, check for that exception in Hibernate storage)
    }
    
    @Test
    public void testRefClassCreateInstance()
    {
        getRepository().beginTrans(true);
        try {
            // refCreateInstance
            RefClass refCarClass = getSamplePackage().getCar();
            
            List<?> noArgs = Collections.EMPTY_LIST;
            Car car1 = (Car)refCarClass.refCreateInstance(noArgs);
            car1.setMake("Lamborghini");
            car1.setModel("Gallardo Superleggera");
            car1.setDoors(2);
            
            Car car2 =
                (Car)refCarClass.refCreateInstance(
                    Arrays.asList(new Object[] { "Koenigsegg", "CCX", 2 }));
            
            carMofIds.add(car1.refMofId());
            carMofIds.add(car2.refMofId());
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Ignore
    @Test
    public void testRefClassCreateStruct()
    {
        // TODO: refCreateStruct (Sample model has no structs)
    }
    
    @Test
    public void testRefFeaturedSetValue()
    {
        getRepository().beginTrans(true);
        try {
            RefClass refCarClass = getSamplePackage().getCar();
            
            Attribute make = null;
            Attribute model = null;
            Attribute doors = null;
            for(Attribute attrib: 
                    getAttributes((MofClass)refCarClass.refMetaObject()))
            {
                String name = attrib.getName();
                if (name.equals("make")) {
                    make = attrib;
                } else if (name.equals("model")) {
                    model = attrib;
                } else if (name.equals("doors")) {
                    doors = attrib;
                }
            }
            Assert.assertNotNull(make);
            Assert.assertNotNull(model);
            Assert.assertNotNull(doors);
    
            final List<?> noArgs = Collections.emptyList();
            Car car1 = (Car)refCarClass.refCreateInstance(noArgs);
            car1.refSetValue("make", "McLaren");
            car1.refSetValue("model", "F1");
            car1.refSetValue("doors", Integer.valueOf(2));
            
            Assert.assertEquals("McLaren", car1.getMake());
            Assert.assertEquals("F1", car1.getModel());
            Assert.assertEquals(2, car1.getDoors());
            
            Car car2 = (Car)refCarClass.refCreateInstance(noArgs);
            car2.refSetValue(make, "Tesla");
            car2.refSetValue(model, "Roadster");
            car2.refSetValue(doors, Integer.valueOf(2));
            
            Assert.assertEquals("Tesla", car2.getMake());
            Assert.assertEquals("Roadster", car2.getModel());
            Assert.assertEquals(2, car2.getDoors());
            
            carMofIds.add(car1.refMofId());
            carMofIds.add(car2.refMofId());
            
            // Repeat with troublesome values
            PhoneNumberClass refPhoneNumberClass = 
                getSpecialPackage().getPhoneNumber();
            AreaCodeClass refAreaCodeClass =
                getSpecialPackage().getAreaCode();
            
            Attribute number = null;
            Attribute areaCodeAttrib = null;
            for(Attribute attrib: 
                    getAttributes((MofClass)refPhoneNumberClass.refMetaObject()))
            {
                String name = attrib.getName();
                if (name.equals("number")) {
                    number = attrib;
                } else if (name.equals("areaCode")) {
                    areaCodeAttrib = attrib;
                }
            }
            Assert.assertNotNull(number);
            Assert.assertNotNull(areaCodeAttrib);
    
            
            Attribute domestic = null;
            Attribute code = null;
            for(Attribute attrib: 
                    getAttributes((MofClass)refAreaCodeClass.refMetaObject()))
            {
                String name = attrib.getName();
                if (name.equals("code")) {
                    code = attrib;
                } else if (name.equals("domestic")) {
                    domestic = attrib;
                }
            }
            Assert.assertNotNull(code);
            Assert.assertNotNull(domestic);
    
            AreaCode areaCode1 =
                (AreaCode)refAreaCodeClass.refCreateInstance(noArgs);
            areaCode1.refSetValue("code", AREA_CODE);
            areaCode1.refSetValue("domestic", DOMESTIC);
            
            Assert.assertEquals(AREA_CODE, areaCode1.getCode());
            Assert.assertEquals(DOMESTIC, areaCode1.isDomestic());
            
            AreaCode areaCode2 =
                (AreaCode)refAreaCodeClass.refCreateInstance(noArgs);
            areaCode2.refSetValue(code, AREA_CODE + "x");
            areaCode2.refSetValue(domestic, !DOMESTIC);
            
            Assert.assertEquals(AREA_CODE + "x", areaCode2.getCode());
            Assert.assertEquals(!DOMESTIC, areaCode2.isDomestic());
            
            PhoneNumber phoneNumber1 = 
                (PhoneNumber)refPhoneNumberClass.refCreateInstance(noArgs);
            phoneNumber1.refSetValue("areaCode", areaCode1);
            phoneNumber1.refSetValue("number", PHONE_NUMBER);
            
            Assert.assertEquals(areaCode1, phoneNumber1.getAreaCode());
            Assert.assertEquals(PHONE_NUMBER, phoneNumber1.getNumber());
            
            PhoneNumber phoneNumber2 = 
                (PhoneNumber)refPhoneNumberClass.refCreateInstance(noArgs);
            phoneNumber2.refSetValue(areaCodeAttrib, areaCode2);
            phoneNumber2.refSetValue(number, PHONE_NUMBER + "x");
            
            Assert.assertEquals(areaCode2, phoneNumber2.getAreaCode());
            Assert.assertEquals(PHONE_NUMBER + "x", phoneNumber2.getNumber());
            
            RefClass refConeClass = getSpecialPackage().getIceCreamCone();
    
            Attribute flavor = null;
            Attribute scoops = null;
            Attribute isMelting = null;
            for(Attribute attrib: 
                    getAttributes((MofClass)refConeClass.refMetaObject()))
            {
                String name = attrib.getName();
                if (name.equals("flavor")) {
                    flavor = attrib;
                } else if (name.equals("scoops")) {
                    scoops = attrib;
                } else if (name.equals("isMelting")) {
                    isMelting = attrib;
                }
            }
            Assert.assertNotNull(flavor);
            Assert.assertNotNull(scoops);
            Assert.assertNotNull(isMelting);        
    
            IceCreamCone cone1 =
                (IceCreamCone)refConeClass.refCreateInstance(noArgs);
            cone1.refSetValue("flavor", FLAVOR);
            cone1.refSetValue("scoops", NUM_SCOOPS);
            cone1.refSetValue("isMelting", IS_MELTING);
    
            Assert.assertEquals(FLAVOR, cone1.getFlavor());
            Assert.assertEquals(NUM_SCOOPS, cone1.getScoops());
            Assert.assertEquals(IS_MELTING, cone1.isMelting());
            
            IceCreamCone cone2 =
                (IceCreamCone)refConeClass.refCreateInstance(noArgs);
            cone2.refSetValue(flavor, FLAVOR);
            cone2.refSetValue(scoops, NUM_SCOOPS + 1);
            cone2.refSetValue(isMelting, !IS_MELTING);
    
            Assert.assertEquals(FLAVOR, cone2.getFlavor());
            Assert.assertEquals(NUM_SCOOPS + 1, cone2.getScoops());
            Assert.assertEquals(!IS_MELTING, cone2.isMelting());
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testRefObjectDelete()
    {
        getRepository().beginTrans(true);
        try {
            RefClass carClass = getSamplePackage().getCar();
            
            int carsDeleted = 0;
            Collection<?> allCars = carClass.refAllOfClass();
            for(Object o: allCars) {
                RefObject car = (RefObject)o;
    
                if (!carMofIds.contains(car.refMofId())) {
                    continue;
                }
                
                car.refDelete();
                carsDeleted++;
            }
    
            Assert.assertEquals(carMofIds.size(), carsDeleted);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testRefAddLink()
    {
        getRepository().beginTrans(true);
        try {
            AreaCode ac = 
                getSpecialPackage().getAreaCode().createAreaCode("415", true);
            
            PhoneNumber pn =
                getSpecialPackage().getPhoneNumber().createPhoneNumber(
                    ac, "555-1212");
            phoneNumberMofId = pn.refMofId();
            
            Building b = 
                getSpecialPackage().getBuilding().createBuilding(
                    "1 Main St.", "Anytown", "XX", "10000");
            buildMofId = b.refMofId();
            
            getSpecialPackage().getHasPhoneNumber().refAddLink(pn, b);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testRefRemoveLink()
    {
        getRepository().beginTrans(true);
        try {
            PhoneNumber pn = findEntity(phoneNumberMofId, PhoneNumber.class);
            Building b = findEntity(buildMofId, Building.class);
            
            Assert.assertTrue(
                getSpecialPackage().getHasPhoneNumber().refLinkExists(pn, b));
            
            getSpecialPackage().getHasPhoneNumber().refRemoveLink(pn, b);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    // TODO: Enki/Hibernate doesn't support live association proxy collections,
    // yet.
    @Ignore
    @Test
    public void testAssocProxyCollectionLiveness()
    {
        String carMofId;
        getRepository().beginTrans(true);
        try {
            Car car = getSamplePackage().getCar().createCar("X", "Y", 2);
            carMofId = car.refMofId();
            
            State ca = getSamplePackage().getState().createState("CA");
            State or = getSamplePackage().getState().createState("OR");
            State nv = getSamplePackage().getState().createState("NV");
            State wa = getSamplePackage().getState().createState("WA");

            car.getRegistrar().add(ca);
            car.getRegistrar().add(or);
            car.getRegistrar().add(nv);
            car.getRegistrar().add(wa);
        }
        finally {
            getRepository().endTrans();
        }
        
        String newCarMofId;
        getRepository().beginTrans(true);
        try {
            Car car = (Car)getRepository().getByMofId(carMofId);
            Car newCar = getSamplePackage().getCar().createCar("Z", "Z", 4);
            newCarMofId = newCar.refMofId();
            
            Registrations regAssoc = getSamplePackage().getRegistrations();
            
            Collection<State> registratars = regAssoc.getRegistrar(car);
            Iterator<State> i = registratars.iterator();
            while(i.hasNext()) {
                State state = i.next();
                i.remove();
                regAssoc.add(newCar, state);
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        getRepository().beginTrans(false);
        try {
            Car car = (Car)getRepository().getByMofId(carMofId);
            Car newCar = (Car)getRepository().getByMofId(newCarMofId);
            
            Assert.assertEquals(0, car.getRegistrar().size());
            Assert.assertEquals(4, newCar.getRegistrar().size());
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testRefAllOfTypeForNewObjects()
    {
        reset();
        
        getRepository().beginTrans(true);
        try {
            Car car = 
                getSamplePackage().getCar().createCar("Lotus", "Elise", 2);
            
            Vehicle vehicle =
                getSamplePackage().getVehicle().createVehicle("Segway", "i2");
            
            Collection<?> vehicles = 
                getSamplePackage().getVehicle().refAllOfType();
            
            Assert.assertEquals(2, vehicles.size());
            Assert.assertTrue(vehicles.contains(car));
            Assert.assertTrue(vehicles.contains(vehicle));       
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    @Test
    public void testRefAllOfClassForNewObjects()
    {
        reset();
        
        getRepository().beginTrans(true);
        try {
            Vehicle vehicle =
                getSamplePackage().getVehicle().createVehicle("Segway", "i2");
            
            Vehicle vehicle2 =
                getSamplePackage().getVehicle().createVehicle("Segway", "x2");
            
            Collection<?> vehicles = 
                getSamplePackage().getVehicle().refAllOfClass();
            
            Assert.assertEquals(2, vehicles.size());
            Assert.assertTrue(vehicles.contains(vehicle));
            Assert.assertTrue(vehicles.contains(vehicle2));       
        } finally {
            getRepository().endTrans(false);
        }
    }
}

// End JmiWriteTest.java
