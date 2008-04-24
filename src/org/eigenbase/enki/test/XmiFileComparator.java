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
 * XmiFileComparator performs an order-insensitive comparison of two XMI files
 * or strings.  In addition to ignoring the order of model element instances 
 * as well as the order of XML attributes, it does not require identical XMI 
 * ID values and ignores the timestamp value in the XMI header.
 * 
 * @author Stephan Zuercher
 */
public class XmiFileComparator
{
    private static final SAXParserFactory saxParserFactory = 
        SAXParserFactory.newInstance();

    private static boolean writeSortedElements = false;
    
    public static void main(String[] args)
    {
        File f1;
        File f2;
        if (args.length >= 2) {
            f1 = new File(args[0]);
            f2 = new File(args[1]);
            
            if (args.length >= 3) {
                setWriteSortedElements(args[2].equals("true"));
            }
        } else {
            // Default are the output files of ExportImportTest
            f1 = new File("test/results/ExportImportTest.xmi");
            f2 = new File("test/results/ExportImportTest2.xmi");
        }
        
        assertEqual(f1, f2);
    }
    
    public static void setWriteSortedElements(boolean enable)
    {
        writeSortedElements = enable;
    }
    
    public static void assertEqual(File expectedFile, File actualFile)
    {
        Element expected = load(expectedFile, new ExpectedElementFactory());
        Element actual = load(actualFile, new ActualElementFactory());
        
        compare(expected, actual);
    }
    
    public static void assertEqual(String expectedXmi, String actualXmi)
    {
        Element expected = load(expectedXmi, new ExpectedElementFactory());
        Element actual = load(actualXmi, new ActualElementFactory());
        
        compare(expected, actual);
    }
    
    private static Element load(
        Reader in, 
        ElementFactory elementFactory,
        File sortedElementsDump)
    {
        try {
            SAXParser parser = saxParserFactory.newSAXParser();
            
            InputSource source = new InputSource(in);
            Handler handler = new Handler(elementFactory);
            
            parser.parse(source, handler);
            
            Element element = handler.getRootElement();
            
            if (writeSortedElements) {
                write(element, sortedElementsDump);
            }
            
            return element;
        }
        catch(Exception e) {
            ModelTestBase.fail(e);
            return null; // unreachable
        }
    }

    private static Element load(String xmi, ElementFactory elementFactory)
    {
        return load(new StringReader(xmi), elementFactory, null);
    }

    private static Element load(File file, ElementFactory elementFactory)
    {
        File dumpFile = null;
        if (writeSortedElements) {
            dumpFile = new File(file.getPath() + ".sorted");
        }
        
        FileReader in;
        try {
            in = new FileReader(file);
        } catch (FileNotFoundException e) {
            ModelTestBase.fail(e);
            return null; // unreachable
        }
        
        return load(in, elementFactory, dumpFile);
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
        StringBuffer m = 
            new StringBuffer(msg)
            .append(" (Expected Line: ")
            .append(expectedSrc.lineNumber)
            .append("; Acutal Line: ")
            .append(actualSrc.lineNumber)
            .append(")");
        Assert.assertEquals(m.toString(), expected, actual);
    }

    private static void write(Element element, File file) throws IOException
    {
        IdRemapper idRemapper = new IdRemapper();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        
        write(element, writer, 0, idRemapper);
    }
    
    private static void write(
        Element element,
        BufferedWriter writer,
        int indent,
        IdRemapper idRemapper) 
    throws IOException
    {
        indent(writer, indent);
        writer.write("<");
        writer.write(element.name);
        if (element.xmiId != null) {
            String id = idRemapper.remap(element.xmiId);
            if (element.isReference) {
                writer.write(" xmi.idref='");
            } else {
                writer.write(" xmi.id='");                
            }
            writer.write(id);
            writer.write("'");
        }
        
        for(Map.Entry<String, String> entry: element.attributes.entrySet()) {
            writer.write(" ");
            writer.write(entry.getKey());
            writer.write("='");
            writer.write(entry.getValue());
            writer.write("'");
        }
        
        if (element.characters.length() == 0 && element.children.isEmpty()) {
            // empty elem
            writer.write(" />");
            writer.newLine();
            return;
        }
        
        writer.write(">");

        if (element.children.isEmpty()) {
            writer.write(element.characters.toString());
        } else {
            writer.newLine();
        
            if (element.characters.length() > 0) {
                indent(writer, indent + 1);
                writer.write(element.characters.toString());
                writer.newLine();
            }
        
            for(Element child: element.children) {
                write(child, writer, indent + 1, idRemapper);
            }
        
            indent(writer, indent);
        }
        writer.write("</");
        writer.write(element.name);
        writer.write(">");
        writer.newLine();
    }

