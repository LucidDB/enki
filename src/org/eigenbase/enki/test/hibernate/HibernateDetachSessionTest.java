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

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.test.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.*;

/**
 * HibernateDetachSessionTest tests Enki/Hibernate session detach/re-attach.
 * 
 * @author Stephan Zuercher
 */
@RunWith(HibernateOnlyTestRunner.class)
public class HibernateDetachSessionTest extends SampleModelTestBase
{
    @Test
    public void testTwoSession() throws Exception
    {
        String sess1CarMofId;
        boolean abort = true;
        // One session already exists (from test harness).
        getRepository().beginTrans(true);
        try {
            Car c = getSamplePackage().getCar().createCar("Ford", "Fiesta", 2);
            sess1CarMofId = c.refMofId();
            abort = false;
        } 
        finally {
            if (abort) {
                getRepository().endTrans(true);
            }
        }

        EnkiMDSession firstSession = getRepository().detachSession();
        
        try {
            getRepository().beginSession();
            try {
                getRepository().beginTrans(false);
                try {
                    Car c = (Car)getRepository().getByMofId(sess1CarMofId);
                    Assert.assertNull(c);
                }
                finally {
                    getRepository().endTrans();
                }
            }
            finally {
                getRepository().endSession();
            }
        }
        finally {
            getRepository().reattachSession(firstSession);
        }

        // First session should have been re-attached.  End it's txn and
        // verify the data was written.
        getRepository().endTrans(false);

        getRepository().beginTrans(false);
        try {
            Car c = (Car)getRepository().getByMofId(sess1CarMofId);
            Assert.assertNotNull(c);
        }
        finally {
            getRepository().endTrans();
        }
    }
}

// End HibernateDetachSessionTest.java
