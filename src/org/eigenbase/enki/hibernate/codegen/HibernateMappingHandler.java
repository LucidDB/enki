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
    private static final String TYPEDEF_SUFFIX = "Type";

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
    
    private final Logger log = 
        Logger.getLogger(HibernateMappingHandler.class.getName());

    private File metaInfEnkiDir;

    private String topLevelPackage;
    
    private String extentName;
    
    private String initializerName;

    private Map<Association, AssociationInfo> assocInfoMap;

    public HibernateMappingHandler()
    {
        this.oneToOneParentTypeSet = new LinkedHashSet<Classifier>();
        this.oneToOneChildTypeSet = new LinkedHashSet<Classifier>();
        
        this.oneToManyParentTypeSet = new LinkedHashSet<Classifier>();
        this.oneToManyChildTypeSet = new LinkedHashSet<Classifier>();
        
        this.manyToManySourceTypeSet = new LinkedHashSet<Classifier>();
        this.manyToManyTargetTypeSet = new LinkedHashSet<Classifier>();
        
        this.subTypeMap = new HashMap<Classifier, Set<Classifier>>();

        this.assocInfoMap = new LinkedHashMap<Association, AssociationInfo>();
    }

    public void setExtentName(String extentName)
    {
        this.extentName = extentName;
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
        if (getPassIndex() == 0) {
            return;
        }
        
        if (HibernateCodeGenUtils.isTransient(cls)) {
            log.fine(
                "Skipping Transient Class Instance Mapping for '" 
                + cls.getName() + "'");
            return;
        }
        
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
            
            final Classifier attribType = attrib.getType();
            MappingType mappingType = getMappingType(attribType);
            switch (mappingType) {
            case BOOLEAN:
                // Boolean type; use a custom accessor since the method names
                // used by JMI don't match Hibernate's default accessor
                writeEmptyElem(
                    "property",
                    "name", fieldName,
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
                    "name", fieldName,
                    "column", hibernateQuote(fieldName),
                    "type", typedefName);
                break;
            }

            case CLASS:
                // MofClass as an attribute; use a Hibernate association
                writeEmptyElem(
                    "many-to-one",
                    "name", fieldName + HibernateJavaHandler.IMPL_SUFFIX,
                    "column", hibernateQuote(fieldName),
                    "unique", "true",
                    "cascade", "save-update");
                break;

            case STRING:
                // String types; use Hibernate text type to force CLOB/TEXT
                // columns.
                
                // TODO: Consider using MOF tags to allow specification of
                // max field size (Hibernate default is 255)
                writeEmptyElem(
                    "property",
                    "name", fieldName,
                    "column", hibernateQuote(fieldName),
                    "type", "text");
                break;

            case OTHER_DATA_TYPE:
                // Generic type
                writeEmptyElem(
                    "property",
                    "name", fieldName,
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
            ReferenceInfo refInfo = new ReferenceInfo(generator, ref);

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
        newLine();

        
        endElem("class");
        newLine();
    }

    private void generateAssociationField(ReferenceInfo refInfo)
        throws GenerationException
    {
        String fieldName = refInfo.getFieldName();

        JavaClassReference classRef = ASSOCIATION_ONE_TO_MANY_IMPL_CLASS;

        if (refInfo.isSingle(0) && refInfo.isSingle(1)) {
            classRef = ASSOCIATION_ONE_TO_ONE_IMPL_CLASS;
        } else if (!refInfo.isSingle(0) && !refInfo.isSingle(1)) {
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
            "column", hibernateQuote(fieldName),
            "class", classRef,
            "not-null", "false",
            "cascade", "save-update");
    }

    private String hibernateQuote(String fieldName)
    {
        return "`" + fieldName + "`";
    }

    private MappingType getMappingType(Classifier type)
    {
        if (type instanceof PrimitiveType) {
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
            return getMappingType(((AliasType)type).getType());
        } else {
            return MappingType.OTHER_DATA_TYPE;
        }
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
        throws GenerationException
    {
        if (getPassIndex() == 0) {
            AssociationInfo assocInfo = new AssociationInfo(generator, assoc);
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
    
    private enum MappingType
    {
        BOOLEAN,          // Primitive boolean
        STRING,           // Primitive string
        ENUMERATION,      // any EnumerationType
        OTHER_DATA_TYPE,  // any other DataType
        CLASS;            // MofClass from the model
    }
}

// End HibernateMappingHandler.java
