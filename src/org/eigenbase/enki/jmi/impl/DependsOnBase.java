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
 * @author Stephan Zuercher
 */
public class DependsOnBase extends RefAssociationBase
{
    public DependsOnBase(
        RefPackage container,
        String end1Name,
        Multiplicity end1Multiplicity,
        String end2Name,
        Multiplicity end2Multiplicity)
    {
        super(
            container, end1Name, end1Multiplicity, end2Name, end2Multiplicity);

        assert(end1Multiplicity == Multiplicity.UNIQUE_COLLECTION);
        assert(end2Multiplicity == Multiplicity.UNIQUE_COLLECTION);
        assert(end1Name.equals("dependent"));
        assert(end2Name.equals("provider"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection refAllLinks()
    {
        return allLinks();
    }
    
    private Collection<RefAssociationLink> allLinks()
    {
        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        
        MetamodelInitializer initalizer = getInitializer();
        
        for(RefClass refClass: initalizer.getAllRefClasses()) {
            for(RefObject refObject: 
                    initalizer.getAllInstancesOf(refClass, false))
            {
                ModelElement modelElement = (ModelElement)refObject;
                
                Collection<?> providers =
                    modelElement.findRequiredElements(
                        Collections.singleton(ModelElement.ALLDEP), false);
                for(Object o: providers) {
                    RefAssociationLink link = 
                        new RefAssociationLink(refObject, (RefObject)o);
                    
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
        if (queryEndName.equals("dependent")) {
            return ((ModelElement)queryObject).findRequiredElements(
                Collections.singleton(ModelElement.ALLDEP), false);
        } else if (queryEndName.equals("provider")) {
            ArrayList<RefObject> result = new ArrayList<RefObject>();
            for(RefAssociationLink link: allLinks()) {
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
