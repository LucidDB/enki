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

import org.junit.*;
import org.junit.runner.*;

import eem.sample.special.*;
import eem.sample.pluginbase.*;

/**
 * AttributeTest tests attributes on MDR class instances.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class AttributeTest extends SampleModelTestBase
{
    @Test
    public void testUnusualAttributeNames()
    {
        String mofId;
        getRepository().beginTrans(true);
        try {
            SpecialPackage specialPkg = getSpecialPackage();
            
            StringManglerTest smt = 
                specialPkg.getStringManglerTest().createStringManglerTest();
            
            smt.setMyAcronym(1);
            smt.setMyEmbeddedacronym(2);
            smt.setMultiWordAttrib(3);
            smt.setTwoWord(4);
            
            mofId = smt.refMofId();
        }
        finally {
            getRepository().endTrans(false);
        }

        getRepository().beginTrans(false);
        try {
            StringManglerTest smt =  
                (StringManglerTest)getRepository().getByMofId(mofId);

            Assert.assertNotNull(smt);
            
            Assert.assertEquals(1, smt.getMyAcronym());
            Assert.assertEquals(2, smt.getMyEmbeddedacronym());
            Assert.assertEquals(3, smt.getMultiWordAttrib());
            Assert.assertEquals(4, smt.getTwoWord());
        }
        finally {
            getRepository().endTrans(false);
        }
    }

    // Disabled due to bug ENK-4
    public void _testCompositionAttributes()
    {
        getRepository().beginTrans(true);
        try {
            PluginBasePackage pkg = getSamplePackage().getPluginBase();

            // This works.
            ComposedElement composed =
                pkg.getComposedElement().createComposedElement();
            CompositeElement composite =
                pkg.getCompositeElement().createCompositeElement();
            composed.setComp(composite);
            
            // But this does not, because the attribute is
            // inherited from an abstract class.
            ConcreteComposedElement concrete =
                pkg.getConcreteComposedElement().createConcreteComposedElement();
            CompositeElement composite2 =
                pkg.getCompositeElement().createCompositeElement();
            concrete.setComp(composite2);
        }
        finally {
            getRepository().endTrans(false);
        }
    }
    
}

// End JmiTest.java
