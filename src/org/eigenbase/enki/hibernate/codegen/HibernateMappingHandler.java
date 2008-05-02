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
    /** Suffix for the cache region used by a particular metamodel. */ 
    private static final String CACHE_REGION_SUFFIX = "ENKI";

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
        "parentType";
    private static final String ASSOC_ONE_TO_ONE_PARENT_ID_COLUMN =
        "parentId";
    private static final String ASSOC_ONE_TO_ONE_CHILD_PROPERTY = "child";
    private static final String ASSOC_ONE_TO_ONE_CHILD_TYPE_COLUMN = 
        "childType";
    private static final String ASSOC_ONE_TO_ONE_CHILD_ID_COLUMN =
        "childId";

    private static final String ASSOC_ONE_TO_MANY_TABLE = "AssocOneToMany";    
    private static final String ASSOC_ONE_TO_MANY_ORDERED_TABLE = 
        "AssocOneToManyOrdered";
    private static final String ASSOC_ONE_TO_MANY_PARENT_PROPERTY = "parent";
    private static final String ASSOC_ONE_TO_MANY_PARENT_TYPE_COLUMN = 
        "parentType";
    private static final String ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN =
        "parentId";
    private static final String ASSOC_ONE_TO_MANY_CHILDREN_PROPERTY = 
        "children";
    private static final String ASSOC_ONE_TO_MANY_CHILD_KEY_COLUMN = 
        "mofId";
    private static final String ASSOC_ONE_TO_MANY_CHILD_ORDINAL_COLUMN = 
        "ordinal";

    private static final String ASSOC_ONE_TO_MANY_CHILDREN_TABLE =
        "AssocOneToManyChildren";
    private static final String ASSOC_ONE_TO_MANY_CHILDREN_ORDERED_TABLE =
        "AssocOneToManyOrderedChildren";
    private static final String ASSOC_ONE_TO_MANY_CHILD_TYPE_COLUMN = 
        "childType";
    private static final String ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN = 
        "childId";
    
    private static final String ASSOC_MANY_TO_MANY_TABLE = "AssocManyToMany";
    private static final String ASSOC_MANY_TO_MANY_ORDERED_TABLE = 
        "AssocManyToManyOrdered";
    private static final String ASSOC_MANY_TO_MANY_SOURCE_PROPERTY = "source";
    private static final String ASSOC_MANY_TO_MANY_SOURCE_TYPE_COLUMN = 
        "sourceType";
    private static final String ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN =
        "sourceId";
    private static final String ASSOC_MANY_TO_MANY_TARGET_PROPERTY = "target";
    private static final String ASSOC_MANY_TO_MANY_TARGET_TABLE = 
        "AssocManyToManyTarget";
    private static final String ASSOC_MANY_TO_MANY_ORDERED_TARGET_TABLE = 
        "AssocManyToManyOrderedTarget";
    private static final String ASSOC_MANY_TO_MANY_TARGET_KEY_COLUMN = 
        "mofId";
    private static final String ASSOC_MANY_TO_MANY_TARGET_ORDINAL_COLUMN = 
        "ordinal";
    private static final String ASSOC_MANY_TO_MANY_TARGET_TYPE_COLUMN = 
        "targetType";
    private static final String ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN = 
        "targetId";

    private static final String ASSOC_ONE_TO_MANY_FETCH_TYPE = "subselect";
    private static final String ASSOC_MANY_TO_MANY_FETCH_TYPE = "join";
    
    // Association HQL query names and named parameters.
    public static final String QUERY_NAME_ALLLINKS = "allLinks";

    public static final String QUERY_PARAM_ALLLINKS_TYPE = "type";

    // Class HQL query names and named parameters
    public static final String QUERY_NAME_ALLOFCLASS = "allOfClass";
    public static final String QUERY_NAME_ALLOFTYPE = "allOfType";
    public static final String QUERY_NAME_BYMOFID = "byMofId";
    
    private static final JavaClassReference ENUM_USER_TYPE_CLASS =
        new JavaClassReference(EnumUserType.class, false);

    /**
     * Controls when the mapping switches from Hibernate's "string" type to
     * the "text" type.  The distinction is that the "text" type uses 
     * {@link java.sql.PreparedStatement#setCharacterStream(int, Reader, int)}
     * to write values.  This value, {@value}, is somewhat arbitrary, although
     * some databases (e.g., MySQL) will balk if it's set too high.
     */
    private static final int STRING_TEXT_CROSSOVER = 32768;

    private Set<Classifier> allTypes;
    
    /** Maps a component type to a list of references to it. */
    private Map<Classifier, List<ComponentInfo>> componentAttribMap;

    private final Logger log = 
        Logger.getLogger(HibernateMappingHandler.class.getName());

    private File metaInfEnkiDir;

    private String topLevelPackage;
    
    private String extentName;
    
    private String tablePrefix;
    private int defaultStringLength;
    
    private String initializerName;

    private Map<Association, AssociationInfo> assocInfoMap;

    private JavaClassReference assocOneToOneClass;
    private JavaClassReference assocOneToManyClass;
    private JavaClassReference assocOneToManyOrderedClass;
    private JavaClassReference assocManyToManyClass;
    private JavaClassReference assocManyToManyOrderedClass;
    
    private JavaClassReference assocTypeMapperClass;

    public HibernateMappingHandler()
    {
        this.allTypes = new LinkedHashSet<Classifier>();
        
        this.assocInfoMap = new LinkedHashMap<Association, AssociationInfo>();

        this.componentAttribMap = 
            new HashMap<Classifier, List<ComponentInfo>>();
        
        this.defaultStringLength = CodeGenUtils.DEFAULT_STRING_LENGTH;
    }

    public void setExtentName(String extentName)
    {
        this.extentName = extentName;
    }
    
    public void setTablePrefix(String tablePrefix)
    {
        this.tablePrefix = tablePrefix;
    }

    /**
    * Configures a default string column length, measured in characters.  
    * If left unspecified, the value of
    * {@link CodeGenUtils#DEFAULT_STRING_LENGTH} is used.  The 
    * default string length may be overridden via a tag on the given 
    * {@link Attribute} or its {@link Classifier} (see 
    * {@link CodeGenUtils#findMaxLengthTag(
    *            Classifier, Attribute, int, Logger)}.)
    * 
    * @param defaultStringLength the new default string length 
    */
    public void setDefaultStringLength(int defaultStringLength)
    {
        this.defaultStringLength = defaultStringLength;
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
        assocOneToManyOrderedClass = 
            new JavaClassReference(
                packageName,
                HibernateJavaHandler.ASSOCIATION_ONE_TO_MANY_ORDERED_BASE.toSimple());
        assocManyToManyClass = 
            new JavaClassReference(
                packageName,
                HibernateJavaHandler.ASSOCIATION_MANY_TO_MANY_BASE.toSimple());
        assocManyToManyOrderedClass = 
            new JavaClassReference(
                packageName,
                HibernateJavaHandler.ASSOCIATION_MANY_TO_MANY_ORDERED_BASE.toSimple());
        assocTypeMapperClass =
            new JavaClassReference(
                packageName,
                HibernateJavaHandler.ASSOCIATION_TYPE_MAPPER_BASE.toSimple());
        
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
            if (!pluginMode) {
                writeOneToOneMapping();
                newLine();
    
                writeOneToManyMapping();
                newLine();
    
                writeOneToManyOrderedMapping();
                newLine();
    
                writeManyToManyMapping();
                newLine();
    
                writeManyToManyOrderedMapping();
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
            if (!pluginMode) {
                writeln(
                    MDRepositoryFactory.PROPERTY_ENKI_TOP_LEVEL_PKG, "=", 
                    topLevelPackage);
            }
            writeln(
                MDRepositoryFactory.PROPERTY_ENKI_EXTENT, "=", extentName);
            writeln(
                HibernateMDRepository.PROPERTY_MODEL_INITIALIZER, "=", 
                initializerName);
            writeln(
                HibernateMDRepository.PROPERTY_MODEL_PLUGIN, "=",
                pluginMode);
            close();
        }
        
        super.endGeneration(throwing);
    }

    private void writeOneToOneMapping() throws GenerationException
    {
        startElem(
            "class",
            "name", assocOneToOneClass,
            "table", tableName(ASSOC_ONE_TO_ONE_TABLE),
            "lazy", "true");
        
        writeCacheElement();
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);
        
        startElem(
            "any",
            "name", ASSOC_ONE_TO_ONE_PARENT_PROPERTY,
            "id-type", "long",
            "meta-type", assocTypeMapperClass,
            "cascade", "save-update");
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
            "meta-type", assocTypeMapperClass,
            "cascade", "save-update");
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
        startElem(
            "class",
            "name", assocOneToManyClass,
            "table", tableName(ASSOC_ONE_TO_MANY_TABLE),
            "lazy", "true");
        
        writeCacheElement();
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);
        
        writeEmptyElem(
            "property",
            "name", ASSOC_REVERSE_POLARITY_PROPERTY,
            "not-null", "true");

        startElem(
            "any",
            "name", ASSOC_ONE_TO_MANY_PARENT_PROPERTY,
            "id-type", "long",
            "meta-type", assocTypeMapperClass);
        writeEmptyElem(
            "column", "name", ASSOC_ONE_TO_MANY_PARENT_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN);
        endElem("any");
        
        startElem(
            "set",
            "name", ASSOC_ONE_TO_MANY_CHILDREN_PROPERTY,
            "table", tableName(ASSOC_ONE_TO_MANY_CHILDREN_TABLE),
            "cascade", "save-update",
            "lazy", "true",
            "fetch", ASSOC_ONE_TO_MANY_FETCH_TYPE);
        writeCacheElement();
        writeEmptyElem(
            "key", 
            "column", ASSOC_ONE_TO_MANY_CHILD_KEY_COLUMN);
        startElem(
            "many-to-any",
            "id-type", "long", 
            "meta-type", assocTypeMapperClass);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_MANY_CHILD_TYPE_COLUMN,
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);
        writeEmptyElem(
            "column", 
            "name", ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN);
        endElem("many-to-any");
        endElem("set");
        
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
    
    private void writeOneToManyOrderedMapping() throws GenerationException
    {
        startElem(
            "class",
            "name", assocOneToManyOrderedClass,
            "table", tableName(ASSOC_ONE_TO_MANY_ORDERED_TABLE),
            "lazy", "true");
        
        writeCacheElement();
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);
        
        writeEmptyElem(
            "property",
            "name", ASSOC_REVERSE_POLARITY_PROPERTY,
            "not-null", "true");

        startElem(
            "any",
            "name", ASSOC_ONE_TO_MANY_PARENT_PROPERTY,
            "id-type", "long",
            "meta-type", assocTypeMapperClass);
        writeEmptyElem(
            "column", "name", ASSOC_ONE_TO_MANY_PARENT_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN);
        endElem("any");
        
        startElem(
            "list",
            "name", ASSOC_ONE_TO_MANY_CHILDREN_PROPERTY,
            "table", tableName(ASSOC_ONE_TO_MANY_CHILDREN_ORDERED_TABLE),
            "cascade", "save-update",
            "lazy", "true",
            "fetch", ASSOC_ONE_TO_MANY_FETCH_TYPE);
        writeCacheElement();        
        writeEmptyElem("key", "column", ASSOC_ONE_TO_MANY_CHILD_KEY_COLUMN);
        writeEmptyElem(
            "list-index", "column", ASSOC_ONE_TO_MANY_CHILD_ORDINAL_COLUMN);
        startElem(
            "many-to-any",
            "id-type", "long", 
            "meta-type", assocTypeMapperClass);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_MANY_CHILD_TYPE_COLUMN,
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);
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
            assocOneToManyOrderedClass,
            " where type = :", QUERY_PARAM_ALLLINKS_TYPE);
        endElem("query");
        
        endElem("class");
    }

    private void writeManyToManyMapping() throws GenerationException
    {
        startElem(
            "class",
            "name", assocManyToManyClass,
            "table", tableName(ASSOC_MANY_TO_MANY_TABLE),
            "lazy", "true");
        
        writeCacheElement();
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);

        writeEmptyElem(
            "property",
            "name", ASSOC_REVERSE_POLARITY_PROPERTY,
            "not-null", "true");
        
        startElem(
            "any",
            "name", ASSOC_MANY_TO_MANY_SOURCE_PROPERTY,
            "id-type", "long",
            "meta-type", assocTypeMapperClass);        
        writeEmptyElem(
            "column", "name", ASSOC_MANY_TO_MANY_SOURCE_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN);
        endElem("any");

        startElem(
            "set",
            "name", ASSOC_MANY_TO_MANY_TARGET_PROPERTY,
            "table", tableName(ASSOC_MANY_TO_MANY_TARGET_TABLE),
            "cascade", "save-update",
            "lazy", "true",
            "fetch", ASSOC_MANY_TO_MANY_FETCH_TYPE);
        writeCacheElement();
        writeEmptyElem(
            "key",
            "column", ASSOC_MANY_TO_MANY_TARGET_KEY_COLUMN);
        startElem(
            "many-to-any",
            "id-type", "long", 
            "meta-type", assocTypeMapperClass);
        writeEmptyElem(
            "column",
            "name", ASSOC_MANY_TO_MANY_TARGET_TYPE_COLUMN,
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);
        writeEmptyElem(
            "column", 
            "name", ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN);
        endElem("many-to-any");
        endElem("set");
        
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
    
    private void writeManyToManyOrderedMapping() throws GenerationException
    {
        startElem(
            "class",
            "name", assocManyToManyOrderedClass,
            "table", tableName(ASSOC_MANY_TO_MANY_ORDERED_TABLE),
            "lazy", "true");
        
        writeCacheElement();
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);

        writeEmptyElem(
            "property",
            "name", ASSOC_REVERSE_POLARITY_PROPERTY,
            "not-null", "true");
        
        startElem(
            "any",
            "name", ASSOC_MANY_TO_MANY_SOURCE_PROPERTY,
            "id-type", "long",
            "meta-type", assocTypeMapperClass);        
        writeEmptyElem(
            "column", "name", ASSOC_MANY_TO_MANY_SOURCE_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN);
        endElem("any");
        
        startElem(
            "list",
            "name", ASSOC_MANY_TO_MANY_TARGET_PROPERTY,
            "table", tableName(ASSOC_MANY_TO_MANY_ORDERED_TARGET_TABLE),
            "cascade", "save-update",
            "lazy", "true",
            "fetch", ASSOC_MANY_TO_MANY_FETCH_TYPE);
        writeCacheElement();
        writeEmptyElem("key", "column", ASSOC_MANY_TO_MANY_TARGET_KEY_COLUMN);
        writeEmptyElem(
            "list-index", "column", ASSOC_MANY_TO_MANY_TARGET_ORDINAL_COLUMN);
        startElem(
            "many-to-any",
            "id-type", "long", 
            "meta-type", assocTypeMapperClass);
        writeEmptyElem(
            "column",
            "name", ASSOC_MANY_TO_MANY_TARGET_TYPE_COLUMN,
            "length", CodeGenUtils.DEFAULT_STRING_LENGTH);
        writeEmptyElem("column", "name", ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN);
        endElem("many-to-any");
        endElem("list");
        
        // Named queries
        newLine();
        startElem(
            "query", 
            "name", QUERY_NAME_ALLLINKS);
        writeCData(
            "from ", assocManyToManyOrderedClass,
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
    
    public void generateClassInstance(MofClass cls)
        throws GenerationException
    {
        if (!isIncluded(cls)) {
            log.fine(
                "Skipping Excluded Class Instance Mapping for '" 
                + cls.getName() + "'");
            return;
        }
        
        if (CodeGenUtils.isTransient(cls)) {
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
            "lazy", "true");

        writeCacheElement();
        
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
            case ENUMERATION:
                {
                    // Enumeration type; use a custom type definition
                    String typedefName = generator.getSimpleTypeName(
                        attrib.getType(),
                        TYPEDEF_SUFFIX);
    
                    writeEmptyElem(
                        "property",
                        "name", propertyName,
                        "column", hibernateQuote(fieldName),
                        "type", typedefName);
                }
                break;
            
            case CLASS:
                componentInfos.add(
                    new ComponentInfo(generator, cls, attrib, true));
                break;

            case STRING:
                writeStringTypeElement(
                    cls,
                    attrib,
                    "property",
                    "name", propertyName,
                    "column", hibernateQuote(fieldName));
                break;
            
            case LIST:
            case COLLECTION:
                {
                    MappingType baseMappingType = 
                        getMappingType(attribType, new FakeMultiplicityType());
                    
                    String collTableName =
                        tableName + "$" + StringUtil.toInitialUpper(fieldName);
                    
                    String collectionElem = 
                        mappingType == MappingType.LIST ? "list" : "set";
                    startElem(
                        collectionElem,
                        "name", propertyName,
                        "table", tableName(collTableName),
                        "cascade", "all",
                        "lazy", "true",
                        "fetch", "join");
                    writeCacheElement();
                    writeEmptyElem(
                        "key",
                        "column", hibernateQuote(MOF_ID_COLUMN_NAME));
                    if (mappingType == MappingType.LIST) {
                        writeEmptyElem(
                            "list-index", 
                            "column", hibernateQuote("order"));
                    }
                    
                    if (baseMappingType == MappingType.STRING) {
                        writeStringTypeElement(
                            cls,
                            attrib, 
                            "element", 
                            "column", hibernateQuote(fieldName));
                    } else {
                        String type = 
                            convertPrimitiveTypeToHibernateTypeName(
                                (PrimitiveType)attribType);

                        writeEmptyElem(
                            "element",
                            "column", hibernateQuote(fieldName),
                            "type", type);
                    }
                    endElem(collectionElem);
                }
                break;
                    

            case BOOLEAN:
            case OTHER_DATA_TYPE:
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
            CodeGenUtils.findUnreferencedAssociations(
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
        
        newLine();
        startElem(
            "query",
            "name", QUERY_NAME_BYMOFID,
            "cacheable", "true");
        // Query against mofId to type mapping.
        writeCData("from ", typeName, " where mofId = :mofId");
        endElem("query");
        
        endElem("class");
        newLine();
    }
    
    private void writeStringTypeElement(
        Classifier cls,
        Attribute attribute, 
        String elementName, 
        Object... xmlAttribs)
    throws GenerationException
    {
        StringBuilder type = new StringBuilder();
        int length = getStringType(cls, attribute, type);

        Object[] modifiedXmlAttribs = new Object[xmlAttribs.length + 4]; 
        for(int i = 0; i < xmlAttribs.length; i++) {
            modifiedXmlAttribs[i] = xmlAttribs[i];
        }
        
        int i = xmlAttribs.length;
        modifiedXmlAttribs[i++] = "type";
        modifiedXmlAttribs[i++] = type.toString();
        
        modifiedXmlAttribs[i++] = "length";
        modifiedXmlAttribs[i++] = String.valueOf(length);

        writeEmptyElem(elementName, modifiedXmlAttribs);
    }

    private int getStringType(
        Classifier cls, Attribute attrib, StringBuilder typeBuffer)
    throws GenerationException
    {
        typeBuffer.setLength(0);
        
        int maxLen = 
            CodeGenUtils.findMaxLengthTag(
                cls, attrib, defaultStringLength, log);
        
        if (maxLen <= STRING_TEXT_CROSSOVER) {
            typeBuffer.append("string");
        } else {
            // very large -- make sure we use stream semantics or mysql will
            // choke
            typeBuffer.append("text");
        }
        return maxLen;
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
        writeEmptyElem("generator", "class", "assigned");
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
        }
    }

    public void generateEnumerationClass(EnumerationType enumType)
        throws GenerationException
    {
        if (!isIncluded(enumType)) {
            log.fine(
                "Skipping Excluded Enumeration Mapping for '" 
                + enumType.getName() + "'");
            return;
        }
        
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
    
    private void writeCacheElement() throws GenerationException
    {
        String region;
        if (tablePrefix != null) {
            region = tablePrefix + CACHE_REGION_SUFFIX;
        } else{
            region = CACHE_REGION_SUFFIX;
        }
        
        writeEmptyElem(
            "cache", "usage", "read-write", "region", region);
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
        /** Boolean value. */
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
