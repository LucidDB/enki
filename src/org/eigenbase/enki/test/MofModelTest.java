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
package org.eigenbase.enki.test;

import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;
import org.netbeans.api.mdr.*;

/**
 * MofModelTest tests aspects of the built-in MOF model.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class MofModelTest extends ProviderComparisonTestBase
{
    private static final boolean DUMP_DEBUGGING_INFO = false;
    
    @Test
    public void testDependsOn()
    {
        if (!okayToRunTests()) {
            return;
        }
        
        SortedMap<String, List<String>> expected = 
            loadByAssoc(
                getAltRepository(), 
                "Class", 
                MofClass.class, 
                "Package", 
                "DependsOn", 
                "dependent");
        
        SortedMap<String, List<String>> got = 
            loadByAssoc(
                getRepository(), 
                "Class",
                MofClass.class, 
                "Package", 
                "DependsOn", 
                "dependent");
        
        dumpSortedMap(expected, got);

        Assert.assertEquals(expected, got);
    }

    private <T extends ModelElement> SortedMap<String, List<String>> loadByAssoc(
        MDRepository repos,
        String refClassName,
        Class<T> javaClass,
        String instanceName,
        String refAssocName,
        String assocEndName)
    {
        repos.beginTrans(false);
        try {
            ModelPackage modelPkg = (ModelPackage)repos.getExtent("MOF");
            
            RefClass refClass = modelPkg.refClass(refClassName);
            T mofInstance = 
                getByTypeAndName(refClass, javaClass, instanceName);
        
            RefAssociation refAssoc = modelPkg.refAssociation(refAssocName);
            
            Association mofAssoc = (Association)refAssoc.refMetaObject();
            AssociationEnd mofAssocEnd = 
                getContainedByTypeAndName(
                    mofAssoc, AssociationEnd.class, assocEndName);

            Collection<?> associatedEnds = 
                refAssoc.refQuery(mofAssocEnd, mofInstance);

            SortedMap<String, List<String>> result = 
                new TreeMap<String, List<String>>();
            for(Object o: associatedEnds) {
                Class<?> cls = o.getClass();
                String clsName = cls.getName();
                String expectedIfaceName;
                
                if (clsName.startsWith("org.eigenbase.enki.jmi.model.")) {
                    expectedIfaceName = 
                        "javax.jmi.model." + cls.getSimpleName();
                } else {
                    Assert.assertTrue(clsName.endsWith("$Impl"));
                    expectedIfaceName = 
                        clsName.substring(0, clsName.length() - 5);
                }
                
                // find the interface class
                Class<?> iface = null;
                for(Class<?> i: cls.getInterfaces()) {
                    if (i.getName().equals(expectedIfaceName)) {
                        iface = i;
                        break;
                    }
                }
                Assert.assertNotNull(iface);

                List<String> names;
                if (result.containsKey(iface.getName())) {
                    names = result.get(iface.getName());
                } else {
                    names = new ArrayList<String>();
                    result.put(iface.getName(), names);
                }
                
                ModelElement modelElem = (ModelElement)o;
                
                names.add(modelElem.getName());
            }
            
            sortMapValues(result);
            
            return result;
        }
        finally {
            repos.endTrans();
        }
    }

    private <T extends ModelElement> T getByTypeAndName(
        RefClass refClass, 
        Class<T> javaClass,
        String name)
    {
        Collection<T> allOfType =
            GenericCollections.asTypedCollection(
                refClass.refAllOfType(), javaClass);
        
        T result = null;
        for(T t: allOfType) {
            if (t.getName().equals(name)) {
                Assert.assertNull("multiple objects with same name", result);
                result = t;
            }
        }

        Assert.assertNotNull("named object not found", result);
        
        return result;
    }
    
    private <T extends ModelElement> T getContainedByTypeAndName(
        Namespace container, Class<T> javaClass, String name)
    {
        List<?> contents = container.getContents();
        
        T result = null;
        for(Object o: contents) {
            if (javaClass.isInstance(o)) {
                T t = javaClass.cast(o);
                
                if (t.getName().equals(name)) {
                    Assert.assertNull(
                        "multiple objects with same type and name", result);
                    result = t;
                }
            }
        }
        
        Assert.assertNotNull("named object not found", result);
        
        return result;
    }
    
    private void sortMapValues(Map<?, List<String>> map)
    {
        for(Map.Entry<?, List<String>> entry: map.entrySet()) {
            List<String> names = entry.getValue();
            Collections.sort(names);
        }
    }
    
    
    private void dumpSortedMap(
        Map<String, List<String>> expected,
        Map<String, List<String>> got)
    {
        if (!DUMP_DEBUGGING_INFO) {
            return;
        }
        
        final String INDENT = "    ";
        
        int colWidth = 0;
        for(String expectedKey: expected.keySet()) {
            colWidth = Math.max(colWidth, expectedKey.length());
        }
        for(List<String> expectedList: expected.values()) {
            for(String expectedValue: expectedList) {
                colWidth = 
                    Math.max(
                        colWidth, expectedValue.length() + INDENT.length());
            }
        }
        final char[] SPACES_ARRAY = new char[colWidth];
        Arrays.fill(SPACES_ARRAY, ' ');
        final String SPACES = new String(SPACES_ARRAY);
        
        Logger log = getTestLogger();
        
        log.info(
            "Expected" + SPACES.substring(0, colWidth - 8) + "\tGot");
        Iterator<Map.Entry<String, List<String>>> expectedIter =
            expected.entrySet().iterator();
        Iterator<Map.Entry<String, List<String>>> gotIter =
            got.entrySet().iterator();
        Map.Entry<String, List<String>> expectedEntry = null;
        Map.Entry<String, List<String>> gotEntry = null;
        while(
            expectedIter.hasNext() || expectedEntry != null ||
            gotIter.hasNext() || gotEntry != null)
        {
            if (expectedEntry == null && expectedIter.hasNext()) {
                expectedEntry = expectedIter.next();
            }
            if (gotEntry == null && gotIter.hasNext()) {
                gotEntry = gotIter.next();
            }
            
            String expectedKey = 
                expectedEntry != null ? expectedEntry.getKey() : null;
            String gotKey = gotEntry != null ? gotEntry.getKey() : null;
            
            int c;
            if (expectedKey == null) {
                c = 1;
            } else if (gotKey == null) {
                c = -1;
            } else {
                c = expectedKey.compareTo(gotKey);
            }
            
            StringBuilder b = new StringBuilder();
            if (c <= 0) {
                b
                    .append(expectedKey)
                    .append(
                        SPACES.substring(0, colWidth - expectedKey.length()));
            } else {
                b.append(SPACES);
            }
            if (c >= 0) {
                b.append("\t" + gotKey);
            }
            log.info(b.toString());
            
            
            List<String> expectedList = Collections.emptyList();
            if (c <= 0) {
                expectedList = expectedEntry.getValue();
            }
            List<String> gotList = Collections.emptyList();
            if (c >= 0) {
                gotList = gotEntry.getValue();
            }
            Iterator<String> expectedValueIter = expectedList.iterator();
            Iterator<String> gotValueIter = gotList.iterator();
            String expectedValue = null;
            String gotValue = null;
            while(
                expectedValueIter.hasNext() || expectedValue != null || 
                gotValueIter.hasNext() || gotValue != null)
            {
                if (expectedValue == null && expectedValueIter.hasNext()) {
                    expectedValue = expectedValueIter.next();
                }
                if (gotValue == null && gotValueIter.hasNext()) {
                    gotValue = gotValueIter.next();
                }
                    
                int c2;
                if (expectedValue == null) {
                    c2 = 1;
                } else if (gotValue == null) {
                    c2 = -1;
                } else {
                    c2 = expectedValue.compareTo(gotValue);
                }

                b.setLength(0);
                if (c2 <= 0) {
                    b
                        .append("    " + expectedValue)
                        .append(
                            SPACES.substring(
                                0, colWidth - expectedValue.length() - 4));

                    expectedValue = null;
                } else {
                    b.append(SPACES);
                }
                if (c2 >= 0) {
                    b.append("\t    " + gotValue);
                    gotValue = null;
                }
                log.info(b.toString());
            }
            
            if (c <= 0) {
                expectedEntry = null;
            }
            if (c >= 0) {
                gotEntry = null;
            }
        }
    }
}

// End MofModelTest.java
