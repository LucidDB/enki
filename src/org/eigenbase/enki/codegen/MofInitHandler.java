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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;

/**
 * MofInitHandler generates a subclass of 
 * {@link org.eigenbase.enki.jmi.impl.MetamodelInitializer} for
 * a particular metamodel.
 * 
 * @author Stephan Zuercher
 */
public class MofInitHandler
    extends JavaHandlerBase
    implements ClassInstanceHandler, AssociationHandler
{
    private static final String INIT_SUFFIX = "Init";

    private static final Set<String> JAVA_KEYWORDS;
    static {
        HashSet<String> set = new HashSet<String>();
        set.addAll(
            Arrays.asList(
                new String[] {
                    "abstract", "assert", "boolean", "break", "byte",
                    "case", "catch", "char", "class", "const",
                    "continue", "default", "do", "double", "else",
                    "enum", "extends", "final", "finally", "float",
                    "for", "goto", "if", "implements", "import",
                    "instanceof", "int", "interface", "long",
                    "native", "new", "package", "private",
                    "protected", "public", "return", "short",
                    "static", "strictfp", "super", "switch",
                    "synchronized", "this", "throw", "throws",
                    "transient", "try", "void", "volatile", "while",
                }));
        JAVA_KEYWORDS = Collections.unmodifiableSet(set);
    }
    
    private static final JavaClassReference METAMODEL_INITIALIZER =
        new JavaClassReference(
            org.eigenbase.enki.jmi.impl.MetamodelInitializer.class, true);
    
    private static final JavaClassReference MODEL_PACKAGE_INTERFACE =
        new JavaClassReference(ModelPackage.class, true);

    private static final JavaClassReference MODEL_PACKAGE_IMPL_CLASS =
        new JavaClassReference(
            org.eigenbase.enki.jmi.model.ModelPackage.class, false);
    
    private static final JavaClassReference JAVA_UTIL_ARRAYS =
        new JavaClassReference(Arrays.class, true);
    
    private static final JavaClassReference JAVA_UTIL_LIST =
        new JavaClassReference(List.class, true);
    
    private static final JavaClassReference JAVA_UTIL_COLLECTION=
        new JavaClassReference(Collection.class, true);
    
    private static final JavaClassReference[] INITIALIZER_REFS = {
        METAMODEL_INITIALIZER,
        MODEL_PACKAGE_INTERFACE,
        JAVA_UTIL_ARRAYS,
        JAVA_UTIL_LIST,
        JAVA_UTIL_COLLECTION,
    };
    
    private final Logger log = 
        Logger.getLogger(MofInitHandler.class.getName());

    private boolean isMetaMetamodel;
    private String prefixTagValue;
    private String prefixTagPackage;

    private List<MofClass> classes;
    private List<Association> associations;
    private Map<RefObject, Integer> indexMap;
    
    private SubordinateHandler subordinateHandler;
    
    public MofInitHandler()
    {
        super();
        
        this.classes = new ArrayList<MofClass>();
        this.associations = new ArrayList<Association>();
        this.indexMap = new IdentityHashMap<RefObject, Integer>();
        this.subordinateHandler = null;
    }
    
    public MofInitHandler(SubordinateHandler subordinateHandler)
    {
        this();
        
        this.subordinateHandler = subordinateHandler;
    }
    
    @Override
    public void beginGeneration()
        throws GenerationException
    {        
        super.beginGeneration();

        String simpleTypeName = "Initializer";
        
        String typeNamePrefix = computeInitializerPackage();
        
        String typeName = typeNamePrefix + "." + simpleTypeName;

        if (subordinateHandler != null) {
            subordinateHandler.setInitializerClassName(typeName);
        }
        
        open(typeName);
        
        isMetaMetamodel = 
            typeNamePrefix.startsWith(
                MofImplementationHandler.JMI_PACKAGE_PREFIX_SUBST);
        
        writeClassHeader(
            null,
            typeName,
            METAMODEL_INITIALIZER.toString(),
            new String[] { },
            JavaClassReference.computeImports(INITIALIZER_REFS),
            true,
            null);
        
        // Generic Metamodel constructor
        startBlock("public ", simpleTypeName, "(String extent)");
        writeln("super(extent);");
        endBlock();
    }

    @Override
    public void endGeneration(boolean throwing)
        throws GenerationException
    {
        if (!throwing) {
            if (!isMetaMetamodel) {
                RefBaseObject mofBase = ((MdrGenerator)generator).getExtent("MOF");
                
                ModelPackage mofModelPkg = (ModelPackage)mofBase;
                ModelPackage modelPkg = 
                    (ModelPackage)generator.getRefBaseObject();
                
                Collection<?> allClasses = 
                    mofModelPkg.getMofClass().refAllOfType();
                for(MofClass mofClass: 
                        GenericCollections.asTypedCollection(
                            allClasses, MofClass.class))
                {
                    String clsName = mofClass.getName();
                    
                    Collection<?> instances =
                        modelPkg.refClass(clsName).refAllOfType();
                    
                    generateClassInstanceImpl(mofClass, instances);
                }
                
                Collection<?> allAssociations = 
                    mofModelPkg.getAssociation().refAllOfType();
                for(Association mofAssoc: 
                        GenericCollections.asTypedCollection(
                            allAssociations, Association.class))
                {
                    String assocName = mofAssoc.getName();
                    
                    Collection<?> links =
                        modelPkg.refAssociation(assocName).refAllLinks();
                    
                    generateAssociationImpl(mofAssoc, links);
                }
            }
            
            newLine();
            emitSuppressWarningsAnnotation();
            startBlock("public void initMetamodel()");
            
            writeln("// Initialize Model Package");
            writeln(
                MODEL_PACKAGE_INTERFACE, 
                " modelPackage = new " +
                "", MODEL_PACKAGE_IMPL_CLASS, 
                "();");
            writeln("setModelPackage(modelPackage);");
    
            newLine();
            writeln("// Pass 1: Instances and attributes");
            for(MofClass cls: classes) {
                initElement(cls);
            }
    
            newLine();
            writeln("// Pass 2: References (associations)");
            for(Association assoc: associations) {
                initAssociations(assoc);
            }
        
            endBlock();
            writeEntityFooter();
            close();
        }
        
        super.endGeneration(throwing);
    }

    public void generateClassInstance(MofClass cls)
        throws GenerationException
    {
        if (!isMetaMetamodel) {
            return;
        }

        RefClass refClass = cls.refImmediatePackage().refClass(cls.getName());
        Collection<?> instances = refClass.refAllOfClass();

        generateClassInstanceImpl(cls, instances);
    }
    
    private void generateClassInstanceImpl(
        MofClass cls, Collection<?> instances)
    throws GenerationException
    {
        if (cls.isAbstract()) {
            log.fine("Skipping class initializer for " + cls.getName());   
            return;
        }
        
        log.fine("Generating class initializer for " + cls.getName());
        
        log.finest(
            "MofClass " + cls.getName() + " is a " + cls.getClass().getName());
        
        classes.add(cls);

        String var = makeVarName(cls);
        String func = makeVarName(cls, INIT_SUFFIX);
        String clsName = 
            generator.getTagValue(cls, Generator.TAGID_SUBSTITUTE_NAME);
        if (clsName == null) {
            clsName = cls.getName();
        }
        
        boolean requiresWarningSupression = false;
        Collection<Attribute> attribs = 
            contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS, 
                ScopeKindEnum.INSTANCE_LEVEL, 
                Attribute.class);

        if (!instances.isEmpty()) {
            newLine();
            writeln("private ", clsName, "[] ", var, ";");
        
            for(Attribute attrib: attribs) {
                if (!attrib.isDerived()) {
                    if (attrib.getMultiplicity().getUpper() != 1) {
                        requiresWarningSupression = true;
                        break;
                    }
                }
            }
        }
        
        newLine();
        if (requiresWarningSupression) {
            emitSuppressWarningsAnnotation();
        }
        startBlock("private void ", func, "()");
        
        if (!instances.isEmpty()) {
            writeln(var, " = new ", clsName, "[", instances.size(), "];");
    
            int index = 0;
            for(RefObject refObj: 
                    GenericCollections.asTypedCollection(
                        instances, RefObject.class))
            {
                indexMap.put(refObj, index);
                
                newLine();
                writeln(
                    var, "[", index, "] = getModelPackage().get", clsName, 
                    "().create", clsName, "(");
                increaseIndent();
                
                boolean first = true;
                for(Attribute attrib: attribs) {
                    if (attrib.isDerived()) {
                        continue;
                    }
                    
                    if (first) {
                        first = false;
                    } else {
                        writeln(",");
                    }
                    
                    Object value = refObj.refGetValue(attrib.getName());
                    
                    writeValue(attrib, value);
                }
                writeln(");");
                decreaseIndent();
                index++;
            }
        }
        
        endBlock();
    }

    public void generateAssociation(Association assoc)
        throws GenerationException
    {
        if (!isMetaMetamodel) {
            return;
        }
        
        RefAssociation refAssoc =
            assoc.refImmediatePackage().refAssociation(assoc.getName());
        
        Collection<?> links = refAssoc.refAllLinks();

        generateAssociationImpl(assoc, links);
    }

    private void generateAssociationImpl(
        Association assoc, Collection<?> links)
    throws GenerationException
    {
        AssociationEnd[] assocEnds =
            contentsOfType(assoc, AssociationEnd.class)
            .toArray(new AssociationEnd[2]);
  
        if (!assocEnds[0].isChangeable() && !assocEnds[1].isChangeable()) {
            log.fine(
                "Skipping association for unmodifiable association " + 
                assoc.getName());
            return;
        }
        
        log.fine("Generating association initializer for " + assoc.getName());

        String var = makeVarName(assoc);
        String func = makeVarName(assoc, INIT_SUFFIX);
        
        associations.add(assoc);
        
        newLine();
        startBlock("private void ", func, "()");
        
        write(assoc.getName(), " ", var, " = ");
        List<?> qualifiedName = assoc.getQualifiedName();
        for(Iterator<?> iter = qualifiedName.iterator(); iter.hasNext(); ) {
            String nameElem = iter.next().toString();
            
            if (iter.hasNext()) {
                write("get", nameElem, "Package().");
            } else {
                writeln("get", nameElem, "();");
            }
        }
        
        ArrayList<RefAssociationLink> sortedLinks = 
            new ArrayList<RefAssociationLink>(
                GenericCollections.asTypedCollection(
                    links, RefAssociationLink.class));
        Collections.sort(sortedLinks, new Comparator<RefAssociationLink>() {
            public int compare(RefAssociationLink o1, RefAssociationLink o2)
            {
                RefObject[][] data = {
                    { o1.refFirstEnd(), o2.refFirstEnd() },
                    { o1.refSecondEnd(), o2.refSecondEnd() }
                };

                for(int i = 0; i < data.length; i++) {
                    RefObject e1 = data[i][0];
                    RefObject e2 = data[i][1];
                    
                    String o1ClassName = 
                        (String)e1.refClass().refMetaObject().refGetValue(
                            "name");
                    String o2ClassName =
                        (String)e2.refClass().refMetaObject().refGetValue(
                            "name");
                
                    int c = o1ClassName.compareTo(o2ClassName);
                    if (c != 0) {
                        return c;
                    }

                    int o1Index = indexMap.get(e1);
                    int o2Index = indexMap.get(e2);
                    
                    c = o1Index - o2Index;
                    if (c != 0) {
                        return c;
                    }
                }
                
                return 0;
            }
        });
        
        newLine();
        for(RefAssociationLink link: sortedLinks) {
            RefObject first = link.refFirstEnd();
            Classifier firstCls = (Classifier)first.refClass().refMetaObject();
            String firstFieldName = makeVarName(firstCls);
            int firstIndex = indexMap.get(first);
            
            RefObject second = link.refSecondEnd();
            Classifier secondCls = 
                (Classifier)second.refClass().refMetaObject();            
            String secondFieldName= makeVarName(secondCls);
            int secondIndex = indexMap.get(second);
            
            writeln(
                var, ".add(", firstFieldName, "[", firstIndex, "], ",
                secondFieldName, "[", secondIndex, "]);");
        }
        
        endBlock();
    }
    
    private String computeInitializerPackage() throws GenerationException
    {
        RefBaseObject refBaseObject = generator.getRefBaseObject();
        if (!(refBaseObject instanceof ModelPackage)) {
            throw new GenerationException(
                "Expected RefBaseObject to be a javax.jmi.model.ModelPackage not a " + 
                refBaseObject.getClass().getName());
        }
        
        ModelPackage modelPackage = (ModelPackage)refBaseObject;
        Collection<?> pkgs = modelPackage.getMofPackage().refAllOfType();
        for(MofPackage pkg: 
                GenericCollections.asTypedCollection(pkgs, MofPackage.class))
        {
            // Ignore these ancillary types.
            if (pkg.getName().equals("PrimitiveTypes") ||
                pkg.getName().equals("CorbaIdlTypes"))
            {
                continue;
            }
         
            if (pkg.getContainer() != null) {
                continue;
            }
            
            for(Tag tag: contentsOfType(pkg, Tag.class)) {
                if (!tag.getTagId().equals("javax.jmi.packagePrefix")) {
                    continue;
                }
                List<?> values = tag.getValues();

                prefixTagValue = values.get(0).toString();
            }
            
            prefixTagPackage = pkg.getName();
        }
        
        String packageName;
        if (prefixTagValue != null) {
            packageName = 
                prefixTagValue + 
                "." + 
                prefixTagPackage.toLowerCase(Locale.US) +
                ".init";
        } else {
            packageName = 
                prefixTagPackage.toLowerCase(Locale.US) +
                ".init";
        }
        
        if (!packageName.startsWith(
                MofImplementationHandler.JMI_PACKAGE_PREFIX))
        {
            return packageName;
        }
        
        packageName = 
            MofImplementationHandler.JMI_PACKAGE_PREFIX_SUBST + 
            packageName.substring(
                MofImplementationHandler.JMI_PACKAGE_PREFIX.length());
        
        return packageName;
    }
    
    private void initElement(MofClass mofClass)
    throws GenerationException
    {
        String funcName = makeVarName(mofClass, INIT_SUFFIX);
        writeln(funcName, "();");
    }
    
    private void initAssociations(Association assoc)
    throws GenerationException
    {
        String funcName = makeVarName(assoc, INIT_SUFFIX);
        writeln(funcName, "();");
    }

    private String makeVarName(ModelElement modelElem) 
    throws GenerationException
    {
        return makeVarName(modelElem, null);
    }
    
    private String makeVarName(ModelElement modelElem, String suffix) 
    throws GenerationException
    {
        StringBuilder varName = new StringBuilder();
        
        varName.append(toInitialLower(modelElem.getName()));
        
        if (modelElem instanceof MofPackage) {
            varName.append("Pkg");
        } else if (modelElem instanceof MofClass) {
        } else if (modelElem instanceof Association) {
            varName.append("Assoc");
        } else {
            throw new GenerationException(
                "Unexpected var type " + 
                modelElem.getClass().getName());
        }
        
        if (suffix != null) {
            varName.append(suffix);
        }

        if (JAVA_KEYWORDS.contains(varName.toString())) {
            varName.append('_');
        }
        
        return varName.toString();
    }

    private String getEnumType(Classifier classifier, RefEnum value)
    {
        StringBuilder enumType = new StringBuilder("javax.jmi");

        boolean isClassContained = 
            classifier.getContainer() instanceof MofClass;
        
        List<?> refTypeName = value.refTypeName();
        int size = refTypeName.size();
        for(int i = 0; i < size; i++) {
            String typeElem = refTypeName.get(i).toString();

            if (isClassContained && i == size - 2) {
                continue;
            }
            
            if (enumType.length() > 0) {
                enumType.append(".");
            }
            
            if (i < size - 1) {
                typeElem = typeElem.toLowerCase(Locale.US);
            }

            enumType.append(typeElem);
        }
        enumType.append("Enum");

        return enumType.toString();
    }
    
    private void writeStruct(Attribute attrib, RefStruct struct)
    {
        boolean isClassContained = 
            attrib.getType().getContainer() instanceof MofClass;

        List<?> refTypeName = struct.refTypeName();
        int size = refTypeName.size();
        for(int i = 0; i < size - 1; i++) {
            String typeElem = refTypeName.get(i).toString();

            if (i > 0) {
                write(".");
            }

            if (isClassContained && i == size - 2) {
                write("get", typeElem, "Class()");
            } else {
                write("get", typeElem, "Package()");
            }
        }
                
        String typeElem = refTypeName.get(size - 1).toString();

        writeln(".create", typeElem, "(");
        increaseIndent();
        
        List<?> refFieldNames = struct.refFieldNames();
        for(Iterator<?> iter = refFieldNames.iterator(); iter.hasNext(); ) {
            String fieldName = iter.next().toString();
            
            Object value = struct.refGetValue(fieldName);
            
            writeSimpleValue(value);
            if (iter.hasNext()) {
                writeln(",");
            }
        }
        write(")");
        decreaseIndent();
    }
    
    private void writeValue(Attribute attrib, Object value)
    {
        if (value instanceof RefEnum) {
            write(
                getEnumType(attrib.getType(), (RefEnum)value), 
                ".", 
                value.toString().toUpperCase(Locale.US));
        } else if (value instanceof RefStruct) {
            RefStruct struct = (RefStruct)value;

            writeStruct(attrib, struct);
        } else if (value instanceof Collection) {
            writeln("Arrays.asList(");
            increaseIndent();
            
            Collection<?> collection = (Collection<?>)value;
            boolean first = true;
            for(Object o: collection) {
                if (first) {
                    first = false;
                } else {
                    writeln(",");
                }
                
                writeValue(attrib, o);
            }
            write(")");
            decreaseIndent();
        } else {
            writeSimpleValue(value);
        }
    }
    
    private void writeSimpleValue(Object value)
    {
        if (value instanceof String) {
            String str = 
                ((String)value)
                    .replace("\n", "\\n")
                    .replace(QUOTE, "\\" + QUOTE);
            
            write(QUOTE, str, QUOTE);
        } else {
            write(String.valueOf(value));
        }
    }
    
    /**
     * SubordinateHandler is implemented by other {@link Handler} 
     * implementations which wish to learn the name of the initializer
     * class generated by {@link MofInitHandler}.
     */
    public static interface SubordinateHandler
    {
        public void setInitializerClassName(String initializerClassName);
    }
}

// End MofInitHander.java
