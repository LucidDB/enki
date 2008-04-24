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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.junit.*;
import org.junit.runner.*;

import eem.sample.*;
import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * ConstraintsTest tests verification of MOF constraints via JMI.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class ConstraintsTest extends SampleModelTestBase
{
    @Test
    public void testBasicAttributeConstraint()
    {
        getRepository().beginTrans(true);
        try {
            Car c = getSamplePackage().getCar().createCar();
            
            c.setMake("Rolls Royce");
            
            boolean foundModel = false;
            boolean foundDoors = false;
            
            Collection<?> errors = c.refVerifyConstraints(false);
            Assert.assertEquals(2, errors.size());
            for(Object errorObj: errors) {
                Assert.assertTrue(
                    errorObj instanceof WrongSizeException);
            
                WrongSizeException wse = (WrongSizeException)errorObj;
            
                Assert.assertTrue(wse.getElementInError() instanceof Attribute);
            
                Attribute errorAttrib = (Attribute)wse.getElementInError();
            
                String errorAttribName = errorAttrib.getName();
                if (errorAttribName.equals("doors")) {
                    Assert.assertFalse(foundDoors);
                    foundDoors = true;
                } else if (errorAttribName.equals("model")) {
                    Assert.assertFalse(foundModel);
                    foundModel = true;
                } else {
                    Assert.fail("unexpected attribute: " + errorAttribName);
                }
            }
        }
        finally {
            getRepository().endTrans(true);
        }
    }

    @Test
    public void testEnumAttributeConstraint()
    {
        getRepository().beginTrans(true);
        try {
            IceCreamCone cone = 
                getSpecialPackage().getIceCreamCone().createIceCreamCone();
            
            cone.setMelting(false);
            cone.setScoops(1);
            
            Collection<?> errors = cone.refVerifyConstraints(false);
            Assert.assertEquals(1, errors.size());
            Object errorObj = errors.iterator().next();
            Assert.assertTrue(
                errorObj instanceof WrongSizeException);
            
            WrongSizeException wse = (WrongSizeException)errorObj;
            
            Assert.assertTrue(wse.getElementInError() instanceof Attribute);
            
            Attribute errorAttrib = (Attribute)wse.getElementInError();
            
            Assert.assertEquals("flavor", errorAttrib.getName());
        }
        finally {
            getRepository().endTrans(true);
        }
    }
    
    @Test
    public void testComponentAttributeConstraint()
    {
        getRepository().beginTrans(true);
        try {
            PhoneNumber num = 
                getSpecialPackage().getPhoneNumber().createPhoneNumber();
            num.setNumber("867-5309");
            
            Collection<?> errors = num.refVerifyConstraints(false);
            Assert.assertEquals(1, errors.size());
            Object errorObj = errors.iterator().next();
            Assert.assertTrue(
                errorObj instanceof WrongSizeException);
            
            WrongSizeException wse = (WrongSizeException)errorObj;
            
            Assert.assertTrue(wse.getElementInError() instanceof Attribute);
            
            Attribute errorAttrib = (Attribute)wse.getElementInError();
            
            Assert.assertEquals("areaCode", errorAttrib.getName());
        }
        finally {
            getRepository().endTrans(true);
        }
    }
    
    @Test
    public void testReferenceConstraint()
    {
        getRepository().beginTrans(true);
        try {
            Entity10 e10 = getSimplePackage().getEntity10().createEntity10();
            
            Collection<?> errors = e10.refVerifyConstraints(false);
            Assert.assertEquals(1, errors.size());
            Object errorObj = errors.iterator().next();
            Assert.assertTrue(
                errorObj instanceof WrongSizeException);
            
            WrongSizeException wse = (WrongSizeException)errorObj;
            
            Assert.assertTrue(
                wse.getElementInError() instanceof AssociationEnd);
            
            AssociationEnd errorEnd = (AssociationEnd)wse.getElementInError();
            
            Assert.assertEquals("Entity11", errorEnd.getName());
        }
        finally {
            getRepository().endTrans(true);
        }
    }
}

// End ConstraintsTest.java
