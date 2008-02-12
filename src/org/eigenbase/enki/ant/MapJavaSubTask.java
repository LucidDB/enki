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
package org.eigenbase.enki.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.netbeans.codegen.*;

/**
 * MapJavaSubTask generates code for a given metamodel.
 * 
 * <p>Attributes:
 * <table>
 * <tr>
 *   <th>Name</th>
 *   <th>Description</th>
 *   <th>Required?</th>
 * </tr>
 * <tr>
 *   <td>extent</td>
 *   <td>Extent into which the metamodel will be imported.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>file</td>
 *   <td>XMI metamodel that needs code generation</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>dir</td>
 *   <td>Directory into which code will be generated (sub directories will be
 *       created as necessary).</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>generatorClass</td>
 *   <td>Fully qualified name of the generator class to use.  For example,
 *       {@link HibernateGenerator} or {@link NetbeansGenerator}.</td>
 *   <td>Yes</td>
 * </tr>
 * <tr>
 *   <td>generatorOptions</td>
 *   <td>Generator-specific options.  Unknown options are ignored.  Format
 *       is name-value pairs separated by commas.  For example, 
 *       <blockquote><pre>option1=value1,option2=value2</pre></blockquote>
 *       If a value must contain an equals sign (=), comma (,) or single 
 *       quotes ('), place single quotes around the entire value.  Escape 
 *       single quotes in the value by doubling them, as in SQL.  For 
 *       example, this options string contains only two name-value pairs: 
 *       <blockquote><pre>opt1='a=b,c=d',opt2='''Hi'''</pre></blockquote>
 *       (The values are <code>a=b,c=d</code> and <code>'Hi'</code>.)</td>
 *   <td>No</td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class MapJavaSubTask extends EnkiTask.SubTask
{
    private static final char QUOTE = '\'';
    private static final char COMMA = ',';

    private String xmiFile;
    private String outputDir;
    private String extent;
    private String generatorClsName;
    private String generatorOptions;
    
    public MapJavaSubTask(String name)
    {
        super(name);
    }
    
    void execute() throws BuildException
    {
        if (xmiFile == null) {
            throw new BuildException("missing file attribute");
        }
        
        if (outputDir == null) {
            throw new BuildException("missing dir attribute");
        }
        
        if (extent == null) {
            throw new BuildException("missing extent attribute");
        }
        
        if (generatorClsName == null) {
            throw new BuildException("missing generatorClass attribute");
        }
        
        Map<String, String> generatorOpts = parseOptions();
        
        Class<? extends MdrGenerator> generatorCls;
        try {
            generatorCls = 
                Class.forName(generatorClsName).asSubclass(MdrGenerator.class);
        } catch (ClassNotFoundException e) {
            throw new BuildException(
                "Generator class must be a subclass of MdrGenerator", e);
        }
        
        MdrGenerator generator;
        try {
            generator = generatorCls.newInstance();
        } catch (Exception e) {
            throw new BuildException(e);
        }
        
        File xmi = new File(xmiFile);
        File dir = new File(outputDir);
        
        generator.setOutputDirectory(dir);
        generator.setXmiFile(xmi);
        generator.setExtentName(extent);
        generator.setUseGenerics(true);
        generator.setOptions(generatorOpts);
        
        try {
            generator.execute();
        } catch (GenerationException e) {
            throw new BuildException(e);
        }
    }
    
    private Map<String, String> parseOptions()
    {
        Map<String, String> options = new LinkedHashMap<String, String>();
        
        if (generatorOptions != null) {
            ParseState state = ParseState.FIND_OPTION_NAME;
            int pos = 0;
            String optionName = null;
            StringBuilder quotedValue = new StringBuilder();
            while(pos < generatorOptions.length()) {
                switch(state) {
                case FIND_OPTION_NAME:
                    int optionNameEnd = generatorOptions.indexOf('=', pos);
                    if (optionNameEnd <= pos) {
                        throw new BuildException(
                            "Invalid generator options: " 
                            + generatorOptions 
                            + "; cannot find option name starting at position "
                            + pos);                        
                    }
                    
                    optionName = 
                        generatorOptions.substring(pos, optionNameEnd);
                    pos = optionNameEnd + 1;
                    state = ParseState.START_VALUE;
                    break;
                    
                case START_VALUE:
                    if (generatorOptions.charAt(pos) == QUOTE) {
                        pos++;
                        state = ParseState.FIND_QUOTED_VALUE_END;
                        quotedValue.setLength(0);
                    } else {
                        state = ParseState.FIND_SIMPLE_VALUE_END;
                        continue;
                    }
                    break;
                    
                case FIND_SIMPLE_VALUE_END:
                    int valueEnd = generatorOptions.indexOf(COMMA, pos);
                    String value;
                    if (valueEnd < pos) {
                        value = generatorOptions.substring(pos);
                        pos = generatorOptions.length();
                    } else {
                        value = generatorOptions.substring(pos, valueEnd);
                        pos = valueEnd + 1;
                    }
                    
                    checkOptionName(optionName, pos);
                    
                    options.put(optionName, value);
                    state = ParseState.FIND_OPTION_NAME;
                    break;

                case FIND_QUOTED_VALUE_END:
                    char ch = generatorOptions.charAt(pos);
                    if (ch == QUOTE) {
                        if (pos + 1 < generatorOptions.length()) {
                            state = ParseState.RESOLVE_QUOTE;
                        } else {
                            // end of value
                            checkOptionName(optionName, pos);
                            options.put(optionName, quotedValue.toString());
                            state = ParseState.FIND_OPTION_NAME;
                        }
                    } else {
                        quotedValue.append(ch);
                    }
                    pos++;
                    break;
                
                case RESOLVE_QUOTE:
                    char rch = generatorOptions.charAt(pos);
                    if (rch == QUOTE) {
                        quotedValue.append(QUOTE);
                        pos++;
                    } else if (rch == COMMA) {
                        // found end of value                        
                        checkOptionName(optionName, pos);
                        
                        options.put(optionName, quotedValue.toString());
                        state = ParseState.FIND_OPTION_NAME;
                        pos += 2; // skip quote and comma
                    }
                    break;
                }
            }
            
            if (state != ParseState.FIND_OPTION_NAME) {
                throw new BuildException(
                    "Invalid generator options: "
                    + generatorOptions
                    + "; expected more value data for last option");
                    
            }
        }
        
        return options;
    }
    

    private void checkOptionName(String optionName, int pos)
    {
        if (optionName == null || 
            optionName.length() == 0)
        {
            throw new BuildException(
                "Invalid generator options: " 
                + generatorOptions 
                + "; value without option name at position "
                + pos);
        }
    }
        
    boolean isCombinableWith(EnkiTask.SubTask subTask)
    {
        // Can't combine since another task might instantiate the MDR 
        // subsystem with a different configuration.
        return false;
    }

    public void setFile(String xmiFile)
    {
        this.xmiFile = xmiFile;
    }
    
    public void setDir(String outputDir)
    {
        this.outputDir = outputDir;
    }

    public void setExtent(String extent)
    {
        this.extent = extent;
    }
    
    public void setGeneratorClass(String generatorClsName)
    {
        this.generatorClsName = generatorClsName;
    }
    
    public void setGeneratorOptions(String generatorOptions)
    {
        this.generatorOptions = generatorOptions;
    }
    
    private enum ParseState
    {
        FIND_OPTION_NAME,
        START_VALUE,
        FIND_SIMPLE_VALUE_END,
        FIND_QUOTED_VALUE_END,
        RESOLVE_QUOTE;
    }
}

// End MapJavaTask.java
