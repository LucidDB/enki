/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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

import java.io.*;

import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.config.*;
import org.junit.*;
import org.junit.runner.*;

/**
 * TablePrefixInputStreamTest tests {@link TablePrefixInputStream}.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class TablePrefixInputStreamTest
{
    private static final String TABLE_PREFIX_SHORTER = "FOO_";
    private static final String TABLE_PREFIX_SAME = "SAME_LENGTH_X_";
    private static final String TABLE_PREFIX_LONGER= "LONGER_LENGTH_X_";
    
    private static final String[] TABLE_PREFIXES = {
        TABLE_PREFIX_SHORTER,
        TABLE_PREFIX_SAME,
        TABLE_PREFIX_LONGER,
    };
    
    private static final String TABLE_PREFIX_REF_REGEX = 
        "\\$\\{tablePrefix\\}";
    
    @Test
    public void testSimple() throws Exception
    {
        String[] inputs = {
            "${tablePrefix}",
            "${tablePrefix} at the start",
            "at the end ${tablePrefix}",
            "in the ${tablePrefix} middle",
        };
        
        for(String prefix: TABLE_PREFIXES) {
            for(int i = 0; i < inputs.length; i++) {
                String input = inputs[i];

                String expected =
                    input.replaceAll(
                        TABLE_PREFIX_REF_REGEX,
                        prefix);
            
                String result = filter(input, prefix);
                
                Assert.assertEquals(expected, result);
            }
        }
    }
    
    @Test
    public void testBufferBoundaries() throws Exception
    {
        final int bufferLen = TablePrefixInputStream.BASE_BUFFER_SIZE;
        final String bufferLenStr = makeString(bufferLen);
        
        final String refStr = HibernateMappingHandler.TABLE_REF;
        
        final int refLen = refStr.length();
        final int min = bufferLen - refLen;
        final int max = bufferLen + refLen;
        
        for(String prefix: TABLE_PREFIXES) {
            for(int i = min; i <= max; i++) {
                StringBuilder b = 
                    new StringBuilder(bufferLen * 2)
                        .append(bufferLenStr)
                        .append(bufferLenStr);
                
                b.replace(i, i + refLen, refStr);
                
                String input = b.toString();
            
                String expected = 
                    input.replaceAll(
                        TABLE_PREFIX_REF_REGEX,
                        prefix);
        
                String result = filter(input, prefix);
                
                Assert.assertEquals(expected, result);
            }
        }
    }
    
    @Test
    public void testBufferEndsAndBoundaries() throws Exception
    {
        final int bufferLen = TablePrefixInputStream.BASE_BUFFER_SIZE;
        final String bufferLenStr = makeString(bufferLen);
        
        final String refStr = HibernateMappingHandler.TABLE_REF;
        
        final int refLen = refStr.length();
        final int min = bufferLen - refLen;
        final int max = bufferLen + refLen;
        
        for(String prefix: TABLE_PREFIXES) {
            for(int i = min; i <= max; i++) {
                StringBuilder b = 
                    new StringBuilder(bufferLen * 2)
                        .append(bufferLenStr)
                        .append(bufferLenStr);
                
                b.replace(0, refLen, refStr);
                b.replace(i, i + refLen, refStr);
                b.replace(bufferLen - refLen, bufferLen, refStr);
                
                String input = b.toString();
            
                String expected = 
                    input.replaceAll(
                        TABLE_PREFIX_REF_REGEX,
                        prefix);
        
                String result = filter(input, prefix);
                
                Assert.assertEquals(expected, result);
            }
        }
    }
    
    @Test
    public void testAdjacentRefs() throws Exception
    {
        final int bufferLen = TablePrefixInputStream.BASE_BUFFER_SIZE;
        
        final String refStr = HibernateMappingHandler.TABLE_REF;
        final int refLen = refStr.length();
        
        for(String prefix: TABLE_PREFIXES) {
            for(int i = 0; i < refLen; i++) {
                StringBuilder b = 
                    new StringBuilder(bufferLen * 2 + refLen * 2);
                for(int j = 0; j < i; j++) {
                    b.append(' ');
                }
                while(b.length() < bufferLen * 2) {
                    b.append(refStr);
                }
                
                String input = b.toString();
            
                String expected = 
                    input.replaceAll(
                        TABLE_PREFIX_REF_REGEX,
                        prefix);
        
                String result = filter(input, prefix);
                
                Assert.assertEquals(expected, result);
            }
        }
    }
    
    @Test
    public void testNearlyAdjacentRefs() throws Exception
    {
        final int bufferLen = TablePrefixInputStream.BASE_BUFFER_SIZE;
        
        final String refStr = HibernateMappingHandler.TABLE_REF;
        final int refLen = refStr.length();
        
        for(String prefix: TABLE_PREFIXES) {
            for(int i = 0; i < refLen; i++) {
                StringBuilder b = 
                    new StringBuilder(bufferLen * 2 + refLen * 2);
                for(int j = 0; j < i; j++) {
                    b.append(' ');
                }
                while(b.length() < bufferLen * 2) {
                    b.append(refStr);
                    b.append(' ');
                }
                
                String input = b.toString();
            
                String expected = 
                    input.replaceAll(
                        TABLE_PREFIX_REF_REGEX,
                        prefix);
        
                String result = filter(input, prefix);
                
                Assert.assertEquals(expected, result);
            }
        }
    }
    
    private String filter(String input, String prefix) throws Exception
    {
        ByteArrayInputStream in = 
            new ByteArrayInputStream(input.getBytes("UTF-8"));
        
        TablePrefixInputStream tpin = new TablePrefixInputStream(in, prefix);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        int ch;
        while((ch = tpin.read()) != -1) {
            out.write(ch);
        }
        
        return new String(out.toByteArray(), "UTF-8");
    }
    
    private String makeString(int length)
    {
        StringBuilder b = new StringBuilder();

        for(int i = 0; i < length; i++) {
            b.append((char)('A' + (i % 26)));
        }
        
        return b.toString();
    }
}

// End TablePrefixInputStreamTest.java
