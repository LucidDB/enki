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
 * Implements MOF's AssociationEnd interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class AssociationEnd
    extends org.eigenbase.enki.jmi.impl.AssociationEndBase
    implements javax.jmi.model.AssociationEnd
{
    // Attribute Fields
    private String name;
    private String annotation;
    private Boolean isNavigable;
    private javax.jmi.model.AggregationKind aggregation;
    private javax.jmi.model.MultiplicityType multiplicity;
    private Boolean isChangeable;

    // Reference Fields
    private javax.jmi.model.DependsOn provider;
    private javax.jmi.model.Contains container;
    private javax.jmi.model.Constrains constraint;
    private javax.jmi.model.IsOfType type;

    // Association Fields
    private javax.jmi.model.Exposes exposes_Referrer;
    private javax.jmi.model.RefersTo refersTo_Referent;
    private javax.jmi.model.AttachesTo attachesTo_Tag;
    private javax.jmi.model.DependsOn dependsOn_Dependent;

    AssociationEnd(
        RefClass refClass)
    {
        super(refClass);

        this.provider = (javax.jmi.model.DependsOn)refImmediatePackage().refAssociation("DependsOn");
        this.container = (javax.jmi.model.Contains)refImmediatePackage().refAssociation("Contains");
        this.constraint = (javax.jmi.model.Constrains)refImmediatePackage().refAssociation("Constrains");
        this.type = (javax.jmi.model.IsOfType)refImmediatePackage().refAssociation("IsOfType");

        this.exposes_Referrer = (javax.jmi.model.Exposes)refImmediatePackage().refAssociation("Exposes");
        this.refersTo_Referent = (javax.jmi.model.RefersTo)refImmediatePackage().refAssociation("RefersTo");
        this.attachesTo_Tag = (javax.jmi.model.AttachesTo)refImmediatePackage().refAssociation("AttachesTo");
        this.dependsOn_Dependent = (javax.jmi.model.DependsOn)refImmediatePackage().refAssociation("DependsOn");
    }

    AssociationEnd(
        RefClass refClass,
        String name,
        String annotation,
        boolean isNavigable,
        javax.jmi.model.AggregationKind aggregation,
        javax.jmi.model.MultiplicityType multiplicity,
        boolean isChangeable)
    {
        this(refClass);

        this.name = name;
        this.annotation = annotation;
        this.isNavigable = isNavigable;
        this.aggregation = aggregation;
        this.multiplicity = multiplicity;
        this.isChangeable = isChangeable;
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

    public boolean isNavigable()
    {
        if (isNavigable == null) {
            return false;
        }
        return isNavigable;
    }

    public void setNavigable(boolean newValue)
    {
        this.isNavigable = newValue;
    }

    public javax.jmi.model.AggregationKind getAggregation()
    {
        return aggregation;
    }

    public void setAggregation(javax.jmi.model.AggregationKind newValue)
    {
        this.aggregation = newValue;
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
        if (isChangeable == null) {
            return false;
        }
        return isChangeable;
    }

    public void setChangeable(boolean newValue)
    {
        this.isChangeable = newValue;
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

    public javax.jmi.model.AssociationEnd otherEnd()
    {
        return super.otherEnd();
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
        if (isNavigable == null) {
            javax.jmi.model.Attribute attrib = findAttribute("isNavigable");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (aggregation == null) {
            javax.jmi.model.Attribute attrib = findAttribute("aggregation");
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
        if (getType() == null) {
            javax.jmi.model.AssociationEnd exposedEnd = findAssociationEnd("IsOfType", "typedElements");
            javax.jmi.model.AssociationEnd referencedEnd = findAssociationEnd("IsOfType", "type");
            errors.add(org.eigenbase.enki.jmi.impl.RefAssociationBase.makeWrongSizeException(exposedEnd, referencedEnd, this));
        }
    }
}

// End AssociationEnd.java
