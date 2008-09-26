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
package org.eigenbase.enki.codegen;

import java.io.*;
import java.util.*;

/**
 * AbstractCodeGenOutput implements the {@link CodeGenXmlOutput} interface in
 * terms of a {@link PrintWriter} by extending {@link AbstractCodeGenOutput}.
 * Subclasses should {@link #setOutput(PrintWriter) set the output PrintWriter}
 * and the {@link #setEncoding(String) output's encoding}.
 * 
 * @author Stephan Zuercher
 */
public abstract class AbstractCodeGenXmlOutput
    extends AbstractCodeGenOutput
    implements CodeGenOutput, CodeGenXmlOutput
{
    private static final String COMMENT_END = "-->";
    private static final String COMMENT_START = "<!--";
    
    protected Stack<String> elemStack;
    private String xmlNamespace;
    private String xmlNamespaceUri;

    protected AbstractCodeGenXmlOutput()
    {
        this.elemStack = new Stack<String>();
    }
    
    public void writeXmlDecl()
    {
        writeln(
            "<?xml version=",
            QUOTE, "1.0", QUOTE,
            " encoding=", QUOTE, getEncoding(), QUOTE, "?>");
    }

    public void writeDocType(String rootElem, String pub, String sys)
    {
        writeln("<!DOCTYPE ", rootElem);
        increaseIndent();
        writeln("PUBLIC ", QUOTE, pub, QUOTE);
        writeln(" ", QUOTE, sys, QUOTE, ">");
        decreaseIndent();
    }

    /**
     * Sets the XML namespace (and namespace URI) going forward.  The next
     * emitted non-empty element will emit an "xmlns:xxx" attribute.  
     * That element and all subsequent elements will use the namespace by 
     * default.  It is the caller's responsibility to clear or reset the 
     * namespace at the appropriate scope.
     * 
     * @param ns XML namespace to use
     * @param uri XML namespace's URI
     */
    public void setXmlNamespace(String ns, String uri)
    {
        this.xmlNamespace = ns + ":";
        this.xmlNamespaceUri = uri;
    }

    public void clearXmlNamespace()
    {
        this.xmlNamespace = null;
        this.xmlNamespaceUri = null;
    }

    /**
     * Starts an element. 
     * 
     * @param name element name
     * @param attribs alternating attribute names and un-escaped values.
     * @throws GenerationException if length of attribs is not even or 0
     */
    public void startElem(String name, Object... attribs)
        throws GenerationException
    {
        startElemHanging(name, attribs);
        writeln();
        increaseIndent();
    }

    private void startElemHanging(String name, Object... attribs)
        throws GenerationException
    {
        if (xmlNamespaceUri != null) {
            // Add attribute
            Object[] augmented = new String[attribs.length + 2];
            augmented[0] = "xmlns:" + xmlNamespace;
            augmented[1] = xmlNamespaceUri;
            for(int i = 0; i < attribs.length; i++) {
                augmented[i + 2] = attribs[i];
            }
            xmlNamespaceUri = null;
            attribs = augmented;
        }
        
        if (attribs.length == 0) {
            write(
                "<",
                xmlNamespace != null ? xmlNamespace : "",
                name,
                ">");
        } else {
            if (attribs.length % 2 != 0) {
                throw new GenerationException(
                    "internal: attributes must come in name/value pairs");
            }
    
            int availableWidth = 
                WRAP_WIDTH - (getIndentLevel() * INDENT.length());
        
            int width = name.length() + 3; // length of "<elem/>"
            
            if (xmlNamespace != null) {
                width += xmlNamespace.length();
            }
            
            for(
                int i = 0; 
                i < attribs.length && width < availableWidth; 
                i += 2)
            {
                // length of " attrib=''"
                width += attribs[i].toString().length() + 4;
                
                width += escapeAttrib(attribs[i + 1].toString()).length();
            }
    
            if (width < availableWidth) {
                write(
                    "<",
                    xmlNamespace != null ? xmlNamespace : "",
                    name);
                for(int i = 0; i < attribs.length; i += 2) {
                    write(
                        " ",
                        attribs[i].toString(),
                        "=",
                        QUOTE, escapeAttrib(attribs[i + 1].toString()), QUOTE);
                }
                write(">");
            } else {
                if (attribs.length % 2 != 0) {
                    throw new GenerationException(
                        "internal: attributes must come in name/value pairs");
                }
                
                writeln(
                    "<",
                    xmlNamespace != null ? xmlNamespace : "",
                    name);
    
                increaseIndent();
                increaseIndent();
                for(int i = 0; i < attribs.length; i += 2) {
                    boolean last = i + 2 >= attribs.length; 
                    write(
                        attribs[i], "=", 
                        QUOTE, escapeAttrib(attribs[i + 1].toString()), QUOTE,
                        (last) ? ">" : "");
                    if (!last) {
                        writeln();
                    }
                }
                decreaseIndent();
                decreaseIndent();
            }
        }
        
        elementStartEvent(name);
    }

    /**
     * Ends the given element after decreasing the current indent.
     * 
     * @param name element name
     */
    public void endElem(String name)
        throws GenerationException
    {
        decreaseIndent();
        writeln("</", name, ">");
        
        elementEndEvent(name);
    }

    /**
     * Writes an empty element. 
     * 
     * @param name element name
     * @param attribs alternating attribute names and un-escaped values.
     * @throws GenerationException if length of attribs is not even or 0
     */
    public void writeEmptyElem(String name, Object... attribs)
        throws GenerationException
    {
        if (attribs.length == 0) {
            writeln("<", name, "/>");
        } else {
            if (attribs.length % 2 != 0) {
                throw new GenerationException(
                    "internal: attributes must come in name/value pairs");
            }
    
            int availableWidth = 
                WRAP_WIDTH - (getIndentLevel() * INDENT.length());
        
            int width = name.length() + 3; // length of "<elem/>"
            
            if (xmlNamespace != null) {
                width += xmlNamespace.length();
            }
            
            for(
                int i = 0; 
                i < attribs.length && width < availableWidth; 
                i += 2)
            {
                // length of " attrib=''"
                width += attribs[i].toString().length() + 4;
                
                width += escapeAttrib(attribs[i + 1].toString()).length();
            }
        
            write(
                "<",
                xmlNamespace != null ? xmlNamespace : "",
                name);                    
            if (width < availableWidth) {
                for(int i = 0; i < attribs.length; i += 2) {
                    write(
                        " ",
                        attribs[i],
                        "=",
                        QUOTE, escapeAttrib(attribs[i + 1].toString()), QUOTE);
                }
                writeln("/>");
            } else {
                writeln();
                increaseIndent();
                for(int i = 0; i < attribs.length; i += 2) {
                    writeln(
                        attribs[i], "=", 
                        QUOTE, escapeAttrib(attribs[i + 1].toString()), QUOTE,
                        (i + 2 >= attribs.length) ? "/>" : "");
                }
                decreaseIndent();
            }
        }
        
        elementStartEvent(name);
        elementEndEvent(name);
    }

    /**
     * Writes a simple element.  A simple element contains only text content.
     * 
     * @param name element name
     * @param value element content 
     * @param attribs attributes and values
     * @throws GenerationException if length of attribs is not even or 0
     */
    public void writeSimpleElem(String name, String value, Object... attribs)
        throws GenerationException
    {
        if (attribs.length > 2) {
            startElem(name, attribs);
            writeText(value);
            endElem(name);
        } else {
            startElemHanging(name, attribs);
            write(escapeText(value));
            writeln("</", name, ">");
            elementEndEvent(name);
        }
    }

    /**
     * Writes text output with XML escaping.
     * 
     * @param strings objects to convert to strings before output
     */
    public void writeText(Object... strings)
        throws GenerationException
    {
        StringBuilder buffer = new StringBuilder();
        for(Object s: strings) {
            buffer.append(s.toString());
        }
        String text = escapeText(buffer.toString());
        writeWrapped("", text);
    }

    /**
     * Writes an XML CDATA section.
     * 
     * @param strings objects to convert to strings for output
     */
    public void writeCData(Object...strings)
    {
        startCData();
        write(strings);
        endCData();
    }

    public void startCData()
    {
        write("<![CDATA[");
    }

    public void endCData()
    {
        writeln("]]>");
    }

    /**
     * Writes an XML comment with wrapping.
     * 
     * @param strings comment contents
     */
    public void writeComment(Object... strings)
    {
        int len = 0;
        for(Object s: strings) {
            len += s.toString().length();
        }
        
        int max =
            WRAP_WIDTH 
            - (getIndentLevel() * INDENT.length()) 
            - COMMENT_START.length()
            - COMMENT_END.length()
            - 2;
        if (len < max) {
            write(COMMENT_START, " ");
            write(strings);
            writeln(" ", COMMENT_END);
        } else {
            writeln(COMMENT_START);
            increaseIndent();
            writeWrapped("", strings);
            decreaseIndent();
            writeln(COMMENT_END);
        }
    }

    /**
     * Escapes the given value for use in an XML attribute.
     * 
     * @param value attribute value to escape
     * @return escaped value
     * @throws GenerationException if the string cannot be represented in XML
     */
    public String escapeAttrib(String value)
        throws GenerationException
    {
        value = 
            value
                .replaceAll("&", "&amp;")
                .replaceAll("'", "&apos;")
                .replaceAll(QUOTE, "&quot;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");
        
        for(char ch: value.toCharArray()) {
            if (ch < 0x0020 && ch != 0x0009 && ch != 0x000A && ch != 0x000D) {
                throw newCharacterException(ch, "control character");
            }
    
            if (Character.isHighSurrogate(ch) || Character.isLowSurrogate(ch))
            {
                throw newCharacterException(ch, "surrogate");
            }
        
            if (ch == 0xFFFE || ch == 0xFFFF) {
                throw newCharacterException(ch, "reserved for BOM");
            }
        }
        
        return value;
    }

    /**
     * Escapes the given text for use as text in an XML document.
     * 
     * @param value text data
     * @return escaped text data
     */
    public String escapeText(String value)
        throws GenerationException
    {
        return escapeAttrib(value);
    }

    private GenerationException newCharacterException(char ch, String reason)
    {
        String hex = Integer.toHexString((int)ch & 0xFFFF);
        
        return new GenerationException(
            "Cannot escape character " + hex + ": " + reason);
    }

    private void elementStartEvent(String elem)
        throws GenerationException
    {
        elemStack.push(elem);
    }

    private void elementEndEvent(String elem)
        throws GenerationException
    {
        if (elemStack.isEmpty()) {
            throw new GenerationException(
                "Unexpected end of element (no open elements): " + elem);
        }
        
        String expectedElem = elemStack.pop();
        
        if (!expectedElem.equals(elem)) {
            throw new GenerationException(
                "Unexpected end of element: " + elem + 
                ", expecting: " + expectedElem);
        }
    }
}

// End AbstractCodeGenXmlOutput.java
