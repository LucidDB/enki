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

import java.sql.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.test.*;
import org.hibernate.dialect.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.*;
import eem.sample.Driver;

/**
 * HibernateClassViewTest tests the all-of-class and all-of-type views
 * that Enki/Hibernate repositories automatically generate.
 * 
 * @author Stephan Zuercher
 */
@RunWith(HibernateOnlyTestRunner.class)
public class HibernateClassViewTest extends SampleModelTestBase
{
    private static final String ARCTIC_CAT = "Arctic Cat";
    private static final String Z1_TURBO = "Z1 Turbo";

    private static final String BLAIR_MORGAN = "Blair Morgan";
    private static final String LICENSE = "A1B2C3";

    private static final String MERCEDES_BENZ = "Mercedes-Benz";
    private static final String CITARO_G = "Citaro G";
    private static final int AXLES = 3;

    private static String vehicleMofId;
    private static String busMofId;
    private static String driverMofId;
    
    private Connection connection;
    private Dialect dialect;
    
    @BeforeClass
    public static void createObjects()
    {
        getRepository().beginTrans(true);
        try {
            Vehicle v = 
                getSamplePackage().getVehicle().createVehicle(
                    ARCTIC_CAT, Z1_TURBO);
            vehicleMofId = v.refMofId();
            
            Bus b =
                getSamplePackage().getBus().createBus(
                    MERCEDES_BENZ, CITARO_G, AXLES);
            busMofId = b.refMofId();
            
            Driver d =
                getSamplePackage().getDriver().createDriver(
                    BLAIR_MORGAN, LICENSE);
            driverMofId = d.refMofId();
            
            v.setDriver(d);
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    @Before
    public void obtainConnection()
    {
        connection = null;
        
        HibernateMDRepository repos = (HibernateMDRepository)getRepository(); 
        
        dialect = repos.getSqlDialect();
        
        repos.beginTrans(false);
        connection = repos.getCurrentSession().connection();
    }
    
    @After
    public void releaseConnection()
    {
        connection = null;
        getRepository().endTrans(false);
        
        getRepository().endSession();
        getRepository().beginSession();
    }
    
    @Test
    public void testAllOfClassViews() throws SQLException
    {
        Statement stmt = connection.createStatement();
        try {
            ResultSet rset = stmt.executeQuery(
                "select * from " + 
                HibernateDialectUtil.quote(dialect, "SMPL_VC_Sample_Vehicle"));
            try {
                if (!rset.next()) {
                    Assert.fail("Missing exepcted Vehicle data");
                }
                
                validateVehicle(rset);
                
                if (rset.next()) {
                    Assert.fail("Retrieved extraneous Vehicle data");
                }
            } finally {
                rset.close();
            }
            
            rset = stmt.executeQuery(
                "select * from " + 
                HibernateDialectUtil.quote(dialect, "SMPL_VC_Sample_Bus"));
            try {
                if (!rset.next()) {
                    Assert.fail("Missing exepcted Bus data");
                }
                
                validateBus(rset, false);

                if (rset.next()) {
                    Assert.fail("Retrieved extraneous Bus data");
                }
            } finally {
                rset.close();
            }

        } finally {
            stmt.close();
        }
    }

    private void validateBus(ResultSet rset, boolean asVehicle)
        throws SQLException
    {
        String mofId = rset.getString("mofId");
        String mofClassName = rset.getString("mofClassName");
        String make = rset.getString("make");
        String model = rset.getString("model");
        
        Assert.assertEquals(busMofId, mofId);
        Assert.assertEquals("Bus", mofClassName);
        Assert.assertEquals(MERCEDES_BENZ, make);
        Assert.assertEquals(CITARO_G, model);
        
        if (!asVehicle) {
            int axles = rset.getInt("axles");
            Assert.assertEquals(AXLES, axles);
        }
    }

    private void validateVehicle(ResultSet rset) throws SQLException
    {
        String mofId = rset.getString("mofId");
        String mofClassName = rset.getString("mofClassName");
        String make = rset.getString("make");
        String model = rset.getString("model");
        String driver = rset.getString("Driver");
        
        Assert.assertEquals(vehicleMofId, mofId);
        Assert.assertEquals("Vehicle", mofClassName);
        Assert.assertEquals(ARCTIC_CAT, make);
        Assert.assertEquals(Z1_TURBO, model);
        Assert.assertEquals(driverMofId, driver);
    }
    
    @Test
    public void testAllOfTypeViews() throws SQLException
    {
        Statement stmt = connection.createStatement();
        try {
            ResultSet rset = stmt.executeQuery(
                "select * from " + 
                HibernateDialectUtil.quote(dialect, "SMPL_VT_Sample_Vehicle"));
            try {
                if (!rset.next()) {
                    Assert.fail("Missing exepcted Vehicle data");
                }
                
                boolean validatedVehicle = false;
                boolean validatedBus = false;
                do {
                    String mofId = rset.getString("mofId");
                    if (vehicleMofId.equals(mofId)) {
                        Assert.assertFalse(validatedVehicle);
                        validateVehicle(rset);
                        validatedVehicle = true;
                    } else if (busMofId.equals(mofId)) {
                        Assert.assertFalse(validatedBus);
                        validateBus(rset, true);
                        validatedBus = true;
                    } else {
                        Assert.fail("Unknown mofId " + mofId);
                    }
                } while(rset.next());
                
                Assert.assertTrue(validatedBus);
                Assert.assertTrue(validatedVehicle);
            } finally {
                rset.close();
            }
            
            rset = stmt.executeQuery(
                "select * from " + 
                HibernateDialectUtil.quote(dialect, "SMPL_VT_Sample_Bus"));
            try {
                if (!rset.next()) {
                    Assert.fail("Missing exepcted Bus data");
                }
                
                validateBus(rset, false);

                if (rset.next()) {
                    Assert.fail("Retrieved extraneous Bus data");
                }
            } finally {
                rset.close();
            }

        } finally {
            stmt.close();
        }
    }
}

// End HibernateClassViewTest.java
