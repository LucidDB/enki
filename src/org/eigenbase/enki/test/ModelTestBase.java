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
    
    @BeforeClass
    public static void setUpTestClass()
    {
        Assert.assertNull(repos);
        Assert.assertNull(pkg);
        
        getPackage();
    }
    
    @AfterClass
    public static void tearDownTestClass()
    {
        MDRepository repository = repos;
        if (repository != null) {
            pkg = null;
            repos = null;
            repository.shutdown();
        }        
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

    private static void load()
    {
        String enkiHome = System.getProperty(PROPERTY_ENKI_HOME);
        Assert.assertNotNull(enkiHome);
        Assert.assertTrue(enkiHome.length() > 0);
        
        String extentName = System.getProperty(PROPERTY_ENKI_TEST_EXTENT);
        Assert.assertNotNull(extentName);
        Assert.assertTrue(extentName.length() > 0);

        File storagePropsFile = 
            new File(enkiHome, "test/TestStorage.properties");
        
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(storagePropsFile));
        } catch (IOException e) {
            fail(e);
        }
        
        repos = MDRepositoryFactory.newMDRepository(props);
        
        pkg = repos.getExtent(extentName);
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
