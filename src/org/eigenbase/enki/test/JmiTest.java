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

import eem.*;
import eem.sample.*;
import eem.sample.simple.*;

/**
 * JmiTest tests read-only methods on various JMI interfaces.
 * 
 * @author Stephan Zuercher
 */
public class JmiTest extends JmiTestBase
{
    @Before
    public void startReadTransaction()
    {
        getRepository().beginTrans(false);
    }

    @After
    public void endReadTransaction()
    {
        getRepository().endTrans();
    }
    
    @Test
    public void testRefObjectRefMetaObject()
    {
        Entity1 e1 = getEntity1Instance();

        List<RefObject> metaObjs = getMetaObjects("Entity1", e1);
        Assert.assertEquals(2, metaObjs.size());

        RefObject metaObj = metaObjs.get(0);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Entity1", ((MofClass)metaObj).getName());

        metaObj = metaObjs.get(1);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Class", ((MofClass)metaObj).getName());
        
        Entity2 e2 = e1.getEntity2();
        List<RefObject> metaObjs2 = getMetaObjects("Entity2", e2);
        Assert.assertEquals(2, metaObjs.size());

        metaObj = metaObjs2.get(0);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Entity2", ((MofClass)metaObj).getName());

        metaObj = metaObjs2.get(1);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Class", ((MofClass)metaObj).getName());
    }

    @Test
    public void testRefPackageRefMetaObject()
    {
        RefPackage refPackage = getEemPackage().getSample();
        
        List<RefObject> metaObjs = getMetaObjects("SamplePackage", refPackage);
        Assert.assertEquals(3, metaObjs.size());
        
        RefObject metaObj = metaObjs.get(0);
        Assert.assertTrue(metaObj instanceof MofPackage);
        Assert.assertEquals("Sample", ((MofPackage)metaObj).getName());

        metaObj = metaObjs.get(1);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Package", ((MofClass)metaObj).getName());

        metaObj = metaObjs.get(2);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Class", ((MofClass)metaObj).getName());
    }
    
    @Test
    public void testRefClassRefMetaObject()
    {
        RefClass refClass =  getSimplePackage().getEntity1();
        
        List<RefObject> metaObjs = getMetaObjects("Entity1 Class", refClass);
        Assert.assertEquals(2, metaObjs.size());

        RefObject metaObj = metaObjs.get(0);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Entity1", ((MofClass)metaObj).getName());

        metaObj = metaObjs.get(1);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Class", ((MofClass)metaObj).getName());
    }
    
    @Test
    public void testRefAssociationRefMetaObject()
    {
        RefAssociation refAssoc = getEemPackage().getSample().getDrives();
        
        List<RefObject> metaObjs = getMetaObjects("DrivesAssoc", refAssoc);
        Assert.assertEquals(3, metaObjs.size());
        
        RefObject metaObj = metaObjs.get(0);
        Assert.assertTrue(metaObj instanceof Association);
        Assert.assertEquals("Drives", ((Association)metaObj).getName());

        metaObj = metaObjs.get(1);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Association", ((MofClass)metaObj).getName());

        metaObj = metaObjs.get(2);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Class", ((MofClass)metaObj).getName());
    }
    
    private List<RefObject> getMetaObjects(String desc, RefBaseObject o)
    {
        ArrayList<RefObject> result = new ArrayList<RefObject>();
        System.out.println(desc);
        try {
            if (o == null) {
                System.out.println("= null early =");
                return result;
            }
            
            RefBaseObject prevRefObj = o;
            for(int i = 0; i < 100; i++) {
                System.out.print(
                    String.valueOf(i) + ": RefObj: " + 
                    prevRefObj.getClass().getName() + " / " + 
                    prevRefObj.refMofId());
                if (prevRefObj instanceof ModelElement) {
                    ModelElement modelElem = (ModelElement)prevRefObj;
                    System.out.print(
                        " (ModelElement.name = " + modelElem.getName() + ")");
                }
                System.out.println();
                
                RefObject refObj = prevRefObj.refMetaObject();
                
                if (refObj == null) {
                    System.out.println("= null =");
                    return result;
                }
                if (refObj == prevRefObj) {
                    System.out.println("= circular =");
                    return result;
                }
                
                result.add(refObj);
                
                prevRefObj = refObj;
            }

            System.out.println("= too many levels =");
            
            return result;
        } finally {
            System.out.println("--");
        }
    }
    
