/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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
import org.eigenbase.enki.util.*;
import org.hibernate.*;

/**
 * HibernateConstraintChecker verifies that all associations point to valid
 * ends.  Such a utility is necessary because the data model of the 
 * Enki/Hibernate repository prevents database constraints from being used
 * in some cases.  This class does not test constraints that are enforced
 * by the database.
 * 
 * @author Stephan Zuercher
 */
public class HibernateConstraintChecker
{
    private final HibernateMDRepository repos;
    
    private Map<String, HashMultiMap<Long, ReferenceSource>> references;
    
    public HibernateConstraintChecker(HibernateMDRepository repos)
    {
        this.repos = repos;
    }
    
    /**
     * Verifies constraints for associations contained in the given package.
     * If <code>deepVerify</code> is true, associations in packages contained
     * by the package are tested recursively.  Otherwise, only associations
     * directly contained in the package are checked.
     *  
     * @param pkg package to verify constraints on
     * @param errors list to which errors will be appended 
     * @param deepVerify recursively check sub-packages
     */
    public void verifyConstraints(
        HibernateRefPackage pkg, List<JmiException> errors, boolean deepVerify)
    {
        List<HibernateRefAssociation> refAssocs = 
            new ArrayList<HibernateRefAssociation>();
        collectAssociations(pkg, refAssocs, deepVerify);
        
        List<JmiException> list = verifyConstraints(refAssocs);
        errors.addAll(list);
    }

    /**
     * Verifies constraints for the given association.
     *  
     * @param assoc association to verify constraints on
     * @param errors list to which errors will be appended 
     */
    public void verifyConstraints(
        HibernateRefAssociation assoc, List<JmiException> errors)
    {
        List<JmiException> list = 
            verifyConstraints(Collections.singletonList(assoc));
        errors.addAll(list);
    }
    
    private List<JmiException> verifyConstraints(
        List<HibernateRefAssociation> refAssocs)
    {
        references = 
            new HashMap<String, HashMultiMap<Long, ReferenceSource>>();
        
        computeReferences(refAssocs);
        
        List<JmiException> errors = checkReferences();
        
        references = null;

        return errors;
    }

    private List<JmiException> checkReferences()
    {
        List<JmiException> errors = new ArrayList<JmiException>();
        
        for(Map.Entry<String, HashMultiMap<Long, ReferenceSource>> e:
                references.entrySet())
        {
            String classId = e.getKey();
            HashMultiMap<Long, ReferenceSource> refs = e.getValue();
            
            RefClass refCls = repos.findRefClass(classId);
            if (refCls == null) {
                errors.add(
                    new ConstraintViolationException(
                        null,
                        null,
                        "Missing RefClass identified by " + classId));
                continue;
            }
            
            Set<Long> expectedMofIds = refs.keySet(); 

            Collection<RefObject> objects = 
                repos.getByMofId(new ArrayList<Long>(expectedMofIds), refCls);
            
            Set<Long> gotMofIds = new HashSet<Long>();
            for(HibernateRefObject refObj: 
                    GenericCollections.asTypedCollection(
                        objects, HibernateRefObject.class))
            {
                gotMofIds.add(refObj.getMofId());
            }
            
            for(Long expected: expectedMofIds) {
                if (!gotMofIds.contains(expected)) {
                    Collection<ReferenceSource> sources = 
                        refs.getValues(expected);
                    
                    StringBuilder b = new StringBuilder();
                    b
                        .append("Missing RefObject: MOF ID: ")
                        .append(expected)
                        .append(", Referenced by: ");
                    String delim = "";
                    for(ReferenceSource s: sources) {
                        b.append(delim).append(s);
                        delim = ", ";
                    }
                    
                    errors.add(
                        new ConstraintViolationException(
                            null, null, b.toString()));
                }
            }
        }
        
        return errors;
    }

