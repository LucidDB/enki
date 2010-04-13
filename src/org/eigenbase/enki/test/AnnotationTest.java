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

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.*;

/**
 * AnnotationTest tests the extent annotation API.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class AnnotationTest extends SampleModelTestBase
{
    @Test
    public void testAnnotation()
    {
        final String expectedAnno = "TS = " + new Date().toString();
        
        getRepository().beginTrans(true);
        try {
            String anno = getRepository().getAnnotation(getTestExtentName());
            
            Assert.assertTrue(anno == null || anno.length() == 0);
            
            getRepository().setAnnotation(getTestExtentName(), expectedAnno);

            anno = getRepository().getAnnotation(getTestExtentName());
            Assert.assertEquals(expectedAnno, anno);
        } finally {
            getRepository().endTrans(false);
        }
        
        // Test that annotation sticks across repository instances.
        bounceRepository();
        
        getRepository().beginTrans(true);
        try {
            String anno = getRepository().getAnnotation(getTestExtentName());            
            Assert.assertEquals(expectedAnno, anno);
            
            // Clear it out again.
            getRepository().setAnnotation(getTestExtentName(), null);
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    
}
