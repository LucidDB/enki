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

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * MassDeletionTest tests the mass deletion API.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
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
            
            objects.remove(0);
            objects.remove(objects.size() - 1);
        
            preservedMofIds.add(mofIds.remove(0));
            preservedMofIds.add(mofIds.remove(mofIds.size() - 1));
      
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

    // REVIEW SWZ 16-Oct-2008:  This test may want to move to some separate 
    // performance suite.
    @Test
    public void testHighFanOutUnordered()
    {
        // JVS: This test can be used for checking whether deletion from high 
        // fanout associations is being optimized.  When optimized, it runs in 
        // about 8 seconds on my laptop.  When not optimized, it takes 150-some
        // seconds.
        // SWZ: Test is presently optimized (HasEntity13 is marked as a
        // high cardinality association).

        // For the LucidDB drop scenarios, I also had to disable
        // hibernate.default_batch_fetch_size to optimize it, but that
        // doesn't seem to come into play in this unit test scenario.
        // I haven't come up with a unit test scenario where it matters.
        
        List<String> result = new ArrayList<String>();

        // Populate a high fanout association instance.
        getRepository().beginTrans(true);
        try {
            Entity12 e12 = getSimplePackage().getEntity12().createEntity12();
            
            for(int i = 0; i < 10000; i++) {
                Entity13 e13 = 
                    getSimplePackage().getEntity13().createEntity13();
                e12.getEntity13().add(e13);
                result.add(e13.refMofId());
            }
        } finally {
            getRepository().endTrans(false);
        }
        
        // Run individual transactions to delete some of the children one by
        // one.
        for(int i = 0; i < 1000; ++i) {
            getRepository().beginTrans(true);
            try {
                RefObject obj =
                    (RefObject) (getRepository().getByMofId(result.get(i)));
                getRepository().delete(Collections.singletonList(obj));
            } finally {
                getRepository().endTrans(false);
            }
        }
    }

    @Test
    public void testDeleteNotLastWithinTxn()
    {
        String id1, id2;
        
        getRepository().beginTrans(true);
        try {
            Entity12 e12;
            e12 = getSimplePackage().getEntity12().createEntity12();
            id1 = e12.refMofId();
            e12 = getSimplePackage().getEntity12().createEntity12();
            id2 = e12.refMofId();
        } finally {
            getRepository().endTrans(false);
        }
        
        getRepository().beginTrans(true);
        try {
            RefObject obj =
                (RefObject) (getRepository().getByMofId(id1));
            getRepository().delete(Collections.singletonList(obj));
            obj =
                (RefObject) (getRepository().getByMofId(id2));
            Assert.assertNotNull(obj);
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    @Test
    public void testMassDeleteWithPendingChanges()
    {
        if (getMdrProvider() == MdrProvider.NETBEANS_MDR) {
            // Netbeans places no restrictions on the mass deletion API, since
            // it is simply converted to a series of single object deletions.
            return;
        }
        
        String e12aMofId;
        getRepository().beginTrans(true);
        try {
            Entity12 e12a = 
                getSimplePackage().getEntity12().createEntity12();
            e12aMofId = e12a.refMofId();
        } finally {
            getRepository().endTrans(false);
        }
            
        getRepository().beginTrans(true);
        try {
            RefObject e12a = (RefObject)getRepository().getByMofId(e12aMofId);
            
            Entity12 e12b = 
                getSimplePackage().getEntity12().createEntity12();
            Assert.assertNotNull(e12b);
            
            getRepository().delete(Collections.singletonList(e12a));
            
            // Expecting an exception since there are pending changes in the
            // txn.
            Assert.fail("missing expected exception");
        } catch(EnkiHibernateException e) {
            // Expected exception
        } finally {
            getRepository().endTrans(true);
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
