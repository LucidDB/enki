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
public abstract class HandlerBase
    extends AbstractCodeGenOutput
    implements Handler, CodeGenOutput
{
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected Generator generator;
    
    /** Output directory. */
    protected File outputDir;
    
    /** Current output file. */
    protected File currentFile;
    
    /** List of included packages. */
    private Set<String> includedPackages;
    
    /** Extra information to print at the start of each file. */
    protected String commonHeader;

    /** {@link #close()} can store an exception here to be thrown later. */
    private GenerationException pendingEx;
    
    /** The current pass index. */
    private int passIndex;
    
    private final Logger log = Logger.getLogger(HandlerBase.class.getName());

    /**
     * Flag indicating if "plug-in mode" is enabled.
     */
    protected boolean pluginMode;

    public HandlerBase()
    {
        this.passIndex = -1;
        this.includedPackages = Collections.emptySet();
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

    /**
     * Configures the set of packages explicitly included for generation.
     * If un-called the Handler defaults to including all packages.  Packages
     * are passed by name, in Java convention (e.g. "eem.sample.special").
     * 
     * @param includedPackagesInit collection of packages to include 
     *                             (null means include all)
     */
    public void setIncludes(Collection<String> includedPackagesInit)
    {
        if (includedPackagesInit != null && !includedPackagesInit.isEmpty()) {
            this.includedPackages = new TreeSet<String>(includedPackagesInit);
        } else {
            this.includedPackages = Collections.emptySet();
        }
    }
    
    public void setPluginMode(boolean pluginMode)
    {
        this.pluginMode = pluginMode;
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
    public void beginPass(int newPassIndex) throws GenerationException
    {
        if (newPassIndex >= getNumPasses()) {
            throw new GenerationException(
                "Invalid internal state: too many passes");
        }
        this.passIndex = newPassIndex;
    }
    
    // Implement Handler
    public void endPass(int endingPassIndex) throws GenerationException
    {
        if (this.passIndex != endingPassIndex) {
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
    
    protected boolean isIncluded(ModelElement elem)
    {
        if (includedPackages.isEmpty()) {
            return true;
        }
        
        String typeName = CodeGenUtils.getTypeName(elem);
        
        int lastDot;
        while((lastDot = typeName.lastIndexOf('.')) > 0) {
            typeName = typeName.substring(0, lastDot);
            
            if (includedPackages.contains(typeName)) {
                return true;
            }
        }
        
        return false;
    }
    
    protected Collection<String> getIncludedPackages()
    {
        return Collections.unmodifiableCollection(includedPackages);
    }
    
    protected void open(File file) throws GenerationException
    {
        open(file, "UTF-8");
    }
    
    protected void open(File file, String encoding) throws GenerationException
    {
        if (hasOutput()) {
            throw new GenerationException(
                "internal: previous file was not closed");
        }
        
        checkPendingException();
        
        currentFile = file;
        try {
            Writer writer = 
                new OutputStreamWriter(
                    new FileOutputStream(currentFile), encoding);
            setEncoding(encoding);
            setOutput(new PrintWriter(writer, false));
            
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
        if (hasOutput()) {
            if (getIndentLevel() != 0 && pendingEx == null) {
                pendingEx = 
                    new GenerationException(
                        "Unbalanced indents in '" + currentFile + "'");
            }
            resetIndent();
            
            closeOutput();

            log.fine("Closed '" + currentFile.toString() + "'");
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
            result[i++] = CodeGenUtils.getTypeName(entity, suffix);
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
            result[i++] = CodeGenUtils.getTypeName(entity, suffix);
        }
        return result;
    }    
}
