/*
//  $Id$
//  Enki generates and implements the JMI and MDR APIs for MOF metamodels.
//  Copyright (C) 2007-2007 The Eigenbase Project
//  Copyright (C) 2007-2007 Disruptive Tech
//  Copyright (C) 2007-2007 LucidEra, Inc.
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
        this.namespaceClass = new NamespaceClass(this);
        this.generalizableElementClass = new GeneralizableElementClass(this);
        this.typedElementClass = new TypedElementClass(this);
        this.classifierClass = new ClassifierClass(this);
        this.mofClassClass = new MofClassClass(this);
        this.dataTypeClass = new DataTypeClass(this);
        this.primitiveTypeClass = new PrimitiveTypeClass(this);
        this.enumerationTypeClass = new EnumerationTypeClass(this);
        this.collectionTypeClass = new CollectionTypeClass(this);
        this.structureTypeClass = new StructureTypeClass(this);
        this.structureFieldClass = new StructureFieldClass(this);
        this.aliasTypeClass = new AliasTypeClass(this);
        this.featureClass = new FeatureClass(this);
        this.structuralFeatureClass = new StructuralFeatureClass(this);
        this.attributeClass = new AttributeClass(this);
        this.referenceClass = new ReferenceClass(this);
        this.behavioralFeatureClass = new BehavioralFeatureClass(this);
        this.operationClass = new OperationClass(this);
        this.mofExceptionClass = new MofExceptionClass(this);
        this.associationClass = new AssociationClass(this);
        this.associationEndClass = new AssociationEndClass(this);
        this.mofPackageClass = new MofPackageClass(this);
        this.importClass = new ImportClass(this);
        this.parameterClass = new ParameterClass(this);
        this.constraintClass = new ConstraintClass(this);
        this.constantClass = new ConstantClass(this);
        this.tagClass = new TagClass(this);

        this.attachesTo = new AttachesTo(this);
        this.dependsOn = new DependsOn(this);
        this.contains = new Contains(this);
        this.generalizes = new Generalizes(this);
        this.aliases = new Aliases(this);
        this.constrains = new Constrains(this);
        this.canRaise = new CanRaise(this);
        this.exposes = new Exposes(this);
        this.refersTo = new RefersTo(this);
        this.isOfType = new IsOfType(this);
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
}

// End ModelPackage.java