    private void computeReferences(List<HibernateRefAssociation> refAssocs)
    {
        Session session = repos.getCurrentSession();
        
        for(HibernateRefAssociation refAssoc: refAssocs) {
            Query query = 
                session.getNamedQuery(refAssoc.getAllLinksQueryName());
            query.setString(
                HibernateMappingHandler.QUERY_PARAM_ALLLINKS_TYPE, 
                refAssoc.type);
            
            List<?> list = query.list();
            for(HibernateAssociation assoc: 
                    GenericCollections.asTypedList(
                        list, HibernateAssociation.class))
            {
                switch(assoc.getKind()) {
                case ONE_TO_ONE:
                    addReferences((HibernateOneToOneLazyAssociation)assoc);
                    break;
                    
                case ONE_TO_MANY:
                case ONE_TO_MANY_ORDERED:
                case ONE_TO_MANY_HIGH_CARDINALITY:
                    addReferences(
                        (HibernateOneToManyLazyAssociationBase)assoc);
                    break;
                    
                case MANY_TO_MANY:
                case MANY_TO_MANY_ORDERED:
                    addReferences(
                        (HibernateManyToManyLazyAssociationBase)assoc);
                    break;
                    
                default:
                    throw new EnkiHibernateException(
                        "unknown assoc kind: " + assoc.getKind());
                }
            }
        }
        
        
        session.clear();
    }
    
    private void collectAssociations(
        HibernateRefPackage pkg, 
        List<HibernateRefAssociation> assocs,
        boolean deepVerify)
    {
        assocs.addAll(
            GenericCollections.asTypedCollection(
                pkg.refAllAssociations(),
                HibernateRefAssociation.class));
        if (!deepVerify) {
            return;
        }
        
        for(HibernateRefPackage subPkg: 
                GenericCollections.asTypedCollection(
                    pkg.refAllPackages(), HibernateRefPackage.class))
        {
            collectAssociations(subPkg, assocs, deepVerify);
        }
    }
    
    private void addReferences(HibernateOneToOneLazyAssociation assoc)
    {
        addReference(assoc, assoc.getParentType(), assoc.getParentId());
        addReference(assoc, assoc.getChildType(), assoc.getChildId());
    }
    
    private void addReferences(HibernateOneToManyLazyAssociationBase assoc)
    {
        addReference(assoc, assoc.getParentType(), assoc.getParentId());
        for(HibernateLazyAssociationBase.Element element: assoc.getElements())
        {
            addReference(assoc, element);
        }
    }
    
    private void addReferences(HibernateManyToManyLazyAssociationBase assoc)
    {
        addReference(assoc, assoc.getSourceType(), assoc.getSourceId());
        for(HibernateLazyAssociationBase.Element element: 
                assoc.getTargetElements())
        {
            addReference(assoc, element);
        }
    }
    
    private void addReference(
        HibernateAssociation assoc,
        HibernateLazyAssociationBase.Element element)
    {
        addReference(assoc, element.getChildType(), element.getChildId());
    }

    private void addReference(
        HibernateAssociation assoc, 
        String type, 
        Long mofId)
    {
        HashMultiMap<Long, ReferenceSource> mofIds = references.get(type);
        if (mofIds == null) {
            mofIds = new HashMultiMap<Long, ReferenceSource>(true);
            references.put(type, mofIds);
        }
        
        mofIds.put(mofId, new ReferenceSource(assoc));
    }
    
    private static class ReferenceSource
    {
        private final HibernateAssociation.Kind kind;
        private final String type;
        private final long mofId;
        
        ReferenceSource(HibernateAssociation assoc)
        {
            this.kind = assoc.getKind();
            this.type = assoc.getType();
            this.mofId = assoc.getMofId();
        }
        
        public int hashCode()
        {
            return kind.hashCode() ^ type.hashCode() ^ (int)mofId;
        }
        
        public boolean equals(Object other)
        {
            ReferenceSource that = (ReferenceSource)other;
            
            return 
                this.mofId == that.mofId && 
                this.kind == that.kind &&
                this.type.equals(that.type);
        }
        
        public String toString()
        {
            return type + "/" + kind + ":" + mofId;
        }
    }
}

// End HibernateConstraintChecker.java
