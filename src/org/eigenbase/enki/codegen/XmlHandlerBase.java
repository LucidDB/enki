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
    implements CodeGenXmlOutput
{
    private final CodeGenXmlOutputWrapper wrapper;
    
    public XmlHandlerBase()
    {
        super();
        
        this.wrapper = new CodeGenXmlOutputWrapper(this);
    }

    public void clearXmlNamespace()
    {
        wrapper.clearXmlNamespace();
    }

    public void endCData()
    {
        wrapper.endCData();
    }

    public void endElem(String name)
        throws GenerationException
    {
        wrapper.endElem(name);
    }

    public String escapeAttrib(String value)
        throws GenerationException
    {
        return wrapper.escapeAttrib(value);
    }

    public String escapeText(String value)
        throws GenerationException
    {
        return wrapper.escapeText(value);
    }

    public void setXmlNamespace(String ns, String uri)
    {
        wrapper.setXmlNamespace(ns, uri);
    }

    public void startCData()
    {
        wrapper.startCData();
    }

    public void startElem(String name, Object... attribs)
        throws GenerationException
    {
        wrapper.startElem(name, attribs);
    }

    public void writeCData(Object... strings)
    {
        wrapper.writeCData(strings);
    }

    public void writeComment(Object... strings)
    {
        wrapper.writeComment(strings);
    }

    public void writeDocType(String rootElem, String pub, String sys)
    {
        wrapper.writeDocType(rootElem, pub, sys);
    }

    public void writeEmptyElem(String name, Object... attribs)
        throws GenerationException
    {
        wrapper.writeEmptyElem(name, attribs);
    }

    public void writeSimpleElem(String name, String value, Object... attribs)
        throws GenerationException
    {
        wrapper.writeSimpleElem(name, value, attribs);
    }

    public void writeText(Object... strings)
        throws GenerationException
    {
        wrapper.writeText(strings);
    }

    public void writeXmlDecl()
    {
        wrapper.writeXmlDecl();
    }
}

// End XmlHandlerBase.java
