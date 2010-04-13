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
package org.eigenbase.enki.codegen;

/**
 * CodeGenXmlOutput represents a class capable of generating XML data to 
 * some output.
 * 
 * @author Stephan Zuercher
 */
public interface CodeGenXmlOutput extends CodeGenOutput
{
    public void writeXmlDecl();

    public void writeDocType(String rootElem, String pub, String sys);

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
    public void setXmlNamespace(String ns, String uri);

    public void clearXmlNamespace();

    /**
     * Starts an element. 
     * 
     * @param name element name
     * @param attribs alternating attribute names and un-escaped values.
     * @throws GenerationException if length of attribs is not even or 0
     */
    public void startElem(String name, Object... attribs)
        throws GenerationException;

    /**
     * Ends the given element after decreasing the current indent.
     * 
     * @param name element name
     */
    public void endElem(String name)
        throws GenerationException;

    /**
     * Writes an empty element. 
     * 
     * @param name element name
     * @param attribs alternating attribute names and un-escaped values.
     * @throws GenerationException if length of attribs is not even or 0
     */
    public void writeEmptyElem(String name, Object... attribs)
        throws GenerationException;

    /**
     * Writes a simple element.  A simple element contains only text content.
     * 
     * @param name element name
     * @param value element content 
     * @param attribs attributes and values
     * @throws GenerationException if length of attribs is not even or 0
     */
    public void writeSimpleElem(String name, String value, Object... attribs)
        throws GenerationException;

    /**
     * Writes text output with XML escaping.
     * 
     * @param strings objects to convert to strings before output
     */
    public void writeText(Object... strings)
        throws GenerationException;

    /**
     * Writes an XML CDATA section.
     * 
     * @param strings objects to convert to strings for output
     */
    public void writeCData(Object... strings);

    public void startCData();

    public void endCData();

    /**
     * Writes an XML comment with wrapping.
     * 
     * @param strings comment contents
     */
    public void writeComment(Object... strings);

    /**
     * Escapes the given value for use in an XML attribute.
     * 
     * @param value attribute value to escape
     * @return escaped value
     * @throws GenerationException if the string cannot be represented in XML
     */
    public String escapeAttrib(String value)
        throws GenerationException;

    /**
     * Escapes the given text for use as text in an XML document.
     * 
     * @param value text data
     * @return escaped text data
     */
    public String escapeText(String value)
        throws GenerationException;

}
// End CodeGenXmlOutput.java