    @Test
    public void testRefObjectMethods()
    {
        Entity1 e1 = getEntity1Instance();
        Entity2 e2 = e1.getEntity2();
        Car car = getCarInstance();

        // Test refClass()
        RefClass e1RefClass = e1.refClass();
        Assert.assertSame(getSimplePackage().getEntity1(), e1RefClass);
        
        RefClass e2RefClass = e2.refClass();
        
        Assert.assertSame(getSimplePackage().getEntity2(), e2RefClass);
        
        // TODO: Test refImmediateComposite
        
        // TODO: Test refOutermoseComposite
        
        // Test refIsInstanceOf
        MofClass e1MofClass =
            (MofClass)getSimplePackage().getEntity1().refMetaObject();
        MofClass e2MofClass =
            (MofClass)getSimplePackage().getEntity2().refMetaObject();

        Assert.assertTrue(e1.refIsInstanceOf(e1MofClass, false));
        Assert.assertTrue(e1.refIsInstanceOf(e1MofClass, true));
        Assert.assertFalse(e1.refIsInstanceOf(e2MofClass, false));
        Assert.assertFalse(e1.refIsInstanceOf(e2MofClass, true));

        Assert.assertTrue(e2.refIsInstanceOf(e2MofClass, false));
        Assert.assertTrue(e2.refIsInstanceOf(e2MofClass, true));
        Assert.assertFalse(e2.refIsInstanceOf(e1MofClass, false));
        Assert.assertFalse(e2.refIsInstanceOf(e1MofClass, true));
        
        MofClass carMofClass =
            (MofClass)getSamplePackage().getCar().refMetaObject();
        MofClass vehicleMofClass =
            (MofClass)getSamplePackage().getVehicle().refMetaObject();
        
        Assert.assertTrue(car.refIsInstanceOf(carMofClass, false));
        Assert.assertTrue(car.refIsInstanceOf(carMofClass, true));
        Assert.assertFalse(car.refIsInstanceOf(vehicleMofClass, false));
        Assert.assertTrue(car.refIsInstanceOf(vehicleMofClass, true));
    }

