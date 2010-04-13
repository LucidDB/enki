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

import javax.jmi.model.*;

import org.eigenbase.enki.util.*;

/**
 * JmiTemplateHandler implements all code generation handlers and generates 
 * the standard JMI template for each type in the given directory.
 * 
 * <p>Note: Support for all template types is not yet implemented.
 * Any Constant, MofException, Operation, or StructureType will cause 
 * {@link UnsupportedOperationException} be thrown.  Classifier scope
 * Attributes are also not supported.
 * 
 * @author Stephan Zuercher
 */
public class JmiTemplateHandler
    extends JavaHandlerBase
    implements AssociationHandler, ClassInstanceHandler, ClassProxyHandler,
               EnumerationClassHandler, EnumerationInterfaceHandler, 
               ExceptionHandler, PackageHandler, StructureHandler
{
    public static final JavaClassReference COLLECTION_CLASS = 
        new JavaClassReference(Collection.class);
    
    public static final JavaClassReference ORDERED_COLLECTION_CLASS = 
        new JavaClassReference(List.class);
    
    private static final JavaClassReference ORDERED_COLLECTION_CLASS_IMPL = 
        new JavaClassReference(ArrayList.class);

    public static final JavaClassReference MAP_CLASS = 
        new JavaClassReference(Map.class);
    public static final JavaClassReference MAP_IMPL_CLASS = 
        new JavaClassReference(HashMap.class);

    public static final JavaClassReference STRING_CLASS = 
        new JavaClassReference(String.class, true);
    
    private final Logger log = 
        Logger.getLogger(JmiTemplateHandler.class.getName());

    public JmiTemplateHandler()
    {
    }
    
    public void generateAssociation(Association assoc)
        throws GenerationException
    {
        String typeName = CodeGenUtils.getTypeName(assoc);

        if (!isIncluded(assoc)) {
            log.fine("Skipping Excluded Association '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Association '" + typeName + "'");
        
        AssociationEnd[] ends = CodeGenUtils.getAssociationEnds(assoc);
        
        String[] names = new String[] {
            CodeGenUtils.getSimpleTypeName(ends[0]),
            CodeGenUtils.getSimpleTypeName(ends[1])
        };
        
        String[] types = new String[] {
            CodeGenUtils.getTypeName(ends[0].getType()),
            CodeGenUtils.getTypeName(ends[1].getType())
        };

        String[] paramDescs = new String[] {
            ASSOC_GENERIC_PARAM1_COMMENT, ASSOC_GENERIC_PARAM2_COMMENT
        };
        
        open(typeName);
        try {
            writeInterfaceHeader(
                assoc, 
                typeName,
                new JavaClassReference[] { REF_ASSOC_CLASS }, 
                ASSOC_PROXY_COMMENT);
        
            // exists
            writeAbstractGenericMethod(
                assoc,
                ASSOC_EXISTS_COMMENT, 
                "boolean",
                ASSOC_EXISTS_RETURN_COMMENT, 
                "exists", 
                types,
                names,
                paramDescs);
            newLine();
            
            // REVIEW: SWZ: 11/7/2007: Netbeans MDR doesn't output generic
            // types for a multiplicity with upper bound != 1.  Will cause
            // diffs if you compare the interfaces.
            
            // get end1 from end2
            if (ends[0].isNavigable()) {
                boolean single = ends[0].getMultiplicity().getUpper() == 1;
                boolean ordered = ends[0].getMultiplicity().isOrdered();
                writeAbstractGenericMethod(
                    assoc,
                    single 
                        ? ASSOC_GET_END_SINGLE_COMMENT 
                        : ASSOC_GET_END_MULTI_COMMENT,
                    single 
                        ? types[0]
                        : CodeGenUtils.getCollectionType(
                            ordered 
                                ? ORDERED_COLLECTION_CLASS 
                                : COLLECTION_CLASS,
                            types[0]),
                    single 
                        ? ASSOC_GET_END_RETURN_SINGLE_COMMENT 
                        : ordered 
                            ? ASSOC_GET_END_RETURN_ORDERED_COMMENT 
                            : ASSOC_GET_END_RETURN_MULTI_COMMENT,
                            CodeGenUtils.getAccessorName(
                                generator, ends[0], null),
                    new String[] { types[1] },
                    new String[] { names[1] },
                    new String[] { ASSOC_GET_END2_PARAM_COMMENT });
                newLine();
            }
            
            // get end2 from end1
            if (ends[1].isNavigable()) {
                boolean single = ends[1].getMultiplicity().getUpper() == 1;
                boolean ordered = ends[1].getMultiplicity().isOrdered();
                writeAbstractGenericMethod(
                    assoc,
                    single 
                        ? ASSOC_GET_END_SINGLE_COMMENT 
                        : ASSOC_GET_END_MULTI_COMMENT,
                    single 
                        ? types[1]
                        : CodeGenUtils.getCollectionType(
                            ordered 
                                ? ORDERED_COLLECTION_CLASS 
                                : COLLECTION_CLASS,
                            types[1]),
                    single 
                        ? ASSOC_GET_END_RETURN_SINGLE_COMMENT 
                        : ordered 
                            ? ASSOC_GET_END_RETURN_ORDERED_COMMENT 
                            : ASSOC_GET_END_RETURN_MULTI_COMMENT,
                            CodeGenUtils.getAccessorName(
                                generator, ends[1], null),
                    new String[] { types[0] },
                    new String[] { names[0] },
                    new String[] { ASSOC_GET_END1_PARAM_COMMENT });
                newLine();
            }

            if (ends[0].isChangeable() && ends[1].isChangeable()) {
                // add
                writeAbstractGenericMethod(
                    assoc,
                    ASSOC_ADD_COMMENT, 
                    "boolean",
                    null,
                    "add",
                    types,
                    names,
                    paramDescs);
                newLine();
                
                // remove
                writeAbstractGenericMethod(
                    assoc,
                    ASSOC_REMOVE_COMMENT, 
                    "boolean",
                    null,
                    "remove",
                    types,
                    names,
                    paramDescs);
            }
            
            writeEntityFooter();
        }
        finally {
            close();
        }
    }


    public void generateClassInstance(MofClass cls)
        throws GenerationException
    {
        String typeName = CodeGenUtils.getTypeName(cls);

        if (!isIncluded(cls)) {
            log.fine("Skipping Excluded Class Instance '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Class Instance '" + typeName + "'");
        
        open(typeName);
        try {
            String[] superClassNames =
                listToStrings(cls.getSupertypes(), ModelElement.class, "");
            if (superClassNames.length == 0) {
                superClassNames = new String[] { REF_OBJECT_CLASS.toString() };
            }
            
            writeInterfaceHeader(
                cls, 
                typeName,
                superClassNames,
                CLASS_COMMENT);
            
            // constants
            Collection<Constant> instanceConstants = 
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.ENTITY_ONLY, 
                    Constant.class);
            if (!instanceConstants.isEmpty()) {
                throw new UnsupportedOperationException(
                    "class instance Constant not supported");
            }
            
            // operations
            Collection<Operation> instanceOperations =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.ENTITY_ONLY, 
                    VisibilityKindEnum.PUBLIC_VIS, 
                    ScopeKindEnum.INSTANCE_LEVEL, 
                    Operation.class);
            if (!instanceOperations.isEmpty()) {
                throw new UnsupportedOperationException(
                    "class instance Operation not supported");
            }
            
            // attributes
            Collection<Attribute> instanceAttributes =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.ENTITY_ONLY, 
                    VisibilityKindEnum.PUBLIC_VIS,
                    ScopeKindEnum.INSTANCE_LEVEL,
                    Attribute.class);
            for(Attribute attrib: instanceAttributes) {
                int upper = attrib.getMultiplicity().getUpper();
                if (upper == 1) {
                    writeAbstractAccessor(
                        attrib,
                        ATTRIB_ACCESSOR_COMMENT,
                        ATTRIB_ACCESSOR_RETURN_COMMENT);
                    
                    if (attrib.isChangeable()) {
                        writeAbstractMutator(
                            attrib,
                            ATTRIB_MUTATOR_COMMENT);
                    }
                } else if (upper != 0) {
                    writeAbstractAccessor(
                        attrib,
                        ATTRIB_ACCESSOR_COMMENT,
                        ATTRIB_ACCESSOR_RETURN_MULTI_COMMENT);
                }
            }
            
            // references
            Collection<Reference> instanceReferences =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.ENTITY_ONLY, 
                    VisibilityKindEnum.PUBLIC_VIS,
                    Reference.class);
            for(Reference ref: instanceReferences) {
                int upper = ref.getMultiplicity().getUpper();
                if (upper == 1) {
                    writeAbstractAccessor(
                        ref,
                        REF_ACCESSOR_COMMENT,
                        REF_ACCESSOR_RETURN_COMMENT);
                    
                    if (ref.isChangeable()) {
                        writeAbstractMutator(
                            ref,
                            REF_MUTATOR_COMMENT);
                    }
                } else if (upper != 0) {
                    writeAbstractAccessor(
                        ref,
                        REF_ACCESSOR_COMMENT,
                        REF_ACCESSOR_RETURN_MULTI_COMMENT);
                }
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
        String typeName = CodeGenUtils.getTypeName(cls, CLASS_PROXY_SUFFIX);

        if (!isIncluded(cls)) {
            log.fine("Skipping Excluded Class Proxy '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Class Proxy '" + typeName + "'");
        
        open(typeName);
        try {
            writeInterfaceHeader(
                cls, 
                typeName,
                new JavaClassReference[] { REF_CLASS_CLASS },
                CLASS_PROXY_COMMENT);
            
            if (!cls.isAbstract()) {
                // Generate create methods
                writeAbstractCreator(
                    cls, 
                    CLASS_PROXY_CREATE_COMMENT, 
                    CLASS_PROXY_CREATE_RETURN_COMMENT, 
                    null, 
                    "");

                Collection<Attribute> allAttributes =
                    CodeGenUtils.contentsOfType(
                        cls, 
                        HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                        ScopeKindEnum.INSTANCE_LEVEL,
                        Attribute.class);
                for(Iterator<Attribute> i = allAttributes.iterator(); 
                    i.hasNext(); ) 
                {
                    Attribute attrib = i.next();
                    
                    if (attrib.isDerived()) {
                        i.remove();
                    }
                }
                
                if (allAttributes.size() > 0) {
                    ModelElement[] params = 
                        allAttributes.toArray(
                            new ModelElement[allAttributes.size()]);
                    writeAbstractCreator(
                        cls, 
                        CLASS_PROXY_CREATE_ARGS_COMMENT,
                        CLASS_PROXY_CREATE_RETURN_COMMENT, 
                        params, 
                        "");
                }
            }
            
            Collection<Operation> allClassOps = 
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    ScopeKindEnum.CLASSIFIER_LEVEL, 
                    Operation.class);
            if (!allClassOps.isEmpty()) {
                throw new UnsupportedOperationException(
                    "Operation not supported");
            }
            
            Collection<Attribute> allClassAttributes =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    ScopeKindEnum.CLASSIFIER_LEVEL, 
                    Attribute.class);
            if (!allClassAttributes.isEmpty()) {
                throw new UnsupportedOperationException(
                    "class-level Attributes not supported");
            }
            
            Collection<StructureType> structTypes =
                CodeGenUtils.contentsOfType(
                    cls, 
                    HierachySearchKindEnum.ENTITY_ONLY,
                    StructureType.class);
            if (!structTypes.isEmpty()) {
                throw new UnsupportedOperationException(
                    "StructureType not supported");
            }
                
            writeEntityFooter();
        }
        finally {
            close();
        }
    }

    public void generateEnumerationInterface(
        EnumerationType enumType)
        throws GenerationException
    {
        String typeName = CodeGenUtils.getTypeName(enumType);
        
        if (!isIncluded(enumType)) {
            log.fine(
                "Skipping Excluded Enumeration Interface '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Enumeration Interface '" + typeName + "'");
        
        open(typeName);
        try {
            writeInterfaceHeader(
                enumType, 
                typeName,
                new JavaClassReference[] { REF_ENUM_CLASS },
                ENUM_INTERFACE_COMMENT);

            writeEntityFooter();
        }
        finally {
            close();
        }
    }

    public void generateEnumerationClass(
        EnumerationType enumType)
        throws GenerationException
    {
        String interfaceTypeName = CodeGenUtils.getSimpleTypeName(enumType);
        
        String typeName = 
            CodeGenUtils.getTypeName(enumType, ENUM_CLASS_SUFFIX);

        if (!isIncluded(enumType)) {
            log.fine("Skipping Excluded Enumeration Class '" + typeName + "'");
            return;
        }
        
        String simpleTypeName = 
            CodeGenUtils.getSimpleTypeName(enumType, ENUM_CLASS_SUFFIX);
        
        log.fine("Generating Enumeration Class '" + typeName + "'");
        
        open(typeName);
        try {
            writeClassHeader(
                enumType, 
                typeName,
                null,
                new String[] { interfaceTypeName },
                true,
                ENUM_CLASS_COMMENT);

            List<?> labels = enumType.getLabels();
            for(String literal: 
                    GenericCollections.asTypedList(labels, String.class))
            {
                writeMethodJavaDoc(
                    enumType,
                    ENUM_LITERAL_COMMENT, 
                    false,
                    null, 
                    null,
                    null,
                    new String[] { literal });
                writeln(
                    "public static final ",
                    simpleTypeName,
                    " ",
                    CodeGenUtils.getEnumFieldName(literal),
                    " = new ",
                    simpleTypeName,
                    "(",
                    QUOTE,
                    literal,
                    QUOTE,
                    ");");
                newLine();
            }
            
            // private fields
            writeln(
                "private static final ",
                CodeGenUtils.getMapType(
                    MAP_CLASS, STRING_CLASS.toString(), simpleTypeName),
                " valueMap;");
            // static initializer for valueMap
            startStmtBlock("static {");
            writeln(
                "valueMap = new ", 
                CodeGenUtils.getMapType(
                    MAP_IMPL_CLASS, STRING_CLASS.toString(), simpleTypeName),
                    "();");
            for(String literal: 
                    GenericCollections.asTypedList(labels, String.class))
            {
                writeln(
                    "valueMap.put(", 
                    QUOTE, literal, QUOTE, ", ", 
                    CodeGenUtils.getEnumFieldName(literal), ");");
            }
            endStmtBlock();
            newLine();
            
            writeln(
                "private static final ",
                CodeGenUtils.getCollectionType(
                    ORDERED_COLLECTION_CLASS, STRING_CLASS.toString()),
                " typeName;");
            // static initializer for typeName
            startStmtBlock("static {");
            writeln(
                CodeGenUtils.getCollectionType(
                    ORDERED_COLLECTION_CLASS_IMPL, "String"),
                " temp = new ",
                CodeGenUtils.getCollectionType(
                    ORDERED_COLLECTION_CLASS_IMPL, "String"),
                "();");
            
            StringBuffer fullyQualifiedName = new StringBuffer();
            for(String part: 
                    GenericCollections.asTypedList(
                        enumType.getQualifiedName(), String.class)) 
            {
                // Add part to the temp collection
                writeln("temp.add(\"", part, "\");");
                
                // build a string containing the fully qualified name
                if (fullyQualifiedName.length() != 0) {
                    fullyQualifiedName.append('.');
                }
                fullyQualifiedName.append(part);
            }
            
            writeln(
        		"typeName = java.util.Collections.unmodifiableList(temp);");
            
            endStmtBlock();
            newLine();
            
            writeln("private final String literalName;");
            newLine();
            
            // constructor
            startBlock(
                "private ",
                simpleTypeName,
                "(String literalName)");
            writeln("this.literalName = literalName;");
            endBlock();
            newLine();
            
            // refTypeName
            writeTrivialAccessor(
                enumType,
                ENUM_REF_TYPE_NAME_COMMENT, 
                ENUM_REF_TYPE_NAME_RETURN_COMMENT, 
                CodeGenUtils.getCollectionType(
                    ORDERED_COLLECTION_CLASS, "String"),
                "refTypeName",
                "typeName");

            // toString
            writeTrivialAccessor(
                enumType,                
                ENUM_TO_STRING_COMMENT, 
                ENUM_TO_STRING_RETURN_COMMENT, 
                "String",
                "toString",
                "literalName");

            // hashCode
            writeTrivialAccessor(
                enumType,
                ENUM_HASH_CODE_COMMENT, 
                ENUM_HASH_CODE_RETURN_COMMENT, 
                "int",
                "hashCode",
                "literalName.hashCode()");

            // equals
            writeMethodJavaDoc(
                enumType,
                ENUM_EQUALS_COMMENT,
                false,
                ENUM_EQUALS_RETURN_COMMENT,
                new String[] { "o" },
                new String[] { ENUM_EQUALS_PARAM_COMMENT },
                null);
            startBlock("public boolean equals(Object o)");
            
            startStmtBlock("if (o instanceof ", simpleTypeName, ") {");
            writeln("return o == this;");
            restartStmtBlock("} else if (o instanceof ", interfaceTypeName, ") {");
            writeln("return o.toString().equals(literalName);");
            restartStmtBlock("} else {");
            writeln("return ");
            increaseIndent();
            writeln("o instanceof javax.jmi.reflect.RefEnum &&");
            writeln("((javax.jmi.reflect.RefEnum) o).refTypeName().equals(typeName) &&");
            writeln("o.toString().equals(literalName);");
            decreaseIndent();
            endStmtBlock();
            
            endBlock();
            newLine();
            
            // forName
            writeMethodJavaDoc(
                enumType,
                ENUM_FOR_NAME_COMMENT,
                false,
                ENUM_FOR_NAME_RETURN_COMMENT,
                new String[] { "name" },
                new String[] { ENUM_FOR_NAME_PARAM_COMMENT },
                null);
            startBlock(
                "public static ", interfaceTypeName, " forName(String name)");
            writeln(simpleTypeName, " value = valueMap.get(name);");
            startConditionalBlock(CondType.IF, "value == null");            
            writeln("throw new IllegalArgumentException(");
            increaseIndent();
            writeln(
                QUOTE, "Unknown literal name '", QUOTE, " + name + ", QUOTE, 
                "' for enumeration '",
                fullyQualifiedName.toString(),
                "'", QUOTE, ");");
            decreaseIndent();
            endBlock();
            writeln("return value;");
            endBlock();
            newLine();
            
            // readResolve
            writeMethodJavaDoc(
                enumType,
                ENUM_READ_RESOLVE_COMMENT,
                false,
                ENUM_READ_RESOLVE_RETURN_COMMENT,
                null,
                null,
                null);
            startBlock(
                "protected Object readResolve() throws java.io.ObjectStreamException");
            startStmtBlock("try {");
            writeln("return forName(literalName);");
            endStmtBlock();
            startStmtBlock("catch(IllegalArgumentException e) {");
            writeln("throw new java.io.InvalidObjectException(e.getMessage());");
            endStmtBlock();
            endBlock();
            
            writeEntityFooter();
        }
        finally {
            close();
        }

    }

    public void generateException(MofException ex)
        throws GenerationException
    {
        String typeName = 
            CodeGenUtils.getTypeName(
                ex, 
                ex.getName().endsWith(EXCEPTION_SUFFIX) 
                    ? ""
                    : EXCEPTION_SUFFIX);
        
        if (!isIncluded(ex)) {
            log.fine("Skipping Excluded Association '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Exception '" + typeName + "'");

        throw new UnsupportedOperationException(
            "exception generation not supported");
    }

    public void generatePackage(MofPackage pkg)
        throws GenerationException
    {
        String typeName = CodeGenUtils.getTypeName(pkg, PACKAGE_SUFFIX);

        if (!isIncluded(pkg)) {
            log.fine("Skipping Excluded Package '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Package '" + typeName + "'");

        open(typeName);
        try {
            String[] superClassNames =
                listToStrings(
                    pkg.getSupertypes(), ModelElement.class, PACKAGE_SUFFIX);
            if (superClassNames.length == 0) {
                superClassNames = 
                    new String[] { REF_PACKAGE_CLASS.toString() };
            }
            
            writeInterfaceHeader(
                pkg, 
                typeName,
                superClassNames,
                PACKAGE_COMMENT);
            
            // REVIEW: SWZ: 11/7/2007: Could just output these items as we
            // come across them, but Netbeans' implementation produces them in
            // groups: imports, nested packages, classes, associations, and
            // finally structure types.  We'll do the same so that we can
            // compare our output to Netbeans MDR output for validation.

            Collection<Import> imports = 
                CodeGenUtils.contentsOfType(pkg, Import.class);
            for(Import imp: imports) {
                // Import is not a Feature, so contentsOfType throws if we
                // try to get it to filter on visibility.
                if (imp.isClustered() && 
                    VisibilityKindEnum.PUBLIC_VIS.equals(imp.getVisibility()))
                {
                    Namespace ns = imp.getImportedNamespace();
                    if (ns instanceof MofPackage &&
                        VisibilityKindEnum.PUBLIC_VIS.equals(
                            ((MofPackage) ns).getVisibility()))
                    {
                        MofPackage importedPkg = (MofPackage) ns;
                        writeAbstractPackageAccessor(
                            importedPkg,
                            PACKAGE_GET_PACKAGE_COMMENT,
                            PACKAGE_GET_PACKAGE_RETURN_COMMENT, 
                            PACKAGE_SUFFIX);
                    }
                }
            }
            
            Collection<MofPackage> packages =
                CodeGenUtils.contentsOfType(pkg, MofPackage.class);
            for(MofPackage nestedPkg: packages) {
                writeAbstractPackageAccessor(
                    nestedPkg, 
                    PACKAGE_GET_PACKAGE_COMMENT,
                    PACKAGE_GET_PACKAGE_RETURN_COMMENT, 
                    PACKAGE_SUFFIX);                    
            }
            
            Collection<MofClass> classes = 
                CodeGenUtils.contentsOfType(pkg, MofClass.class);
            for(MofClass cls: classes) {
                writeAbstractPackageAccessor(
                    cls, 
                    PACKAGE_GET_CLASS_COMMENT,
                    PACKAGE_GET_CLASS_RETURN_COMMENT, 
                    CLASS_PROXY_SUFFIX);
            }
            
            Collection<Association> assocs = 
                CodeGenUtils.contentsOfType(pkg, Association.class);
            for(Association assoc: assocs) {
                writeAbstractPackageAccessor(
                    assoc, 
                    PACKAGE_GET_ASSOC_COMMENT,
                    PACKAGE_GET_ASSOC_RETURN_COMMENT, 
                    "");
            }
            
            Collection<StructureType> structs =
                CodeGenUtils.contentsOfType(pkg, StructureType.class);
            if (!structs.isEmpty()) {
                throw new UnsupportedOperationException(
                    "StructureType not yet supported");
            }
            
            writeEntityFooter();
        }
        finally {
            close();
        }
    }

    public void generateStructure(
        StructureType struct)
        throws GenerationException
    {
        String typeName = CodeGenUtils.getTypeName(struct);

        if (!isIncluded(struct)) {
            log.fine("Skipping Excluded Structure '" + typeName + "'");
            return;
        }
        
        log.fine("Generating Structure '" + typeName + "'");
        
        throw new UnsupportedOperationException(
            "structure type generation not supported");
    }
}
