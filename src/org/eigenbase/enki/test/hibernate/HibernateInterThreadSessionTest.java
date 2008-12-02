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

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.test.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.*;

import java.util.*;

/**
 * HibernateInterThreadSessionTest tests
 * {@link HibernateMDRepository#enableInterThreadSessions}.
 *
 * @author John Sichi
 * @version $Id$
 */
@RunWith(HibernateOnlyTestRunner.class)
public class HibernateInterThreadSessionTest extends SampleModelTestBase
{
    private int nSuccess;

    @Test
    public void testMultipleThreads()
        throws Exception
    {
        // Use only brand new threads here so that their TLS gets initialized.

        HibernateMDRepository.enableInterThreadSessions(true);
        try {
        
            nSuccess = 0;
            Thread t1 = new Thread() 
                {
                    public void run() 
                    {
                        getRepository().beginSession();
                        getRepository().beginTrans(true);
                        getSamplePackage().getCar().createCar(
                            "Ford", "Pinto", 3);
                        ++nSuccess;
                    }
                };
            t1.start();
            t1.join();
                    
            Thread t2 = new Thread() 
                {
                    public void run() 
                    {
                        Collection c =
                            getSamplePackage().getCar().refAllOfType();
                        Assert.assertEquals(1, c.size());
                        Car car = (Car) c.iterator().next();
                        Assert.assertEquals("Pinto", car.getModel());
                        car.setDoors(2);
                        getRepository().endTrans();
                        getRepository().endSession();
                        ++nSuccess;
                    }
                };
            t2.start();
            t2.join();

            // If this fails, check the test log for excn details
            Assert.assertEquals(2, nSuccess);
        } finally {
            HibernateMDRepository.enableInterThreadSessions(false);
        }
    }
}

// End HibernateInterThreadSessionTest.java