    @Test
    public void testRefFeaturedMethods()
    {
        Car car = getCarInstance();
        
        Attribute make = null;
        Attribute model = null;
        Attribute doors = null;
        for(Attribute attrib: getAttributes((MofClass)car.refMetaObject())) {
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
        
        // Test refGetValue
        Assert.assertEquals(CAR_MAKE, car.getMake());
        Assert.assertEquals(CAR_MAKE, car.refGetValue("make"));
        Assert.assertEquals(CAR_MAKE, car.refGetValue(make));
        
        Assert.assertEquals(CAR_MODEL, car.getModel());
        Assert.assertEquals(CAR_MODEL, car.refGetValue("model"));
        Assert.assertEquals(CAR_MODEL, car.refGetValue(model));
        
        Assert.assertEquals(CAR_NUM_DOORS, car.getDoors());
        Assert.assertEquals(CAR_NUM_DOORS, car.refGetValue("doors"));
        Assert.assertEquals(CAR_NUM_DOORS, car.refGetValue(doors));
        
        // TODO: Test refInvokeOperation
    }
    
    @Test
    public void testRefPackageMethods()
    {
        // refClass(String/RefObject)
        RefPackage refSimplePackage = getSamplePackage().getSimple();
        
        RefClass e1RefClassByName = refSimplePackage.refClass("Entity1");
        Assert.assertSame(
            getSimplePackage().getEntity1(), e1RefClassByName);
        
        RefClass e2RefClassByName = refSimplePackage.refClass("Entity2");
        Assert.assertSame(
            getSimplePackage().getEntity2(), e2RefClassByName);

        RefObject mofEntity1Class = 
            getSimplePackage().getEntity1().refMetaObject();
        RefClass e1RefClassByInstance = 
            refSimplePackage.refClass(mofEntity1Class);
        Assert.assertSame(
            getSimplePackage().getEntity1(), e1RefClassByInstance);
        
        RefObject mofEntity2Class = 
            getSimplePackage().getEntity2().refMetaObject();
        RefClass e2RefClassByInstance = 
            refSimplePackage.refClass(mofEntity2Class);
        Assert.assertSame(
            getSimplePackage().getEntity2(), e2RefClassByInstance);

        // refPackage(String/RefObject)
        EemPackage eemPackage = getEemPackage();
        
        RefPackage refSamplePackage = eemPackage.refPackage("Sample");
        Assert.assertEquals(getSamplePackage(), refSamplePackage);
        
        RefPackage refSamplePkgByInstance = 
            eemPackage.refPackage(getSamplePackage().refMetaObject());
        Assert.assertEquals(getSamplePackage(), refSamplePkgByInstance);
        

        // refAssociation(String/RefObject)
        SimplePackage simplePackage = getSimplePackage();
        
        HasAnEntity2 hasAnEntity2 = simplePackage.getHasAnEntity2();
        RefAssociation refHasAnEntityAssoc = 
            simplePackage.refAssociation("HasAnEntity2");
        Assert.assertEquals(hasAnEntity2, refHasAnEntityAssoc);
        
        RefAssociation refHasAnEntityAssocByInstance = 
            simplePackage.refAssociation(hasAnEntity2.refMetaObject());
        Assert.assertEquals(hasAnEntity2, refHasAnEntityAssocByInstance);
        
        // refAllClasses
        Collection<?> refAllClasses = getSamplePackage().refAllClasses();
        for(Object o: refAllClasses) {
            Assert.assertTrue(o instanceof RefClass);
            
            RefClass refClass = (RefClass)o;
            
            Assert.assertEquals(
                getSamplePackage(), refClass.refImmediatePackage());
        }
        
        // refAllPackages
        Collection<?> refAllPackages = getEemPackage().refAllPackages();
        Assert.assertTrue(refAllPackages.contains(getSamplePackage()));
        Assert.assertFalse(
            refAllPackages.contains(getSamplePackage().getSimple()));
        for(Object o: refAllPackages) {
            Assert.assertTrue(o instanceof RefPackage);
            
            RefPackage refPackage = (RefPackage)o;
            
            Assert.assertEquals(
                getEemPackage(), refPackage.refImmediatePackage());
        }
        

        // refAllAssociations
        Collection<?> refAllAssociations = 
            refSimplePackage.refAllAssociations();
        for(Object o: refAllAssociations) {
            Assert.assertTrue(o instanceof RefAssociation);

            RefAssociation refAssociation = (RefAssociation)o;
            
            Assert.assertEquals(
                refSimplePackage, refAssociation.refImmediatePackage());
        }
        
        // TODO: refGetEnum

    }
    
    @Test
    public void testRefClassMethods()
    {
        // refAllOfClass
        RefClass refCarClass = getSamplePackage().getCar();
        Collection<?> carInstances = refCarClass.refAllOfClass();
        for(Object o: carInstances) {
            Assert.assertTrue(o instanceof Car);
            
            RefObject refObject = (RefObject)o;
            
            Assert.assertEquals(refCarClass, refObject.refClass());
        }
        Assert.assertFalse(carInstances.isEmpty());
        
        // refAllOfType
        ArrayList<Object> carInstancesCopy = 
            new ArrayList<Object>(carInstances);
        RefClass refVehicleClass = getSamplePackage().getVehicle();
        Collection<?> vehicleInstances = refVehicleClass.refAllOfType();
        for(Object o: vehicleInstances) {
            Assert.assertTrue(o instanceof Vehicle);
            
            if (o instanceof Car) {
                RefObject refObject = (RefObject)o;

                Assert.assertEquals(refCarClass, refObject.refClass());
                
                Assert.assertTrue(carInstancesCopy.remove(refObject));
            }
        }
        Assert.assertTrue(carInstancesCopy.isEmpty());

        // TODO: refGetEnum
    }
}

// End JmiTest.java
