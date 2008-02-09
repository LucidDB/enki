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
import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;

/**
 * GeneratorBase is an abstract base class for Enki code generators.
 * It manages an XMI file (the code generator input), an output directory
 * (code generation target) and a flag that controls whether or not
 * Java generic types are used for collections.  In addition, GeneratorBase
 * provides access to model tags and provides utility methods for 
 * determining the correct name for types, parameters, accessor methods, and
 * mutator methods.
 * 
 * @author Stephan Zuercher
 */
public abstract class GeneratorBase implements Generator
{
    private static final String COLLECTION_INTERFACE = 
        Collection.class.getName();

    private static final String ORDERED_COLLECTION_INTERFACE = 
        List.class.getName();


    protected File xmiFile;
    protected File outputDir;
    protected boolean enableGenerics;
    
    private Set<RefObject> visited;
    
    /** All configured handlers. */
    private List<Handler> allHandlers;
    
    /** Handlers to be invoked during the current pass. */
    private List<Handler> handlers;
    
    private RefBaseObject refBaseObject;
    
    private int passIndex;
    
    protected GeneratorBase()
    {
        this.visited = new HashSet<RefObject>();
        this.allHandlers = new ArrayList<Handler>();
        this.handlers = new ArrayList<Handler>();
        this.enableGenerics = false;
        this.passIndex = -1;
    }

    // implements Generator
    public void setXmiFile(File xmiFile)
    {
        this.xmiFile = xmiFile;
    }

    // implements Generator
    public File getXmiFile()
    {
        return xmiFile;
    }
    
    // implements Generator
    public void setOutputDirectory(File outputDir)
    {
        this.outputDir = outputDir;
    }
    
    // implements Generator
    public boolean setUseGenerics(boolean enableGenerics)
    {
        boolean oldSetting = this.enableGenerics;
        this.enableGenerics = enableGenerics;
        return oldSetting;
    }
    
    // implements Generator
    public final void addHandler(Handler handler)
    {
        handler.setGenerator(this);
        handler.setOutputDir(outputDir);
        allHandlers.add(handler);
    }
    
    // implements Generator
    public final RefBaseObject getRefBaseObject()
    {
        return refBaseObject;
    }
    
    protected void visitRefBaseObject(RefBaseObject obj)
    throws GenerationException
    {
        this.refBaseObject = obj;
        
        int numPasses = 1;
        for(Handler handler: allHandlers) {
            numPasses = Math.max(numPasses, handler.getNumPasses());
        }
        
        invokeGenerationStart();
        
        boolean throwing = true;
        try {
            for(int i = 0; i < numPasses; i++) {
                passIndex = i;
                
                // Set up for this pass.
                visited.clear();
                handlers.clear();
                for(Handler handler: allHandlers) {
                    if (i < handler.getNumPasses()) {
                        handlers.add(handler);
                    }
                }
                
                invokeBeginPass();
                
                if (obj instanceof RefPackage) {
                    visitRefPackage((RefPackage)obj);
                } else if (obj instanceof RefObject) {
                    visitRefObject((RefObject)obj);
                } else if (obj instanceof RefAssociation) {
                    visitRefAssociation((RefAssociation)obj);
                } else if (obj instanceof RefClass) {
                    visitRefClass((RefClass)obj);
                }
                
                invokeEndPass();
            }
            throwing = false;
        } finally {
            invokeGenerationEnd(throwing);
        }
    }

    protected void visitRefClass(RefClass cls) throws GenerationException
    {
        if (cls.refImmediatePackage() instanceof ModelPackage) {
            for(Object e: cls.refAllOfClass()) {
                RefObject obj = (RefObject)e;
                
                visitRefObject(obj);
            }
        }
    }

    protected void visitRefAssociation(RefAssociation assoc) 
    throws GenerationException
    {
        visitRefObject(assoc.refMetaObject());
    }
    
