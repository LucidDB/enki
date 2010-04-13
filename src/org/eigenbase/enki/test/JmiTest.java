/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;

import eem.*;
import eem.sample.*;
import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * JmiTest tests read-only methods on various JMI interfaces.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
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
    
    @Test
    public void testMetamodelRefMetaObject()
    {
        String metamodelExtentName = null;
        for(String extentName: getRepository().getExtentNames()) {
            if (!extentName.equals(getTestExtentName())) {
                metamodelExtentName = extentName;
                break;
            }
        }
        Assert.assertNotNull(metamodelExtentName);
        
        RefPackage p = getRepository().getExtent(metamodelExtentName);
        Assert.assertNotNull(p);
        
        ModelPackage modelPkg = (ModelPackage)p;
        
        List<RefObject> metaObjs = 
            getMetaObjects("Sample Model Package", modelPkg);
        Assert.assertEquals(3, metaObjs.size());
        
        RefObject metaObj = metaObjs.get(0);
        Assert.assertTrue(metaObj instanceof MofPackage);
        Assert.assertEquals("Model", ((MofPackage)metaObj).getName());

        metaObj = metaObjs.get(1);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Package", ((MofClass)metaObj).getName());

        metaObj = metaObjs.get(2);
        Assert.assertTrue(metaObj instanceof MofClass);
        Assert.assertEquals("Class", ((MofClass)metaObj).getName());
    }
    
    private List<RefObject> getMetaObjects(String desc, RefBaseObject o)
    {
        Logger log = ModelTestBase.getTestLogger();
        
        ArrayList<RefObject> result = new ArrayList<RefObject>();
        log.info(desc);
        if (o == null) {
            log.info("= null early =");
            return result;
        }
        
        RefBaseObject prevRefObj = o;
        for(int i = 0; i < 100; i++) {
            StringBuilder b = new StringBuilder();
            b
                .append(String.valueOf(i))
                .append(": RefObj: ") 
                .append(prevRefObj.getClass().getName())
                .append(" / ")
                .append(prevRefObj.refMofId());
            if (prevRefObj instanceof ModelElement) {
                ModelElement modelElem = (ModelElement)prevRefObj;
                b.append(
                    " (ModelElement.name = " + modelElem.getName() + ")");
            }
            log.info(b.toString());
            
            RefObject refObj = prevRefObj.refMetaObject();
            
            if (refObj == null) {
                log.info("= null =");
                return result;
            }
            if (refObj == prevRefObj) {
                log.info("= circular =");
                return result;
            }
            
            result.add(refObj);
            
            prevRefObj = refObj;
        }

        log.info("= too many levels =");
        
        return result;
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
        
        // Test refImmediateComposite: see CompositeAssociationTest
        
        // Test refOutermoseComposite: see CompositeAssociationTest
        
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

        
        PhoneNumber phoneNumber = getPhoneNumberInstance();
        
        Attribute number = null;
        Attribute areaCodeAttrib = null;
        for(Attribute attrib: 
                getAttributes((MofClass)phoneNumber.refMetaObject()))
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

        
        AreaCode areaCode = phoneNumber.getAreaCode();

        Attribute domestic = null;
        Attribute code = null;
        for(Attribute attrib: 
                getAttributes((MofClass)areaCode.refMetaObject()))
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

        
        IceCreamCone cone = getIceCreamConeInstance();
        
        Attribute flavor = null;
        Attribute scoops = null;
        Attribute isMelting = null;
        for(Attribute attrib: 
                getAttributes((MofClass)cone.refMetaObject()))
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

        Assert.assertEquals(areaCode, phoneNumber.refGetValue(areaCodeAttrib));
        Assert.assertEquals(PHONE_NUMBER, phoneNumber.refGetValue(number));
        Assert.assertEquals(AREA_CODE, areaCode.refGetValue(code));
        Assert.assertEquals(DOMESTIC, areaCode.refGetValue(domestic));
        
        Assert.assertEquals(FLAVOR, cone.refGetValue(flavor));
        Assert.assertEquals(NUM_SCOOPS, cone.refGetValue(scoops));
        Assert.assertEquals(IS_MELTING, cone.refGetValue(isMelting));
        
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

    @Test
    public void testRefAssociationMethodsOnOneToOne()
    {
        HasAnEntity2 hasAnEntity2Assoc = getSimplePackage().getHasAnEntity2();

        // test refAllLinks()
        Collection<RefAssociationLink> links =
            GenericCollections.asTypedCollection(
                hasAnEntity2Assoc.refAllLinks(),
                RefAssociationLink.class);
        Assert.assertEquals(1, links.size());
        RefAssociationLink link = links.iterator().next();
        
        Assert.assertTrue(link.refFirstEnd() instanceof Entity1);
        Assert.assertTrue(link.refSecondEnd() instanceof Entity2);
        
        Entity1 e1 = (Entity1)link.refFirstEnd();
        Entity2 e2 = (Entity2)link.refSecondEnd();
        
        Assert.assertEquals(e2, e1.getEntity2());
        Assert.assertEquals(e1, e2.getEntity1());
        
        // test refLinkExists()
        Assert.assertTrue(
            hasAnEntity2Assoc.refLinkExists(e1, e2));
        
        // TODO: refLinkExists negative test
        
        // test refQuery
        Association mofAssoc = (Association)hasAnEntity2Assoc.refMetaObject();
        AssociationEnd[] ends = new AssociationEnd[2];
        int i = 0;
        for(Object o: mofAssoc.getContents()) {
            if (o instanceof AssociationEnd) {
                ends[i++] = (AssociationEnd)o;
            }
        }
        
        Collection<Entity2> entities2ByQuery =
            GenericCollections.asTypedCollection(
                hasAnEntity2Assoc.refQuery(ends[0], e1),
                Entity2.class);
        Assert.assertEquals(1, entities2ByQuery.size());
        Entity2 e2ByQuery = entities2ByQuery.iterator().next();
        Assert.assertEquals(e2, e2ByQuery);
        
        Collection<Entity1> entities1ByQuery =
            GenericCollections.asTypedCollection(
                hasAnEntity2Assoc.refQuery(ends[1], e2),
                Entity1.class);
        Assert.assertEquals(1, entities1ByQuery.size());
        Entity1 e1ByQuery = entities1ByQuery.iterator().next();
        Assert.assertEquals(e1, e1ByQuery);
        
        // negative refQuery
        try {
            hasAnEntity2Assoc.refQuery(ends[1], e1);
            Assert.fail("expected exception");
        } catch(Exception e) {
            // expected
        }
        try {
            hasAnEntity2Assoc.refQuery(ends[0], e2);
            Assert.fail("expected exception");
        } catch(Exception e) {
            // expected
        }
    }
    

    @Test
    public void testRefAssociationMethodsOnOneToMany()
    {
        eem.sample.special.Contains containsAssoc = 
            getSpecialPackage().getContains();

        // test refAllLinks()
        Collection<RefAssociationLink> links =
            GenericCollections.asTypedCollection(
                containsAssoc.refAllLinks(),
                RefAssociationLink.class);
        Assert.assertEquals(3, links.size());

        SampleContainer aContainer = null;
        SampleContainer anotherContainer = null;
        Entity anEntity = null;
        Entity anotherEntity = null;

        for(RefAssociationLink link: links) {
            SampleContainer container = (SampleContainer)link.refFirstEnd();
            Entity entity = (Entity)link.refSecondEnd();
            
            if (container.getName().equals("a container")) {
                aContainer = container;
                if (entity instanceof SampleContainer) {
                    Assert.assertEquals("another container", entity.getName());
                    Assert.assertNull(anotherContainer);
                    anotherContainer = (SampleContainer)entity;
                } else {
                    Assert.assertEquals("an entity", entity.getName());
                    Assert.assertNull(anEntity);
                    anEntity = entity;
                }
            } else if (container.getName().equals("another container")) {
                Assert.assertFalse(entity instanceof Container);
                Assert.assertEquals("another entity", entity.getName());
                anotherContainer = container;
                Assert.assertNull(anotherEntity);
                anotherEntity = entity;
            } else {
                Assert.fail("unknown link");
            }
        }
        Assert.assertNotNull(aContainer);
        Assert.assertNotNull(anotherContainer);        
        Assert.assertNotNull(anEntity);
        Assert.assertNotNull(anotherEntity);
        
        Assert.assertEquals(aContainer, anotherContainer.getEntityContainer());
        Assert.assertEquals(aContainer, anEntity.getEntityContainer());
        Assert.assertEquals(
            anotherContainer, anotherEntity.getEntityContainer());
        
        // test refLinkExists()
        Assert.assertTrue(
            containsAssoc.refLinkExists(aContainer, anotherContainer));
        Assert.assertTrue(
            containsAssoc.refLinkExists(aContainer, anEntity));
        Assert.assertTrue(
            containsAssoc.refLinkExists(anotherContainer, anotherEntity));
        
        Assert.assertFalse(
            containsAssoc.refLinkExists(aContainer, anotherEntity));
        Assert.assertFalse(
            containsAssoc.refLinkExists(anotherContainer, anEntity));
        
        // test refQuery
        Association mofAssoc = (Association)containsAssoc.refMetaObject();
        AssociationEnd[] ends = new AssociationEnd[2];
        int i = 0;
        for(Object o: mofAssoc.getContents()) {
            if (o instanceof AssociationEnd) {
                ends[i++] = (AssociationEnd)o;
            }
        }
        
        Collection<SampleContainer> containersByQuery =
            GenericCollections.asTypedCollection(
                containsAssoc.refQuery(ends[1], anEntity),
                SampleContainer.class);
        Assert.assertEquals(1, containersByQuery.size());
        SampleContainer aContainerByQuery = 
            containersByQuery.iterator().next();
        Assert.assertEquals(aContainer, aContainerByQuery);
        
        Collection<SampleContainer> containersByQuery2 =
            GenericCollections.asTypedCollection(
                containsAssoc.refQuery(ends[1], anotherEntity),
                SampleContainer.class);
        Assert.assertEquals(1, containersByQuery2.size());
        SampleContainer anotherContainerByQuery = 
            containersByQuery2.iterator().next();
        Assert.assertEquals(anotherContainer, anotherContainerByQuery);

        Collection<Entity> entitiesByQuery =
            GenericCollections.asTypedCollection(
                containsAssoc.refQuery(ends[0], aContainer),
                Entity.class);
        Assert.assertEquals(2, entitiesByQuery.size());
        SampleContainer anotherContainerByQuery2 = null;
        Entity anEntityByQuery = null;
        for(Entity entity: entitiesByQuery) {
            if (entity instanceof SampleEntity) {
                Assert.assertNull(anEntityByQuery);
                anEntityByQuery = entity;
            } else {
                Assert.assertNull(anotherContainerByQuery2);
                anotherContainerByQuery2 = (SampleContainer)entity;
            }
        }
        Assert.assertNotNull(anotherContainerByQuery2);
        Assert.assertNotNull(anEntityByQuery);
        Assert.assertEquals(anotherContainer, anotherContainerByQuery2);
        Assert.assertEquals(anEntity, anEntityByQuery);
        
        // negative refQuery
        try {
            containsAssoc.refQuery(ends[0], anEntity);
            Assert.fail("expected exception");
        } catch(Exception e) {
            // expected
        }
        try {
            containsAssoc.refQuery(ends[0], anotherEntity);
            Assert.fail("expected exception");
        } catch(Exception e) {
            // expected
        }
    }
    
    @Test
    public void testRefAssociationMethodsOnManyToMany()
    {
        Registrations registrationsAssoc =
            getSamplePackage().getRegistrations();

        // test refAllLinks()
        Collection<RefAssociationLink> links =
            GenericCollections.asTypedCollection(
                registrationsAssoc.refAllLinks(),
                RefAssociationLink.class);
        Assert.assertEquals(4, links.size());

        Car car1 = null;
        Car car2 = null;
        State ca = null;
        State nv = null;
        State or = null;
        Set<State> car1States = new HashSet<State>();
        Set<State> car2States = new HashSet<State>();
        for(RefAssociationLink link: links) {
            Car car = (Car)link.refFirstEnd();
            State state = (State)link.refSecondEnd();
            
            if (state.getName().equals("CA")) {
                if (ca != null) {
                    Assert.assertEquals(ca, state);
                } else {
                    ca = state;
                }
            } else if (state.getName().equals("NV")) {
                if (nv != null) {
                    Assert.assertEquals(nv, state);
                } else {
                    nv = state;
                }
            } else if (state.getName().equals("OR")) {
                if (or != null) {
                    Assert.assertEquals(or, state);
                } else {
                    or = state;
                }
            } else {
                Assert.fail("unknown state");
            }

            if (car.getMake().equals(CAR_MAKE)) {
                if (car1 != null) {
                    Assert.assertEquals(car1, car);
                } else {
                    Assert.assertEquals(CAR_MODEL, car.getModel());
                    Assert.assertEquals(CAR_NUM_DOORS, car.getDoors());
                    car1 = car;
                }
                car1States.add(state);
            } else if (car.getMake().equals(CAR2_MAKE)) {
                if (car2 != null) {
                    Assert.assertEquals(car2, car);
                } else {
                    Assert.assertEquals(CAR2_MODEL, car.getModel());
                    Assert.assertEquals(CAR2_NUM_DOORS, car.getDoors());
                    car2 = car;
                }
                car2States.add(state);
            } else {
                Assert.fail("unknown car");
            }
        }
        Assert.assertNotNull(car1);
        Assert.assertNotNull(car2);
        Assert.assertNotNull(ca);
        Assert.assertNotNull(nv);
        Assert.assertNotNull(or);
        Assert.assertEquals(2, car1States.size());
        Assert.assertTrue(car1States.contains(ca));
        Assert.assertTrue(car1States.contains(nv));
        Assert.assertEquals(2, car2States.size());
        Assert.assertTrue(car2States.contains(ca));
        Assert.assertTrue(car2States.contains(or));
        
        // test refLinkExists()
        Assert.assertTrue(
            registrationsAssoc.refLinkExists(car1, ca));
        Assert.assertTrue(
            registrationsAssoc.refLinkExists(car1, nv));
        Assert.assertTrue(
            registrationsAssoc.refLinkExists(car2, ca));
        Assert.assertTrue(
            registrationsAssoc.refLinkExists(car2, or));

        Assert.assertFalse(
            registrationsAssoc.refLinkExists(car1, or));
        Assert.assertFalse(
            registrationsAssoc.refLinkExists(car2, nv));
        
        // test refQuery
        Association mofAssoc = (Association)registrationsAssoc.refMetaObject();
        AssociationEnd[] ends = new AssociationEnd[2];
        int i = 0;
        for(Object o: mofAssoc.getContents()) {
            if (o instanceof AssociationEnd) {
                ends[i++] = (AssociationEnd)o;
            }
        }
        
        Collection<Car> carsByQuery =
            GenericCollections.asTypedCollection(
                registrationsAssoc.refQuery(ends[1], ca),
                Car.class);
        Assert.assertEquals(2, carsByQuery.size());
        Assert.assertTrue(carsByQuery.contains(car1));
        Assert.assertTrue(carsByQuery.contains(car2));

        Collection<Car> carsByQuery2 =
            GenericCollections.asTypedCollection(
                registrationsAssoc.refQuery(ends[1], nv),
                Car.class);
        Assert.assertEquals(1, carsByQuery2.size());
        Car car1ByQuery2 = carsByQuery2.iterator().next();
        Assert.assertEquals(car1, car1ByQuery2);
        
        Collection<State> statesByQuery =
            GenericCollections.asTypedCollection(
                registrationsAssoc.refQuery(ends[0], car1),
                State.class);
        Assert.assertEquals(2, statesByQuery.size());
        Assert.assertTrue(statesByQuery.contains(ca));
        Assert.assertTrue(statesByQuery.contains(nv));

        // negative refQuery
        try {
            registrationsAssoc.refQuery(ends[1], car1);
            Assert.fail("expected exception");
        } catch(Exception e) {
            // expected
        }
        try {
            registrationsAssoc.refQuery(ends[0], ca);
            Assert.fail("expected exception");
        } catch(Exception e) {
            // expected
        }
        try {
            registrationsAssoc.refQuery(ends[0], nv);
            Assert.fail("expected exception");
        } catch(Exception e) {
            // expected
        }
    }
    
    @Test
    public void testMofExtent()
    {
        getRepository().beginTrans(false);
        try {
            RefPackage pkg = getRepository().getExtent("MOF");
            Assert.assertNotNull(pkg);
            
            MofPackage mofPkg = (MofPackage)(pkg.refMetaObject());
            Assert.assertNotNull(mofPkg);
            Assert.assertNotNull(mofPkg.getContents());
        }
        finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testEqualsWithClassMismatch()
    {
        Car car = getCarInstance();
        Assert.assertFalse(car.equals("tesla"));
    }

    @Test
    public void testStructFieldOrder()
        throws Exception
    {
        // See ENK-3 for commentary.  We want to verify that
        // MultiplicityType's fields always come out in the
        // correct order.
        
        Car car = getCarInstance();

        // any attribute will do
        Attribute attrib =
            getAttributes((MofClass)car.refMetaObject()).get(0);
        MultiplicityType mt = attrib.getMultiplicity();


        List<String> expectedNames = new ArrayList<String>();
        expectedNames.add("lower");
        expectedNames.add("upper");
        expectedNames.add("isOrdered");
        expectedNames.add("isUnique");
        Assert.assertEquals(expectedNames, mt.refFieldNames());

        // Rest of this should probably be in MofModelTest instead.
        // Verify that in MOF, the corresponding fields of
        // Multiplicity's StructureType also match.

        // Find the class descriptor for Attribute in the MOF Model
        MofClass attribClass = (MofClass) attrib.refMetaObject();

        // Walk up to its parent class (StructuralFeature).
        MofClass structuralFeatureClass =
            (MofClass) attribClass.getSupertypes().get(0);
        
        // StructuralFeature has an attribute named multiplicity, which
        // is what we're after.
        TypedElement te = (TypedElement)
            structuralFeatureClass.lookupElement("multiplicity");

        // Get that attribute's type, which should be a
        // StructureType.
        StructureType st = (StructureType) te.getType();

        // Enumerate that StructureType's fields.
        List<String> mofFieldNames = new ArrayList<String>();
        System.out.println("CONTENTS = " + st.getContents().getClass().getName());
        for (Object obj : st.getContents()) {
            StructureField sf = (StructureField) obj;
            mofFieldNames.add(sf.getName());
        }
        Assert.assertEquals(expectedNames, mofFieldNames);
    }
}

// End JmiTest.java
