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
package org.eigenbase.enki.test.hibernate;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.test.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.special.*;

/**
 * HibernateStringLengthTest tests support for custom string attribute
 * lengths in the Hibernate MDR implementation.
 * 
 * @author Stephan Zuercher
 */
@RunWith(HibernateOnlyTestRunner.class)
public class HibernateStringLengthTest extends SampleModelTestBase
{
    private final String DATA = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*";
    
    @BeforeClass
    public static void endBaseClassSession()
    {
        getRepository().endSession();
    }
    
    @AfterClass
    public static void restartBaseClassSession()
    {
        getRepository().beginSession();
    }
    
    @Before
    public void beginSession()
    {
        getRepository().beginSession();
    }
    
    @After
    public void endSession()
    {
        getRepository().endSession();
    }
    
    @Test
    public void testDefaultLength()
    {
        // Assume default remains 128.
        final String[] safeValues = {
            makeString(32),
            makeString(127),
            makeString(128),
        };
        
        final String[] unsafeValues = {
            makeString(129),
            makeString(1024),
        };
        
        verifySafeValues(
            safeValues, 
            new CustomTagTargetCreator(),
            new NameCallback());
        verifyUnsafeValues(
            unsafeValues, 
            new CustomTagTargetCreator(),
            new NameCallback());
    }

    @Test
    public void testExtendedLength()
    {
        // Model says 192.
        final String[] safeValues = {
            makeString(32),
            makeString(128),
            makeString(191),
            makeString(192),
        };
        
        final String[] unsafeValues = {
            makeString(193),
            makeString(1024),
        };
        
        verifySafeValues(
            safeValues, 
            new CustomTagTargetCreator(),
            new LongStringCallback());
        verifyUnsafeValues(
            unsafeValues,
            new CustomTagTargetCreator(),
            new LongStringCallback());
    }
    
    @Test
    public void testUnlimitedLength()
    {
        // How much RAM do you have?
        final String[] safeValues = {
            makeString(32),
            makeString(128),
            makeString(192),
            makeString(256),
            makeString(32768),
        };
        
        verifySafeValues(
            safeValues, 
            new CustomTagTargetCreator(),
            new VeryLongStringCallback());
    }
    
    @Test
    public void testLengthFromClassLevelTag()
    {
        // Assume default remains 128.
        final String[] safeValues = {
            makeString(32),
            makeString(63),
            makeString(64),
        };
        
        final String[] unsafeValues = {
            makeString(65),
            makeString(128),
        };
        
        verifySafeValues(
            safeValues,
            new CustomClassifierTagTargetCreator(),
            new NameCallback2());
        verifyUnsafeValues(
            unsafeValues, 
            new CustomClassifierTagTargetCreator(),
            new NameCallback2());
    }
    
    @Test
    public void testLengthFromInheritedClassLevelTag()
    {
        final String[] safeValues = {
            makeString(32),
            makeString(63),
            makeString(64),
        };
        
        final String[] unsafeValues = {
            makeString(65),
            makeString(128),
        };
        
        verifySafeValues(
            safeValues,
            new CustomClassifierTagTargetSubCreator(),
            new NameCallback3());
        verifySafeValues(
            safeValues,
            new CustomClassifierTagTargetSubCreator(),
            new DescriptionCallback());
        verifyUnsafeValues(
            unsafeValues, 
            new CustomClassifierTagTargetSubCreator(),
            new NameCallback3());
        verifyUnsafeValues(
            unsafeValues, 
            new CustomClassifierTagTargetSubCreator(),
            new DescriptionCallback());
    }

