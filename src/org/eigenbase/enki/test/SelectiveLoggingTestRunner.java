/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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
import java.lang.annotation.*;
import java.util.*;

import org.eigenbase.enki.mdr.*;
import org.junit.internal.runners.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * SelectiveLoggingTestRunner allows tests in a class to be selectively 
 * executed based on the configured MdrProvider.  Use the inner annotations
 * {@link Include} or {@link Exclude} to choose providers.  By default, all
 * providers are included.  Specifying an inclusion causes only the tests to
 * be executed only for the listed providers.
 * 
 * @author Stephan Zuercher
 */
public class SelectiveLoggingTestRunner
    extends LoggingTestRunner
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Include
    {
        MdrProvider[] value();
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Exclude
    {
        MdrProvider[] value();
    }

    protected final MdrProvider mdrProvider;
    private final boolean runTests;    
    
    public SelectiveLoggingTestRunner(Class<?> klass)
        throws InitializationError
    {
        super(klass);
        
        this.mdrProvider = getMdrProvider();
        this.runTests = shouldTestsRun(klass);
    }

    public SelectiveLoggingTestRunner(Class<?> klass, Runner runner)
        throws InitializationError
    {
        super(klass, runner);
        
        this.mdrProvider = getMdrProvider();
        this.runTests = shouldTestsRun(klass);
    }
    
    private static MdrProvider getMdrProvider()
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
            return null;
        }
        
        String providerName = 
            props.getProperty(MDRepositoryFactory.ENKI_IMPL_TYPE);
        
        MdrProvider provider = MdrProvider.valueOf(providerName);
        
        return provider;
    }

    protected boolean shouldTestsRun(Class<?> klass)
    {
        Include includeAnno = klass.getAnnotation(Include.class);
        Exclude excludeAnno = klass.getAnnotation(Exclude.class);
        
        if (includeAnno != null) {
            if (excludeAnno != null) {
                getLogger().warning(
                    klass.toString() + 
                    " has both @Include and @Exclude, ignoring exclusion(s)");
            }
            
            for(MdrProvider includedProvider: includeAnno.value()) {
                if (mdrProvider == includedProvider) {
                    return true;
                }
            }
            
            return false;
        } else if (excludeAnno != null) {
            for(MdrProvider excludedProvider: excludeAnno.value()) {
                if (mdrProvider == excludedProvider) {
                    return false;
                }
            }
            
            return true;
        }
        
        // Default is "include all"
        return true;
    }
    
    @Override
    public void run(RunNotifier notifier)
    {
        if (runTests) {
            super.run(notifier);
        } else {
            getLogger().info("Skipping " + getTestClass());
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

// End SelectiveLoggingTestRunner.java
