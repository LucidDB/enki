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

import javax.jmi.reflect.*;

import org.junit.*;
import org.junit.runner.*;

import eem.sample.special.*;

/**
 * EnumerationTest tests storage and retrieval of enumerations.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class EnumerationTest extends SampleModelTestBase
{
    @Test
    public void testEnumerations()
    {
        IceCreamFlavor flavors[] = {
            IceCreamFlavorEnum.CHOCOLATE,
            IceCreamFlavorEnum.VANILLA,
            IceCreamFlavorEnum.STRAWBERRY,
            IceCreamFlavorEnum.NEOPOLITAN,
        };
        
        String[] coneMofIds = createCones(flavors);

        checkCones(coneMofIds, flavors);
    }

    private void checkCones(String[] coneMofIds, IceCreamFlavor[] flavors)
    {
        Assert.assertEquals(coneMofIds.length, flavors.length);
        
        getRepository().beginTrans(false);
        
        for(int i = 0; i < coneMofIds.length; i++) {
            RefBaseObject refCone = getRepository().getByMofId(coneMofIds[i]);
            Assert.assertTrue(refCone instanceof RefObject);
            Assert.assertTrue(refCone instanceof IceCreamCone);
            
            IceCreamCone cone = (IceCreamCone)refCone;
            
            Assert.assertEquals(flavors[i], cone.getFlavor());
            Assert.assertEquals(i + 1, cone.getScoops());
            Assert.assertEquals((i & 1) == 1, cone.isMelting());
        }
        
        getRepository().endTrans();
    }

    private String[] createCones(IceCreamFlavor[] flavors)
    {
        String[] coneMofIds = new String[flavors.length];
        
        getRepository().beginTrans(true);
        
        IceCreamConeClass iceCreamConeClass = 
            getSpecialPackage().getIceCreamCone();

        for(int i = 0; i < flavors.length; i++) {
            IceCreamCone cone = iceCreamConeClass.createIceCreamCone();
            cone.setFlavor(flavors[i]);
            cone.setScoops(i + 1);
            cone.setMelting((i & 1) == 1);
            
            coneMofIds[i] = cone.refMofId();
        }
                
        getRepository().endTrans();
        
        return coneMofIds;
    }
}

// End EnumerationTest.java
