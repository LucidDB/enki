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

import java.lang.management.*;
import java.util.*;

import javax.management.*;
import javax.management.openmbean.*;

import org.eigenbase.enki.mdr.*;
import org.junit.*;
import org.junit.runner.*;

/**
 * MBeanTest tests Enki's JMX MBeans.
 * 
 * @author Stephan Zuercher
 */
@RunWith(SelectiveLoggingTestRunner.class)
@SelectiveLoggingTestRunner.Exclude(MdrProvider.ENKI_TRANSIENT)
public class MBeanTest extends SampleModelTestBase
{
    private static MBeanServer mbeanServer;
    
    public static void main(String[] args) throws Exception
    {
        setUpTestClass();
        try {
            startMBeanServer();
            try {
                new MBeanTest().testMbeans();
            } finally {
                stopMBeanServer();
            }
        } finally {
            tearDownTestClass();
        }
    }
    
    @BeforeClass
    public static void startMBeanServer() throws Exception
    {
        // Assume that the only MBean server is the platform server.
        // Test will fail if another one is registered, since EnkiMBeanUtil
        // will prefer it to this one.
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    
    @AfterClass
    public static void stopMBeanServer() throws Exception
    {
    }
    
    @Test
    public void testMbeans() throws Exception
    {
        Set<?> mbeans = mbeanServer.queryMBeans(null, null);
        
        // Prefixes match EnkiMBeanUtils.getName()
        List<String> providerPrefixes = new ArrayList<String>();
        for(MdrProvider provider: MdrProvider.values()) {
            providerPrefixes.add(provider.toString() + "#");
        }
        
        ObjectName mbeanName = null;
        for(Object o: mbeans) {
            ObjectInstance mbeanInstance = (ObjectInstance)o;

            for(String providerPrefix: providerPrefixes) {
                if (mbeanInstance.getObjectName().toString().contains(
                        providerPrefix))
                {
                    Assert.assertNull(
                        "found multiple matching mbeans", mbeanName);
                    mbeanName = mbeanInstance.getObjectName();
                }
            }
        }
        Assert.assertNotNull(mbeanName);
        
        MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(mbeanName);
        Assert.assertNotNull(mbeanInfo);
        
        MBeanAttributeInfo[] mbeanAttribs = mbeanInfo.getAttributes();
        Assert.assertNotNull(mbeanAttribs);

        for(MBeanAttributeInfo mbeanAttrib: mbeanAttribs) {
            String name = mbeanAttrib.getName();
            Assert.assertTrue(name, mbeanAttrib.isReadable());
            Assert.assertFalse(name, mbeanAttrib.isWritable());

            Object value = mbeanServer.getAttribute(mbeanName, name);
            
            System.out.println("Attrib: " + name);
            if (name.equals("ProviderType")) {
                Assert.assertEquals(
                    getRepository().getProviderType().toString(), value);
            } else if (name.equals("ExtentNames")) {
                String[] extentNames = (String[])value;
                
                Assert.assertEquals(3, extentNames.length);
                Set<String> extentNamesSet = 
                    new HashSet<String>(Arrays.asList(extentNames));
                
                Set<String> expected = 
                    new HashSet<String>(
                        Arrays.asList(
                            new String[] {
                                "MOF",
                                "SampleMetamodel",
                                "SampleRepository"
                            }));
                Assert.assertEquals(expected, extentNamesSet);
            } else if (name.equals("StorageProperties") ||
                       name.equals("PerformanceStatistics"))
            {
                // Just enumerate the values without expecting any specific
                // values.  These will vary widely by repositoroy type.
                TabularData table = (TabularData)value;
                
                Collection<?> rows = table.values();
                for(Object anonRow: rows) {
                    CompositeData row = (CompositeData)anonRow;
                
                    Collection<?> values = row.values();
                    
                    Assert.assertEquals(
                        "Value in: " + row.toString(), 2, values.size());
                }
            } else {
                Assert.fail("Unknown MBean attribute: " + name);
            }
        }
        
        MBeanConstructorInfo[] mbeanConstructors = mbeanInfo.getConstructors();
        Assert.assertTrue(
            mbeanConstructors == null || mbeanConstructors.length == 0);

        MBeanOperationInfo[] mbeanOps = mbeanInfo.getOperations();
        Assert.assertNotNull(mbeanOps);
        
        for(MBeanOperationInfo mbeanOp: mbeanOps) {
            String name = mbeanOp.getName();
            
            System.out.println("Op: " + name);
            
            if (!name.equals("getExtentAnnotation") &&
                !name.equals("enablePerformanceStatistics") &&
                !name.equals("disablePerformanceStatistics"))
            {
                Assert.fail("Unknown MBean operation: " + name);
                
            }
        }
    }
}

// End MBeanTest.java
