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
 * Implements MOF's Constraint interface.
 *
 * @author {@link org.eigenbase.enki.codegen.MofImplementationHandler}
 */
public class Constraint
    extends org.eigenbase.enki.jmi.impl.ModelElementBase
    implements javax.jmi.model.Constraint
{
    // Attribute Fields
    private String name;
    private String annotation;
    private String expression;
    private String language;
    private javax.jmi.model.EvaluationKind evaluationPolicy;

    // Reference Fields
    private javax.jmi.model.DependsOn provider;
    private javax.jmi.model.Contains container;
    private javax.jmi.model.Constrains constraint;
    private javax.jmi.model.Constrains constrainedElement;

    // Association Fields
    private javax.jmi.model.AttachesTo attachesTo_Tag;
    private javax.jmi.model.DependsOn dependsOn_Dependent;

    Constraint(
        RefClass refClass)
    {
        super(refClass);

        this.provider = (javax.jmi.model.DependsOn)refImmediatePackage().refAssociation("DependsOn");
        this.container = (javax.jmi.model.Contains)refImmediatePackage().refAssociation("Contains");
        this.constraint = (javax.jmi.model.Constrains)refImmediatePackage().refAssociation("Constrains");
        this.constrainedElement = (javax.jmi.model.Constrains)refImmediatePackage().refAssociation("Constrains");

        this.attachesTo_Tag = (javax.jmi.model.AttachesTo)refImmediatePackage().refAssociation("AttachesTo");
        this.dependsOn_Dependent = (javax.jmi.model.DependsOn)refImmediatePackage().refAssociation("DependsOn");
    }

    Constraint(
        RefClass refClass,
        String name,
        String annotation,
        String expression,
        String language,
        javax.jmi.model.EvaluationKind evaluationPolicy)
    {
        this(refClass);

        this.name = name;
        this.annotation = annotation;
        this.expression = expression;
        this.language = language;
        this.evaluationPolicy = evaluationPolicy;
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

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String newValue)
    {
        this.expression = newValue;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String newValue)
    {
        this.language = newValue;
    }

    public javax.jmi.model.EvaluationKind getEvaluationPolicy()
    {
        return evaluationPolicy;
    }

    public void setEvaluationPolicy(javax.jmi.model.EvaluationKind newValue)
    {
        this.evaluationPolicy = newValue;
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

    @SuppressWarnings("unchecked")
    public Collection/*<javax.jmi.model.ModelElement>*/ getConstrainedElements()
    {
        return constrainedElement.getConstrainedElement(this);
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
        if (expression == null) {
            javax.jmi.model.Attribute attrib = findAttribute("expression");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (language == null) {
            javax.jmi.model.Attribute attrib = findAttribute("language");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (evaluationPolicy == null) {
            javax.jmi.model.Attribute attrib = findAttribute("evaluationPolicy");
            errors.add(new javax.jmi.reflect.WrongSizeException(attrib));
        }
        if (getConstrainedElements().size() < 1) {
            javax.jmi.model.AssociationEnd exposedEnd = findAssociationEnd("Constrains", "constraint");
            javax.jmi.model.AssociationEnd referencedEnd = findAssociationEnd("Constrains", "constrainedElement");
            errors.add(org.eigenbase.enki.jmi.impl.RefAssociationBase.makeWrongSizeException(exposedEnd, referencedEnd, this));
        }
    }
}

// End Constraint.java
