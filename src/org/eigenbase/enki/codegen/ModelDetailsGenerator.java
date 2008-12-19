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

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Handler;

import javax.jmi.model.*;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

/**
 * ModelDetailsGenerator produces a text file information about an XMI-based
 * metamodel.
 * 
 * Arguments to {@link #main(String[])} are:
 * <ol>
 *   <li>XMI filename</li>
 *   <li>Output directory for "model.details" file</li>
 *   <li>Boolean flag indicating whether to include ("true") or exclude 
 *       ("false") transient packages.</li>
 *   <li>Boolean flag indicating whether to include ("true") or exclude
 *       ("false") classes that have no attributes after filtering</li>
 *   <li>Remaining arguments are a case-insensitive list of attribute types
 *       to display.  Optional.  If unspecified, all attributes are 
 *       displayed.</li>
 * </ol>
 * 
 * Output contains an alphabetized list of {@link MofClass} instances in the
 * model, prefixed with their package names.  Each MofClass contains a list
 * of attribute names, with model type and Enki storage information.  Finally,
 * each subclass of the MofClass is listed.
 * 
 * @author Stephan Zuercher
 */
public class ModelDetailsGenerator extends ModelGraphGenerator
{
    private static Set<String> showAttribsOfTypeParam;
    private static boolean showAttribFreeClassesParam;
    private static boolean showTransientClassesParam;

    @Override
    protected void configureHandlers()
    {
        ModelHandler handler = new ModelHandler();
        handler.setShowAttribsOfType(showAttribsOfTypeParam);
        handler.setShowAttribFreeClasses(showAttribFreeClassesParam);
        handler.setShowTransientClasses(showTransientClassesParam);
        addHandler(handler);
    }
    
