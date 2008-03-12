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

import java.io.*;
import java.util.*;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.test.*;
import org.junit.internal.runners.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * HibernateOnlyTestRunner is an implementation of {@link Runner} that only
 * invokes tests in the test class if Enki's Hibernate implementation is
 * in use.  Useful when tests are Enki/Hibernate specific.
 *  
 * @author Stephan Zuercher
 */
public class HibernateOnlyTestRunner
    extends TestClassRunner
{
    private final boolean runTests;
    
    public HibernateOnlyTestRunner(Class<?> klass) 
        throws InitializationError
    {
        super(klass);
        
        this.runTests = checkForHibernate();
    }

    public HibernateOnlyTestRunner(Class<?> klass, Runner runner)
        throws InitializationError
    {
        super(klass, runner);

        this.runTests = checkForHibernate();
    }

    private boolean checkForHibernate()
    {
        Properties props = new Properties();
        try {
            FileInputStream in = 
                new FileInputStream(
                    ModelTestBase.getStoragePropertiesPath());
            props.load(in);
            in.close();
        }
        catch(IOException e) {
            return false;
        }
        
        String providerName = 
            props.getProperty(MDRepositoryFactory.ENKI_IMPL_TYPE);
        
        MdrProvider provider = MdrProvider.valueOf(providerName);
        
        return provider == MdrProvider.ENKI_HIBERNATE;
    }
    
    @Override
    public void run(RunNotifier notifier)
    {
        if (runTests) {
            super.run(notifier);
        }
    }

    @Override
    public int testCount()
    {
        if (runTests) {
            return super.testCount();
        }
        
        return 0;
    }
}

// End HibernateOnlyTestRunner.java
