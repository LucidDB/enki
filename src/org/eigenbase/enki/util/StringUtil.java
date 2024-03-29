/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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
package org.eigenbase.enki.util;

import java.util.*;
import java.util.regex.*;

import com.sun.org.apache.bcel.internal.generic.*;

/**
 * StringUtil provides static String utility methods.
 * 
 * @author Stephan Zuercher
 */
public class StringUtil
{
    /** Regular expression use to divide a UML name into "words." */
    private static final Pattern ALPHANUMERIC_WORD_BOUNDARY =
        Pattern.compile("\\P{Alnum}+");
    
    /** Regular expression use to divide a CamelCase name into "words." */
    private static final Pattern CAMELCASE_WORD_BOUNDARY =
        Pattern.compile("\\p{Lower}\\p{Upper}");
    
    
    private StringUtil()
    {
    }

    /**
     * Returns the given string with its first character converted to lower
     * case.
     * 
     * @param str any string
     * @return the given string with its first character converted to lower
     *         case.
     */
    public static String toInitialLower(String str)
    {
        if (str == null) {
            return null;
        }
        
        StringBuilder buf = new StringBuilder();
        int len = str.length();
        if (len > 0) {
            buf.append(Character.toLowerCase(str.charAt(0)));
            if (len > 1) {
                buf.append(str.substring(1));
            }
        }
        
        return buf.toString();
    }

    /**
     * Returns the given string with its first character converted to upper
     * case.
     * 
     * @param str any string
     * @return the given string with its first character converted to upper
     *         case.
     */
    public static String toInitialUpper(String str)
    {
        if (str == null) {
            return null;
        }
        
        StringBuilder buf = new StringBuilder();
        int len = str.length();
        if (len > 0) {
            buf.append(Character.toUpperCase(str.charAt(0)));
            if (len > 1) {
                buf.append(str.substring(1));
            }
        }
        
        return buf.toString();
    }


    /**
     * Takes an identifier and modifies to match the JMI Specification.
     * 
     * <p>The specification identifies four styles of identifier:
     * <ul>
     * <li>Package name identifiers are all-lower-case.</li>
     * <li>Class name identifiers are camel-case with an initial upper-case 
     *     letter.</li>
     * <li>Operation and attribute name identifiers are camel-case with an
     *     initial lower-case letter.</li>
     * <li>Constants and enumeration literals are all-upper-case with 
     *     underscores separating words.</li>
     * </ul>
     * 
     * @see JSR 40 Final Specification, Section 4.7.2, Rules for Generating 
     *      Identifiers
     * @param ident identifier to mangle
     * @param idType type of identifier
     * @return mangled identifier
     */
    public static String mangleIdentifier(String ident, IdentifierType idType)
    {
        // JMI says all identifiers are alphabetic only.  Netbeans accepts 
        // numbers, so we do as well.  First divide identifier into words based
        // on non-alphanumeric characters.
        String[] words = ALPHANUMERIC_WORD_BOUNDARY.split(ident); 
        
        StringBuilder mangledIdent = new StringBuilder();
        
        for(String word: words) {
            // Each word may already be CamelCase, so iterate over the 
            // sub-words. JMI spec just says "words" are converted to initial 
            // caps. Netbeans splits CamelCase into words, which is prettier.
            Matcher matcher = CAMELCASE_WORD_BOUNDARY.matcher(word);
            
            int start = 0;
            while(start < word.length()) {
                int end;
                if (matcher.find(start)) {
                    end = matcher.end() - 1;
                } else {
                    end = word.length();
                }
                
                String subword = word.substring(start, end);
            
                switch(idType) {
                case ALL_CAPS:
                    if (mangledIdent.length() > 0) {
                        mangledIdent.append('_');
                    }
                    mangledIdent.append(subword.toUpperCase(Locale.US));
                    break;
                    
                case CAMELCASE_INIT_LOWER:
                case CAMELCASE_INIT_UPPER:
                    subword = subword.toLowerCase(Locale.US);
    
                    if (idType == IdentifierType.CAMELCASE_INIT_UPPER ||
                        mangledIdent.length() > 0)
                    {
                        // Capitalize
                        mangledIdent.append(
                            Character.toUpperCase(subword.charAt(0)));
                        mangledIdent.append(subword.substring(1));
                    } else {
                        // Initial lower-case word
                        mangledIdent.append(subword);
                    }
                    break;
                    
                case ALL_LOWER:
                    mangledIdent.append(subword.toLowerCase(Locale.US));
                    break;
                    
                case SIMPLE:
                    mangledIdent.append(subword);
                    break;
                    
                default:
                    throw new IllegalArgumentException(
                        "unknown identifier type");
                }
                
                start = end;
            }
        }
        
        return mangledIdent.toString();
    }
    

    /**
     * Identifies various types of identifier mangling, based on how the input 
     * is modified.  Word boundaries for identifiers are CamelCase case
     * transitions and non-alphanumeric characters.
     */
    public enum IdentifierType
    {
        /**
         * Converts strings to upper case identifiers with underscores 
         * separating words.
         */
        ALL_CAPS,
        
        /**
         * Converts strings to lower case identifiers with no word separation.
         */
        ALL_LOWER,
        
        /**
         * Converts strings to CamelCase identifiers with the first word 
         * starting in lower case.  Examples:
         * <table>
         * <tr>
         *   <th>Original</th>
         *   <th>Mangled Output</th>
         * </tr>
         * <tr>
         *   <td>SQL2003</td>
         *   <td>sql2003</td>
         * </tr>
         * <tr>
         *   <td>SQL2003Spec</td>
         *   <td>sql2003spec</td>
         * </tr>
         * <tr>
         *   <td>SQL2003spec</td>
         *   <td>sql2003spec</td>
         * </tr>
         * <tr>
         *   <td>originalSQLData</td>
         *   <td>originalSqldata</td>
         * </tr>
         * <tr>
         *   <td>one two three</td>
         *   <td>oneTwoThree</td>
         * </tr>
         * <tr>
         *   <td>MetaDataRepository</td>
         *   <td>metaDataRepository</td>
         * </tr>
         * <tr>
         *   <td>abc-xyz</td>
         *   <td>abcXyz</td>
         * </tr>
         * </table>
         */
        CAMELCASE_INIT_LOWER,
        
        /**
         * Converts strings to CamelCase identifiers with the first word 
         * starting in upper case.  Examples are the same as for 
         * {@link #CAMELCASE_INIT_LOWER}, with the exception that the first
         * letter is always capitalized.
         */
        CAMELCASE_INIT_UPPER,
        
        /**
         * Converts strings to identifiers by removing word non-alphanumeric
         * word separators. Capitalization is unmodified.
         */
        SIMPLE;
    }
}

// End StringUtil.java
