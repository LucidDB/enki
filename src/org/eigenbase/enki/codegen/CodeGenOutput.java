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

/**
 * CodeGenOutput represents a class capable of generating code to some output.
 * The output class supports indenting and wrapping.
 * 
 * @author Stephan Zuercher
 */
public interface CodeGenOutput
{
    /** Indent string. */
    public static final String INDENT = "    ";

    public static final String QUOTE = "\"";

    /** 
     * Default wrapping column for {@link #writeWrapped(String, Object...)}.
     */
    public static final int WRAP_WIDTH = 78;
    
    public String getEncoding();
    
    /**
     * Writes the given objects to the output as strings.  Presumes that
     * the result of each object's {@link Object#toString()} method is the 
     * desired output.  If the output is currently at the start of a line,
     * makes a call to {@link #indent()} before writing the strings.
     * 
     * @param strings objects to write to the output
     * @throws NullPointerException if any object in strings is null
     */
    public void write(Object... strings);

    /**
     * Writes the given objects to the output and starts a new line.
     * Equivalent to calling {@link #write(Object...)} followed by
     * {@link #newLine()}.
     * 
     * @param strings objects to convert to strings and write to the output
     */
    public void writeln(Object... strings);

    /**
     * Starts a new line of output.
     */
    public void newLine();

    /**
     * Emits {@link #INDENT} once for each indent level.
     */
    public void indent();

    /**
     * Returns the current indent level.
     * @return the current indent level.
     */
    public int getIndentLevel();

    /**
     * Increases the current indent by one level.
     */
    public void increaseIndent();

    /**
     * Decreases the indent by one level.  The indent cannot be reduced
     * below zero.
     */
    public void decreaseIndent();

    /**
     * Resets the indent level to 0.
     */
    public void resetIndent();

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
     * space is emitted after the prefix.
     * 
     * @param prefix wrapped-line prefix (e.g., " *" for multi-line Java-style
     *               comments
     * @param strings zero of more objects to be converted to strings and
     *                wrapped 
     *                
     */
    public void writeWrapped(String prefix, Object... strings);

}
// End CodeGenOutput.java
