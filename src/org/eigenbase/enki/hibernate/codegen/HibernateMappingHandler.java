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
package org.eigenbase.enki.hibernate.codegen;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;

/**
 * HibernateMappingHandler generates Hibernate mapping file and an Enki
 * Hibernate provider configuration file.  HibernateMappingHandler takes
 * two passes over the model.  The first pass is used to generate Hibernate
 * typedef declarations for enumerations.  The second pass generates Hibernate
 * class declarations for persistent entities.
 * 
 * @author Stephan Zuercher
 */
public class HibernateMappingHandler
    extends XmlHandlerBase
    implements ClassInstanceHandler, AssociationHandler, PackageHandler, 
               EnumerationClassHandler, MofInitHandler.SubordinateHandler
{
    /** Name of the column that stores an entity's MOF ID. */
    private static final String MOF_ID_COLUMN_NAME = "mofId";

    /** Name of the object property that stores an entity's MOF ID. */
    private static final String MOF_ID_PROPERTY_NAME = "mofId";

    /** Suffix for enumeration type defs. */
    private static final String TYPEDEF_SUFFIX = "Type";

    /** Name of the type property for association. */
    private static final String ASSOC_TYPE_PROPERTY = "type";
    
    /** Name of the reversed property for association. */
    private static final String ASSOC_REVERSE_POLARITY_PROPERTY = 
        "reversed";

    private static final String ASSOC_ONE_TO_ONE_TABLE = "AssocOneToOne";
    private static final String ASSOC_ONE_TO_ONE_PARENT_PROPERTY = "parent";
    private static final String ASSOC_ONE_TO_ONE_PARENT_TYPE_COLUMN = 
        "parent_type";
    private static final String ASSOC_ONE_TO_ONE_PARENT_ID_COLUMN =
        "parent_id";
    private static final String ASSOC_ONE_TO_ONE_CHILD_PROPERTY = "child";
    private static final String ASSOC_ONE_TO_ONE_CHILD_TYPE_COLUMN = 
        "child_type";
    private static final String ASSOC_ONE_TO_ONE_CHILD_ID_COLUMN =
        "child_id";

    private static final String ASSOC_ONE_TO_MANY_TABLE = "AssocOneToMany";
    private static final String ASSOC_ONE_TO_MANY_PARENT_PROPERTY = "parent";
    private static final String ASSOC_ONE_TO_MANY_PARENT_TYPE_COLUMN = 
        "parent_type";
    private static final String ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN =
        "parent_id";
    private static final String ASSOC_ONE_TO_MANY_CHILDREN_PROPERTY = 
        "children";
    private static final String ASSOC_ONE_TO_MANY_CHILD_KEY_COLUMN = 
        "child_key";
    private static final String ASSOC_ONE_TO_MANY_CHILD_ORDINAL_COLUMN = 
        "child_ordinal";

    private static final String ASSOC_ONE_TO_MANY_CHILDREN_TABLE =
        "AssocOneToManyChildren";
    private static final String ASSOC_ONE_TO_MANY_CHILD_TYPE_COLUMN = 
        "child_type";
    private static final String ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN = 
        "child_id";
    
    private static final String ASSOC_MANY_TO_MANY_TABLE = "AssocManyToMany";
    private static final String ASSOC_MANY_TO_MANY_SOURCE_PROPERTY = "source";
    private static final String ASSOC_MANY_TO_MANY_SOURCE_TYPE_COLUMN = 
        "source_type";
    private static final String ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN =
        "source_id";
    private static final String ASSOC_MANY_TO_MANY_TARGET_PROPERTY = "target";
    private static final String ASSOC_MANY_TO_MANY_TARGET_TABLE = 
        "AssocManyToManyTarget";
    private static final String ASSOC_MANY_TO_MANY_TARGET_KEY_COLUMN = 
        "target_key";
    private static final String ASSOC_MANY_TO_MANY_TARGET_ORDINAL_COLUMN = 
        "target_ordinal";
    private static final String ASSOC_MANY_TO_MANY_TARGET_TYPE_COLUMN = 
        "target_type";
    private static final String ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN = 
        "target_id";

    /**
     * Default prefix for association entity names.
     */
    public static final String DEFAULT_ASSOC_ENTITY_PREFIX = "_ENTITY_";

    // Association HQL query names and named parameters.
    public static final String QUERY_NAME_ALLLINKS = "allLinks";

    public static final String QUERY_PARAM_ALLLINKS_TYPE = "type";

    // Class HQL query names and named parameters
    public static final String QUERY_NAME_ALLOFCLASS = "allOfClass";
    public static final String QUERY_NAME_ALLOFTYPE = "allOfType";
    
    private static final JavaClassReference BOOLEAN_PROPERTY_ACCESSOR_CLASS =
        new JavaClassReference(BooleanPropertyAccessor.class, false);
    
    private static final JavaClassReference ENUM_USER_TYPE_CLASS =
        new JavaClassReference(EnumUserType.class, false);
    
    private Set<Classifier> oneToOneParentTypeSet;
    private Set<Classifier> oneToOneChildTypeSet;

    private Set<Classifier> oneToManyParentTypeSet;
    private Set<Classifier> oneToManyChildTypeSet;
    
    private Set<Classifier> manyToManySourceTypeSet;
    private Set<Classifier> manyToManyTargetTypeSet;

    private Map<Classifier, Set<Classifier>> subTypeMap;
    private Set<Classifier> allTypes;
    
    /** Maps a component type to a list of references to it. */
    private Map<Classifier, List<ComponentInfo>> componentAttribMap;

    private final Logger log = 
        Logger.getLogger(HibernateMappingHandler.class.getName());

    private File metaInfEnkiDir;

    private String topLevelPackage;
    
    private String extentName;
    
    private String tablePrefix;
    
    private String initializerName;

    private Map<Association, AssociationInfo> assocInfoMap;

    private JavaClassReference assocOneToOneClass;
    private JavaClassReference assocOneToManyClass;
    private JavaClassReference assocManyToManyClass;
    
    public HibernateMappingHandler()
    {
        this.oneToOneParentTypeSet = new LinkedHashSet<Classifier>();
        this.oneToOneChildTypeSet = new LinkedHashSet<Classifier>();
        
        this.oneToManyParentTypeSet = new LinkedHashSet<Classifier>();
        this.oneToManyChildTypeSet = new LinkedHashSet<Classifier>();
        
        this.manyToManySourceTypeSet = new LinkedHashSet<Classifier>();
        this.manyToManyTargetTypeSet = new LinkedHashSet<Classifier>();
        
        this.subTypeMap = new LinkedHashMap<Classifier, Set<Classifier>>();
        this.allTypes = new LinkedHashSet<Classifier>();
        
        this.assocInfoMap = new LinkedHashMap<Association, AssociationInfo>();

        this.componentAttribMap = 
            new HashMap<Classifier, List<ComponentInfo>>();
    }

    public void setExtentName(String extentName)
    {
        this.extentName = extentName;
    }
    
    public void setTablePrefix(String tablePrefix)
    {
        this.tablePrefix = tablePrefix;
    }

    public void setInitializerClassName(String initializerName)
    {
        this.initializerName = initializerName;
    }
    
    @Override
    public int getNumPasses()
    {
        return 2;
    }
    
    // Implement Handler
    public void beginGeneration() throws GenerationException
    {        
        super.beginGeneration();

        ModelPackage modelPackage = 
            (ModelPackage)generator.getRefBaseObject();
        String packageName = 
            TagUtil.getFullyQualifiedPackageName(modelPackage);
        
        assocOneToOneClass = 
            new JavaClassReference(
                packageName,
                HibernateJavaHandler.ASSOCIATION_ONE_TO_ONE_BASE.toSimple());
        assocOneToManyClass = 
            new JavaClassReference(
                packageName,
                HibernateJavaHandler.ASSOCIATION_ONE_TO_MANY_BASE.toSimple());
        assocManyToManyClass = 
            new JavaClassReference(
                packageName,
                HibernateJavaHandler.ASSOCIATION_MANY_TO_MANY_BASE.toSimple());

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
        
        File file = 
            new File(metaInfEnkiDir, HibernateMDRepository.MAPPING_XML);
        
        open(file);
        
        writeXmlDecl();
        writeDocType(
            "hibernate-mapping",
            "-//Hibernate/Hibernate Mapping DTD 3.0//EN",
            "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd");
        newLine();

        startElem("hibernate-mapping");
    }
    
    // Implement Handler
    public void endGeneration(boolean throwing) throws GenerationException
    {
        if (!throwing) {
            for(Map.Entry<Classifier, List<ComponentInfo>> entry:
                    componentAttribMap.entrySet())
            {
                Classifier referencedType = entry.getKey();
                for(ComponentInfo componentInfo: entry.getValue()) {
                    MofClass ownerType = componentInfo.getOwnerType();
                    switch(componentInfo.getKind()) {
                    case ONE_TO_ONE:
                        oneToOneParentTypeSet.add(ownerType);
                        oneToOneChildTypeSet.add(referencedType);
                        break;
                    
                    case ONE_TO_MANY:
                        oneToManyParentTypeSet.add(ownerType);
                        oneToManyChildTypeSet.add(referencedType);
                        break;
                        
                    case MANY_TO_MANY:
                        manyToManySourceTypeSet.add(ownerType);
                        manyToManyTargetTypeSet.add(referencedType);
                        break;
                    }
                }
            }
            
            if (!oneToOneParentTypeSet.isEmpty()) {
                writeOneToOneMapping();
                newLine();
            }
            
            if (!oneToManyParentTypeSet.isEmpty()) {
                writeOneToManyMapping();
                newLine();
            }
            
            if (!manyToManySourceTypeSet.isEmpty()) {
                writeManyToManyMapping();
            }
            
            generateAllOfTypeQueries();
            
            endElem("hibernate-mapping");
        }
        
        close();
                
        if (!throwing) {
            if (initializerName == null) {
                throw new GenerationException("Unknown initializer");
            }
            
            File enkiConfigFile = 
                new File(
                    metaInfEnkiDir, HibernateMDRepository.CONFIG_PROPERTIES);
            open(enkiConfigFile);

            writeln("# Generated Enki Metamodel Properties");
            newLine();
            
            writeln(
                MDRepositoryFactory.PROPERTY_ENKI_IMPLEMENTATION, "=", 
                MdrProvider.ENKI_HIBERNATE.name());
            writeln(
                MDRepositoryFactory.PROPERTY_ENKI_TOP_LEVEL_PKG, "=", 
                topLevelPackage);
            writeln(
                MDRepositoryFactory.PROPERTY_ENKI_EXTENT, "=", extentName);
            writeln(
                HibernateMDRepository.PROPERTY_MODEL_INITIALIZER, "=", 
                initializerName);
            close();
        }
        
        super.endGeneration(throwing);
    }

    private void writeOneToOneMapping() throws GenerationException
    {
        Map<String, String> oneToOneParentTypeMap =
            buildTypeMap(oneToOneParentTypeSet);
        Map<String, String> oneToOneChildTypeMap = 
            buildTypeMap(oneToOneChildTypeSet);
        
        int parentLength = 64;
        for(String key: oneToOneParentTypeMap.keySet()) {
            parentLength = Math.max(parentLength, key.length());
        }
        int childLength = 64;
        for(String key: oneToOneChildTypeMap.keySet()) {
            childLength = Math.max(childLength, key.length());
        }

        startElem(
            "class",
            "name", assocOneToOneClass,
            "table", tableName(ASSOC_ONE_TO_ONE_TABLE));
        
        writeEmptyElem("cache", "usage", "read-write");
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", String.valueOf(parentLength));
        
        startElem(
            "any",
            "name", ASSOC_ONE_TO_ONE_PARENT_PROPERTY,
            "id-type", "long",
            "meta-type", "string",
            "cascade", "save-update");
        for(Map.Entry<String, String> entry: oneToOneParentTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        
        writeEmptyElem(
            "column", "name", ASSOC_ONE_TO_ONE_PARENT_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_ONE_PARENT_ID_COLUMN);
        endElem("any");

        startElem(
            "any",
            "name", ASSOC_ONE_TO_ONE_CHILD_PROPERTY,
            "id-type", "long",
            "meta-type", "string",
            "cascade", "save-update");
        for(Map.Entry<String, String> entry: oneToOneChildTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        
        writeEmptyElem(
            "column", "name", ASSOC_ONE_TO_ONE_CHILD_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_ONE_CHILD_ID_COLUMN);
        endElem("any");
        
        // Named queries
        newLine();
        startElem(
            "query", 
            "name", QUERY_NAME_ALLLINKS);
        writeCData(
            "from ", 
            assocOneToOneClass,
            " where type = :", QUERY_PARAM_ALLLINKS_TYPE);
        endElem("query");
        
        endElem("class");
    }
    
    private void writeOneToManyMapping() throws GenerationException
    {
        Map<String, String> oneToManyParentTypeMap = 
            buildTypeMap(oneToManyParentTypeSet);
        Map<String, String> oneToManyChildTypeMap = 
            buildTypeMap(oneToManyChildTypeSet);

        int parentLength = 64;
        for(String key: oneToManyParentTypeMap.keySet()) {
            parentLength = Math.max(parentLength, key.length());
        }
        int childLength = 64;
        for(String key: oneToManyChildTypeMap.keySet()) {
            childLength = Math.max(childLength, key.length());
        }
        
        startElem(
            "class",
            "name", assocOneToManyClass,
            "table", tableName(ASSOC_ONE_TO_MANY_TABLE));
        
        writeEmptyElem("cache", "usage", "read-write");
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", String.valueOf(parentLength));
        
        writeEmptyElem(
            "property",
            "name", ASSOC_REVERSE_POLARITY_PROPERTY,
            "not-null", "true");

        startElem(
            "any",
            "name", ASSOC_ONE_TO_MANY_PARENT_PROPERTY,
            "id-type", "long",
            "meta-type", "string");
        for(Map.Entry<String, String> entry: oneToManyParentTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        
        writeEmptyElem(
            "column", "name", ASSOC_ONE_TO_MANY_PARENT_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN);
        endElem("any");
        
        startElem(
            "list",
            "name", ASSOC_ONE_TO_MANY_CHILDREN_PROPERTY,
            "table", tableName(ASSOC_ONE_TO_MANY_CHILDREN_TABLE),
            "cascade", "save-update",
            "fetch", "subselect");
        writeEmptyElem("cache", "usage", "read-write");        
        writeEmptyElem("key", "column", ASSOC_ONE_TO_MANY_CHILD_KEY_COLUMN);
        writeEmptyElem(
            "list-index", "column", ASSOC_ONE_TO_MANY_CHILD_ORDINAL_COLUMN);
        startElem(
            "many-to-any",
            "id-type", "long", 
            "meta-type", "string");
        for(Map.Entry<String, String> entry: oneToManyChildTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_MANY_CHILD_TYPE_COLUMN,
            "length", String.valueOf(childLength));
        writeEmptyElem("column", "name", ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN);
        endElem("many-to-any");
        endElem("list");

        // Named queries
        newLine();
        startElem(
            "query", 
            "name", QUERY_NAME_ALLLINKS);
        writeCData(
            "from ", 
            assocOneToManyClass,
            " where type = :", QUERY_PARAM_ALLLINKS_TYPE);
        endElem("query");
        
        endElem("class");
    }
    
    private void writeManyToManyMapping() throws GenerationException
    {
        Map<String, String> manyToManySourceTypeMap = 
            buildTypeMap(manyToManySourceTypeSet);
        Map<String, String> manyToManyTargetTypeMap = 
            buildTypeMap(manyToManyTargetTypeSet);

        int sourceLength = 64;
        for(String key: manyToManySourceTypeMap.keySet()) {
            sourceLength = Math.max(sourceLength, key.length());
        }
        int targetLength = 64;
        for(String key: manyToManyTargetTypeMap.keySet()) {
            targetLength = Math.max(targetLength, key.length());
        }
        int length = Math.max(sourceLength, targetLength);

        startElem(
            "class",
            "name", assocManyToManyClass,
            "table", tableName(ASSOC_MANY_TO_MANY_TABLE));
        
        writeEmptyElem("cache", "usage", "read-write");
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", String.valueOf(length));

        writeEmptyElem(
            "property",
            "name", ASSOC_REVERSE_POLARITY_PROPERTY,
            "not-null", "true");
        
        startElem(
            "any",
            "name", ASSOC_MANY_TO_MANY_SOURCE_PROPERTY,
            "id-type", "long",
            "meta-type", "string");
        for(Map.Entry<String, String> entry: manyToManySourceTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        for(Map.Entry<String, String> entry: manyToManyTargetTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        
        writeEmptyElem(
            "column", "name", ASSOC_MANY_TO_MANY_SOURCE_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN);
        endElem("any");
        
        startElem(
            "list",
            "name", ASSOC_MANY_TO_MANY_TARGET_PROPERTY,
            "table", tableName(ASSOC_MANY_TO_MANY_TARGET_TABLE),
            "cascade", "save-update",
            "fetch", "join");
        writeEmptyElem("cache", "usage", "read-write");
        writeEmptyElem("key", "column", ASSOC_MANY_TO_MANY_TARGET_KEY_COLUMN);
        writeEmptyElem(
            "list-index", "column", ASSOC_MANY_TO_MANY_TARGET_ORDINAL_COLUMN);
        startElem(
            "many-to-any",
            "id-type", "long", 
            "meta-type", "string");
        for(Map.Entry<String, String> entry: manyToManySourceTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        for(Map.Entry<String, String> entry: manyToManyTargetTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        writeEmptyElem(
            "column",
            "name", ASSOC_MANY_TO_MANY_TARGET_TYPE_COLUMN,
            "length", String.valueOf(length));
        writeEmptyElem("column", "name", ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN);
        endElem("many-to-any");
        endElem("list");
        
        // Named queries
        newLine();
        startElem(
            "query", 
            "name", QUERY_NAME_ALLLINKS);
        writeCData(
            "from ", assocManyToManyClass,
            " where type = :", QUERY_PARAM_ALLLINKS_TYPE, " and reversed = 0");
        endElem("query");
        
        endElem("class");
    }
    
    /**
     * Generate named queries for all classes, abstract or otherwise.  We
     * do not generate these within each class mapping since abstract classes 
     * don't get a mapping and it's more straightforward to access all these
     * queries in the same way. 
     */
    private void generateAllOfTypeQueries() throws GenerationException
    {
        for(Classifier cls: allTypes) {
            String interfaceName = generator.getTypeName(cls);

            String queryName = interfaceName + "." + QUERY_NAME_ALLOFTYPE;
            
            newLine();
            startElem(
                "query", 
                "name", queryName,
                "cacheable", "true");
            // Polymorphic query against interface type will return instances
            // of this class and any other implementation of the interface 
            // (e.g., all subclasses).
            writeCData("from ", interfaceName);
            endElem("query");
        }
    }
    
    /**
     * Build a map of simple type name to fully qualified type name.  Uses
     * the {@link #subTypeMap} to include all sub types of the Classifiers
     * found in typeSet.  Abstract sub types are not included in the map, 
     * but they are traversed to find additional concrete sub types.
     * 
     * @param typeSet Set of Classifiers representing types to place into the
     *                map (including all sub types)
     * @return map of simple type name to fully qualified type name for the
     *         given classifiers and their sub types.
     */
    private Map<String, String> buildTypeMap(
        Set<Classifier> typeSet)
    {
        Map <String, String> typeMap = new LinkedHashMap<String, String>();
        
        buildTypeMapRecursive(typeSet, typeMap);
        
        return typeMap;
    }

    private void buildTypeMapRecursive(
        Set<Classifier> typeSet,
        Map<String, String> typeMap)
    {
        for(Classifier cls: typeSet) {
            String simpleTypeName = generator.getSimpleTypeName(cls);
            String typeName = 
                generator.getTypeName(cls, HibernateJavaHandler.IMPL_SUFFIX);
            
            typeMap.put(simpleTypeName, typeName);
            
            Set<Classifier> subTypeSet = subTypeMap.get(cls);
            if (subTypeSet != null) {
                buildTypeMapRecursive(subTypeSet, typeMap);
            }
        }
    }

    public void generateClassInstance(MofClass cls)
        throws GenerationException
    {
        if (HibernateCodeGenUtils.isTransient(cls)) {
            log.fine(
                "Skipping Transient Class Instance Mapping for '" 
                + cls.getName() + "'");
            return;
        }
        
        if (getPassIndex() == 0) {
            if (!cls.isAbstract()) {
                Collection<Attribute> instanceAttributes =
                    contentsOfType(
                        cls,
                        HierachySearchKindEnum.ENTITY_ONLY, 
                        VisibilityKindEnum.PUBLIC_VIS,
                        ScopeKindEnum.INSTANCE_LEVEL,
                        Attribute.class);

                for(Attribute attrib: instanceAttributes) {
                    if (attrib.isDerived()) {
                        continue;
                    }
                    if (attrib.getType() instanceof DataType) {
                        continue;
                    }
                    
                    addComponentAttrib(
                        attrib.getType(), 
                        new ComponentInfo(
                            generator, cls, attrib, false));
                }
            }
            return;
        }
        
        String typeName = 
            generator.getTypeName(cls, HibernateJavaHandler.IMPL_SUFFIX);
        
        String tableName = generator.getSimpleTypeName(cls);
        
        allTypes.add(cls);
        
        // Build map of Classifiers to their sub types.
        for(Classifier superType: 
                GenericCollections.asTypedList(
                    cls.getSupertypes(), Classifier.class))
        {
            Set<Classifier> subTypes = subTypeMap.get(superType);
            if (subTypes == null) {
                subTypes = new LinkedHashSet<Classifier>();
                subTypeMap.put(superType, subTypes);
            }
            
            subTypes.add(cls);
        }
        
        if (cls.isAbstract()) {
            log.fine(
                "Skipping Class Instance Mapping '" + typeName + "'");
            return;
        }
        
        log.fine(
            "Generating Class Instance Mapping '" + typeName + "'");

        startElem(
            "class", 
            "name", typeName,
            "table", tableName(tableName),
            "batch-size", "25");

        writeEmptyElem("cache", "usage", "read-write");
        
        // MOF Id
        writeIdBlock();
        newLine();
        
        List<ComponentInfo> componentInfos = 
            new ArrayList<ComponentInfo>();
        if (componentAttribMap.containsKey(cls)) {
            componentInfos.addAll(componentAttribMap.get(cls));
        }
        
        // Attributes
        Collection<Attribute> instanceAttributes =
            contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS,
                ScopeKindEnum.INSTANCE_LEVEL,
                Attribute.class);

        for(Attribute attrib: instanceAttributes) {
            if (attrib.isDerived()) {
                continue;
            }
            
            String fieldName = attrib.getName();
            fieldName = generator.getClassFieldName(fieldName);
            
            String propertyName = fieldName + HibernateJavaHandler.IMPL_SUFFIX;
            
            final Classifier attribType = attrib.getType();
            MappingType mappingType = 
                getMappingType(attribType, attrib.getMultiplicity());
            switch (mappingType) {
            case BOOLEAN:
                // Boolean type; use a custom accessor since the method names
                // used by JMI don't match Hibernate's default accessor
                writeEmptyElem(
                    "property",
                    "name", propertyName,
                    "column", hibernateQuote(fieldName),
                    "access", BOOLEAN_PROPERTY_ACCESSOR_CLASS);
                break;

            case ENUMERATION: {
                // Enumeration type; use a custom type definition
                String typedefName = generator.getSimpleTypeName(
                    attrib.getType(),
                    TYPEDEF_SUFFIX);

                writeEmptyElem(
                    "property",
                    "name", propertyName,
                    "column", hibernateQuote(fieldName),
                    "type", typedefName);
                break;
            }
            
            case CLASS:
                componentInfos.add(
                    new ComponentInfo(generator, cls, attrib, true));
                break;

            case STRING:
                // String types; use Hibernate text type to force CLOB/TEXT
                // columns.
                
                // REVIEW: SWZ: 1/15/08: Consider using MOF tags to allow 
                // specification of max field size. For type string, the
                // Hibernate default is varchar(255).  For text/mysql it's
                // 65K.  For text/mysql with length > 65K, it's ~2^31.
                writeEmptyElem(
                    "property",
                    "name", propertyName,
                    "column", hibernateQuote(fieldName),
                    "type", "text");
                break;
                
            case LIST:
            case COLLECTION:
                MappingType baseMappingType = 
                    getMappingType(attribType, new FakeMultiplicityType());
                String type;
                if (baseMappingType == MappingType.STRING) {
                    // REVIEW: SWZ: 2/8/08: See STRING case above.
                    // This case is split out from the method below because
                    // we'll need to lookup the max string size.
                    type = "text"; 
                } else {
                    type = 
                        convertPrimitiveTypeToHibernateTypeName(
                            (PrimitiveType)attribType);
                }
                
                String collTableName =
                    tableName + "$" + StringUtil.toInitialUpper(fieldName);
                
                startElem(
                    mappingType == MappingType.LIST ? "list" : "set",
                    "name", propertyName,
                    "table", tableName(collTableName),
                    "cascade", "all",
                    "fetch", "join");
                writeEmptyElem("cache", "usage", "read-write");
                writeEmptyElem(
                    "key",
                    "column", hibernateQuote(MOF_ID_COLUMN_NAME));
                if (mappingType == MappingType.LIST) {
                    writeEmptyElem(
                        "list-index", 
                        "column", hibernateQuote("order"));
                }
                writeEmptyElem(
                    "element",
                    "column", hibernateQuote(fieldName),
                    "type", type);
                
                endElem("set");
                break;
                    

            case OTHER_DATA_TYPE:
                // Generic type
                writeEmptyElem(
                    "property",
                    "name", propertyName,
                    "column", hibernateQuote(fieldName));
                break;

            default:
                throw new GenerationException("Unknown mapping type '"
                    + mappingType + "'");
            }
        }

        // References
        Collection<Reference> instanceReferences =
            contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS,
                Reference.class);
        for(Reference ref: instanceReferences) {
            ReferenceInfo refInfo = new ReferenceInfoImpl(generator, ref);

            generateAssociationField(refInfo);
        }
        
        // Unreferenced associations
        Map<Association, ReferenceInfo> unrefAssocRefInfoMap =
            new HashMap<Association, ReferenceInfo>();
        
        Collection<Association> unreferencedAssociations = 
            HibernateCodeGenUtils.findUnreferencedAssociations(
                generator,
                assocInfoMap,
                cls,
                instanceReferences, 
                unrefAssocRefInfoMap);

        for(Association unref: unreferencedAssociations) {
            ReferenceInfo refInfo = unrefAssocRefInfoMap.get(unref);
            
            generateAssociationField(refInfo);
        }

        for(ComponentInfo componentInfo: componentInfos) {
            generateAssociationField(componentInfo);
        }
        
        newLine();
        startElem(
            "query", 
            "name", QUERY_NAME_ALLOFCLASS,
            "cacheable", "true");
        // Polymorphic query against implementation type will return only
        // instances of this exact class.
        writeCData("from ", typeName);
        endElem("query");        
        
        endElem("class");
        newLine();
    }

    private void generateAssociationField(ReferenceInfo refInfo)
        throws GenerationException
    {
        String fieldName = refInfo.getFieldName();

        // NOTE: Cannot specify not-null=true here because Hibernate
        // may try to insert the object without the association and
        // then circle back to create the association.  Instead, the
        // lower multiplicity bound is checked at run-time.  See
        // HibernateJavaHandler and HibernateObject.checkConstraints().
        
        writeEmptyElem(
            "many-to-one",
            "name", fieldName + HibernateJavaHandler.IMPL_SUFFIX,
            "column", hibernateQuote(fieldName),
            "not-null", "false",
            "cascade", "save-update");
    }

    private String hibernateQuote(String fieldName)
    {
        return "`" + fieldName + "`";
    }
    
    private String tableName(String tableName)
    {
        if (tablePrefix != null) {
            tableName = tablePrefix + tableName;
        }
        
        return hibernateQuote(tableName);
    }
    
    private MappingType getMappingType(
        Classifier type, MultiplicityType multiplicity)
    {
        if (type instanceof PrimitiveType) {
            if (multiplicity.getUpper() != 1) {
                if (multiplicity.isOrdered()) {
                    return MappingType.LIST;
                } else {
                    return MappingType.COLLECTION;
                }
            }
            if (type.getName().equalsIgnoreCase("boolean")) {
                return MappingType.BOOLEAN;
            } else if (type.getName().equalsIgnoreCase("string")) {
                return MappingType.STRING;
            } else {
                return MappingType.OTHER_DATA_TYPE;
            }
        } else if (type instanceof EnumerationType) {
            return MappingType.ENUMERATION;
        } else if (!(type instanceof DataType)) {
            return MappingType.CLASS;
        } else if (type instanceof AliasType) {
            return getMappingType(((AliasType)type).getType(), multiplicity);
        } else {
            return MappingType.OTHER_DATA_TYPE;
        }
    }
    
    private void writeIdBlock()
        throws GenerationException
    {
        startElem(
            "id",
            "name", MOF_ID_PROPERTY_NAME,
            "column", hibernateQuote(MOF_ID_COLUMN_NAME));
        startElem("generator", "class", "assigned");
        endElem("generator");
        endElem("id");
    }
    
    private void addComponentAttrib(
        Classifier type, ComponentInfo componentInfo)
    {
        List<ComponentInfo> componentOfList =
            componentAttribMap.get(type);
        
        if (componentOfList == null) {
            componentOfList = new ArrayList<ComponentInfo>();
            componentAttribMap.put(type, componentOfList);
        }
        
        componentOfList.add(componentInfo);
    }
    
    private String convertPrimitiveTypeToHibernateTypeName(PrimitiveType type)
    {
        String typeName = type.getName();
        final String javaTypeName = 
            Primitives.convertTypeNameToPrimitive("java.lang." + typeName);
        
        String hibernateTypeName;
        if (javaTypeName.equals("int")) {
            hibernateTypeName = "integer";
        } else if (javaTypeName.equals("char")) {
            hibernateTypeName = "character";
        } else {
            // All others match java primitives
            hibernateTypeName = javaTypeName;
        }
        
        return hibernateTypeName; 
    }

    public void generateAssociation(Association assoc) 
        throws GenerationException
    {
        if (getPassIndex() == 0) {
            AssociationInfo assocInfo = 
                new AssociationInfoImpl(generator, assoc);
            assocInfoMap.put(assoc, assocInfo);

            return;
        }
        
        if (HibernateCodeGenUtils.isTransient(assoc)) {
            log.fine(
                "Skipping Transient Association Mapping for '" 
                + assoc.getName() + "'");
            return;
        }
        
        String interfaceName = generator.getTypeName(assoc);
        
        log.fine("Analyzing Association Mapping '" + interfaceName + "'");

        AssociationEnd[] ends = generator.getAssociationEnds(assoc);
        
        int end0Upper = ends[0].getMultiplicity().getUpper();
        int end1Upper = ends[1].getMultiplicity().getUpper();
        
        if (end0Upper != 1 && end1Upper != 1) {
            // Many-to-many
            manyToManySourceTypeSet.add(ends[0].getType());
            manyToManyTargetTypeSet.add(ends[1].getType());
        } else if (end0Upper == 1 && end1Upper == 1) {
            // One-to-one
            oneToOneParentTypeSet.add(ends[0].getType());
            oneToOneChildTypeSet.add(ends[1].getType());
        } else if (end0Upper == 1) {
            // One-to-many, end 0 is parent
            oneToManyParentTypeSet.add(ends[0].getType());
            oneToManyChildTypeSet.add(ends[1].getType());
        } else {
            // One-to-many, end 1 is parent
            oneToManyChildTypeSet.add(ends[0].getType());
            oneToManyParentTypeSet.add(ends[1].getType());
        }
    }

    public void generateEnumerationClass(EnumerationType enumType)
        throws GenerationException
    {
        if (getPassIndex() != 0) {
            return;
        }

        String typeName = generator.getTypeName(enumType, ENUM_CLASS_SUFFIX);

        String typedefName = 
            generator.getSimpleTypeName(enumType, TYPEDEF_SUFFIX);
        
        startElem(
            "typedef", "name", typedefName, "class", ENUM_USER_TYPE_CLASS);
        writeSimpleElem(
            "param", typeName, "name", EnumUserType.ENUM_CLASS_PARAM);
        endElem("typedef");
        newLine();
    }

    public void generatePackage(MofPackage pkg)
        throws GenerationException
    {
        if (getPassIndex() != 0) {
            return;
        }
        
        if (pkg.getContainer() == null && 
            !pkg.getName().equals("PrimitiveTypes"))
        {
            // TODO: reinstate
//            assert(topLevelPackage == null);
            topLevelPackage =
                generator.getTypeName(
                    pkg, PACKAGE_SUFFIX + HibernateJavaHandler.IMPL_SUFFIX);
        }
    }
    
    /**
     * FakeMultiplicityType is a trivial implementation of 
     * {@link MultiplicityType} that represents a multiplicity with upper 
     * bound 1.  All JMI reflective APIs return null.
     */
    private final class FakeMultiplicityType
        implements MultiplicityType
    {
        private static final long serialVersionUID = -6511104088995504510L;

        public int getLower()
        {
            return 0;
        }

        public int getUpper()
        {
            return 1;
        }

        public boolean isOrdered()
        {
            return false;
        }

        public boolean isUnique()
        {
            return false;
        }

        public List<?> refFieldNames()
        {
            return null;
        }

        public Object refGetValue(String arg0)
        {
            return null;
        }

        public List<?> refTypeName()
        {
            return null;
        }
    }

    /**
     * MappingType distinguishes between several ways that an {@link Attribute}
     * may be mapped to a database column.
     */
    private enum MappingType
    {
        /** Boolean value.  Mapped using {@link BooleanPropertyAccessor}. */
        BOOLEAN,
        
        /** String value.  Maybe mapped as varchar or text. */
        STRING,
        
        /** Enumeration value.  Mapped using a Hibernate typedef. */
        ENUMERATION,
        
        /** 
         * Generic value.  Mapped by allowing Hibernate to inspect property's 
         * type.
         */
        OTHER_DATA_TYPE,
        
        /** 
         * Component attribute.  Mapped by modeling it as if it were an
         * {@link Association}.
         */
        CLASS,
        
        /** 
         * List of values. Mapped as a Hibernate list.  Note that the 
         * underlying type must still be mapped properly.
         */
        LIST,
        
        /** 
         * Collection of values.  Mapped as a Hibernate set.  Note that the 
         * underlying type must still be mapped properly.
         */ 
        COLLECTION;
    }
}

// End HibernateMappingHandler.java
