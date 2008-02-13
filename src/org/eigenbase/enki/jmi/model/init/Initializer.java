package org.eigenbase.enki.jmi.model.init;

import java.util.*;
import javax.jmi.model.*;
import org.eigenbase.enki.jmi.impl.*;

public final class Initializer
    extends MetamodelInitializer
{
    public Initializer(String extent)
    {
        super(extent);
    }

    private MofClass[] class_;

    private void classInit()
    {
        class_ = new MofClass[28];

        class_[0] = getModelPackage().getMofClass().createMofClass(
            "Tag",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[1] = getModelPackage().getMofClass().createMofClass(
            "Constant",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[2] = getModelPackage().getMofClass().createMofClass(
            "Constraint",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[3] = getModelPackage().getMofClass().createMofClass(
            "Parameter",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[4] = getModelPackage().getMofClass().createMofClass(
            "Import",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[5] = getModelPackage().getMofClass().createMofClass(
            "Package",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[6] = getModelPackage().getMofClass().createMofClass(
            "AssociationEnd",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[7] = getModelPackage().getMofClass().createMofClass(
            "Association",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[8] = getModelPackage().getMofClass().createMofClass(
            "Exception",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[9] = getModelPackage().getMofClass().createMofClass(
            "Operation",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[10] = getModelPackage().getMofClass().createMofClass(
            "BehavioralFeature",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[11] = getModelPackage().getMofClass().createMofClass(
            "Reference",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[12] = getModelPackage().getMofClass().createMofClass(
            "Attribute",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[13] = getModelPackage().getMofClass().createMofClass(
            "StructuralFeature",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[14] = getModelPackage().getMofClass().createMofClass(
            "Feature",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[15] = getModelPackage().getMofClass().createMofClass(
            "AliasType",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[16] = getModelPackage().getMofClass().createMofClass(
            "StructureField",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[17] = getModelPackage().getMofClass().createMofClass(
            "StructureType",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[18] = getModelPackage().getMofClass().createMofClass(
            "CollectionType",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[19] = getModelPackage().getMofClass().createMofClass(
            "EnumerationType",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[20] = getModelPackage().getMofClass().createMofClass(
            "PrimitiveType",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[21] = getModelPackage().getMofClass().createMofClass(
            "DataType",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[22] = getModelPackage().getMofClass().createMofClass(
            "Class",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[23] = getModelPackage().getMofClass().createMofClass(
            "Classifier",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[24] = getModelPackage().getMofClass().createMofClass(
            "TypedElement",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[25] = getModelPackage().getMofClass().createMofClass(
            "GeneralizableElement",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[26] = getModelPackage().getMofClass().createMofClass(
            "Namespace",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        class_[27] = getModelPackage().getMofClass().createMofClass(
            "ModelElement",
            "",
            false,
            false,
            true,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);
    }

    private PrimitiveType[] primitiveType;

    private void primitiveTypeInit()
    {
        primitiveType = new PrimitiveType[15];

        primitiveType[0] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaWChar",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[1] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaChar",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[2] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaString",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[3] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaLongDouble",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[4] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaUnsignedLongLong",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[5] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaUnsignedLong",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[6] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaUnsignedShort",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[7] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaShort",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[8] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "CorbaOctet",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[9] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "String",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[10] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "Double",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[11] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "Float",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[12] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "Long",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[13] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "Integer",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        primitiveType[14] = getModelPackage().getPrimitiveType().createPrimitiveType(
            "Boolean",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);
    }

    private EnumerationType[] enumerationType;

    @SuppressWarnings("unchecked")
    private void enumerationTypeInit()
    {
        enumerationType = new EnumerationType[5];

        enumerationType[0] = getModelPackage().getEnumerationType().createEnumerationType(
            "EvaluationKind",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            Arrays.asList(
                "immediate",
                "deferred"));

        enumerationType[1] = getModelPackage().getEnumerationType().createEnumerationType(
            "DirectionKind",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            Arrays.asList(
                "in_dir",
                "out_dir",
                "inout_dir",
                "return_dir"));

        enumerationType[2] = getModelPackage().getEnumerationType().createEnumerationType(
            "AggregationKind",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            Arrays.asList(
                "none",
                "shared",
                "composite"));

        enumerationType[3] = getModelPackage().getEnumerationType().createEnumerationType(
            "ScopeKind",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            Arrays.asList(
                "instance_level",
                "classifier_level"));

        enumerationType[4] = getModelPackage().getEnumerationType().createEnumerationType(
            "VisibilityKind",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            Arrays.asList(
                "public_vis",
                "protected_vis",
                "private_vis"));
    }

    private void collectionTypeInit()
    {
    }

    private StructureType[] structureType;

    private void structureTypeInit()
    {
        structureType = new StructureType[1];

        structureType[0] = getModelPackage().getStructureType().createStructureType(
            "MultiplicityType",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);
    }

    private StructureField[] structureField;

    private void structureFieldInit()
    {
        structureField = new StructureField[4];

        structureField[0] = getModelPackage().getStructureField().createStructureField(
            "isUnique",
            "");

        structureField[1] = getModelPackage().getStructureField().createStructureField(
            "isOrdered",
            "");

        structureField[2] = getModelPackage().getStructureField().createStructureField(
            "upper",
            "");

        structureField[3] = getModelPackage().getStructureField().createStructureField(
            "lower",
            "");
    }

    private void aliasTypeInit()
    {
    }

    private Attribute[] attribute;

    private void attributeInit()
    {
        attribute = new Attribute[31];

        attribute[0] = getModelPackage().getAttribute().createAttribute(
            "values",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                false),
            true,
            false);

        attribute[1] = getModelPackage().getAttribute().createAttribute(
            "tagId",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[2] = getModelPackage().getAttribute().createAttribute(
            "value",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[3] = getModelPackage().getAttribute().createAttribute(
            "evaluationPolicy",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[4] = getModelPackage().getAttribute().createAttribute(
            "language",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[5] = getModelPackage().getAttribute().createAttribute(
            "expression",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[6] = getModelPackage().getAttribute().createAttribute(
            "multiplicity",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[7] = getModelPackage().getAttribute().createAttribute(
            "direction",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[8] = getModelPackage().getAttribute().createAttribute(
            "isClustered",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[9] = getModelPackage().getAttribute().createAttribute(
            "visibility",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[10] = getModelPackage().getAttribute().createAttribute(
            "isChangeable",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[11] = getModelPackage().getAttribute().createAttribute(
            "multiplicity",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[12] = getModelPackage().getAttribute().createAttribute(
            "aggregation",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[13] = getModelPackage().getAttribute().createAttribute(
            "isNavigable",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[14] = getModelPackage().getAttribute().createAttribute(
            "isDerived",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[15] = getModelPackage().getAttribute().createAttribute(
            "isQuery",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[16] = getModelPackage().getAttribute().createAttribute(
            "isDerived",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[17] = getModelPackage().getAttribute().createAttribute(
            "isChangeable",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[18] = getModelPackage().getAttribute().createAttribute(
            "multiplicity",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[19] = getModelPackage().getAttribute().createAttribute(
            "visibility",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[20] = getModelPackage().getAttribute().createAttribute(
            "scope",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[21] = getModelPackage().getAttribute().createAttribute(
            "multiplicity",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[22] = getModelPackage().getAttribute().createAttribute(
            "labels",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                true,
                true),
            true,
            false);

        attribute[23] = getModelPackage().getAttribute().createAttribute(
            "isSingleton",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[24] = getModelPackage().getAttribute().createAttribute(
            "visibility",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[25] = getModelPackage().getAttribute().createAttribute(
            "isAbstract",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[26] = getModelPackage().getAttribute().createAttribute(
            "isLeaf",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[27] = getModelPackage().getAttribute().createAttribute(
            "isRoot",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[28] = getModelPackage().getAttribute().createAttribute(
            "annotation",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);

        attribute[29] = getModelPackage().getAttribute().createAttribute(
            "qualifiedName",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                true,
                false),
            false,
            true);

        attribute[30] = getModelPackage().getAttribute().createAttribute(
            "name",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true,
            false);
    }

    private Reference[] reference;

    private void referenceInit()
    {
        reference = new Reference[12];

        reference[0] = getModelPackage().getReference().createReference(
            "elements",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                false,
                true),
            true);

        reference[1] = getModelPackage().getReference().createReference(
            "constrainedElements",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                false,
                true),
            true);

        reference[2] = getModelPackage().getReference().createReference(
            "importedNamespace",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        reference[3] = getModelPackage().getReference().createReference(
            "exceptions",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true),
            true);

        reference[4] = getModelPackage().getReference().createReference(
            "referencedEnd",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        reference[5] = getModelPackage().getReference().createReference(
            "exposedEnd",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        reference[6] = getModelPackage().getReference().createReference(
            "type",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        reference[7] = getModelPackage().getReference().createReference(
            "supertypes",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true),
            true);

        reference[8] = getModelPackage().getReference().createReference(
            "contents",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true),
            true);

        reference[9] = getModelPackage().getReference().createReference(
            "constraints",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        reference[10] = getModelPackage().getReference().createReference(
            "container",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                0,
                1,
                false,
                false),
            true);

        reference[11] = getModelPackage().getReference().createReference(
            "requiredElements",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            false);
    }

    private Operation[] operation;

    private void operationInit()
    {
        operation = new Operation[12];

        operation[0] = getModelPackage().getOperation().createOperation(
            "otherEnd",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[1] = getModelPackage().getOperation().createOperation(
            "findElementsByTypeExtended",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[2] = getModelPackage().getOperation().createOperation(
            "lookupElementExtended",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[3] = getModelPackage().getOperation().createOperation(
            "allSupertypes",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[4] = getModelPackage().getOperation().createOperation(
            "nameIsValid",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[5] = getModelPackage().getOperation().createOperation(
            "findElementsByType",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[6] = getModelPackage().getOperation().createOperation(
            "resolveQualifiedName",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[7] = getModelPackage().getOperation().createOperation(
            "lookupElement",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[8] = getModelPackage().getOperation().createOperation(
            "isVisible",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[9] = getModelPackage().getOperation().createOperation(
            "isFrozen",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[10] = getModelPackage().getOperation().createOperation(
            "isRequiredBecause",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        operation[11] = getModelPackage().getOperation().createOperation(
            "findRequiredElements",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);
    }

    private MofException[] exception;

    private void exceptionInit()
    {
        exception = new MofException[2];

        exception[0] = getModelPackage().getMofException().createMofException(
            "NameNotResolved",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        exception[1] = getModelPackage().getMofException().createMofException(
            "NameNotFound",
            "",
            javax.jmi.model.ScopeKindEnum.INSTANCE_LEVEL,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);
    }

    private Association[] association;

    private void associationInit()
    {
        association = new Association[10];

        association[0] = getModelPackage().getAssociation().createAssociation(
            "IsOfType",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        association[1] = getModelPackage().getAssociation().createAssociation(
            "RefersTo",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        association[2] = getModelPackage().getAssociation().createAssociation(
            "Exposes",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            true);

        association[3] = getModelPackage().getAssociation().createAssociation(
            "CanRaise",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        association[4] = getModelPackage().getAssociation().createAssociation(
            "Constrains",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        association[5] = getModelPackage().getAssociation().createAssociation(
            "Aliases",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        association[6] = getModelPackage().getAssociation().createAssociation(
            "Generalizes",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        association[7] = getModelPackage().getAssociation().createAssociation(
            "Contains",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);

        association[8] = getModelPackage().getAssociation().createAssociation(
            "DependsOn",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            true);

        association[9] = getModelPackage().getAssociation().createAssociation(
            "AttachesTo",
            "",
            true,
            true,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);
    }

    private AssociationEnd[] associationEnd;

    private void associationEndInit()
    {
        associationEnd = new AssociationEnd[20];

        associationEnd[0] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "typedElements",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        associationEnd[1] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "type",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        associationEnd[2] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "referencedEnd",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        associationEnd[3] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "referent",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        associationEnd[4] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "exposedEnd",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        associationEnd[5] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "referrer",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        associationEnd[6] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "except",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true),
            true);

        associationEnd[7] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "operation",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        associationEnd[8] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "constrainedElement",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                false,
                true),
            true);

        associationEnd[9] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "constraint",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        associationEnd[10] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "imported",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false),
            true);

        associationEnd[11] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "importer",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        associationEnd[12] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "subtype",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            true);

        associationEnd[13] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "supertype",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true),
            true);

        associationEnd[14] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "containedElement",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true),
            true);

        associationEnd[15] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "container",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.COMPOSITE,
            getModelPackage().createMultiplicityType(
                0,
                1,
                false,
                false),
            true);

        associationEnd[16] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "provider",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            false);

        associationEnd[17] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "dependent",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true),
            false);

        associationEnd[18] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "tag",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true),
            true);

        associationEnd[19] = getModelPackage().getAssociationEnd().createAssociationEnd(
            "modelElement",
            "",
            true,
            javax.jmi.model.AggregationKindEnum.NONE,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                false,
                true),
            true);
    }

    private MofPackage[] package_;

    private void packageInit()
    {
        package_ = new MofPackage[3];

        package_[0] = getModelPackage().getMofPackage().createMofPackage(
            "CorbaIdlTypes",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        package_[1] = getModelPackage().getMofPackage().createMofPackage(
            "Model",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);

        package_[2] = getModelPackage().getMofPackage().createMofPackage(
            "PrimitiveTypes",
            "",
            false,
            false,
            false,
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS);
    }

    private Import[] import_;

    private void importInit()
    {
        import_ = new Import[1];

        import_[0] = getModelPackage().getImport().createImport(
            "PrimitiveTypes",
            "",
            javax.jmi.model.VisibilityKindEnum.PUBLIC_VIS,
            false);
    }

    private Parameter[] parameter;

    private void parameterInit()
    {
        parameter = new Parameter[28];

        parameter[0] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[1] = getModelPackage().getParameter().createParameter(
            "includeSubtypes",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[2] = getModelPackage().getParameter().createParameter(
            "ofType",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[3] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true));

        parameter[4] = getModelPackage().getParameter().createParameter(
            "name",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[5] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[6] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true));

        parameter[7] = getModelPackage().getParameter().createParameter(
            "proposedName",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[8] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[9] = getModelPackage().getParameter().createParameter(
            "includeSubtypes",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[10] = getModelPackage().getParameter().createParameter(
            "ofType",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[11] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                true));

        parameter[12] = getModelPackage().getParameter().createParameter(
            "qualifiedName",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                true,
                false));

        parameter[13] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[14] = getModelPackage().getParameter().createParameter(
            "name",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[15] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[16] = getModelPackage().getParameter().createParameter(
            "restOfName",
            "",
            javax.jmi.model.DirectionKindEnum.OUT_DIR,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                true,
                false));

        parameter[17] = getModelPackage().getParameter().createParameter(
            "explanation",
            "",
            javax.jmi.model.DirectionKindEnum.OUT_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[18] = getModelPackage().getParameter().createParameter(
            "name",
            "",
            javax.jmi.model.DirectionKindEnum.OUT_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[19] = getModelPackage().getParameter().createParameter(
            "otherElement",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[20] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[21] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[22] = getModelPackage().getParameter().createParameter(
            "reason",
            "",
            javax.jmi.model.DirectionKindEnum.OUT_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[23] = getModelPackage().getParameter().createParameter(
            "otherElement",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[24] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[25] = getModelPackage().getParameter().createParameter(
            "recursive",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                1,
                false,
                false));

        parameter[26] = getModelPackage().getParameter().createParameter(
            "kinds",
            "",
            javax.jmi.model.DirectionKindEnum.IN_DIR,
            getModelPackage().createMultiplicityType(
                1,
                -1,
                false,
                true));

        parameter[27] = getModelPackage().getParameter().createParameter(
            "**result**",
            "",
            javax.jmi.model.DirectionKindEnum.RETURN_DIR,
            getModelPackage().createMultiplicityType(
                0,
                -1,
                false,
                true));
    }

    private Constraint[] constraint;

    private void constraintInit()
    {
        constraint = new Constraint[58];

        constraint[0] = getModelPackage().getConstraint().createConstraint(
            "ConstantsTypeMustBePrimitive",
            "",
            "context Constant\ninv: self.type.oclIsOfType(PrimitiveType)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[1] = getModelPackage().getConstraint().createConstraint(
            "ConstantsValueMustMatchType",
            "",
            "context Constant\ninv: ...",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[2] = getModelPackage().getConstraint().createConstraint(
            "ConstraintsLimitedToContainer",
            "",
            "context Constraint\ninv:\nself.constrainedElements ->\n  forAll(c | self.container.extendedNamespace() -> includes(c))",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[3] = getModelPackage().getConstraint().createConstraint(
            "CannotConstrainThisElement",
            "",
            "context Constraint\ninv:\nself.constrainedElements -> \n  forAll(c | not Set{Constraint, Tag, Imports, Constant} -> \n		  includes(c.oclType())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[4] = getModelPackage().getConstraint().createConstraint(
            "NestedPackagesCannotImport",
            "",
            "context Import\ninv:\nself.container -> notEmpty implies\n  self.container -> asSequence -> first -> container -> isEmpty",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[5] = getModelPackage().getConstraint().createConstraint(
            "CannotImportNestedComponents",
            "",
            "context Import\ninv: not self.container.allContents() -> includes(self.imported)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[6] = getModelPackage().getConstraint().createConstraint(
            "CannotImportSelf",
            "",
            "context Import\ninv: self.container <> self.imported",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[7] = getModelPackage().getConstraint().createConstraint(
            "CanOnlyImportPackagesAndClasses",
            "",
            "context Import\ninv:\nself.imported.oclIsTypeOf(Class) or\nself.imported.oclIsTypeOf(Package)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[8] = getModelPackage().getConstraint().createConstraint(
            "ImportedNamespaceMustBeVisible",
            "",
            "context Import\ninv: self.container.isVisible(self.importedNamespace)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[9] = getModelPackage().getConstraint().createConstraint(
            "PackagesCannotBeAbstract",
            "",
            "context Package\ninv: not self.isAbstract",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[10] = getModelPackage().getConstraint().createConstraint(
            "PackageContainmentRules",
            "",
            "context Package\ninv:\nSet{Package, Class, DataType, Association, Exception, \n    Constant, Constraint, Import, Tag}) ->\n  includesAll(self.contentTypes)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[11] = getModelPackage().getConstraint().createConstraint(
            "CannotHaveTwoAggregateEnds",
            "",
            "context AssociationEnd\ninv: \nself.aggregation <> #none implies self.otherEnd = #none",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[12] = getModelPackage().getConstraint().createConstraint(
            "CannotHaveTwoOrderedEnds",
            "",
            "context AssociationEnd\ninv:\nself.multiplicity.isOrdered implies \n  not self.otherEnd.multiplicity.isOrdered",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[13] = getModelPackage().getConstraint().createConstraint(
            "EndsMustBeUnique",
            "",
            "context AssociationEnd\ninv: \n(self.multiplicity.upper > 1 or \n self.multiplicity.upper = UNBOUNDED) implies\n  self.multiplicity.isUnique",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[14] = getModelPackage().getConstraint().createConstraint(
            "EndTypeMustBeClass",
            "",
            "context AssociationEnd\ninv: self.type.oclIsTypeOf(Class)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[15] = getModelPackage().getConstraint().createConstraint(
            "AssociationsMustBeBinary",
            "",
            "context Association\ninv: self.contents -> \nselect(c | c.oclIsTypeOf(AssociationEnd)) -> size = 2",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[16] = getModelPackage().getConstraint().createConstraint(
            "AssociationsMustBePublic",
            "",
            "context Association\ninv: self.visibility = #public_vis",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[17] = getModelPackage().getConstraint().createConstraint(
            "AssociationsCannotBeAbstract",
            "",
            "context Association\ninv: not self.isAbstract",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[18] = getModelPackage().getConstraint().createConstraint(
            "AssociationMustBeRootAndLeaf",
            "",
            "context Association\ninv: self.isRoot and self.isLeaf",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[19] = getModelPackage().getConstraint().createConstraint(
            "AssociationsHaveNoSupertypes",
            "",
            "context Association\ninv: self.supertypes -> isEmpty",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[20] = getModelPackage().getConstraint().createConstraint(
            "AssociationContainmentRules",
            "",
            "context Association\ninv: \nSet{AssociationEnd, Constraint, Tag} ->\n  includesAll(self.contentTypes())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[21] = getModelPackage().getConstraint().createConstraint(
            "ExceptionsHaveOnlyOutParameters",
            "",
            "context Exception\ninv:\nself.contents -> \n  select(c | c.oclIsTypeOf(Parameter)) ->\n    forAll(p : Parameter | p.direction = #out_dir)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[22] = getModelPackage().getConstraint().createConstraint(
            "ExceptionContainmentRules",
            "",
            "context Exception\ninv: Set{Parameter, Tag}) -> includesAll(self.contentTypes())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[23] = getModelPackage().getConstraint().createConstraint(
            "OperationExceptionsMustBeVisible",
            "",
            "context Operation\ninv: self.exceptions -> forAll(e | self.isVisible(e))",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[24] = getModelPackage().getConstraint().createConstraint(
            "OperationsHaveAtMostOneReturn",
            "",
            "context Operation\ninv:\nself.contents -> \n  select(c | c.oclIsTypeOf(Parameter)) ->\n    select(p : Parameter | p.direction = #return_dir) -> size < 2",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[25] = getModelPackage().getConstraint().createConstraint(
            "OperationContainmentRules",
            "",
            "context Operation\ninv: \nSet{Parameter, Constraint, Tag} -> includesAll(self.contentTypes())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[26] = getModelPackage().getConstraint().createConstraint(
            "ReferencedEndMustBeVisible",
            "",
            "context Reference\ninv: self.isVisible(self.referencedEnd)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[27] = getModelPackage().getConstraint().createConstraint(
            "ContainerMustMatchExposedType",
            "",
            "context Reference\ninv:\nself.container.allSupertypes() -> including(self) ->\n  includes(self.referencedEnd.otherEnd.type)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[28] = getModelPackage().getConstraint().createConstraint(
            "ReferencedEndMustBeNavigable",
            "",
            "context Reference\ninv: self.referencedEnd.isNavigable",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[29] = getModelPackage().getConstraint().createConstraint(
            "ReferenceTypeMustMatchEndType",
            "",
            "context Reference\ninv: self.type = self.referencedEnd.type",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[30] = getModelPackage().getConstraint().createConstraint(
            "ChangeableReferenceMustHaveChangeableEnd",
            "",
            "context Reference\ninv: self.isChangeable = self.referencedEnd.isChangeable",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[31] = getModelPackage().getConstraint().createConstraint(
            "ReferenceMustBeInstanceScoped",
            "",
            "context Reference\ninv: self.scope = #instance_level",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[32] = getModelPackage().getConstraint().createConstraint(
            "ReferenceMultiplicityMustMatchEnd",
            "",
            "context Reference\ninv: self.multiplicity = self.referencedEnd.multiplicity",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[33] = getModelPackage().getConstraint().createConstraint(
            "StructureFieldContainmentRules",
            "",
            "context StructureField\ninv: Set{Constraint, Tag}) -> includesAll(self.contentTypes)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[34] = getModelPackage().getConstraint().createConstraint(
            "MustHaveFields",
            "",
            "context StructureType\ninv: self.contents -> exists(c | c.oclIsOfType(StructureField))",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[35] = getModelPackage().getConstraint().createConstraint(
            "DataTypesCannotBeAbstract",
            "",
            "context DataType\ninv: not self.isAbstract",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[36] = getModelPackage().getConstraint().createConstraint(
            "DataTypesHaveNoSupertypes",
            "",
            "context DataType\ninv: self.supertypes -> isEmpty",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[37] = getModelPackage().getConstraint().createConstraint(
            "DataTypeContainmentRules",
            "",
            "context DataType\ninv: \nif self.oclIsOfType(StructureType)\nthen\n  Set{TypeAlias, Constraint, Tag, StructureField} ->\n    includesAll(self.contentTypes())\nelse\n  Set{TypeAlias, Constraint, Tag} -> \n    includesAll(self.contentTypes())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[38] = getModelPackage().getConstraint().createConstraint(
            "MustBeUnorderedNonunique",
            "",
            "context MultiplicityType\ninv: \nself.upper = 1 implies (not self.isOrdered and not self.isUnique)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[39] = getModelPackage().getConstraint().createConstraint(
            "UpperMustBePositive",
            "",
            "context MultiplicityType\ninv: self.upper >= 1 or self.upper = Unbounded",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[40] = getModelPackage().getConstraint().createConstraint(
            "LowerCannotExceedUpper",
            "",
            "context MultiplicityType\ninv: self.lower <= self.upper or self.upper = Unbounded",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[41] = getModelPackage().getConstraint().createConstraint(
            "LowerCannotBeNegativeOrUnbounded",
            "",
            "context MultiplicityType\ninv: self.lower >= 0 and self.lower <> Unbounded",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[42] = getModelPackage().getConstraint().createConstraint(
            "AbstractClassesCannotBeSingleton",
            "",
            "context Class\ninv: self.isAbstract implies not self.isSingleton",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[43] = getModelPackage().getConstraint().createConstraint(
            "ClassContainmentRules",
            "",
            "context Class\ninv: \nSet{Class, DataType, Attribute, Reference, Operation,\n    Exception, Constant, Constraint, Tag} ->\n  includesAll(self.contentTypes())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[44] = getModelPackage().getConstraint().createConstraint(
            "TypeMustBeVisible",
            "",
            "context TypedElement\ninv: self.isVisible(self.type)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[45] = getModelPackage().getConstraint().createConstraint(
            "AssociationsCannotBeTypes",
            "",
            "context TypedElement\ninv: not self.type.oclIsKindOf(Association)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[46] = getModelPackage().getConstraint().createConstraint(
            "NoSubtypesAllowedForLeaf",
            "",
            "context GeneralizableElement\ninv: self.supertypes -> forAll(s | not s.isLeaf)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[47] = getModelPackage().getConstraint().createConstraint(
            "SupertypesMustBeVisible",
            "",
            "context GeneralizableElement\ninv: self.supertypes -> forAll(s | self.isVisible(s))",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[48] = getModelPackage().getConstraint().createConstraint(
            "NoSupertypesAllowedForRoot",
            "",
            "context GeneralizableElement\ninv: self.isRoot implies self.supertypes -> isEmpty",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[49] = getModelPackage().getConstraint().createConstraint(
            "DiamondRuleMustBeObeyed",
            "",
            "context GeneralizableElement\ninv:\nlet superNamespaces = \n  self.supertypes -> collect(s | s.extendedNamespace) in\nsuperNamespaces -> asSet -> isUnique(s | s.name)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[50] = getModelPackage().getConstraint().createConstraint(
            "ContentsMustNotCollideWithSupertypes",
            "",
            "context GeneralizableElement\ninv:\nlet superContents = self.allSupertypes() -> \n  collect(s | s.contents) in\nself.contents -> forAll(m1 | superContents -> \n  forAll(m2 | m1.name = m2.name implies m1 = m2))",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[51] = getModelPackage().getConstraint().createConstraint(
            "SupertypeKindMustBeSame",
            "",
            "context GeneralizableElement\ninv: self.supertypes -> forAll(s | s.oclType() = self.oclType())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[52] = getModelPackage().getConstraint().createConstraint(
            "SupertypeMustNotBeSelf",
            "",
            "context GeneralizableElement\ninv: self.allSupertypes() -> forAll(s | s <> self)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[53] = getModelPackage().getConstraint().createConstraint(
            "ContentNamesMustNotCollide",
            "",
            "context Namespace\ninv: self.contents.forAll(e1, e2 | e1.name = e2.name implies r1 = r2)",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[54] = getModelPackage().getConstraint().createConstraint(
            "FrozenDependenciesCannotBeChanged",
            "",
            "context ModelElement\npost: \nself.isFrozen() implies \n  let myClasses = self.oclType() -> allSupertypes() -> \n    includes(self.oclType()) in\n  let myRefs = Set(Reference) = \n    self.RefBaseObject::refMetaObject() -> asOclType(Class) -> \n    findElementsByTypeExtended(Reference) in\n  let myDepRefs = myRefs -> \n    select(r | Set{\"\n		  \"\n		  \"\n      includes(r.name)) in\n  myDepRefs -> forAll(r | \n    self.RefObject::refValue@pre(r) = self.RefObject::refValue(r))",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[55] = getModelPackage().getConstraint().createConstraint(
            "FrozenElementsCannotBeDeleted",
            "",
            "context ModelElement\npost: \n(self.isFrozen@pre() and \n self.container@pre -> notEmpty and\n self.container.isFrozen@pre()) implies\n(self.container.Object::non_existent() or \n not self.Object::non_existent())",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[56] = getModelPackage().getConstraint().createConstraint(
            "FrozenAttributesCannotBeChanged",
            "",
            "context ModelElement\ninv: \nself.isFrozen() implies \n  let myTypes = \n    self.oclType() -> allSupertypes() -> includes(self.oclType()) in\n  let myAttrs : Set(Attribute) = \n    self.RefBaseObject::refMetaObject() -> asOclType(Class) -> \n    findElementsByTypeExtended(Attribute) in\n  myAttrs -> forAll(a | \n    self.RefObject::refValue@pre(a) = self.RefObject::refValue(a))",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);

        constraint[57] = getModelPackage().getConstraint().createConstraint(
            "MustBeContainedUnlessPackage",
            "",
            "context ModelElement\ninv: \nnot self.oclIsTypeOf(Package) implies \n  self.container -> size = 1",
            "OCL",
            javax.jmi.model.EvaluationKindEnum.DEFERRED);
    }

    private Constant[] constant;

    private void constantInit()
    {
        constant = new Constant[13];

        constant[0] = getModelPackage().getConstant().createConstant(
            "Unbounded",
            "",
            "-1");

        constant[1] = getModelPackage().getConstant().createConstant(
            "AllDep",
            "",
            "all");

        constant[2] = getModelPackage().getConstant().createConstant(
            "IndirectDep",
            "",
            "indirect");

        constant[3] = getModelPackage().getConstant().createConstant(
            "TaggedElementsDep",
            "",
            "tagged elements");

        constant[4] = getModelPackage().getConstant().createConstant(
            "ReferencedEndsDep",
            "",
            "referenced ends");

        constant[5] = getModelPackage().getConstant().createConstant(
            "TypeDefinitionDep",
            "",
            "type definition");

        constant[6] = getModelPackage().getConstant().createConstant(
            "ImportDep",
            "",
            "import");

        constant[7] = getModelPackage().getConstant().createConstant(
            "SpecializationDep",
            "",
            "specialization");

        constant[8] = getModelPackage().getConstant().createConstant(
            "ConstrainedElementsDep",
            "",
            "constrained elements");

        constant[9] = getModelPackage().getConstant().createConstant(
            "ConstraintDep",
            "",
            "constraint");

        constant[10] = getModelPackage().getConstant().createConstant(
            "SignatureDep",
            "",
            "signature");

        constant[11] = getModelPackage().getConstant().createConstant(
            "ContentsDep",
            "",
            "contents");

        constant[12] = getModelPackage().getConstant().createConstant(
            "ContainerDep",
            "",
            "container");
    }

    private Tag[] tag;

    @SuppressWarnings("unchecked")
    private void tagInit()
    {
        tag = new Tag[64];

        tag[0] = getModelPackage().getTag().createTag(
            "*tag",
            "",
            "javax.jmi.packagePrefix",
            Arrays.asList(
                "javax.jmi"));

        tag[1] = getModelPackage().getTag().createTag(
            "*tag_62",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[2] = getModelPackage().getTag().createTag(
            "*tag_61",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[3] = getModelPackage().getTag().createTag(
            "*tag_60",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[4] = getModelPackage().getTag().createTag(
            "*tag_59",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[5] = getModelPackage().getTag().createTag(
            "*tag_58",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[6] = getModelPackage().getTag().createTag(
            "*tag_57",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[7] = getModelPackage().getTag().createTag(
            "*tag_56",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[8] = getModelPackage().getTag().createTag(
            "*tag_55",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[9] = getModelPackage().getTag().createTag(
            "*tag_54",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[10] = getModelPackage().getTag().createTag(
            "*tag_53",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[11] = getModelPackage().getTag().createTag(
            "*tag_52",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[12] = getModelPackage().getTag().createTag(
            "*tag_51",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[13] = getModelPackage().getTag().createTag(
            "*tag_50",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[14] = getModelPackage().getTag().createTag(
            "*tag_49",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[15] = getModelPackage().getTag().createTag(
            "*tag_48",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[16] = getModelPackage().getTag().createTag(
            "*tag_47",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[17] = getModelPackage().getTag().createTag(
            "*tag_46",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[18] = getModelPackage().getTag().createTag(
            "*tag_45",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[19] = getModelPackage().getTag().createTag(
            "*tag_44",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[20] = getModelPackage().getTag().createTag(
            "*tag_43",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[21] = getModelPackage().getTag().createTag(
            "*tag_42",
            "",
            "javax.jmi.substituteName",
            Arrays.asList(
                "MofPackage"));

        tag[22] = getModelPackage().getTag().createTag(
            "*tag_41",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[23] = getModelPackage().getTag().createTag(
            "*tag_40",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[24] = getModelPackage().getTag().createTag(
            "*tag_39",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[25] = getModelPackage().getTag().createTag(
            "*tag_38",
            "",
            "javax.jmi.substituteName",
            Arrays.asList(
                "MofException"));

        tag[26] = getModelPackage().getTag().createTag(
            "*tag_37",
            "",
            "org.omg.mof.idl_substitute_name",
            Arrays.asList(
                "MofException"));

        tag[27] = getModelPackage().getTag().createTag(
            "*tag_36",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[28] = getModelPackage().getTag().createTag(
            "*tag_35",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[29] = getModelPackage().getTag().createTag(
            "*tag_34",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[30] = getModelPackage().getTag().createTag(
            "*tag_33",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[31] = getModelPackage().getTag().createTag(
            "*tag_32",
            "",
            "org.omg.mof.idl_substitute_name",
            Arrays.asList(
                "MofAttribute"));

        tag[32] = getModelPackage().getTag().createTag(
            "*tag_31",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[33] = getModelPackage().getTag().createTag(
            "*tag_30",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[34] = getModelPackage().getTag().createTag(
            "*tag_29",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[35] = getModelPackage().getTag().createTag(
            "*tag_28",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[36] = getModelPackage().getTag().createTag(
            "*tag_27",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[37] = getModelPackage().getTag().createTag(
            "*tag_26",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[38] = getModelPackage().getTag().createTag(
            "*tag_25",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[39] = getModelPackage().getTag().createTag(
            "*tag_24",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[40] = getModelPackage().getTag().createTag(
            "*tag_23",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[41] = getModelPackage().getTag().createTag(
            "*tag_22",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[42] = getModelPackage().getTag().createTag(
            "*tag_21",
            "",
            "javax.jmi.substituteName",
            Arrays.asList(
                "MofClass"));

        tag[43] = getModelPackage().getTag().createTag(
            "*tag_20",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[44] = getModelPackage().getTag().createTag(
            "*tag_19",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[45] = getModelPackage().getTag().createTag(
            "*tag_18",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[46] = getModelPackage().getTag().createTag(
            "*tag_17",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[47] = getModelPackage().getTag().createTag(
            "*tag_16",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[48] = getModelPackage().getTag().createTag(
            "*tag_15",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[49] = getModelPackage().getTag().createTag(
            "*tag_14",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[50] = getModelPackage().getTag().createTag(
            "*tag_13",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[51] = getModelPackage().getTag().createTag(
            "*tag_12",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[52] = getModelPackage().getTag().createTag(
            "*tag_11",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[53] = getModelPackage().getTag().createTag(
            "*tag_10",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[54] = getModelPackage().getTag().createTag(
            "*tag_9",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[55] = getModelPackage().getTag().createTag(
            "*tag_8",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[56] = getModelPackage().getTag().createTag(
            "*tag_7",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[57] = getModelPackage().getTag().createTag(
            "*tag_6",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[58] = getModelPackage().getTag().createTag(
            "*tag_5",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[59] = getModelPackage().getTag().createTag(
            "*tag_4",
            "",
            "org.omg.xmi.namespace",
            Arrays.asList(
                "Model"));

        tag[60] = getModelPackage().getTag().createTag(
            "*tag_3",
            "",
            "javax.jmi.packagePrefix",
            Arrays.asList(
                "javax.jmi"));

        tag[61] = getModelPackage().getTag().createTag(
            "*tag_2",
            "",
            "org.omg.mof.idl_version",
            Arrays.asList(
                "1.4"));

        tag[62] = getModelPackage().getTag().createTag(
            "*tag_1",
            "",
            "org.omg.mof.idl_prefix",
            Arrays.asList(
                "org.omg.mof"));

        tag[63] = getModelPackage().getTag().createTag(
            "*tag",
            "",
            "javax.jmi.packagePrefix",
            Arrays.asList(
                "javax.jmi"));
    }

    private void attachesToAssocInit()
    {
        AttachesTo attachesToAssoc = getModelPackage().getAttachesTo();

        attachesToAssoc.add(package_[2], tag[63]);
        attachesToAssoc.add(attribute[30], tag[57]);
        attachesToAssoc.add(attribute[29], tag[56]);
        attachesToAssoc.add(attribute[28], tag[55]);
        attachesToAssoc.add(operation[11], tag[54]);
        attachesToAssoc.add(operation[10], tag[53]);
        attachesToAssoc.add(class_[27], tag[58]);
        attachesToAssoc.add(exception[1], tag[51]);
        attachesToAssoc.add(exception[0], tag[50]);
        attachesToAssoc.add(operation[7], tag[49]);
        attachesToAssoc.add(operation[6], tag[48]);
        attachesToAssoc.add(operation[4], tag[47]);
        attachesToAssoc.add(class_[26], tag[52]);
        attachesToAssoc.add(class_[25], tag[46]);
        attachesToAssoc.add(class_[24], tag[45]);
        attachesToAssoc.add(class_[23], tag[44]);
        attachesToAssoc.add(class_[2], tag[18]);
        attachesToAssoc.add(attribute[2], tag[14]);
        attachesToAssoc.add(class_[1], tag[15]);
        attachesToAssoc.add(attribute[1], tag[12]);
        attachesToAssoc.add(attribute[0], tag[11]);
        attachesToAssoc.add(class_[0], tag[13]);
        attachesToAssoc.add(association[9], tag[10]);
        attachesToAssoc.add(association[8], tag[9]);
        attachesToAssoc.add(association[7], tag[8]);
        attachesToAssoc.add(association[6], tag[7]);
        attachesToAssoc.add(association[5], tag[6]);
        attachesToAssoc.add(association[4], tag[5]);
        attachesToAssoc.add(association[3], tag[4]);
        attachesToAssoc.add(association[2], tag[3]);
        attachesToAssoc.add(association[1], tag[2]);
        attachesToAssoc.add(association[0], tag[1]);
        attachesToAssoc.add(package_[1], tag[62]);
        attachesToAssoc.add(package_[1], tag[61]);
        attachesToAssoc.add(package_[1], tag[60]);
        attachesToAssoc.add(package_[1], tag[59]);
        attachesToAssoc.add(package_[0], tag[0]);
        attachesToAssoc.add(class_[22], tag[43]);
        attachesToAssoc.add(class_[22], tag[42]);
        attachesToAssoc.add(class_[21], tag[41]);
        attachesToAssoc.add(class_[20], tag[40]);
        attachesToAssoc.add(class_[19], tag[39]);
        attachesToAssoc.add(class_[18], tag[38]);
        attachesToAssoc.add(class_[17], tag[37]);
        attachesToAssoc.add(class_[16], tag[36]);
        attachesToAssoc.add(class_[15], tag[35]);
        attachesToAssoc.add(class_[14], tag[34]);
        attachesToAssoc.add(class_[13], tag[33]);
        attachesToAssoc.add(class_[12], tag[32]);
        attachesToAssoc.add(class_[12], tag[31]);
        attachesToAssoc.add(class_[11], tag[30]);
        attachesToAssoc.add(class_[10], tag[29]);
        attachesToAssoc.add(class_[9], tag[28]);
        attachesToAssoc.add(class_[8], tag[27]);
        attachesToAssoc.add(class_[8], tag[26]);
        attachesToAssoc.add(class_[8], tag[25]);
        attachesToAssoc.add(class_[7], tag[24]);
        attachesToAssoc.add(class_[6], tag[23]);
        attachesToAssoc.add(class_[5], tag[22]);
        attachesToAssoc.add(class_[5], tag[21]);
        attachesToAssoc.add(class_[4], tag[20]);
        attachesToAssoc.add(class_[3], tag[19]);
        attachesToAssoc.add(attribute[5], tag[17]);
        attachesToAssoc.add(attribute[4], tag[16]);
    }

    private void containsAssocInit()
    {
        Contains containsAssoc = getModelPackage().getContains();

        containsAssoc.add(package_[2], tag[63]);
        containsAssoc.add(package_[2], primitiveType[14]);
        containsAssoc.add(package_[2], primitiveType[13]);
        containsAssoc.add(package_[2], primitiveType[12]);
        containsAssoc.add(package_[2], primitiveType[11]);
        containsAssoc.add(package_[2], primitiveType[10]);
        containsAssoc.add(package_[2], primitiveType[9]);
        containsAssoc.add(operation[11], parameter[27]);
        containsAssoc.add(operation[11], parameter[26]);
        containsAssoc.add(operation[11], parameter[25]);
        containsAssoc.add(operation[11], tag[54]);
        containsAssoc.add(operation[10], parameter[24]);
        containsAssoc.add(operation[10], parameter[23]);
        containsAssoc.add(operation[10], parameter[22]);
        containsAssoc.add(operation[10], tag[53]);
        containsAssoc.add(operation[9], parameter[21]);
        containsAssoc.add(operation[8], parameter[20]);
        containsAssoc.add(operation[8], parameter[19]);
        containsAssoc.add(class_[27], tag[58]);
        containsAssoc.add(class_[27], constraint[57]);
        containsAssoc.add(class_[27], constraint[56]);
        containsAssoc.add(class_[27], constraint[55]);
        containsAssoc.add(class_[27], constraint[54]);
        containsAssoc.add(class_[27], attribute[30]);
        containsAssoc.add(class_[27], tag[57]);
        containsAssoc.add(class_[27], attribute[29]);
        containsAssoc.add(class_[27], tag[56]);
        containsAssoc.add(class_[27], attribute[28]);
        containsAssoc.add(class_[27], tag[55]);
        containsAssoc.add(class_[27], reference[11]);
        containsAssoc.add(class_[27], constant[12]);
        containsAssoc.add(class_[27], constant[11]);
        containsAssoc.add(class_[27], constant[10]);
        containsAssoc.add(class_[27], constant[9]);
        containsAssoc.add(class_[27], constant[8]);
        containsAssoc.add(class_[27], constant[7]);
        containsAssoc.add(class_[27], constant[6]);
        containsAssoc.add(class_[27], constant[5]);
        containsAssoc.add(class_[27], constant[4]);
        containsAssoc.add(class_[27], constant[3]);
        containsAssoc.add(class_[27], constant[2]);
        containsAssoc.add(class_[27], constant[1]);
        containsAssoc.add(class_[27], operation[11]);
        containsAssoc.add(class_[27], operation[10]);
        containsAssoc.add(class_[27], reference[10]);
        containsAssoc.add(class_[27], reference[9]);
        containsAssoc.add(class_[27], operation[9]);
        containsAssoc.add(class_[27], operation[8]);
        containsAssoc.add(exception[1], parameter[18]);
        containsAssoc.add(exception[1], tag[51]);
        containsAssoc.add(exception[0], parameter[17]);
        containsAssoc.add(exception[0], parameter[16]);
        containsAssoc.add(exception[0], tag[50]);
        containsAssoc.add(operation[7], parameter[15]);
        containsAssoc.add(operation[7], parameter[14]);
        containsAssoc.add(operation[7], tag[49]);
        containsAssoc.add(operation[6], parameter[13]);
        containsAssoc.add(operation[6], parameter[12]);
        containsAssoc.add(operation[6], tag[48]);
        containsAssoc.add(operation[5], parameter[11]);
        containsAssoc.add(operation[5], parameter[10]);
        containsAssoc.add(operation[5], parameter[9]);
        containsAssoc.add(operation[4], parameter[8]);
        containsAssoc.add(operation[4], parameter[7]);
        containsAssoc.add(operation[4], tag[47]);
        containsAssoc.add(class_[26], tag[52]);
        containsAssoc.add(class_[26], constraint[53]);
        containsAssoc.add(class_[26], exception[1]);
        containsAssoc.add(class_[26], exception[0]);
        containsAssoc.add(class_[26], reference[8]);
        containsAssoc.add(class_[26], operation[7]);
        containsAssoc.add(class_[26], operation[6]);
        containsAssoc.add(class_[26], operation[5]);
        containsAssoc.add(class_[26], operation[4]);
        containsAssoc.add(operation[3], parameter[6]);
        containsAssoc.add(operation[2], parameter[5]);
        containsAssoc.add(operation[2], parameter[4]);
        containsAssoc.add(operation[1], parameter[3]);
        containsAssoc.add(operation[1], parameter[2]);
        containsAssoc.add(operation[1], parameter[1]);
        containsAssoc.add(class_[25], tag[46]);
        containsAssoc.add(class_[25], constraint[52]);
        containsAssoc.add(class_[25], constraint[51]);
        containsAssoc.add(class_[25], constraint[50]);
        containsAssoc.add(class_[25], constraint[49]);
        containsAssoc.add(class_[25], constraint[48]);
        containsAssoc.add(class_[25], constraint[47]);
        containsAssoc.add(class_[25], constraint[46]);
        containsAssoc.add(class_[25], attribute[27]);
        containsAssoc.add(class_[25], attribute[26]);
        containsAssoc.add(class_[25], attribute[25]);
        containsAssoc.add(class_[25], attribute[24]);
        containsAssoc.add(class_[25], reference[7]);
        containsAssoc.add(class_[25], operation[3]);
        containsAssoc.add(class_[25], operation[2]);
        containsAssoc.add(class_[25], operation[1]);
        containsAssoc.add(class_[24], tag[45]);
        containsAssoc.add(class_[24], constraint[45]);
        containsAssoc.add(class_[24], constraint[44]);
        containsAssoc.add(class_[24], reference[6]);
        containsAssoc.add(class_[23], tag[44]);
        containsAssoc.add(class_[2], tag[18]);
        containsAssoc.add(class_[2], constraint[3]);
        containsAssoc.add(class_[2], constraint[2]);
        containsAssoc.add(class_[2], attribute[5]);
        containsAssoc.add(class_[2], tag[17]);
        containsAssoc.add(class_[2], attribute[4]);
        containsAssoc.add(class_[2], tag[16]);
        containsAssoc.add(class_[2], enumerationType[0]);
        containsAssoc.add(class_[2], attribute[3]);
        containsAssoc.add(class_[2], reference[1]);
        containsAssoc.add(class_[1], tag[15]);
        containsAssoc.add(class_[1], constraint[1]);
        containsAssoc.add(class_[1], constraint[0]);
        containsAssoc.add(class_[1], attribute[2]);
        containsAssoc.add(class_[1], tag[14]);
        containsAssoc.add(class_[0], tag[13]);
        containsAssoc.add(class_[0], attribute[1]);
        containsAssoc.add(class_[0], tag[12]);
        containsAssoc.add(class_[0], attribute[0]);
        containsAssoc.add(class_[0], tag[11]);
        containsAssoc.add(class_[0], reference[0]);
        containsAssoc.add(association[9], tag[10]);
        containsAssoc.add(association[9], associationEnd[19]);
        containsAssoc.add(association[9], associationEnd[18]);
        containsAssoc.add(association[8], tag[9]);
        containsAssoc.add(association[8], associationEnd[17]);
        containsAssoc.add(association[8], associationEnd[16]);
        containsAssoc.add(association[7], tag[8]);
        containsAssoc.add(association[7], associationEnd[15]);
        containsAssoc.add(association[7], associationEnd[14]);
        containsAssoc.add(association[6], tag[7]);
        containsAssoc.add(association[6], associationEnd[13]);
        containsAssoc.add(association[6], associationEnd[12]);
        containsAssoc.add(association[5], tag[6]);
        containsAssoc.add(association[5], associationEnd[11]);
        containsAssoc.add(association[5], associationEnd[10]);
        containsAssoc.add(association[4], tag[5]);
        containsAssoc.add(association[4], associationEnd[9]);
        containsAssoc.add(association[4], associationEnd[8]);
        containsAssoc.add(association[3], tag[4]);
        containsAssoc.add(association[3], associationEnd[7]);
        containsAssoc.add(association[3], associationEnd[6]);
        containsAssoc.add(association[2], tag[3]);
        containsAssoc.add(association[2], associationEnd[5]);
        containsAssoc.add(association[2], associationEnd[4]);
        containsAssoc.add(association[1], tag[2]);
        containsAssoc.add(association[1], associationEnd[3]);
        containsAssoc.add(association[1], associationEnd[2]);
        containsAssoc.add(association[0], tag[1]);
        containsAssoc.add(association[0], associationEnd[1]);
        containsAssoc.add(association[0], associationEnd[0]);
        containsAssoc.add(package_[1], import_[0]);
        containsAssoc.add(package_[1], tag[62]);
        containsAssoc.add(package_[1], tag[61]);
        containsAssoc.add(package_[1], tag[60]);
        containsAssoc.add(package_[1], tag[59]);
        containsAssoc.add(package_[1], class_[27]);
        containsAssoc.add(package_[1], enumerationType[4]);
        containsAssoc.add(package_[1], class_[26]);
        containsAssoc.add(package_[1], class_[25]);
        containsAssoc.add(package_[1], class_[24]);
        containsAssoc.add(package_[1], class_[23]);
        containsAssoc.add(package_[1], class_[22]);
        containsAssoc.add(package_[1], constant[0]);
        containsAssoc.add(package_[1], structureType[0]);
        containsAssoc.add(package_[1], constraint[41]);
        containsAssoc.add(package_[1], constraint[40]);
        containsAssoc.add(package_[1], constraint[39]);
        containsAssoc.add(package_[1], constraint[38]);
        containsAssoc.add(package_[1], class_[21]);
        containsAssoc.add(package_[1], class_[20]);
        containsAssoc.add(package_[1], class_[19]);
        containsAssoc.add(package_[1], class_[18]);
        containsAssoc.add(package_[1], class_[17]);
        containsAssoc.add(package_[1], class_[16]);
        containsAssoc.add(package_[1], class_[15]);
        containsAssoc.add(package_[1], enumerationType[3]);
        containsAssoc.add(package_[1], class_[14]);
        containsAssoc.add(package_[1], class_[13]);
        containsAssoc.add(package_[1], class_[12]);
        containsAssoc.add(package_[1], class_[11]);
        containsAssoc.add(package_[1], class_[10]);
        containsAssoc.add(package_[1], class_[9]);
        containsAssoc.add(package_[1], class_[8]);
        containsAssoc.add(package_[1], class_[7]);
        containsAssoc.add(package_[1], enumerationType[2]);
        containsAssoc.add(package_[1], class_[6]);
        containsAssoc.add(package_[1], class_[5]);
        containsAssoc.add(package_[1], class_[4]);
        containsAssoc.add(package_[1], enumerationType[1]);
        containsAssoc.add(package_[1], class_[3]);
        containsAssoc.add(package_[1], class_[2]);
        containsAssoc.add(package_[1], class_[1]);
        containsAssoc.add(package_[1], class_[0]);
        containsAssoc.add(package_[1], association[9]);
        containsAssoc.add(package_[1], association[8]);
        containsAssoc.add(package_[1], association[7]);
        containsAssoc.add(package_[1], association[6]);
        containsAssoc.add(package_[1], association[5]);
        containsAssoc.add(package_[1], association[4]);
        containsAssoc.add(package_[1], association[3]);
        containsAssoc.add(package_[1], association[2]);
        containsAssoc.add(package_[1], association[1]);
        containsAssoc.add(package_[1], association[0]);
        containsAssoc.add(package_[0], tag[0]);
        containsAssoc.add(package_[0], primitiveType[8]);
        containsAssoc.add(package_[0], primitiveType[7]);
        containsAssoc.add(package_[0], primitiveType[6]);
        containsAssoc.add(package_[0], primitiveType[5]);
        containsAssoc.add(package_[0], primitiveType[4]);
        containsAssoc.add(package_[0], primitiveType[3]);
        containsAssoc.add(package_[0], primitiveType[2]);
        containsAssoc.add(package_[0], primitiveType[1]);
        containsAssoc.add(package_[0], primitiveType[0]);
        containsAssoc.add(class_[22], tag[43]);
        containsAssoc.add(class_[22], tag[42]);
        containsAssoc.add(class_[22], constraint[43]);
        containsAssoc.add(class_[22], constraint[42]);
        containsAssoc.add(class_[22], attribute[23]);
        containsAssoc.add(structureType[0], structureField[3]);
        containsAssoc.add(structureType[0], structureField[2]);
        containsAssoc.add(structureType[0], structureField[1]);
        containsAssoc.add(structureType[0], structureField[0]);
        containsAssoc.add(class_[21], tag[41]);
        containsAssoc.add(class_[21], constraint[37]);
        containsAssoc.add(class_[21], constraint[36]);
        containsAssoc.add(class_[21], constraint[35]);
        containsAssoc.add(class_[20], tag[40]);
        containsAssoc.add(class_[19], tag[39]);
        containsAssoc.add(class_[19], attribute[22]);
        containsAssoc.add(class_[18], tag[38]);
        containsAssoc.add(class_[18], attribute[21]);
        containsAssoc.add(class_[17], tag[37]);
        containsAssoc.add(class_[17], constraint[34]);
        containsAssoc.add(class_[16], tag[36]);
        containsAssoc.add(class_[16], constraint[33]);
        containsAssoc.add(class_[15], tag[35]);
        containsAssoc.add(class_[14], tag[34]);
        containsAssoc.add(class_[14], attribute[20]);
        containsAssoc.add(class_[14], attribute[19]);
        containsAssoc.add(class_[13], tag[33]);
        containsAssoc.add(class_[13], attribute[18]);
        containsAssoc.add(class_[13], attribute[17]);
        containsAssoc.add(class_[12], tag[32]);
        containsAssoc.add(class_[12], tag[31]);
        containsAssoc.add(class_[12], attribute[16]);
        containsAssoc.add(class_[11], tag[30]);
        containsAssoc.add(class_[11], constraint[32]);
        containsAssoc.add(class_[11], constraint[31]);
        containsAssoc.add(class_[11], constraint[30]);
        containsAssoc.add(class_[11], constraint[29]);
        containsAssoc.add(class_[11], constraint[28]);
        containsAssoc.add(class_[11], constraint[27]);
        containsAssoc.add(class_[11], constraint[26]);
        containsAssoc.add(class_[11], reference[5]);
        containsAssoc.add(class_[11], reference[4]);
        containsAssoc.add(class_[10], tag[29]);
        containsAssoc.add(class_[9], tag[28]);
        containsAssoc.add(class_[9], constraint[25]);
        containsAssoc.add(class_[9], constraint[24]);
        containsAssoc.add(class_[9], constraint[23]);
        containsAssoc.add(class_[9], attribute[15]);
        containsAssoc.add(class_[9], reference[3]);
        containsAssoc.add(class_[8], tag[27]);
        containsAssoc.add(class_[8], tag[26]);
        containsAssoc.add(class_[8], tag[25]);
        containsAssoc.add(class_[8], constraint[22]);
        containsAssoc.add(class_[8], constraint[21]);
        containsAssoc.add(class_[7], tag[24]);
        containsAssoc.add(class_[7], constraint[20]);
        containsAssoc.add(class_[7], constraint[19]);
        containsAssoc.add(class_[7], constraint[18]);
        containsAssoc.add(class_[7], constraint[17]);
        containsAssoc.add(class_[7], constraint[16]);
        containsAssoc.add(class_[7], constraint[15]);
        containsAssoc.add(class_[7], attribute[14]);
        containsAssoc.add(operation[0], parameter[0]);
        containsAssoc.add(class_[6], tag[23]);
        containsAssoc.add(class_[6], constraint[14]);
        containsAssoc.add(class_[6], constraint[13]);
        containsAssoc.add(class_[6], constraint[12]);
        containsAssoc.add(class_[6], constraint[11]);
        containsAssoc.add(class_[6], attribute[13]);
        containsAssoc.add(class_[6], attribute[12]);
        containsAssoc.add(class_[6], attribute[11]);
        containsAssoc.add(class_[6], attribute[10]);
        containsAssoc.add(class_[6], operation[0]);
        containsAssoc.add(class_[5], tag[22]);
        containsAssoc.add(class_[5], tag[21]);
        containsAssoc.add(class_[5], constraint[10]);
        containsAssoc.add(class_[5], constraint[9]);
        containsAssoc.add(class_[4], tag[20]);
        containsAssoc.add(class_[4], constraint[8]);
        containsAssoc.add(class_[4], constraint[7]);
        containsAssoc.add(class_[4], constraint[6]);
        containsAssoc.add(class_[4], constraint[5]);
        containsAssoc.add(class_[4], constraint[4]);
        containsAssoc.add(class_[4], attribute[9]);
        containsAssoc.add(class_[4], attribute[8]);
        containsAssoc.add(class_[4], reference[2]);
        containsAssoc.add(class_[3], tag[19]);
        containsAssoc.add(class_[3], attribute[7]);
        containsAssoc.add(class_[3], attribute[6]);
    }

    private void generalizesAssocInit()
    {
        Generalizes generalizesAssoc = getModelPackage().getGeneralizes();

        generalizesAssoc.add(class_[27], class_[0]);
        generalizesAssoc.add(class_[27], class_[2]);
        generalizesAssoc.add(class_[27], class_[4]);
        generalizesAssoc.add(class_[27], class_[14]);
        generalizesAssoc.add(class_[27], class_[24]);
        generalizesAssoc.add(class_[27], class_[26]);
        generalizesAssoc.add(class_[14], class_[10]);
        generalizesAssoc.add(class_[26], class_[10]);
        generalizesAssoc.add(class_[26], class_[25]);
        generalizesAssoc.add(class_[25], class_[5]);
        generalizesAssoc.add(class_[25], class_[23]);
        generalizesAssoc.add(class_[24], class_[1]);
        generalizesAssoc.add(class_[24], class_[3]);
        generalizesAssoc.add(class_[24], class_[6]);
        generalizesAssoc.add(class_[14], class_[13]);
        generalizesAssoc.add(class_[24], class_[13]);
        generalizesAssoc.add(class_[21], class_[15]);
        generalizesAssoc.add(class_[24], class_[15]);
        generalizesAssoc.add(class_[24], class_[16]);
        generalizesAssoc.add(class_[21], class_[18]);
        generalizesAssoc.add(class_[24], class_[18]);
        generalizesAssoc.add(class_[23], class_[7]);
        generalizesAssoc.add(class_[23], class_[21]);
        generalizesAssoc.add(class_[23], class_[22]);
        generalizesAssoc.add(class_[21], class_[17]);
        generalizesAssoc.add(class_[21], class_[19]);
        generalizesAssoc.add(class_[21], class_[20]);
        generalizesAssoc.add(class_[13], class_[11]);
        generalizesAssoc.add(class_[13], class_[12]);
        generalizesAssoc.add(class_[10], class_[8]);
        generalizesAssoc.add(class_[10], class_[9]);
    }

    private void aliasesAssocInit()
    {
        Aliases aliasesAssoc = getModelPackage().getAliases();

        aliasesAssoc.add(import_[0], package_[2]);
    }

    private void constrainsAssocInit()
    {
        Constrains constrainsAssoc = getModelPackage().getConstrains();

        constrainsAssoc.add(constraint[57], class_[27]);
        constrainsAssoc.add(constraint[56], class_[27]);
        constrainsAssoc.add(constraint[55], class_[27]);
        constrainsAssoc.add(constraint[54], class_[27]);
        constrainsAssoc.add(constraint[53], class_[26]);
        constrainsAssoc.add(constraint[52], class_[25]);
        constrainsAssoc.add(constraint[51], class_[25]);
        constrainsAssoc.add(constraint[50], class_[25]);
        constrainsAssoc.add(constraint[49], class_[25]);
        constrainsAssoc.add(constraint[48], class_[25]);
        constrainsAssoc.add(constraint[47], class_[25]);
        constrainsAssoc.add(constraint[46], class_[25]);
        constrainsAssoc.add(constraint[45], class_[24]);
        constrainsAssoc.add(constraint[44], class_[24]);
        constrainsAssoc.add(constraint[1], class_[1]);
        constrainsAssoc.add(constraint[0], class_[1]);
        constrainsAssoc.add(constraint[43], class_[22]);
        constrainsAssoc.add(constraint[42], class_[22]);
        constrainsAssoc.add(constraint[41], package_[1]);
        constrainsAssoc.add(constraint[40], package_[1]);
        constrainsAssoc.add(constraint[39], package_[1]);
        constrainsAssoc.add(constraint[38], package_[1]);
        constrainsAssoc.add(constraint[37], class_[21]);
        constrainsAssoc.add(constraint[36], class_[21]);
        constrainsAssoc.add(constraint[35], class_[21]);
        constrainsAssoc.add(constraint[34], class_[17]);
        constrainsAssoc.add(constraint[33], class_[16]);
        constrainsAssoc.add(constraint[32], class_[11]);
        constrainsAssoc.add(constraint[31], class_[11]);
        constrainsAssoc.add(constraint[30], class_[11]);
        constrainsAssoc.add(constraint[29], class_[11]);
        constrainsAssoc.add(constraint[28], class_[11]);
        constrainsAssoc.add(constraint[27], class_[11]);
        constrainsAssoc.add(constraint[26], class_[11]);
        constrainsAssoc.add(constraint[25], class_[9]);
        constrainsAssoc.add(constraint[24], class_[9]);
        constrainsAssoc.add(constraint[23], class_[9]);
        constrainsAssoc.add(constraint[22], class_[8]);
        constrainsAssoc.add(constraint[21], class_[8]);
        constrainsAssoc.add(constraint[20], class_[7]);
        constrainsAssoc.add(constraint[19], class_[7]);
        constrainsAssoc.add(constraint[18], class_[7]);
        constrainsAssoc.add(constraint[17], class_[7]);
        constrainsAssoc.add(constraint[16], class_[7]);
        constrainsAssoc.add(constraint[15], class_[7]);
        constrainsAssoc.add(constraint[14], class_[6]);
        constrainsAssoc.add(constraint[13], class_[6]);
        constrainsAssoc.add(constraint[12], class_[6]);
        constrainsAssoc.add(constraint[11], class_[6]);
        constrainsAssoc.add(constraint[10], class_[5]);
        constrainsAssoc.add(constraint[9], class_[5]);
        constrainsAssoc.add(constraint[8], class_[4]);
        constrainsAssoc.add(constraint[7], class_[4]);
        constrainsAssoc.add(constraint[6], class_[4]);
        constrainsAssoc.add(constraint[5], class_[4]);
        constrainsAssoc.add(constraint[4], class_[4]);
        constrainsAssoc.add(constraint[3], class_[2]);
        constrainsAssoc.add(constraint[2], class_[2]);
    }

    private void canRaiseAssocInit()
    {
        CanRaise canRaiseAssoc = getModelPackage().getCanRaise();

        canRaiseAssoc.add(operation[7], exception[1]);
        canRaiseAssoc.add(operation[6], exception[0]);
        canRaiseAssoc.add(operation[2], exception[1]);
    }

    private void exposesAssocInit()
    {
        Exposes exposesAssoc = getModelPackage().getExposes();

        exposesAssoc.add(reference[11], associationEnd[17]);
        exposesAssoc.add(reference[10], associationEnd[14]);
        exposesAssoc.add(reference[9], associationEnd[8]);
        exposesAssoc.add(reference[8], associationEnd[15]);
        exposesAssoc.add(reference[7], associationEnd[12]);
        exposesAssoc.add(reference[6], associationEnd[0]);
        exposesAssoc.add(reference[0], associationEnd[18]);
        exposesAssoc.add(reference[5], associationEnd[5]);
        exposesAssoc.add(reference[4], associationEnd[3]);
        exposesAssoc.add(reference[3], associationEnd[7]);
        exposesAssoc.add(reference[2], associationEnd[11]);
        exposesAssoc.add(reference[1], associationEnd[9]);
    }

    private void refersToAssocInit()
    {
        RefersTo refersToAssoc = getModelPackage().getRefersTo();

        refersToAssoc.add(reference[11], associationEnd[16]);
        refersToAssoc.add(reference[10], associationEnd[15]);
        refersToAssoc.add(reference[9], associationEnd[9]);
        refersToAssoc.add(reference[8], associationEnd[14]);
        refersToAssoc.add(reference[7], associationEnd[13]);
        refersToAssoc.add(reference[6], associationEnd[1]);
        refersToAssoc.add(reference[0], associationEnd[19]);
        refersToAssoc.add(reference[5], associationEnd[4]);
        refersToAssoc.add(reference[4], associationEnd[2]);
        refersToAssoc.add(reference[3], associationEnd[6]);
        refersToAssoc.add(reference[2], associationEnd[10]);
        refersToAssoc.add(reference[1], associationEnd[8]);
    }

    private void isOfTypeAssocInit()
    {
        IsOfType isOfTypeAssoc = getModelPackage().getIsOfType();

        isOfTypeAssoc.add(primitiveType[14], attribute[8]);
        isOfTypeAssoc.add(primitiveType[14], attribute[10]);
        isOfTypeAssoc.add(primitiveType[14], attribute[13]);
        isOfTypeAssoc.add(primitiveType[14], attribute[14]);
        isOfTypeAssoc.add(primitiveType[14], attribute[15]);
        isOfTypeAssoc.add(primitiveType[14], attribute[16]);
        isOfTypeAssoc.add(primitiveType[14], attribute[17]);
        isOfTypeAssoc.add(primitiveType[14], structureField[0]);
        isOfTypeAssoc.add(primitiveType[14], structureField[1]);
        isOfTypeAssoc.add(primitiveType[14], attribute[23]);
        isOfTypeAssoc.add(primitiveType[14], parameter[1]);
        isOfTypeAssoc.add(primitiveType[14], attribute[25]);
        isOfTypeAssoc.add(primitiveType[14], attribute[26]);
        isOfTypeAssoc.add(primitiveType[14], attribute[27]);
        isOfTypeAssoc.add(primitiveType[14], parameter[8]);
        isOfTypeAssoc.add(primitiveType[14], parameter[9]);
        isOfTypeAssoc.add(primitiveType[14], parameter[20]);
        isOfTypeAssoc.add(primitiveType[14], parameter[21]);
        isOfTypeAssoc.add(primitiveType[14], parameter[24]);
        isOfTypeAssoc.add(primitiveType[14], parameter[25]);
        isOfTypeAssoc.add(primitiveType[13], structureField[2]);
        isOfTypeAssoc.add(primitiveType[13], structureField[3]);
        isOfTypeAssoc.add(primitiveType[13], constant[0]);
        isOfTypeAssoc.add(primitiveType[9], attribute[0]);
        isOfTypeAssoc.add(primitiveType[9], attribute[1]);
        isOfTypeAssoc.add(primitiveType[9], attribute[2]);
        isOfTypeAssoc.add(primitiveType[9], attribute[4]);
        isOfTypeAssoc.add(primitiveType[9], attribute[5]);
        isOfTypeAssoc.add(primitiveType[9], attribute[22]);
        isOfTypeAssoc.add(primitiveType[9], parameter[4]);
        isOfTypeAssoc.add(primitiveType[9], parameter[7]);
        isOfTypeAssoc.add(primitiveType[9], parameter[12]);
        isOfTypeAssoc.add(primitiveType[9], parameter[14]);
        isOfTypeAssoc.add(primitiveType[9], parameter[16]);
        isOfTypeAssoc.add(primitiveType[9], parameter[17]);
        isOfTypeAssoc.add(primitiveType[9], parameter[18]);
        isOfTypeAssoc.add(primitiveType[9], parameter[22]);
        isOfTypeAssoc.add(primitiveType[9], parameter[26]);
        isOfTypeAssoc.add(primitiveType[9], constant[1]);
        isOfTypeAssoc.add(primitiveType[9], constant[2]);
        isOfTypeAssoc.add(primitiveType[9], constant[3]);
        isOfTypeAssoc.add(primitiveType[9], constant[4]);
        isOfTypeAssoc.add(primitiveType[9], constant[5]);
        isOfTypeAssoc.add(primitiveType[9], constant[6]);
        isOfTypeAssoc.add(primitiveType[9], constant[7]);
        isOfTypeAssoc.add(primitiveType[9], constant[8]);
        isOfTypeAssoc.add(primitiveType[9], constant[9]);
        isOfTypeAssoc.add(primitiveType[9], constant[10]);
        isOfTypeAssoc.add(primitiveType[9], constant[11]);
        isOfTypeAssoc.add(primitiveType[9], constant[12]);
        isOfTypeAssoc.add(primitiveType[9], attribute[28]);
        isOfTypeAssoc.add(primitiveType[9], attribute[29]);
        isOfTypeAssoc.add(primitiveType[9], attribute[30]);
        isOfTypeAssoc.add(class_[27], associationEnd[8]);
        isOfTypeAssoc.add(class_[27], associationEnd[14]);
        isOfTypeAssoc.add(class_[27], associationEnd[16]);
        isOfTypeAssoc.add(class_[27], associationEnd[17]);
        isOfTypeAssoc.add(class_[27], associationEnd[19]);
        isOfTypeAssoc.add(class_[27], reference[0]);
        isOfTypeAssoc.add(class_[27], reference[1]);
        isOfTypeAssoc.add(class_[27], parameter[3]);
        isOfTypeAssoc.add(class_[27], parameter[5]);
        isOfTypeAssoc.add(class_[27], parameter[11]);
        isOfTypeAssoc.add(class_[27], parameter[13]);
        isOfTypeAssoc.add(class_[27], parameter[15]);
        isOfTypeAssoc.add(class_[27], reference[8]);
        isOfTypeAssoc.add(class_[27], parameter[19]);
        isOfTypeAssoc.add(class_[27], parameter[23]);
        isOfTypeAssoc.add(class_[27], parameter[27]);
        isOfTypeAssoc.add(class_[27], reference[11]);
        isOfTypeAssoc.add(enumerationType[4], attribute[9]);
        isOfTypeAssoc.add(enumerationType[4], attribute[19]);
        isOfTypeAssoc.add(enumerationType[4], attribute[24]);
        isOfTypeAssoc.add(class_[26], associationEnd[10]);
        isOfTypeAssoc.add(class_[26], associationEnd[15]);
        isOfTypeAssoc.add(class_[26], reference[2]);
        isOfTypeAssoc.add(class_[26], reference[10]);
        isOfTypeAssoc.add(class_[25], associationEnd[12]);
        isOfTypeAssoc.add(class_[25], associationEnd[13]);
        isOfTypeAssoc.add(class_[25], parameter[6]);
        isOfTypeAssoc.add(class_[25], reference[7]);
        isOfTypeAssoc.add(class_[24], associationEnd[0]);
        isOfTypeAssoc.add(class_[23], associationEnd[1]);
        isOfTypeAssoc.add(class_[23], reference[6]);
        isOfTypeAssoc.add(class_[2], associationEnd[9]);
        isOfTypeAssoc.add(class_[2], reference[9]);
        isOfTypeAssoc.add(class_[0], associationEnd[18]);
        isOfTypeAssoc.add(class_[22], parameter[2]);
        isOfTypeAssoc.add(class_[22], parameter[10]);
        isOfTypeAssoc.add(structureType[0], attribute[6]);
        isOfTypeAssoc.add(structureType[0], attribute[11]);
        isOfTypeAssoc.add(structureType[0], attribute[18]);
        isOfTypeAssoc.add(structureType[0], attribute[21]);
        isOfTypeAssoc.add(enumerationType[3], attribute[20]);
        isOfTypeAssoc.add(class_[11], associationEnd[3]);
        isOfTypeAssoc.add(class_[11], associationEnd[5]);
        isOfTypeAssoc.add(class_[9], associationEnd[7]);
        isOfTypeAssoc.add(class_[8], associationEnd[6]);
        isOfTypeAssoc.add(class_[8], reference[3]);
        isOfTypeAssoc.add(enumerationType[2], attribute[12]);
        isOfTypeAssoc.add(class_[6], associationEnd[2]);
        isOfTypeAssoc.add(class_[6], associationEnd[4]);
        isOfTypeAssoc.add(class_[6], parameter[0]);
        isOfTypeAssoc.add(class_[6], reference[4]);
        isOfTypeAssoc.add(class_[6], reference[5]);
        isOfTypeAssoc.add(class_[4], associationEnd[11]);
        isOfTypeAssoc.add(enumerationType[1], attribute[7]);
        isOfTypeAssoc.add(enumerationType[0], attribute[3]);
    }

    @SuppressWarnings("unchecked")
    public void initMetamodel()
    {
        // Initialize Model Package
        org.eigenbase.enki.jmi.model.ModelPackage modelPackage = new org.eigenbase.enki.jmi.model.ModelPackage();
        setModelPackage(modelPackage);

        // Pass 1: Instances and attributes
        classInit();
        primitiveTypeInit();
        enumerationTypeInit();
        collectionTypeInit();
        structureTypeInit();
        structureFieldInit();
        aliasTypeInit();
        attributeInit();
        referenceInit();
        operationInit();
        exceptionInit();
        associationInit();
        associationEndInit();
        packageInit();
        importInit();
        parameterInit();
        constraintInit();
        constantInit();
        tagInit();

        // Pass 2: References (associations)
        attachesToAssocInit();
        containsAssocInit();
        generalizesAssocInit();
        aliasesAssocInit();
        constrainsAssocInit();
        canRaiseAssocInit();
        exposesAssocInit();
        refersToAssocInit();
        isOfTypeAssocInit();

        // Pass 3: Meta Objects
        setRefMetaObject(getModelPackage().getMofClass(), findMofClassByName("Class", true));
        setRefMetaObject(getModelPackage().getTypedElement(), findMofClassByName("TypedElement", true));
        setRefMetaObject(getModelPackage().getDataType(), findMofClassByName("DataType", true));
        setRefMetaObject(getModelPackage().getAliasType(), findMofClassByName("AliasType", true));
        setRefMetaObject(getModelPackage().getStructureType(), findMofClassByName("StructureType", true));
        setRefMetaObject(getModelPackage().getMofPackage(), findMofClassByName("Package", true));
        setRefMetaObject(getModelPackage().getImport(), findMofClassByName("Import", true));
        setRefMetaObject(getModelPackage().getTag(), findMofClassByName("Tag", true));
        setRefMetaObject(getModelPackage().getAssociationEnd(), findMofClassByName("AssociationEnd", true));
        setRefMetaObject(getModelPackage().getBehavioralFeature(), findMofClassByName("BehavioralFeature", true));
        setRefMetaObject(getModelPackage().getEnumerationType(), findMofClassByName("EnumerationType", true));
        setRefMetaObject(getModelPackage().getStructureField(), findMofClassByName("StructureField", true));
        setRefMetaObject(getModelPackage().getModelElement(), findMofClassByName("ModelElement", true));
        setRefMetaObject(getModelPackage().getPrimitiveType(), findMofClassByName("PrimitiveType", true));
        setRefMetaObject(getModelPackage().getFeature(), findMofClassByName("Feature", true));
        setRefMetaObject(getModelPackage().getAttribute(), findMofClassByName("Attribute", true));
        setRefMetaObject(getModelPackage().getNamespace(), findMofClassByName("Namespace", true));
        setRefMetaObject(getModelPackage().getConstraint(), findMofClassByName("Constraint", true));
        setRefMetaObject(getModelPackage().getClassifier(), findMofClassByName("Classifier", true));
        setRefMetaObject(getModelPackage().getAssociation(), findMofClassByName("Association", true));
        setRefMetaObject(getModelPackage().getReference(), findMofClassByName("Reference", true));
        setRefMetaObject(getModelPackage().getGeneralizableElement(), findMofClassByName("GeneralizableElement", true));
        setRefMetaObject(getModelPackage().getOperation(), findMofClassByName("Operation", true));
        setRefMetaObject(getModelPackage().getCollectionType(), findMofClassByName("CollectionType", true));
        setRefMetaObject(getModelPackage().getConstant(), findMofClassByName("Constant", true));
        setRefMetaObject(getModelPackage().getMofException(), findMofClassByName("Exception", true));
        setRefMetaObject(getModelPackage().getParameter(), findMofClassByName("Parameter", true));
        setRefMetaObject(getModelPackage().getStructuralFeature(), findMofClassByName("StructuralFeature", true));
        setRefMetaObject(getModelPackage().getConstrains(), findAssociationByName("Constrains", true));
        setRefMetaObject(getModelPackage().getAliases(), findAssociationByName("Aliases", true));
        setRefMetaObject(getModelPackage().getDependsOn(), findAssociationByName("DependsOn", true));
        setRefMetaObject(getModelPackage().getAttachesTo(), findAssociationByName("AttachesTo", true));
        setRefMetaObject(getModelPackage().getContains(), findAssociationByName("Contains", true));
        setRefMetaObject(getModelPackage().getRefersTo(), findAssociationByName("RefersTo", true));
        setRefMetaObject(getModelPackage().getExposes(), findAssociationByName("Exposes", true));
        setRefMetaObject(getModelPackage().getIsOfType(), findAssociationByName("IsOfType", true));
        setRefMetaObject(getModelPackage().getCanRaise(), findAssociationByName("CanRaise", true));
        setRefMetaObject(getModelPackage().getGeneralizes(), findAssociationByName("Generalizes", true));
    }
}

// End Initializer.java