    private static void indent(BufferedWriter writer, int indent)
        throws IOException
    {
        for(int i = 0; i < indent; i++) {
            writer.write("  ");
        }
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
            assert(name != null);
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
            String chars = characters.toString().trim();
            characters.setLength(0);
            characters.append(chars);
        }
        
        public void sort()
        {
            for(Element childElement: children) {
                childElement.sort();
            }
            
            Collections.sort(children, ElementComparator.instance);
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
    
    private static class ElementComparator 
        implements Comparator<Element>
    {
        private static final ElementComparator instance = 
            new ElementComparator();
        
        public int compare(Element o1, Element o2)
        {
            int c = o1.name.compareTo(o2.name);
            if (c != 0) {
                return c;
            }
            
            if (!o1.attributes.isEmpty() || !o2.attributes.isEmpty()) {
                Iterator<Map.Entry<String, String>> iter1 =
                    o1.attributes.entrySet().iterator();
                Iterator<Map.Entry<String, String>> iter2 =
                    o2.attributes.entrySet().iterator();
                while(iter1.hasNext() && iter2.hasNext()) {
                    Map.Entry<String, String> e1 = iter1.next();
                    Map.Entry<String, String> e2 = iter2.next();
                    
                    c = e1.getKey().compareTo(e2.getKey());
                    if (c != 0) {
                        return c;
                    }

                    c = e1.getValue().compareTo(e2.getValue());
                    if (c != 0) {
                        return c;
                    }
                }
                
                if (iter1.hasNext()) {
                    return 1;
                } else if (iter2.hasNext()) {
                    return -1;
                }
            }

            if (o1.isReference || o2.isReference) {
                Element subst1 = 
                    o1.isReference ? o1.getReferencedElement() : o1;
                Element subst2 = 
                    o2.isReference ? o2.getReferencedElement() : o2;
                    
                c = compare(subst1, subst2);
                if (c != 0) {
                    return c;
                }
            }
            
            if (o1.characters.length() > 0 || o2.characters.length() > 0) {
                c = o1.characters.toString().compareTo(
                    o2.characters.toString());
                if (c != 0) {
                    return c;
                }                
            }
            
            c = compare(o1.children, o2.children);
            if (c != 0) {
                return c;
            }
            
            if (o1.xmiId != null && o2.xmiId != null) {
                if (o1.isReference == o2.isReference) {
                    c = o1.xmiId.compareTo(o2.xmiId);
                    if (c != 0) {
                        return c;
                    }
                }
            }
            
            return 0;
        }
        
        private int compare(List<Element> o1children, List<Element> o2children)
        {
            Iterator<Element> o1iter = o1children.iterator();
            Iterator<Element> o2iter = o2children.iterator();
            
            while(o1iter.hasNext() && o2iter.hasNext()) {
                Element o1child = o1iter.next();
                Element o2child = o2iter.next();
                
                int c = compare(o1child, o2child);
                if (c != 0) {
                    return c;
                }
            }
            
            if (o1iter.hasNext()) {
                return 1;
            } else if (o2iter.hasNext()) {
                return -1;
            }
            
            return 0;
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
                root.sort();
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

    private static class IdRemapper
    {
        private Map<String, String> cache;
        private int nextId;
        
        IdRemapper()
        {
            this.cache = new HashMap<String, String>();
            this.nextId = 1;
        }
        
        public String remap(String id) 
        {
            String remappedId = cache.get(id);
            if (remappedId == null) {
                remappedId = "a" + nextId++;
                
                cache.put(id, remappedId);
            }
            
            return remappedId;
        }
    }
}
// End XmiFileComparator.java
