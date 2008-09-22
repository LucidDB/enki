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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.runner.*;
import org.junit.*;

/**
 * XmiFileComparatorTest tests {@link XmiFileComparator}.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class XmiFileComparatorTest
{
    /**
     * Verifies that the stack overflow reported in ENK-1 does not regress.
     * 
     * @see <a href="http://issues.eigenbase.org/browse/ENK-1">ENK-1</a>
     */
    @Test
    public void verifyBugENK1() throws Exception
    {
        // The order of references and comparisons causes the issue. The 
        // original test case files were identical except for timestamp.
        String xmi = load("ENK-1-testcase.xmi");
        
        XmiFileComparator.assertEqual(xmi, xmi);
    }
    
    private static String load(String resourceName) throws IOException
    {        
        InputStream in = 
            XmiFileComparatorTest.class.getResourceAsStream(resourceName);

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        
        StringBuilder b = new StringBuilder();
        
        char[] buffer = new char[4096];
        int num;
        while((num = reader.read(buffer)) >= 0) {
            b.append(buffer, 0, num);
        }
        
        reader.close();
        
        return b.toString();
    }
}
