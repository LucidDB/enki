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

import org.junit.*;
import org.junit.runner.*;

import eem.sample.special.*;

/**
 * CharacterSetTest tests storing various non-ASCII strings in the repository
 * to insure that they are properly stored and retrieved.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class CharacterSetTest extends SampleModelTestBase
{
    private static final String GERMAN = "Z\u00fcrcherstra\u00dfe";
    
    // Lifted from Wikipedia: contains kanji, hiragana and katakana characters.
    // It's a newspaper headline that says "Radcliffe to compete in Olympic 
    // marathon, also implied to appear in the 10,000 m" (or at least that's
    // what Wikipedia claims). Run "native2ascii -reverse" on this file in
    // a unicode-enabled terminal window to see it in its native form. 
    private static final String JAPANESE = 
        "\u30e9\u30c9\u30af\u30ea\u30d5\u3001\u30de\u30e9\u30bd\u30f3\u4e94\u8f2a\u4ee3\u8868\u306b1\u4e07m\u51fa\u5834\u306b\u3082\u542b\u307f";

    @Test
    public void testCharacterSet()
    {
        String germanMofId;
        String japaneseMofId;
        
        getRepository().beginTrans(true);
        try {
            SampleElement germanElem =
                getSpecialPackage().getSampleElement().createSampleElement();
            germanElem.setName(GERMAN);
            germanMofId = germanElem.refMofId();
            
            SampleElement japaneseElem =
                getSpecialPackage().getSampleElement().createSampleElement();
            japaneseElem.setName(JAPANESE);
            japaneseMofId = japaneseElem.refMofId();
        } finally {
            getRepository().endTrans(false);
        }
        getRepository().endSession();
        getRepository().beginSession();
        
        getRepository().beginTrans(false);
        try {
            SampleElement germanElem = 
                (SampleElement)getRepository().getByMofId(
                    germanMofId,
                    getSpecialPackage().getSampleElement());
            Assert.assertNotNull(germanElem);
            Assert.assertEquals(GERMAN, germanElem.getName());
            
            SampleElement japaneseElem =
                (SampleElement)getRepository().getByMofId(
                    japaneseMofId,
                    getSpecialPackage().getSampleElement());
            Assert.assertNotNull(japaneseElem);
            Assert.assertEquals(JAPANESE, japaneseElem.getName());
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testStringLengthLimitWithCharacterSet() throws Exception
    {
        // The idea here is that we want to store a 128-character string
        // whose character set encoded representation is more than 128 bytes 
        // long.
        
        StringBuilder text128builder = new StringBuilder();
        boolean toggle= true;
        while(text128builder.length() < 128) {
            text128builder.append(toggle ? GERMAN : JAPANESE);
            toggle = !toggle;
        }
        text128builder.setLength(128);
        
        final String text128 = text128builder.toString();
        
        Assert.assertTrue(text128.getBytes("UTF-8").length > 128);

        String text128mofId;
        getRepository().beginTrans(true);
        try {
            SampleElement text128Elem =
                getSpecialPackage().getSampleElement().createSampleElement();
            text128Elem.setName(text128);
            text128mofId = text128Elem.refMofId();
        } finally {
            getRepository().endTrans(false);
        }
        getRepository().endSession();
        getRepository().beginSession();

        getRepository().beginTrans(false);
        try {
            SampleElement text128Elem =
                (SampleElement)getRepository().getByMofId(
                    text128mofId,
                    getSpecialPackage().getSampleElement());
            Assert.assertNotNull(text128Elem);
            Assert.assertEquals(text128, text128Elem.getName());
        } finally {
            getRepository().endTrans();
        }
    }
}

// End CharacterSetTest.java
