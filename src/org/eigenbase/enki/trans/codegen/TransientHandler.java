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
package org.eigenbase.enki.trans.codegen;

import java.io.*;
import java.util.*;

import javax.jmi.model.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.codegen.MofInitHandler.*;
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.trans.*;
import org.eigenbase.enki.util.*;
import org.eigenbase.enki.util.StringUtil.*;

/**
 * TransientHandler extends {@link TransientImplementationHandler} to support
 * a completely transient repository implementation.
 * 
 * @author Stephan Zuercher
 */
public class TransientHandler 
    extends TransientImplementationHandler
    implements SubordinateHandler
{
    /** Reference to {@link MetamodelInitializer}. */
    private static final JavaClassReference METAMODEL_INITIALIZER_CLASS =
        new JavaClassReference(MetamodelInitializer.class, true);

    /** Reference to {@link TransientMDRepository}. */
    private static final JavaClassReference TRANSIENT_REPOS_CLASS =
        new JavaClassReference(TransientMDRepository.class, false);
    
    /** Reference to {@link TransientMDRepository}. */
    private static final JavaClassReference INTERNAL_MDR_ERROR =
        new JavaClassReference(InternalMdrError.class, false);
    
    /** Reference to {@link TransientRefObject}. */
    private static final JavaClassReference TRANSIENT_REF_OBJECT =
        new JavaClassReference(TransientRefObject.class, false);
    
    /** Reference to {@link OwnedCollection}. */
    private static final JavaClassReference OWNED_COLLECTION =
        new JavaClassReference(OwnedCollection.class, false);
    
    /** Reference to {@link OwnedList}. */
    private static final JavaClassReference OWNED_LIST =
        new JavaClassReference(OwnedList.class, false);
    
    private static final String OWNER_FIELD = "_owner";
    private static final String ANNOTATION_FIELD = "_annotation";
    
    private File metaInfEnkiDir;

    private String topLevelPackage;
    
    private String extentName;

    private String initializerName;

    private ModelGraph modelGraph;
    
    public TransientHandler()
    {
        super();
    }
    
    public void setExtentName(String extentName)
    {
        this.extentName = extentName;
    }
    
    public void setInitializerClassName(String initializerName)
    {
        this.initializerName = initializerName;
    }
    
    @Override
    public void beginGeneration()
    {
        File metaInfDir = 
            new File(outputDir, MDRepositoryFactory.META_INF_DIR_NAME);
        if (!metaInfDir.exists()) {
            metaInfDir.mkdir();
        }
        
        metaInfEnkiDir = 
            new File(metaInfDir, MDRepositoryFactory.ENKI_DIR_NAME);
        if (!metaInfEnkiDir.exists()) {
            metaInfEnkiDir.mkdir();
        }
        
        ModelPackage modelPkg = (ModelPackage)generator.getRefBaseObject();
        modelGraph = new ModelGraph(modelPkg);
    }
    
    @Override
    public void endGeneration(boolean throwing)
        throws GenerationException
    {
        if (!throwing) {
            if (initializerName == null) {
                throw new GenerationException("Unknown initializer");
            }
            
            File enkiConfigFile = 
                new File(
                    metaInfEnkiDir, MDRepositoryFactory.CONFIG_PROPERTIES);
            open(enkiConfigFile);
    
            writeln("# Generated Enki Metamodel Properties");
            newLine();
            
            writeln(
                MDRepositoryFactory.PROPERTY_ENKI_IMPLEMENTATION, "=", 
                MdrProvider.ENKI_TRANSIENT.name());
            if (!pluginMode) {
                writeln(
                    MDRepositoryFactory.PROPERTY_ENKI_TOP_LEVEL_PKG, "=", 
                    topLevelPackage);
            }
            writeln(
                MDRepositoryFactory.PROPERTY_ENKI_EXTENT, "=", extentName);
            writeln(
                TransientMDRepository.PROPERTY_MODEL_INITIALIZER, "=", 
                initializerName);
            writeln(
                TransientMDRepository.PROPERTY_MODEL_PLUGIN, "=",
                pluginMode);

            writeln(
                TransientMDRepository.PROPERTY_MODEL_PACKAGE_VERSION, "=",
                TransientMDRepository.PACKAGE_VERSION);
            
            close(false);
        }
        
        super.endGeneration(throwing);
    }


    @Override
    public void generatePackage(MofPackage pkg)
        throws GenerationException
    {
        super.generatePackage(pkg);
    
        if (pkg.getContainer() == null && 
            !pkg.getName().equals("PrimitiveTypes"))
        {
            topLevelPackage =
                CodeGenUtils.getTypeName(
                    pkg, PACKAGE_SUFFIX + generator.getImplSuffix());
        }
    }

    @Override
    protected String convertToTypeName(String entityName)
        throws GenerationException
    {
        return entityName + generator.getImplSuffix();
    }

    @Override
    protected String computeSuffix(String baseSuffix)
    {
        if (baseSuffix != null) {
            return baseSuffix + generator.getImplSuffix();
        }
        return generator.getImplSuffix();
    }

    @Override
    protected void generateCustomPackageInit(MofPackage pkg)
    {
        writeln(
            METAMODEL_INITIALIZER_CLASS,
            ".getCurrentInitializer().setRefMetaObject(this, ", 
            QUOTE, pkg.getName(), QUOTE, ");");
    }

    @Override
    protected void generateCustomPackageMethods(MofPackage pkg)
    {
        newLine();
        startBlock("public void refDelete()");
        startStmtBlock("for(Object c: refAllClasses()) {");
        writeln(REF_CLASS_CLASS, " rc = (", REF_CLASS_CLASS, ")c;");
        startStmtBlock("for(Object o: rc.refAllOfClass()) {");
        writeln("((", REF_OBJECT_CLASS, ")o).refDelete();");
        endBlock();
        endBlock();
        
        newLine();
        startStmtBlock("for(Object p: refAllPackages()) {");
        writeln("((", REF_PACKAGE_CLASS, ")p).refDelete();");
        endBlock();
        
        if (pkg.getContainer() == null) {
            newLine();
            startStmtBlock("try {");
            writeln("getRepository().dropExtentStorage(this);");
            decreaseIndent();
            startStmtBlock("} catch(Exception e) {");
            writeln("throw new ", INTERNAL_MDR_ERROR, "(e);");
            endBlock();
        }
        
        endBlock();
    }

    @Override
    protected void generateCustomAssociationInit(Association assoc)
    {
        writeln(
            METAMODEL_INITIALIZER_CLASS,
            ".getCurrentInitializer().setRefMetaObject(this, ", 
            QUOTE, assoc.getName(), QUOTE, ");");
    }

    @Override
    protected void generateCustomClassProxyInit(MofClass cls)
    {
        writeln(
            METAMODEL_INITIALIZER_CLASS,
            ".getCurrentInitializer().setRefMetaObject(this, ", 
            QUOTE, cls.getName(), QUOTE, ");");
    }
    
    @Override
    protected void generateCustomClassProxyMethods(MofClass cls)
    {
        // Override registration method
        newLine();
        startBlock("protected void register(RefObject instance)");
        writeln(
            "((", 
            TRANSIENT_REPOS_CLASS, 
            ")getRepository()).register(this, instance);");
        writeln("super.register(instance);");
        endBlock();
        
        // Override un-registration method
        newLine();
        startBlock("protected void unregister(RefObject instance)");
        writeln("super.unregister(instance);");
        writeln(
            "((", 
            TRANSIENT_REPOS_CLASS, 
            ")getRepository()).unregister(this, instance);");
        endBlock();
    }
    
    @Override
    protected JavaClassReference[] getCustomInterfaces()
    {
        return new JavaClassReference[] { TRANSIENT_REF_OBJECT };
    }
    
    @Override
    protected void generateCustomClassInstanceFields(MofClass cls)
    {
        writeln("private ", REF_OBJECT_CLASS, " ", OWNER_FIELD, ";");
        writeln("private String ", ANNOTATION_FIELD, ";");
        newLine();
    }
    
    @Override
    protected void generateCustomClassInstanceInit(
        MofClass cls, boolean withAttributes)
        throws GenerationException
    {
        ModelGraph.ClassVertex clsVertex = 
            modelGraph.getVertexForMofClass(cls);
        Set<ModelGraph.AttributeEdge> edges = 
            modelGraph.getAttributeGraph().outgoingEdgesOf(clsVertex);
        for(ModelGraph.AttributeEdge edge: edges) {
            Attribute attrib = edge.getAttribute();
            String name = 
                CodeGenUtils.getClassFieldName(attrib.getName());

            if (attrib.getMultiplicity().getUpper() != 1) {
                if (!withAttributes) {
                    writeOwnedCollectionCall(
                        name, attrib.getMultiplicity().isOrdered());
                }
            } else {
                startConditionalBlock(CondType.IF, name, " != null");
                writeMarkOwnerCall(attrib, name, false);
                endBlock();
            }
        }
    }
    
    private void writeMarkOwnerCall(
        Attribute attrib, String varName, boolean clear)
    throws GenerationException    
    {
        String ownerValue = (clear ? "null" : "this");
        writeln(
            "((", TRANSIENT_REF_OBJECT,")", varName, ").markOwner(", 
            ownerValue, ");");
    }
    
    private void writeOwnedCollectionCall(String varName, boolean ordered)
    throws GenerationException    
    {
        writeln("this.", varName, " = new ",
            ordered ? OWNED_LIST : OWNED_COLLECTION, "(this.",
            varName, ", this);");
    }
    
    @Override
    protected void generateCustomClassInstanceMutator(
        MofClass cls, Attribute attrib)
    throws GenerationException
    {
        Classifier type = attrib.getType();
        if (type instanceof AliasType) {
            type = ((AliasType)type).getType();
        }
        
        if (type instanceof MofClass) {
            String name = CodeGenUtils.getClassFieldName(attrib.getName());
            
            startConditionalBlock(CondType.IF, name, " != null");
            writeMarkOwnerCall(attrib, name, true);
            endBlock();
            
            startConditionalBlock(CondType.IF, "newValue != null");
            writeMarkOwnerCall(attrib, "newValue", false);
            endBlock();
            newLine();
        }
    }
    
    @Override
    protected void generateCustomClassInstanceMethods(
        MofClass cls,
        Collection<ReferenceInfo> references,
        Collection<ReferenceInfo> unreferencedAssocs)
    throws GenerationException
    {
        // Override refDelete method
        newLine();
        startBlock("public void refDelete()");
        writeln("super.unregister();");    
        
        // Delete composite attributes
        Collection<Attribute> instanceAttributes =
            CodeGenUtils.contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS,
                ScopeKindEnum.INSTANCE_LEVEL,
                Attribute.class);
        for(Attribute attrib: instanceAttributes) {
            if (attrib.isDerived()) {
                continue;
            }
            
            Classifier type = attrib.getType();
            if (type instanceof AliasType) {
                type = ((AliasType)type).getType();
            }
            
            if (!(type instanceof MofClass)) {
                continue;
            }

            String name = attrib.getName();
            name = CodeGenUtils.getClassFieldName(name);
            
            if (attrib.getMultiplicity().getUpper() == 1) {
                startConditionalBlock(CondType.IF, name, " != null");
                writeln(name, ".refDelete();");
                endBlock();
            } else {
                startBlock("for(", REF_OBJECT_CLASS, " obj: ", name, ")");
                writeln("obj.refDelete();");
                endBlock();
            }
        }
        
        List<ReferenceInfo> all = new ArrayList<ReferenceInfo>();
        all.addAll(references);
        all.addAll(unreferencedAssocs);
        
        int count = 1;
        for(ReferenceInfo refInfo: all) {
            String fieldName = 
                generator.transformIdentifier(refInfo.getFieldName());
            String endName = refInfo.getEndName(refInfo.getExposedEndIndex());
            
            if (refInfo.isComposite(refInfo.getExposedEndIndex())) {
                String cName = "refs" + count;
                String iName = "iter" + count++;
                writeln(
                    TransientImplementationHandler.JAVA_UTIL_COLLECTION_CLASS,
                    "<?> ", cName, " = ", fieldName, ".refQuery(", 
                    QUOTE, endName, QUOTE, 
                    ", this);");
                writeln(
                    TransientImplementationHandler.JAVA_UTIL_ITERATOR_CLASS,
                    "<?> ", iName, " = ", cName, ".iterator();");
                startBlock("while(", iName, ".hasNext())");
                writeln(
                    REF_OBJECT_CLASS, " o = (", REF_OBJECT_CLASS, ")", 
                    iName, ".next();");
                writeln(iName, ".remove();");
                writeln("o.refDelete();");
                endBlock();
            } else {
                // Not a composite, just remove the associations in one shot
                writeln(
                    fieldName, ".refQuery(", 
                    QUOTE, endName, QUOTE, ", this).clear();");
            }
        }
        endBlock();

        // Override refImmediateComposite method
        newLine();
        startBlock("public ", REF_OBJECT_CLASS, " refImmediateComposite()");
        boolean generatedVar = false;
        for(ReferenceInfo refInfo: references) {
            if (refInfo.isComposite()) {
                if (refInfo.isComposite()) {
                    assert(refInfo.isSingle());
                    
                    if (!generatedVar) {
                        writeln(REF_OBJECT_CLASS, " o;");
                        generatedVar = true;
                    }
                    
                    writeln(
                        "o = ", 
                        CodeGenUtils.getAccessorName(
                            generator, refInfo.getReference()),
                        "();");
                    startConditionalBlock(
                        CondType.IF, "o != null");
                    writeln("return o;");
                    endBlock();
                }                
            }
        }
        for(ReferenceInfo refInfo: unreferencedAssocs) {
            if (refInfo.isComposite()) {
                assert(refInfo.isSingle());
                
                int end = refInfo.getReferencedEndIndex();
                
                if (!generatedVar) {
                    writeln(REF_OBJECT_CLASS, " o;");
                    generatedVar = true;
                }
                writeln(
                    "o = ", refInfo.getFieldName(), 
                    ".get", 
                    StringUtil.mangleIdentifier(
                        refInfo.getEndName(end),
                        IdentifierType.CAMELCASE_INIT_UPPER), 
                    "(this);");
                startConditionalBlock(CondType.IF, "o != null");
                writeln("return o;");
                endBlock();
            }            
        }
        
        writeln("return ", OWNER_FIELD, ";");
        endBlock();
        
        // Implement markOwner
        newLine();
        startBlock("public void markOwner(", REF_OBJECT_CLASS, " newOwner)");
        startConditionalBlock(
            CondType.IF, OWNER_FIELD, " != null && newOwner != null");
        writeln("throw new IllegalArgumentException();");
        endBlock();
        writeln("this.", OWNER_FIELD, " = newOwner;");
        endBlock();
        
        // Implement annotate
        newLine();
        startBlock("public void annotate(String annotation)");
        writeln("this.", ANNOTATION_FIELD, " = annotation;");
        endBlock();
        
        // Implement annotation
        newLine();
        startBlock("public String annotation()");
        writeln("return this.", ANNOTATION_FIELD, ";");
        endBlock();
        
    }

}
// End TransientHandler.java
