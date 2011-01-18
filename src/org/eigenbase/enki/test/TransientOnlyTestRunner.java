/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2011 The Eigenbase Project
// Copyright (C) 2011 SQLstream, Inc.
// Copyright (C) 2011 Dynamo BI Corporation
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.eigenbase.enki.test;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.test.*;
import org.junit.internal.runners.*;
import org.junit.runner.*;

/**
 * TransientOnlyTestRunner is an implementation of {@link Runner} that only
 * invokes tests in the test class if Enki's transient implementation is
 * in use.  Useful when tests are transient-specific.
 *
 * @author John Sichi
 * @version $Id$
 */
public class TransientOnlyTestRunner
    extends SelectiveLoggingTestRunner
{
    public TransientOnlyTestRunner(Class<?> klass) 
        throws InitializationError
    {
        super(klass);
    }

    public TransientOnlyTestRunner(Class<?> klass, Runner runner)
        throws InitializationError
    {
        super(klass, runner);
    }

    @Override
    protected boolean shouldTestsRun(Class<?> klass)
    {
        return mdrProvider == MdrProvider.ENKI_TRANSIENT;
    }
}

// End TransientOnlyTestRunner.java
