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
        
        this.type = type;

        getHibernateRepository().registerRefAssociation(
            getAssociationIdentifier(),
            this);
    }

    protected abstract String getAssociationIdentifier();
    
    public abstract String getTable();
    public abstract String getCollectionTable();
    
    public Collection<?> refAllLinks()
    {
        logJmi("refAllLinks");
        
        getHibernateRepository().checkTransaction(false);

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
        logJmi("refLinkExists");

        checkTypes(end1, end2);
        
        getHibernateRepository().checkTransaction(false);

        HibernateAssociation assoc = 
            ((HibernateAssociable)end1).getAssociation(type, true);
        if (assoc == null) {
            return false;
        }
        
        Collection<? extends RefObject> queryResult = assoc.query(true);
        
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
        
        getHibernateRepository().checkTransaction(false);
        
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
        logJmi("refAddLink");
        
        checkTypes(end1, end2);

        getHibernateRepository().checkTransaction(true);

        fireAddEvent(end1, end2);
        
        HibernateAssociable assoc1 = (HibernateAssociable)end1;
        HibernateAssociable assoc2 = (HibernateAssociable)end2;

        return assoc1.getOrCreateAssociation(type, true).add(assoc1, assoc2);
    }

    public boolean refRemoveLink(RefObject end1, RefObject end2)
    {
        logJmi("refRemoveLink");
        
        checkTypes(end1, end2);

        getHibernateRepository().checkTransaction(true);

        fireRemoveEvent(end1, end2);
        
        HibernateAssociable associable1 = (HibernateAssociable)end1;
        HibernateAssociable associable2 = (HibernateAssociable)end2;
        
        HibernateAssociation association1 = 
            associable1.getAssociation(type, true);
        HibernateAssociation association2 = 
            associable2.getAssociation(type, false);
        if (association1 == null || association2 == null) {
            // These are not associated
            return false;
        }
        
        return association1.remove(associable1, associable2);
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
            Collection<?> c = query(true, end1);
            int pos = 0;
            for(Object o: c) {
                if (end2.equals(o)) {
                    index = pos;
                    break;
                }
                pos++;
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

    /**
     * Retrieve the class that represents the type of this instance.  Separate
     * from {@link #getClass()} because Hibernate can and will proxy the
     * class and we don't want to accidentally retrieve the wrong type.
     * 
     * @return class that represents this object's type
     */
    public abstract Class<? extends HibernateAssociation> getInstanceClass();
    
    /**
     * Get basic association type.
     * 
     * @return the {@link HibernateAssociation}.Kind that represents this 
     *         association
     */
    public abstract HibernateAssociation.Kind getKind();
    
    @Override
    protected void checkConstraints(List<JmiException> errors, boolean deep)
    {
        super.checkConstraints(errors, deep);
        
        HibernateConstraintChecker cc = 
            new HibernateConstraintChecker(getHibernateRepository());
        cc.verifyConstraints(this, errors);
    }
}
