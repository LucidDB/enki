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

import eem.sample.*;

/**
 * MdrApiTest tests various aspects of the MDR API.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class MdrApiTest extends SampleModelTestBase
{
    private static String carMofId;
    
    @BeforeClass
    public static void setupCar()
    {
        getRepository().beginTrans(true);
        try {
            Car car = getSamplePackage().getCar().createCar("TATA", "Sumo", 4);
            
            carMofId = car.refMofId();
        }
        finally {
            getRepository().endTrans(false);
        }
    }
    
    @Test
    public void testImplicitTransactions()
    {
        // Perform without a transaction
        Car car = findCar();
        Assert.assertNotNull(car);
        
        // Repeat with a transaction
        getRepository().beginTrans(false);
        try {
            car = findCar();
            Assert.assertNotNull(car);
        }
        finally {
            getRepository().endTrans();
        }
        
        try {
            getRepository().endTrans();
            Assert.fail("expected exception");
        } catch(Throwable t) {
            // expected
        }
        
    }

    private Car findCar()
    {
        Car car = null;
        Collection<?> cars = getSamplePackage().getCar().refAllOfClass();
        for(Car c: GenericCollections.asTypedCollection(cars, Car.class)) {
            if (c.refMofId().equals(carMofId)) {
                car = c;
                break;
            }
        }
        
        return car;
    }
}

// End MdrApiTest.java
