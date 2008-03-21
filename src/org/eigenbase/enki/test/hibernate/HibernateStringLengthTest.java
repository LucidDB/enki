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
        
        CustomTagTargetCreator creatorCallback = new CustomTagTargetCreator();
        AttribCallback<CustomTagTarget> nameCallback =
            new AttribCallback<CustomTagTarget>("name");
        
        verifySafeValues(safeValues, creatorCallback, nameCallback);
        verifyUnsafeValues(unsafeValues, creatorCallback, nameCallback);
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
        
        CustomTagTargetCreator creatorCallback = new CustomTagTargetCreator();
        AttribCallback<CustomTagTarget> string192Callback = 
            new AttribCallback<CustomTagTarget>("string192");
        
        verifySafeValues(safeValues, creatorCallback, string192Callback);
        verifyUnsafeValues(unsafeValues, creatorCallback, string192Callback);
    }
    
    @Test
    public void testExtendedLength2()
    {
        // Model says 64k - 1 characters
        final String[] safeValues = {
            makeString(32),
            makeString(128),
            makeString(192),
            makeString(65535),
        };
        
        final String[] unsafeValues = {
            makeString(65536),
        };
        
        CustomTagTargetCreator creatorCallback = new CustomTagTargetCreator();
        AttribCallback<CustomTagTarget> string64kCallback = 
            new AttribCallback<CustomTagTarget>("string64k");
        
        verifySafeValues(safeValues, creatorCallback, string64kCallback);
        verifyUnsafeValues(unsafeValues, creatorCallback, string64kCallback);
    }
    
    @Test
    public void testExtendedLength3()
    {
        try {
            // Model says 2^24 - 1 characters
            final String[] safeValues = {
                makeString(32),
                makeString(128),
                makeString(192),
                makeString((1 << 24) - 1),
            };
            
            CustomTagTargetCreator creatorCallback = 
                new CustomTagTargetCreator();
            AttribCallback<CustomTagTarget> string16mCallback = 
                new AttribCallback<CustomTagTarget>("string16m");
            
            verifySafeValues(safeValues, creatorCallback, string16mCallback);
        }
        catch(OutOfMemoryError e) {
            System.gc();
            
            System.err.println(
                "WARNING: skipped " + getClass().getName() + 
                ".testExtendedLength3() due to memory constraints");
        }
    }
    
    @Test
    public void testExtendedLength3Negative()
    {
        try {
            // Model says 2^24 - 1 characters
            final String[] unsafeValues = {
                makeString(1 << 24),
            };
            
            CustomTagTargetCreator creatorCallback = 
                new CustomTagTargetCreator();
            AttribCallback<CustomTagTarget> string16mCallback = 
                new AttribCallback<CustomTagTarget>("string64k");
            
            verifyUnsafeValues(
                unsafeValues, creatorCallback, string16mCallback);
        }
        catch(OutOfMemoryError e) {
            System.gc();
            
            System.err.println(
                "WARNING: skipped " + getClass().getName() + 
                ".testExtendedLength3Negative() due to memory constraints");
        }
    }
    
    @Test
    public void testUnlimitedLength()
    {
        try {
            final String[] safeValues = {
                makeString(32),
                makeString(128),
                makeString(192),
                makeString(1 << 24),
            };
            
            verifySafeValues(
                safeValues, 
                new CustomTagTargetCreator(),
                new AttribCallback<CustomTagTarget>("stringUnlimited"));
        }
        catch(OutOfMemoryError e) {
            System.gc();
            
            System.err.println(
                "WARNING: skipped " + getClass().getName() + 
                ".testUnlimitedLength() due to memory constraints");
        }
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
            new AttribCallback<CustomClassifierTagTarget>("name"));
        verifyUnsafeValues(
            unsafeValues, 
            new CustomClassifierTagTargetCreator(),
            new AttribCallback<CustomClassifierTagTarget>("name"));
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
            new AttribCallback<CustomClassifierTagTargetSub>("name"));
        verifySafeValues(
            safeValues,
            new CustomClassifierTagTargetSubCreator(),
            new AttribCallback<CustomClassifierTagTargetSub>("description"));
        verifyUnsafeValues(
            unsafeValues, 
            new CustomClassifierTagTargetSubCreator(),
            new AttribCallback<CustomClassifierTagTargetSub>("name"));
        verifyUnsafeValues(
            unsafeValues, 
            new CustomClassifierTagTargetSubCreator(),
            new AttribCallback<CustomClassifierTagTargetSub>("description"));
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
     * AttribCallback is a simple interface for allowing a particular string
     * attribute on a {@link RefObject} to be written or read.
     */
    private static class AttribCallback<T extends RefObject>
    {
        private final String attributeName;
        
        public AttribCallback(String attributeName)
        {
            this.attributeName = attributeName;
        }
        
        public void set(T obj, String value)
        {
            obj.refSetValue(attributeName, value);
        }
        
        public String get(T obj)
        {
            return (String)obj.refGetValue(attributeName);
        }
    }    
}

// End HibernateStringLengthTest.java
