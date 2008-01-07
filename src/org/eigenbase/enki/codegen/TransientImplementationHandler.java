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

    private static final JavaClassReference REF_OBJECT_IMPL_CLASS =
        new JavaClassReference(RefObjectBase.class, true);
    
    private static final JavaClassReference REF_ASSOC_IMPL_CLASS =
        new JavaClassReference(RefAssociationBase.class, true);
    
    private static final JavaClassReference REF_CLASS_IMPL_CLASS =
        new JavaClassReference(RefClassBase.class, true);
    
    private static final JavaClassReference REF_PACKAGE_IMPL_CLASS =
        new JavaClassReference(RefPackageBase.class, true);
    
    private static final JavaClassReference REF_STRUCT_IMPL_CLASS =
        new JavaClassReference(RefStructBase.class, true);
    
    private static final JavaClassReference DEPENDS_ON_BASE_CLASS =
        new JavaClassReference(DependsOnBase.class, true);
    
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
    
    private static final JavaClassReference JAVA_UTIL_COLLECTION_CLASS =
        new JavaClassReference(Collection.class, true);

    private static final JavaClassReference JAVA_UTIL_COLLECTIONS_CLASS =
        new JavaClassReference(Collections.class, true);
    
    private static final JavaClassReference JAVA_UTIL_ITERATOR_CLASS =
        new JavaClassReference(Iterator.class, true);

    private static final JavaClassReference JMI_EXCEPTION_CLASS =
        new JavaClassReference(JmiException.class, true);
    
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
        DEPENDS_ON_BASE_CLASS,
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
    };
    
    private final Logger log = 
        Logger.getLogger(MofImplementationHandler.class.getName());

    public TransientImplementationHandler()
    {
        super();
        
        setDisplayHeaderWarning(false);
    }
    
    public void generateAssociation(Association assoc)
        throws GenerationException
    {
        String interfaceName = generator.getTypeName(assoc);
        
        String typeName = convertToTypeName(interfaceName);

        log.fine("Generating Association Implementation '" + typeName + "'");

        AssociationInfo assocInfo = new AssociationInfo(generator, assoc);
        
        String baseClass = REF_ASSOC_IMPL_CLASS.toString();
        if (!assocInfo.isChangeable(0) && !assocInfo.isChangeable(1)) {
            // DependsOn Associations require special handling: the existence
            // of dependencies between objects is intrinsic to the model, 
            // rather than the creation of arbitrary associations.
            if (!assoc.getName().equals("DependsOn")) {
                throw new GenerationException(
                    "Unhandled derived association: " + assoc.getName());
            }
            baseClass = DEPENDS_ON_BASE_CLASS.toString();
        }
        
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
            
            startBlock(
                "public ",
                generator.getSimpleTypeName(assoc, computeSuffix("")),
                "(", REF_PACKAGE_CLASS, " container)");
            writeln(
                "super(");
            increaseIndent();
            writeln("container,");
            writeln(QUOTE, assocInfo.getEndName(0), QUOTE, ",");
            writeln(MULTIPLICITY_CLASS, ".", end1Multiplicity, ",");
            writeln(QUOTE, assocInfo.getEndName(1), QUOTE, ",");
            writeln(MULTIPLICITY_CLASS, ".", end2Multiplicity, ");");
            decreaseIndent();
            endBlock();
            newLine();

            // exists
            startGenericMethodBlock(
                assoc,
                "boolean",
                "exists", 
                assocInfo.getEndTypes(),
                assocInfo.getEndNames());
            writeln(
                "return super.refLinkExists(", 
                assocInfo.getEndName(0),
                ", ",
                assocInfo.getEndName(1), ");");
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
                    assocInfo.getEndNames());
                writeln(
                    "return super.refAddLink(", 
                    assocInfo.getEndName(0), ", ", 
                    assocInfo.getEndName(1), ");");
                endBlock();

                newLine();
                
                // remove
                startGenericMethodBlock(
                    assoc,
                    "boolean",
                    "remove",
                    assocInfo.getEndTypes(),
                    assocInfo.getEndNames());
                writeln(
                    "return super.refRemoveLink(", 
                    assocInfo.getEndName(0), ", ", 
                    assocInfo.getEndName(1), ");");
                endBlock();
            }
            
            writeEntityFooter();
        }
        finally {
            close();
        }            
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
                    : generator.getCollectionType(
                        ordered 
                            ? ORDERED_COLLECTION_CLASS
                            : COLLECTION_CLASS,
                        assocInfo.getEndType(getIndex)),
                generator.getAccessorName(assocInfo.getEnd(getIndex), null),
                new String[] { assocInfo.getEndType(fromIndex) },
                new String[] { assocInfo.getEndName(fromIndex) });
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
                assocInfo.getEndName(fromIndex), ");");
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
        String interfaceName = generator.getTypeName(cls);
        
        String typeName = convertToTypeName(interfaceName);

        if (cls.isAbstract()) {
            log.fine(
                "Skipping Class Instance Implementation '" + typeName + "'");
            return;
        }
        
        log.fine(
            "Generating Class Instance Implementation '" + typeName + "'");

        Collection<Reference> instanceReferences =
            contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS,
                Reference.class);

        open(typeName);
        try {            
            String[] interfaces = new String[] { interfaceName };
            
            writeClassHeader(
                cls, 
                typeName,
                REF_OBJECT_IMPL_CLASS.toString(),
                interfaces,
                JavaClassReference.computeImports(CLASS_INSTANCE_REFS),
                false,
                MOF_CLASS_COMMENT);
            
            // fields
            writeln("// Attribute Fields");
            Collection<Attribute> instanceAttributes =
                contentsOfType(
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
                String fieldName = writePrivateField(attrib, false, false);
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
                ReferenceInfo refInfo = new ReferenceInfo(generator, ref);

                writePrivateField(
                    refInfo.getAssocInterfaceName(),
                    refInfo.getFieldName(), 
                    false, 
                    false);
                
                refInfoMap.put(ref, refInfo);
            }
            newLine();
            
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
                        generator.getTypeName(attrib.getType());
                    
                    String collTypeName;
                    if (mult.isOrdered()) {
                        collTypeName = 
                            generator.getCollectionType(
                                ORDERED_COLLECTION_IMPL_CLASS, elemTypeName);
                    } else {
                        collTypeName = 
                            generator.getCollectionType(
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

                    writeln(
                        "this.",
                        refInfo.getFieldName(),
                        " = (", refInfo.getAssocInterfaceName(), 
                        ")refImmediatePackage().refAssociation(",
                        QUOTE, refInfo.getAssoc().getName(), QUOTE,
                        ");");
                }
            }
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
                    String[] paramInfo = generator.getParam(params[i]);
                    paramTypes[i + 1] = paramInfo[0];
                    paramNames[i + 1] = paramInfo[1];
                }
                startConstructorBlock(
                    cls, paramTypes, paramNames, computeSuffix(""));
                writeln("this(refClass);");
                newLine();
                for(Attribute attrib: nonDerivedAttribs) {
                    String fieldName = nonDerivedAttribNames.get(attrib);
                    String[] paramInfo = generator.getParam(attrib);
                    MultiplicityType mult = attrib.getMultiplicity();
                    int upper = mult.getUpper();
                    if (upper == -1 || upper > 1) {
                        // Copy the collection
                        writeln(
                            "this.",
                            fieldName,
                            ".addAll(",
                            paramInfo[1],
                            ");");
                    } else {
                        writeln(
                            "this.",
                            fieldName,
                            " = ",
                            paramInfo[1],
                            ";");
                    }       
                }
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
                        generator.getAccessorName(attrib), 
                        "();");
                    endBlock();
                    
                    if (upper == 1 && attrib.isChangeable()) {
                        assert(false): "Changeable derived attrib";
                    }
                    continue;
                }
                
                String fieldName = nonDerivedAttribNames.get(attrib);
                
                if (upper == 1) {
                    newLine();
                    startAccessorBlock(attrib, true);
                    writeln("return ", fieldName, ";");
                    endBlock();
                    
                    if (attrib.isChangeable()) {
                        newLine();
                        startMutatorBlock(attrib);
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
                writeln(
                    "return ",
                    refInfo.getFieldName(),
                    ".",
                    refInfo.getAccessorName(), "(this);");
                endBlock();
                
                if (refInfo.isSingle() && refInfo.isChangeable()) {
                    newLine();
                    startMutatorBlock(ref);
                    if (refInfo.isReferencedEndFirst()) {
                        writeln(
                            refInfo.getFieldName(),
                            ".add(newValue, this);");
                    } else {
                        writeln(
                            refInfo.getFieldName(),
                            ".add(this, newValue);");                        
                    }
                    endBlock();
                }
            }
            
            // Operation methods
            // NOTE: MOF operations do not use generic types, so we can't emit
            // methods that use them here.
            Collection<Operation> operations =
                contentsOfType(
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
                    contentsOfType(op, null, null, Parameter.class);
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
                    returnTypeName = generator.getTypeName(returnType);
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

                        String[] paramInfo = generator.getParam(arg);
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
                            generator.getTypeName(
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
                        String[] paramInfo = generator.getParam(arg);
                        write(paramInfo[1]);
                    }
                    decreaseIndent();
                }
                writeln(");");
                endBlock();                
            }
            
            writeEntityFooter();
        }
        finally {
            close();
        }
    }

    public void generateClassProxy(MofClass cls)
        throws GenerationException
    {
        String interfaceName = generator.getTypeName(cls, CLASS_PROXY_SUFFIX);
        
        String typeName = convertToTypeName(interfaceName);

        String instImplTypeName = 
            convertToTypeName(generator.getTypeName(cls));
        
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
            endBlock();
            
            if (!cls.isAbstract()) {
                // No-arg factory method
                newLine();
                startCreatorBlock(cls, null, "");
                writeln("return new ", instImplTypeName, "(this);");
                endBlock();
                
                Collection<Attribute> allAttributes =
                    contentsOfType(
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
                    writeln(
                        "return new ",
                        instImplTypeName,
                        "(");
                    increaseIndent();
                    writeln("this,");
                    for(Iterator<Attribute> i = allAttributes.iterator(); 
                        i.hasNext(); ) 
                    {
                        Attribute attrib = i.next();
                        
                        String[] paramInfo = generator.getParam(attrib);

                        writeln(paramInfo[1], i.hasNext() ? "," : ");");
                    }
                    decreaseIndent();
                    endBlock();
                }
            }

            writeEntityFooter();
        }
        finally {
            close();
        }
    }

    public void generatePackage(MofPackage pkg)
        throws GenerationException
    {
        String interfaceName = 
            generator.getTypeName(pkg, PACKAGE_SUFFIX);
        
        String typeName = convertToTypeName(interfaceName);

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
                contentsOfType(pkg, MofPackage.class);
            boolean hasPackages = !packages.isEmpty();
            if (hasPackages) {
                writeln("// Packages");
            }
            for(MofPackage nestedPkg: packages) {
                String fieldName = 
                    writePrivateField(
                        nestedPkg, true, false, PACKAGE_SUFFIX);
                packageFieldNames.add(fieldName);
            }
            
            ArrayList<String> classFieldNames = new ArrayList<String>();
            Collection<MofClass> classes = contentsOfType(pkg, MofClass.class);
            boolean hasClasses = !classes.isEmpty();
            if (hasClasses) {
                if (hasPackages) {
                    newLine();
                }
                writeln("// Class Proxies");
            }
            for(MofClass cls: classes) {
                String fieldName =
                    writePrivateField(
                        cls, true, false, CLASS_PROXY_SUFFIX);
                classFieldNames.add(fieldName);
            }
            
            ArrayList<String> assocFieldNames = new ArrayList<String>();
            Collection<Association> assocs = 
                contentsOfType(pkg, Association.class);
            boolean hasAssocs = !assocs.isEmpty();
            if (hasAssocs) {
                if (hasPackages || hasClasses) {
                    newLine();
                }
                writeln("// Associations");
            }
            for(Association assoc: assocs) {
                String fieldName =
                    writePrivateField(assoc, true, false, "");
                assocFieldNames.add(fieldName);
            }
            newLine();
            
            // constructor
            boolean isContained = pkg.getContainer() != null;
            if (isContained) {
                startBlock(
                    "public ",
                    generator.getSimpleTypeName(
                        pkg, computeSuffix(PACKAGE_SUFFIX)),
                    "(", REF_PACKAGE_CLASS, " container)");
                writeln("super(container);");
            } else {
                startBlock(
                    "public ",
                    generator.getSimpleTypeName(
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
                writeln(
                    "this.",
                    nameIter.next(),
                    " = new ",
                    generator.getSimpleTypeName(
                        pkgIter.next(), computeSuffix(PACKAGE_SUFFIX)),
                    "(this);");
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
                writeln(
                    "this.",
                    nameIter.next(),
                    " = new ",
                    generator.getSimpleTypeName(
                        clsIter.next(), computeSuffix(CLASS_PROXY_SUFFIX)),
                    "(this);");
            }

            // initialize association fields
            if (hasAssocs && 
                (hasPackages || hasClasses))
            {
                newLine();
            }
            
            Iterator<Association> assocIter;
            for(
                nameIter = assocFieldNames.iterator(),
                    assocIter = assocs.iterator();
                nameIter.hasNext() && assocIter.hasNext(); )
            {
                Association assoc = assocIter.next();
                writeln(
                    "this.",
                    nameIter.next(),
                    " = new ",
                    generator.getSimpleTypeName(assoc, computeSuffix("")),
                    "(this);");
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
                contentsOfType(
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
                    contentsOfType(
                        struct, 
                        HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                        StructureField.class);
                
                ModelElement[] args = 
                    structFields.toArray(
                        new ModelElement[structFields.size()]);
                startCreatorBlock(struct, args, false, "");
                write("return new ", generator.getSimpleTypeName(struct), "(");
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
                        
                        String[] paramInfo = generator.getParam(field);
                        write(paramInfo[1]);
                    }
                    writeln(");");
                    decreaseIndent();
                }
                endBlock();
            }

            // TODO: exceptions?
            
            writeEntityFooter();
        }
        finally {
            close();
        }
    }
    
    protected void generateCustomPackageInit(MofPackage pkg)
    {
    }
    
    public void generateStructure(StructureType struct)
        throws GenerationException
    {
        String interfaceName = generator.getTypeName(struct);
        
        String typeName = convertToTypeName(interfaceName);

        log.fine(
            "Generating Structure Implementation '" + typeName + "'");

        Collection<StructureField> fields =
            contentsOfType(
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
                String fieldTypeName = generator.getTypeName(field);
                String fieldName = 
                    generator.getClassFieldName(field.getName());
                
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
                String fieldName = generator.getClassFieldName(arg.getName());
                writeln("this.", fieldName, " = ", fieldName, ";");
            }
            
            endBlock();
            
            
            // field methods
            for(StructureField field: fields) {
                newLine();
                startBlock(
                    "public ",
                    generator.getTypeName(field),
                    " ", 
                    generator.getAccessorName(field, null), 
                    "() throws ",
                    JMI_EXCEPTION_CLASS);
                writeln(
                    "return ",
                    generator.getClassFieldName(field.getName()),
                    ";");
                endBlock();
            }
            
            // write serial version UID since RefStruct is Serializable 
            long uid = crc.getValue() | ((long)typeName.hashCode() << 32);
            
            newLine();
            writeln(
                "private static final long serialVersionUID = ", uid, "L;");
            
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