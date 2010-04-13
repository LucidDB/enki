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

import org.junit.*;
import org.junit.runner.*;

import eem.sample.special.*;

/**
 * EntityAttributeTest tests model entities used directly as attributes of
 * another entity.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class EntityAttributeTest
    extends SampleModelTestBase
{
    @Test
    public void testEntityAttribute()
    {
        String phoneNumberMofId = 
            createPhoneNumber("415", true, "555-1212");
        
        checkPhoneNumber(phoneNumberMofId, "415", true, "555-1212");
    }
    
    private String createPhoneNumber(
        String areaCodeStr, boolean isDomestic, String phoneNumberStr)
    {
        getRepository().beginTrans(true);
        
        try {
            AreaCodeClass areaCodeClass = getSpecialPackage().getAreaCode();
            PhoneNumberClass phoneNumberClass = 
                getSpecialPackage().getPhoneNumber();
            
            AreaCode areaCode = 
                areaCodeClass.createAreaCode(areaCodeStr, true);
            
            PhoneNumber phoneNumber = 
                phoneNumberClass.createPhoneNumber(areaCode, phoneNumberStr);
            
            return phoneNumber.refMofId();
        } finally {
            getRepository().endTrans();
        }
    }
    
    private void checkPhoneNumber(
        String phoneNumberMofId, 
        String areaCodeStr, 
        boolean isDomestic, 
        String phoneNumberStr)
    {
        getRepository().beginTrans(false);
        
        try {
            PhoneNumber phoneNumber = 
                (PhoneNumber)getRepository().getByMofId(phoneNumberMofId);
            
            Assert.assertEquals(phoneNumberStr, phoneNumber.getNumber());
            
            AreaCode areaCode = phoneNumber.getAreaCode();
            
            Assert.assertEquals(areaCodeStr, areaCode.getCode());
            Assert.assertEquals(isDomestic, areaCode.isDomestic());
        } finally {
            getRepository().endTrans();
        }
    }
}

// End EntityAttributeTest.java
