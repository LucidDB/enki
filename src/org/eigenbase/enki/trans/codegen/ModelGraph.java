/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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
package org.eigenbase.enki.trans.codegen;

import java.util.*;

import javax.jmi.model.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.util.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;

/**
 * ModelGraph builds a JGraphT model of a MOF model's inheritance hierarchy,
 * associations, and non-primitive attributes.
 * 
 * @author Stephan Zuercher
 */
public class ModelGraph
{
    // REVIEW: SWZ: 2009-01-13: Eventually we can migrate Farrago's model
    // graph to Enki and replace this (although Farrago's implementation
    // doesn't support the attributes).
    
    // REVIEW: SWZ: 2009-01-13: Increase usage of this class throughout Enki's
    // code generation.
    
    private final ModelPackage modelPkg;
    
    private final DirectedGraph<ClassVertex, InheritanceEdge> inheritanceGraph;
    private final UnmodifiableDirectedGraph<ClassVertex, InheritanceEdge>
        unmodifiableInheritanceGraph;
    
    private final DirectedGraph<ClassVertex, AssociationEdge> associationGraph;
    private final UnmodifiableDirectedGraph<ClassVertex, AssociationEdge>
        unmodifiableAssociationGraph;
    
    private final DirectedGraph<ClassVertex, AttributeEdge> attributeGraph;
    private final UnmodifiableDirectedGraph<ClassVertex, AttributeEdge>
        unmodifiableAttributeGraph;
    
    private final Map<MofClass, ClassVertex> vertexMap;
    
    private final Map<Association, AssociationEdge> assocEdgeMap;
    
    public ModelGraph(ModelPackage modelPkg)
    {
        this.modelPkg = modelPkg;
        
        this.inheritanceGraph = 
            new DirectedMultigraph<ClassVertex, InheritanceEdge>(
                InheritanceEdge.class);
        this.unmodifiableInheritanceGraph = 
            new UnmodifiableDirectedGraph<ClassVertex, InheritanceEdge>(
                inheritanceGraph);
        
        this.associationGraph =
            new DirectedMultigraph<ClassVertex, AssociationEdge>(
                AssociationEdge.class);
        this.unmodifiableAssociationGraph =
            new UnmodifiableDirectedGraph<ClassVertex, AssociationEdge>(
                associationGraph);
        
        this.attributeGraph =
            new DirectedMultigraph<ClassVertex, AttributeEdge>(
                AttributeEdge.class);
        this.unmodifiableAttributeGraph = 
            new UnmodifiableDirectedGraph<ClassVertex, AttributeEdge>(
                attributeGraph);
        
        this.vertexMap = new HashMap<MofClass, ClassVertex>();
        this.assocEdgeMap = new HashMap<Association, AssociationEdge>();
        
        for(MofPackage mofPackage:
                GenericCollections.asTypedCollection(
                    modelPkg.getMofPackage().refAllOfClass(),
                    MofPackage.class))
        {
            addMofPackage(mofPackage);
        }
    }
    
    public ModelPackage getModelPackage()
    {
        return modelPkg;
    }
    
    private void addMofPackage(MofPackage mofPackage)
    {
        for(ModelElement modelElement:
                GenericCollections.asTypedCollection(
                    mofPackage.getContents(), ModelElement.class))
        {
            if (modelElement instanceof MofPackage) {
                addMofPackage((MofPackage) modelElement);
            } else if (modelElement instanceof MofClass) {
                addMofClass((MofClass) modelElement);
            } else if (modelElement instanceof Association) {
                addMofAssoc((Association) modelElement);
            }
        }
    }
 
    private ClassVertex addMofClass(MofClass mofClass)
    {
        ClassVertex vertex = getVertexForMofClass(mofClass);
        if (vertex != null) {
            return vertex;
        }
        
        vertex = new ClassVertex(mofClass);
        inheritanceGraph.addVertex(vertex);
        associationGraph.addVertex(vertex);
        attributeGraph.addVertex(vertex);
        
        vertexMap.put(mofClass, vertex);

        for(MofClass superClass:
                GenericCollections.asTypedCollection(
                    mofClass.getSupertypes(), MofClass.class))
        {
            ClassVertex superVertex = addMofClass(superClass);
            InheritanceEdge edge =
                new InheritanceEdge(superVertex, vertex);
            inheritanceGraph.addEdge(superVertex, vertex, edge);
        }
        
        Collection<Attribute> attributes =
            CodeGenUtils.contentsOfType(mofClass, Attribute.class);
        for(Attribute attrib: attributes) {
            if (attrib.getScope() != ScopeKindEnum.INSTANCE_LEVEL ||
                attrib.getVisibility() != VisibilityKindEnum.PUBLIC_VIS)
            {
                continue;
            }
            
            Classifier type = attrib.getType();
            if (type instanceof AliasType) {
                type = ((AliasType)type).getType();
            }
            
            if (type instanceof MofClass) {
                ClassVertex attribVertex = addMofClass((MofClass)type);
                
                AttributeEdge attribEdge = 
                    new AttributeEdge(vertex, attribVertex, attrib);
                attributeGraph.addEdge(vertex, attribVertex, attribEdge);
            }
        }
        
        return vertex;
    }
    
