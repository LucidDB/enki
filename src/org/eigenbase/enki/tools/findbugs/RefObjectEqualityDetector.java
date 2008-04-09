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
package org.eigenbase.enki.tools.findbugs;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.bcel.*;

/**
 * RefObjectEqualityDetector is a FindBugs detector plug-in which reports 
 * the use of the identify equality and inequality operators on instances
 * of {@link javax.jmi.reflect.RefObject}.
 * 
 * @author Stephan Zuercher
 */
public class RefObjectEqualityDetector 
    extends OpcodeStackDetector
    implements StatelessDetector
{
    private static final String REF_OBJECT_CLASS = 
        "javax.jmi.reflect.RefObject";
    
    private final BugReporter bugReporter;
    private JavaClass refObjClass;
    
    public RefObjectEqualityDetector(BugReporter bugReporter)
    {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int opCode)
    {
        if (opCode == IF_ACMPEQ || opCode == IF_ACMPNE) {
            // Object comparison (== or !=).  Are they RefObjects?
            
            OpcodeStack.Item rhs = stack.getStackItem(0);
            OpcodeStack.Item lhs = stack.getStackItem(1);
            
            if (isRefObject(lhs) || isRefObject(rhs)) {
                bugReporter.reportBug(
                    new BugInstance(
                        "ENKI_REFOBJECT_EQUALITY",
                        HIGH_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this));
            }
        }
    }
    
    private boolean isRefObject(OpcodeStack.Item item)
    {
        if (item.isNull()) {
            return false;
        }
        
        // JVM type signatures: "I" means int, "J" means long, 
        // "Ljava/lang/String;" means java.lang.String, etc.
        String signature = item.getSignature();
        
        if (!signature.startsWith("L")) {
            // Not an object
            return false;
        }
        
        // Convert Lfoo/bar/Klass; to foo.bar.Klass
        signature = signature.substring(1, signature.length() - 1);
        signature = signature.replace('/', '.');
        
        try {
            if (refObjClass == null) {
                refObjClass = Repository.lookupClass(REF_OBJECT_CLASS);
            }
            
            JavaClass cls = Repository.lookupClass(signature);
            
            return Repository.implementationOf(cls, refObjClass);
        }
        catch(ClassNotFoundException e) {
            // FindBugs' built-in detectors ignore these.  Make sure they
            // get noticed until it's better understood when (if ever) this
            // happens.
            throw new AssertionError(e);
        }
    }
}

// End RefObjectEqualityDetector.java
