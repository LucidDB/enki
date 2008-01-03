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

import javax.swing.text.*;

import org.junit.*;

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
public class CircularAssociationTest
    extends SampleModelTestBase
{
    private static final String CONTAINER_NAME_PREFIX = "container";
    private static final String ENTITY_NAME_PREFIX = "entity";

    @Test
    public void testCircularAssociations()
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
            SampleContainerClass containerClass = 
                getSpecialPackage().getSampleContainer();
            SampleEntityClass entityClass =
                getSpecialPackage().getSampleEntity();
            
            SampleContainer top = 
                containerClass.createSampleContainer("top");
            scheduleForDelete(top);
            
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
                    scheduleForDelete(entities[i]);
                }
                
                container = nextContainer;
            } while(d > 0);
            
            return top.refMofId();
        }
        finally {
            getRepository().endTrans();
        }
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
}

// End CircularAssociationTest.java
