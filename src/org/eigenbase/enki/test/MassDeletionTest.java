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

import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;
import org.junit.*;

import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * MassDeletionTest tests the mass deletion API.
 * 
 * @author Stephan Zuercher
 */
public class MassDeletionTest extends SampleModelTestBase
{
    private List<String> createObjects()
    {
        return createObjects(false);
    }
    
    private List<String> createObjects(boolean includeCompositions)
    {
        getRepository().beginTrans(true);
        try {
            SampleContainer container1 =
                getSpecialPackage().getSampleContainer().createSampleContainer(
                    "container1");
            
            SampleEntity entity1 = 
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "entity1");

            SampleEntity entity2 = 
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "entity2");
            
            SampleContainer container2 =
                getSpecialPackage().getSampleContainer().createSampleContainer(
                    "container2");

            container1.getContainedEntity().add(entity1);
            container1.getContainedEntity().add(entity2);
            container1.getContainedEntity().add(container2);
            
            SampleEntity entity3 = 
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "entity3");

            SampleEntity entity4 = 
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "entity4");

            SampleContainer container3 =
                getSpecialPackage().getSampleContainer().createSampleContainer(
                    "container3");

            container2.getContainedEntity().add(entity3);
            container2.getContainedEntity().add(entity4);
            container2.getContainedEntity().add(container3);
            
            SampleEntity entity5 = 
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "entity5");

            SampleEntity entity6 = 
                getSpecialPackage().getSampleEntity().createSampleEntity(
                    "entity6");
            
            container3.getContainedEntity().add(entity5);            
            container3.getContainedEntity().add(entity6);
            
            List<String> result = new ArrayList<String>();
            
            result.add(container1.refMofId());
            result.add(entity1.refMofId());
            result.add(entity2.refMofId());
            result.add(container2.refMofId());
            result.add(entity3.refMofId());
            result.add(entity4.refMofId());
            result.add(container3.refMofId());
            result.add(entity5.refMofId());
            result.add(entity6.refMofId());
            
            if (includeCompositions) {
                AreaCode ac = 
                    getSpecialPackage().getAreaCode().createAreaCode(
                        "415", true);
                PhoneNumber pn = 
                    getSpecialPackage().getPhoneNumber().createPhoneNumber(
                        ac, "867-5309");
                
                result.add(ac.refMofId());
                result.add(pn.refMofId());
            }
            
            return result;
        }
        finally {
            getRepository().endTrans(false);
        }
    }
    
    private List<String> createOrderedObjects()
    {
        List<String> result = new ArrayList<String>();
        
        getRepository().beginTrans(true);
        try {
            Entity16 e16 = getSimplePackage().getEntity16().createEntity16();
            result.add(e16.refMofId());
            
            for(int i = 0; i < 10; i++) {
                Entity17 e17 = 
                    getSimplePackage().getEntity17().createEntity17();
                e16.getEntity17().add(e17);
                result.add(e17.refMofId());
            }
        }
        finally {
            getRepository().endTrans(false);
        }
        
        return result;
    }

    @Test
    public void testMassDeletion()
    {
        List<String> mofIds = createObjects();
        
        getRepository().beginTrans(true);
        try {
            List<RefObject> objects = new ArrayList<RefObject>(mofIds.size());
            
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNotNull(object);
                objects.add(object);
            }
            
            Collections.reverse(objects);
      
            getRepository().delete(objects);

            getRepository().endTrans(false);
        }
        catch(RuntimeException t) {
            getRepository().endTrans(true);
            
            throw t;
        }
        
        getRepository().beginTrans(false);
        try {
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNull("MOF ID: " + mofId, object);
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testMassDeletionWithDanglingRefs()
    {
        List<String> mofIds = createObjects();
        
        getRepository().beginTrans(true);
        try {
            List<RefObject> objects = new ArrayList<RefObject>(mofIds.size());
            
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNotNull(object);
                
                String name = (String)object.refGetValue("name");
                if (name.equals("container2") || name.equals("entity6")) {
                    continue;
                }
                
                objects.add(object);
            }
            
            Collections.reverse(objects);
      
            getRepository().delete(objects);

            getRepository().endTrans(false);
        }
        catch(RuntimeException t) {
            getRepository().endTrans(true);
            
            throw t;
        }
        
        getRepository().beginTrans(false);
        try {
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                
                if (object != null) {
                    String name = (String)object.refGetValue("name");
                    Assert.assertTrue(
                        "Name: " + name,
                        name.equals("container2") || name.equals("entity6"));
                        
                } else {
                    Assert.assertNull("MOF ID: " + mofId, object);
                }
            }
        }
        finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testMassDeletionWithDanglingRefs2()
    {
        List<String> mofIds = createObjects();
        
        getRepository().beginTrans(true);
        try {
            List<RefObject> objects = new ArrayList<RefObject>(mofIds.size());
            
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNotNull(object);
                
                String name = (String)object.refGetValue("name");
                if (name.equals("container1") || name.equals("entity1")) {
                    continue;
                }
                
                objects.add(object);
            }
            
            Collections.reverse(objects);
      
            getRepository().delete(objects);

            getRepository().endTrans(false);
        }
        catch(RuntimeException t) {
            getRepository().endTrans(true);
            
            throw t;
        }
        
        getRepository().beginTrans(false);
        try {
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                
                if (object != null) {
                    String name = (String)object.refGetValue("name");
                    Assert.assertTrue(
                        "Name: " + name,
                        name.equals("container1") || name.equals("entity1"));
                        
                } else {
                    Assert.assertNull("MOF ID: " + mofId, object);
                }
            }
        }
        finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testMassDeletionWithComposition()
    {
        List<String> mofIds = createObjects(true);
        
        getRepository().beginTrans(true);
        try {
            List<RefObject> objects = new ArrayList<RefObject>(mofIds.size());
            
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNotNull(object);
                
                if (object instanceof AreaCode) {
                    continue;
                }
                
                objects.add(object);
            }
            
            getRepository().delete(objects);

            getRepository().endTrans(false);
        }
        catch(RuntimeException t) {
            getRepository().endTrans(true);
            
            throw t;
        }
        
        getRepository().beginTrans(false);
        try {
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNull("MOF ID: " + mofId, object);
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        // TODO: test that no association exists without a reference to it
        // (e.g., make sure all assocs point to an object that points back)
    }
    
    @Ignore // BROKEN!  Need to fix up ordinal values
    @Test
    public void testMassDeletionOrdered()
    {
        // Test what happens when mass deletion removes items from an ordered
        // association
        
        List<String> mofIds = createOrderedObjects();
        List<String> preservedMofIds = new ArrayList<String>();
        
        preservedMofIds.add(mofIds.remove(0));
        
        // Delete all but first and last Entity17 instances.
        getRepository().beginTrans(true);
        try {
            List<RefObject> objects = new ArrayList<RefObject>(mofIds.size());
            
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNotNull(object);
                objects.add(object);
            }
            
            objects.remove(9);
            objects.remove(0);
        
            preservedMofIds.add(mofIds.remove(9));
            preservedMofIds.add(mofIds.remove(0));
      
            getRepository().delete(objects);
    
            getRepository().endTrans(false);
        }
        catch(RuntimeException t) {
            getRepository().endTrans(true);
            
            throw t;
        }
        
        // Verify the right objects were deleted.
        getRepository().beginTrans(false);
        try {
            for(String mofId: mofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNull("MOF ID: " + mofId, object);
            }

            for(String mofId: preservedMofIds) {
                RefObject object = 
                    (RefObject)getRepository().getByMofId(mofId);
                Assert.assertNotNull("MOF ID: " + mofId, object);
            }            
        }
        finally {
            getRepository().endTrans(false);
        }

        // Access the Entity17 instances via the association to make sure
        // we didn't hose it (by leaving the ordinals with gaps).
        getRepository().beginTrans(false);
        try {
            String e16MofId = preservedMofIds.get(0);
            Entity16 e16 = (Entity16)getRepository().getByMofId(e16MofId);
            Assert.assertNotNull("E16 MOF ID: " + e16MofId, e16);

            List<Entity17> e17s = e16.getEntity17();

            Assert.assertEquals(
                preservedMofIds.get(1), e17s.get(0).refMofId());
            Assert.assertEquals(
                preservedMofIds.get(2), e17s.get(1).refMofId());

        }
        finally {
            getRepository().endTrans(false);
        }
    }
    
    @After
    public void checkIntegrity()
    {
        getRepository().endSession();
        getRepository().beginSession();
        getRepository().beginTrans(false);
        try {
            Collection<?> containers = 
                getSpecialPackage().getSampleContainer().refAllOfType();
            
            for(SampleContainer container: 
                GenericCollections.asTypedCollection(
                    containers, SampleContainer.class))
            {
                container.getContainedEntity();
            }
            
            Collection<?> entities = 
                getSpecialPackage().getSampleEntity().refAllOfType();
            
            for(SampleEntity entity: 
                GenericCollections.asTypedCollection(
                    entities, SampleEntity.class))
            {
                entity.getEntityContainer();
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
}

// End MassDeletionTest.java
