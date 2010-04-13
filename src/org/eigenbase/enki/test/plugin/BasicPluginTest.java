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
package org.eigenbase.enki.test.plugin;

import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.test.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.pluginbase.*;

/**
 * BasicPluginTest exercies the plugin model without directly using any of its
 * classes.
 * 
 * TODO: Build improvements to allow tests in this package to use
 *       plugin model classes directly either by 1) wrangling classpaths and
 *       JAR files and including Eclipse support or 2) move the plugin test 
 *       cases to a separate "plugin" directory and build/test them there. 
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class BasicPluginTest extends SampleModelTestBase
{
    @Test
    public void testPluginRefClasses()
    {
        getRepository().beginTrans(false);
        try {
            RefPackage pluginPackage = getSamplePackage().refPackage("Plugin");
            Assert.assertNotNull(pluginPackage);
            
            RefClass pluginElemClass = pluginPackage.refClass("PluginElement");
            Assert.assertNotNull(pluginElemClass);
            
            MofClass pluginElemMofClass = 
                (MofClass)pluginElemClass.refMetaObject();
            Assert.assertNotNull(pluginElemMofClass);
            List<?> pluginElemSupertypes = pluginElemMofClass.getSupertypes();
            Assert.assertEquals(1, pluginElemSupertypes.size());
            
            MofClass baseElementMofClass =
                (MofClass)pluginElemSupertypes.get(0);
            Assert.assertNotNull(baseElementMofClass);
            Assert.assertEquals("BaseElement", baseElementMofClass.getName());
            
            RefClass pluginBananaClass = 
                pluginPackage.refClass("PluginBanana");
            Assert.assertNotNull(pluginBananaClass);

            MofClass pluginBananaMofClass = 
                (MofClass)pluginBananaClass.refMetaObject();
            Assert.assertNotNull(pluginBananaMofClass);
            
            List<?> pluginBananaSupertypes = 
                pluginBananaMofClass.getSupertypes();
            Assert.assertEquals(1, pluginBananaSupertypes.size());
            MofClass pluginFruitMofClass = 
                (MofClass)pluginBananaSupertypes.get(0);
            Assert.assertNotNull(pluginFruitMofClass);
            Assert.assertEquals(
                "PluginFruit", pluginFruitMofClass.getName());
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testPluginEntities()
    {
        String bananaTreeMofId;
        String bananaMofId;
        getRepository().beginTrans(true);
        try {
            RefPackage pluginPackage = getSamplePackage().refPackage("Plugin");
            Assert.assertNotNull(pluginPackage);
            
            RefClass bananaClass = 
                pluginPackage.refClass("PluginBanana");

            RefClass bananaTreeClass =
                pluginPackage.refClass("PluginBananaTree");
            
            RefObject banana = 
                bananaClass.refCreateInstance(Collections.EMPTY_LIST);
            bananaMofId = banana.refMofId();
            
            RefObject bananaTree =
                bananaTreeClass.refCreateInstance(
                    Collections.singletonList("My Backyard"));
            bananaTreeMofId = bananaTree.refMofId();
            
            RefAssociation growsAssoc = pluginPackage.refAssociation("Grows");
            
            growsAssoc.refAddLink(banana, bananaTree);
        }
        finally {
            getRepository().endTrans(false);
        }
        
        // Read them back
        getRepository().beginTrans(false);
        try {
            RefObject bananaTree = 
                (RefObject)getRepository().getByMofId(bananaTreeMofId);
            Assert.assertNotNull(bananaTree);
            
            RefPackage pluginPackage = getSamplePackage().refPackage("Plugin");
            RefAssociation growsAssoc = pluginPackage.refAssociation("Grows");
            
            Collection<?> bananas = growsAssoc.refQuery("Grower", bananaTree);
            Assert.assertEquals(1, bananas.size());
            
            RefObject banana = (RefObject)bananas.iterator().next();
            Assert.assertEquals(bananaMofId, banana.refMofId());
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testCrossModelPluginAssociation()
    {
        // I sure picked great names for these:
        // pluginbase.BaseContainer contains pluginbase.BaseElement
        // pluginbase.ContainsBase associates BaseContainer(0..1) with
        //     BaseElement(0..*)

        // pluginbase.BaseContainer is a BaseElement
        // pluginbase.FooElement is a BaseContainer
        // pluginbase.BarElement is a BaseElement
        // plugin.PluginElement is a pluginbase.BaseElement
        // plugin.PluginBarElement is a pluginbase.BarElement
        // plugin.PluginFooElement is a pluginbase.FooElement

        String fooMofId;
        String pluginFooMofId;
        getRepository().beginTrans(true);
        try {
            // Create a FooElement and add a BarElement, PluginElement and 
            // PluginBarElement
            PluginBasePackage pluginBasePkg = 
                getSamplePackage().getPluginBase();
            RefPackage pluginPkg = getSamplePackage().refPackage("Plugin");
            RefClass pluginFooElementClass = 
                pluginPkg.refClass("PluginFooElement");
            RefClass pluginElementClass = pluginPkg.refClass("PluginElement");
            RefClass pluginBarElementClass = 
                pluginPkg.refClass("PluginBarElement");
            

            FooElement fooElement = 
                pluginBasePkg.getFooElement().createFooElement("foo");
            fooMofId = fooElement.refMofId();
            
            BarElement barElement = 
                pluginBasePkg.getBarElement().createBarElement("bar:foo");
            
            BaseElement pluginElement = 
                (BaseElement)pluginElementClass.refCreateInstance(
                    Collections.singletonList("plugin:foo"));
                
            BarElement pluginBarElement = 
                (BarElement)pluginBarElementClass.refCreateInstance(
                    Collections.singletonList("plugin-bar:foo"));
            
            fooElement.getContainedBase().add(barElement);
            fooElement.getContainedBase().add(pluginElement);
            fooElement.getContainedBase().add(pluginBarElement);
            
            // Create a PluginFooElement and add a BarElement, PluginElement,
            // and PluginBarElement
            
            FooElement pluginFooElement = 
                (FooElement)pluginFooElementClass.refCreateInstance(
                    Collections.singletonList("plugin-foo"));
            pluginFooMofId = pluginFooElement.refMofId();
            
            BarElement barElement2 = 
                pluginBasePkg.getBarElement().createBarElement(
                    "bar:plugin-foo");
            
            BaseElement pluginElement2 = 
                (BaseElement)pluginElementClass.refCreateInstance(
                    Collections.singletonList("plugin:plugin-foo"));
                
            BarElement pluginBarElement2 = 
                (BarElement)pluginBarElementClass.refCreateInstance(
                    Collections.singletonList("plugin-bar:plugin-foo"));
            
            pluginFooElement.getContainedBase().add(barElement2);
            pluginFooElement.getContainedBase().add(pluginElement2);
            pluginFooElement.getContainedBase().add(pluginBarElement2);
        }
        finally {
            getRepository().endTrans(false);
        }
        
        Set<String> expectedFooElementContentNames = 
            new HashSet<String>(
                Arrays.asList("bar:foo", "plugin:foo", "plugin-bar:foo"));
        Set<String> expectedPluginFooElementContentNames = 
            new HashSet<String>(
                Arrays.asList(
                    "bar:plugin-foo",
                    "plugin:plugin-foo", 
                    "plugin-bar:plugin-foo"));
        
        getRepository().beginTrans(false);
        try {
            // Read the FooElement and check its contents
            FooElement fooElement = 
                (FooElement)getRepository().getByMofId(fooMofId);
            
            Collection<BaseElement> contents = fooElement.getContainedBase();
        
            Set<String> gotFooElementContentNames = new HashSet<String>();
            for(BaseElement elem: contents) {
                gotFooElementContentNames.add(elem.getName());
            }
            Assert.assertEquals(
                expectedFooElementContentNames, gotFooElementContentNames);
            
            // repeat for PluginFooElement
            FooElement pluginFooElement =
                (FooElement)getRepository().getByMofId(pluginFooMofId);
            
            contents = pluginFooElement.getContainedBase();
        
            Set<String> gotPluginFooElementContentNames = new HashSet<String>();
            for(BaseElement elem: contents) {
                gotPluginFooElementContentNames.add(elem.getName());
            }
            Assert.assertEquals(
                expectedPluginFooElementContentNames, 
                gotPluginFooElementContentNames);
        }
        finally {
            getRepository().endTrans();
        }
    }
}

// End BasicPluginTest.java
