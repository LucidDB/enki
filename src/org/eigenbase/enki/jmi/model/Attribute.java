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

import java.util.*;
import javax.jmi.reflect.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * Implements MOF's Attribute interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class Attribute
    extends RefObjectBase
    implements javax.jmi.model.Attribute
{
    // Attribute Fields
    private String name;
    private String annotation;
    private javax.jmi.model.ScopeKind scope;
    private javax.jmi.model.VisibilityKind visibility;
    private javax.jmi.model.MultiplicityType multiplicity;
    private Boolean isChangeable;
    private Boolean isDerived;

    // Reference Fields
    private javax.jmi.model.DependsOn provider;
    private javax.jmi.model.Contains container;
    private javax.jmi.model.Constrains constraint;
    private javax.jmi.model.IsOfType type;

    // Association Fields
    private javax.jmi.model.AttachesTo attachesTo_Tag;
    private javax.jmi.model.DependsOn dependsOn_Dependent;

    Attribute(
        RefClass refClass)
    {
        super(refClass);

        this.provider = (javax.jmi.model.DependsOn)refImmediatePackage().refAssociation("DependsOn");
        this.container = (javax.jmi.model.Contains)refImmediatePackage().refAssociation("Contains");
        this.constraint = (javax.jmi.model.Constrains)refImmediatePackage().refAssociation("Constrains");
        this.type = (javax.jmi.model.IsOfType)refImmediatePackage().refAssociation("IsOfType");

        this.attachesTo_Tag = (javax.jmi.model.AttachesTo)refImmediatePackage().refAssociation("AttachesTo");
        this.dependsOn_Dependent = (javax.jmi.model.DependsOn)refImmediatePackage().refAssociation("DependsOn");
    }

    Attribute(
        RefClass refClass,
        String name,
        String annotation,
        javax.jmi.model.ScopeKind scope,
        javax.jmi.model.VisibilityKind visibility,
        javax.jmi.model.MultiplicityType multiplicity,
        boolean isChangeable,
        boolean isDerived)
    {
        this(refClass);

        this.name = name;
        this.annotation = annotation;
        this.scope = scope;
        this.visibility = visibility;
        this.multiplicity = multiplicity;
        this.isChangeable = isChangeable;
        this.isDerived = isDerived;
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

    public javax.jmi.model.ScopeKind getScope()
    {
        return scope;
    }

    public void setScope(javax.jmi.model.ScopeKind newValue)
    {
        this.scope = newValue;
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

    public boolean isChangeable()
    {
        return isChangeable;
    }

    public void setChangeable(boolean newValue)
    {
        this.isChangeable = newValue;
    }

    public boolean isDerived()
    {
        return isDerived;
    }

    public void setDerived(boolean newValue)
    {
        this.isDerived = newValue;
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
        container.refQuery("containedElement", this).clear();
        if (newValue != null) {
            container.add(newValue, this);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.Constraint>*/ getConstraints()
    {
        return constraint.getConstraint(this);
    }

    public javax.jmi.model.Classifier getType()
    {
        return type.getType(this);
    }

    public void setType(javax.jmi.model.Classifier newValue)
    {
        type.refQuery("typedElements", this).clear();
        if (newValue != null) {
            type.add(newValue, this);
        }
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
        if (scope == null) {
            javax.jmi.model.Attribute attrib = findAttribute("scope");
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
        if (isChangeable == null) {
            javax.jmi.model.Attribute attrib = findAttribute("isChangeable");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (isDerived == null) {
            javax.jmi.model.Attribute attrib = findAttribute("isDerived");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (getType() == null) {
            javax.jmi.model.AssociationEnd exposedEnd = findAssociationEnd("IsOfType", "typedElements");
            javax.jmi.model.AssociationEnd referencedEnd = findAssociationEnd("IsOfType", "type");
            errors.add(org.eigenbase.enki.jmi.impl.RefAssociationBase.makeWrongSizeException(exposedEnd, referencedEnd, this));
        }
    }
}

// End Attribute.java
