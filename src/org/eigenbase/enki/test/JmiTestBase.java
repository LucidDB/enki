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

import org.eigenbase.enki.util.*;
import org.junit.*;

import eem.sample.*;
import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * JmiTestBase provides basic functionality common to {@link JmiTest} and
 * {@link JmiWriteTest}.
 * 
 * @author Stephan Zuercher
 */
public class JmiTestBase extends SampleModelTestBase
{
    protected static final String CAR_MAKE = "Ferrari";
    protected static final String CAR_MODEL = "GTB 599";
    protected static final int CAR_NUM_DOORS = 2;

    protected static final String CAR2_MAKE = "Jaguar";
    protected static final String CAR2_MODEL = "XF";
    protected static final int CAR2_NUM_DOORS = 4;

    private static String e1MofId;
    private static String carMofId;

    /**
     * Creates some basic entities for use in tests. The objects' MOF IDs are
     * recorded for later use in {@link #getCarInstance()} and 
     * {@link #getEntity1Instance()}.
     */
    @BeforeClass
    public static void createEntities()
    {
        getRepository().beginTrans(true);
    
        boolean rollback = true;
        
        try {
            // 1-to-1 association
            Entity1 e1 = getSimplePackage().getEntity1().createEntity1();
            
            Entity2 e2 = getSimplePackage().getEntity2().createEntity2();
            
            e1.setEntity2(e2);
            
            // many-to-many association
            Car car = getSamplePackage().getCar().createCar();
            car.setMake(CAR_MAKE);
            car.setModel(CAR_MODEL);
            car.setDoors(CAR_NUM_DOORS);

            Car car2 = getSamplePackage().getCar().createCar();
            car2.setMake(CAR2_MAKE);
            car2.setModel(CAR2_MODEL);
            car2.setDoors(CAR2_NUM_DOORS);

            State ca = getSamplePackage().getState().createState("CA");
            State nv = getSamplePackage().getState().createState("NV");
            State or = getSamplePackage().getState().createState("OR");
            
            car.getRegistrar().add(ca);
            car.getRegistrar().add(nv);
            
            car2.getRegistrar().add(ca);
            car2.getRegistrar().add(or);
            
            // 1-to-many association
            SampleContainer container = 
                getSpecialPackage().getSampleContainer().createSampleContainer(
                    "a container");
            
            SampleEntity entity = 
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "an entity");
            
            SampleContainer anotherContainer =
                getSpecialPackage().getSampleContainer().createSampleContainer(
                    "another container");
            
            SampleEntity anotherEntity =
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "another entity");
            
            container.getContainedEntity().add(entity);
            container.getContainedEntity().add(anotherContainer);
            anotherContainer.getContainedEntity().add(anotherEntity);
            
            e1MofId = e1.refMofId();
            carMofId = car.refMofId();
            
            rollback = false;
        } finally {
            getRepository().endTrans(rollback);
        }
    }

    /**
     * @return the instance of Entity1 created by {@link #createEntities()}.
     */
    protected static Entity1 getEntity1Instance()
    {
        Collection<Entity1> all =
            GenericCollections.asTypedCollection(
                getSimplePackage().getEntity1().refAllOfClass(),
                Entity1.class);
        Assert.assertTrue(all.size() >= 1);
        
        for(Entity1 e1: all) {
            if (e1.refMofId().equals(e1MofId)) {
                return e1;
            }
        }
        
        Assert.fail("Could not find Entity1 with MOF ID = " + e1MofId);
        return null; // unreachable
    }

    /**
     * @return the instance of Car created by {@link #createEntities()}.
     */
    protected static Car getCarInstance()
    {
        Collection<Car> all =
            GenericCollections.asTypedCollection(
                getSamplePackage().getCar().refAllOfClass(),
                Car.class);
        Assert.assertTrue(all.size() >= 1);
        
        for(Car car: all) {
            if (car.refMofId().equals(carMofId)) {
                return car;
            }
        }
        
        Assert.fail("Could not find Car with MOF ID = " + carMofId);
        return null; // unreachable
    }
    
    /**
     * Retrieves all {@link Attribute} instances from the contents of
     * the given {@link RefObject} and its super types.
     * 
     * @param type a MofClass instance
     * @return List of Attribute instances for the object's type and 
     *         super-types
     */
    protected static List<Attribute> getAttributes(MofClass type)
    {
        ArrayList<Attribute> attribs = new ArrayList<Attribute>();
        
        List<ModelElement> contents = 
            GenericCollections.asTypedList(
                type.getContents(), ModelElement.class);
        for(ModelElement me: contents) {
            if (me instanceof Attribute) {
                attribs.add((Attribute)me);
            }
        }
        
        List<MofClass> superTypes = 
            GenericCollections.asTypedList(
                type.allSupertypes(), MofClass.class);
        for(MofClass superType: superTypes) {
            contents = 
                GenericCollections.asTypedList(
                    superType.getContents(), ModelElement.class);
            for(ModelElement me: contents) {
                if (me instanceof Attribute) {
                    if (me instanceof Attribute) {
                        attribs.add((Attribute)me);
                    }
                }
            }
        }
        
        return attribs;
    }

}

// End JmiTestBsae.java
