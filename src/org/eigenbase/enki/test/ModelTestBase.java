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
import java.util.logging.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.test.hibernate.*;
import org.eigenbase.enki.util.*;
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
    // Model extent name
    private static final String PROPERTY_ENKI_TEST_EXTENT = "enki.test.extent";
    
    // Enki home directory (relative path for storage props file)
    private static final String PROPERTY_ENKI_HOME = "enki.home";
    
    // Location of storage props file (may be absolute or relative to 
    // value of PROPERTY_ENKI_HOME.
    private static final String PROPERTY_ENKI_TEST_STORAGE_PROPS = 
        "enki.test.storageProps";
    
    // Default value for PROPERTY_ENKI_TEST_STORAGE_PROPS
    private static final String TEST_STORAGE_PROPERTIES_PATH = 
        "test/TestStorage.properties";
    
    private static final Logger log = 
        Logger.getLogger("org.eigenbase.enki.test");
    
    private static EnkiMDRepository repos;
    private static RefPackage pkg;
    private static String testExtentName;
    private static Properties storageProps;
    
    /**
     * Sets up this class for testing against the configured repository.
     * There must be no loaded repository. It loads the repository via
     * {@link #getPackage()}, starts a session and write transaction,
     * deletes all existing objects via {@link #delete(RefPackage)}, and 
     * commits the transaction, leaving the session open.
     */
    @BeforeClass
    public static void setUpTestClass()
    {
        Assert.assertNull(repos);
        Assert.assertNull(pkg);
        
        getPackage();
        
        repos.beginSession();
        repos.beginTrans(true);
        boolean rollback = true;
        try {
            delete(pkg);
            rollback = false;
        }
        finally {
            repos.endTrans(rollback);
        }
    }
    
    /**
     * Closes the current session and deletes all objects from the repository.
     * The same steps are taken before each test class.  Assumes all 
     * transactions on the repository are finished before it is called.
     * Leaves an open session on the repository.
     */
    public static void reset()
    {
       repos.endSession();
       
       repos.beginSession();
       repos.beginTrans(true);
       boolean rollback = true;
       try {
           delete(pkg);
           rollback = false;
       }
       finally {
           repos.endTrans(rollback);
       }
    }
    
    /**
     * Reloads the configured test extent from the repository by invoking
     * {@link MDRepository#getExtent(String)}.
     */
    protected void resetMetaModel()
    {
        pkg = getRepository().getExtent(getTestExtentName());
    }
    
    /**
     * Iterates over all packages and classes in the given RefPackage and
     * deletes each object, taking to avoid errors with composite associations.
     * 
     * @param refPkg package from which to delete objects
     */
    private static void delete(RefPackage refPkg)
    {
        for(RefClass cls: 
                GenericCollections.asTypedCollection(
                    refPkg.refAllClasses(),
                    RefClass.class))
        {
            Collection<RefObject> allOfClass = 
                new ArrayList<RefObject>(
                    GenericCollections.asTypedCollection(
                        cls.refAllOfClass(),
                        RefObject.class));

            Iterator<RefObject> iter = allOfClass.iterator();
            while(iter.hasNext()) {
                RefFeatured owner = iter.next().refImmediateComposite();
                if (owner != null && owner instanceof RefObject)
                {
                    // For Netbeans: If we delete this object before we happen
                    // to have deleted its composite owner, the owner will
                    // throw an exception (because this object was already
                    // deleted). Can happen depending on iteration order of
                    // pkg.refAllClasses(). So remove this object from the
                    // collection and let the owner delete it.
                    iter.remove();
                }
            }

            for(RefObject obj: allOfClass) {
                obj.refDelete();
            }
        }
        
        for(RefPackage p: 
                GenericCollections.asTypedCollection(
                    refPkg.refAllPackages(),
                    RefPackage.class))
        {
            delete(p);
        }
    }
    
    /**
     * Shuts down repository started in {@link #setUpTestClass()}, if any.
     * Clears the statis fields used to hold references to the extent and
     * repository.
     */
    @AfterClass
    public static void tearDownTestClass()
    {
        EnkiMDRepository repository = repos;
        if (repository != null) {
            repository.endSession();
            pkg = null;
            repos = null;
            repository.shutdown();
        }        
    }

    /**
     * Shuts down and then restarts the repository.  Performs the same 
     * operations as {@link #tearDownTestClass()} followed by
     * {@link #setUpTestClass()} <b>except</b> that no objects are deleted
     * from the repository. 
     */
    protected static void bounceRepository()
    {
        tearDownTestClass();
        
        Assert.assertNull(repos);
        Assert.assertNull(pkg);
        
        getPackage();
        
        repos.beginSession();
    }
    
    
    /**
     * Loads the object corresponding to the given MOF ID and casts it to the
     * given class.  If the cast fails, throws an AssertionError.
     * 
     * @param <E> expected type of the object
     * @param refMofId the object's MOF ID
     * @param cls expected type of the object
     * @return the object corresponding to refMofId, or null if it doesn't
     *         exist
     * @throws AssertionError if the object is not of type E
     */
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
    
    /**
     * Retrieves the test logger.
     * 
     * @return the test logger
     */
    public static Logger getTestLogger()
    {
        return log;
    }
    
    /**
     * Retrieves the configured test extent.  The extent is cached.  Call
     * {@link #resetMetaModel()} to refresh the object.  If not yet loaded,
     * the repository is loaded via {@link #load()}.
     * 
     * @return the configured test extent.
     */
    protected static RefPackage getPackage()
    {
        if (pkg == null) {
            load();
        }

        return pkg;
    }
    
    /**
     * Retrieves the configured test repository.  The repository object is
     * cached. If not yet loaded, the repository is loaded via {@link #load()}.  
     * 
     * @return the configured test repository
     */
    protected static EnkiMDRepository getRepository()
    {
        if (repos == null) {
            load();
        }
        
        return repos;
    }

    /**
     * Retrieves the configured test repository's provider type.  This allows
     * tests to modify their expected results for differences between 
     * implementations.  See {@link HibernateOnlyTestRunner} for a simple
     * way to control test execution per-class.  Modify tests on a
     * per-implementation basis sparingly!
     * 
     * @return the configured test repository's provider type.
     */
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
    
    /**
     * Returns the test extent's name.
     * 
     * @return the test extent's name
     */
    protected static String getTestExtentName()
    {
        return testExtentName;
    }
    
    /**
     * Returns the value of the system property containing the path to the 
     * storage properties file used to configure the test repository. The
     * value may be relative to {@link #PROPERTY_ENKI_HOME}. 
     * 
     * @see #makeStoragePropertiesPath(String)
     * @return path to the storage properties file
     */
    public static String getStoragePropertiesPath()
    {
        return System.getProperty(
            PROPERTY_ENKI_TEST_STORAGE_PROPS, TEST_STORAGE_PROPERTIES_PATH);
    }
    
    /**
     * Converts the value of the given, possibly relative, storage properties 
     * path into an absolute path.
     * 
     * @param path the storage properties path 
     * @return absolute path to the given storage properties file
     */
    public static File makeStoragePropertiesPath(String path)
    {
        String enkiHome = System.getProperty(PROPERTY_ENKI_HOME);
        Assert.assertNotNull(enkiHome);
        Assert.assertTrue(enkiHome.length() > 0);

        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }
        
        return new File(enkiHome, path);
    }
    
    /**
     * Loads the test repository and extent described by the system properties.
     */
    private static void load()
    {
        testExtentName = System.getProperty(PROPERTY_ENKI_TEST_EXTENT);
        Assert.assertNotNull(testExtentName);
        Assert.assertTrue(testExtentName.length() > 0);

        RepositoryDetails result = 
            loadRepository(testExtentName, getStoragePropertiesPath());
        
        storageProps = result.storageProps;
        repos = result.repos;
        pkg = result.pkg;
    }
    
    /**
     * Loads the test repository and extent described by the given extent name
     * and storage properties path.
     * 
     * @param extentName name of an existing extent in the repository
     * @param storagePropsPath path to the storage properties file, must be
     *                         absolute or relative to the value of the
     *                         {@link #PROPERTY_ENKI_HOME} system property
     *                         
     * @return a {@link RepositoryDetails} object describing the loaded 
     *         repository
     * @throws AssertionError if the storage properties cannot be loaded
     * @see MDRepositoryFactory#newMDRepository(Properties)
     */
    protected static RepositoryDetails loadRepository(
        String extentName, String storagePropsPath)
    {
        RepositoryDetails result = new RepositoryDetails();

        File storagePropsFile = makeStoragePropertiesPath(storagePropsPath);
        
        result.storageProps = new Properties();
        try {
            result.storageProps.load(new FileInputStream(storagePropsFile));
        } catch (IOException e) {
            fail(e);
        }
        
        result.repos = 
            MDRepositoryFactory.newMDRepository(result.storageProps);
        Assert.assertNotNull(result.repos);
        
        result.pkg = result.repos.getExtent(extentName);
        Assert.assertNotNull(result.pkg);        
        
        return result;
    }
    
    /**
     * Invokes {@link #fail(String, Throwable) fail(null, t)}.
     * 
     * @param t an exception
     */
    protected static void fail(Throwable t)
    {
        fail(null, t);
    }
    
    /**
     * Converts the message and exception into an error message and invokes
     * JUnit's {@link Assert#fail(String)}.
     * @param message error message
     * @param t exception
     */
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
    
    /**
     * Simple class describing the repository under test.
     */
    protected static class RepositoryDetails
    {
        public EnkiMDRepository repos;
        public RefPackage pkg;
        public Properties storageProps;
    }
}

// End ModelTestBase.java