    protected void visitRefObject(RefObject obj) throws GenerationException
    {
        if (!generatObject(obj)) {
            return;
        }
        
        Namespace outermost = (Namespace)obj;
        while(true) {
            Namespace container = outermost.getContainer();
            if (container == null) {
                break;
            }
            outermost = container;
        }

        if (!(outermost instanceof MofPackage)) {
            return;
        }
        
        String ignoreLifecycleString =
            TagUtil.getTagValue(outermost, TagUtil.TAGID_IGNORE_LIFECYCLE);
        boolean ignoreLifecycle = Boolean.parseBoolean(ignoreLifecycleString);

        if (obj instanceof Association) {
            if (!ignoreLifecycle) {
                invokeAssociationTemplate((Association)obj);
            }
        } else if (obj instanceof MofClass || obj instanceof MofPackage) {
            GeneralizableElement elm = (GeneralizableElement)obj;
            visited.add(elm);
            
            for(Object superType: elm.getSupertypes()) {
                visitRefObject((RefObject)superType);
            }
            
            Collection<?> contents = elm.getContents();
            for(Object content: contents) {
                visitRefObject((RefObject)content);
            }

            if (elm instanceof MofPackage) {
                if (!ignoreLifecycle) {
                    invokePackageTemplate((MofPackage)elm);
                }
            } else {
                invokeClassInstanceTemplate((MofClass)elm);

                if (!ignoreLifecycle) {
                    invokeClassProxyTemplate((MofClass)elm);
                }
            }
        } else if (obj instanceof EnumerationType) {
            EnumerationType et = (EnumerationType)obj;

            invokeEnumerationInterfaceTemplate(et);
            invokeEnumerationClassTemplate(et);
        } else if (obj instanceof StructureType) {
            invokeStructureTemplate((StructureType)obj);
        } else if (obj instanceof MofException) {
            invokeExceptionTemplate((MofException)obj);
        } else {
//            throw new GenerationException(
//                "unknown type '" + obj.getClass() + "'");
        }

    }

    protected void visitRefPackage(RefPackage pkg) throws GenerationException
    {
        if (pkg instanceof ModelPackage) {
            ModelPackage modelPkg = (ModelPackage)pkg;
    
            for(Object e: modelPkg.getMofPackage().refAllOfClass()) {
                ModelElement elem = (ModelElement)e;
                
                if (elem.getContainer() == null) {
                    visitRefObject(elem);
                }
            }
        }
    }

    protected boolean generatObject(RefObject obj)
    {
        if (visited.contains(obj) || !(obj instanceof Namespace)) {
            return false;
        }
        
        return true;
    }

    private void invokeGenerationStart() throws GenerationException
    {
        for(Handler h: allHandlers) {
            h.beginGeneration();
        }
    }
    
    private void invokeGenerationEnd(boolean throwing) 
    throws GenerationException
    {
        for(Handler h: allHandlers) {
            h.endGeneration(throwing);
        }
    }
    
    private void invokeBeginPass() throws GenerationException
    {
        for(Handler h: handlers) {
            h.beginPass(passIndex);
        }
    }
    
    private void invokeEndPass() throws GenerationException
    {
        for(Handler h: handlers) {
            h.endPass(passIndex);
        }
    }

    private void invokeAssociationTemplate(Association assoc) 
        throws GenerationException
    {
        for(AssociationHandler h: handlersOfType(AssociationHandler.class)) {
            h.generateAssociation(assoc);
        }
    }
    
    private void invokePackageTemplate(MofPackage pkg) 
        throws GenerationException
    {
        for(PackageHandler h: handlersOfType(PackageHandler.class)) {
            h.generatePackage(pkg);
        }
    }

    private void invokeClassInstanceTemplate(MofClass cls)
        throws GenerationException
    {
        for(ClassInstanceHandler h: handlersOfType(ClassInstanceHandler.class))
        {
            h.generateClassInstance(cls);
        }
    }

    private void invokeClassProxyTemplate(MofClass cls)
        throws GenerationException
    {
        for(ClassProxyHandler h: 
            handlersOfType(ClassProxyHandler.class))
        {
            h.generateClassProxy(cls);
        }
    }

