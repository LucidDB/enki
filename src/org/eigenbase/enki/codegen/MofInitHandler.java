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

    private List<MofClass> classes;
    private List<Association> associations;
    private Map<RefObject, Integer> indexMap;
    private Map<String, String> classMetaObjInitMap;
    private Map<String, String> associationMetaObjInitMap;
    
    private SubordinateHandler subordinateHandler;
    
    public MofInitHandler()
    {
        super();
        
        this.classes = new ArrayList<MofClass>();
        this.associations = new ArrayList<Association>();
        this.indexMap = new IdentityHashMap<RefObject, Integer>();
        this.classMetaObjInitMap = new HashMap<String, String>();
        this.associationMetaObjInitMap = new HashMap<String, String>();
        
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
                RefBaseObject mofBase = 
                    ((MdrGenerator)generator).getExtent("MOF");
                
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
                    
                    Collection<RefObject> instances =
                        GenericCollections.asTypedCollection(
                            modelPkg.refClass(clsName).refAllOfType(),
                            RefObject.class);
                    
                    generateClassInstanceImpl(mofClass, instances);
                }
                
                Collection<?> allAssociations = 
                    mofModelPkg.getAssociation().refAllOfType();
                for(Association mofAssoc: 
                        GenericCollections.asTypedCollection(
                            allAssociations, Association.class))
                {
                    String assocName = mofAssoc.getName();
                    
                    generateAssociationImpl(
                        mofAssoc, modelPkg.refAssociation(assocName));
                }
            }
            
            newLine();
            emitSuppressWarningsAnnotation();
            startBlock("public void initMetamodel()");
            
            if (!pluginMode) {
                writeln("// Initialize Model Package");
                writeln(
                    MODEL_PACKAGE_IMPL_CLASS, 
                    " modelPackage = new " +
                    "", MODEL_PACKAGE_IMPL_CLASS, 
                    "();");
                writeln("setModelPackage(modelPackage);");
                if (!isMetaMetamodel) {
                    writeln(
                        "setRefMetaObject(modelPackage, ", 
                        QUOTE, "Model", QUOTE, ");");
                }
            }
            
            newLine();
            writeln("// Initialize MOF Instances");
            for(MofClass cls: classes) {
                initElement(cls);
            }
    
            newLine();
            writeln("// Initialize MOF Associations");
            for(Association assoc: associations) {
                initAssociations(assoc);
            }
            
            if (!pluginMode) {
                newLine();
                writeln("// Initialize M3 meta objects");
                for(Map.Entry<String, String> entry: 
                    classMetaObjInitMap.entrySet())
                {
                    writeln(
                        "setRefMetaObject(getModelPackage().get", entry.getKey(),
                        "(), findMofClassByName(", 
                        QUOTE, entry.getValue(), QUOTE, ", true));");
                }
                for(Map.Entry<String, String> entry: 
                    associationMetaObjInitMap.entrySet())
                {
                    writeln(
                        "setRefMetaObject(getModelPackage().get", entry.getKey(),
                        "(), findAssociationByName(", 
                        QUOTE, entry.getValue(), QUOTE, ", true));");
                }
                if (isMetaMetamodel) {
                    writeln(
                        "setRefMetaObject(getModelPackage(), findMofPackageByName(",
                        QUOTE, "Model", QUOTE, ", true));");
                }
            }
            
            customInitialization();
            
            endBlock();
            
            if (pluginMode) {
                customStitchPackages();
            }
            
            writeEntityFooter();
            close();
        }
        
        super.endGeneration(throwing);
    }

    protected void customInitialization() throws GenerationException
    {
        // Default does nothing.
    }
    
    protected void customStitchPackages() throws GenerationException
    {
        // Default does nothing.
    }
    
    public void generateClassInstance(MofClass cls)
        throws GenerationException
    {
        if (!isMetaMetamodel) {
            return;
        }

        RefClass refClass = cls.refImmediatePackage().refClass(cls.getName());
        Collection<RefObject> instances = 
            GenericCollections.asTypedCollection(
                refClass.refAllOfClass(), RefObject.class);

        generateClassInstanceImpl(cls, instances);
    }
    
    private void generateClassInstanceImpl(
        MofClass cls, Collection<RefObject> instances)
    throws GenerationException
    {
        String clsName = TagUtil.getSubstName(cls);
        classMetaObjInitMap.put(clsName, cls.getName());

        if (cls.isAbstract()) {
            log.fine("Skipping class initializer for " + cls.getName());   
            return;
        }
        
        log.fine("Generating class initializer for " + cls.getName());
        classes.add(cls);

        String var = makeVarName(cls);
        String func = makeVarName(cls, INIT_SUFFIX);

        boolean requiresWarningSupression = false;
        Collection<Attribute> attribs = 
            CodeGenUtils.contentsOfType(
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
            for(RefObject refObj: instances) {
                indexMap.put(refObj, index);
                
                ModelElement owner = (ModelElement)refObj;
                if (owner instanceof Tag) {
                    // Tags are special cases.
                    Tag tag = (Tag)owner;
                    for(ModelElement tagged: 
                            GenericCollections.asTypedCollection(
                                tag.getElements(), ModelElement.class))
                    {
                        while(!isFirstClass(tagged)) {
                            tagged = tagged.getContainer();
                        }

                        // Cause isIncluded(owner) to evaluate properly.
                        owner = tagged;
                        
                        if (!isIncluded(tagged)) {
                            break;
                        }
                    }
                } else {
                    while(!isFirstClass(owner)) {
                        owner = owner.getContainer();
                    }
                }                
                if (isIncluded(owner)) {
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
                } else if (pluginMode && refObj instanceof MofClass) {
                    newLine();
                    writeln(
                        var, "[", index, "] = findMofClassByName(", 
                        QUOTE, refObj.refGetValue("name"), QUOTE, 
                        ", false);");
                } else if (pluginMode && refObj instanceof MofPackage) {
                    newLine();
                    writeln(
                        var, "[", index, "] = findMofPackageByName(", 
                        QUOTE, refObj.refGetValue("name"), QUOTE, 
                        ", false);");
                } else if (pluginMode && refObj instanceof Association) {
                    newLine();
                    writeln(
                        var, "[", index, "] = findAssociationByName(", 
                        QUOTE, refObj.refGetValue("name"), QUOTE, 
                        ", false);");
                } else if (pluginMode && refObj instanceof Tag) {
                    // Tag shared across plugin/base.  Hard to handle since
                    // tags are not uniquely identified by any attribute.
                } else {
                    newLine();
                    ModelElement refMetaObject = 
                        (ModelElement)refObj.refMetaObject();
                    String typeName = CodeGenUtils.getTypeName(refMetaObject);
                    String className = 
                        refMetaObject.refGetValue("name").toString();
                    String instanceName = 
                        refObj.refGetValue("name").toString();
                    writeln(
                        var, "[", index, "] = (", typeName, ")findGeneric(",
                        QUOTE, className, QUOTE, ", ",
                        QUOTE, instanceName, QUOTE, ");");
                }
                index++;
            }
        }
        
        endBlock();
    }

    private boolean isFirstClass(ModelElement owner)
    {
        return owner instanceof MofClass || 
              owner instanceof MofPackage || 
              owner instanceof Association;
    }

    public void generateAssociation(Association assoc)
        throws GenerationException
    {
        if (!isMetaMetamodel) {
            return;
        }
        
        RefAssociation refAssoc =
            assoc.refImmediatePackage().refAssociation(assoc.getName());
        
        generateAssociationImpl(assoc, refAssoc);
    }

    private void generateAssociationImpl(
        Association assoc, RefAssociation refAssociation)
    throws GenerationException
    {
        String assocName = TagUtil.getSubstName(assoc);
        associationMetaObjInitMap.put(assocName, assoc.getName());

        AssociationEnd[] assocEnds =
            CodeGenUtils.contentsOfType(assoc, AssociationEnd.class)
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

        // Need to preserve collection ordering.  So, determine which end of
        // the association is ordered (if neither, we behave as if end 2 is
        // ordered, even though it's not).  We built a list of the unique
        // query ends (e.g., the instances of the unordered end of the 
        // association), then query for the associated instances of the ordered
        // end of the association and emit our initializer in the order that
        // those objects are returned.
        
        boolean queryByFirstEnd = assocEnds[1].getMultiplicity().isOrdered();
        boolean queryBySecondEnd = assocEnds[0].getMultiplicity().isOrdered();
        
        // REVIEW: SWZ: 12/19/07: How would we handle the following case?  Not trying for now.        
        // One or both must be false.  If both are true, the exception is thrown.
        if (queryByFirstEnd && queryBySecondEnd) {
            throw new GenerationException(
                "Internal error: Cannot handle many-to-many ordered associations");
        }

        // Only keep unique ends, but preserve order.
        LinkedHashSet<RefObject> queryEnds = new LinkedHashSet<RefObject>();
        for(RefAssociationLink link: 
                GenericCollections.asTypedCollection(
                    refAssociation.refAllLinks(), 
                    RefAssociationLink.class))
        {
            if (queryBySecondEnd) {
                queryEnds.add(link.refSecondEnd());
            } else {
                queryEnds.add(link.refFirstEnd());
            }
        }
        
        newLine();
        for(RefObject queryEnd: queryEnds) {
            
            String queryEndName;
            if (queryBySecondEnd) {
                queryEndName = assocEnds[1].getName();
            } else {
                queryEndName = assocEnds[0].getName();
            }
            
            Collection<RefObject> multiple = 
                GenericCollections.asTypedCollection(
                    refAssociation.refQuery(queryEndName, queryEnd), 
                    RefObject.class);
            
            Classifier queryEndCls = 
                (Classifier)queryEnd.refClass().refMetaObject();
            String queryEndFieldName = makeVarName(queryEndCls);
            int queryEndIndex = indexMap.get(queryEnd);
            boolean queryEndIncluded = isIncluded((ModelElement)queryEnd);
            
            for(RefObject otherEnd: multiple) {
                Classifier otherEndCls = 
                    (Classifier)otherEnd.refClass().refMetaObject();            
                String otherEndFieldName= makeVarName(otherEndCls);
                int otherEndIndex = indexMap.get(otherEnd);
                boolean otherEndIncluded;
                if (otherEnd instanceof Tag) {
                    otherEndIncluded = queryEndIncluded;
                } else {
                    otherEndIncluded = isIncluded((ModelElement)otherEnd);
                }

                if (!pluginMode && (!queryEndIncluded || !otherEndIncluded)) {
                    // Normal mode, but one end or the other is not included,
                    // so don't generate the association add.
                    continue;
                } else if (pluginMode && 
                           (!queryEndIncluded && !otherEndIncluded))
                {
                    // Plugin mode, neither end is included, so don't generate
                    // the association add: it's already been done elsewhere.
                    continue;
                }
                
                if (queryBySecondEnd) {
                    writeln(
                        var, 
                        ".add(", otherEndFieldName, "[", otherEndIndex, "], ",
                        queryEndFieldName, "[", queryEndIndex, "]);");
                } else {
                    writeln(
                        var, 
                        ".add(", queryEndFieldName, "[", queryEndIndex, "], ",
                        otherEndFieldName, "[", otherEndIndex, "]);");
                }
            }
        }
        
        endBlock();
    }
    
    protected String computeInitializerPackage() throws GenerationException
    {
        RefBaseObject refBaseObject = generator.getRefBaseObject();
        if (!(refBaseObject instanceof ModelPackage)) {
            throw new GenerationException(
                "Expected RefBaseObject to be a javax.jmi.model.ModelPackage not a " + 
                refBaseObject.getClass().getName());
        }
        
        ModelPackage modelPackage = (ModelPackage)refBaseObject;
        
        String packageName = 
            TagUtil.getFullyQualifiedPackageName(modelPackage);
        packageName += ".init";
        
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
        
        varName.append(StringUtil.toInitialLower(modelElem.getName()));
        
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
