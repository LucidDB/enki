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

import java.io.*;
import java.text.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

/**
 * JavaHandlerBase extends {@link HandlerBase} to provide convenience functions
 * for generator Java code.
 * 
 * <p>Various methods in this class format comments.  Formatting is 
 * achieved via {@link MessageFormat} and the methods usually provide one or
 * two fields implicitly and allow the caller to specify more.  Implicit
 * fields always start at 0.  So for instance, calling
 * 
 * <pre>
 *     writeClassJavaDoc(c, "The class {0} has a comment.");
 * </pre>
 * 
 * for a {@link MofClass} whose name is "SimpleClass" generates a JavaDoc 
 * comment like this (with asterisks in place of x):
 * 
 * <pre>
 *     /xx
 *      x The class SimpleClass has a comment.
 *      x/
 * </pre>
 *
 * Typically, if a "write" method takes a {@link ModelElement} and a comment
 * string, the model element's name is used as the first format parameter.
 * If the method takes additional ModelElements or other arrays to specify
 * argument types, those are appended to the format parameters in order.
 * 
 * @author Stephan Zuercher
 */
public abstract class JavaHandlerBase
    extends HandlerBase
{
    /** 
     * Contains a warning about sub-classing generated interfaces.
     */
    public static final String HEADER_WARNING =
        "<p><em><strong>Note:</strong> This type should not be subclassed, implemented or directly instantiated by clients. It is generated from a MOF metamodel and implemented by Enki or MDR.</em></p>";

    // package comments
    public static final String PACKAGE_COMMENT = "{0} package interface.";
    public static final String PACKAGE_GET_ASSOC_COMMENT = 
        "Returns {1} association proxy object.";
    public static final String PACKAGE_GET_ASSOC_RETURN_COMMENT = 
        "{1} association proxy object.";
    
    public static final String PACKAGE_GET_PACKAGE_COMMENT = 
        "Returns nested package {1}.";
    public static final String PACKAGE_GET_PACKAGE_RETURN_COMMENT = 
        "Proxy object related to nested package {1}.";

    public static final String PACKAGE_GET_CLASS_COMMENT = 
        "Returns {1} class proxy object.";
    public static final String PACKAGE_GET_CLASS_RETURN_COMMENT = 
        "{1} class proxy object.";

    // class proxy comments
    public static final String CLASS_PROXY_COMMENT = "{0} class proxy interface.";

    public static final String CLASS_PROXY_CREATE_COMMENT = 
        "The default factory operation used to create an instance object.";
    public static final String CLASS_PROXY_CREATE_RETURN_COMMENT = 
        "The created instance object.";
    public static final String CLASS_PROXY_CREATE_ARGS_COMMENT = 
        "Creates an instance object having attributes initialized by the passed values.";
 
    // class instance comments
    public static final String CLASS_COMMENT = 
        "{0} object instance interface.";

    // attribute accessor method comments
    public static final String ATTRIB_ACCESSOR_COMMENT = 
        "Returns the value of attribute {0}.";
    public static final String ATTRIB_ACCESSOR_RETURN_COMMENT = 
        "Value of attribute {0}.";
    public static final String ATTRIB_ACCESSOR_RETURN_MULTI_COMMENT = 
        "Value of {0} attribute. Element type: '{'@link {1}'}'";

    // attribute mutator method comments
    public static final String ATTRIB_MUTATOR_COMMENT = 
        "Sets the value of {0} attribute. See '{'@link #{1}'}' for description on the attribute.";

    // reference accessor method comments
    public static final String REF_ACCESSOR_COMMENT = 
        "Returns the value of reference {0}.";
    public static final String REF_ACCESSOR_RETURN_COMMENT = 
        "Value of reference {0}.";
    public static final String REF_ACCESSOR_RETURN_MULTI_COMMENT = 
        "Value of reference {0}. Element type: '{'@link {1}'}'";

    // reference mutator method comments
    public static final String REF_MUTATOR_COMMENT = 
        "Sets the value of {0} reference. See '{'@link #{1}'}' for description on the attribute.";
    
    // enumeration interface comment
    public static final String ENUM_INTERFACE_COMMENT = 
        "{0} enumeration interface.";
    
    // enumeration class comments
    public static final String ENUM_CLASS_COMMENT = 
        "{0} enumeration class implementation.";
    public static final String ENUM_LITERAL_COMMENT = 
        "Enumeration constant corresponding to literal {1}.";
    public static final String ENUM_REF_TYPE_NAME_COMMENT = 
        "Returns fully qualified name of the enumeration type.";
    public static final String ENUM_REF_TYPE_NAME_RETURN_COMMENT =
        "List containing all parts of the fully qualified name.";
    public static final String ENUM_TO_STRING_COMMENT =
        "Returns a string representation of the enumeration value.";
    public static final String ENUM_TO_STRING_RETURN_COMMENT =
        "A string representation of the enumeration value.";
    public static final String ENUM_HASH_CODE_COMMENT =
        "Returns a hash code for this enumeration value.";
    public static final String ENUM_HASH_CODE_RETURN_COMMENT =
        "A hash code for this enumeration value.";
    public static final String ENUM_EQUALS_COMMENT =
        "Indicates whether some other object is equal to this enumeration value.";
    public static final String ENUM_EQUALS_RETURN_COMMENT =
        "true if the other object is the enumeration of the same type and of the same value.";
    public static final String ENUM_EQUALS_PARAM_COMMENT =
        "The reference object with which to compare.";
    public static final String ENUM_FOR_NAME_COMMENT =
        "Translates literal name to correspondent enumeration value.";
    public static final String ENUM_FOR_NAME_RETURN_COMMENT =
        "Enumeration value corresponding to the passed literal.";
    public static final String ENUM_FOR_NAME_PARAM_COMMENT =
        "Enumeration literal.";
    public static final String ENUM_READ_RESOLVE_COMMENT =
        "Resolves serialized instance of enumeration value.";
    public static final String ENUM_READ_RESOLVE_RETURN_COMMENT =
        "Resolved enumeration value.";
   
    // association comments
    public static final String ASSOC_PROXY_COMMENT =
        "{0} association proxy interface.";
    public static final String ASSOC_EXISTS_COMMENT =
        "Queries whether a link currently exists between a given pair of instance objects in the association's link set.";
    public static final String ASSOC_GENERIC_PARAM1_COMMENT =
        "Value of the first association end.";
    public static final String ASSOC_GENERIC_PARAM2_COMMENT =
        "Value of the second association end.";
    public static final String ASSOC_EXISTS_RETURN_COMMENT =
        "Returns true if the queried link exists.";
    public static final String ASSOC_ADD_COMMENT =
        "Creates a link between the pair of instance objects in the association's link set.";
    public static final String ASSOC_REMOVE_COMMENT =
        "Removes a link between a pair of instance objects in the current association's link set.";
    public static final String ASSOC_GET_END_SINGLE_COMMENT =
        "Queries the instance object that is related to a particular instance object by a link in the current association's link set.";
    public static final String ASSOC_GET_END_MULTI_COMMENT =
        "Queries the instance objects that are related to a particular instance object by a link in the current association's link set.";
    public static final String ASSOC_GET_END1_PARAM_COMMENT =
        "Required value of the first association end.";
    public static final String ASSOC_GET_END_RETURN_SINGLE_COMMENT =
        "Related object or <code>null</code> if none exists.";
    public static final String ASSOC_GET_END_RETURN_MULTI_COMMENT =
        "Collection of related objects.";
    public static final String ASSOC_GET_END_RETURN_ORDERED_COMMENT =
        "List of related objects.";
    public static final String ASSOC_GET_END2_PARAM_COMMENT =
        "Required value of the second association end.";

    private static final String METHOD_MUTATOR_PARAM_NAME = "newValue";
    private static final String METHOD_MUTATOR_PARAM_COMMENT = 
        "New value to be set.";

    private boolean displayHeaderWarning = true;
    
    protected void setDisplayHeaderWarning(boolean displayHeaderWarning)
    {
        this.displayHeaderWarning = displayHeaderWarning;
    }
    
    /**
     * Generates the start of a Java interface.
     * 
     * @param entity entity the interface represents
     * @param className type name of the interface
     * @param superInterfaceNames zero or more parent interfaces
     * @param comment JavaDoc comment
     */
    protected void writeInterfaceHeader(
        ModelElement entity, 
        String className,
        String[] superInterfaceNames,
        String comment)
    {
        writeEntityHeader(
            entity, 
            className,
            superInterfaceNames,
            EMPTY_STRING_ARRAY,
            null,
            true,
            false, comment);
    }

    /**
     * Generates the start of a Java interface.
     * 
     * @param entity entity the interface represents
     * @param className type name of the interface
     * @param superInterfaceRefs zero or more parent interface references
     * @param comment JavaDoc comment
     */
    protected void writeInterfaceHeader(
        ModelElement entity, 
        String className,
        JavaClassReference[] superInterfaceRefs,
        String comment)
    {
        String[] superInterfaceNames = null;
        if (superInterfaceRefs != null) {
            superInterfaceNames = new String[superInterfaceRefs.length];
            for(int i = 0; i < superInterfaceRefs.length; i++) {
                superInterfaceNames[i] = superInterfaceRefs[i].toString();
            }
        }
        writeEntityHeader(
            entity, 
            className,
            superInterfaceNames,
            EMPTY_STRING_ARRAY,
            null,
            true,
            false, comment);
    }

    /**
     * Generates the start of a Java class.
     * 
     * @param entity entity the class represents
     * @param className type name of the class
     * @param superClassName an explicit super class, if any
     * @param interfaceNames zero or more implemented interfaces
     * @param isFinal if true the class is marked final
     * @param comment JavaDoc comment
     */
    protected void writeClassHeader(
        ModelElement entity, 
        String className,
        String superClassName,
        String[] interfaceNames,
        boolean isFinal,
        String comment)
    {
        writeClassHeader(
            entity, 
            className,
            superClassName, 
            interfaceNames, 
            null, 
            isFinal,
            comment);
    }
    
    /**
     * Generates the start of a Java class.
     * 
     * @param entity entity the class represents
     * @param className type name of the class
     * @param superClassRef an explicit super class, if any
     * @param interfaceRefs zero or more implemented interfaces
     * @param isFinal if true the class is marked final
     * @param comment JavaDoc comment
     */
    protected void writeClassHeader(
        ModelElement entity, 
        String className,
        JavaClassReference superClassRef,
        JavaClassReference[] interfaceRefs,
        boolean isFinal,
        String comment)
    {
        writeClassHeader(
            entity, 
            className,
            superClassRef, 
            interfaceRefs, 
            null, 
            isFinal,
            comment);
    }
    
    /**
     * Generates the start of a Java class.
     * 
     * @param entity entity the class represents
     * @param className type name of the class
     * @param superClassRef an explicit super class reference, or null
     * @param interfaceRefs zero or more implemented interface references
     * @param importRefs references to classes to import
     * @param isFinal if true the class is marked final
     * @param comment JavaDoc comment
     */
    protected void writeClassHeader(
        ModelElement entity, 
        String className,
        JavaClassReference superClassRef,
        JavaClassReference[] interfaceRefs,
        JavaClassReference[] importRefs,
        boolean isFinal,
        String comment)
    {
        String[] interfaceNames = null;
        if (interfaceRefs != null) {
            interfaceNames = new String[interfaceRefs.length];
            for(int i = 0; i < interfaceRefs.length; i++) {
                interfaceNames[i] = interfaceRefs[i].toString();
            }
        }

        writeClassHeader(
            entity,
            className,
            superClassRef == null ? null : superClassRef.toString(),
            interfaceNames,
            JavaClassReference.computeImports(importRefs),
            isFinal,
            comment);
    }
        
    /**
     * Generates the start of a Java class.
     * 
     * @param entity entity the class represents
     * @param className type name of the class
     * @param superClassName an explicit super class, if any
     * @param interfaceNames zero or more implemented interfaces
     * @param imports names of classes to import (e.g., "java.util.List" or
     *                "java.text.*")
     * @param isFinal if true the class is marked final
     * @param comment JavaDoc comment
     */
    protected void writeClassHeader(
        ModelElement entity, 
        String className,
        String superClassName,
        String[] interfaceNames,
        String[] imports,
        boolean isFinal,
        String comment)
    {
        String[] superClassNames = 
            superClassName != null 
                ? new String[] { superClassName } 
                : EMPTY_STRING_ARRAY;
                
        writeEntityHeader(
            entity, 
            className,
            superClassNames,
            interfaceNames, 
            imports,
            false,
            isFinal,
            comment);
    }
    
    /**
     * Generates a class or interface header as specified. The common header is
     * emitted immediately, if set.  Then writes a package clause, class
     * JavaDoc, and entity declaration.  There may only be multiple super
     * class names if isInterface is true.  Likewise, if isInterface is 
     * true, there may not be any interface names.
     * 
     * <p>Automatically increases the indent level.
     * 
     * @param entity entity the class represents
     * @param className type name of the class
     * @param superClassNames explicit super classes, if any
     * @param interfaceNames zero or more implemented interfaces
     * @param imports names of classes to import (e.g. "java.util.List"
     *                or "java.text.*")
     * @param isInterface if true the class is an interface
     * @param isFinal if true the class is marked final
     * @param comment JavaDoc comment
     * @see #writeEntityDeclarationStart(String, String[], String[], boolean, boolean)
     */
    private void writeEntityHeader(
        ModelElement entity, 
        String className,
        String[] superClassNames,
        String[] interfaceNames,
        String[] imports,
        boolean isInterface,
        boolean isFinal,
        String comment)
    {
        if (commonHeader != null) {
            writeln("/*");
            writeWrapped("// ", commonHeader);
            writeln("*/");
            newLine();
        }
        
        writePackageClause(className);
        writeImports(imports);
        if (comment != null) {
            writeClassJavaDoc(entity, comment);
        }
        writeEntityDeclarationStart(
            className,
            superClassNames, 
            interfaceNames, 
            isInterface,
            isFinal);
    }

    private void writeImports(String[] imports)
    {
        if (imports != null) {
            for(String imp: imports) {
                writeln("import ", imp, ";");
            }
            newLine();
        }
    }
    
    /**
     * Write a package clause for the given class name.  For instance,
     * writes <tt>package java.lang;</tt> for the input "java.lang.String".
     * 
     * @param className a fully-qualified class name
     */
    protected void writePackageClause(String className)
    {
        int end = className.lastIndexOf('.');
        
        writeln(
            "package ",
            className.substring(0, end),
            ";");
        newLine();
    }
    
    /**
     * Writes a JavaDoc comment for the given entity.  See {@link HandlerBase}
     * for details on comment formatting.
     * 
     * @param entity entity to write a JavaDoc comment for
     * @param comment formatted comment
     */
    protected void writeClassJavaDoc(ModelElement entity, String comment)
    {
        writeln("/**");
        if (comment != null) {
            String entityName = null;
            if (entity != null) {
                entityName = entity.getName();
            }
            writeWrapped(
                " *", MessageFormat.format(comment, entityName));
        }
        
        if (entity != null) {
            writeWrapped(" *", entity.getAnnotation());
        }
        
        if (displayHeaderWarning) {
            writeWrapped(" *", " ");
            writeWrapped(" *", HEADER_WARNING);
        }
        writeln(" */");
    }

    /**
     * Writes the start of a class or interface.
     * Asserts that various combinations of parameters are legal (interfaces 
     * cannot be final, single inheritance for classes).  
     * 
     * @param className class name 
     * @param superClassNames superclass name(s) (cannot be null, empty okay) 
     * @param interfaces interface names (cannot be null, empty okay)
     * @param isInterface true if an interface
     * @param isFinal true if final class
     */
    private void writeEntityDeclarationStart(
        String className,
        String[] superClassNames, 
        String[] interfaces, 
        boolean isInterface,
        boolean isFinal)
    {
        int start = className.lastIndexOf('.') + 1;
        String simpleClassName = className.substring(start);
        
        assert(simpleClassName != null && simpleClassName.length() > 0);
        
        // One super class, unless we're generating an interface
        assert(superClassNames.length <= 1 || isInterface);
        
        // No interfaces implemented unless we're generating a class
        assert(interfaces.length == 0 || !isInterface);
        
        // Cannot have a final interface.
        assert(!isInterface || !isFinal);
        
        writeln(
            "public",
            isFinal ? " final " : " ",
            isInterface ? "interface " : "class ",
            simpleClassName);
        String extensions = listToString("extends ", superClassNames);
        if (extensions.length() > 0) {
            increaseIndent();
            writeln(extensions);
            decreaseIndent();
        }
        String implementations = listToString("implements ", interfaces);
        if (implementations.length() > 0) {
            increaseIndent();
            writeln(implementations);
            decreaseIndent();
        }
        writeln("{");
        increaseIndent();
    }

    /**
     * Generates an abstract methods for accessing a {@link RefPackage}
     * instance.
     * @param entity the {@link MofPackage} 
     * @param comment JavaDoc comment
     * @param returnComment JavaDoc return value comment
     * @param suffix suffix to append to the entity's type name
     */
    protected void writeAbstractPackageAccessor(
        ModelElement entity,
        String comment, 
        String returnComment,
        String suffix)
    {
        writePackageAccessor(entity, comment, returnComment, suffix, true);
    }
    
    protected void startPackageAccessorBlock(
        ModelElement entity,
        String suffix)
    {
        writePackageAccessor(entity, null, null, suffix, false);
        writeln("{");
        increaseIndent();
    }
    
    private void writePackageAccessor(
        ModelElement entity,
        String comment, 
        String returnComment,
        String suffix,
        boolean isAbstract)
    {
        String typeName = generator.getTypeName(entity);

        String entityName = generator.getSimpleTypeName(entity);

        if (comment != null) {
            writeMethodJavaDoc(
                entity, 
                comment, 
                true,
                returnComment,
                null,
                null,
                true, 
                new String[] { entityName });
        }

        write(
            "public ", 
            typeName,
            suffix,
            " get",
            entityName,
            "()");
        if (isAbstract) {
            writeln(";");
        } else {
            writeln();
        }
    }
    
    protected void writeAbstractCreator(
        GeneralizableElement entity,
        String comment, 
        String returnComment,
        ModelElement[] params,
        String suffix)
    {
        writeCreator(
            entity, comment, returnComment, params, suffix, true, true);
        newLine();
    }
    
    protected void startCreatorBlock(
        GeneralizableElement entity,
        ModelElement[] params,
        String suffix)
    {
        writeCreator(entity, null, null, params, suffix, false, true);
        writeln("{");
        increaseIndent();
    }
    
    protected void startCreatorBlock(
        GeneralizableElement entity,
        ModelElement[] params,
        boolean returnSimpleType,
        String suffix)
    {
        writeCreator(
            entity, null, null, params, suffix, false, returnSimpleType);
        writeln("{");
        increaseIndent();
    }
    
    private void writeCreator(
        GeneralizableElement entity,
        String comment, 
        String returnComment,
        ModelElement[] params,
        String suffix,
        boolean isAbstract,
        boolean returnSimpleType)
    {
        String entityName = generator.getSimpleTypeName(entity);
        String returnTypeName = entityName;
        if (!returnSimpleType) {
            returnTypeName = generator.getTypeName(entity);
        }
        
        if (comment != null) {
            writeMethodJavaDoc(
                entity,
                comment, 
                false, 
                returnComment,
                params,
                new String[] { entityName });
        }
        
        if (params == null || params.length == 0) {
            writeln(
                "public ", 
                returnTypeName,
                suffix,
                " create",
                entityName,
                "()",
                isAbstract ? ";" : "");
        } else {
            writeln(
                "public ", 
                returnTypeName,
                suffix,
                " create",
                entityName,
                "(");
            increaseIndent();
            for(int i = 0; i < params.length; i++) {
                String[] paramInfo = generator.getParam(params[i]);
                writeln(
                    paramInfo[0],
                    " ",
                    paramInfo[1],
                    (i + 1 < params.length) 
                        ? "," 
                        : (")" + (isAbstract ? ";" : "")));
            }
            
            decreaseIndent();
        }
    }

    protected void writeAbstractAccessor(
        StructuralFeature feature,
        String comment,
        String returnDescription)
    {
        writeAccessor(
            feature, comment, returnDescription, true, null, null, false);
        newLine();
    }

    protected void startAccessorBlock(
        StructuralFeature feature)
    {
        startAccessorBlock(feature, false);
    }
    
    protected void startAccessorBlock(
        StructuralFeature feature,
        boolean useJavaUtilImport)
    {
        writeAccessor(
            feature, null, null, false, null, null, useJavaUtilImport);
        writeln("{");
        increaseIndent();
    }
    
    protected void startAccessorBlock(
        StructuralFeature feature, String methodSuffix)
    {
        startAccessorBlock(feature, methodSuffix, false);
    }

    protected void startAccessorBlock(
        StructuralFeature feature, String methodSuffix, String typeSuffix)
    {
        startAccessorBlock(feature, methodSuffix, typeSuffix, false);
    }
    
    protected void startAccessorBlock(
        StructuralFeature feature,
        String methodSuffix, 
        boolean useJavaUtilImport)
    {
        startAccessorBlock(feature, methodSuffix, null, useJavaUtilImport);
    }
    
    protected void startAccessorBlock(
        StructuralFeature feature,
        String methodSuffix, 
        String typeSuffix,
        boolean useJavaUtilImport)
    {
        writeAccessor(
            feature, 
            null, 
            null,
            false,
            methodSuffix, 
            typeSuffix,
            useJavaUtilImport);
        writeln("{");
        increaseIndent();
    }
    
    private void writeAccessor(
        StructuralFeature feature,
        String comment,
        String returnDescription,
        boolean isAbstract,
        String methodSuffix, 
        String typeSuffix,
        boolean useJavaUtilImport)
    {
        String typeName = generator.getTypeName(feature, typeSuffix);
        if (useJavaUtilImport && typeName.startsWith("java.util.")) {
            typeName = typeName.substring(10);
        }
        
        String methodName = generator.getAccessorName(feature);
        if (methodName == null) {
            // will happen if upper multiplicity bound == 0
            return;
        }
        
        if (methodSuffix != null) {
            methodName += methodSuffix;
        }

        if (comment != null) {
            writeMethodJavaDoc(
                feature,
                comment,
                true,
                returnDescription,
                null,
                null,
                new String[] { generator.getTypeName(feature.getType()) });
        }
        
        writeln(
            "public ",
            typeName,
            " ",
            methodName,
            "()",
            isAbstract ? ";" : "");
    }

    protected void writeAbstractGenericMethod(
        ModelElement entity,
        String comment,
        String returnTypeName,
        String returnDescription,
        String methodName,
        String[] paramTypes,
        String[] paramNames,
        String[] paramDescriptions)
    {
        writeGenericMethod(
            entity,
            "public",
            comment,
            returnTypeName,
            returnDescription,
            methodName,
            paramTypes,
            paramNames,
            paramDescriptions,
            true,
            false);
    }
    
    protected void startGenericMethodBlock(
        ModelElement entity,
        String returnTypeName,
        String methodName,
        String[] paramTypes,
        String[] paramNames)
    {
        writeGenericMethod(
            entity, 
            "public",
            null,
            returnTypeName,
            null, 
            methodName,
            paramTypes,
            paramNames,
            null,
            false,
            false);
        writeln("{");
        increaseIndent();
    }
    
    protected void startConstructorBlock(
        ModelElement entity,
        String[] paramTypes,
        String[] paramNames,
        String suffix)
    {
        startConstructorBlock(
            entity,
            paramTypes,
            paramNames,
            false,
            suffix);
    }
    
    protected void startConstructorBlock(
        ModelElement entity,
        String[] paramTypes,
        String[] paramNames,
        boolean isPublic,
        String suffix)
    {
        writeGenericMethod(
            entity, 
            isPublic ? "public" : "",
            null,
            null,
            null, 
            generator.getSimpleTypeName(entity, suffix),
            paramTypes,
            paramNames,
            null,
            false,
            true);
        writeln("{");
        increaseIndent();
    }
    
    protected void startConstructorBlock(
        ModelElement entity,
        ModelElement[] params,
        String suffix)
    {
        startConstructorBlock(entity, params, false, suffix);
    }
    
    protected void startConstructorBlock(
        ModelElement entity,
        ModelElement[] params,
        boolean isPublic,
        String suffix)
    {
        String[] paramTypes = new String[params.length];
        String[] paramNames = new String[params.length];
        for(int i = 0; i < params.length; i++) {
            String[] paramInfo = generator.getParam(params[i]);
            paramTypes[i] = paramInfo[0];
            paramNames[i] = paramInfo[1];
        }

        startConstructorBlock(entity, paramTypes, paramNames, isPublic, suffix);
    }
    
    private void writeGenericMethod(
        ModelElement entity,
        String visibility,
        String comment,
        String returnTypeName,
        String returnDescription,
        String methodName,
        String[] paramTypes,
        String[] paramNames,
        String[] paramDescriptions,
        boolean isAbstract,
        boolean isConstructor)
    {
        assert(
            (paramTypes == null && paramNames == null) ||
            (paramTypes != null && paramNames!= null && 
                paramTypes.length == paramNames.length));
        assert(
            paramDescriptions == null || 
            paramDescriptions.length == 
                (paramTypes != null ? paramTypes.length : 0));
        assert(!isAbstract || !isConstructor);
        
        if (comment != null) {
            writeMethodJavaDoc(
                entity,
                comment,
                false,
                returnDescription,
                paramNames,
                paramDescriptions,
                null);
        }
        
        if (paramTypes != null) {
            writeln(
                visibility,
                visibility.length() > 0 ? " " : "",
                isConstructor ? "" : returnTypeName,
                isConstructor ? "" : " ",
                methodName,
                "(");
            increaseIndent();
            for(int i = 0; i < paramNames.length; i++) {
                writeln(
                    paramTypes[i],
                    " ",
                    paramNames[i],
                    (i + 1 < paramNames.length)
                        ? ","
                        : (")" + (isAbstract ? ";" : "")));
            }
            decreaseIndent();
        } else {
            writeln(
                visibility,
                visibility.length() > 0 ? " " : "",
                isConstructor ? "" : returnTypeName,
                isConstructor ? "" : " ",
                methodName,
                "()",
                isAbstract ? ";" : "");
        }
    }

    protected void writeAbstractMutator(
        StructuralFeature feature, 
        String comment)
    {
        writeMutator(feature, comment, true, null, null);
        newLine();
    }
    
    protected void startMutatorBlock(StructuralFeature feature)
    {
        writeMutator(feature, null, false, null, null);
        writeln("{");
        increaseIndent();
    }
    
    protected void startMutatorBlock(
        StructuralFeature feature, String methodSuffix)
    {
        startMutatorBlock(feature, methodSuffix, null);
    }
    protected void startMutatorBlock(
        StructuralFeature feature, String methodSuffix, String typeSuffix)
    {
        writeMutator(feature, null, false, methodSuffix, typeSuffix);
        writeln("{");
        increaseIndent();
    }
    
    private void writeMutator(
        StructuralFeature feature, 
        String comment,
        boolean isAbstract,
        String methodSuffix,
        String typeSuffix)
    {
        String typeName = generator.getTypeName(feature);
        if (typeSuffix != null) {
            typeName += typeSuffix;
        }
        
        String methodName = generator.getMutatorName(feature);
        
        if (methodSuffix != null) {
            methodName += methodSuffix;
        }
        
        String accessorName = generator.getAccessorName(feature);
        
        if (comment != null) {
            writeMethodJavaDoc(
                feature,
                comment,
                false,
                null,
                new String[] { METHOD_MUTATOR_PARAM_NAME },
                new String[] { METHOD_MUTATOR_PARAM_COMMENT },
                new String[] { accessorName + "()" });
        }
        
        writeln(
            "public void ",
            methodName,
            "(",
            typeName,
            " ",
            METHOD_MUTATOR_PARAM_NAME,
            ")",
            isAbstract ? ";" : "");
    }
    
    protected void writeTrivialAccessor(
        ModelElement entity,
        String comment,
        String returnDescription,
        String typeName,
        String methodName,
        String fieldName)
    {
        writeMethodJavaDoc(
            entity,
            comment,
            false,
            returnDescription,
            null,
            null,
            EMPTY_STRING_ARRAY);
        startBlock("public ", typeName, " ", methodName, "()");
        writeln("return ", fieldName, ";");
        endBlock();
        newLine();
    }
    
    protected void writeMethodJavaDoc(
        ModelElement entity,
        String comment,
        boolean annotate,
        String returnDescription,
        String[] argNames,
        String[] argDescriptions,
        String[] formatParams)
    {
        writeMethodJavaDoc(
            entity, 
            comment, 
            annotate,
            returnDescription,
            argNames, 
            argDescriptions,
            true, 
            formatParams);
    }
    
    protected void writeMethodJavaDoc(
        ModelElement entity,
        String comment,
        boolean annotate,
        String returnDescription,
        ModelElement[] args,
        String[] formatParams)
    {
        String[] argNames = null;
        String[] argDescriptions = null;
        
        if (args != null) {
            argNames = new String[args.length];
            argDescriptions = new String[args.length];
            
            for(int i = 0; i < args.length; i++) {
                ModelElement arg = args[i];
                argNames[i] = generator.getParam(arg)[1];
                argDescriptions[i] = arg.getAnnotation();
            }
        }
        
        writeMethodJavaDoc(
            entity, 
            comment, 
            annotate,
            returnDescription, 
            argNames, 
            argDescriptions,
            false,
            formatParams);
    }
    
    private void writeMethodJavaDoc(
        ModelElement entity,
        String comment,
        boolean annotate,
        String returnDescription,
        String[] argNames,
        String[] argDescriptions,
        boolean formatDescriptions,
        String[] formatParams)
    {
        if ((argNames != null ? argNames.length : 0) != 
            (argDescriptions != null ? argDescriptions.length : 0))
        {
            throw new IllegalArgumentException(
                "argNames.length != argDescriptions.length");
        }
        
        String[] temp = 
            formatParams == null ? EMPTY_STRING_ARRAY : formatParams;
        formatParams = new String[temp.length + 1];
        formatParams[0] = entity.getName();
        for(int i = 0; i < temp.length; i++) {
            formatParams[i + 1] = temp[i];
        }
        
        writeln("/**");
        if (comment != null) {
            writeWrapped(
                " *", 
                MessageFormat.format(comment, (Object[])formatParams));
        }
        
        if (annotate) {
            writeWrapped(" *", entity.getAnnotation());
        }
        
        if (argNames != null) {
            for(int i = 0; i < argNames.length; i++) {
                String description = argDescriptions[i];
                if (formatDescriptions && description != null) {
                    description = 
                        MessageFormat.format(
                            description, (Object[])formatParams);
                }
                
                if (description != null) {
                    writeWrapped(" *", "@param ", argNames[i], " ", description);
                }
            }
        }
        
        if (returnDescription != null) {
            if (formatDescriptions) {
                returnDescription = 
                    MessageFormat.format(
                        returnDescription, (Object[])formatParams);
            }
            writeWrapped(" *", "@return ", returnDescription);
        }
        
        writeln(" */");
    }
    
    /**
     * Writes a private field with the given type and keywords and with a name 
     * generated from type's name.
     * 
     * @param feature field type
     * @param isFinal controls whether field is final
     * @param isStatic controls whether field is static
     * @return the field's name
     */
    protected String writePrivateField(
        StructuralFeature feature, 
        boolean isFinal, 
        boolean isStatic)
    {
        String fieldName = feature.getName();
        fieldName = generator.getClassFieldName(fieldName);
        
        writePrivateField(
            feature, 
            fieldName, 
            isFinal,
            isStatic,
            null);
        
        return fieldName;
    }
    
    /**
     * Writes a private field with the given type and keywords and with a name 
     * generated from type's name.
     * 
     * @param feature field type
     * @param fieldNameSuffix suffix for the field name
     * @param isFinal controls whether field is final
     * @param isStatic controls whether field is static
     * 
     * @return the field's name
     */
    protected String writePrivateField(
        StructuralFeature feature, 
        String fieldNameSuffix,
        boolean isFinal, 
        boolean isStatic)
    {
        String fieldName = feature.getName();
        fieldName = generator.getClassFieldName(fieldName);
        if (fieldNameSuffix != null) {
            fieldName += fieldNameSuffix;
        }
        
        writePrivateField(
            feature, 
            fieldName, 
            isFinal,
            isStatic,
            null);
        
        return fieldName;
    }
    
    /**
     * Writes a private field with the given type and keywords and with a name 
     * generated from the feature's name.
     * 
     * @param fieldType field type
     * @param feature feature from which to derive the field's name
     * @param isFinal controls whether field is final
     * @param isStatic controls whether field is static
     * 
     * @return the field's name
     */
    protected String writePrivateField(
        String fieldType,
        StructuralFeature feature, 
        boolean isFinal, 
        boolean isStatic)
    {
        String fieldName = feature.getName();
        fieldName = generator.getClassFieldName(fieldName);
        
        writePrivateField(
            fieldType, 
            fieldName, 
            isFinal,
            isStatic);
        
        return fieldName;
    }
    
    /**
     * Writes a private field with the given type and keywords and with a name 
     * generated from type's name.
     * 
     * @param type field type
     * @param isFinal controls whether field is final
     * @param isStatic controls whether field is static
     * @param typeSuffix suffix for the type (and therefore field) name
     * 
     * @return the field's name
     */
    protected String writePrivateField(
        ModelElement type, 
        boolean isFinal, 
        boolean isStatic, 
        String typeSuffix)
    {
        String fieldName = generator.getSimpleTypeName(type, typeSuffix);
        fieldName = generator.getClassFieldName(fieldName);
        
        writePrivateField(
            type, 
            fieldName, 
            isFinal,
            isStatic,
            typeSuffix);
        
        return fieldName;
    }
    
    /**
     * Writes a private field with the given type, keywords and name.
     * 
     * @param type field type
     * @param name the field's name
     * @param isFinal controls whether field is final
     * @param isStatic controls whether field is static
     * @param typeSuffix suffix for the type name
     */
    private void writePrivateField(
        ModelElement type, 
        String name, 
        boolean isFinal, 
        boolean isStatic, 
        String typeSuffix)
    {
        String fieldType;
        if (type instanceof StructuralFeature) {
            fieldType = 
                generator.getTypeName((StructuralFeature)type, typeSuffix);
        } else {
            fieldType = generator.getTypeName(type, typeSuffix);
        }

        writePrivateField(fieldType, name, isFinal, isStatic);
    }

    /**
     * Writes a private field with the given type, keywords and name.
     * 
     * @param fieldType field type
     * @param fieldName the field's name
     * @param isFinal controls whether field is final
     * @param isStatic controls whether field is static
     */
    protected void writePrivateField(
        String fieldType,
        String fieldName,
        boolean isFinal, 
        boolean isStatic)
    {
        writeln(
            "private ",
            isStatic ? "static " : "",
            isFinal ? "final " : "",
            fieldType,
            " ",
            fieldName,
            ";");
    }
    
    /**
     * Writes a private field with the given type, keywords and name.
     * 
     * @param fieldType field type
     * @param fieldName the field's name
     * @param value the field's value
     * @param isPrivate if true, make the field private (vs. public)
     */
    protected void writeConstant(
        String fieldType,
        String fieldName,
        String value,
        boolean isPrivate)
    {
        writeln(
            (isPrivate ? "private " : "public "),
            "static final ",
            fieldType,
            " ",
            fieldName,
            " = ",
            value,
            ";");
    }
    protected void writeEntityFooter()
    {
        endBlock();
    }
    
    protected void startBlock(Object... stmt)
    {
        writeln(stmt);
        writeln("{");
        increaseIndent();
    }
    
    protected void endBlock()
    {
        decreaseIndent();
        writeln("}");
    }

    protected void startStmtBlock(Object... stmt)
    {
        writeln(stmt);
        increaseIndent();
    }
    
    protected void restartStmtBlock(Object... stmt)
    {
        decreaseIndent();
        writeln(stmt);
        increaseIndent();
    }
    
    protected void endStmtBlock()
    {
        endBlock();
    }
    
    protected void startConditionalBlock(CondType type, Object ... condition)
    {
        if (type == CondType.ELSEIF || type == CondType.ELSE) {
            decreaseIndent();
            write("} else ");
        }
        if (type == CondType.IF || type == CondType.ELSEIF) {
            write("if (");
            write(condition);
            write(") ");
        }
        writeln("{");
        increaseIndent();
    }
    

    /**
     * Open a new file.  Concrete subclasses should call this method when they 
     * wish to begin generating a new file.  The type name is a fully-qualified
     * Java class name.  Periods are automatically converted into directory
     * separators.
     * 
     * @param typeName name of the type to write to a file.
     * @throws GenerationException if there's an IO error or there is already 
     *                             an open file or if the previous file
     *                             had a late error (see {@link #close()}
     */
    protected void open(String typeName) throws GenerationException
    {
        File file = mkdirs(typeName);
        
        open(file);
    }
    
    protected void close() throws GenerationException
    {
        newLine();
        writeln("// End ", currentFile.getName());

        super.close();
    }
    
    private File mkdirs(String typeName)
    {
        String[] paths = typeName.split("\\.");
        
        File parent = outputDir;
        for(int i = 0; i < paths.length - 1; i++) {
            String path = paths[i];
            File dir = new File(parent, path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            parent = dir;
        }
        
        String filename = paths[paths.length - 1] + ".java";
        
        return new File(parent, filename);
    }
    
    protected void emitSuppressWarningsAnnotation()
    {
        writeln("@SuppressWarnings(", QUOTE, "unchecked", QUOTE, ")");
    }

    /**
     * CondType enumerates the various types of conditional blocks that may
     * be started via 
     * {@link JavaHandlerBase#startConditionalBlock(CondType, Object...)}.
     * These are, an initial <code>if</code> statement and subsequent
     * <code>else if</code> or <code>else</code> statements.
     */
    public static enum CondType
    {
        /** Emit an <code>if (<i>condition</i>) {</code> block. */
        IF,
        
        /** Emit an <code>} else if (<i>condition</i>) {</code> block. */
        ELSEIF,
        
        /** Emit an <code>} else {</code> block. */
        ELSE;
    }
}

// End JavaHandlerBase.java
