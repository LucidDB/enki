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
    @Ignore
    @Test
    public void testCircularAssociations()
    {
        getRepository().beginTrans(true);
        
        SampleContainerClass containerClass = 
            getSpecialPackage().getSampleContainer();
        SampleEntityClass entityClass =
            getSpecialPackage().getSampleEntity();
        
        SampleContainer top = 
            containerClass.createSampleContainer("top");
        SampleEntity topEntity1 =
            entityClass.createSampleEntity("topEntity1");
        scheduleForDelete(topEntity1);
        SampleContainer middle = 
            containerClass.createSampleContainer("middle");
        SampleEntity topEntity2 =
            entityClass.createSampleEntity("topEntity2");
        top.getContainedEntity().add(topEntity1);
        top.getContainedEntity().add(middle);
        top.getContainedEntity().add(topEntity2);
        
        SampleEntity middleEntity1 = 
            entityClass.createSampleEntity("middleEntity1");
        SampleEntity middleEntity2 = 
            entityClass.createSampleEntity("middleEntity2");
        middle.getContainedEntity().add(middleEntity1);
        middle.getContainedEntity().add(middleEntity2);

        scheduleForDelete(top);
        scheduleForDelete(topEntity1);
        scheduleForDelete(topEntity2);
        scheduleForDelete(middle);
        scheduleForDelete(middleEntity1);
        scheduleForDelete(middleEntity2);

        getRepository().endTrans();
    }
}

// End CircularAssociationTest.java