    private void addMofAssoc(Association assoc)
    {
        if (getEdgeForMofAssoc(assoc) != null) {
            return;
        }
        
        AssociationEdge assocEdge = new AssociationEdge(assoc);

        MofClass sourceClass = (MofClass) assocEdge.getSourceEnd().getType();
        MofClass targetClass = (MofClass) assocEdge.getTargetEnd().getType();
        ClassVertex sourceVertex = addMofClass(sourceClass);
        ClassVertex targetVertex = addMofClass(targetClass);

        associationGraph.addEdge(sourceVertex, targetVertex, assocEdge);
        assocEdgeMap.put(assoc, assocEdge);
    }
    
    public DirectedGraph<ClassVertex, InheritanceEdge> getInheritanceGraph()
    {
        return unmodifiableInheritanceGraph;
    }
    
    public DirectedGraph<ClassVertex, AssociationEdge> getAssociationGraph()
    {
        return unmodifiableAssociationGraph;
    }
    
    public DirectedGraph<ClassVertex, AttributeEdge> getAttributeGraph()
    {
        return unmodifiableAttributeGraph;
    }
    
    public ClassVertex getVertexForMofClass(MofClass mofClass)
    {
        return vertexMap.get(mofClass);
    }
    
    public AssociationEdge getEdgeForMofAssoc(Association assoc)
    {
        return assocEdgeMap.get(assoc);
    }
    
    public static class ClassVertex
    {
        private final MofClass mofClass;
        
        ClassVertex(MofClass cls)
        {
            this.mofClass = cls;
        }

        public MofClass getMofClass()
        {
            return mofClass;
        }
        
        public String toString()
        {
            return mofClass.getName();
        }

        public int hashCode()
        {
            return mofClass.hashCode();
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof ClassVertex)) {
                return false;
            }
            return mofClass.equals(((ClassVertex) obj).mofClass);
        }
    }
    
    public static class InheritanceEdge extends DefaultEdge
    {
        private static final long serialVersionUID = 8499865589049423429L;

        private final ClassVertex superClass;
        private final ClassVertex subClass;
        
        InheritanceEdge(ClassVertex superClass, ClassVertex subClass)
        {
            this.superClass = superClass;
            this.subClass = subClass;
        }
        
        public ClassVertex getSuperClass()
        {
            return superClass;
        }
        
        public ClassVertex getSubClass()
        {
            return subClass;
        }
        
        public String toString()
        {
            return 
                getSuperClass().toString() + "_generalizes_" + getSubClass();
        }
    }
    
    public static class AssociationEdge extends DefaultEdge
    {
        private static final long serialVersionUID = -111145932198294944L;

        private final Association assoc;
        private final AssociationEnd[] assocEnds;
        
        AssociationEdge(Association assoc)
        {
            this.assoc = assoc;
            
            List<?> ends = assoc.getContents();
            
            this.assocEnds = new AssociationEnd[] {
                (AssociationEnd)ends.get(0),
                (AssociationEnd)ends.get(1)
            };
            
            boolean swapEnds = false;

            if (assocEnds[1].getAggregation() == AggregationKindEnum.COMPOSITE)
            {
                swapEnds = true;
            }

            if (assocEnds[0].getMultiplicity().getUpper() != 1
                && assocEnds[1].getMultiplicity().getUpper() == 1)
            {
                swapEnds = true;
            }

            if (assocEnds[0].getMultiplicity().isOrdered()) {
                swapEnds = true;
            }

            if (swapEnds) {
                AssociationEnd tmp = assocEnds[0];
                assocEnds[0] = assocEnds[1];
                assocEnds[1] = tmp;
            }            
        }
        
        public Association getMofAssociation()
        {
            return assoc;
        }
        
        public AssociationEnd getSourceEnd()
        {
            return getEnd(0);
        }
        
        public AssociationEnd getTargetEnd()
        {
            return getEnd(1);
        }
        
        public AssociationEnd getEnd(int end)
        {
            return assocEnds[end];
        }
        
        public boolean matchesMofDirection()
        {
            return getSourceEnd().equals(assoc.getContents().get(0));
        }
    }
    
    public static class AttributeEdge extends DefaultEdge
    {
        private static final long serialVersionUID = -5616502469857617729L;

        private final ClassVertex owner;
        private final ClassVertex attributeClass;
        private final Attribute attribute;
        
        AttributeEdge(
            ClassVertex owner, 
            ClassVertex attributeClass,
            Attribute attribute)
        {
            this.owner = owner;
            this.attributeClass = attributeClass;
            this.attribute = attribute;
        }
        
        public ClassVertex getOwner()
        {
            return owner;
        }
        
        public ClassVertex getAttributeClass()
        {
            return attributeClass;
        }
        
        public Attribute getAttribute()
        {
            return attribute;
        }
        
        public String toString()
        {
            return 
                getOwner().toString() 
                + "_contains_" 
                + getAttributeClass() 
                + ":" + attribute.getName();
        }
    }
}

// End ModelGraph.java
