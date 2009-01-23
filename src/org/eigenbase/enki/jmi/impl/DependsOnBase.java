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
package org.eigenbase.enki.jmi.impl;

import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

/**
 * DependsOnBase is a special case of {@link RefAssociationBase} for the
 * MOF model DependsOn association.  It exists because the DependsOn
 * association is not changeable.  The links for the association are
 * generated automatically at run-time via the 
 * {@link ModelElement#findRequiredElements(Collection, boolean)} operation
 * on {@link ModelElement}.
 * 
 * @author Stephan Zuercher
 */
public abstract class DependsOnBase extends RefAssociationBase
{
    private static final String PROVIDER_END_NAME = "provider";
    private static final String DEPENDENT_END_NAME = "dependent";

    public DependsOnBase(
        RefPackage container,
        String end1Name,
        Class<? extends RefObject> end1Class,
        Multiplicity end1Multiplicity,
        String end2Name,
        Class<? extends RefObject> end2Class,
        Multiplicity end2Multiplicity)
    {
        super(
            container, 
            end1Name, 
            end1Class, 
            end1Multiplicity, 
            end2Name,
            end2Class,
            end2Multiplicity);

        assert(!end1Multiplicity.isSingle() && end1Multiplicity.isUnique());
        assert(!end2Multiplicity.isSingle() && end2Multiplicity.isUnique());
        assert(end1Name.equals(DEPENDENT_END_NAME));
        assert(end2Name.equals(PROVIDER_END_NAME));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection refAllLinks()
    {
        return allLinks();
    }
    
    private Collection<RefAssociationLinkImpl> allLinks()
    {
        ArrayList<RefAssociationLinkImpl> links = 
            new ArrayList<RefAssociationLinkImpl>();
        
        MetamodelInitializer initalizer = getInitializer();
        
        for(RefClassBase refClass: initalizer.getAllRefClasses()) {
            for(Object obj: refClass.refAllOfClass()) {
                RefObject refObject = (RefObject)obj;
                ModelElement modelElement = (ModelElement)refObject;
                
                Collection<?> providers =
                    modelElement.findRequiredElements(
                        Collections.singleton(ModelElement.ALLDEP), false);
                for(Object o: providers) {
                    RefAssociationLinkImpl link = 
                        new RefAssociationLinkImpl(refObject, (RefObject)o);
                    
                    links.add(link);
                }
            }
        }
        
        return links;
    }

    @Override
    public boolean refLinkExists(RefObject end1, RefObject end2)
    {
        if (!(end1 instanceof ModelElement)) {
            throw new InvalidCallException(this, end1);
        }

        if (!(end2 instanceof ModelElement)) {
            throw new InvalidCallException(this, end2);
        }
        
        ModelElement dependent = (ModelElement)end1;
        
        return 
            dependent.findRequiredElements(
                Collections.singleton(ModelElement.ALLDEP), false).contains(
                    end2);
            
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection refQuery(RefObject queryEnd, RefObject queryObject)
    {
        String queryEndName = (String)queryEnd.refGetValue("name");
        try {
            return refQuery(queryEndName, queryObject);
        }
        catch(InvalidNameException e) {
            throw new InvalidCallException(this, queryEnd);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection refQuery(String queryEndName, RefObject queryObject)
    {
        if (queryEndName.equals(DEPENDENT_END_NAME)) {
            return ((ModelElement)queryObject).findRequiredElements(
                Collections.singleton(ModelElement.ALLDEP), true);
        } else if (queryEndName.equals(PROVIDER_END_NAME)) {
            ArrayList<RefObject> result = new ArrayList<RefObject>();
            for(RefAssociationLinkImpl link: allLinks()) {
                if (link.refSecondEnd().equals(queryObject)) {
                    result.add(link.refFirstEnd());
                }
            }
            return result;
        }
        
        throw new InvalidNameException(queryEndName);
    }

    @Override
    public boolean refAddLink(RefObject end1, RefObject end2)
    {
        throw new InvalidCallException(this, end1);
    }

    @Override
    public boolean refRemoveLink(RefObject end1, RefObject end2)
    {
        throw new InvalidCallException(this, end1);
    }
}

// End DependsOnBase.java
