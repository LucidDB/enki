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

import java.io.*;
import java.text.*;
import java.util.*;

import javax.jmi.model.*;

import org.eigenbase.enki.util.*;

/**
 * HandlerBase is an abstract class that provides common functionality for
 * implementations of {@link Handler} and its sub-interfaces.
 * 
 * <p>Various methods in this class format comments.  Formatting is 
 * achieved via {@link MessageFormat} and the methods usually provide one or
 * two fields implicitly and allow the caller to specify more.  Implicit
 * fields always start at 0.  So for instance, calling
 * 
 * <pre>
 *     writeClassJavaDoc(c, "The class {0} has a comment.");
 * </pre>
 * 
 * for a {@link MofClass} whose name is "SimpleClass" generates a JavaDoc 
 * comment like this (with asterisks in place of x):
 * 
 * <pre>
 *     /xx
 *      x The class SimpleClass has a comment.
 *      x/
 * </pre>
 * 
 * <table>
 * <tr>
 * <th>Method</th><th>Implicit Format Parameters</th>
 * </tr>
 * <tr>
 * <td>writeClassJavaDoc</td><td>Class Name</td><
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public abstract class HandlerBase implements Handler
{
    /** Indent string. */
    public static final String INDENT = "    ";
    
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String QUOTE = "\"";
    
    /** 
     * Default wrapping column for {@link #writeWrapped(String, String...)}.
     */
    protected static final int WRAP_WIDTH = 78;
    
    protected Generator generator;
    
    /** Output directory. */
    protected File outputDir;
    
    /** Current output file. */
    protected File currentFile;
    
    /** Print stream for the current output file. */
    private PrintStream output;
    
    /** Current output encoding. */
    protected String encoding;
    
    /** Current indent level. */
    private int indentLevel;
    
    /** Extra information to print at the start of each file. */
    protected String commonHeader;

    /** Flag indicating whether we're at the start of a line or not. */
    private boolean startOfLine;
    
    /** {@link #close()} can store an exception here to be thrown later. */
    private GenerationException pendingEx;
    
    public HandlerBase()
    {
    }

    public void setGenerator(Generator generator)
    {
        this.generator = generator;
    }
    
    /**
     * Configure the output directory.  Called automatically by 
     * {@link Generator#addHandler(Handler)}.  Do not call 
     * this method on an instance of {@link HandlerBase} that has
     * already been added to a {@link Generator}.
     * 
     * @param outputDir directory in which to store output files.
     */
    public void setOutputDir(File outputDir)
    {
        this.outputDir = outputDir;
    }

    // Implement Handler
    public void beginGeneration() throws GenerationException
    {
    }
    
    // Implement Handler
    public void endGeneration(boolean throwing) throws GenerationException
    {
        if (!throwing) {
            checkPendingException();
        }
    }
    
    protected void open(File file) throws GenerationException
    {
        open(file, "UTF-8");
    }
    
    protected void open(File file, String encoding) throws GenerationException
    {
        if (output != null) {
            throw new GenerationException(
                "internal: previous file was not closed");
        }
        
        checkPendingException();
        
        currentFile = file;
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(currentFile);
            this.encoding = encoding;
            output = new PrintStream(stream, false, encoding);
        } catch (IOException e) {
            throw new GenerationException(e);
        }
        
        indentLevel = 0;
        startOfLine = true;
    }

    private void checkPendingException()
        throws GenerationException
    {
        if (pendingEx != null) {
            GenerationException ex = pendingEx;
            pendingEx = null;
            throw ex;
        }
    }
    
    /**
     * Close the current open file.  Closes the file opened by
     * {@link #open(String)}.  This method may store an exception
     * in {@link #pendingEx} which will be thrown by the next call to
     * {@link #open(String)}.  In particular, this method checks for
     * unbalanced calls to {@link #increaseIndent()} and 
     * {@link #decreaseIndent()}.
     */
    protected void close() throws GenerationException
    {
        if (output != null) {
            if (indentLevel != 0 && pendingEx == null) {
                pendingEx = 
                    new GenerationException(
                        "Unbalanced indents in '" + currentFile + "'");
            }
            resetIndent();
            
            output.close();
            output = null;
        }
    }
    
    
    
    /**
     * Sets a common header to be written at the start of every entity.
     * The header data may contain embedded new-lines, but should not 
     * contain comment characters.  Comment characters and wrapping
     * occurs automatically.
     * 
     * @param header header data
     */
    protected void setCommonHeader(String header)
    {
        commonHeader = header;
    }
    
    protected String listToString(String prefix, String... items)
    {
        return listToString(prefix, 1, items);
    }
    
    protected String listToString(String prefix, int concatEachN, String... items)
    {
        StringBuilder buffer = new StringBuilder();
        for(int i = 0, n = 0; i < items.length; i++) {
            String item = items[i];
            
            if (buffer.length() > 0) {
                if (n % concatEachN == 0) {
                    buffer.append(", ");
                    n = 0;
                } else {
                    buffer.append(" ");
                }
            } else {
                buffer.append(prefix);
            }
            
            buffer.append(item);
            n++;
        }
        return buffer.toString();
    }
    
    protected String[] listToStrings(
        List<ModelElement> entities, 
        String suffix)
    {
        String[] result = new String[entities.size()];
        int i = 0;
        for(ModelElement entity: entities) {
            result[i++] = generator.getTypeName(entity, suffix);
        }
        return result;
    }
    
    protected <E extends ModelElement> String[] listToStrings(
        List<?> entities,
        Class<E> cls,
        String suffix)
    {
        String[] result = new String[entities.size()];
        int i = 0;
        for(E entity: GenericCollections.asTypedList(entities, cls)) {
            result[i++] = generator.getTypeName(entity, suffix);
        }
        return result;
    }
    
    protected void write(Object... strings)
    {
        if (startOfLine) {
            indent();
            startOfLine = false;
        }

        for(Object s: strings) {
            if (s == null) {
                throw new NullPointerException();
            }
            
            output.print(s.toString());
        }
    }
    
    protected void writeln(Object... strings)
    {
        write(strings);
        output.println();
        startOfLine = true;
    }
    
    protected void newLine()
    {
        output.println();
        startOfLine = true;
    }
    
    protected void indent()
    {
        for(int i = 0; i < indentLevel; i++) {
            output.print(INDENT);
        }
    }
    
    protected int getIndentLevel()
    {
        return indentLevel;
    }
    
    protected void increaseIndent()
    {
        indentLevel++;
    }
    
    protected void decreaseIndent()
    {
        indentLevel--;
        if (indentLevel < 0) {
            assert(false);
            indentLevel = 0;
        }
    }
    
    protected void resetIndent()
    {
        indentLevel = 0;
    }
    
    protected void writeWrapped(String prefix, Object... strings)
    {
        StringBuilder buffer = new StringBuilder();
        for(Object s: strings) {
            buffer.append(s.toString());
        }
        
        String s = buffer.toString();
        if (s.length() == 0) {
            return;
        }
        
        String[] lines = s.split("\\r*\\n");

        final int wrapWidth = 
            WRAP_WIDTH - (INDENT.length() * indentLevel) - prefix.length() - 1;
        
        int consecutiveBlank = 0;
        buffer.setLength(0);
        for(String line: lines) {
            StringBuilder indent = new StringBuilder();
            int indentEnd = 0;
            while(indentEnd < line.length())
            {
                char ch = line.charAt(indentEnd);
                if (ch != '\t' && ch != ' ') {
                    break;
                }

                indentEnd++;
                if (ch == '\t') {
                    indent.append(INDENT);
                } else {
                    indent.append(' ');
                }
            }
            line = line.trim();
            
            if (line.length() == 0) {
                if (consecutiveBlank < 1) {
                    writeln(prefix);
                    consecutiveBlank++;
                }
                continue;
            }
            consecutiveBlank = 0;
            
            String[] words = line.split(" +");

            if (indent.length() > 0) {
                buffer.append(indent);
            } else {
                buffer.append(' ');
            }
            boolean justIndent = true;
            for(String word: words) {
                int len = buffer.length();

                if (!justIndent) {
                    if (len + word.length() >= wrapWidth) {
                        writeln(prefix, buffer.toString());
                        buffer.setLength(0);
                        len = 0;
                    }

                    buffer.append(' ');
                }
                buffer.append(word);
                justIndent = false;
            }
            if (buffer.length() > 0) {
                writeln(prefix, buffer.toString());
                buffer.setLength(0);
            }
        }
    }
    
    /**
     * Iterates over contents of the entity and returns a collection 
     * containing all contents of the given type.  Objects of any scope and 
     * visibility are returned.  Super types are not searched.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    protected <E> Collection<E> contentsOfType(
        GeneralizableElement entity, Class<E> cls)
    {
        return contentsOfType(
            entity, 
            HierachySearchKindEnum.ENTITY_ONLY,
            null, 
            null,
            cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type.
     * Objects of any scope and visibility are returned.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    protected <E> Collection<E> contentsOfType(
        GeneralizableElement entity,
        HierachySearchKindEnum search, 
        Class<E> cls)
    {
        return contentsOfType(entity, search, null, null, cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type
     * with the given scope.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param scope content scope
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    protected <E> Collection<E> contentsOfType(
        GeneralizableElement entity, 
        HierachySearchKindEnum search, 
        ScopeKind scope,
        Class<E> cls)
    {
        return contentsOfType(entity, search, null, scope, cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type
     * with the given visibility.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param visibility content visibility
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    protected <E> Collection<E> contentsOfType(
        GeneralizableElement entity, 
        HierachySearchKindEnum search, 
        VisibilityKind visibility, 
        Class<E> cls)
    {
        return contentsOfType(entity, search, visibility, null, cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type
     * with the given visibility and scope.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param visibility content visibility
     * @param scope content scope
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    protected <E> Collection<E> contentsOfType(
        GeneralizableElement entity,
        HierachySearchKindEnum search, 
        VisibilityKind visibility,
        ScopeKind scope,
        Class<E> cls)
    {
        // LinkedHashSet prevents duplicate entries and preserves insertion
        // order as the iteration order, which are both desired here.
        LinkedHashSet<E> result = new LinkedHashSet<E>();
        
        if (search == HierachySearchKindEnum.INCLUDE_SUPERTYPES) {
            for(Namespace namespace: 
                    GenericCollections.asTypedList(
                        entity.allSupertypes(), Namespace.class))
            {
                result.addAll(
                    contentsOfType(namespace, visibility, scope, cls));
            }
        }
        
        result.addAll(
            contentsOfType(entity, visibility, scope, cls));

        return result;
    }

    protected <E> Collection<E> contentsOfType(
        Namespace namespace,
        VisibilityKind visibility,
        ScopeKind scope,
        Class<E> cls)
    {
        LinkedHashSet<E> result = new LinkedHashSet<E>();

        for(Object o: namespace.getContents()) {
            if (cls.isInstance(o)) {
                if (visibility != null && 
                    !((Feature)o).getVisibility().equals(visibility))
                {
                    continue;
                }
                
                if (scope != null &&
                    !((Feature)o).getScope().equals(scope))
                {
                    continue;
                }

                result.add(cls.cast(o));
            }
        }
        
        return result;
    }
    
    protected static String toInitialLower(String str)
    {
        StringBuilder buf = new StringBuilder();
        if (str == null) {
            return null;
        }
        
        int len = str.length();
        if (len > 0) {
            buf.append(Character.toLowerCase(str.charAt(0)));
            if (len > 1) {
                buf.append(str.substring(1));
            }
        }
        
        return buf.toString();
    }

    protected static enum HierachySearchKindEnum
    {
        ENTITY_ONLY,
        INCLUDE_SUPERTYPES;
    }    
}
