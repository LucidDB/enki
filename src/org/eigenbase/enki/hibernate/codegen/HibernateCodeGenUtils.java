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

import java.util.*;

import javax.jmi.model.*;

import org.eigenbase.enki.codegen.*;

/**
 * HibernateCodeGenUtils contains utilities for generating Hibernate model
 * code.
 * 
 * @author Stephan Zuercher
 */
public class HibernateCodeGenUtils
{
    private HibernateCodeGenUtils()
    {
    }

    /**
     * Determine whether the given {@link ModelElement} is transient.
     * 
     * @param modelElement any model element 
     * @return true if the model element is a member of a transient package
     */
    static boolean isTransient(ModelElement modelElement)
    {
        if (modelElement instanceof MofPackage) {
            // TODO: switch to using an Enki-specific MOF tag
            if (((MofPackage)modelElement).getName().equals("Fennel")) {
                return true;
            }
        }
        
        Namespace container = modelElement.getContainer();
        if (container == null) {
            return false;
        } else {
            return isTransient(container);
        }
    }

    /**
     * Find all associations that refer to the given MofClass which are not
     * described by {@link Reference} instances.
     * 
     * @param generator code generator
     * @param assocInfoMap map of {@link Association} to 
     *                     {@link AssociationInfo} for all associations in the 
     *                     model
     * @param cls {@link MofClass} to find unreferenced associations
     * @param references collection of {@link Reference} instances associated
     *                   with <code>cls</code>
     * @param unrefAssocRefInfoMap empty, modifiable map which is populated 
     *                             with pseudo-{@link ReferenceInfo} objects
     *                             to facilitate code gen for the unreferenced
     *                             associations
     * @return collection of unreferenced {@link Association} instances
     */
    static Collection<Association> findUnreferencedAssociations(
        Generator generator,
        Map<Association, AssociationInfo> assocInfoMap,
        MofClass cls, 
        Collection<Reference> references,
        Map<Association, ReferenceInfo> unrefAssocRefInfoMap)
    {
        // Start with all associations
        Set<Association> result = 
            new LinkedHashSet<Association>(assocInfoMap.keySet());
        
        // Remove associations that are included in references
        for(Reference ref: references) {
            Association assoc = 
                (Association)ref.getExposedEnd().getContainer();
            
            result.remove(assoc);
        }
        
        // Remove associations that don't refer to the given MofClass
        Iterator<Association> iter = result.iterator();
        ASSOC_LOOP:
        while(iter.hasNext()) {
            Association assoc = iter.next();
            
            AssociationInfo assocInfo = assocInfoMap.get(assoc);
            
            for(int endIndex = 0; endIndex < 2; endIndex++) {
                AssociationEnd end = assocInfo.getEnd(endIndex);
    
                Classifier endType = end.getType();
                if (endType instanceof AliasType) {
                    endType = ((AliasType)endType).getType();
                }
                
                if (cls.equals(endType) || 
                    cls.allSupertypes().contains(endType))
                {
                    // End is the exposed end
                    AssociationEnd refEnd = assocInfo.getEnd(1 - endIndex);
                 
                    // Verify it's not circular
                    // REVIEW: SWZ: We do not handle the case where there is
                    // a circular association without a Reference.
                    Classifier refEndType = refEnd.getType();
                    if (refEndType instanceof AliasType) {
                        refEndType = ((AliasType)refEndType).getType();
                    }
                    if (cls.equals(refEndType) ||
                        cls.allSupertypes().contains(refEndType))
                    {
                        throw new IllegalStateException("circular");
                    }
                    
                    ReferenceInfoImpl refInfo = 
                        new ReferenceInfoImpl(generator, assoc, refEnd);
                    unrefAssocRefInfoMap.put(assoc, refInfo);
                    
                    continue ASSOC_LOOP;
                }
            }
            
            iter.remove();
        }
        
        return result;
    }
}

// End HibernateCodeGenUtils.java
