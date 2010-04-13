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

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.junit.*;
import org.netbeans.api.mdr.*;

/**
 * ProviderComparisonTestBase allows an Enki-specific provider (e.g. 
 * the Hibernate provider) to be compared with Netbeans MDR.  
 * 
 * REVIEW: SWZ: 2008-02-21: Without additional build support, the Netbeans
 * repository doesn't contain the sample model, so only the MOF model can
 * be compared.  There is no fundamental reason that the build couldn't be
 * modified to support loading the Sample model in both repositories.  For
 * time-to-build reasons it might make sense to put BTree-based copy of
 * the Sample model under version control.
 * 
 * @author Stephan Zuercher
 */
public abstract class ProviderComparisonTestBase
    extends SampleModelTestBase
{
    protected static MDRepository altRepos;
    protected static RefPackage altPkg;
    protected static Properties altStorageProps;
    protected static String altExtentName;
    
    protected static boolean okayToRunTests()
    {
        return getMdrProvider() != MdrProvider.NETBEANS_MDR;
    }
    
    @BeforeClass
    public static void configureAlternateRepository()
    {
        Assert.assertNull(altRepos);
        Assert.assertNull(altPkg);
        
        if (okayToRunTests()) {
            getAltRepository();
        }
    }
    
    @AfterClass
    public static void shutdownAlternateRepository()
    {
        MDRepository repository = altRepos;
        if (repository != null) {
            altPkg = null;
            altRepos = null;
            repository.shutdown();

            Assert.assertTrue(okayToRunTests());
        }        
    }

    protected static MDRepository getAltRepository()
    {
        Assert.assertTrue(okayToRunTests());
        
        if (altRepos == null) {
            altLoad();
        }

        return altRepos;
    }

    protected static RefPackage getAltPackage()
    {
        Assert.assertTrue(okayToRunTests());
        
        if (altPkg == null) {
            altLoad();
        }

        return altPkg;
    }
    
    private static void altLoad()
    {
        altExtentName = "MOF";
        RepositoryDetails result = 
            loadRepository(
                altExtentName, "test/ComparisonTestStorage.properties");
        altRepos = result.repos;
        altPkg = result.pkg;
        altStorageProps = result.storageProps;
    }
    
}

// End ProviderComparisonTestBase.java
