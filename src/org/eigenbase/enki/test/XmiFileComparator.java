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
package org.eigenbase.enki.test;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.junit.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * XmiFileComparator performs an order-insensitive comparison of two XMI files.
 * In addition to ignoring the order of model element instances as well as
 * the order of XML attributes, it does not require identical XMI ID values 
 * and ignores the timestamp value in the XMI header.
 * 
 * @author Stephan Zuercher
 */
public class XmiFileComparator
{
    private static final SAXParserFactory saxParserFactory = 
        SAXParserFactory.newInstance();

    public static void main(String[] args)
    {
        File f1 = new File("test/results/ExportImportTest.xmi");
        File f2 = new File("test/results/ExportImportTest2.xmi");
        
        assertEqual(f1, f2);
    }
    public static void assertEqual(File expectedFile, File actualFile)
    {
        Element expected = load(expectedFile, new ExpectedElementFactory());
        Element actual = load(actualFile, new ActualElementFactory());
        
        compare(expected, actual);
    }
    
    private static Element load(File file, ElementFactory elementFactory)
    {
        try {
            SAXParser parser = saxParserFactory.newSAXParser();
            
            Handler handler = new Handler(elementFactory);
            parser.parse(file, handler);
            
            return handler.getRootElement();
        }
        catch(Exception e) {
            ModelTestBase.fail(e);
            return null; // unreachable
        }
    }
    
    private static void compare(Element expected, Element actual)
    {
        compare(expected, actual, true);
    }
    
    private static void compare(
        Element expected, Element actual, boolean recurse)
    {
        check(
            "element name mismatch", 
            expected, 
            actual,
            expected.name,
            actual.name);
        
        check(
            "attrib count mismatch",
            expected,
            actual,
            expected.attributes.size(),
            actual.attributes.size());
        
        for(Map.Entry<String, String> entry: expected.attributes.entrySet()) {
            String attribName = entry.getKey();
            StringBuffer expectedAttrib = 
                new StringBuffer()
                .append(attribName)
                .append("=")
                .append(entry.getValue());
            StringBuffer actualAttrib = 
                new StringBuffer()
                .append(attribName)
                .append("=")
                .append(actual.attributes.get(attribName));
            check(
                "attrib value mismatch", 
                expected, 
                actual, 
                expectedAttrib.toString(), 
                actualAttrib.toString());
        }
        
        check(
            "reference mismatch", 
            expected, 
            actual, 
            expected.isReference, 
            actual.isReference);
        
        if (expected.isReference) {
            Element expectedRef = expected.getReferencedElement();
            Element actualRef = actual.getReferencedElement();
            
            compare(expectedRef, actualRef, false);
        }
        
        check(
            "child element count mismatch",
            expected,
            actual,
            expected.children.size(),
            actual.children.size());
        
        if (recurse) {
            for(int i = 0; i < expected.children.size(); i++) {
                Element expectedChild = expected.children.get(i);
                Element actualChild = actual.children.get(i);
                
                compare(expectedChild, actualChild);
            }
        }
    }
    
    private static void check(
            String msg,
            Element expectedSrc, 
            Element actualSrc, 
            Object expected, 
            Object actual)
    {
        StringBuffer m = new StringBuffer(msg);
        m
            .append(" (Expected Line: ")
            .append(expectedSrc.lineNumber)
            .append("; Acutal Line: ")
            .append(actualSrc.lineNumber)
            .append(")");
        Assert.assertEquals(m.toString(), expected, actual);
    }

    private abstract static class Element
    {
        private final String name;
        private final SortedMap<String, String> attributes;
        private final String xmiId;
        private final boolean isReference;
        private final int lineNumber;
        private final List<Element> children;
        private final StringBuffer characters;
        
