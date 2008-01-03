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

import java.io.*;
import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.junit.*;
import org.netbeans.api.mdr.*;

/**
 * ModelTestBase is an abstract base class for model-based tests.
 * 
 * <p>Requires the following system properties to be set:
 * <ul>
 * <li>enki.home - Enki development directory to allow the test to find the
 *     "test" directory.</li>
 * <li>enki.test.extent - Name of the extent that contains the sample model's
 *     data.</li>
 * </ul>
 * 
 * @author Stephan Zuercher
 */
public abstract class ModelTestBase
{
    private static final String PROPERTY_ENKI_TEST_EXTENT = "enki.test.extent";
    private static final String PROPERTY_ENKI_HOME = "enki.home";
    private static MDRepository repos;
    private static RefPackage pkg;
    private static String testExtentName;
    
    private static List<String> mofIdsToDelete;
    private static Properties storageProps;
    
    @BeforeClass
    public static void setUpTestClass()
    {
        Assert.assertNull(repos);
        Assert.assertNull(pkg);
        
        getPackage();
        
        mofIdsToDelete = new ArrayList<String>();
    }
    
    @AfterClass
    public static void tearDownTestClass()
    {
        if (mofIdsToDelete != null && !mofIdsToDelete.isEmpty()) {
            repos.beginTrans(true);
            try {
                for(ListIterator<String> iter = 
                        mofIdsToDelete.listIterator(mofIdsToDelete.size());
                    iter.hasPrevious(); )
                {
                    RefObject refObject = 
                        (RefObject)repos.getByMofId(iter.previous());
                    
                    if (refObject != null) {
                        refObject.refDelete();            
                    }
                }
            }
            finally {
                repos.endTrans();
            }
        }
        
        MDRepository repository = repos;
        if (repository != null) {
            pkg = null;
            repos = null;
            repository.shutdown();
        }        
    }

    protected <E> E findEntity(String refMofId, Class<E> cls)
    {
        RefBaseObject refBaseObject = repos.getByMofId(refMofId);
        try {
            return cls.cast(refBaseObject);
        }
        catch(ClassCastException e) {
            fail(
                "RebBaseObject (" + refMofId + ") is not a " + cls.getName(),
                e);
            return null; // unreachable
        }
    }
    
    protected static void scheduleForDelete(RefObject refObject)
    {
        if (mofIdsToDelete.contains(refObject.refMofId())) {
            return;
        }
        
        mofIdsToDelete.add(refObject.refMofId());
    }
    
    protected static RefPackage getPackage()
    {
        if (pkg == null) {
            load();
        }

        return pkg;
    }
    
    protected static MDRepository getRepository()
    {
        if (repos == null) {
            load();
        }
        
        return repos;
    }

    protected static MdrProvider getMdrProvider()
    {
        if (repos == null) {
            load();
        }
        
        String enkiImplType = 
            storageProps.getProperty(MDRepositoryFactory.ENKI_IMPL_TYPE);
        MdrProvider implType = MdrProvider.valueOf(enkiImplType);
        return implType;

    }
    
    protected static String getTestExtentName()
    {
        return testExtentName;
    }
    
    private static void load()
    {
        String enkiHome = System.getProperty(PROPERTY_ENKI_HOME);
        Assert.assertNotNull(enkiHome);
        Assert.assertTrue(enkiHome.length() > 0);
        
        testExtentName = System.getProperty(PROPERTY_ENKI_TEST_EXTENT);
        Assert.assertNotNull(testExtentName);
        Assert.assertTrue(testExtentName.length() > 0);

        File storagePropsFile = 
            new File(enkiHome, "test/TestStorage.properties");
        
        storageProps = new Properties();
        try {
            storageProps.load(new FileInputStream(storagePropsFile));
        } catch (IOException e) {
            fail(e);
        }
        
        repos = MDRepositoryFactory.newMDRepository(storageProps);
        Assert.assertNotNull(repos);
        
        pkg = repos.getExtent(testExtentName);
        Assert.assertNotNull(pkg);        
    }
    
    protected static void fail(Throwable t)
    {
        fail(null, t);
    }
    
    protected static void fail(String message, Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (message != null) {
            pw.println(message);
        }
        t.printStackTrace(pw);
        
        Assert.fail(sw.toString());
    }
}

// End ModelTestBase.java
