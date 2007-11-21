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

/**
 * HibernateMappingHandler generates a Hibernate mapping file.
 * 
 * @author Stephan Zuercher
 */
public class HibernateMappingHandler
    extends XmlHandlerBase
    implements ClassInstanceHandler, AssociationHandler
{
    private static final String ASSOC_TYPE_PROPERTY = "type";

    private static final String ASSOC_ONE_TO_ONE_TABLE = "AssocOneToOne";
    private static final String ASSOC_ONE_TO_ONE_LEFT_PROPERTY = "left";
    private static final String ASSOC_ONE_TO_ONE_LEFT_TYPE_COLUMN = 
        "left_type";
    private static final String ASSOC_ONE_TO_ONE_LEFT_ID_COLUMN =
        "left_id";
    private static final String ASSOC_ONE_TO_ONE_RIGHT_PROPERTY = "right";
    private static final String ASSOC_ONE_TO_ONE_RIGHT_TYPE_COLUMN = 
        "right_type";
    private static final String ASSOC_ONE_TO_ONE_RIGHT_ID_COLUMN =
        "right_id";

    private static final String ASSOC_ONE_TO_MANY_TABLE = "AssocOneToMany";
    private static final String ASSOC_ONE_TO_MANY_PARENT_PROPERTY = "parent";
    private static final String ASSOC_ONE_TO_MANY_UNIQUE_CONSTRAINT =
        "parent_id_type_unique";
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
    private static final String ASSOC_MANY_TO_MANY_UNIQUE_CONSTRAINT =
        "source_id_type_unique";
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

    // TODO: get the name from the class directly and change to non-prototype
    public static final String MOF_ID_GENERATOR_CLASS = 
        "org.eigenbase.enki.prototype.MofIdGenerator";


    private String mappingFilePrefix;
    
    private Set<Classifier> oneToOneLeftTypeSet;
    private Set<Classifier> oneToOneRightTypeSet;

    private Set<Classifier> oneToManyParentTypeSet;
    private Set<Classifier> oneToManyChildTypeSet;
    
    private Set<Classifier> manyToManyLeftTypeSet;
    private Set<Classifier> manyToManyRightTypeSet;

    private Map<Classifier, Set<Classifier>> subTypeMap;
    
    private final Logger log = 
        Logger.getLogger(HibernateMappingHandler.class.getName());

    public HibernateMappingHandler()
    {
        this.oneToOneLeftTypeSet = new LinkedHashSet<Classifier>();
        this.oneToOneRightTypeSet = new LinkedHashSet<Classifier>();
        
        this.oneToManyParentTypeSet = new LinkedHashSet<Classifier>();
        this.oneToManyChildTypeSet = new LinkedHashSet<Classifier>();
        
        this.manyToManyLeftTypeSet = new LinkedHashSet<Classifier>();
        this.manyToManyRightTypeSet = new LinkedHashSet<Classifier>();
        
        this.subTypeMap = new HashMap<Classifier, Set<Classifier>>();
    }

    /**
     * Sets the prefix used to name a unique mapping file.
     * 
     * @param mappingFilePrefix a legal file name
     */
    public void setMappingFilePrefix(String mappingFilePrefix)
    {
        this.mappingFilePrefix = mappingFilePrefix;
    }
    
    // Implement Handler
    public void beginGeneration() throws GenerationException
    {        
        super.beginGeneration();
        
        if (mappingFilePrefix == null || mappingFilePrefix.length() == 0) {
            throw new GenerationException("Missing mapping file prefix");
        }
        
        File file = new File(outputDir, mappingFilePrefix + "-mapping.xml");
        
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
            if (!oneToOneLeftTypeSet.isEmpty()) {
                writeOneToOneMapping();
                newLine();
            }
            
            if (!oneToManyParentTypeSet.isEmpty()) {
                writeOneToManyMapping();
                newLine();
            }
            
            if (!manyToManyLeftTypeSet.isEmpty()) {
                writeManyToManyMapping();
            }
            
            endElem("hibernate-mapping");
        }
        
        close();
        
        super.endGeneration(throwing);
    }

    private void writeOneToOneMapping() throws GenerationException
    {
        Map<String, String> oneToOneLeftTypeMap =
            buildTypeMap(oneToOneLeftTypeSet);
        Map<String, String> oneToOneRightTypeMap = 
            buildTypeMap(oneToOneRightTypeSet);
        
        int leftLength = 64;
        for(String key: oneToOneLeftTypeMap.keySet()) {
            leftLength = Math.max(leftLength, key.length());
        }
        int rightLength = 64;
        for(String key: oneToOneRightTypeMap.keySet()) {
            rightLength = Math.max(rightLength, key.length());
        }

        startElem(
            "class",
            "name", ASSOCIATION_ONE_TO_ONE_IMPL_CLASS,
            "table", ASSOC_ONE_TO_ONE_TABLE);
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "length", String.valueOf(leftLength));
        
        startElem(
            "any",
            "name", ASSOC_ONE_TO_ONE_LEFT_PROPERTY,
            "id-type", "long",
            "meta-type", "string",
            "cascade", "save-update");
        for(Map.Entry<String, String> entry: oneToOneLeftTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        
        writeEmptyElem(
            "column", "name", ASSOC_ONE_TO_ONE_LEFT_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_ONE_LEFT_ID_COLUMN);
        endElem("any");

        startElem(
            "any",
            "name", ASSOC_ONE_TO_ONE_RIGHT_PROPERTY,
            "id-type", "long",
            "meta-type", "string",
            "cascade", "save-update");
        for(Map.Entry<String, String> entry: oneToOneRightTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        
        writeEmptyElem(
            "column", "name", ASSOC_ONE_TO_ONE_RIGHT_TYPE_COLUMN);
        writeEmptyElem(
            "column",
            "name", ASSOC_ONE_TO_ONE_RIGHT_ID_COLUMN);
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
        
        startElem(
            "class",
            "name", ASSOCIATION_ONE_TO_MANY_IMPL_CLASS,
            "table", ASSOC_ONE_TO_MANY_TABLE);
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "unique-key", ASSOC_ONE_TO_MANY_UNIQUE_CONSTRAINT,
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
            "name", ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN,
            "unique-key", ASSOC_ONE_TO_MANY_UNIQUE_CONSTRAINT);
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
        Map<String, String> manyToManyLeftTypeMap = 
            buildTypeMap(manyToManyLeftTypeSet);
        Map<String, String> manyToManyRightTypeMap = 
            buildTypeMap(manyToManyRightTypeSet);

        int leftLength = 64;
        for(String key: manyToManyLeftTypeMap.keySet()) {
            leftLength = Math.max(leftLength, key.length());
        }
        int rightLength = 64;
        for(String key: manyToManyRightTypeMap.keySet()) {
            rightLength = Math.max(rightLength, key.length());
        }
        int length = Math.max(leftLength, rightLength);
        
        startElem(
            "class",
            "name", ASSOCIATION_MANY_TO_MANY_IMPL_CLASS,
            "table", ASSOC_MANY_TO_MANY_TABLE);
        
        writeIdBlock();
        
        writeEmptyElem(
            "property",
            "name", ASSOC_TYPE_PROPERTY,
            "not-null", "true",
            "unique-key", ASSOC_MANY_TO_MANY_UNIQUE_CONSTRAINT,
            "length", String.valueOf(length));

        
        startElem(
            "any",
            "name", ASSOC_MANY_TO_MANY_SOURCE_PROPERTY,
            "id-type", "long",
            "meta-type", "string");
        for(Map.Entry<String, String> entry: manyToManyLeftTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        for(Map.Entry<String, String> entry: manyToManyRightTypeMap.entrySet())
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
            "name", ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN,
            "unique-key", ASSOC_MANY_TO_MANY_UNIQUE_CONSTRAINT);
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
        for(Map.Entry<String, String> entry: manyToManyLeftTypeMap.entrySet())
        {
            writeEmptyElem(
                "meta-value",
                "value", entry.getKey(),
                "class", entry.getValue());
        }
        for(Map.Entry<String, String> entry: manyToManyRightTypeMap.entrySet())
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
        for(Object o: cls.getSupertypes()) {
            Classifier superType = (Classifier)o;
            
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
            AssociationEnd[] ends = getAssociationEnds(assoc);
            
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

            AssociationEnd thisEnd;
            if (ends[0] == ref.getExposedEnd()) {
                thisEnd = ends[0];
            } else {
                thisEnd = ends[1];
            }
                
            // If this is a 1..* mapping, don't allow nulls
            String notNull = "false";
            if (thisEnd.getMultiplicity().getLower() != 0) {
                notNull = "true";
            }
                
            writeEmptyElem(
                "many-to-one",
                "name", fieldName + HibernateJavaHandler.IMPL_SUFFIX,
                "column", fieldName,
                "class", classRef,
                "not-null", notNull,
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
        startElem("generator", "class", MOF_ID_GENERATOR_CLASS);
        // TODO: specify non-default names for the underlying table/column 
        // TODO: parameterize the interval
        writeSimpleElem("param", "100", "name", "interval");
        endElem("generator");
        endElem("id");
    }
    
    public void generateAssociation(Association assoc)
    {
        String interfaceName = generator.getTypeName(assoc);
        
        log.fine("Analyzing Association Mapping '" + interfaceName + "'");

        AssociationEnd[] ends = getAssociationEnds(assoc);
        assert(ends.length == 2);
        
        int end0Upper = ends[0].getMultiplicity().getUpper();
        int end1Upper = ends[1].getMultiplicity().getUpper();
        
        if (end0Upper != 1 && end1Upper != 1) {
            // Many-to-many
            manyToManyLeftTypeSet.add(ends[0].getType());
            manyToManyRightTypeSet.add(ends[1].getType());
        } else if (end0Upper == 1 && end1Upper == 1) {
            // One-to-one
            oneToOneLeftTypeSet.add(ends[0].getType());
            oneToOneRightTypeSet.add(ends[1].getType());
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

    private AssociationEnd[] getAssociationEnds(Association assoc)
    {
        List<?> contents = assoc.getContents();
        assert(contents.size() == 2);
        
        Iterator<?> contentIter = contents.iterator();
        AssociationEnd[] ends = new AssociationEnd[] {
            (AssociationEnd)contentIter.next(),
            (AssociationEnd)contentIter.next()
        };
        return ends;
    }
}

// End HibernateMappingHandler.java
