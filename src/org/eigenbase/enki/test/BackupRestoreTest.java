/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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

import java.io.*;
import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.*;
import eem.sample.special.*;

/**
 * BackupRestoreTest tests repository extent backup and restore.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class BackupRestoreTest extends SampleModelTestBase
{
    private static final boolean CORN_BOOLEANA = true;
    private static final boolean CORN_BOOLEANB = true;
    private static final int CORN_INTA = 1;
    private static final int CORN_INTB = 2;
    private static final long CORN_LONGA = 1L << 33;
    private static final long CORN_LONGB = 1L << 34;
    private static final float CORN_FLOATA = 3.14159f;
    private static final float CORN_FLOATB = 2.71828f;
    private static final double CORN_DOUBLEA = Math.sqrt(2.0);
    private static final double CORN_DOUBLEB = Math.sqrt(3.0);

    private static final IceCreamFlavor CONE1_FLAVOR = 
        IceCreamFlavorEnum.CHOCOLATE;
    private static final boolean CONE1_MELTING = false;
    private static final int CONE1_SCOOPS = 1;
    private static final IceCreamFlavor CONE2_FLAVOR = 
        IceCreamFlavorEnum.NEOPOLITAN;
    private static final boolean CONE2_MELTING = false;
    private static final int CONE2_SCOOPS = 1;

    private static final String DRIVER_NAME = "Bob";
    private static final String DRIVER_LICENSE = "ABC123";

    private static final String CAR_MODEL = "Nagari";
    private static final String CAR_MAKE = "Bolwell";
    private static final int CAR_DOORS = 2;
    
    private static final String PASSENGER_NAME = "Fred";

    private static final String SHORT_STRING;
    static {
        StringBuilder b = new StringBuilder();
        b.append("S");
        for(int i = 0; i < 7; i++) {
            b.append(b.toString());
        }
        SHORT_STRING = b.toString();
    }
    
    private static final String MEDIUM_STRING;
    static {
        StringBuilder b = new StringBuilder();
        b.append("M");
        for(int i = 0; i < 15; i++) {
            b.append(b.toString());
        }
        MEDIUM_STRING = b.toString();
    }
    
    private static final String LONG_STRING;
    static {
        StringBuilder b = new StringBuilder();
        b.append("L");
        for(int i = 0; i < 20; i++) {
            b.append(b.toString());
        }
        LONG_STRING = b.toString();
    }
    
    @Test
    public void test() throws Exception
    {
        createData();
        long maxOriginalMofId = validateData(true);
        backup();
        restore();
        long minNewMofId = validateData(false);
        
        Assert.assertTrue(minNewMofId > maxOriginalMofId);
    }
    
    private void createData()
    {
        getRepository().beginTrans(true);
        try {
            SpecialPackage specialPkg = getSpecialPackage();
            SamplePackage samplePkg = getSamplePackage();
            
            for(Object o: specialPkg.getCornucopia().refAllOfClass()) {
                ((RefObject)o).refDelete();
            }
            
            for(Object o: specialPkg.getIceCreamCone().refAllOfClass()) {
                ((RefObject)o).refDelete();
            }
            
            for(Object o: samplePkg.getDriver().refAllOfClass()) {
                ((RefObject)o).refDelete();
            }
            
            for(Object o: samplePkg.getPassenger().refAllOfClass()) {
                ((RefObject)o).refDelete();
            }
            
            for(Object o: samplePkg.getCar().refAllOfClass()) {
                ((RefObject)o).refDelete();
            }
            
            for(Object o: specialPkg.getCustomTagTarget().refAllOfClass()) {
                ((RefObject)o).refDelete();
            }
            
            // Create objects will all various primitive types, strings,
            // an enumeration, and an association.
            Cornucopia o = 
                specialPkg.getCornucopia().createCornucopia();
            
            o.setBooleanA(CORN_BOOLEANA);
            o.setBooleanB(CORN_BOOLEANB);
            
            o.setIntegerA(CORN_INTA);
            o.setIntegerB(CORN_INTB);
            
            o.setLongA(CORN_LONGA);
            o.setLongB(CORN_LONGB);
            
            o.setFloatA(CORN_FLOATA);
            o.setFloatB(CORN_FLOATB);
            
            o.setDoubleA(CORN_DOUBLEA);
            o.setDoubleB(CORN_DOUBLEB);
            
            Driver d = samplePkg.getDriver().createDriver();
            d.setName(DRIVER_NAME);
            d.setLicense(DRIVER_LICENSE);
            
            Passenger p = samplePkg.getPassenger().createPassenger();
            p.setName(PASSENGER_NAME);
            
            Car car = samplePkg.getCar().createCar();
            car.setDoors(CAR_DOORS);
            car.setDriver(d);
            car.setMake(CAR_MAKE);
            car.setModel(CAR_MODEL);
            car.setOwner(d);
            car.getRider().add(p);
            
            IceCreamCone c1 = 
                specialPkg.getIceCreamCone().createIceCreamCone();
            c1.setFlavor(CONE1_FLAVOR);
            c1.setMelting(CONE1_MELTING);
            c1.setScoops(CONE1_SCOOPS);
            
            IceCreamCone c2 = 
                specialPkg.getIceCreamCone().createIceCreamCone();
            c2.setFlavor(CONE2_FLAVOR);
            c2.setMelting(CONE2_MELTING);
            c2.setScoops(CONE2_SCOOPS);
            
            CustomTagTarget ctt1 = 
                specialPkg.getCustomTagTarget().createCustomTagTarget();
            ctt1.setString192(SHORT_STRING);
            ctt1.setString64k(MEDIUM_STRING);
            ctt1.setString16m(LONG_STRING);
            
            CustomTagTarget ctt2 = 
                specialPkg.getCustomTagTarget().createCustomTagTarget();
            ctt2.setString192("");
            ctt2.setString64k("");
            ctt2.setString16m("");
            
            createAdditionalData();
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    protected void createAdditionalData()
    {
        // Extension point for subclasses.
    }
    
    private long validateData(boolean returnMaxMofId)
    {
        getRepository().endSession();
        getRepository().beginSession();
        
        getRepository().beginTrans(false);
        try {
            SamplePackage samplePkg = getSamplePackage();
            SpecialPackage specialPkg = getSpecialPackage();
            
            Collection<?> corns = 
                specialPkg.getCornucopia().refAllOfClass();
            Assert.assertEquals(1, corns.size());
            
            Collection<?> cars = samplePkg.getCar().refAllOfClass();
            Assert.assertEquals(1, cars.size());
            
            Collection<?> drivers = samplePkg.getDriver().refAllOfClass();
            Assert.assertEquals(1, drivers.size());
            
            Collection<?> passengers = 
                samplePkg.getPassenger().refAllOfClass();
            Assert.assertEquals(1, passengers.size());
            
            Collection<?> cones = 
                specialPkg.getIceCreamCone().refAllOfClass();
            Assert.assertEquals(2, cones.size());
             
            Collection<?> ctts = 
                specialPkg.getCustomTagTarget().refAllOfClass();
            Assert.assertEquals(2, ctts.size());
            
            Cornucopia corn = (Cornucopia)corns.iterator().next();
            Assert.assertEquals(CORN_INTA, corn.getIntegerA());
            Assert.assertEquals(CORN_INTB, corn.getIntegerB());
            Assert.assertEquals(CORN_LONGA, corn.getLongA());
            Assert.assertEquals(CORN_LONGB, corn.getLongB());
            Assert.assertEquals(CORN_FLOATA, corn.getFloatA());
            Assert.assertEquals(CORN_FLOATB, corn.getFloatB());
            
            // N.B.: MySQL rounds on select
            Assert.assertEquals(
                CORN_DOUBLEA, corn.getDoubleA(), 0.00000000001);
            Assert.assertEquals(
                CORN_DOUBLEB, corn.getDoubleB(), 0.00000000001);
            Assert.assertEquals(CORN_BOOLEANA, corn.isBooleanA());
            Assert.assertEquals(CORN_BOOLEANB, corn.isBooleanB());

            Driver driver = (Driver)drivers.iterator().next();            
            Assert.assertEquals(DRIVER_NAME, driver.getName());
            Assert.assertEquals(DRIVER_LICENSE, driver.getLicense());
            
            Passenger passenger = (Passenger)passengers.iterator().next();
            Assert.assertEquals(PASSENGER_NAME, passenger.getName());
            
            Car car = (Car)cars.iterator().next();
            Assert.assertEquals(CAR_MAKE, car.getMake());
            Assert.assertEquals(CAR_MODEL, car.getModel());
            Assert.assertEquals(CAR_DOORS, car.getDoors());

            Assert.assertEquals(driver, car.getDriver());
            Assert.assertEquals(driver, car.getOwner());
            Assert.assertEquals(1, car.getRider().size());
            Assert.assertTrue(car.getRider().contains(passenger));
            
            Iterator<?> iter = cones.iterator();
            IceCreamCone cone1 = (IceCreamCone)iter.next();
            IceCreamCone cone2 = (IceCreamCone)iter.next();
            
            if (!cone1.getFlavor().equals(CONE1_FLAVOR)) {
                IceCreamCone temp = cone1;
                cone1 = cone2;
                cone2 = temp;
            }
            
            Assert.assertEquals(CONE1_FLAVOR, cone1.getFlavor());
            Assert.assertEquals(CONE1_MELTING, cone1.isMelting());
            Assert.assertEquals(CONE1_SCOOPS, cone1.getScoops());
            
            Assert.assertEquals(CONE2_FLAVOR, cone2.getFlavor());
            Assert.assertEquals(CONE2_MELTING, cone2.isMelting());
            Assert.assertEquals(CONE2_SCOOPS, cone2.getScoops());
            
            iter = ctts.iterator();
            CustomTagTarget ctt1 = (CustomTagTarget)iter.next();
            CustomTagTarget ctt2 = (CustomTagTarget)iter.next();

            if (ctt1.getString192().length() == 0) {
                CustomTagTarget temp = ctt1;
                ctt1 = ctt2;
                ctt2 = temp;
            }

            Assert.assertEquals(SHORT_STRING, ctt1.getString192());
            Assert.assertEquals(MEDIUM_STRING, ctt1.getString64k());
            Assert.assertEquals(LONG_STRING, ctt1.getString16m());     
                        
            Assert.assertEquals("", ctt2.getString192());
            Assert.assertEquals("", ctt2.getString64k());
            Assert.assertEquals("", ctt2.getString16m());     

            long additionalMofId = validateAdditionalData(returnMaxMofId);
            
            getSamplePackage().refVerifyConstraints(true);

            long mofId = findMofId(
                returnMaxMofId,
                corns,
                cars,
                drivers,
                passengers,
                cones,
                ctts);
            
            if (returnMaxMofId) {
                return Math.max(mofId, additionalMofId);
            } else {
                return Math.min(mofId, additionalMofId);
            }
        } finally {
            getRepository().endTrans();
        }
    }
    
    protected long validateAdditionalData(boolean returnMaxMofId)
    {
        // Extension point for subclasses.

        if (returnMaxMofId) {
            return Long.MIN_VALUE;
        } else {
            return Long.MAX_VALUE;
        }
    }
    
    protected final long findMofId(
        boolean returnMaxMofId, Collection<?>... collections)
    {
        long result = returnMaxMofId ? Long.MIN_VALUE : Long.MAX_VALUE;
        
        for(Collection<?> c: collections) {
            for(Object o: c) {
                RefBaseObject rob = (RefBaseObject)o;
                
                String refMofId = rob.refMofId();
                long mofId = MofIdUtil.parseMofIdStr(refMofId);
                
                if (returnMaxMofId) {
                    result = Math.max(result, mofId);
                } else {
                    result = Math.min(result, mofId);
                }
            }
        }
        
        return result;
    }
    
    private void backup() throws Exception
    {
        getRepository().endSession();
        getRepository().beginSession();
        
        File file = new File("test/results/BackupRestoreTest.backup");
        
        FileOutputStream output = new FileOutputStream(file);
        
        getRepository().beginTrans(true);
        try {
            getRepository().backupExtent(getTestExtentName(), output);
        } finally {
            getRepository().endTrans(false);
        }
        
        output.flush();
        output.close();
    }
    
    private void restore() throws Exception
    {
        getRepository().endSession();
        getRepository().beginSession();
        
        File file = new File("test/results/BackupRestoreTest.backup");
        
        FileInputStream input = new FileInputStream(file);
        
        try {
            getRepository().beginTrans(true);
            try {
                getRepository().restoreExtent(
                    getTestExtentName(), 
                    "SampleMetamodel", 
                    "EEM", 
                    input);
            } finally {
                getRepository().endTrans(false);
            }
        } finally {
            input.close();
            resetMetaModel();
        }
    }
}
