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

import java.io.*;

/**
 * AbstractCodeGenOutput implements the {@link CodeGenOutput} interface in
 * terms of a {@link PrintWriter}.  Subclasses should 
 * {@link #setOutput(PrintWriter) set the output PrintWriter} and the 
 * {@link #setEncoding(String) output's encoding}.
 * 
 * @author Stephan Zuercher
 */
public abstract class AbstractCodeGenOutput
    implements CodeGenOutput
{
    /** Print stream for the current output file. */
    private PrintWriter output;
    
    /** Current output encoding. */
    private String encoding;
    
    /** Current indent level. */
    protected int indentLevel;
    
    /** Flag indicating whether we're at the start of a line or not. */
    protected boolean startOfLine;

    protected boolean hasOutput()
    {
        return output != null;
    }
    
    protected void setOutput(PrintWriter output)
    {
        this.output = output;
    }

    protected void closeOutput()
    {
        output.flush();
        output.close();
        output = null;
    }
    
    protected void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
    
    public String getEncoding()
    {
        return encoding;
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
    public void write(Object... strings)
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
    public void writeln(Object... strings)
    {
        write(strings);
        output.println();
        startOfLine = true;
    }

    /**
     * Starts a new line of output.
     */
    public void newLine()
    {
        output.println();
        startOfLine = true;
    }

    /**
     * Emits {@link #INDENT} once for each indent level.
     */
    public void indent()
    {
        for(int i = 0; i < indentLevel; i++) {
            output.print(INDENT);
        }
    }

    /**
     * Returns the current indent level.
     * @return the current indent level.
     */
    public int getIndentLevel()
    {
        return indentLevel;
    }

    /**
     * Increases the current indent by one level.
     */
    public void increaseIndent()
    {
        indentLevel++;
    }

    /**
     * Decreases the indent by one level.  The indent cannot be reduced
     * below zero.
     */
    public void decreaseIndent()
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
    public void resetIndent()
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
     * space is emitted after the prefix.
     * 
     * @param prefix wrapped-line prefix (e.g., " *" for multi-line Java-style
     *               comments
     * @param strings zero of more objects to be converted to strings and
     *                wrapped 
     *                
     */
    public void writeWrapped(String prefix, Object... strings)
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

}

// End AbstractCodeGenOutput.java
