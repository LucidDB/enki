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
import javax.jmi.xmi.*;

import org.eigenbase.enki.mdr.*;
import org.junit.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.xmi.*;

import eem.sample.*;

/**
 * Tests export and importing models.
 * 
 * @author Stephan Zuercher
 */
public class ExportImportTest extends SampleModelTestBase
{
    @BeforeClass
    public static void populateExtent()
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
    }
    
    @Test
    public void testExportImport() throws Exception
    {
        if (getMdrProvider() == MdrProvider.NETBEANS_MDR) {
            // TODO: Fix discrepancy between Netbeans and Enki-Hibernate.
            // Netbeans RefPackage.refDelete() destroys the metamodel as well,
            // requiring a re-import (which this test doesn't do, hence the
            // skip).
            System.out.println("Skipping export/import test for Netbeans");
            return;
        }
        
        File file = new File("test/results/ExportImportTest.xmi");
        
        RefPackage refPackage = getPackage();
        
        exportExtent(refPackage, file);

        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.length() > 0L);
        
        deleteExtent(refPackage);
        
        importExtent(refPackage, file);
        
        File file2 = new File("test/results/ExportImportTest2.xmi");
        exportExtent(refPackage, file2);

        XmiFileComparator.assertEqual(file, file2);
    }

    private void exportExtent(RefPackage refPackage, File file) 
        throws IOException
    {
        getRepository().beginTrans(false);
        
        try {
            XmiWriter xmiWriter = 
                XMIWriterFactory.getDefault().createXMIWriter();
            FileOutputStream outStream = new FileOutputStream(file);
            try {
                xmiWriter.write(outStream, refPackage, "1.2");
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
    
    private void importExtent(RefPackage extent, File file) throws Exception
    {
        MDRepository mdrRepos = getRepository();

        XmiReader xmiReader = XMIReaderFactory.getDefault().createXMIReader();
        boolean rollback = false;
        try {
            mdrRepos.beginTrans(true);
            rollback = true;
            
            xmiReader.read(
                file.toURL().toString(),
                extent);
            
            rollback = false;
            mdrRepos.endTrans();
        } finally {
            if (rollback) {
                mdrRepos.endTrans(true);
            }
        }
    }    
}

// End ExportImportTest.java
