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

import org.junit.*;
import org.junit.runner.*;

import eem.sample.special.*;

/**
 * CircularAssociationTest tests circular associations.  For example, a
 * common pattern is the "Contains" association, where a particular type
 * (a "container" or "namespace") holds objects of an abstract base type
 * (an "entity" or "element").  In addition, the container is itself is
 * usually an abstract subclass of the base type.
 *    
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class CircularAssociationTest
    extends SampleModelTestBase
{
    private static final String CONTAINER_NAME_PREFIX = "container";
    private static final String ENTITY_NAME_PREFIX = "entity";

    private static final String DEPENDENCY_NAME_PREFIX = "dep";
    private static final String SUPPLIER_NAME_PREFIX = "supplier";
    private static final String CLIENT_NAME_PREFIX = "client";    
    
    @Test
    public void testCircularOneToManyAssociations()
    {
        String topContainerMofId = createContainmentHierarchy(3, 3);
        
        validateContainmentHierarchy(topContainerMofId, 3, 3);
    }
    
    private String createContainmentHierarchy(final int depth, final int width)
    {
        Assert.assertTrue(depth > 0);
        Assert.assertTrue(width > 0);
        
        getRepository().beginTrans(true);
        
        try {
            return createContainmentHierarchySansTxn(depth, width);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    static String createContainmentHierarchySansTxn(
        final int depth, final int width)
    {
        SampleContainerClass containerClass = 
            getSpecialPackage().getSampleContainer();
        SampleEntityClass entityClass =
            getSpecialPackage().getSampleEntity();
        
        SampleContainer top = 
            containerClass.createSampleContainer("top");
        
        SampleContainer container = top;
        int d = depth;
        do {
            d--;
            Entity[] entities = new Entity[width];
        
            int containerIndex = -1;
            if (d > 0) {
                containerIndex = d % width;
            }
            
            SampleContainer nextContainer = null;
            for(int i = 0; i < width; i++) {
                String prefix = ENTITY_NAME_PREFIX;
                if (i == containerIndex) {
                    nextContainer = containerClass.createSampleContainer();
                    entities[i] = nextContainer;
                    prefix = CONTAINER_NAME_PREFIX;
                } else {
                    entities[i] = entityClass.createSampleEntity();
                }

                entities[i].setName(
                    prefix + ": " + (depth - d) + ": " + i);
                
                container.getContainedEntity().add(entities[i]);
            }
            
            container = nextContainer;
        } while(d > 0);
        
        return top.refMofId();

    }
    
    private void validateContainmentHierarchy(
        String topMofId, final int depth, final int width)
    {
        getRepository().beginTrans(false);
        
        try {
            SampleContainer top = 
                (SampleContainer)getRepository().getByMofId(topMofId);
            
            SampleContainer container = top;
            int d = depth;
            do {
                d--;
                Collection<Entity> entities = container.getContainedEntity();
                
                Set<Integer> expectedContainerPositions = 
                    Collections.emptySet();
                int containerIndex = -1;
                if (d > 0) {
                    containerIndex = d % width;
                    expectedContainerPositions = 
                        new HashSet<Integer>(
                            Collections.singleton(containerIndex));
                }
                
                Set<Integer> expectedEntityPositions = new HashSet<Integer>();
                for(int i = 0; i < width; i++) {
                    if (i != containerIndex) {
                        expectedEntityPositions.add(i);
                    }
                }
                
                Assert.assertEquals(width, entities.size());
                
                SampleContainer nextContainer = null;
                for(Entity entity: entities) {
                    boolean isContainer = false;
                    String expectedPrefix =  ENTITY_NAME_PREFIX;
                    if (entity instanceof SampleContainer) {
                        nextContainer = (SampleContainer)entity;
                        expectedPrefix =  CONTAINER_NAME_PREFIX;
                        isContainer = true;
                    } else {
                        Assert.assertTrue(entity instanceof SampleEntity);
                    }
                    
                    expectedPrefix += ": " + (depth - d) + ": ";
                    Assert.assertTrue(
                        entity.getName().startsWith(expectedPrefix));
                    int position = 
                        Integer.parseInt(
                            entity.getName().substring(
                                expectedPrefix.length()));
                    if (isContainer) {
                        Assert.assertTrue(
                            expectedContainerPositions.contains(position));
                        expectedContainerPositions.remove(position);
                    } else {
                        Assert.assertTrue(
                            expectedEntityPositions.contains(position));
                        expectedEntityPositions.remove(position);
                    }
                }
                
                Assert.assertTrue(expectedEntityPositions.isEmpty());
                Assert.assertTrue(expectedContainerPositions.isEmpty());
                
                container = nextContainer;
            } while(d > 0);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testCircularManyToManyAssociations()
    {
        List<String> supMofIds = new ArrayList<String>();
        List<String> clientMofIds = new ArrayList<String>();
        List<String> depMofIds = createDependencies(supMofIds, clientMofIds);
        
        validateDependencies(depMofIds, false);
        validateDependencies(depMofIds, true);
        
        validateDependenciesInReverse(supMofIds, clientMofIds);
    }
    
    private List<String> createDependencies(
        List<String> supMofIds, List<String> clientMofIds)
    {
        getRepository().beginTrans(true);
        
        try {
            return createDependenciesSansTxn(supMofIds, clientMofIds);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    private List<String> createDependenciesSansTxn(
        List<String> supMofIds, List<String> clientMofIds)
    {
        SampleElementClass sampleElemCls = 
            getSpecialPackage().getSampleElement();
        
        SampleElement sup1 =
            sampleElemCls.createSampleElement(SUPPLIER_NAME_PREFIX + "1");
        supMofIds.add(sup1.refMofId());

        SampleElement sup2 =
            sampleElemCls.createSampleElement(SUPPLIER_NAME_PREFIX + "2");
        supMofIds.add(sup2.refMofId());
        
        SampleElement client1 = 
            sampleElemCls.createSampleElement(CLIENT_NAME_PREFIX + "1");
        clientMofIds.add(client1.refMofId());

        SampleElement client2 = 
            sampleElemCls.createSampleElement(CLIENT_NAME_PREFIX + "2");
        clientMofIds.add(client2.refMofId());

        SampleElement client3 = 
            sampleElemCls.createSampleElement(CLIENT_NAME_PREFIX + "3");
        clientMofIds.add(client3.refMofId());

        DependencyClass depCls = getSpecialPackage().getDependency();
        
        Dependency dep1 = 
            depCls.createDependency(DEPENDENCY_NAME_PREFIX + "1");
        dep1.getSupplier().add(sup1);
        dep1.getClient().add(client1);
        dep1.getClient().add(client2);
        
        Dependency dep2 = 
            depCls.createDependency(DEPENDENCY_NAME_PREFIX + "2");
        dep2.getSupplier().add(sup2);
        dep2.getClient().add(client2);
        dep2.getClient().add(client3);
        
        return Arrays.asList(
            new String[] { dep1.refMofId(), dep2.refMofId() });
    }
    
    private void validateDependencies(List<String> depMofIds, boolean useProxy)
    {
        getRepository().beginTrans(false);

        DependencySupplier depSupAssoc = 
            getSpecialPackage().getDependencySupplier();
        DependencyClient depClientAssoc =
            getSpecialPackage().getDependencyClient();
        
        try {
            for(int i = 0; i < depMofIds.size(); i++) {
                String depMofId = depMofIds.get(i);
                
                Dependency dep = 
                    (Dependency)getRepository().getByMofId(depMofId);
                
                Collection<Element> suppliers;
                if (useProxy) {
                    suppliers = depSupAssoc.getSupplier(dep);
                } else {
                    suppliers = dep.getSupplier();
                }
                
                Assert.assertEquals(1, suppliers.size());
                
                Element supplier = suppliers.iterator().next();
                
                Assert.assertEquals(
                    SUPPLIER_NAME_PREFIX + (i + 1), supplier.getName());
                
                Collection<Element> clients;
                if (useProxy) {
                    clients = depClientAssoc.getClient(dep);
                } else {
                    clients = dep.getClient();
                }
                
                Assert.assertEquals(2, clients.size());

                Set<String> expectedNames = new HashSet<String>();
                expectedNames.add(CLIENT_NAME_PREFIX + (i + 1));
                expectedNames.add(CLIENT_NAME_PREFIX + (i + 2));

                Set<String> gotNames = new HashSet<String>();
                for(Element client: clients) {
                    gotNames.add(client.getName());
                }
                
                Assert.assertEquals(expectedNames, gotNames);   
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    private void validateDependenciesInReverse(
        List<String> supMofIds, List<String> clientMofIds)
    {
        getRepository().beginTrans(false);

        DependencySupplier depSupAssoc = 
            getSpecialPackage().getDependencySupplier();
        DependencyClient depClientAssoc =
            getSpecialPackage().getDependencyClient();
        
        try {
            for(String supMofId: supMofIds) {
                Element supplier = 
                    (Element)getRepository().getByMofId(supMofId);
                Assert.assertNotNull(supplier);
                
                Collection<Dependency> deps = 
                    depSupAssoc.getSupplierDependency(supplier);
                Assert.assertEquals(1, deps.size());
                
                Dependency dep = deps.iterator().next();
                
                String expectedName = 
                    DEPENDENCY_NAME_PREFIX + 
                    supplier.getName().replaceAll("[A-Za-z]+", "");
                
                Assert.assertEquals(expectedName, dep.getName());
            }

            for(String clientMofId: clientMofIds) {
                Element client = 
                    (Element)getRepository().getByMofId(clientMofId);
                Assert.assertNotNull(client);

                Collection<Dependency> deps = 
                    depClientAssoc.getClientDependency(client);
                
                int clientNameSuffix =
                    Integer.parseInt(
                        client.getName().replaceAll("[A-Za-z]+", ""));
                
                int expectedNumDeps = 1;
                if (clientNameSuffix >= 2 && 
                    clientNameSuffix < clientMofIds.size())
                {
                    expectedNumDeps = 2;
                }
                
                Assert.assertEquals(expectedNumDeps, deps.size());
                
                Set<String> expectedDepNames = new HashSet<String>();
                if (clientNameSuffix < clientMofIds.size()) {
                    expectedDepNames.add(
                        DEPENDENCY_NAME_PREFIX + clientNameSuffix);
                }
                if (expectedNumDeps > 1 || 
                    clientNameSuffix == clientMofIds.size())
                {
                    expectedDepNames.add(
                        DEPENDENCY_NAME_PREFIX + (clientNameSuffix - 1));                    
                }
                
                Set<String> gotDepNames = new HashSet<String>();
                for(Dependency dep: deps) {
                    gotDepNames.add(dep.getName());
                }
                
                Assert.assertEquals(expectedDepNames, gotDepNames);
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
}

// End CircularAssociationTest.java
