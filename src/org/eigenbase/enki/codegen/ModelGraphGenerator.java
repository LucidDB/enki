/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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

import javax.jmi.model.*;

/**
 * ModelGraphGenerator creates a graph of the given metamodel's MofClass
 * inheritance hierarchy and then emits a file containing a list of vertices
 * and edges in the graph.  The vertices represent MofClasses described by
 * the metamodel and the edges represent super-to-sub-class relationships.
 * The file is in a format suitable for display with the graphing program
 * dotty.
 * 
 * @author Stephan Zuercher
 */
public class ModelGraphGenerator
    extends MdrGenerator
{
    @Override
    protected void configureHandlers()
    {
        ModelHandler handler = new ModelHandler();
        addHandler(handler);
    }
    
    public static void main(String[] args)
    {
        try {
            String xmiFileName = args[0];
            String outputDir = args[1];
            
            CodeGenUtils.setEnableGenerics(true);
            
            ModelGraphGenerator g = new ModelGraphGenerator();
            g.setXmiFile(new File(xmiFileName));
            g.setOutputDirectory(new File(outputDir));
            g.setExtentName(DEFAULT_ENKI_MODEL_EXTENT_NAME);
            g.execute();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static class Vertex
    {
        private final MofClass cls;
        private String desc;
        
        public Vertex(MofClass cls)
        {
            this.cls = cls;
            this.desc = null;
        }
        
        public int hashCode()
        {
            return cls.hashCode();
        }
        
        public boolean equals(Object other)
        {
            return cls.equals(((Vertex)other).cls);
        }
        
        public String toString()
        {
            if (desc == null) {
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
                
                if (cls.isAbstract()) {
                    buf.append('*');
                }                

                desc = buf.toString();
            }
            return desc;
        }
    }
    
    private static class Edge
    {
        private final Vertex source;
        private final Vertex target;
        
        public Edge(Vertex source, Vertex target)
        {
            this.source = source;
            this.target = target;
        }
        
        public String toString()
        {
            return 
                source.toString() + " -> " + target.toString();
        }
        
        public boolean equals(Object other)
        {
            return 
                source.equals(((Edge)other).source) &&
                target.equals(((Edge)other).target);
        }
        
        public int hashCode()
        {
            return source.hashCode() ^ target.hashCode();
        }
    }
    
    private static class ModelHandler implements ClassInstanceHandler
    {
        private File outputDir;
        
        private LinkedHashSet<Vertex> vertices =
            new LinkedHashSet<Vertex>();
        
        private LinkedHashSet<Edge> edges =
            new LinkedHashSet<Edge>();
        
        public void generateClassInstance(MofClass cls)
            throws GenerationException
        {
            Vertex target = new Vertex(cls);
            
            vertices.add(target);
            
            List<?> superTypes = cls.getSupertypes();
            for(Object o: superTypes) {
                MofClass superType = (MofClass)o;
                
                Vertex source = new Vertex(superType);
                vertices.add(source);
                
                Edge edge = new Edge(source, target);
                edges.add(edge);
            }
        }
        
        public int getNumPasses()
        {
            return 1;
        }

        public void beginGeneration() throws GenerationException
        {
        }

        public void beginPass(int passIndex)
        {
        }
        
        public void endPass(int passIndex)
        {    
        }
        
        public void endGeneration(boolean throwing)
            throws GenerationException
        {
            if (vertices.isEmpty() && edges.isEmpty()) {
                return;
            }
            
            try {
                File file = new File(outputDir, "model.graph");
                BufferedWriter w = new BufferedWriter(new FileWriter(file));
                
                w.write(String.valueOf(vertices.size()));
                w.newLine();
                for(Vertex v: vertices) {
                    w.write(v.toString());
                    w.newLine();
                }
                
                w.write(String.valueOf(edges.size()));
                w.newLine();
                for(Edge e: edges) {
                    w.write(e.toString());
                    w.newLine();
                }
                
                w.close();
            }
            catch(Throwable t) {
                throw new GenerationException(t);
            }
        }

        public void setGenerator(Generator generator)
        {
        }

        public void setOutputDir(File outputDir)
        {
            this.outputDir = outputDir;
        }
        
    }
}

// End ModelGraphGenerator.java
