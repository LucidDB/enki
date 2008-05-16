/*
//  $Id$
//  Enki generates and implements the JMI and MDR APIs for MOF metamodels.
//  Copyright (C) 2007-2008 The Eigenbase Project
//  Copyright (C) 2007-2008 Disruptive Tech
//  Copyright (C) 2007-2008 LucidEra, Inc.
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

import java.util.*;
import javax.jmi.reflect.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * Implements MOF's CollectionType interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class CollectionType
    extends RefObjectBase
    implements javax.jmi.model.CollectionType
{
    // Attribute Fields
    private String name;
    private String annotation;
    private Boolean isRoot;
    private Boolean isLeaf;
    private Boolean isAbstract;
    private javax.jmi.model.VisibilityKind visibility;
    private javax.jmi.model.MultiplicityType multiplicity;

    // Reference Fields
    private javax.jmi.model.DependsOn provider;
    private javax.jmi.model.Contains container;
    private javax.jmi.model.Constrains constraint;
    private javax.jmi.model.Contains containedElement;
    private javax.jmi.model.Generalizes supertype;
    private javax.jmi.model.IsOfType type;

    CollectionType(
        RefClass refClass)
    {
        super(refClass);

        this.provider = (javax.jmi.model.DependsOn)refImmediatePackage().refAssociation("DependsOn");
        this.container = (javax.jmi.model.Contains)refImmediatePackage().refAssociation("Contains");
        this.constraint = (javax.jmi.model.Constrains)refImmediatePackage().refAssociation("Constrains");
        this.containedElement = (javax.jmi.model.Contains)refImmediatePackage().refAssociation("Contains");
        this.supertype = (javax.jmi.model.Generalizes)refImmediatePackage().refAssociation("Generalizes");
        this.type = (javax.jmi.model.IsOfType)refImmediatePackage().refAssociation("IsOfType");
    }

    CollectionType(
        RefClass refClass,
        String name,
        String annotation,
        boolean isRoot,
        boolean isLeaf,
        boolean isAbstract,
        javax.jmi.model.VisibilityKind visibility,
        javax.jmi.model.MultiplicityType multiplicity)
    {
        this(refClass);

        this.name = name;
        this.annotation = annotation;
        this.isRoot = isRoot;
        this.isLeaf = isLeaf;
        this.isAbstract = isAbstract;
        this.visibility = visibility;
        this.multiplicity = multiplicity;
    }

    // Attribute Methods

    public String getName()
    {
        return name;
    }

    public void setName(String newValue)
    {
        this.name = newValue;
    }

    @SuppressWarnings("unchecked")
    public List/*<String>*/ getQualifiedName()
    {
        return super.getQualifiedName();
    }

    public String getAnnotation()
    {
        return annotation;
    }

    public void setAnnotation(String newValue)
    {
        this.annotation = newValue;
    }

    public boolean isRoot()
    {
        return isRoot;
    }

    public void setRoot(boolean newValue)
    {
        this.isRoot = newValue;
    }

    public boolean isLeaf()
    {
        return isLeaf;
    }

    public void setLeaf(boolean newValue)
    {
        this.isLeaf = newValue;
    }

    public boolean isAbstract()
    {
        return isAbstract;
    }

    public void setAbstract(boolean newValue)
    {
        this.isAbstract = newValue;
    }

    public javax.jmi.model.VisibilityKind getVisibility()
    {
        return visibility;
    }

    public void setVisibility(javax.jmi.model.VisibilityKind newValue)
    {
        this.visibility = newValue;
    }

    public javax.jmi.model.MultiplicityType getMultiplicity()
    {
        return multiplicity;
    }

    public void setMultiplicity(javax.jmi.model.MultiplicityType newValue)
    {
        this.multiplicity = newValue;
    }

    // Reference Methods

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.ModelElement>*/ getRequiredElements()
    {
        return provider.getProvider(this);
    }

    public javax.jmi.model.Namespace getContainer()
    {
        return container.getContainer(this);
    }

    public void setContainer(javax.jmi.model.Namespace newValue)
    {
        container.add(newValue, this);
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.Constraint>*/ getConstraints()
    {
        return constraint.getConstraint(this);
    }

    @SuppressWarnings("unchecked")
    public List/*<javax.jmi.model.ModelElement>*/ getContents()
    {
        return containedElement.getContainedElement(this);
    }

    @SuppressWarnings("unchecked")
    public List/*<javax.jmi.model.GeneralizableElement>*/ getSupertypes()
    {
        return supertype.getSupertype(this);
    }

    public javax.jmi.model.Classifier getType()
    {
        return type.getType(this);
    }

    public void setType(javax.jmi.model.Classifier newValue)
    {
        type.add(newValue, this);
    }

    // Operation Methods

    @SuppressWarnings("unchecked")
    public java.util.Collection/*<javax.jmi.model.ModelElement>*/ findRequiredElements(
        java.util.Collection/*<String>*/ kinds,
        boolean recursive)
    {
        return super.findRequiredElements(
            kinds,
            recursive);
    }

    public boolean isRequiredBecause(
        javax.jmi.model.ModelElement otherElement,
        String[] reason)
    {
        return super.isRequiredBecause(
            otherElement,
            reason);
    }

    public boolean isFrozen()
    {
        return super.isFrozen();
    }

    public boolean isVisible(
        javax.jmi.model.ModelElement otherElement)
    {
        return super.isVisible(
            otherElement);
    }

    public javax.jmi.model.ModelElement lookupElement(
        String name)
    throws javax.jmi.model.NameNotFoundException
    {
        return super.lookupElement(
            name);
    }

    @SuppressWarnings("unchecked")
    public javax.jmi.model.ModelElement resolveQualifiedName(
        java.util.List/*<String>*/ qualifiedName)
    throws javax.jmi.model.NameNotResolvedException
    {
        return super.resolveQualifiedName(
            qualifiedName);
    }

    @SuppressWarnings("unchecked")
    public java.util.List/*<javax.jmi.model.ModelElement>*/ findElementsByType(
        javax.jmi.model.MofClass ofType,
        boolean includeSubtypes)
    {
        return super.findElementsByType(
            ofType,
            includeSubtypes);
    }

    public boolean nameIsValid(
        String proposedName)
    {
        return super.nameIsValid(
            proposedName);
    }

    @SuppressWarnings("unchecked")
    public java.util.List/*<javax.jmi.model.GeneralizableElement>*/ allSupertypes()
    {
        return super.allSupertypes();
    }

    public javax.jmi.model.ModelElement lookupElementExtended(
        String name)
    throws javax.jmi.model.NameNotFoundException
    {
        return super.lookupElementExtended(
            name);
    }

    @SuppressWarnings("unchecked")
    public java.util.List/*<javax.jmi.model.ModelElement>*/ findElementsByTypeExtended(
        javax.jmi.model.MofClass ofType,
        boolean includeSubtypes)
    {
        return super.findElementsByTypeExtended(
            ofType,
            includeSubtypes);
    }

    protected void checkConstraints(List<javax.jmi.reflect.JmiException> errors, boolean deepVerify)
    {
        if (name == null) {
            javax.jmi.model.Attribute attrib = findAttribute("name");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (getQualifiedName().size() < 1) {
            javax.jmi.model.Attribute attrib = findAttribute("qualifiedName");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (annotation == null) {
            javax.jmi.model.Attribute attrib = findAttribute("annotation");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (isRoot == null) {
            javax.jmi.model.Attribute attrib = findAttribute("isRoot");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (isLeaf == null) {
            javax.jmi.model.Attribute attrib = findAttribute("isLeaf");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (isAbstract == null) {
            javax.jmi.model.Attribute attrib = findAttribute("isAbstract");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (visibility == null) {
            javax.jmi.model.Attribute attrib = findAttribute("visibility");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (multiplicity == null) {
            javax.jmi.model.Attribute attrib = findAttribute("multiplicity");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (getType() == null) {
            javax.jmi.model.AssociationEnd exposedEnd = findAssociationEnd("IsOfType", "typedElements");
            javax.jmi.model.AssociationEnd referencedEnd = findAssociationEnd("IsOfType", "type");
            errors.add(org.eigenbase.enki.jmi.impl.RefAssociationBase.makeWrongSizeException(exposedEnd, referencedEnd, this));
        }
    }
}

// End CollectionType.java