    public static void main(String[] args)
    {
        try {
            int i = 0;
            String xmiFileName = args[i++];
            String outputDir = args[i++];
            showTransientClassesParam = Boolean.valueOf(args[i++]);
            showAttribFreeClassesParam = Boolean.valueOf(args[i++]);
            showAttribsOfTypeParam = new HashSet<String>();
            for( ; i < args.length; i++) {
                showAttribsOfTypeParam.add(args[i]);
            }
            if (showAttribsOfTypeParam.isEmpty()) {
                showAttribsOfTypeParam = null;
            }
            
            ModelDetailsGenerator g = new ModelDetailsGenerator();
            g.setXmiFile(new File(xmiFileName));
            g.setOutputDirectory(new File(outputDir));
            g.setExtentName(DEFAULT_ENKI_MODEL_EXTENT_NAME);
            g.setUseGenerics(true);
            g.execute();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private static class ModelHandler implements ClassInstanceHandler
    {
        private File outputDir;
        
        private DirectedGraph<MofClass, DefaultEdge> graph =
            new DefaultDirectedGraph<MofClass, DefaultEdge>(DefaultEdge.class);

        private Set<String> showAttribsOfType;
        private boolean showAttribFreeClasses;
        private boolean showTransientClasses;
        
        public void generateClassInstance(MofClass cls)
            throws GenerationException
        {
            if (!showTransientClasses && CodeGenUtils.isTransient(cls)) {
                return;
            }
                
            if (!graph.containsVertex(cls)) {
                graph.addVertex(cls);
            }
            
            List<?> superTypes = cls.getSupertypes();
            for(Object o: superTypes) {
                MofClass superType = (MofClass)o;
                
                if (!graph.containsVertex(superType)) {
                    graph.addVertex(superType);
                }
                
                graph.addEdge(superType, cls);
            }
        }

        public void beginGeneration()
            throws GenerationException
        {
        }

        public void beginPass(int passIndex)
            throws GenerationException
        {
        }

        public void endGeneration(boolean throwing)
            throws GenerationException
        {
            List<MofClass> vertices = 
                new ArrayList<MofClass>(graph.vertexSet());
            Collections.sort(vertices, new Comparator<MofClass>() {
                public int compare(MofClass o1, MofClass o2)
                {
                    return makeDesc(o1, false).compareTo(makeDesc(o2, false));
                }
            });
            
            Logger log = Logger.getLogger("ModelDetailsGenerator");
            Handler[] handlers = log.getHandlers();
            for(Handler handler: handlers) {
                log.removeHandler(handler);
            }
            
            try {
                File file = new File(outputDir, "model.details");
                final BufferedWriter w = 
                    new BufferedWriter(new FileWriter(file));

                Handler handler = new Handler() {
                    private final BufferedWriter wrtr = w;
                    @Override
                    public void close() throws SecurityException
                    {
                    }

                    @Override
                    public void flush()
                    {
                    }

                    @Override
                    public void publish(LogRecord record)
                    {
                        try {
                            wrtr.write(record.getLevel().getName());
                            wrtr.write(": ");
                            wrtr.write(record.getMessage());
                            wrtr.newLine();
                        }
                        catch(IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                log.addHandler(handler);
                
                for(MofClass cls: vertices) {
                    LinkedHashMap<Attribute, String> attribs = 
                        new LinkedHashMap<Attribute, String>();
                    for(Object o: cls.getContents()) {
                        if (!(o instanceof Attribute)) {
                            continue;
                        }
                        Attribute attrib = (Attribute)o;
                        Classifier type = attrib.getType();
                        if (type instanceof AliasType) {
                            type = ((AliasType)type).getType();
                        }
                        
                        boolean showAttrib = false;
                        if (showAttribsOfType == null) {
                            showAttrib = true;
                        } else {
                            for(String s: showAttribsOfType) {
                                if (s.equalsIgnoreCase(type.getName())) {
                                    showAttrib = true;
                                    break;
                                }
                            }
                        }
                        
                        if (showAttrib) {
                            if (type instanceof MofClass) {
                                attribs.put(attrib, makeDesc((MofClass)type));
                            } else {
                                attribs.put(attrib, type.getName());
                            }
                        }
                    }
                    
                    if (!showAttribFreeClasses && attribs.isEmpty()) {
                        continue;
                    }
                    
                    String desc = makeDesc(cls);
                    
                    w.write(desc);
                    w.newLine();
                    
                    w.write("\tAttributes:");
                    w.newLine();
                    for(Map.Entry<Attribute, String> entry: attribs.entrySet())
                    {
                        w.write("\t\t");
                        Attribute attrib = entry.getKey();
                        w.write(attrib.getName());
                        w.write(": ");
                        w.write(entry.getValue());
                        
                        int len = 
                            CodeGenUtils.findMaxLengthTag(
                                cls, 
                                attrib, 
                                CodeGenUtils.DEFAULT_STRING_LENGTH, 
                                log);
                        
                        if (len == Integer.MAX_VALUE) {
                            w.write(" (max length: unlimited)");
                        } else {
                            w.write(" (max length: " + len + ")");
                        }
                        
                        w.newLine();
                    }

                    w.write("\tSubclasses:");
                    w.newLine();
                    BreadthFirstIterator<MofClass, DefaultEdge> iter =
                        new BreadthFirstIterator<MofClass, DefaultEdge>(
                            graph, cls);
                    iter.next();  // skip cls itself
                    while(iter.hasNext()) {
                        MofClass subType = iter.next();
                        
                        w.write("\t\t");
                        w.write(makeDesc(subType));
                        w.newLine();

                        for(Attribute attrib: attribs.keySet()) {
                            int baseLen = 
                                CodeGenUtils.findMaxLengthTag(
                                    cls, 
                                    attrib, 
                                    CodeGenUtils.DEFAULT_STRING_LENGTH, 
                                    log);
                            
                            int thisLen =
                                CodeGenUtils.findMaxLengthTag(
                                    subType, 
                                    attrib, 
                                    CodeGenUtils.DEFAULT_STRING_LENGTH, 
                                    log);
                            
                            if (baseLen != thisLen) {
                                w.write("\t\t\t(overrides '");
                                w.write(attrib.getName());
                                w.write("' max length: ");
                                w.write(String.valueOf(thisLen));
                                w.write(')');
                                w.newLine();
                            }
                        }
                    }
                    w.newLine();
                }
                
                w.close();
            }
            catch(Throwable t) {
                throw new GenerationException(t);
            }
        }
            
        private String makeDesc(MofClass cls)
        {
            return makeDesc(cls, true);
        }
        
        private String makeDesc(MofClass cls, boolean showAbstract)
        {
            ArrayList<String> namespaces = new ArrayList<String>();
            Namespace ns = cls;
            do {
                namespaces.add(0, ns.getName());
            } while((ns = ns.getContainer()) != null);
            
            StringBuffer buf = new StringBuffer();
            for(String namespace: namespaces) {
                if (buf.length( )!= 0) {
                    buf.append('.');
                }
                buf.append(namespace);
            }
            
            if (showAbstract && cls.isAbstract()) {
                buf.append(" [abstract]");
            }                

            return buf.toString();
        }

        public void endPass(int passIndex)
            throws GenerationException
        {
        }

        public int getNumPasses()
        {
            return 1;
        }

        public void setGenerator(Generator generator)
        {
        }

        public void setOutputDir(File outputDir)
        {
            this.outputDir = outputDir;
        }
        
        public void setShowAttribsOfType(Set<String> showAttribsOfType)
        {
            this.showAttribsOfType = showAttribsOfType;
        }
        
        public void setShowAttribFreeClasses(boolean showAttribFreeClasses)
        {
            this.showAttribFreeClasses = showAttribFreeClasses;
        }
        
        public void setShowTransientClasses(boolean showTransientClasses)
        {
            this.showTransientClasses = showTransientClasses;
        }
    }
}

// End ModelDetailsGenerator.java
