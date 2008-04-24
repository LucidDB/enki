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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;

import eem.*;

/**
 * TagUtilTest tests utility methods in {@link TagUtil}.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class TagUtilTest extends SampleModelTestBase
{
    @Test
    public void testMaxLength()
    {
        List<Classifier> allClasses = getAllClassifiers();
        
        for(Classifier c: allClasses) {
            List<Attribute> attribs = getAttributes(c);
            for(Attribute a: attribs) {
                int maxLength = TagUtil.findMaxLengthTag(c, a, 128);
                Assert.assertTrue(maxLength > 0);
            }
        }
    }
    
    private List<Classifier> getAllClassifiers()
    {
        EemPackage pkg = getEemPackage();

        List<Classifier> list = new ArrayList<Classifier>();
        
        getAllClassifiers(list, pkg);
        
        return list;
    }
    
    private void getAllClassifiers(List<Classifier> list, RefPackage pkg)
    {
        for(RefClass cls: 
                GenericCollections.asTypedCollection(
                    pkg.refAllClasses(), RefClass.class))
        {
            Classifier c = (Classifier)cls.refMetaObject();
            
            list.add(c);
        }

        for(RefPackage p: 
                GenericCollections.asTypedCollection(
                    pkg.refAllPackages(), RefPackage.class))
        {
            getAllClassifiers(list, p);
        }
    }
    
    private List<Attribute> getAttributes(Classifier c)
    {
        List<Attribute> list = new ArrayList<Attribute>();
        
        for(Object o: c.getContents()) {
            if (o instanceof Attribute) {
                list.add((Attribute)o);
            }
        }
        
        return list;
    }
}



// End TagUtilTest.java
