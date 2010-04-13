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

import java.io.*;
import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;
import javax.jmi.xmi.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.xmi.*;

import eem.sample.*;

/**
 * Tests export and import of models.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class ExportImportTest extends SampleModelTestBase
{
    private static String sampleMetamodelName;

    /**
     * Fixture for location of baseline export to diff against.
     */
    private static final File file =
        new File("test/results/ExportImportTest.xmi");
    
    @BeforeClass
    public static void populateExtent()
        throws Exception
    {
        getRepository().beginTrans(true);
        
        boolean rollback = true;
        try {
            String[] springfieldStateNames = {
                "Arkansas", "Colorado", "Florida", "Georgia", "Illinois", 
                "Indiana", "Iowa", "Kentucky", "Louisana", "Maine", 
                "Massachussets", "Michigan", "Minnesota", "Missouri",
                "Nebraska", "New Hampshire", "New Jersey", "New York", 
                "Ohio", "Oregon", "Pennsylvania", "South Carolina",
                "South Dakota", "Tennesee", "Vermont", "Virginia", 
                "West Virginia", "Wisconsin"
            };
            
            StateClass stateClass = getSamplePackage().getState();
            ArrayList<State> springfieldStates = new ArrayList<State>();
            for(String stateName: springfieldStateNames) {
                State state = stateClass.createState(stateName);
                springfieldStates.add(state);
            }
            
            Driver homerSimpson = 
                getSamplePackage().getDriver().createDriver(
                    "Homer J. Simpson", "XXX000");
            
            Car blueCar = 
                getSamplePackage().getCar().createCar("Ford", "Blue Car", 4);
            
            blueCar.setOwner(homerSimpson);
            blueCar.setDriver(homerSimpson);
            for(int i = 0; i < springfieldStates.size() / 4 * 3; i++) {
                blueCar.getRegistrar().add(springfieldStates.get(i));
            }
            
            Bus schoolBus = getSamplePackage().getBus().createBus(
                "MCI", "Yellow School Bus", 3);
                        
            Passenger lisaSimpson = 
                getSamplePackage().getPassenger().createPassenger(
                    "Lisa Simpson");
            
            Passenger ralphWiggum =
                getSamplePackage().getPassenger().createPassenger(
                    "Ralph Wiggum");
            
            Person montyBurns =
                getSamplePackage().getPerson().createPerson("Monty Burns");
            
            Driver ottoMann = 
                getSamplePackage().getDriver().createDriver(
                    "Otto Mann", "DEF456");
            
            schoolBus.setOwner(montyBurns);
            schoolBus.setDriver(ottoMann);
            schoolBus.getRider().add(lisaSimpson);
            schoolBus.getRider().add(ralphWiggum);
            for(int i = springfieldStates.size() / 4;
                i < springfieldStates.size(); 
                i++)
            {
                schoolBus.getRegistrar().add(springfieldStates.get(i));
            }

            // Create objects with a circular association
            CircularAssociationTest.createContainmentHierarchySansTxn(3, 3);
            
            rollback = false;
        } finally {
            getRepository().endTrans(rollback);
        }
        
        for(String extentName: getRepository().getExtentNames()) {
            if (!extentName.equals(getTestExtentName())) {
                sampleMetamodelName = extentName;
                break;
            }
        }
        
        getTestLogger().info("Metamodel = " + sampleMetamodelName);

        RefPackage refPackage = getPackage();
        
        exportExtent(refPackage, file);
    }
    
    @Test
    public void testExportImport() throws Exception
    {
        RefPackage refPackage = getPackage();
        
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.length() > 0L);
        
        deleteExtent(refPackage);
        
        // From here forward, the old refPackage instance and any base class 
        // methods are invalid!
        
        refPackage = createExtent();
        
        importExtent(refPackage, file);
        
        File file2 = new File("test/results/ExportImportTest2.xmi");
        exportExtent(refPackage, file2);

        XmiFileComparator.assertEqual(file, file2);
    }

    @Test
    public void testMultipleExtents()
        throws Exception
    {
        MDRepository mdrRepos = getRepository();
        if (mdrRepos instanceof HibernateMDRepository) {
            HibernateMDRepository hibernateRepos =
                (HibernateMDRepository) mdrRepos;
            hibernateRepos.enableMultipleExtentsForSameModel();
        }

        // Verify that two extents can coexist.  (Or three if
        // the fixture package still exists.)
        RefPackage refPackageX = createExtent("extentX");
        RefPackage refPackageY = createExtent("extentY");

        // Verify we can import into both of them.  Save the roots returned by
        // one of them, because we can't rely on package export yet with
        // Hibernate (currently, it would incorrectly include both extents
        // since it's not able to discriminate them).
        Collection<?> rootsX = importExtent(refPackageX, file);
        importExtent(refPackageY, file);

        File file2 = new File("test/results/ExportImportTestMultiple.xmi");
        exportXmi(null, file2, rootsX);
        
        deleteExtent(refPackageX);
        deleteExtent(refPackageY);

        // make sure other tests use the default mode
        bounceRepository();

        XmiFileComparator.assertEqual(file, file2);
    }

    @Test
    public void testMultipleExtentsUnsupportedByDefault()
        throws Exception
    {
        if (!(getRepository() instanceof HibernateMDRepository)) {
            // Only Hibernate has this limitation.
            return;
        }
        
        // Note that which createExtent call fails depends on
        // whether other tests have already deleted the fixture
        // package, but either way, one of them is
        // guaranteed to fail.
        RefPackage refPackageA = null;
        RefPackage refPackageB = null;
        try {
            refPackageA = createExtent("extentA");
            refPackageB = createExtent("extentB");
            Assert.fail("expected EnkiCreationFailedException");
        } catch (EnkiCreationFailedException ex) {
            Assert.assertEquals(
                "Metamodel 'SampleMetamodel' has already been instantiated",
                ex.getMessage());
        }
        if (refPackageA != null) {
            deleteExtent(refPackageA);
        }
    }
    
    private static void exportExtent(RefPackage refPackage, File exportFile) 
        throws IOException
    {
        exportXmi(refPackage, exportFile, null);
    }
    
    private static void exportXmi(
        RefPackage refPackage, File exportFile, Collection<?> roots) 
        throws IOException
    {
        getRepository().beginTrans(false);
        
        try {
            XMIWriter xmiWriter = GenericBackupRestore.createXmiWriter();
            FileOutputStream outStream = new FileOutputStream(exportFile);
            try {
                if (roots == null) {
                    xmiWriter.write(outStream, refPackage, "1.2");
                } else {
                    assert(refPackage == null);
                    xmiWriter.write(outStream, roots, "1.2");
                }
            } finally {
                outStream.close();
            }
        } finally {
            getRepository().endTrans();
        }
    }
    
    private void deleteExtent(RefPackage refPackage)
    {
        getRepository().beginTrans(true);
        
        try {
            refPackage.refDelete();
        } finally {
            getRepository().endTrans();
        }
    }
    
    private RefPackage createExtent() throws Exception
    {
        return createExtent(getTestExtentName());
    }
    
    private RefPackage createExtent(String extentName) throws Exception
    {
        getRepository().beginTrans(true);
        
        try {
            RefPackage metamodelExtent =
                getRepository().getExtent(sampleMetamodelName);
            
            ModelPackage modelPackage = (ModelPackage)metamodelExtent;
            
            MofPackage extentPackage = null;
            for(MofPackage mofPkg: 
                    GenericCollections.asTypedCollection(
                        modelPackage.getMofPackage().refAllOfClass(),
                        MofPackage.class))
            {
                if (mofPkg.getName().equals("EEM")) {
                    extentPackage = mofPkg;
                    break;
                }
            }

            return getRepository().createExtent(
                extentName, extentPackage);
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    private Collection<?> importExtent(
        RefPackage extent, File importFile) throws Exception
    {
        MDRepository mdrRepos = getRepository();

        XmiReader xmiReader = XMIReaderFactory.getDefault().createXMIReader();
        boolean rollback = false;
        try {
            mdrRepos.beginTrans(true);
            rollback = true;
            
            Collection<?> roots = xmiReader.read(
                importFile.toURL().toString(),
                extent);
            
            rollback = false;
            mdrRepos.endTrans();
            return roots;
        } finally {
            if (rollback) {
                mdrRepos.endTrans(true);
            }
        }
    }
}

// End ExportImportTest.java