        private Element(String name, Attributes xmlAttributes, Locator locator)
        {
            this.name = name;
            this.attributes = new TreeMap<String, String>();
            
            String xmiId = null;
            boolean isReference = false;
            for(int i = 0; i < xmlAttributes.getLength(); i++) {
                String attribName = xmlAttributes.getQName(i);
                String attribValue = xmlAttributes.getValue(i);
                
                if (attribName.equals("xmi.id")) {
                    assert(xmiId == null);
                    xmiId = attribValue;
                } else if (attribName.equals("xmi.idref")) {
                    assert(xmiId == null);
                    xmiId = attribValue;
                    isReference = true;
                } else if (name.equals("XMI") && attribName.equals("timestamp")) {
                    this.attributes.put(attribName, "<timestamp-ignored>");                    
                } else {
                    this.attributes.put(attribName, attribValue);
                }
            }
            this.xmiId = xmiId;
            this.isReference = isReference;
            this.lineNumber = locator.getLineNumber();
            this.children = new ArrayList<Element>();
            this.characters = new StringBuffer();
            
            if (!isReference && xmiId != null) {
                getXmiIdElemMap().put(xmiId, this);
            }
        }
     
        public Element getReferencedElement()
        {
            assert(isReference);
            
            Element element = getXmiIdElemMap().get(xmiId);
            return element;
        }
        
        public void finish()
        {
            Collections.sort(children, ElementNameComparator.instance);
            
            String chars = characters.toString().trim();
            characters.setLength(0);
            characters.append(chars);
        }
        
        public void addChild(Element child)
        {
            this.children.add(child);
        }
        
        protected abstract Map<String, Element> getXmiIdElemMap();
    }

    private static class ExpectedElement extends Element
    {
        private static Map<String, Element> xmiIdElemMap = 
            new HashMap<String, Element>();
        
        public ExpectedElement(
            String name, Attributes xmlAttributes, Locator locator)
        {
            super(name, xmlAttributes, locator);
        }
        
        @Override
        protected Map<String, Element> getXmiIdElemMap()
        {
            return xmiIdElemMap;
        }
    }
    
    private static class ActualElement extends Element
    {
        private static Map<String, Element> xmiIdElemMap = 
            new HashMap<String, Element>();
        
        public ActualElement(
            String name, Attributes xmlAttributes, Locator locator)
        {
            super(name, xmlAttributes, locator);
        }

        @Override
        protected Map<String, Element> getXmiIdElemMap()
        {
            return xmiIdElemMap;
        }
    }
    
    private static class ElementNameComparator 
        implements Comparator<Element>
    {
        private static final ElementNameComparator instance = 
            new ElementNameComparator();
        
        public int compare(Element o1, Element o2)
        {
            return o1.name.compareTo(o2.name);
        }
    }
    
    private static class Handler extends DefaultHandler
    {
        private final ElementFactory elementFactory;
        private Element root;
        private Stack<Element> elementStack;
        private Locator locator;
        
        private Handler(ElementFactory elementFactory)
        {
            this.elementFactory = elementFactory;
            this.elementStack = new Stack<Element>();
        }
        
        public Element getRootElement()
        {
            return root;
        }

        @Override
        public void setDocumentLocator(Locator locator)
        {
            this.locator = locator;
        }

        @Override
        public void startElement(
            String uri,
            String localName,
            String name,
            Attributes attributes)
            throws SAXException
        {
            Element element = 
                elementFactory.createElement(name, attributes, locator);

            if (!elementStack.empty()) {
                elementStack.peek().addChild(element);
            }
            
            elementStack.push(element);
        }

        @Override
        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            if (!elementStack.empty()) {
                elementStack.peek().characters.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String name)
            throws SAXException
        {
            Element element = elementStack.pop();

            element.finish();
            if (elementStack.empty()) {
                root = element;
            }
        }
    }
    
    private static abstract class ElementFactory
    {
        public abstract Element createElement(
            String name, Attributes xmlAttributes, Locator locator);
    }
    
    private static class ExpectedElementFactory extends ElementFactory
    {
        @Override
        public Element createElement(
            String name, Attributes xmlAttributes, Locator locator)
        {
            return new ExpectedElement(name, xmlAttributes, locator);
        }
    }
    
    private static class ActualElementFactory extends ElementFactory
    {
        @Override
        public Element createElement(
            String name, Attributes xmlAttributes, Locator locator)
        {
            return new ActualElement(name, xmlAttributes, locator);
        }
    }

}
// End XmiFileComparator.java
