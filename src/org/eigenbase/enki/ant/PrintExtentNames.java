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
package org.eigenbase.enki.ant;

import org.eigenbase.enki.mdr.*;


/**
 * PrintExtentNamesSubTask prints a list of existing extent names.
 * 
 * <p>Attributes: None.
 * 
 * @author Stephan Zuercher
 */
public class PrintExtentNames extends EnkiTask.SubTask
{
    public PrintExtentNames(String name)
    {
        super(name);
    }

    @Override
    protected void execute()
    {
        EnkiMDRepository repos = getMDRepository(true);
        repos.beginSession();
        try {
            String[] extentNames = repos.getExtentNames();
            for(String extentName: extentNames) {
                System.out.println(extentName);
            }
        } finally {
            repos.endSession();
        }
    }
}

// End PrintExtentNames.java
