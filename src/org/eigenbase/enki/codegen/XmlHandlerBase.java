/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007-2007 The Eigenbase Project
// Copyright (C) 2007-2007 Disruptive Tech
// Copyright (C) 2007-2007 LucidEra, Inc.
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


/**
 * XmlHandlerBase extends {@link HandlerBase} with convenience functions for
 * generating an XML file.
 * 
 * @author Stephan Zuercher
 */
public class XmlHandlerBase
    extends HandlerBase
{
    private static final String COMMENT_END = "-->";

    private static final String COMMENT_START = "<!--";

    private String xmlNamespace;
    private String xmlNamespaceUri;
    
    public XmlHandlerBase()
    {
        super();
    }
    
    protected void writeXmlDecl()
    {
        writeln(
            "<?xml version=",
            QUOTE, "1.0", QUOTE,
            " encoding=", QUOTE, encoding, QUOTE, "?>");
    }
    
    protected void writeDocType(String rootElem, String pub, String sys)
    {
        writeln("<!DOCTYPE ", rootElem);
        increaseIndent();
        writeln("PUBLIC ", QUOTE, pub, QUOTE);
        writeln(" ", QUOTE, sys, QUOTE, ">");
        decreaseIndent();
    }
    
    /**
     * Set the XML namespace (and namespace URI) going forward.  The next
     * emitted non-empty element will emit an "xmlns:xxx" attribute.  
     * That element and all subsequent elements will use the namespace by 
     * default.  It is the caller's responsibility to clear or reset the 
     * namespace at the appropriate scope.
     * 
     * @param ns
     * @param uri
     */
    protected void setXmlNamespace(String ns, String uri)
    {
        this.xmlNamespace = ns + ":";
        this.xmlNamespaceUri = uri;
    }
    
    protected void clearXmlNamespace()
    {
        this.xmlNamespace = null;
        this.xmlNamespaceUri = null;
    }
    
    /**
     * Start an element. 
     * 
     * @param name element name
     * @param attribs alternating attribute names and un-escaped values.
     * @throws GenerationException if length of attribs is not even or 0
     */
    protected void startElem(String name, Object... attribs) 
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
    }

    protected void endElem(String name)
    {
        decreaseIndent();
        writeln("</", name, ">");
    }
    
    /**
     * Write an empty element. 
     * 
     * @param name element name
     * @param attribs alternating attribute names and un-escaped values.
     * @throws GenerationException if length of attribs is not even or 0
     */
    protected void writeEmptyElem(String name, Object... attribs)
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
    }
    
    protected void writeSimpleElem(
        String name, 
        String value, 
        Object... attribs)
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
        }
    }
    
    protected void writeText(Object... strings)
    {
        StringBuilder buffer = new StringBuilder();
        for(Object s: strings) {
            buffer.append(s.toString());
        }
        String text = escapeText(buffer.toString());
        writeWrapped("", text);
    }
    
    protected void writeCData(Object...strings)
    {
        write("<![CDATA[");
        write(strings);
        writeln("]]>");
    }
    
    protected void writeComment(Object... strings)
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
    
    protected String escapeAttrib(String value)
    {
        value = 
            value
            .replaceAll("&", "&amp;")
            .replaceAll("'", "&apos;")
            .replaceAll(QUOTE, "&quot;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");
        return value;
    }
    
    protected String escapeText(String value)
    {
         return escapeAttrib(value);
    }
}

// End XmlHandlerBase.java
