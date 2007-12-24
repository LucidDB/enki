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

import eem.sample.*;

/**
 * JmiWriteTest tests methods on various JMI interfaces.  As its name implies,
 * the test methods modify the repository.
 * 
 * @author Stephan Zuercher
 */
public class JmiWriteTest extends JmiTestBase
{
    private List<String> carMofIds = new ArrayList<String>();
    
    @Before
    public void startWriteTransaction()
    {
        getRepository().beginTrans(true);
    }

    @After
    public void endWriteTransaction()
    {
        getRepository().endTrans();
    }
    
//    @Test
    public void testRefPackageCreateStruct()
    {
        // TODO: refCreateStruct  (Sample model has no structs)
    }
    
//    @Test
    public void testRefPackageDelete()
    {
        // TODO: refDelete (only on outermost package for Netbeans, check for that exception in Hibernate storage)
    }
    
    @Test
    public void testRefClassCreateInstance()
    {
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
    
//    @Test
    public void testRefClassCreateStruct()
    {
        // TODO: refCreateStruct (Sample model has no structs)
    }
    
    @Test
    public void testRefFeaturedSetValue()
    {
        // refCreateInstance
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

        List<?> noArgs = Collections.emptyList();
        Car car1 = (Car)refCarClass.refCreateInstance(noArgs);
        car1.refSetValue("make", "McLaren");
        car1.refSetValue("model", "F1");
        car1.refSetValue("doors", Integer.valueOf(2));
        
        Car car2 = (Car)refCarClass.refCreateInstance(noArgs);
        car2.refSetValue(make, "Tesla");
        car2.refSetValue(model, "Roadster");
        car2.refSetValue(doors, Integer.valueOf(2));
        
        carMofIds.add(car1.refMofId());
        carMofIds.add(car2.refMofId());
    }
    
    @Test
    public void testRefObjectDelete()
    {
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
}

// End JmiWriteTest.java
