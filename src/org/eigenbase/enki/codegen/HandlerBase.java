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
import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;

import org.eigenbase.enki.util.*;

/**
 * HandlerBase is an abstract class that provides common functionality for
 * implementations of {@link Handler} and its sub-interfaces.
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
     * Default wrapping column for {@link #writeWrapped(String, Object...)}.
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
    
    /** The current pass index. */
    private int passIndex;
    
    private final Logger log = Logger.getLogger(HandlerBase.class.getName());

    public HandlerBase()
    {
        this.passIndex = -1;
    }

    public void setGenerator(Generator generator)
    {
        this.generator = generator;
    }
    
    /**
     * Configures the output directory.  Called automatically by 
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
    
    // Implement Handler
    public int getNumPasses()
    {
        return 1;
    }
    
    // Implement Handler
    public void beginPass(int passIndex) throws GenerationException
    {
        if (passIndex >= getNumPasses()) {
            throw new GenerationException(
                "Invalid internal state: too many passes");
        }
        this.passIndex = passIndex;
    }
    
    // Implement Handler
    public void endPass(int passIndex) throws GenerationException
    {
        if (this.passIndex != passIndex) {
            throw new GenerationException(
                "Invalid internal state: passIndex mismatch");
        }
        
        this.passIndex = -1;
    }
    
    protected int getPassIndex() throws GenerationException
    {
        if (this.passIndex == -1) {
            throw new GenerationException(
                "Invalid internal state: passIndex == -1");
        }
        
        return passIndex;
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
            
            log.fine("Opened '" + currentFile.toString() + "'");
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
     * Closes the current open file.  Closes the file opened by
     * {@link #open(File)} or {@link #open(File, String)}.  This method may 
     * store an exception in {@link #pendingEx} which will be thrown by the 
     * next call to either open method.  In particular, this method checks for
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

            log.fine("Closeed '" + currentFile.toString() + "'");
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
    
    /**
     * Writes the given objects to the output as strings.  Presumes that
     * the result of each object's {@link #toString()} method is the 
     * desired output.  If the output is currently at the start of a line,
     * makes a call to {@link #indent()} before writing the strings.
     * 
     * @param strings objects to write to the output
     * @throws NullPointerException if any object in strings is null
     */
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
    
    /**
     * Writes the given objects to the output and starts a new line.
     * Equivalent to calling {@link #write(Object...)} followed by
     * {@link #newLine()}.
     * 
     * @param strings objects to conver to strings and write to the output
     */
    protected void writeln(Object... strings)
    {
        write(strings);
        output.println();
        startOfLine = true;
    }
    
    /**
     * Starts a new line of output.
     */
    protected void newLine()
    {
        output.println();
        startOfLine = true;
    }
    
    /**
     * Emits {@link #INDENT} once for each indent level.
     */
    protected void indent()
    {
        for(int i = 0; i < indentLevel; i++) {
            output.print(INDENT);
        }
    }
    
    /**
     * Returns the current indent level.
     * @return the current indent level.
     */
    protected int getIndentLevel()
    {
        return indentLevel;
    }
    
    /**
     * Increases the current indent by one level.
     */
    protected void increaseIndent()
    {
        indentLevel++;
    }
    
    /**
     * Decreases the indent by one level.  The indent cannot be reduced
     * below zero.
     */
    protected void decreaseIndent()
    {
        indentLevel--;
        if (indentLevel < 0) {
            assert(false);
            indentLevel = 0;
        }
    }
    
    /**
     * Resets the indent level to 0.
     */
    protected void resetIndent()
    {
        indentLevel = 0;
    }
    
    /**
     * Writes the given objects as strings wrapping long lines.  Wrapping is
     * achieved by converting the given objects to strings and concatenating
     * them.  The result is then split into lines if there are any embedded
     * new line characters.  Each line is then wrapped to a width of
     * {@link #WRAP_WIDTH}, taking into account the given prefix and current
     * indent level.
     * 
     * <p>This method converts consecutive spaces into single spaces.  It 
     * converts consecutive blank lines (e.g., multiple new lines separated
     * with nothing but tabs or spaces) into a single blank line.  A blank
     * space is emitted after the prfix.
     * 
     * @param prefix wrapped-line prefix (e.g., " *" for multi-line Java-style
     *               comments
     * @param strings zero of more objects to be converted to strings and
     *                wrapped 
     *                
     */
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
            
            // Put beginning-of-line indent into indent and trim it from line.
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
            
            // Collapse consecutive blank lines.
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
                // prefix is written directly, but we place a space after it
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

    /**
     * Iterates over contents of the namespace and returns a collection 
     * containing all contents of the given type with the given visibility and
     * scope.
     * 
     * @param <E> content type
     * @param namespace namespace to search over
     * @param visibility content visibility
     * @param scope content scope
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    protected <E> Collection<E> contentsOfType(
        Namespace namespace,
        VisibilityKind visibility,
        ScopeKind scope,
        Class<E> cls)
    {
        LinkedHashSet<E> result = new LinkedHashSet<E>();

        for(Object o: namespace.getContents()) {
            if (!cls.isInstance(o)) {
                logDiscard(
                    namespace, 
                    visibility, 
                    scope, 
                    cls, 
                    "wrong type", 
                    o.getClass().getName());
                continue;
            }
            
            if (visibility != null && 
                !((Feature)o).getVisibility().equals(visibility))
            {
                logDiscard(
                    namespace, 
                    visibility, 
                    scope, 
                    cls,
                    "wrong visibility", 
                    ((Feature)o).getVisibility().toString());
                continue;
            }
            
            if (scope != null &&
                !((Feature)o).getScope().equals(scope))
            {
                logDiscard(
                    namespace, 
                    visibility, 
                    scope, 
                    cls,
                    "wrong scope", 
                    ((Feature)o).getScope().toString());
                continue;
            }

            logAccept(namespace, visibility, scope, cls);

            result.add(cls.cast(o));
        }
        
        return result;
    }

    private <E> void logAccept(
        Namespace namespace,
        VisibilityKind visibility,
        ScopeKind scope,
        Class<E> cls)
    {
        if (!log.isLoggable(Level.FINEST)) {
            return;
        }

        log.finest(
            "contentsOfType(" +
            namespace.getName() + ": " +
            (visibility == null ? "<any-vis>" : visibility.toString()) + ", " +
            (scope == null ? "<any-scope>" : scope.toString()) + ", " +
            cls.getName() + "): ok");
    }
    
    private void logDiscard(
        Namespace namespace, 
        VisibilityKind visibility, 
        ScopeKind scope, 
        Class<?> cls, 
        String desc, 
        String value)
    {
        if (!log.isLoggable(Level.FINEST)) {
            return;
        }
        
        log.finest(
            "contentsOfType(" +
            namespace.getName() + ": " +
            (visibility == null ? "<any-vis>" : visibility.toString()) + ", " +
            (scope == null ? "<any-scope>" : scope.toString()) + ", " +
            cls.getName() + "): " + desc + ": " + value);
    }
    
    /**
     * HierarchySearchKindEnum is used as a flag to control methods that may
     * either search only a given entity or the entity and its super types.
     */
    protected static enum HierachySearchKindEnum
    {
        /** Search only the given entity, ignoring super types. */
        ENTITY_ONLY,
        
        /** Search the given entity, followed by all of its super types. */
        INCLUDE_SUPERTYPES;
    }    
}
