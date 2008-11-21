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
package org.eigenbase.enki.test.plugin;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.test.*;
import org.junit.*;
import org.junit.runner.*;

/**
 * PluginBackupRestoreTest extends {@link BackupRestoreTest} to include 
 * plug-in model classes.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class PluginBackupRestoreTest
    extends BackupRestoreTest
{
    @Override
    protected void createAdditionalData()
    {
        // Assumes an active metadata write transaction.
        
        RefPackage pluginPackage = getSamplePackage().refPackage("Plugin");
        Assert.assertNotNull(pluginPackage);
        
        RefClass bananaClass = 
            pluginPackage.refClass("PluginBanana");

        RefClass bananaTreeClass =
            pluginPackage.refClass("PluginBananaTree");
        
        RefObject banana = 
            bananaClass.refCreateInstance(Collections.EMPTY_LIST);

        RefObject bananaTree =
            bananaTreeClass.refCreateInstance(
                Collections.singletonList("My Backyard"));
        
        RefAssociation growsAssoc = pluginPackage.refAssociation("Grows");
        
        growsAssoc.refAddLink(banana, bananaTree);
    }

    @Override
    protected long validateAdditionalData(boolean returnMaxMofId)
    {
        // Assumes an active metadata read transaction.
        
        RefPackage pluginPackage = getSamplePackage().refPackage("Plugin");
        Assert.assertNotNull(pluginPackage);
        
        RefClass bananaClass = 
            pluginPackage.refClass("PluginBanana");
        Collection<?> bananas = bananaClass.refAllOfClass();
        Assert.assertEquals(1, bananas.size());
        
        RefClass bananaTreeClass =
            pluginPackage.refClass("PluginBananaTree");
        Collection<?> bananaTrees = bananaTreeClass.refAllOfClass();
        Assert.assertEquals(1, bananaTrees.size());
        
        RefObject bananaTree = (RefObject)bananaTrees.iterator().next();
        Assert.assertEquals("My Backyard", bananaTree.refGetValue("location"));
        
        return findMofId(
            returnMaxMofId,
            bananas,
            bananaTrees);
    }

}
