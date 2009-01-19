/*
//  $Id$
//  Enki generates and implements the JMI and MDR APIs for MOF metamodels.
//  Copyright (C) 2007-2009 The Eigenbase Project
//  Copyright (C) 2007-2009 Disruptive Tech
//  Copyright (C) 2007-2009 LucidEra, Inc.
// 
//  This library is free software; you can redistribute it and/or modify it
//  under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation; either version 2.1 of the License, or (at
//  your option) any later version.
// 
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
//  USA
*/

package org.eigenbase.enki.jmi.model;

import org.eigenbase.enki.jmi.impl.*;

/**
 * Implements MOF's Model package interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class ModelPackage
    extends RefPackageBase
    implements javax.jmi.model.ModelPackage
{
    // Class Proxies
    private final javax.jmi.model.ModelElementClass modelElementClass;
    private final javax.jmi.model.NamespaceClass namespaceClass;
    private final javax.jmi.model.GeneralizableElementClass generalizableElementClass;
    private final javax.jmi.model.TypedElementClass typedElementClass;
    private final javax.jmi.model.ClassifierClass classifierClass;
    private final javax.jmi.model.MofClassClass mofClassClass;
    private final javax.jmi.model.DataTypeClass dataTypeClass;
    private final javax.jmi.model.PrimitiveTypeClass primitiveTypeClass;
    private final javax.jmi.model.EnumerationTypeClass enumerationTypeClass;
    private final javax.jmi.model.CollectionTypeClass collectionTypeClass;
    private final javax.jmi.model.StructureTypeClass structureTypeClass;
    private final javax.jmi.model.StructureFieldClass structureFieldClass;
    private final javax.jmi.model.AliasTypeClass aliasTypeClass;
    private final javax.jmi.model.FeatureClass featureClass;
    private final javax.jmi.model.StructuralFeatureClass structuralFeatureClass;
    private final javax.jmi.model.AttributeClass attributeClass;
    private final javax.jmi.model.ReferenceClass referenceClass;
    private final javax.jmi.model.BehavioralFeatureClass behavioralFeatureClass;
    private final javax.jmi.model.OperationClass operationClass;
    private final javax.jmi.model.MofExceptionClass mofExceptionClass;
    private final javax.jmi.model.AssociationClass associationClass;
    private final javax.jmi.model.AssociationEndClass associationEndClass;
    private final javax.jmi.model.MofPackageClass mofPackageClass;
    private final javax.jmi.model.ImportClass importClass;
    private final javax.jmi.model.ParameterClass parameterClass;
    private final javax.jmi.model.ConstraintClass constraintClass;
    private final javax.jmi.model.ConstantClass constantClass;
    private final javax.jmi.model.TagClass tagClass;

    // Associations
    private final javax.jmi.model.AttachesTo attachesTo;
    private final javax.jmi.model.DependsOn dependsOn;
    private final javax.jmi.model.Contains contains;
    private final javax.jmi.model.Generalizes generalizes;
    private final javax.jmi.model.Aliases aliases;
    private final javax.jmi.model.Constrains constrains;
    private final javax.jmi.model.CanRaise canRaise;
    private final javax.jmi.model.Exposes exposes;
    private final javax.jmi.model.RefersTo refersTo;
    private final javax.jmi.model.IsOfType isOfType;

    public ModelPackage()
    {
        super(null);

        this.modelElementClass = new ModelElementClass(this);
        super.addClass("ModelElement", this.modelElementClass);
        this.namespaceClass = new NamespaceClass(this);
        super.addClass("Namespace", this.namespaceClass);
        this.generalizableElementClass = new GeneralizableElementClass(this);
        super.addClass("GeneralizableElement", this.generalizableElementClass);
        this.typedElementClass = new TypedElementClass(this);
        super.addClass("TypedElement", this.typedElementClass);
        this.classifierClass = new ClassifierClass(this);
        super.addClass("Classifier", this.classifierClass);
        this.mofClassClass = new MofClassClass(this);
        super.addClass("Class", this.mofClassClass);
        this.dataTypeClass = new DataTypeClass(this);
        super.addClass("DataType", this.dataTypeClass);
        this.primitiveTypeClass = new PrimitiveTypeClass(this);
        super.addClass("PrimitiveType", this.primitiveTypeClass);
        this.enumerationTypeClass = new EnumerationTypeClass(this);
        super.addClass("EnumerationType", this.enumerationTypeClass);
        this.collectionTypeClass = new CollectionTypeClass(this);
        super.addClass("CollectionType", this.collectionTypeClass);
        this.structureTypeClass = new StructureTypeClass(this);
        super.addClass("StructureType", this.structureTypeClass);
        this.structureFieldClass = new StructureFieldClass(this);
        super.addClass("StructureField", this.structureFieldClass);
        this.aliasTypeClass = new AliasTypeClass(this);
        super.addClass("AliasType", this.aliasTypeClass);
        this.featureClass = new FeatureClass(this);
        super.addClass("Feature", this.featureClass);
        this.structuralFeatureClass = new StructuralFeatureClass(this);
        super.addClass("StructuralFeature", this.structuralFeatureClass);
        this.attributeClass = new AttributeClass(this);
        super.addClass("Attribute", this.attributeClass);
        this.referenceClass = new ReferenceClass(this);
        super.addClass("Reference", this.referenceClass);
        this.behavioralFeatureClass = new BehavioralFeatureClass(this);
        super.addClass("BehavioralFeature", this.behavioralFeatureClass);
        this.operationClass = new OperationClass(this);
        super.addClass("Operation", this.operationClass);
        this.mofExceptionClass = new MofExceptionClass(this);
        super.addClass("Exception", this.mofExceptionClass);
        this.associationClass = new AssociationClass(this);
        super.addClass("Association", this.associationClass);
        this.associationEndClass = new AssociationEndClass(this);
        super.addClass("AssociationEnd", this.associationEndClass);
        this.mofPackageClass = new MofPackageClass(this);
        super.addClass("Package", this.mofPackageClass);
        this.importClass = new ImportClass(this);
        super.addClass("Import", this.importClass);
        this.parameterClass = new ParameterClass(this);
        super.addClass("Parameter", this.parameterClass);
        this.constraintClass = new ConstraintClass(this);
        super.addClass("Constraint", this.constraintClass);
        this.constantClass = new ConstantClass(this);
        super.addClass("Constant", this.constantClass);
        this.tagClass = new TagClass(this);
        super.addClass("Tag", this.tagClass);

        this.attachesTo = new AttachesTo(this);
        super.addAssociation("AttachesTo", this.attachesTo);
        this.dependsOn = new DependsOn(this);
        super.addAssociation("DependsOn", this.dependsOn);
        this.contains = new Contains(this);
        super.addAssociation("Contains", this.contains);
        this.generalizes = new Generalizes(this);
        super.addAssociation("Generalizes", this.generalizes);
        this.aliases = new Aliases(this);
        super.addAssociation("Aliases", this.aliases);
        this.constrains = new Constrains(this);
        super.addAssociation("Constrains", this.constrains);
        this.canRaise = new CanRaise(this);
        super.addAssociation("CanRaise", this.canRaise);
        this.exposes = new Exposes(this);
        super.addAssociation("Exposes", this.exposes);
        this.refersTo = new RefersTo(this);
        super.addAssociation("RefersTo", this.refersTo);
        this.isOfType = new IsOfType(this);
        super.addAssociation("IsOfType", this.isOfType);
    }

    // Class Proxy Accessors

    public javax.jmi.model.ModelElementClass getModelElement()
    {
        return modelElementClass;
    }

    public javax.jmi.model.NamespaceClass getNamespace()
    {
        return namespaceClass;
    }

    public javax.jmi.model.GeneralizableElementClass getGeneralizableElement()
    {
        return generalizableElementClass;
    }

    public javax.jmi.model.TypedElementClass getTypedElement()
    {
        return typedElementClass;
    }

    public javax.jmi.model.ClassifierClass getClassifier()
    {
        return classifierClass;
    }

    public javax.jmi.model.MofClassClass getMofClass()
    {
        return mofClassClass;
    }

    public javax.jmi.model.DataTypeClass getDataType()
    {
        return dataTypeClass;
    }

    public javax.jmi.model.PrimitiveTypeClass getPrimitiveType()
    {
        return primitiveTypeClass;
    }

    public javax.jmi.model.EnumerationTypeClass getEnumerationType()
    {
        return enumerationTypeClass;
    }

    public javax.jmi.model.CollectionTypeClass getCollectionType()
    {
        return collectionTypeClass;
    }

    public javax.jmi.model.StructureTypeClass getStructureType()
    {
        return structureTypeClass;
    }

    public javax.jmi.model.StructureFieldClass getStructureField()
    {
        return structureFieldClass;
    }

    public javax.jmi.model.AliasTypeClass getAliasType()
    {
        return aliasTypeClass;
    }

    public javax.jmi.model.FeatureClass getFeature()
    {
        return featureClass;
    }

    public javax.jmi.model.StructuralFeatureClass getStructuralFeature()
    {
        return structuralFeatureClass;
    }

    public javax.jmi.model.AttributeClass getAttribute()
    {
        return attributeClass;
    }

    public javax.jmi.model.ReferenceClass getReference()
    {
        return referenceClass;
    }

    public javax.jmi.model.BehavioralFeatureClass getBehavioralFeature()
    {
        return behavioralFeatureClass;
    }

    public javax.jmi.model.OperationClass getOperation()
    {
        return operationClass;
    }

    public javax.jmi.model.MofExceptionClass getMofException()
    {
        return mofExceptionClass;
    }

    public javax.jmi.model.AssociationClass getAssociation()
    {
        return associationClass;
    }

    public javax.jmi.model.AssociationEndClass getAssociationEnd()
    {
        return associationEndClass;
    }

    public javax.jmi.model.MofPackageClass getMofPackage()
    {
        return mofPackageClass;
    }

    public javax.jmi.model.ImportClass getImport()
    {
        return importClass;
    }

    public javax.jmi.model.ParameterClass getParameter()
    {
        return parameterClass;
    }

    public javax.jmi.model.ConstraintClass getConstraint()
    {
        return constraintClass;
    }

    public javax.jmi.model.ConstantClass getConstant()
    {
        return constantClass;
    }

    public javax.jmi.model.TagClass getTag()
    {
        return tagClass;
    }


    // Association Accessors

    public javax.jmi.model.AttachesTo getAttachesTo()
    {
        return attachesTo;
    }

    public javax.jmi.model.DependsOn getDependsOn()
    {
        return dependsOn;
    }

    public javax.jmi.model.Contains getContains()
    {
        return contains;
    }

    public javax.jmi.model.Generalizes getGeneralizes()
    {
        return generalizes;
    }

    public javax.jmi.model.Aliases getAliases()
    {
        return aliases;
    }

    public javax.jmi.model.Constrains getConstrains()
    {
        return constrains;
    }

    public javax.jmi.model.CanRaise getCanRaise()
    {
        return canRaise;
    }

    public javax.jmi.model.Exposes getExposes()
    {
        return exposes;
    }

    public javax.jmi.model.RefersTo getRefersTo()
    {
        return refersTo;
    }

    public javax.jmi.model.IsOfType getIsOfType()
    {
        return isOfType;
    }


    // StructureType factory methods

    public javax.jmi.model.MultiplicityType createMultiplicityType(
        int lower,
        int upper,
        boolean isOrdered,
        boolean isUnique)
    {
        return new MultiplicityType(
            lower,
            upper,
            isOrdered,
            isUnique);
    }

    protected void checkConstraints(java.util.List<javax.jmi.reflect.JmiException> errors, boolean deepVerify)
    {
    }
}

// End ModelPackage.java