    private void invokeEnumerationInterfaceTemplate(EnumerationType enm)
        throws GenerationException
    {
        for(EnumerationInterfaceHandler h: 
            handlersOfType(EnumerationInterfaceHandler.class))
        {
            h.generateEnumerationInterface(enm);
        }
    }

    private void invokeEnumerationClassTemplate(EnumerationType enm)
        throws GenerationException
    {
        for(EnumerationClassHandler h: 
            handlersOfType(EnumerationClassHandler.class))
        {
            h.generateEnumerationClass(enm);
        }
    }


    private void invokeStructureTemplate(StructureType struct)
        throws GenerationException
    {
        for(StructureHandler h: handlersOfType(StructureHandler.class)) {
            h.generateStructure(struct);
        }
    }

    private void invokeExceptionTemplate(MofException ex)
        throws GenerationException
    {
        for(ExceptionHandler h: handlersOfType(ExceptionHandler.class)) {
            h.generateException(ex);
        }
    }

    private <E> Collection<E> handlersOfType(
        final Class<E> cls)
    {
        assert(Handler.class.isAssignableFrom(cls));
        
        return new AbstractCollection<E>() {
            @Override
            public Iterator<E> iterator()
            {
                return new Iterator<E>() {
                    Iterator<Handler> iter = handlers.iterator();
                    Class<E> type = cls;
                    E next = null;
                    
                    public boolean hasNext()
                    {
                        if (next != null) {
                            return true;
                        }
                        
                        while(iter.hasNext()) {
                            Handler candidate = iter.next();
                            if (type.isInstance(candidate)) {
                                next = type.cast(candidate);
                                return true;
                            }
                        }
                        
                        return false;
                    }

                    public E next()
                    {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        
                        E result = next;
                        next = null;
                        return result;
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                    
                };
            }

            @Override
            public int size()
            {
                int size = 0;
                for(Iterator<E> iter = iterator(); iter.hasNext(); ) {
                    iter.next();
                    size++;
                }
                return size;
            }
        };
    }
    
    // implements Generator
    public String[] getParam(ModelElement param)
    {
        String[] result = new String[2];

        if (param instanceof StructuralFeature) {
            result[0] = getTypeName((StructuralFeature)param);
        } else if (param instanceof Parameter) {
            result[0] = getTypeName((Parameter)param);            
        } else if (param instanceof TypedElement) {
            result[0] = getTypeName((TypedElement)param);
        } else {
            assert(false);
        }
        
        String name = 
            TagUtil.getTagValue(param, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (name == null) {
            name = param.getName();
        }
        
        result[1] = 
            StringUtil.mangleIdentifier(
                name, StringUtil.IdentifierType.CAMELCASE_INIT_LOWER);
        
        return result;
    }
    
    // implements Generator
    public String getTypeName(StructuralFeature feature)
    {
        return getTypeName(feature, feature.getMultiplicity());
    }
    
    // implements Generator
    public String getTypeName(StructuralFeature feature, String suffix)
    {
        return getTypeName(feature, feature.getMultiplicity(), suffix);
    }
    
    // implements Generator
    public String getTypeName(Parameter param)
    {
        return getTypeName(param, param.getMultiplicity());
    }
    
    public String getTypeName(TypedElement type)
    {
        return getTypeName(type, (MultiplicityType)null);
    }
    
    public String getTypeName(TypedElement elem, MultiplicityType mult)
    {
        return getTypeName(elem, mult, null);
    }
    
    public String getTypeName(
        TypedElement elem, MultiplicityType mult, String suffix)
    {
        ModelElement type = elem.getType();
        if (type instanceof AliasType) {
            type = ((AliasType)type).getType();
        }
        String typeName = getTypeName(type, suffix);
        
        String collType = null;
        if (mult != null && (mult.getUpper() > 1 || mult.getUpper() == -1)) {
            if (mult.isOrdered()) {
                collType = ORDERED_COLLECTION_INTERFACE;
            } else {
                collType = COLLECTION_INTERFACE;
            }
        } else if (mult == null || mult.getLower() >= 1) {
            String primitiveTypeName = 
                Primitives.convertTypeNameToPrimitive(typeName);
            if (primitiveTypeName != null) {
                typeName = primitiveTypeName;
            }
        }
        
        if (collType != null) {
            return getCollectionType(collType, typeName);
        }

        return typeName;
    }
    
    // implements Generator
    public String getCollectionType(
        JavaClassReference collectionType, String elementType)
    {
        return getCollectionType(collectionType.toString(), elementType);
    }
    
    private String getCollectionType(String collectionType, String elementType)
    {
        StringBuffer result = new StringBuffer(collectionType);
        if (!enableGenerics) {
            result.append("/*");
        }

        result.append('<').append(elementType).append('>');

        if (!enableGenerics) {
            result.append("*/");
        }

        return result.toString();
    }
    
    // implements Generator
    public String getTypeName(ModelElement elem)
    {
        return getTypeName(elem, "");
    }

    // implements Generator
    public String getTypeName(ModelElement elem, String suffix)
    {
        String name = getSimpleTypeName(elem, suffix == null ? "" : suffix);
        
        if (elem instanceof PrimitiveType) {
            // REVIEW: SWZ: 11/07/2007: I think it's nicer to leave off the
            // extraneous "java.lang".  This differs from Netbeans MDR and
            // could cause problems if a model has unpackaged elements with
            // the same names as the Java primitive wrapper classes.  Note
            // that Primitives contains entries for the bare and fully
            // qualified versions.
            
            return /*"java.lang." + */ name;
        }
        
        return 
            getTypePrefix(elem, new StringBuilder())
            .append('.')
            .append(name)
            .toString();
    }
    
    // implements Generator
    public String getSimpleTypeName(ModelElement elem)
    {
        return getSimpleTypeName(elem, "");
    }
    
    // implements Generator
    public String getSimpleTypeName(ModelElement elem, String suffix)
    {
        String name = TagUtil.getTagValue(elem, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (name == null) {
            name = elem.getName();
        }
        
        if (elem instanceof PrimitiveType) {
            return name;
        } else if (elem instanceof Constant) {
            name = 
                StringUtil.mangleIdentifier(
                    name, StringUtil.IdentifierType.ALL_CAPS);
        } else {
            boolean initCaps = 
                elem instanceof MofClass || 
                elem instanceof MofPackage ||
                elem instanceof Association || 
                elem instanceof MofException ||
                elem instanceof StructureType || 
                elem instanceof EnumerationType ||
                elem instanceof CollectionType || 
                elem instanceof Import;
            name = 
                StringUtil.mangleIdentifier(
                    name, 
                    initCaps 
                        ? StringUtil.IdentifierType.CAMELCASE_INIT_UPPER 
                        : StringUtil.IdentifierType.CAMELCASE_INIT_LOWER);
        }

        // SPECIAL CASE: If the name happens to end with Exception, don't
        // double it.
        if (ExceptionHandler.EXCEPTION_SUFFIX.equals(suffix) &&
            name.endsWith(suffix))
        {
            return name;
        }
        
        return name + suffix;
    }
    
    private StringBuilder getTypePrefix(
        ModelElement elem, StringBuilder buffer)
    {
        ModelElement pkg = elem;

        while (!(pkg instanceof MofPackage)) {
            pkg = pkg.getContainer();
        }

        Namespace container = pkg.getContainer();
        if (container == null) {
            String pkgPrefix = 
                TagUtil.getTagValue(pkg, TagUtil.TAGID_PACKAGE_PREFIX);
            if (pkgPrefix != null) {
                // Package names are all-lowercase alphabetic.
                // REVIEW: SWZ: 10/31/2007: Make sure pkgPrefix doesn't 
                // contain non-alphabetic characters.
                buffer.append(pkgPrefix.toLowerCase(Locale.US)).append('.');
            }
        } else {
            getTypePrefix(container, buffer).append('.');
        }

        String packageName =
            TagUtil.getTagValue(pkg, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (packageName == null) {
            packageName = 
                StringUtil.mangleIdentifier(pkg.getName(), StringUtil.IdentifierType.ALL_LOWER);
        }

        // Package names are all-lowercase
        return buffer.append(packageName.toLowerCase(Locale.US));
    }

    // implements Generator
    public String getAccessorName(StructuralFeature feature)
    {
        return getAccessorName(feature, feature.getMultiplicity());
    }
    
    // implements Generator
    public String getAccessorName(TypedElement elem, MultiplicityType mult)
    {
        Classifier attribType = elem.getType();
        if (attribType instanceof AliasType) {
            attribType = ((AliasType)attribType).getType();
        }
        
        String attribName = elem.getName();
        String baseName = 
            attribName.length() <= 1
            ? attribName.toUpperCase(Locale.US)
            : attribName.substring(0, 1).toUpperCase(Locale.US) + 
                attribName.substring(1);
            
        String accessorName = null;

        // Upper bound -1 means infinity.
        if (mult == null || mult.getUpper() == 1) {
                
            if (attribType instanceof PrimitiveType && 
                attribType.getName().equals("Boolean"))
            {
                
                if (baseName.startsWith("Is")) {
                    accessorName = "is" + baseName.substring(2);
                } else {
                    accessorName = "is" + baseName;
                }
            } else {
                accessorName = "get" + baseName;
            }
            
        } else if (mult.getUpper() != 0) {
            accessorName = "get" + baseName;
        }

        return accessorName;
    }
    
    // implements Generator
    public String getMutatorName(StructuralFeature feature)
    {
        Classifier attribType = feature.getType();
        if (attribType instanceof AliasType) {
            attribType = ((AliasType)attribType).getType();
        }

        String attribName = feature.getName();
        String baseName = 
            attribName.length() <= 1
            ? attribName.toUpperCase(Locale.US)
            : attribName.substring(0, 1).toUpperCase(Locale.US) + 
                attribName.substring(1);
            
        String mutatorName = null;

        if (feature.getMultiplicity().getUpper() == 1 &&
            attribType instanceof PrimitiveType &&
            attribType.getName().equals("Boolean") &&
            baseName.startsWith("Is"))
        {
            mutatorName = "set" + baseName.substring(2);
        } else {
            mutatorName = "set" + baseName;
        }

        return mutatorName;
    }
    
    // implements Generator
    public String getEnumFieldName(String literal)
    {
        return StringUtil.mangleIdentifier(literal, StringUtil.IdentifierType.ALL_CAPS);
    }
    
    // implements Generator
    public String getClassFieldName(String literal)
    {
        return StringUtil.mangleIdentifier(literal, StringUtil.IdentifierType.CAMELCASE_INIT_LOWER);
    }
    
    public AssociationEnd[] getAssociationEnds(Association assoc)
    {
        List<?> contents = assoc.getContents();

        AssociationEnd[] ends = new AssociationEnd[2];
        Iterator<?> endIter = contents.iterator();
        int i = 0;
        while(endIter.hasNext()) {
            Object o = endIter.next();
            if (o instanceof AssociationEnd) {
                if (i >= 2) {
                    throw new IllegalStateException(
                        "Association has more than two ends");
                }
                
                AssociationEnd end = (AssociationEnd)o;
                
                ends[i++] = end;
            }
        }
        if (i != 2) {
            throw new IllegalStateException(
                "Association does not have exactly two ends");
        }
        
        return ends;
    }
    
    public AssociationKindEnum getAssociationKind(Association assoc)
    {
        AssociationEnd ends[] = getAssociationEnds(assoc);
        int[] upperBounds = new int[2];
        for(int i = 0; i < 2; i++) {
            AssociationEnd end = (AssociationEnd)ends[i];
                
            upperBounds[i] = end.getMultiplicity().getUpper();
        }
        
        if (upperBounds[0] == 1 && upperBounds[1] == 1) {
            return AssociationKindEnum.ONE_TO_ONE;
        } else if (upperBounds[0] != 1 && upperBounds[1] != 1) {
            return AssociationKindEnum.MANY_TO_MANY;            
        } else {
            return AssociationKindEnum.ONE_TO_MANY;
        }
    }
}
