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
package org.eigenbase.enki.netbeans;

import java.util.logging.*;

import javax.jmi.reflect.*;

import org.openide.ErrorManager;
import org.openide.util.*;
import org.openide.util.lookup.*;

/**
 * MdrTraceUtil provides a mechanism to redirect Netbeans MDR tracing to a
 * Java {@link Logger}.
 * 
 * @author Stephan Zuercher
 */
public class MdrTraceUtil
{
    // Don't use this Logger outside of TracingErrorManager.
    private static Logger tracer;

    private static final String LOOKUP_PROP_NAME = "org.openide.util.Lookup";

    private MdrTraceUtil()
    {
    }
    
    /**
     * Integrates MDR tracing with {@link java.util.logging}. Must be called 
     * before first usage of MDR.
     *
     * @param mdrTracer {@link Logger} for MDR tracing
     */
    public static void integrateTracing(Logger mdrTracer)
    {
        tracer = mdrTracer;

        // Install a lookup mechanism which will register our
        // TracingErrorManager.
        try {
            System.setProperty(
                LOOKUP_PROP_NAME,
                TraceIntegrationLookup.class.getName());

            // Force load of our lookup now if it hasn't been done yet.
            Lookup.getDefault();
        } finally {
            System.getProperties().remove(LOOKUP_PROP_NAME);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Helper class for implementing MDR trace integration.
     */
    public static class TraceIntegrationLookup
        extends ProxyLookup
    {
        public TraceIntegrationLookup()
        {
            // Delete the property we set to get here.
            System.getProperties().remove(LOOKUP_PROP_NAME);

            // Now it's safe to call the default.  This
            // is a recursive call, but it will hit the base case
            // now since we cleared the property first.
            Lookup defaultLookup = Lookup.getDefault();

            // Register our custom ErrorManager together with the default.
            ErrorManager em = new TracingErrorManager(tracer);
            setLookups(
                new Lookup[] {
                    defaultLookup,
                    Lookups.singleton(em)
                });
        }
    }

    /**
     * TracingErrorManager overrides the Netbeans ErrorManager to intercept
     * messages and route them to the configured tracer.
     */
    private static class TracingErrorManager
        extends ErrorManager
    {
        private final Logger tracer;

        TracingErrorManager(Logger tracer)
        {
            this.tracer = tracer;
        }

        // implement ErrorManager
        public Throwable attachAnnotations(
            Throwable t,
            Annotation [] arr)
        {
            return t;
        }

        // implement ErrorManager
        public Annotation [] findAnnotations(Throwable t)
        {
            return null;
        }

        // implement ErrorManager
        public Throwable annotate(
            Throwable t,
            int severity,
            String message,
            String localizedMessage,
            Throwable stackTrace,
            java.util.Date date)
        {
            Level level = convertSeverity(severity);
            if (!tracer.isLoggable(level)) {
                return t;
            }
            tracer.throwing(
                "MdrUtil.TracingErrorManager",
                "annotate",
                t);
            if (tracer.isLoggable(Level.FINEST) && (stackTrace != null)) {
                tracer.throwing(
                    "MdrUtil.TracingErrorManager",
                    "annotate:stackTrace",
                    stackTrace);
            }
            tracer.log(level, message, t);
            if (t instanceof JmiException) {
                JmiException ex = (JmiException) t;
                tracer.log(
                    level,
                    "JmiException.ELEMENT:  " + ex.getElementInError());
                tracer.log(
                    level,
                    "JmiException.OBJECT:  " + ex.getObjectInError());
            }
            if (t instanceof TypeMismatchException) {
                TypeMismatchException ex = (TypeMismatchException) t;
                tracer.log(
                    level,
                    "TypeMismatchException.EXPECTED:  "
                    + ex.getExpectedType());
            }
            return t;
        }

        // implement ErrorManager
        public void notify(int severity, Throwable t)
        {
            tracer.throwing("MdrUtil.TracingErrorManager", "notify", t);
        }

        // implement ErrorManager
        public void log(int severity, String s)
        {
            tracer.log(
                convertSeverity(severity),
                s);
        }

        private static Level convertSeverity(int severity)
        {
            switch (severity) {
            case INFORMATIONAL:
                return Level.FINE;
            case WARNING:
                return Level.WARNING;
            case USER:
                return Level.INFO;
            case EXCEPTION:
                return Level.SEVERE;
            case ERROR:
                return Level.SEVERE;
            default:
                return Level.FINER;
            }
        }

        // implement ErrorManager
        public ErrorManager getInstance(String name)
        {
            return this;
        }
    }
}

// End MdrTraceUtil.java
