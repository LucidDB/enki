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
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;

/**
 * HibernateMappingHandler generates Hibernate mapping file and an Enki
 * Hibernate provider configuration file.
 * 
 * @author Stephan Zuercher
 */
public class HibernateMappingHandler
    extends XmlHandlerBase
    implements ClassInstanceHandler, AssociationHandler, PackageHandler, 
               MofInitHandler.SubordinateHandler
{
    private static final String ASSOC_TYPE_PROPERTY = "type";

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
     * Name of the class for one-to-one associations.
     */
    public static final JavaClassReference ASSOCIATION_ONE_TO_ONE_IMPL_CLASS = 
        new JavaClassReference(
            HibernateJavaHandler.ASSOCIATION_ONE_TO_ONE_IMPL_CLASS, false);
    
    /**
     * Name of the class for one-to-many associations.
     */
    public static final JavaClassReference ASSOCIATION_ONE_TO_MANY_IMPL_CLASS = 
        new JavaClassReference(
            HibernateJavaHandler.ASSOCIATION_ONE_TO_MANY_IMPL_CLASS, false);
    
    /**
     * Name of the class for many-to-many associations.
     */
    public static final JavaClassReference ASSOCIATION_MANY_TO_MANY_IMPL_CLASS = 
        new JavaClassReference(
            HibernateJavaHandler.ASSOCIATION_MANY_TO_MANY_IMPL_CLASS, false);

    private Set<Classifier> oneToOneParentTypeSet;
    private Set<Classifier> oneToOneChildTypeSet;

    private Set<Classifier> oneToManyParentTypeSet;
    private Set<Classifier> oneToManyChildTypeSet;
    
    private Set<Classifier> manyToManySourceTypeSet;
    private Set<Classifier> manyToManyTargetTypeSet;

    private Map<Classifier, Set<Classifier>> subTypeMap;
    
    private final Logger log = 
        Logger.getLogger(HibernateMappingHandler.class.getName());

    private File metaInfEnkiDir;

    private String topLevelPackage;
    
    private String extentName;
    
    private String initializerName;
    
    public HibernateMappingHandler()
    {
        this.oneToOneParentTypeSet = new LinkedHashSet<Classifier>();
        this.oneToOneChildTypeSet = new LinkedHashSet<Classifier>();
        
        this.oneToManyParentTypeSet = new LinkedHashSet<Classifier>();
        this.oneToManyChildTypeSet = new LinkedHashSet<Classifier>();
        
        this.manyToManySourceTypeSet = new LinkedHashSet<Classifier>();
        this.manyToManyTargetTypeSet = new LinkedHashSet<Classifier>();
        
        this.subTypeMap = new HashMap<Classifier, Set<Classifier>>();
    }

    public void setExtentName(String extentName)
    {
        this.extentName = extentName;
    }

    public void setInitializerClassName(String initializerName)
    {
        this.initializerName = initializerName;
    }
    
    // Implement Handler
    public void beginGeneration() throws GenerationException
    {        
        super.beginGeneration();

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

        // TODO: distinct tables/names for multi-extent repositories
        startElem(
            "class",
            "name", ASSOCIATION_ONE_TO_ONE_IMPL_CLASS,
            "table", ASSOC_ONE_TO_ONE_TABLE);
        
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
        
        // TODO: distinct tables/names for multi-extent repositories
        startElem(
            "class",
            "name", ASSOCIATION_ONE_TO_MANY_IMPL_CLASS,
            "table", ASSOC_ONE_TO_MANY_TABLE);
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", String.valueOf(parentLength));
        
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
            "table", ASSOC_ONE_TO_MANY_CHILDREN_TABLE,
            "cascade", "save-update",
            "fetch", "join");
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

        // TODO: distinct tables/names for multi-extent repositories
        startElem(
            "class",
            "name", ASSOCIATION_MANY_TO_MANY_IMPL_CLASS,
            "table", ASSOC_MANY_TO_MANY_TABLE);
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", String.valueOf(length));

        
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
            "table", ASSOC_MANY_TO_MANY_TARGET_TABLE,
            "cascade", "save-update",
            "fetch", "join");
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
        endElem("class");
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
        String typeName = 
            generator.getTypeName(cls, HibernateJavaHandler.IMPL_SUFFIX);
        
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

        startElem("class", "name", typeName);

        // REVIEW: SWZ: 11/9/2007: Any good way to make this some kind of 
        // default for the generator.  Value must be the same across all 
        // classes. Could use an XML entity.
        
        // MOF Id
        writeIdBlock();
        newLine();
        
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
            writeEmptyElem("property", "name", fieldName);
        }

        // References
        Collection<Reference> instanceReferences =
            contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS,
                Reference.class);
        for(Reference ref: instanceReferences) {
            Association assoc = 
                (Association)ref.getExposedEnd().getContainer();
            AssociationEnd[] ends = generator.getAssociationEnds(assoc);
            
          int end0Upper = ends[0].getMultiplicity().getUpper();
          int end1Upper = ends[1].getMultiplicity().getUpper();

            String fieldName = generator.getSimpleTypeName(assoc);
            fieldName = toInitialLower(fieldName);

            JavaClassReference classRef = ASSOCIATION_ONE_TO_MANY_IMPL_CLASS;

            if (end0Upper == 1 && end1Upper == 1) {
                classRef = ASSOCIATION_ONE_TO_ONE_IMPL_CLASS;
            } else if (end0Upper != 1 && end1Upper != 1) {
                classRef = ASSOCIATION_MANY_TO_MANY_IMPL_CLASS;
            }

            // NOTE: Cannot specify not-null=true here because Hibernate
            // may try to insert the object without the association and
            // then circle back to create the association.  Instead, the
            // lower multiplicity bound is checked at run-time.  See
            // HibernateJavaHandler and HibernateObject.checkConstraints().
            
            writeEmptyElem(
                "many-to-one",
                "name", fieldName + HibernateJavaHandler.IMPL_SUFFIX,
                "column", fieldName,
                "class", classRef,
                "not-null", "false",
                "cascade", "save-update");
        }
        
        endElem("class");
        newLine();
    }

    private void writeIdBlock()
        throws GenerationException
    {
        startElem(
            "id",
            "name", "mofId",
            "column", "mofId");
        startElem("generator", "class", "assigned");
        endElem("generator");
        endElem("id");
    }
    
    public void generateAssociation(Association assoc)
    {
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

    public void generatePackage(MofPackage pkg)
        throws GenerationException
    {
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
}

// End HibernateMappingHandler.java
