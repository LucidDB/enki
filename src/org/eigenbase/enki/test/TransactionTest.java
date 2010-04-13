/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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

import org.eigenbase.enki.mdr.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.*;

/**
 * TransactionTest tests various aspects of MDR transactions, such as nesting.
 * 
 * @author Stephan Zuercher
 */
@RunWith(SelectiveLoggingTestRunner.class)
@SelectiveLoggingTestRunner.Exclude(MdrProvider.ENKI_TRANSIENT)
public class TransactionTest extends SampleModelTestBase
{
    @Test
    public void testReadInWriteTransactionNesting()
    {
        getRepository().beginTrans(true);
        try {
            try {
                getSamplePackage().getCar().createCar();
            }
            catch(HibernateException e) {                
                Assert.fail("Object creation should not fail in write txn");
            }
            
            getRepository().beginTrans(false);
            
            try {
                getSamplePackage().getVehicle().refAllOfType();
                
                try {
                    getSamplePackage().getCar().createCar();
                } catch(HibernateException e) {
                    Assert.fail(
                        "Object creation succeedss in nested read txn");
                }
            } finally {
                getRepository().endTrans(false);
            }
        } finally {
            getRepository().endTrans(true);
        }
    }

    @Test
    public void testWriteInReadTransactionNesting()
    {
        getRepository().beginTrans(false);
        try {
            getSamplePackage().getVehicle().refAllOfType();
            
            try {
                getSamplePackage().getCar().createCar();
                
                Assert.fail("Object creation should fail in nested read txn");
            } catch(Throwable t) {
                // Expected
                t = null;
            }
            
            try {
                getRepository().beginTrans(true);
                
                Assert.fail("Write transaction nested in read did not fail");
            } catch(Throwable t) {
                // ignored
            }
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testReadInReadTransactionNesting()
    {
        getRepository().beginTrans(false);
        try {
            getSamplePackage().getVehicle().refAllOfType();
            
            try {
                getSamplePackage().getCar().createCar();
                
                Assert.fail("Object creation should fail in nested read txn");
            }
            catch(Throwable t) {
                // Expected
            }

            getRepository().beginTrans(false);
            
            getSamplePackage().getVehicle().refAllOfType();
            
            try {
                
                getSamplePackage().getCar().createCar();
                
                Assert.fail("Object creation should fail in nested read txn");
            }
            catch(Throwable t) {
                // Expected
            }
            
            getRepository().endTrans();
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testInnerRollbackWriteInWriteTransactionNesting()
    {
        String carMofId1 = null;
        String carMofId2 = null;
        getRepository().beginTrans(true);
        try {
            Car car1 = 
                getSamplePackage().getCar().createCar("Ford", "Focus", 2);
            carMofId1 = car1.refMofId();
            
            getRepository().beginTrans(true);
            try {
                Car car2 = 
                    getSamplePackage().getCar().createCar("Ford", "Fusion", 2);
                carMofId2 = car2.refMofId();
            } finally {
                // rollback occurs (nested txn)
                getRepository().endTrans(true);
            }
        } finally {
            // commit is converted to rollback (inner txn said rollback)
            getRepository().endTrans(false);
        }
        
        getRepository().beginTrans(false);
        try {
            // Since any nested txn said rollback, the txn was rolled back
            Assert.assertNull(
                getRepository().getByMofId(carMofId1));
            Assert.assertNull(
                getRepository().getByMofId(carMofId2));
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testInnerCommitWriteInWriteTransactionNesting()
    {
        String carMofId1 = null;
        String carMofId2 = null;
        getRepository().beginTrans(true);
        try {
            Car car1 = 
                getSamplePackage().getCar().createCar("Geo", "Metro", 2);
            carMofId1 = car1.refMofId();
            
            getRepository().beginTrans(true);
            try {
                Car car2 = 
                    getSamplePackage().getCar().createCar("Ford", "Fiesta", 2);
                carMofId2 = car2.refMofId();
            } finally {
                getRepository().endTrans(false);
            }
        } finally {
            getRepository().endTrans(true);
        }
        
        getRepository().beginTrans(false);
        try {
            // Since any nested txn said rollback, the txn was rolled back
            Assert.assertNull(
                getRepository().getByMofId(carMofId2));
            Assert.assertNull(
                getRepository().getByMofId(carMofId1));
        } finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testCommitWriteInWriteTransactionNesting()
    {
        String carMofId1 = null;
        String carMofId2 = null;
        getRepository().beginTrans(true);
        try {
            Car car1 = 
                getSamplePackage().getCar().createCar("Geo", "Metro", 2);
            carMofId1 = car1.refMofId();
            
            getRepository().beginTrans(true);
            try {
                Car car2 = 
                    getSamplePackage().getCar().createCar("Ford", "Fiesta", 2);
                carMofId2 = car2.refMofId();
            } finally {
                getRepository().endTrans(false);
            }
        } finally {
            getRepository().endTrans(false);
        }
        
        getRepository().beginTrans(false);
        try {
            Assert.assertNotNull(
                getRepository().getByMofId(carMofId2));
            Assert.assertNotNull(
                getRepository().getByMofId(carMofId1));
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testReadRollback()
    {
        getRepository().beginTrans(false);
        
        try {
            getRepository().endTrans(true);
            
            Assert.fail("Read transactions cannot be rolled back");
        } catch(Throwable t) {
            // Expected
        }            
    }
}

// End TransactionTest.java
