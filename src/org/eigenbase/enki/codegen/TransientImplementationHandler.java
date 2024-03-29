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

import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;

/**
 * TransientImplementationGenerator generates code for an in-memory MOF model 
 * that can be used to implement any metamodel.
 * 
 * @author Stephan Zuercher
 */
public abstract class TransientImplementationHandler
    extends JavaHandlerBase
    implements 
        AssociationHandler, 
        ClassInstanceHandler, 
        ClassProxyHandler,
        PackageHandler,
        StructureHandler
{
    public static final String JMI_PACKAGE_PREFIX = "javax.jmi.";
    
    public static final String JMI_PACKAGE_PREFIX_SUBST = 
        "org.eigenbase.enki.jmi.";
    
    private static final String AUTHOR_NOTE = 
        "\n\n" + 
        "@author '{'@link " + MofImplementationHandler.class.getName() + "}";
    
    private static final String MOF_CLASS_COMMENT = 
        "Implements MOF''s {0} interface." + AUTHOR_NOTE;

    private static final String MOF_ASSOC_COMMENT = 
        "Implements MOF''s {0} association interface." + AUTHOR_NOTE;
    
    /** Class comment for {@link RefPackage} implementations. */
    private static final String MOF_PACKAGE_COMMENT = 
        "Implements MOF''s {0} package interface." + AUTHOR_NOTE;

    /** Class comment for {@link RefClass} implementations. */
    private static final String MOF_CLASS_PROXY_COMMENT = 
        "Implements MOF''s {0} class proxy interface." + AUTHOR_NOTE;

    protected static final JavaClassReference REF_OBJECT_IMPL_CLASS =
        new JavaClassReference(RefObjectBase.class, true);
    
    protected static final JavaClassReference REF_ASSOC_IMPL_CLASS =
        new JavaClassReference(RefAssociationBase.class, true);
    
    private static final JavaClassReference REF_CLASS_IMPL_CLASS =
        new JavaClassReference(RefClassBase.class, true);
    
    private static final JavaClassReference REF_PACKAGE_IMPL_CLASS =
        new JavaClassReference(RefPackageBase.class, true);
    
    private static final JavaClassReference REF_STRUCT_IMPL_CLASS =
        new JavaClassReference(RefStructBase.class, true);
    
    /**
     * Name of the class used to stored unordered collections of attributes.
     */
    private static final JavaClassReference COLLECTION_IMPL_CLASS = 
        new JavaClassReference(HashSet.class, true);

    /**
     * Name of the class used to stored ordered collections of attributes.
     */
    private static final JavaClassReference ORDERED_COLLECTION_IMPL_CLASS = 
        new JavaClassReference(ArrayList.class, true);

    private static final JavaClassReference COLLECTION_CLASS = 
        new JavaClassReference(Collection.class, true);

    private static final JavaClassReference ORDERED_COLLECTION_CLASS = 
        new JavaClassReference(List.class, true);

    private static final JavaClassReference JAVA_UTIL_LIST_CLASS =
        new JavaClassReference(List.class, true);
    
    public static final JavaClassReference JAVA_UTIL_COLLECTION_CLASS =
        new JavaClassReference(Collection.class, true);

    private static final JavaClassReference JAVA_UTIL_COLLECTIONS_CLASS =
        new JavaClassReference(Collections.class, true);
    
    public static final JavaClassReference JAVA_UTIL_ITERATOR_CLASS =
        new JavaClassReference(Iterator.class, true);

    private static final JavaClassReference JAVA_UTIL_ARRAYS_CLASS =
        new JavaClassReference(Arrays.class, true);
    
    private static final JavaClassReference MULTIPLICITY_CLASS =
        new JavaClassReference(Multiplicity.class, true);

    private static final JavaClassReference[] CLASS_INSTANCE_REFS = {
        REF_OBJECT_IMPL_CLASS,
        REF_CLASS_CLASS.asImport(),
        JAVA_UTIL_LIST_CLASS,
        JAVA_UTIL_COLLECTION_CLASS,
        JAVA_UTIL_COLLECTIONS_CLASS,
        COLLECTION_IMPL_CLASS,
        ORDERED_COLLECTION_IMPL_CLASS,
    };
    
    private static final JavaClassReference[] ASSOC_REFS = {
        REF_ASSOC_IMPL_CLASS,
        MULTIPLICITY_CLASS,
        COLLECTION_CLASS,
        ORDERED_COLLECTION_CLASS,
    };
    
    private static final JavaClassReference[] CLASS_PROXY_REFS = {
        REF_PACKAGE_CLASS.asImport(),
        REF_CLASS_IMPL_CLASS,
    };
    
    private static final JavaClassReference[] PACKAGE_REFS = {
        REF_PACKAGE_IMPL_CLASS,
    };
    
    private static final JavaClassReference[] STRUCT_REFS = {
        REF_STRUCT_IMPL_CLASS,
        JMI_EXCEPTION_CLASS,
        JAVA_UTIL_ARRAYS_CLASS,
        JAVA_UTIL_LIST_CLASS,
    };
    
    private final Logger log = 
        Logger.getLogger(MofImplementationHandler.class.getName());

    /** Map of {@link Association} to {@link AssociationInfo}. */
    private Map<Association, AssociationInfo> assocInfoMap;
    
    public TransientImplementationHandler()
    {
        super();
        
        setDisplayHeaderWarning(false);
        
        this.assocInfoMap = new HashMap<Association, AssociationInfo>();
    }
    
    public int getNumPasses()
    {
        return 2;
    }
    
    public void generateAssociation(Association assoc)
        throws GenerationException
    {
        String interfaceName = CodeGenUtils.getTypeName(assoc);
        
        String typeName = convertToTypeName(interfaceName);

        AssociationInfo assocInfo = new AssociationInfoImpl(assoc);
                
        if (getPassIndex() == 0) {
            assocInfoMap.put(assoc, assocInfo);
            return;
        }
        
        if (!isIncluded(assoc)) {
            log.fine(
                "Skipping Excluded Association Implementation '" 
                + typeName + "'");
            return;
        }
        
        log.fine("Generating Association Implementation '" + typeName + "'");

        String baseClass = getAssociationBaseClass(assocInfo).toString();
        
        open(typeName);
        try {
            writeClassHeader(
                assoc,
                typeName, 
                baseClass,
                new String[] { interfaceName }, 
                JavaClassReference.computeImports(ASSOC_REFS),
                false,
                MOF_ASSOC_COMMENT);
        
            Multiplicity end1Multiplicity = 
                Multiplicity.fromMultiplicityType(
                    assocInfo.getEnd(0).getMultiplicity());
            
            Multiplicity end2Multiplicity = 
                Multiplicity.fromMultiplicityType(
                    assocInfo.getEnd(1).getMultiplicity());
            
            // constructor
            startBlock(
                "public ",
                CodeGenUtils.getSimpleTypeName(assoc, computeSuffix("")),
                "(", REF_PACKAGE_CLASS, " container)");
            writeln(
                "super(");
            increaseIndent();
            writeln("container,");
            writeln(QUOTE, assocInfo.getEndName(0), QUOTE, ",");
            writeln(
                CodeGenUtils.getTypeName(assocInfo.getEnd(0).getType()), 
                ".class,");
            writeln(end1Multiplicity.toInstantiationString(true), ",");
            writeln(QUOTE, assocInfo.getEndName(1), QUOTE, ",");
            writeln(
                CodeGenUtils.getTypeName(assocInfo.getEnd(1).getType()), 
                ".class,");
            writeln(end2Multiplicity.toInstantiationString(true), ");");
            newLine();
            generateCustomAssociationInit(assoc);
            decreaseIndent();
            endBlock();
            newLine();

            // exists
            startGenericMethodBlock(
                assoc,
                "boolean",
                "exists", 
                assocInfo.getEndTypes(),
                assocInfo.getEndIdentifiers());
            writeln(
                "return super.refLinkExists(", 
                assocInfo.getEndIdentifier(0),
                ", ",
                assocInfo.getEndIdentifier(1), ");");
            endBlock();
            newLine();
            
            // get end1 from end2
            generateAssociationEndAccessor(assoc, assocInfo, 0, 1);
            
            // get end2 from end1
            generateAssociationEndAccessor(assoc, assocInfo, 1, 0);

            if (assocInfo.isChangeable(0) && assocInfo.isChangeable(1)) {
                // add
                startGenericMethodBlock(
                    assoc,
                    "boolean",
                    "add",
                    assocInfo.getEndTypes(),
                    assocInfo.getEndIdentifiers());
                writeln(
                    "return super.refAddLink(", 
                    assocInfo.getEndIdentifier(0), ", ", 
                    assocInfo.getEndIdentifier(1), ");");
                endBlock();

                newLine();
                
                // remove
                startGenericMethodBlock(
                    assoc,
                    "boolean",
                    "remove",
                    assocInfo.getEndTypes(),
                    assocInfo.getEndIdentifiers());
                writeln(
                    "return super.refRemoveLink(", 
                    assocInfo.getEndIdentifier(0), ", ", 
                    assocInfo.getEndIdentifier(1), ");");
                endBlock();
            }

            writeEntityFooter();
        }
        finally {
            close();
        }            
    }

    protected JavaClassReference getAssociationBaseClass(
        AssociationInfo assocInfo)
    throws GenerationException
    {
        return REF_ASSOC_IMPL_CLASS;
    }
    
    /**
     * Allows subclasses to perform additional initialization during 
     * construction of a association proxy object.
     * 
     * @param assoc Association proxy being generated
     */
    protected void generateCustomAssociationInit(Association assoc)
        throws GenerationException
    {
    }
    
    private void generateAssociationEndAccessor(
        Association assoc,
        AssociationInfo assocInfo,
        int getIndex,
        int fromIndex)
    {
        if (assocInfo.getEnd(getIndex).isNavigable()) {
            boolean getEndSingle = assocInfo.isSingle(getIndex);
            boolean ordered = assocInfo.isOrdered(getIndex);
            if (!getEndSingle) {
                // Suppress unchecked collection warnings.
                emitSuppressWarningsAnnotation();
            }
            startGenericMethodBlock(
                assoc,
                getEndSingle 
                    ? assocInfo.getEndType(getIndex)
                    : CodeGenUtils.getCollectionType(
                        ordered 
                            ? ORDERED_COLLECTION_CLASS
                            : COLLECTION_CLASS,
                        assocInfo.getEndType(getIndex)),
                CodeGenUtils.getAccessorName(
                    generator, assocInfo.getEnd(getIndex), null),
                new String[] { assocInfo.getEndType(fromIndex) },
                new String[] { assocInfo.getEndIdentifier(fromIndex) });
            if (getEndSingle) {
                write(COLLECTION_CLASS, "<?> result = ");
            } else {
                write("return ");
                if (ordered) {
                    write("(", ORDERED_COLLECTION_CLASS, ")");
                }
            }
            
            writeln("super.refQuery(");
            increaseIndent();
            writeln(
                QUOTE, assocInfo.getEndName(fromIndex), QUOTE, ", ", 
                assocInfo.getEndIdentifier(fromIndex), ");");
            decreaseIndent();
            
            if (getEndSingle) {
                // Convert from collection to typed element
                writeln(
                    JAVA_UTIL_ITERATOR_CLASS, "<?> iter = result.iterator();");
                startConditionalBlock(CondType.IF, "iter.hasNext()");
                String cast = assocInfo.getEndType(getIndex);
                writeln("return (", cast, ")iter.next();");
                endStmtBlock();
                writeln("return null;");
            }
            
            endBlock();
            newLine();
        }
    }

    public void generateClassInstance(MofClass cls)
        throws GenerationException
    {
        if (getPassIndex() == 0) {
            return;
        }
        
        String interfaceName = CodeGenUtils.getTypeName(cls);
        
        String typeName = convertToTypeName(interfaceName);

        if (!isIncluded(cls)) {
            log.fine(
                "Skipping Excluded Class Instance Implementation '" 
                + typeName + "'");
            return;
        }
        
        if (cls.isAbstract()) {
            log.fine(
                "Skipping Class Instance Implementation '" + typeName + "'");
            return;
        }
        
        log.fine(
            "Generating Class Instance Implementation '" + typeName + "'");

        Collection<Reference> instanceReferences =
            CodeGenUtils.contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS,
                Reference.class);

        open(typeName);
        try {
            JavaClassReference[] extraInterfaces = getCustomInterfaces();
            
            String[] interfaces = new String[extraInterfaces.length + 1];
            interfaces[0] = interfaceName;
            int iface = 1;
            for(JavaClassReference extraInterface: extraInterfaces) {
                interfaces[iface++] = extraInterface.toString();
            }
            
            List<JavaClassReference> imports = 
                new ArrayList<JavaClassReference>();
            imports.addAll(Arrays.asList(CLASS_INSTANCE_REFS));
            imports.addAll(Arrays.asList(extraInterfaces));
            
            JavaClassReference baseClass = getClassInstanceBaseClass(cls);
            
            writeClassHeader(
                cls, 
                typeName,
                baseClass.toString(),
                interfaces,
                JavaClassReference.computeImports(
                    imports.toArray(new JavaClassReference[0])),
                false,
                MOF_CLASS_COMMENT);
            
            // fields
            writeln("// Attribute Fields");
            Collection<Attribute> instanceAttributes =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    VisibilityKindEnum.PUBLIC_VIS,
                    ScopeKindEnum.INSTANCE_LEVEL,
                    Attribute.class);
            ArrayList<Attribute> nonDerivedAttribs = 
                new ArrayList<Attribute>();
            Map<Attribute, String> nonDerivedAttribNames = 
                new HashMap<Attribute, String>();
            boolean hasCollectionField = false;
            for(Attribute attrib: instanceAttributes) {
                if (attrib.isDerived()) {
                    continue;
                }
                
                nonDerivedAttribs.add(attrib);
                
                int upper = attrib.getMultiplicity().getUpper();
                if (upper < 0 || upper > 1) {
                    emitSuppressWarningsAnnotation();
                    hasCollectionField = true;
                }
                String fieldName = writeField(attrib, "private", false, false);
                nonDerivedAttribNames.put(attrib, fieldName);
            }
            newLine();
            
            // Reference fields
            // This produces multiple fields for self-references (such as
            // Contains).  Not a big deal, but perhaps confusing.
            writeln("// Reference Fields");
            Map<Reference, ReferenceInfo> refInfoMap =
                new HashMap<Reference, ReferenceInfo>();
            
            for(Reference ref: instanceReferences) {
                ReferenceInfo refInfo = new ReferenceInfoImpl(ref);

                writeField(
                    refInfo.getAssocInterfaceName(),
                    generator.transformIdentifier(refInfo.getFieldName()),
                    "private", 
                    false, 
                    false);
                
                refInfoMap.put(ref, refInfo);
            }
            newLine();
            
            // Association fields
            writeln("// Association Fields");
            Map<Association, ReferenceInfo> unrefAssocRefInfoMap =
                new HashMap<Association, ReferenceInfo>();

            Collection<Association> unreferencedAssociations = 
                CodeGenUtils.findUnreferencedAssociations(
                    assocInfoMap,
                    cls,
                    instanceReferences, 
                    unrefAssocRefInfoMap);

            for(Association assoc: unreferencedAssociations) {
                ReferenceInfo refInfo = unrefAssocRefInfoMap.get(assoc);
                
                writeField(
                    refInfo.getAssocInterfaceName(),
                    generator.transformIdentifier(refInfo.getFieldName()),
                    "private", 
                    false, 
                    false);
            }
            newLine();
            
            generateCustomClassInstanceFields(cls);
            
            // "zero-arg" constructor
            if (hasCollectionField) {
                emitSuppressWarningsAnnotation();
            }
            startConstructorBlock(
                cls, 
                new String[] { REF_CLASS_CLASS.toSimple() }, 
                new String[] { "refClass" }, 
                computeSuffix(""));
            writeln("super(refClass);");
            for(Attribute attrib: nonDerivedAttribs) {
                MultiplicityType mult = attrib.getMultiplicity();
                int upper = mult.getUpper();
                if (upper == -1 || upper > 1) {
                    // Multiple values -- initialize collection
                    String fieldName = nonDerivedAttribNames.get(attrib);

                    String elemTypeName = 
                        CodeGenUtils.getTypeName(attrib.getType());
                    
                    String collTypeName;
                    if (mult.isOrdered()) {
                        collTypeName = 
                            CodeGenUtils.getCollectionType(
                                ORDERED_COLLECTION_IMPL_CLASS, elemTypeName);
                    } else {
                        collTypeName = 
                            CodeGenUtils.getCollectionType(
                                COLLECTION_IMPL_CLASS, elemTypeName);
                    }
                    
                    writeln(
                        "this.",
                        fieldName,
                        " = new ",
                        collTypeName,
                        "();");
                }
            }
            
            if (!instanceReferences.isEmpty()) {
                newLine();
                for(Reference ref: instanceReferences) {
                    ReferenceInfo refInfo = refInfoMap.get(ref);

                    write(
                        "this.",
                        generator.transformIdentifier(refInfo.getFieldName()),
                        " = (", refInfo.getAssocInterfaceName(), 
                        ")");
                    generateAssocExpression(cls, refInfo.getAssoc());
                    writeln(";");
                }
            }
            if (!unreferencedAssociations.isEmpty()) {
                newLine();
                for(Association assoc: unreferencedAssociations) {
                    ReferenceInfo refInfo = unrefAssocRefInfoMap.get(assoc);
                    
                    write(
                        "this.",
                        generator.transformIdentifier(refInfo.getFieldName()),
                        " = (", refInfo.getAssocInterfaceName(), 
                        ")");
                    generateAssocExpression(cls, refInfo.getAssoc());
                    writeln(";");
                }
            }
            generateCustomClassInstanceInit(cls, nonDerivedAttribs, false);
            endBlock();
            
            // constructor
            if (instanceAttributes.size() > 0) {
                newLine();
                if (hasCollectionField) {
                    emitSuppressWarningsAnnotation();
                }
                ModelElement[] params = 
                    nonDerivedAttribs.toArray(
                        new ModelElement[nonDerivedAttribs.size()]);
                String[] paramTypes = new String[params.length + 1];
                String[] paramNames = new String[params.length + 1];
                paramTypes[0] = REF_CLASS_CLASS.toSimple();
                paramNames[0] = "refClass";
                for(int i = 0; i < params.length; i++) {
                    String[] paramInfo = CodeGenUtils.getParam(params[i]);
                    paramTypes[i + 1] = paramInfo[0];
                    paramNames[i + 1] = paramInfo[1];
                }
                startConstructorBlock(
                    cls, paramTypes, paramNames, computeSuffix(""));
                writeln("this(refClass);");
                newLine();
                for(Attribute attrib: nonDerivedAttribs) {
                    String fieldName = nonDerivedAttribNames.get(attrib);
                    String[] paramInfo = CodeGenUtils.getParam(attrib);
                    MultiplicityType mult = attrib.getMultiplicity();
                    int upper = mult.getUpper();
                    if (upper == -1 || upper > 1) {
                        // Copy the collection
                        startConditionalBlock(
                            CondType.IF, paramInfo[1], " != null");
                        writeln(
                            "this.",
                            fieldName,
                            ".addAll(",
                            paramInfo[1],
                            ");");
                        endBlock();
                    } else {
                        writeln(
                            "this.",
                            fieldName,
                            " = ",
                            paramInfo[1],
                            ";");
                    }       
                }
                generateCustomClassInstanceInit(cls, nonDerivedAttribs, true);
                endBlock();
            }
            
            // attribute methods
            if (!instanceAttributes.isEmpty()) {
                newLine();
                writeln("// Attribute Methods");
            }
            for(Attribute attrib: instanceAttributes) {
                int upper = attrib.getMultiplicity().getUpper();

                if (attrib.isDerived()) {
                    newLine();
                    if (upper < 0 || upper > 1) {
                        emitSuppressWarningsAnnotation();
                    }
                    startAccessorBlock(attrib, true);
                    writeln(
                        "return super.", 
                        CodeGenUtils.getAccessorName(generator, attrib), 
                        "();");
                    endBlock();
                    
                    if (upper == 1 && attrib.isChangeable()) {
                        assert(false): "Changeable derived attrib";
                    }
                    continue;
                }
                
                boolean primitiveConversionRequired = false;
                String fieldTypeName = CodeGenUtils.getTypeName(attrib);
                if (Primitives.isPrimitiveType(fieldTypeName) &&
                    attrib.getMultiplicity().getLower() != 0)
                {
                    primitiveConversionRequired = true;
                }
                
                String fieldName = nonDerivedAttribNames.get(attrib);
                
                if (upper == 1) {
                    newLine();
                    startAccessorBlock(attrib, true);
                    if (primitiveConversionRequired) {
                        startConditionalBlock(
                            CondType.IF, fieldName, " == null");
                        writeln(
                            "return ", 
                            Primitives.getPrimitiveDefaultLiteral(
                                fieldTypeName),
                            ";");
                        endBlock();
                    }
                    writeln("return ", fieldName, ";");
                    endBlock();
                    
                    if (attrib.isChangeable()) {
                        newLine();
                        startMutatorBlock(attrib);
                        generateCustomClassInstanceMutator(cls, attrib);
                        writeln("this.", fieldName, " = newValue;");
                        endBlock();                        
                    }
                } else if (upper != 0) {
                    newLine();
                    emitSuppressWarningsAnnotation();
                    startAccessorBlock(attrib, true);
                    
                    if (attrib.isChangeable()) {
                        writeln("return ", fieldName, ";");
                    } else {
                        if (attrib.getMultiplicity().isOrdered()) {
                            writeln(
                                "return ",
                                JAVA_UTIL_COLLECTIONS_CLASS,
                                ".unmodifiableList(", 
                                fieldName,
                                ");");
                        } else {
                            writeln(
                                "return ",
                                JAVA_UTIL_COLLECTIONS_CLASS,
                                ".unmodifiableCollection(",
                                fieldName,
                                ");");                            
                        }
                    }
                    endBlock();
                }
            }
            
            // reference methods
            if (!instanceReferences.isEmpty()) {
                newLine();
                writeln("// Reference Methods");
            }
            for(Reference ref: instanceReferences) {
                ReferenceInfo refInfo = refInfoMap.get(ref);

                newLine();
                if (!refInfo.isSingle()) {
                    emitSuppressWarningsAnnotation();
                }
                startAccessorBlock(ref, true);
                String assocFieldName = 
                    generator.transformIdentifier(refInfo.getFieldName());
                
                writeln(
                    "return ",
                    assocFieldName,
                    ".",
                    refInfo.getAccessorName(generator), "(this);");
                endBlock();
                
                if (refInfo.isSingle() && refInfo.isChangeable()) {
                    newLine();
                    startMutatorBlock(ref);
                    String endName = 
                        refInfo.getEndName(refInfo.getExposedEndIndex());
                    writeln(
                        assocFieldName, ".refQuery(", 
                        QUOTE, endName, QUOTE, ", this).clear();");
                    startConditionalBlock(CondType.IF, "newValue != null");
                    if (refInfo.isReferencedEndFirst()) {
                        writeln(assocFieldName, ".add(newValue, this);");
                    } else {
                        writeln(assocFieldName, ".add(this, newValue);");                        
                    }
                    endBlock();
                    endBlock();
                }
            }
            
            // Operation methods
            // NOTE: MOF operations do not use generic types, so we can't emit
            // methods that use them here.
            Collection<Operation> operations =
                CodeGenUtils.contentsOfType(
                    cls, 
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    VisibilityKindEnum.PUBLIC_VIS, 
                    Operation.class);
            if (!operations.isEmpty()) {
                newLine();
                writeln("// Operation Methods");
            }
            for(Operation op: operations) {
                newLine();

                Collection<Parameter> parameters =
                    CodeGenUtils.contentsOfType(
                        op, null, null, Parameter.class);
                List<Parameter> args = new ArrayList<Parameter>();
                Parameter returnType = null;
                boolean suppressedWarnings = false;
                for(Parameter param: parameters) {
                    int upper = param.getMultiplicity().getUpper();
                    if (!suppressedWarnings && (upper < 0 || upper > 1)) {
                        emitSuppressWarningsAnnotation();
                        suppressedWarnings = true;
                    }
                    
                    if (param.getDirection() == DirectionKindEnum.RETURN_DIR) {
                        if (returnType != null) {
                            throw new GenerationException(
                                "Multiple return types for operation '" + 
                                op.getName() + "'");
                        }
                        returnType = param;
                    } else {
                        args.add(param);
                    }
                }
                
                String returnTypeName = "void";
                if (returnType != null) {
                    returnTypeName = CodeGenUtils.getTypeName(returnType);
                }

                write(
                    "public ",
                    returnTypeName,
                    " ",
                    op.getName(),
                    "(");
                if (!args.isEmpty()) {
                    newLine();
                    increaseIndent();
                    boolean first = true;
                    for(Parameter arg: args) {
                        if (!first) {
                            writeln(",");
                        } else {
                            first = false;
                        }
                        
                        String arrayBrackets = " ";
                        if (arg.getDirection() == DirectionKindEnum.OUT_DIR ||
                            arg.getDirection() == DirectionKindEnum.INOUT_DIR)
                        {
                            arrayBrackets = "[] ";
                        }

                        String[] paramInfo = CodeGenUtils.getParam(arg);
                        write(paramInfo[0], arrayBrackets, paramInfo[1]);                        
                    }
                    decreaseIndent();
                }
                write(")");
                List<?> exceptions = op.getExceptions();
                if (!exceptions.isEmpty()) {
                    newLine();
                    write("throws ");
                    boolean first = true;
                    for(MofException ex: 
                            GenericCollections.asTypedList(
                                exceptions, MofException.class))
                    {
                        if (!first) {
                            write(", ");
                        } else {
                            first = false;
                        }

                        write(
                            CodeGenUtils.getTypeName(
                                ex, ExceptionHandler.EXCEPTION_SUFFIX));
                    }
                }
                startBlock();
                if (returnType != null) {
                    write("return ");
                }
                write("super.", op.getName(), "(");
                if (!args.isEmpty()) {
                    newLine();
                    increaseIndent();
                    boolean first = true;
                    for(Parameter arg: args) {
                        if (!first) {
                            writeln(",");
                        } else {
                            first = false;
                        }
                        String[] paramInfo = CodeGenUtils.getParam(arg);
                        write(paramInfo[1]);
                    }
                    decreaseIndent();
                }
                writeln(");");
                endBlock();                
            }
            
            newLine();
            writeCheckConstraints(instanceAttributes, instanceReferences);
            
            generateCustomClassInstanceMethods(cls, refInfoMap.values(), unrefAssocRefInfoMap.values());
            writeEntityFooter();
        }
        finally {
            close();
        }
    }
    
    protected JavaClassReference getClassInstanceBaseClass(MofClass cls)
        throws GenerationException
    {
        return REF_OBJECT_IMPL_CLASS;
    }

    private void generateAssocExpression(MofClass fromCls, Association toAssoc)
    {
        if (fromCls.getContainer().equals(toAssoc.getContainer())) {
            write(
                "refImmediatePackage().refAssociation(",
                QUOTE, toAssoc.getName(), QUOTE, ")");
        } else {
            List<String> containerNames = new ArrayList<String>();
            MofPackage outerMostPkg = (MofPackage)fromCls.getContainer();
            while(outerMostPkg.getContainer() != null) {
                outerMostPkg = (MofPackage)outerMostPkg.getContainer();
            }
            MofPackage container = (MofPackage)toAssoc.getContainer();
            while(!container.equals(outerMostPkg)) {
                containerNames.add(container.getName());
                container = (MofPackage)container.getContainer();
            }
            
            Collections.reverse(containerNames);
            
            write("refOutermostPackage()");
            increaseIndent();
            for(String containerName: containerNames) {
                writeln();
                write(".refPackage(", QUOTE, containerName, QUOTE, ")");
            }
            if (!containerNames.isEmpty()) {
                writeln();
            }
            write(".refAssociation(",QUOTE, toAssoc.getName(), QUOTE, ")");
            decreaseIndent();
        }
        
    }
    
    protected void writeCheckConstraints(
        Collection<Attribute> attribs, Collection<Reference> refs)
    {
        startBlock(
            "protected void checkConstraints(",
            JAVA_UTIL_LIST_CLASS, 
            "<", JavaHandlerBase.JMI_EXCEPTION_CLASS, "> errors, ",
            "boolean deepVerify)");
        
        // NOTE: These checks are equivalent to Netbeans implementation.
        // More could be tested.
        
        for(Attribute attrib: attribs) {
            int lower = attrib.getMultiplicity().getLower(); 
            int upper = attrib.getMultiplicity().getUpper();
            if (lower == 1 && upper == 1) {
                startConditionalBlock(
                    CondType.IF,
                    CodeGenUtils.getClassFieldName(attrib.getName()),
                    " == null");
            } else if (upper != 1 && lower > 0) {
                // Check that lower-bounded multi-value attribute meets lower
                // bound
                startConditionalBlock(
                    CondType.IF, 
                    CodeGenUtils.getAccessorName(generator, attrib, false), 
                    "().size() < ",
                    lower);
            } else {
                continue;
            }
            
            // Find attribute.
            JavaClassReference ATTRIBUTE_CLASS = 
                new JavaClassReference(Attribute.class, false);
            writeln(
                ATTRIBUTE_CLASS, " attrib = findAttribute(",
                QUOTE, attrib.getName(), QUOTE, ");");
            writeln(
                "errors.add(new ", 
                JavaHandlerBase.WRONG_SIZE_EXCEPTION_CLASS, "(attrib));");
            endBlock();
        }
        
        for(Reference ref: refs) {
            int lower = ref.getMultiplicity().getLower(); 
            int upper = ref.getMultiplicity().getUpper();
            
            if (lower == 1 && upper == 1) {
                // Check that required single-value attribute has a value.
                startConditionalBlock(
                    CondType.IF, 
                    CodeGenUtils.getAccessorName(generator, ref), 
                    "() == null");
            } else if (upper != 1 && lower > 0) {
                // Check that lower-bounded multi-value attribute meets lower
                // bound
                startConditionalBlock(
                    CondType.IF, 
                    CodeGenUtils.getAccessorName(generator, ref), 
                    "().size() < ", lower);
            } else {
                continue;
            }

            AssociationEnd exposedEnd = ref.getExposedEnd();
            String assocName = exposedEnd.getContainer().getName();
            String exposedEndName = exposedEnd.getName();
            String referencedEndName = ref.getReferencedEnd().getName();
            JavaClassReference ASSOCIATION_END_CLASS = 
                new JavaClassReference(AssociationEnd.class, false);
            JavaClassReference REF_ASSOCIATION_BASE_CLASS = 
                new JavaClassReference(RefAssociationBase.class, false);
            writeln(
                ASSOCIATION_END_CLASS, " exposedEnd = findAssociationEnd(", 
                QUOTE, assocName, QUOTE, ", ", 
                QUOTE, exposedEndName, QUOTE, ");");
            writeln(
                ASSOCIATION_END_CLASS, " referencedEnd = findAssociationEnd(", 
                QUOTE, assocName, QUOTE, ", ", 
                QUOTE, referencedEndName, QUOTE, ");");
            writeln(
                "errors.add(", 
                REF_ASSOCIATION_BASE_CLASS,
                ".makeWrongSizeException(exposedEnd, referencedEnd, this));");
            endBlock();
        }
        
        endBlock();
    }
    
    /**
     * Allows subclasses to add additional interfaces to the implementation.
     * 
     * @return an array of JavaClassReferences: may be empty, must not be null
     */
    protected JavaClassReference[] getCustomInterfaces()
    {
        return new JavaClassReference[0];
    }
    
    /**
     * Allows subclasses to generate additional fields for class instance
     * objects.
     * 
     * @param cls class instance being generated
     */
    protected void generateCustomClassInstanceFields(MofClass cls)
        throws GenerationException
    {
    }
    
    /**
     * Allows subclasses to generate additional initialization for class 
     * instance objects. This method is invoked during the generation of the
     * given class's constructor.
     * 
     * @param cls class instance being generated
     *
     * @param nonDerivedAttributes non-derived attributes to be initialized
     * by this class
     *
     * @param withAttributes true for non-default constructor
     */
    protected void generateCustomClassInstanceInit(
        MofClass cls, List<Attribute> nonDerivedAttributes,
        boolean withAttributes)
        throws GenerationException
    {
    }
    
    /**
     * Allows subclasses to generate additional code for class instance mutator
     * methods. 
     * 
     * @param cls class instance being generated
     * @param attrib attribute for which a mutator is being generated
     */
    protected void generateCustomClassInstanceMutator(
        MofClass cls, Attribute attrib)
    throws GenerationException
    {
    }

    /**
     * Allows subclasses to generate additional methods for class instance
     * objects.
     * 
     * @param cls class instance being generated
     * @param references a collection of references to associations
     * @param unreferencedAssocs a collection of faked references to 
     *                           associations which have no References 
     */
    protected void generateCustomClassInstanceMethods(
        MofClass cls,
        Collection<ReferenceInfo> references,
        Collection<ReferenceInfo> unreferencedAssocs)
    throws GenerationException
    {
    }
    
    public void generateClassProxy(MofClass cls)
        throws GenerationException
    {
        if (getPassIndex() == 0) {
            return;
        }

        String interfaceName = 
            CodeGenUtils.getTypeName(cls, CLASS_PROXY_SUFFIX);
        
        String typeName = convertToTypeName(interfaceName);

        if (!isIncluded(cls)) {
            log.fine(
                "Skipping Excluded Class Proxy Implementation '" 
                + typeName + "'");
            return;
        }
        
        String instImplTypeName = 
            convertToTypeName(CodeGenUtils.getTypeName(cls));
        
        log.fine("Generating Class Proxy Implementation '" + typeName + "'");

        open(typeName);
        try {
            writeClassHeader(
                cls,
                typeName, 
                REF_CLASS_IMPL_CLASS.toString(),
                new String[] { interfaceName }, 
                JavaClassReference.computeImports(CLASS_PROXY_REFS),
                false,
                MOF_CLASS_PROXY_COMMENT);
            
            // constructor
            startConstructorBlock(
                cls, 
                new String[] { REF_PACKAGE_CLASS.toSimple() }, 
                new String[] { "container" }, 
                computeSuffix(CLASS_PROXY_SUFFIX));
            writeln("super(container);");
            newLine();
            generateCustomClassProxyInit(cls);
            endBlock();
            
            if (!cls.isAbstract()) {
                // No-arg factory method
                newLine();
                startCreatorBlock(cls, null, "");
                writeln(instImplTypeName, " instance = ");
                increaseIndent();
                writeln("new ", instImplTypeName, "(this);");
                decreaseIndent();
                writeln("register(instance);");
                writeln("return instance;");
                endBlock();
                
                Collection<Attribute> allAttributes =
                    CodeGenUtils.contentsOfType(
                        cls, 
                        HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                        ScopeKindEnum.INSTANCE_LEVEL,
                        Attribute.class);
                boolean hasCollectionAttrib = false;
                for(Iterator<Attribute> i = allAttributes.iterator(); 
                    i.hasNext(); ) 
                {
                    Attribute attrib = i.next();
                    
                    if (attrib.isDerived()) {
                        i.remove();
                        continue;
                    }
                    
                    int upper = attrib.getMultiplicity().getUpper();
                    if (upper < 0 || upper > 1) {
                        hasCollectionAttrib = true;
                    }
                }
                
                // factory method                
                if (allAttributes.size() > 0) {
                    newLine();
                    
                    if (hasCollectionAttrib) {
                        emitSuppressWarningsAnnotation();
                    }
                    ModelElement[] params = 
                        allAttributes.toArray(
                            new ModelElement[allAttributes.size()]);
                    startCreatorBlock(cls, params, "");  
                    writeln(instImplTypeName, " instance =");
                    increaseIndent();
                    writeln("new ", instImplTypeName, "(");
                    increaseIndent();
                    writeln("this,");
                    for(Iterator<Attribute> i = allAttributes.iterator(); 
                        i.hasNext(); ) 
                    {
                        Attribute attrib = i.next();
                        
                        String[] paramInfo = CodeGenUtils.getParam(attrib);

                        writeln(paramInfo[1], i.hasNext() ? "," : ");");
                    }
                    decreaseIndent();
                    decreaseIndent();
                    writeln("register(instance);");
                    writeln("return instance;");
                    endBlock();
                }
            }
            
            newLine();
            writeCheckConstraints();
            
            newLine();
            startBlock("public Class<?> getInstanceClass()");
            if (cls.isAbstract()) {
                writeln("return null;");
            } else {
                writeln("return ", instImplTypeName, ".class;");
            }
            endBlock();
            
            generateCustomClassProxyMethods(cls);
            
            writeEntityFooter();
        }
        finally {
            close();
        }
    }
    
    /**
     * Allows subclasses to perform additional initialization during 
     * construction of a class proxy object.
     * 
     * @param cls Class proxy constructor being generated
     */
    protected void generateCustomClassProxyInit(MofClass cls)
        throws GenerationException
    {
    }
    
    /**
     * Allows subclasses to generate additional methods for class proxy 
     * objects.
     * 
     * @param cls class proxy being generated
     */
    protected void generateCustomClassProxyMethods(MofClass cls)
        throws GenerationException
    {
    }
        
    public void generatePackage(MofPackage pkg)
        throws GenerationException
    {
        if (getPassIndex() == 0) {
            return;
        }
        
        // TODO: SWZ: 2008-07-07: Modify this to deal with aliased packages
        // in a manner similar to HibernateJavaHandler.  Currently does not
        // generate an addAliasedPackages() method.
        
        String interfaceName = 
            CodeGenUtils.getTypeName(pkg, PACKAGE_SUFFIX);
        
        String typeName = convertToTypeName(interfaceName);

        if (!isIncluded(pkg)) {
            log.fine(
                "Skipping Excluded Package Implementation '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Package Implementation '" + typeName + "'");

        open(typeName);
        try {
            writeClassHeader(
                pkg,
                typeName, 
                REF_PACKAGE_IMPL_CLASS.toString(),
                new String[] { interfaceName }, 
                JavaClassReference.computeImports(PACKAGE_REFS),
                false,
                MOF_PACKAGE_COMMENT);
            
            // Generate fields for all nested packages, class proxies and
            // associations.

            ArrayList<String> packageFieldNames = new ArrayList<String>();
            Collection<MofPackage> packages =
                CodeGenUtils.contentsOfType(pkg, MofPackage.class);
            boolean hasPackages = !packages.isEmpty();
            if (hasPackages) {
                writeln("// Packages");
            }
            for(MofPackage nestedPkg: packages) {
                String fieldName = 
                    writeField(
                        nestedPkg, "private", true, false, PACKAGE_SUFFIX);
                packageFieldNames.add(fieldName);
            }
            
            ArrayList<String> classFieldNames = new ArrayList<String>();
            Collection<MofClass> classes = 
                CodeGenUtils.contentsOfType(pkg, MofClass.class);
            boolean hasClasses = !classes.isEmpty();
            if (hasClasses) {
                if (hasPackages) {
                    newLine();
                }
                writeln("// Class Proxies");
            }
            for(MofClass cls: classes) {
                String fieldName =
                    writeField(
                        cls, "private", true, false, CLASS_PROXY_SUFFIX);
                classFieldNames.add(fieldName);
            }
            
            ArrayList<String> assocFieldNames = new ArrayList<String>();
            Collection<Association> assocs = 
                CodeGenUtils.contentsOfType(pkg, Association.class);
            boolean hasAssocs = !assocs.isEmpty();
            if (hasAssocs) {
                if (hasPackages || hasClasses) {
                    newLine();
                }
                writeln("// Associations");
            }
            for(Association assoc: assocs) {
                String fieldName =
                    writeField(assoc, "private", true, false, "");
                assocFieldNames.add(fieldName);
            }
            newLine();
            
            // constructor
            boolean isContained = pkg.getContainer() != null;
            if (isContained) {
                startBlock(
                    "public ",
                    CodeGenUtils.getSimpleTypeName(
                        pkg, computeSuffix(PACKAGE_SUFFIX)),
                    "(", REF_PACKAGE_CLASS, " container)");
                writeln("super(container);");
            } else {
                startBlock(
                    "public ",
                    CodeGenUtils.getSimpleTypeName(
                        pkg, computeSuffix(PACKAGE_SUFFIX)),
                    "()");
                writeln("super(null);");
            }
            newLine();
            
            generateCustomPackageInit(pkg);
            
            // initialize nested package fields
            Iterator<String> nameIter;
            Iterator<MofPackage> pkgIter;
            for(
                nameIter = packageFieldNames.iterator(),
                    pkgIter = packages.iterator();
                nameIter.hasNext() && pkgIter.hasNext(); )
            {
                String fieldName = nameIter.next();
                MofPackage nestedPkg = pkgIter.next();
                
                writeln(
                    "this.",
                    fieldName,
                    " = new ",
                    CodeGenUtils.getTypeName(
                        nestedPkg, computeSuffix(PACKAGE_SUFFIX)),
                    "(this);");
                writeln(
                    "super.addPackage(", 
                    QUOTE, nestedPkg.getName(), QUOTE, 
                    ", this.", fieldName, ");");
            }
            
            // initialize class proxy fields 
            if (hasClasses && hasPackages) {
                newLine();
            }
            
            Iterator<MofClass> clsIter;
            for(
                nameIter = classFieldNames.iterator(),
                    clsIter = classes.iterator();
                nameIter.hasNext() && clsIter.hasNext(); )
            {
                String fieldName = nameIter.next();
                MofClass nestedCls = clsIter.next();
                writeln(
                    "this.",
                    fieldName,
                    " = new ",
                    CodeGenUtils.getSimpleTypeName(
                        nestedCls, computeSuffix(CLASS_PROXY_SUFFIX)),
                    "(this);");
                writeln(
                    "super.addClass(", 
                    QUOTE, nestedCls.getName(), QUOTE, 
                    ", this.", fieldName, ");");
            }

            // initialize association fields
            if (hasAssocs && (hasPackages || hasClasses)) {
                newLine();
            }
            
            Iterator<Association> assocIter;
            for(
                nameIter = assocFieldNames.iterator(),
                    assocIter = assocs.iterator();
                nameIter.hasNext() && assocIter.hasNext(); )
            {
                String fieldName = nameIter.next();
                Association assoc = assocIter.next();
                writeln(
                    "this.",
                    fieldName,
                    " = new ",
                    CodeGenUtils.getSimpleTypeName(assoc, computeSuffix("")),
                    "(this);");
                writeln(
                    "super.addAssociation(", 
                    QUOTE, assoc.getName(), QUOTE, 
                    ", this.", fieldName, ");");
            }
            
            endBlock();
            newLine();
            
            // generate accessor methods

            if (hasPackages) {
                writeln("// Package Accessors");
                newLine();
            }

            // package accessors
            for(
                nameIter = packageFieldNames.iterator(),
                    pkgIter = packages.iterator();
                nameIter.hasNext() && pkgIter.hasNext(); )
            {
                MofPackage nestedPkg = pkgIter.next();
                startPackageAccessorBlock(nestedPkg, PACKAGE_SUFFIX);
                
                writeln("return ", nameIter.next(), ";");                

                endBlock();
                newLine();
            }
            
            // class proxy accessors
            if (hasClasses) {
                if (hasPackages) {
                    newLine();
                }
                writeln("// Class Proxy Accessors");
                newLine();
            }
            
            for(
                nameIter = classFieldNames.iterator(),
                    clsIter = classes.iterator();
                nameIter.hasNext() && clsIter.hasNext(); )
            {
                MofClass cls = clsIter.next();
                startPackageAccessorBlock(cls, CLASS_PROXY_SUFFIX);
                
                writeln("return ", nameIter.next(), ";");
                
                endBlock();
                newLine();
            }

            // association accessors
            if (hasAssocs) {
                if (hasPackages || hasClasses) {
                    newLine();
                }
                writeln("// Association Accessors");
                newLine();
            }
            for(
                nameIter = assocFieldNames.iterator(),
                    assocIter = assocs.iterator();
                nameIter.hasNext() && assocIter.hasNext(); )
            {
                Association assoc = assocIter.next();
                startPackageAccessorBlock(assoc, "");
                
                writeln("return ", nameIter.next(), ";");
                
                endBlock();
                newLine();
            }

            // structure type factory methods
            Collection<StructureType> structs = 
                CodeGenUtils.contentsOfType(
                    pkg, 
                    HierachySearchKindEnum.ENTITY_ONLY, 
                    StructureType.class);
            boolean hasStructs = !structs.isEmpty();
            if (hasStructs) {
                if (hasAssocs || hasPackages || hasClasses) {
                    newLine();
                }
                writeln("// StructureType factory methods");
                newLine();
            }
            for(StructureType struct: structs) {
                Collection<StructureField> structFields = 
                    CodeGenUtils.contentsOfType(
                        struct, 
                        HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                        StructureField.class);
                
                ModelElement[] args = 
                    structFields.toArray(
                        new ModelElement[structFields.size()]);
                startCreatorBlock(struct, args, false, "");
                write("return new ", CodeGenUtils.getSimpleTypeName(struct), "(");
                if (!structFields.isEmpty()) {
                    newLine();
                    increaseIndent();
                    boolean first = true;
                    for(StructureField field: structFields) {
                        if (!first) {
                            writeln(",");
                        } else {
                            first = false;
                        }
                        
                        String[] paramInfo = CodeGenUtils.getParam(field);
                        write(paramInfo[1]);
                    }
                    writeln(");");
                    decreaseIndent();
                }
                endBlock();
            }

            // TODO: MOF exceptions?
            
            newLine();
            writeCheckConstraints();            
            generateCustomPackageMethods(pkg);
            writeEntityFooter();
        }
        finally {
            close();
        }
    }
    
    /**
     * Allows subclasses to perform additional initialization during 
     * construction of a package proxy object.
     * 
     * @param pkg package being generated
     */
    protected void generateCustomPackageInit(MofPackage pkg)
        throws GenerationException
    {
    }
    
    /**
     * Allows subclasses to generate additional methods during package proxy 
     * generation.
     * 
     * @param pkg package being generated
     */
    protected void generateCustomPackageMethods(MofPackage pkg)
        throws GenerationException
    {
    }
    
    public void generateStructure(StructureType struct)
        throws GenerationException
    {
        String interfaceName = CodeGenUtils.getTypeName(struct);
        
        String typeName = convertToTypeName(interfaceName);

        if (!isIncluded(struct)) {
            log.fine(
                "Skipping Excluded Structure Implementation '" 
                + typeName + "'");
            return;
        }
        
        log.fine(
            "Generating Structure Implementation '" + typeName + "'");

        Collection<StructureField> fields =
            CodeGenUtils.contentsOfType(
                struct,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                StructureField.class);

        open(typeName);
        try {            
            String[] interfaces = new String[] { interfaceName };
            
            writeClassHeader(
                struct, 
                typeName,
                REF_STRUCT_IMPL_CLASS.toString(),
                interfaces,
                JavaClassReference.computeImports(STRUCT_REFS),
                false,
                MOF_CLASS_COMMENT);
            
            CRC32 crc = new CRC32();
            crc.update(typeName.getBytes());
            for(String iface: interfaces) {
                crc.update(iface.getBytes());
            }
            
            // fields
            writeln("// Fields");
            for(StructureField field: fields) {
                String fieldTypeName = CodeGenUtils.getTypeName(field);
                String fieldName = 
                    CodeGenUtils.getClassFieldName(field.getName());
                
                crc.update(fieldTypeName.getBytes());
                crc.update(fieldName.getBytes());
                
                writeln(
                    "public ", 
                    fieldTypeName,
                    " ",
                    fieldName,
                    ";");
            }
            newLine();
                        
            // constructor
            ModelElement[] args = 
                fields.toArray(new ModelElement[fields.size()]);
            startConstructorBlock(struct, args, "");
            writeln("super();");
            newLine();
            
            for(ModelElement arg: args) {
                String fieldName = CodeGenUtils.getClassFieldName(arg.getName());
                writeln("this.", fieldName, " = ", fieldName, ";");
            }
            
            endBlock();
                        
            // field methods
            for(StructureField field: fields) {
                newLine();
                startBlock(
                    "public ",
                    CodeGenUtils.getTypeName(field),
                    " ", 
                    CodeGenUtils.getAccessorName(generator, field, null), 
                    "() throws ",
                    JMI_EXCEPTION_CLASS);
                writeln(
                    "return ",
                    CodeGenUtils.getClassFieldName(field.getName()),
                    ";");
                endBlock();
            }
            
            // write serial version UID since RefStruct is Serializable 
            long uid = crc.getValue() | ((long)typeName.hashCode() << 32);
            
            newLine();
            writeln(
                "private static final long serialVersionUID = ", uid, "L;");
            
            newLine();
            writeCheckConstraints();
            
            newLine();
            writeln("// Implement RefStruct");
            startBlock("public ", JAVA_UTIL_LIST_CLASS, "<?> refFieldNames()");
            writeln("return ", JAVA_UTIL_ARRAYS_CLASS, ".asList(");
            increaseIndent();
            writeln("new String[] {");
            increaseIndent();
            for(StructureField field: fields) {
                String fieldName = 
                    CodeGenUtils.getClassFieldName(field.getName());
                writeln(QUOTE, fieldName, QUOTE, ",");
            }
            decreaseIndent();
            writeln("});");
            decreaseIndent();
            endBlock();
            
            writeEntityFooter();
        }
        finally {
            close();
        }
    }

    protected abstract String convertToTypeName(String entityName) 
        throws GenerationException;
    
    protected abstract String computeSuffix(String baseSuffix);
}

// End MofImplementationGenerator.java
