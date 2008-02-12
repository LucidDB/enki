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
package org.eigenbase.enki.hibernate.jmi;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;
import org.netbeans.api.mdr.events.*;

/**
 * HibernateRefAssociation provides a Hibernate-based implementation of 
 * {@link RefAssociation}.
 *
 * @author Stephan Zuercher
 */
public abstract class HibernateRefAssociation
    extends RefAssociationBase
    implements RefAssociation
{
    protected final String type;

    protected HibernateRefAssociation(
        RefPackage container,
        String type,
        String end1Name,
        Multiplicity end1Multiplicity,
        String end2Name,
        Multiplicity end2Multiplicity)
    {
        super(
            container, 
            end1Name,
            end1Multiplicity,
            end2Name,
            end2Multiplicity);
        
        this.type = type;

        HibernateRefAssociationRegistry.instance().registerRefAssociation(
            getAssociationIdentifier(),
            this);
    }

    protected abstract String getAssociationIdentifier();
    
    public Collection<?> refAllLinks()
    {
        Session session = getHibernateRepository().getCurrentSession();
        
        Query query = session.getNamedQuery(getAllLinksQueryName());
        query.setString(
            HibernateMappingHandler.QUERY_PARAM_ALLLINKS_TYPE, type);
        
        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        List<?> list = query.list();
        for(HibernateAssociation assoc: 
                GenericCollections.asTypedList(
                    list, HibernateAssociation.class))
        {
            Iterator<RefAssociationLink> iter = assoc.linkIterator();
            while(iter.hasNext()) {
                RefAssociationLink link = iter.next();
                links.add(link);
            }
        }
        
        return links;
    }

    /**
     * Obtain the name of the query that implements {@link #refAllLinks()} 
     * where the query parameter is the association type.  Query must return
     * a list of {@link HibernateAssociation} objects.  
     * 
     * @return Hibernate query name for query as described above
     */
    protected abstract String getAllLinksQueryName();

    public boolean refLinkExists(RefObject end1, RefObject end2)
    {
        checkTypes(end1, end2);
        
        HibernateAssociation assoc = 
            ((HibernateAssociable)end1).getAssociation(type, true);
        if (assoc == null) {
            return false;
        }
        
        Collection<? extends RefObject> queryResult = 
            assoc.query(true);
        
        return queryResult.contains(end2);
    }

    @Override
    protected Collection<? extends RefObject> query(
        boolean isFirstEnd, RefObject queryObject)
    {
        if (isFirstEnd) {
            checkFirstEndType(queryObject);
        } else {
            checkSecondEndType(queryObject);
        }
        
        HibernateAssociation assoc = 
            ((HibernateAssociable)queryObject).getAssociation(
                type, isFirstEnd);
        if (assoc == null) {
            return Collections.emptySet();
        }
        
        return assoc.query(isFirstEnd);
    }
    
    public boolean refAddLink(RefObject end1, RefObject end2)
    {
        checkTypes(end1, end2);

        fireAddEvent(end1, end2);
        
        HibernateAssociable assoc1 = (HibernateAssociable)end1;
        HibernateAssociable assoc2 = (HibernateAssociable)end2;

        return assoc1.getOrCreateAssociation(type, true).add(assoc1, assoc2);
    }

    public boolean refRemoveLink(RefObject end1, RefObject end2)
    {
        checkTypes(end1, end2);

        fireRemoveEvent(end1, end2);
        
        HibernateAssociable associable1 = (HibernateAssociable)end1;
        HibernateAssociable associable2 = (HibernateAssociable)end2;
        
        HibernateAssociation association1 = 
            associable1.getAssociation(type, true);
        HibernateAssociation association2 = 
            associable2.getAssociation(type, false);
        if (association1 == null || 
            association2 == null || 
            !association1.equals(association2))
        {
            // These are not associated
            return false;
        }
        
        return association1.remove(associable1, associable2);
    }

    protected abstract Class<? extends RefObject> getFirstEndType();
    protected abstract Class<? extends RefObject> getSecondEndType();
    
    private void checkTypes(RefObject end1, RefObject end2)
    {
        checkFirstEndType(end1);
        checkSecondEndType(end2);
    }

    private void checkFirstEndType(RefObject end1)
    {
        Class<?> end1Type = getFirstEndType();
        if (!end1Type.isAssignableFrom(end1.getClass())) {
            throw new TypeMismatchException(end1Type, this, end1);
        }
    }    
    
    private void checkSecondEndType(RefObject end2)
    {
        Class<?> end2Type = getSecondEndType();
        if (!end2Type.isAssignableFrom(end2.getClass())) {
            throw new TypeMismatchException(end2Type, this, end2);
        }
    }
    
    protected void fireAddEvent(RefObject end1, RefObject end2)
    {
        generateAddEvent(end1, end1Name, end2, AssociationEvent.POSITION_NONE);
    }

    public void fireAddEvent(
        boolean fixedIsFirstEnd, 
        RefObject fixedEnd, 
        RefObject end)
    {
        fireAddEvent(
            fixedIsFirstEnd, fixedEnd, end, AssociationEvent.POSITION_NONE);
    }
    
    public void fireAddEvent(
        boolean fixedIsFirstEnd, 
        RefObject fixedEnd, 
        RefObject end, 
        int index)
    {
        String fixedEndName;
        Multiplicity mult;
        if (fixedIsFirstEnd) {
            mult = end2Multiplicity;
            fixedEndName = end1Name;
        } else {
            mult = end1Multiplicity;
            fixedEndName = end2Name;
        }
        
        if (!mult.isOrdered()) {
            index = AssociationEvent.POSITION_NONE;
        }
        
        generateAddEvent(fixedEnd, fixedEndName, end, index);
    }
    
    private void generateAddEvent(
        RefObject fixedEnd, String fixedEndName, RefObject end, int index)
    {
        getHibernateRepository().enqueueEvent(
            new AssociationEvent(
                this,
                AssociationEvent.EVENT_ASSOCIATION_ADD,
                fixedEnd, 
                fixedEndName, 
                null,
                end,
                index));
    }
    
    protected void fireRemoveEvent(RefObject end1, RefObject end2)
    {
        int index = AssociationEvent.POSITION_NONE;
        if (end2Multiplicity.isOrdered()) {
            int pos = ((List<?>)query(true, end1)).indexOf(end2);
            if (pos >= 0) {
                index = pos;
            }
        }

        generateRemoveEvent(end1, end1Name, end2, index);
    }

    public void fireRemoveEvent(
        boolean fixedIsFirstEnd, RefObject fixedEnd, RefObject end)
    {
        fireRemoveEvent(
            fixedIsFirstEnd, fixedEnd, end, AssociationEvent.POSITION_NONE);
    }
    
    public void fireRemoveEvent(
        boolean fixedIsFirstEnd, RefObject fixedEnd, RefObject end, int index)
    {
        String fixedEndName;
        Multiplicity mult;
        if (fixedIsFirstEnd) {
            mult = end2Multiplicity;
            fixedEndName = end1Name;
        } else {
            mult = end1Multiplicity;
            fixedEndName = end2Name;
        }

        if (!mult.isOrdered()) {
            index = AssociationEvent.POSITION_NONE;
        }
        
        generateRemoveEvent(fixedEnd, fixedEndName, end, index);
    }
    
    private void generateRemoveEvent(
        RefObject fixedEnd, String fixedEndName, RefObject end, int index)
    {
        getHibernateRepository().enqueueEvent(
            new AssociationEvent(
                this,
                AssociationEvent.EVENT_ASSOCIATION_REMOVE,
                fixedEnd, 
                fixedEndName, 
                end,
                null,
                index >= 0 ? index : AssociationEvent.POSITION_NONE));
    }
    
    public void fireSetEvent(
        boolean fixedIsFirstEnd,
        RefObject fixedEnd, 
        RefObject oldEnd, 
        RefObject newEnd,
        int index)
    {
        String fixedEndName;
        Multiplicity mult;
        if (fixedIsFirstEnd) {
            mult = end2Multiplicity;
            fixedEndName = end1Name;
        } else {
            mult = end1Multiplicity;
            fixedEndName = end2Name;
        }

        if (!mult.isOrdered()) {
            index = AssociationEvent.POSITION_NONE;
        }
        
        getHibernateRepository().enqueueEvent(
            new AssociationEvent(
                this,
                AssociationEvent.EVENT_ASSOCIATION_SET,
                fixedEnd,
                fixedEndName,
                oldEnd,
                newEnd,
                index));
    }
    
    protected HibernateMDRepository getHibernateRepository()
    {
        return (HibernateMDRepository)getRepository();
    }
}
