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

import java.util.logging.*;

import org.junit.internal.runners.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * LoggingTestRunner logs entry and exit of test cases.  Also logs skipped
 * test cases and failures.
 * 
 * @author Stephan Zuercher
 */
public class LoggingTestRunner extends TestClassRunner
{
    public LoggingTestRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
    }

    public LoggingTestRunner(Class<?> klass, Runner runner)
        throws InitializationError
    {
        super(klass, runner);
    }
    
    protected Logger getLogger()
    {
        return ModelTestBase.getTestLogger();
    }

    @Override
    public void run(RunNotifier notifier)
    {
        super.run(new LoggingNotifier(notifier, getLogger()));
    }
    
    private static class LoggingNotifier extends RunNotifier
    {
        private final RunNotifier notifier;
        private final Logger log;
        
        private LoggingNotifier(RunNotifier notifier, Logger log)
        {
            super();
            
            this.notifier = notifier;
            this.log = log;
        }
        
        @Override
        public void fireTestStarted(Description description)
            throws StoppedByUserException
        {
            log.info("Entering " + description.getDisplayName());
            
            notifier.fireTestStarted(description);
        }

        @Override
        public void fireTestFailure(Failure failure)
        {
            log.info("Failed " + failure.getDescription());

            notifier.fireTestFailure(failure);
        }

        @Override
        public void fireTestFinished(Description description)
        {
            log.info("Leaving " + description.getDisplayName());
            
            notifier.fireTestFinished(description);
        }

        @Override
        public void fireTestIgnored(Description description)
        {
            log.info("Skipped " + description.getDisplayName());
            
            notifier.fireTestIgnored(description);
        }

        @Override
        public void fireTestRunStarted(Description description)
        {
            notifier.fireTestRunStarted(description);
        }

        @Override
        public void fireTestRunFinished(Result result)
        {
            notifier.fireTestRunFinished(result);
        }

        @Override
        public void addFirstListener(RunListener listener)
        {
            notifier.addFirstListener(listener);
        }

        @Override
        public void addListener(RunListener listener)
        {
            notifier.addListener(listener);
        }

        @Override
        public void removeListener(RunListener listener)
        {
            notifier.removeListener(listener);
        }

        @Override
        public void pleaseStop()
        {
            notifier.pleaseStop();
        }
    }
}

// End LoggingTestRunner.java
