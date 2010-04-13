/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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
package org.eigenbase.enki.codegen;

import java.util.*;

import javax.jmi.model.*;

import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;

import sun.security.action.*;

/**
 * MofImplementationHandler is used to generate classes in the
 * {@link org.eigenbase.enki.jmi.impl} package.  This generator is typically 
 * not used except during development.
 * 
 * @author Stephan Zuercher
 */
public class MofImplementationHandler
    extends TransientImplementationHandler
{
    private static final JavaClassReference DEPENDS_ON_BASE_CLASS =
        new JavaClassReference(DependsOnBase.class, false);
    
    private static final JavaClassReference MODEL_ELEMENT_BASE_CLASS =
        new JavaClassReference(ModelElementBase.class, false);
    
    private static final JavaClassReference GENERALIZABLE_ELEMENT_BASE_CLASS =
        new JavaClassReference(GeneralizableElementBase.class, false);

    private static final JavaClassReference NAMESPACE_BASE_CLASS =
        new JavaClassReference(NamespaceBase.class, false);
    
    private static final JavaClassReference ASSOCIATION_END_BASE_CLASS =
        new JavaClassReference(AssociationEndBase.class, false);
    
    public MofImplementationHandler()
    {
        super();
    }
    
    @Override
    public void setIncludes(Collection<String> includedPackages)
    {
        throw new UnsupportedOperationException(
            "MofImplementatinHandler does not support explicit package inclusion");
    }


    protected String convertToTypeName(String entityName) 
        throws GenerationException
    {
        if (!entityName.startsWith(JMI_PACKAGE_PREFIX)) {
            throw new GenerationException(
                "All types are expected to be within " + JMI_PACKAGE_PREFIX);
        }
        
        entityName = 
            JMI_PACKAGE_PREFIX_SUBST + 
            entityName.substring(JMI_PACKAGE_PREFIX.length());
        
        return entityName;
    }

    protected String computeSuffix(String baseSuffix)
    {
        return baseSuffix;
    }
    
    protected JavaClassReference getAssociationBaseClass(
        AssociationInfo assocInfo)
    throws GenerationException
    {
        JavaClassReference baseClass = REF_ASSOC_IMPL_CLASS;
        if (!assocInfo.isChangeable(0) && !assocInfo.isChangeable(1)) {
            // DependsOn Associations require special handling: the existence
            // of dependencies between objects is intrinsic to the model, 
            // rather than the creation of arbitrary associations.
            if (!assocInfo.getAssoc().getName().equals("DependsOn")) {
                throw new GenerationException(
                    "Unhandled derived association: " + 
                    assocInfo.getAssoc().getName());
            }
            baseClass = DEPENDS_ON_BASE_CLASS;
        }

        return baseClass;
    }
    
    protected JavaClassReference getClassInstanceBaseClass(MofClass cls)
        throws GenerationException
    {
        List<?> supertypes = cls.allSupertypes();
        Set<String> supertypeNames = new HashSet<String>();
        for(MofClass supertype: 
                GenericCollections.asTypedList(supertypes, MofClass.class))
        {
            supertypeNames.add(supertype.getName());
        }
        supertypeNames.add(cls.getName());

        if (supertypeNames.contains("GeneralizableElement")) {
            return GENERALIZABLE_ELEMENT_BASE_CLASS;
        }
        
        if (supertypeNames.contains("Namespace")) {
            return NAMESPACE_BASE_CLASS;
        }
        
        if (supertypeNames.contains("AssociationEnd")) {
            return ASSOCIATION_END_BASE_CLASS;
        }
        
        if (supertypeNames.contains("ModelElement")) {
            return MODEL_ELEMENT_BASE_CLASS;
        }
        
        return REF_OBJECT_IMPL_CLASS;
    }
}

// End MofImplementationHandler.java