    private <T extends RefObject> void verifySafeValues(
        String[] expectedValues,
        CreatorCallback<T> createCallback,
        AttribCallback<T> callback)
    {
        List<String> mofIds = 
            createValues(expectedValues, createCallback, callback);
        
        Assert.assertEquals(expectedValues.length, mofIds.size());
        
        getRepository().beginTrans(false);
        try {
            for(int i = 0; i < mofIds.size(); i++) {
                String mofId = mofIds.get(i);
                String expectedValue = expectedValues[i];
                
                T obj = 
                    createCallback.getCreatedType().cast(
                        getRepository().getByMofId(mofId));
                
                String gotValue = callback.get(obj);
                
                Assert.assertEquals(expectedValue, gotValue);
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    private <T extends RefObject> void verifyUnsafeValues(
        String[] unsafeValues,
        CreatorCallback<T> createCallback,
        AttribCallback<T> callback)
    {
        try {
            createValues(unsafeValues, createCallback, callback);
            
            Assert.fail("expected exception");
        }
        catch(Throwable t) {
            // expected
        }
    }
    
    private <E extends RefObject> List<String> createValues(
        String[] safeValues,
        CreatorCallback<E> createCallback,
        AttribCallback<E> callback)
    {
        List<String> mofIds = new ArrayList<String>();

        getRepository().beginTrans(true);
        try {
            for(String value: safeValues) {
                E obj = createCallback.create();
                callback.set(obj, value);
                mofIds.add(obj.refMofId());
            }
        }
        finally {
            getRepository().endTrans(false);
        }
        
        return mofIds;
    }
    
    private String makeString(int len)
    {
        StringBuilder builder = new StringBuilder();
        
        while(len >= DATA.length()) {
            builder.append(DATA);
            len -= DATA.length();
        }
        
        if (len > 0) {
            builder.append(DATA.substring(0, len));
        }
        
        return builder.toString();
    }
    
    private interface CreatorCallback<T>
    {
        public T create();
        public Class<T> getCreatedType();
    }
    
    private static class CustomTagTargetCreator
        implements CreatorCallback<CustomTagTarget>
    {
        public CustomTagTarget create()
        {
            return 
                getSpecialPackage()
                .getCustomTagTarget()
                .createCustomTagTarget();
        }
        
        public Class<CustomTagTarget> getCreatedType()
        {
            return CustomTagTarget.class;
        }
    }
    
    private static class CustomClassifierTagTargetCreator
        implements CreatorCallback<CustomClassifierTagTarget>
    {
        public CustomClassifierTagTarget create()
        {
            return 
                getSpecialPackage()
                .getCustomClassifierTagTarget()
                .createCustomClassifierTagTarget();
        }
        
        public Class<CustomClassifierTagTarget> getCreatedType()
        {
            return CustomClassifierTagTarget.class;
        }
    }
    
    private static class CustomClassifierTagTargetSubCreator
        implements CreatorCallback<CustomClassifierTagTargetSub>
    {
        public CustomClassifierTagTargetSub create()
        {
            return 
                getSpecialPackage()
                .getCustomClassifierTagTargetSub()
                .createCustomClassifierTagTargetSub();
        }
        
        public Class<CustomClassifierTagTargetSub> getCreatedType()
        {
            return CustomClassifierTagTargetSub.class;
        }
    }

    /**
     * AttribCallback is a simple interface for allowing a particular attribute
     * on a <code>CustomTagTarget</code> to be written or read.
     */
    private interface AttribCallback<T>
    {
        public void set(T obj, String value);
        public String get(T obj);
    }
    
    /**
     * NameCallback implements {@link AttribCallback} for the "name" attribute
     * of the {@link CustomTagTarget} type.
     */
    private static class NameCallback 
        implements AttribCallback<CustomTagTarget>
    {
        public void set(CustomTagTarget c, String value)
        {
            c.setName(value);
        }
        
        public String get(CustomTagTarget c)
        {
            return c.getName();
        }
    }

    /**
     * NameCallback implements {@link AttribCallback} for the "longString"
     * attribute of the {@link CustomTagTarget} type.
     */
    private static class LongStringCallback
        implements AttribCallback<CustomTagTarget>
    {
        public void set(CustomTagTarget c, String value)
        {
            c.setLongString(value);
        }
        
        public String get(CustomTagTarget c)
        {
            return c.getLongString();
        }
    }

    /**
     * NameCallback implements {@link AttribCallback} for the "veryLongString"
     * attribute of the {@link CustomTagTarget} type.
     */
    private static class VeryLongStringCallback 
        implements AttribCallback<CustomTagTarget>
    {
        public void set(CustomTagTarget c, String value)
        {
            c.setVeryLongString(value);
        }
        
        public String get(CustomTagTarget c)
        {
            return c.getVeryLongString();
        }
    }
    
    /**
     * NameCallback2 implements {@link AttribCallback} for the "name" attribute
     * of the {@link CustomClassifierTagTarget} type.
     */
    private static class NameCallback2
        implements AttribCallback<CustomClassifierTagTarget>
    {
        public void set(CustomClassifierTagTarget c, String value)
        {
            c.setName(value);
        }
        
        public String get(CustomClassifierTagTarget c)
        {
            return c.getName();
        }
    }

    /**
     * NameCallback3 implements {@link AttribCallback} for the "name" attribute
     * of the {@link CustomClassifierTagTargetSub} type.
     */
    private static class NameCallback3
        implements AttribCallback<CustomClassifierTagTargetSub>
    {
        public void set(CustomClassifierTagTargetSub c, String value)
        {
            c.setName(value);
        }
        
        public String get(CustomClassifierTagTargetSub c)
        {
            return c.getName();
        }
    }

    /**
     * DescriptionCallback implements {@link AttribCallback} for the 
     * "description" attribute of the {@link CustomClassifierTagTargetSub} 
     * type.
     */
    private static class DescriptionCallback
        implements AttribCallback<CustomClassifierTagTargetSub>
    {
        public void set(CustomClassifierTagTargetSub c, String value)
        {
            c.setDescription(value);
        }
        
        public String get(CustomClassifierTagTargetSub c)
        {
            return c.getDescription();
        }
    }
}

// End HibernateStringLengthTest.java
