/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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
package org.eigenbase.enki.hibernate;

import java.sql.*;
import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.util.GenericCollections;
import org.eigenbase.enki.util.HashMultiMap;
import org.hibernate.*;
import org.hibernate.dialect.Dialect;

/**
 * HibernateMassDeletionUtil is a utility class that implements deleting 
 * a number of {@link RefObject} instances in the most efficient manner 
 * possible by utilizing special knowledge of internal Enki/Hibernate
 * data structures.
 * 
 * @author Stephan Zuercher
 */
class HibernateMassDeletionUtil
{
    private static final int MAX_IN_CLAUSE = 256;

    private static final String IN_PARAMS = "?...";

    private final HibernateMDRepository repos;
    private final Dialect sqlDialect;
    private final String quotedMofId;
    
    private Session session; 
    
    /** 
     * Map of {@link HibernateRefClass} identifier to collection of MOF IDs
     * of that type to be deleted.  Use the identifier rather than the instance
     * itself so that we can perform lookups when we know the identifier only.
     */
    private HashMultiMap<String, Long> delMap =
        new HashMultiMap<String, Long>(true);

    /**
     * Collection of {@link HibernateAssociation} objects to be evaluated for
     * modification or deletion.
     */
    private Set<HibernateAssociation> assocs = 
        new HashSet<HibernateAssociation>();
    
    /**
     * Collection of {@link Eviction} objects describing all entities to be
     * evicted from the second level cache.
     */
    private HashSet<Eviction> evictionSet = new HashSet<Eviction>();

    /**
     * Map of association type to collection of association MOF IDs to be
     * deleted.
     */
    private HashMultiMap<HibernateAssociation.Kind, Long> assocDelMap =
        new HashMultiMap<HibernateAssociation.Kind, Long>();
    
    /**
     * Map of {@link HibernateRefClass} to a multi-map that describes the
     * column names and MOF IDs that need to be cleared to remove references
     * to associations that will be deleted.
     */
    private Map<HibernateRefClass, HashMultiMap<String, Long>> assocDerefMap = 
        new HashMap<HibernateRefClass, HashMultiMap<String, Long>>();
    
    /**
     * Map of association type to collection of child identifiers to remove.
     * Each child identifier represents the deletion of a single end of an 
     * association where the other ends are left in place.
     */
    private HashMultiMap<HibernateAssociation.Kind, Long> assocRemoveMap =
        new HashMultiMap<HibernateAssociation.Kind, Long>(true);
    
    /**
     * Map of association type to collection of ordered collection fix-up
     * operations.  Each fix-up operation represents the deletion of all
     * children beyond a certain ordinal and the re-insertion (with corrected
     * ordinals) of undeleted successors.
     */
    private HashMultiMap<HibernateAssociation.Kind, OrderedAssocFixUp> assocFixUpMap =
        new HashMultiMap<HibernateAssociation.Kind, OrderedAssocFixUp>();
    
    /**
     * Map of association type to table name.
     */
    private Map<HibernateAssociation.Kind, String> tableMap = 
        new HashMap<HibernateAssociation.Kind, String>();
    
    /**
     * Map of association type to collection table name.
     */
    private Map<HibernateAssociation.Kind, String> collectionTableMap = 
        new HashMap<HibernateAssociation.Kind, String>();
    
    /**
     * Used to prevent duplication of effort when processing many-to-many
     * associations.
     */
    private Set<HibernateAssociation> processedManyToManyAssocs = 
        new HashSet<HibernateAssociation>();

    private boolean indiscriminateOneToManyDelete;
    private boolean indiscriminateOneToManyHighCardinalityDelete;

    HibernateMassDeletionUtil(HibernateMDRepository repos)
    {
        this.repos = repos;
        this.sqlDialect = repos.getSqlDialect();
        this.quotedMofId = quote("mofId");
    }
    
    void massDelete(Collection<RefObject> objects)
    {
        if (objects.isEmpty()) {
            return;
        }
        
        session = repos.getCurrentSession();

        // 1. Collect into map of HRC -> HRO
        // 2. Hunt down composites as well
        computeDeletionMap(objects);
        
        // 3. Track Associations
        computeAssociationMaps();

        session.clear();
  
        // Evict all objects that we'll modify from the second-level cache.
        SessionFactory sessionFactory = session.getSessionFactory();
        for(Eviction ev: evictionSet) {
            ev.evict(sessionFactory);
        }
        
        // REVIEW: SWZ: 2008-08-13: For now, there's only one region for 
        // query caches.  Use any HibernateRefClass to find it.  Splitting
        // in multiple regions may help performance, since we're presently
        // dumping allOfType/Class queries for all types (even if none were
        // deleted).
        HibernateRefClass anyHrc = 
            repos.findRefClass(delMap.keySet().iterator().next());
        String queryCacheRegion = anyHrc.getQueryCacheRegion();
        sessionFactory.evictQueries(queryCacheRegion);
        
        Connection conn = session.connection();

        try {
            // iterate over objects that reference associations to be deleted
            // and remove references
            for(Map.Entry<HibernateRefClass, HashMultiMap<String, Long>> e: 
                    assocDerefMap.entrySet()) {
                HibernateRefClass hrc = e.getKey();
                String table = hrc.getTable();
    
                HashMultiMap<String, Long> map = e.getValue();
                for(String columnName: map.keySet()) {
                    Collection<Long> deRefMofIds = map.getValues(columnName);
                    
                    executeInSql(
                        conn, deRefMofIds,
                        "update ", quote(table), " set ", quote(columnName),  
                        " = null where ", quotedMofId, 
                        " in (", IN_PARAMS, ")");
                }
            }
                
            // delete the actual objects
            for(String hrcId: delMap.keySet()) {
                HibernateRefClass hrc = repos.findRefClass(hrcId);
    
                Collection<Long> delMofIds = delMap.getValues(hrcId);
                
                executeInSql(
                    conn, delMofIds,
                    "delete from ", quote(hrc.getTable()), 
                    " where ", quotedMofId, " in (", IN_PARAMS, ")");
            }
            
            // delete associations
            for(HibernateAssociation.Kind assocKind: assocDelMap.keySet())
            {
                Collection<Long> assocMofIds = assocDelMap.getValues(assocKind);
                
                String collectionTable = collectionTableMap.get(assocKind);
                if (collectionTable != null) {
                    executeInSql(
                        conn, assocMofIds, 
                        "delete from ", quote(collectionTable), 
                        " where ", quotedMofId, " in (", IN_PARAMS, ")");
                }
                
                executeInSql(
                    conn, assocMofIds, 
                    "delete from ", quote(tableMap.get(assocKind)),
                    " where ", quotedMofId, " in (", IN_PARAMS, ")");
            }
            
            final String quotedChildId = quote("childId");
            
            // delete associations members
            for(HibernateAssociation.Kind assocKind: assocRemoveMap.keySet()) {
                Collection<Long> removeMofIds = 
                    assocRemoveMap.getValues(assocKind);
                
                executeInSql(
                    conn, removeMofIds, 
                    "delete from ", quote(collectionTableMap.get(assocKind)), 
                    " where ", quotedChildId, " in (", IN_PARAMS, ")");
            }
            
            // fix-up ordered associations
            executeFixUps(conn);
        }
        catch(SQLException e) {
            throw new HibernateException(e);
        }
        
        repos.recordObjectDeletions(delMap.values());
    }

    /**
     * Deletes all extent objects as quickly as possible.  Note this method
     * does not modify any of HibernateMDRepository's session information, such
     * as all of type or class caches.  It's intended use is to implement
     * dropExtent only.
     *
     * @param session a Hibernate session object
     * @param pkg the RefPackage (extent root) to delete objects from
     */
    void massDeleteAll(Session session, RefPackage pkg)
    {
        Map<String, String> instanceClassMap = new HashMap<String, String>();
        Set<Class<?>> evictionSet = new HashSet<Class<?>>();
        
        List<String> assocTables = new ArrayList<String>();

        // Make map of this extent's instance class names to class ids
        LinkedList<RefPackage> pkgQueue = new LinkedList<RefPackage>();
        pkgQueue.add(pkg);
        while(!pkgQueue.isEmpty()) {
            RefPackage p = pkgQueue.removeFirst();
            
            pkgQueue.addAll(
                GenericCollections.asTypedCollection(
                    p.refAllPackages(), RefPackage.class));
            
            for(RefClass rc:
                    GenericCollections.asTypedCollection(
                        p.refAllClasses(), RefClass.class))
            {
                // Transient classes are not be HibernateRefClass instances.
                if (rc instanceof HibernateRefClass) {
                    HibernateRefClass hrc = (HibernateRefClass)rc;

                    Class<?> cls = hrc.getInstanceClass();
                    if (cls != null) {
                        instanceClassMap.put(
                            cls.getName(), hrc.getClassIdentifier());
                        evictionSet.add(cls);
                    }
                }
            }
            
            for(RefAssociation ra:
                GenericCollections.asTypedCollection(
                    p.refAllAssociations(), RefAssociation.class))
            {
                // Transient associations are not be HibernateRefAssociation
                // instances.
                if (ra instanceof HibernateRefAssociation) {
                    HibernateRefAssociation hra = (HibernateRefAssociation)ra;
                    // Tables deleted in the order added, so add collection
                    // tables first to remove their foreign key ref to assoc
                    // table.
                    String collectionTable = hra.getCollectionTable();
                    if (collectionTable != null) {
                        assocTables.add(collectionTable);
                    }
                    
                    assocTables.add(hra.getTable());
                }
            }
        }
        
        // Generate multi-map of class identifier to MOF IDs pending deletion.
        // If a class is not present in the instanceClassMap, it must not
        // belong to this extent.
        Query query = session.getNamedQuery("AllTypeMappings");
        List<?> allTypeMappings = query.list();
        for(MofIdTypeMapping m: 
                GenericCollections.asTypedList(
                    allTypeMappings, MofIdTypeMapping.class))
        {
            String classId = instanceClassMap.get(m.getTypeName());
            if (classId == null) {
                continue;
            }

            delMap.put(classId, m.getMofId());
        }
        
        session.clear();
        
        // Evict all instance of the deleted types from the second level cache.
        SessionFactory sessionFactory = session.getSessionFactory();
        for(Class<?> cls: evictionSet) {
            sessionFactory.evict(cls);
        }
        sessionFactory.evictQueries();
        
        Connection conn = session.connection();
        
        // Perform deletions
        try {
            // Delete the actual objects first (removing their foreign key
            // references to assoc tables).
            for(String hrcId: delMap.keySet()) {
                HibernateRefClass hrc = repos.findRefClass(hrcId);
    
                Collection<Long> delMofIds = delMap.getValues(hrcId);
                
                executeInSql(
                    conn, delMofIds,
                    "delete from ", quote(hrc.getTable()), 
                    " where ", quotedMofId, " in (", IN_PARAMS, ")");
            }
            
            // Delete association objects (collection tables first, see above).
            Statement stmt = conn.createStatement();
            try {
                for(String table: assocTables) {
                    stmt.executeUpdate("delete from " + quote(table));
                }
            } finally {
                stmt.close();
            }
            
            // Clean up the type lookup map.
            executeInSql(
                conn, delMap.values(),
                "delete from ", quote("ENKI_TYPE_LOOKUP"), 
                " where ", quotedMofId, " in (", IN_PARAMS, ")");
        } catch(SQLException e) {
            throw new HibernateException(e);
        }
    }

    
    /**
     * Iterate over the given objects and add them to the {@link #delMap}.
     * Also traverses composite associations and adds composing objects to 
     * {@link #delMap} while populating {@link #assocDelMap} with the composing
     * associations.  Finally, populates {@link #assocs} with all other
     * associations. 
     * 
     * @param objects list of objects to be deleted
     */
    private void computeDeletionMap(Collection<? extends RefObject> objects)
    {
        for(RefObject object: objects) {
            HibernateRefObject hro = (HibernateRefObject)object;
            HibernateRefClass hrc = (HibernateRefClass)hro.refClass();
  
            // Build deletion map
            Long mofId = hro.getMofId();
            delMap.put(hrc.getClassIdentifier(), mofId);
            evictionSet.add(new Eviction(hrc.getInstanceClass(), mofId));
            
            // Find composites
            for(HibernateAssociation assoc: hro.getComposingAssociations()) {
                Collection<HibernateAssociable> composingObjects = 
                    assoc.get((HibernateAssociable)hro);
                
                // Recursively add these objects to the deletion map
                computeDeletionMap(composingObjects);

                assocs.add(assoc);
            }
            
            // Deal with remaining associations later
            assocs.addAll(hro.getNonComposingAssociations());
        }
    }
    
    private void computeAssociationMaps()
    {
        HibernateAssociation[] exemplars = new HibernateAssociation[6];
        
        for(HibernateAssociation assoc: assocs) {
            switch(assoc.getKind()) {
            case ONE_TO_ONE:
                computeAssociationMaps(
                    (HibernateOneToOneLazyAssociation)assoc);
                exemplars[0] = assoc;
                break;
                
            case ONE_TO_MANY:
                computeAssociationMaps(
                    (HibernateOneToManyLazyAssociationBase)assoc, false);
                exemplars[1] = assoc;
                break;
                
            case ONE_TO_MANY_HIGH_CARDINALITY:
                computeAssociationMaps(
                    (HibernateOneToManyLazyAssociationBase)assoc, false);
                exemplars[2] = assoc;
                break;
                
            case ONE_TO_MANY_ORDERED:
                computeAssociationMaps(
                    (HibernateOneToManyLazyAssociationBase)assoc, true);
                exemplars[3] = assoc;
                break;
                
            case MANY_TO_MANY:
                computeAssociationMaps(
                    (HibernateManyToManyLazyAssociationBase)assoc, false);
                exemplars[4] = assoc;
                break;
                
            case MANY_TO_MANY_ORDERED:
                computeAssociationMaps(
                    (HibernateManyToManyLazyAssociationBase)assoc, true);
                exemplars[5] = assoc;
                break;
                
            default:    
                throw new IllegalStateException();
            }
        }
        
        processedManyToManyAssocs = null;
        
        computeTableMaps(exemplars);
    }
    
    private void computeAssociationMaps(
        HibernateOneToOneLazyAssociation assoc)
    {
        boolean parentInDelMap = 
            isInDeletionMap(assoc.getParentType(), assoc.getParentId());
        boolean childInDelMap = 
            isInDeletionMap(assoc.getChildType(), assoc.getChildId());

        if (!parentInDelMap && !childInDelMap) {
            throw new EnkiHibernateException(
                "invalid 1-to-1 association (neither end deleted)");
        }
        
        // Delete the association no matter what: just a question of whether
        // we need to update parent or child or neither.
        Long assocMofId = assoc.getMofId();
        assocDelMap.put(assoc.getKind(), assocMofId);
        evictionSet.add(new Eviction(assoc.getInstanceClass(), assocMofId));
        
        if (parentInDelMap && childInDelMap) {
            return;
        }
        
        String type;
        Long mofId;
        boolean isFirstEnd;
        if (!childInDelMap) {
            type = assoc.getChildType();
            mofId = assoc.getChildId();
            isFirstEnd = false;
        } else {
            type = assoc.getParentType();
            mofId = assoc.getParentId();
            isFirstEnd = true;
        }
        
        addAssocDerefOp(type, mofId, assoc.getType(), isFirstEnd);
    }

    private void computeAssociationMaps(
        HibernateOneToManyLazyAssociationBase assoc, boolean isOrdered)
    {
        String parentType = assoc.getParentType();
        Long parentId = assoc.getParentId();
        boolean parentInDelMap = isInDeletionMap(parentType, parentId);

        Long assocMofId = assoc.getMofId();
        evictionSet.add(
            new Eviction(
                assoc.getInstanceClass(),
                assoc.getCollectionName(),
                assocMofId));
            
        HibernateAssociation.Kind assocKind = assoc.getKind();
        
        if (parentInDelMap) {
            // Delete the association no matter what: just a question of 
            // whether all children are deleted or not.
            assocDelMap.put(assocKind, assocMofId);
            
            String assocType = assoc.getType();
            boolean isFirstEnd = assoc.getReversed();
            
            // Removed reference to association for any child that's not being
            // deleted.
            for(HibernateLazyAssociationBase.Element childElem: 
                    assoc.getElements())
            {
                String childType = childElem.getChildType();
                Long childId = childElem.getChildId();

                if (!isInDeletionMap(childType, childId)) {
                    // Create a dereference operation
                    addAssocDerefOp(
                        childType, 
                        childId,
                        assocType,
                        isFirstEnd);
                }
            }
        } else {
            // Parent is not being deleted.  

            // For an unordered 1-to-many association, we are free to carry out
            // "indiscriminate" deletion; just purge all references to deleted
            // objects, regardless of which association they are part of.
            // Deleted objects which happen not to be children of any 1-to-many
            // unordered associations will just be skipped by the DELETE
            // statement since there won't be any corresponding records
            // matching those MOFID's in the IN list.  Note that a side-effect
            // here is that if we are deleting all of the remaining children
            // from a particular association instance (case 1 below), then that
            // association instance will stay around, empty.  So, empty
            // instances have to be tolerated, and must behave the same as if
            // the association instance no longer existed.
            if (!isOrdered) {
                boolean isHighCard =                         
                    assocKind == 
                        HibernateAssociation.Kind.ONE_TO_MANY_HIGH_CARDINALITY;

                if (isHighCard) {
                    if (indiscriminateOneToManyHighCardinalityDelete) {
                        // If we've already done this for some other
                        // association, no need to waste cycles doing it again;
                        // assocRemoveMap doesn't track association, only 
                        // association kind.
                        return;
                    }
                    indiscriminateOneToManyHighCardinalityDelete = true;
                } else {
                    assert(assocKind == HibernateAssociation.Kind.ONE_TO_MANY);
                    if (indiscriminateOneToManyDelete) {
                        // Same situation, but for normal 1-to-many
                        return;
                    }
                    indiscriminateOneToManyDelete = true;
                }
                
                for (Long mofId : delMap.values()) {
                    assocRemoveMap.put(assocKind, mofId);
                }
                return;
            }
            
            // Otherwise, three cases:
            // 1. all children in del map => delete assoc & deref from parent
            // 2. no children in del map => error
            // 3. else => remove children in del map from assoc links
            
            Collection<HibernateLazyAssociationBase.Element> children = 
                assoc.getElements();
            
            // Look for first child not in the deletion map (which implies 
            // case 2 or 3 above)
            boolean atLeastOneUndeletedChild = false;
            
            for(HibernateLazyAssociationBase.Element child: children) {
                boolean childInDelMap = 
                    isInDeletionMap(child.getChildType(), child.getChildId());
                
                // Don't remove association links here: let delete code handle
                // the case where all are deleted.
                if (!childInDelMap) {
                    atLeastOneUndeletedChild = true;
                    break;
                }
            }
            
            if (!atLeastOneUndeletedChild) {
                assocDelMap.put(assocKind, assocMofId);
                
                addAssocDerefOp(
                    parentType,
                    parentId,
                    assoc.getType(),
                    !assoc.getReversed());
            } else {
                // Remove children in deletion map from the association
                Iterator<HibernateLazyAssociationBase.Element> iter =
                    children.iterator();
                int removals;
                if (isOrdered) {
                    removals = 
                        removeDeletedOrderedElements(
                            assocKind, assocMofId, iter);
                } else {
                    removals = removeDeletedElements(assocKind, iter);
                }
                
                if (removals == 0) {
                    throw new EnkiHibernateException(
                        "invalid 1-to-* association (no ends deleted)");
                }
            }
        }
    }

    private void computeAssociationMaps(
        HibernateManyToManyLazyAssociationBase assoc, boolean isOrdered)
    {
        if (processedManyToManyAssocs.contains(assoc)) {
            // Many-to-many assocs are stored as multiple one-to-many assocs.
            // It's possible that the recursion below can cause the same
            // assoc to be handled multiple times; skip the repeats.
            return;
        }
        
        boolean sourceInDelMap = 
            isInDeletionMap(assoc.getSourceType(), assoc.getSourceId());
        
        Long assocMofId = assoc.getMofId();
        evictionSet.add(
            new Eviction(
                assoc.getInstanceClass(), 
                assoc.getCollectionName(), 
                assocMofId));

        HibernateAssociation.Kind assocKind = assoc.getKind();
        
        if (sourceInDelMap) {
            // Delete the association no matter what: just a question of 
            // whether all targets are deleted or not.
            assocDelMap.put(assocKind, assocMofId);
            
            boolean isFirstEnd = assoc.getReversed();
            
            // Remove reference to association for any child that's not being
            // deleted.
            for(HibernateAssociable target: assoc.getTargetCollection()) {
                boolean targetInDelMap = isInDeletionMap(target);

                if (!targetInDelMap) {
                    // Don't automatically dereference; just repeat this 
                    // operation for the reverse direction association.  If
                    // this child is being deleted the reverse association
                    // is already in assocs and will be (or has been) dealt
                    // with.
                    
                    HibernateAssociation targetAssoc = 
                        target.getAssociation(assoc.getType(), isFirstEnd);
                                        
                    computeAssociationMaps(
                        (HibernateManyToManyLazyAssociationBase)targetAssoc,
                        isOrdered);
                }
            }
        } else {
            // Source is not being deleted.
            // 1. All targets are in del map => delete assoc and deref from src
            // 2. No targets are in del map => someone else got here first
            //    (This case is handled by the check at the start of the 
            //     method).
            // 3. else remove targets in del map from assoc links
            
            Collection<HibernateLazyAssociationBase.Element> targets = 
                assoc.getTargetElements();

            boolean atLeastOneUndeletedTarget = false;
            for(HibernateLazyAssociationBase.Element target: targets) {
                boolean targetInDelMap = 
                    isInDeletionMap(
                        target.getChildType(), target.getChildId());
                
                // Don't remove association links here: let delete code handle
                // the case where all are deleted.
                if (!targetInDelMap) {
                    atLeastOneUndeletedTarget = true;
                    break;
                }
            }
            
            if (!atLeastOneUndeletedTarget) {
                assocDelMap.put(assocKind, assocMofId);
                
                addAssocDerefOp(
                    assoc.getSourceType(), 
                    assoc.getSourceId(), 
                    assoc.getType(), 
                    !assoc.getReversed());
            } else {
                // Remove targets in deletion map from the association.
                Iterator<HibernateLazyAssociationBase.Element> iter =
                    targets.iterator();

                if (isOrdered) {
                    removeDeletedOrderedElements(assocKind, assocMofId, iter);
                } else {
                    removeDeletedElements(assocKind, iter);
                }
            }
        }
        
        processedManyToManyAssocs.add(assoc);
    }
    
    private void addAssocDerefOp(
        String classId,
        Long mofId,
        String assocType,
        boolean isFirstEnd)
    {
        HibernateRefClass hrc = repos.findRefClass(classId);
        
        HashMultiMap<String, Long> derefMap = 
            assocDerefMap.get(hrc);
        if (derefMap == null) {
            derefMap = new HashMultiMap<String, Long>();
            assocDerefMap.put(hrc, derefMap);
        }
        
        String columnName = 
            hrc.getAssociationColumnName(assocType, isFirstEnd);
        
        derefMap.put(columnName, mofId);
        
        evictionSet.add(new Eviction(hrc.getInstanceClass(), mofId));
    }
    
    private int removeDeletedElements(
        HibernateAssociation.Kind assocKind,
        Iterator<HibernateLazyAssociationBase.Element> iter)
    {
        int removals = 0;
        while(iter.hasNext()) {
            HibernateLazyAssociationBase.Element child = iter.next();
            
            boolean childInDelMap = 
                isInDeletionMap(
                    child.getChildType(), child.getChildId());
            
            if (childInDelMap) {
                assocRemoveMap.put(assocKind, child.getChildId());
                removals++;
            }
        }
        
        return removals;
    }

    private int removeDeletedOrderedElements(
        HibernateAssociation.Kind assocKind,
        long assocMofId,
        Iterator<HibernateLazyAssociationBase.Element> iter)
    {
        OrderedAssocFixUp fixUp = null;
        
        // Note: ordinal is only valid until the first deleted element is 
        // encountered.
        int ordinal = 0;
        int removals = 0;        
        while(iter.hasNext()) {
            HibernateLazyAssociationBase.Element child = iter.next();
            
            boolean childInDelMap = 
                isInDeletionMap(
                    child.getChildType(), child.getChildId());
            
            // REVIEW: SWZ: 2008-08-15: An optimization here would be to detect
            // when all the deleted children are at the end of the collection,
            // place them in assocRemoveMap, and leave out the fix-up object.
            // This would delete the child in all ordered assocs (which is okay
            // because it is a deleted object).  Other ordered assocs might
            // still require fix up operations, which would also work 
            // correctly.
            
            if (childInDelMap) {
                // No need to place the child in the assocRemoveMap, we'll
                // use the fixup operation to delete it.
                removals++;
                
                if (fixUp == null) {
                    fixUp = new OrderedAssocFixUp(assocMofId, ordinal);
                    assocFixUpMap.put(assocKind, fixUp);
                }                
            } else if (fixUp != null) {
                fixUp.childTypes.add(child.getChildType());
                fixUp.childIds.add(child.getChildId());
            }
            
            ordinal++;
        }
        
        return removals;
    }
    
    private void computeTableMaps(HibernateAssociation... assocs)
    {
        for(HibernateAssociation assoc: assocs) {
            if (assoc != null) {
                HibernateAssociation.Kind kind = assoc.getKind();
                
                String tableName = assoc.getTable();
                tableMap.put(kind, tableName);
                
                String collectionTableName = assoc.getCollectionTable();
                if (collectionTableName != null) {
                    collectionTableMap.put(kind, collectionTableName);
                }
            }
        }
    }

    private boolean isInDeletionMap(RefObject object)
    {
        HibernateRefClass hrc = (HibernateRefClass)object.refClass();
        
        return delMap.contains(
            hrc.getClassIdentifier(), ((HibernateRefObject)object).getMofId());
    }
    
    private boolean isInDeletionMap(String classifierId, Long mofId)
    {
        return delMap.contains(classifierId, mofId);
    }
    
    private int buildInSql(
        StringBuilder sql, 
        int numValues,
        int maxParams,
        String... dml)
    {
        final int numParams;
        if (maxParams == -1) {
            if (numValues <= MAX_IN_CLAUSE) {
                // Fewer values than maximum number of parameters; use number 
                // of values as the number of parameters.
                numParams = numValues;
            } else if (numValues % MAX_IN_CLAUSE == 0) {
                // Number of values is an exact multiple of the maximum number
                // of parameters, so use the max.
                numParams = MAX_IN_CLAUSE;
            } else {
                // Compute minimum number of statements that need to be 
                // executed
                int numStmts = (numValues / MAX_IN_CLAUSE) + 1;
                if (numValues % numStmts == 0) {
                    // Number of values is a multiple of the number of 
                    // statements, so produce that many statements (saves
                    // an extra prepare)
                    numParams = numValues / numStmts;
                } else {
                    // Just use the max size and prepare an extra statement 
                    // for the leftover values.
                    // REVIEW: SWZ: 2008-08-12: An optimization might be to
                    // execute more statements of equal size. e.g., 628 values
                    // with MAX_IN_CLAUSE=256 means 3 statements.  Currently
                    // we'll do 256, 256, 116.  Is it better to instead prepare
                    // 1 statement with 157 parameters and execute it 4 times?
                    numParams = MAX_IN_CLAUSE;
                }
            }
        } else {
            numParams = maxParams;
        }
        
        for(String s: dml) {
            if (s.equals(IN_PARAMS)) {
                sql.append("?");
                for(int i = 1; i < numParams; i++) {
                    sql.append(", ?");
                }
            } else {
                sql.append(s);
            }
        }
        
        return numParams;
    }
    
    private void executeInSql(
        Connection conn, 
        Collection<Long> values, 
        String... sqlParts)
    throws SQLException
    {
        int numValues = values.size();

        if (numValues == 0) {
            return;
        }
        
        // Generate a SQL DML statement with a parameterized in clause.
        StringBuilder sqlBuilder = new StringBuilder();
        final int numParams = buildInSql(sqlBuilder, numValues, -1, sqlParts);        
        String sql = sqlBuilder.toString();
        
        // If necessary, generate a final DML statement with fewer parameters
        // to handle leftovers.
        String remainderSql = null;
        if (numValues > numParams && numValues % numParams != 0) {
            sqlBuilder.setLength(0);
            buildInSql(sqlBuilder, numValues, numValues % numParams, sqlParts);
            remainderSql = sqlBuilder.toString();
        }
        
        int fullIters = numValues / numParams;
        
        // Execute primary statement. Execute until out of values (or if there
        // is a remainder statement, as many as fullIters times).
        PreparedStatement stmt = conn.prepareStatement(sql);
        int param = 1;
        Iterator<Long> iter = values.iterator();
        try {
            while(iter.hasNext()) {
                Long mofId = iter.next();
                
                stmt.setLong(param++, mofId);
                
                if (param > numParams) {
                    param = 1;
                    stmt.executeUpdate();
                    
                    if (remainderSql != null && --fullIters == 0) {
                        break;
                    }
                }
            }
            assert(param == 1);
        }
        finally {
            stmt.close();
        }
            
        if (remainderSql != null) {
            // Execute remainder statement for the last group.
            assert(iter.hasNext());
            
            stmt = conn.prepareStatement(remainderSql);
            param = 1;
            try {
                while(iter.hasNext()) {
                    Long mofId = iter.next();
                    
                    stmt.setLong(param++, mofId);
                }
                stmt.executeUpdate();
            }
            finally {
                stmt.close();
            }
        }
    }
    
    private void executeFixUps(Connection conn) throws SQLException
    {
        final String quotedOrdinal = quote("ordinal");
        
        for(HibernateAssociation.Kind assocKind: assocFixUpMap.keySet()) {
            Collection<OrderedAssocFixUp> fixUpOps =
                assocFixUpMap.getValues(assocKind);

            if (fixUpOps.isEmpty()) {
                continue;
            }
            
            PreparedStatement delStmt = 
                conn.prepareStatement(
                    concat(
                        "delete from ",
                        quote(collectionTableMap.get(assocKind)),
                        " where ", quotedMofId, " = ? and ",
                        quotedOrdinal, " >= ?"));
            
            PreparedStatement insStmt = 
                conn.prepareStatement(
                    concat(
                        "insert into ", 
                        quote(collectionTableMap.get(assocKind)),
                        " values (?, ?, ?, ?)"));
            
            for(OrderedAssocFixUp fixUpOp: fixUpOps) {
                delStmt.setLong(1, fixUpOp.mofId);
                delStmt.setInt(2, fixUpOp.initialOrdinal);
                delStmt.executeUpdate();
                
                Iterator<String> typeIter = fixUpOp.childTypes.iterator();
                Iterator<Long> idIter = fixUpOp.childIds.iterator();
                int ordinal = fixUpOp.initialOrdinal;
                while(typeIter.hasNext()) {
                    insStmt.setLong(1, fixUpOp.mofId);
                    insStmt.setString(2, typeIter.next());
                    insStmt.setLong(3, idIter.next());
                    insStmt.setInt(4, ordinal++);
                    insStmt.executeUpdate();
                }
            }
        }
    }
    
    private String quote(String entity)
    {
        return HibernateDialectUtil.quote(sqlDialect, entity);
    }
    
    private String concat(String... strs)
    {
        StringBuilder b = new StringBuilder();
        for(String s: strs) {
            b.append(s);
        }
        return b.toString();
    }
    
    private static class Eviction
    {
        final Class<?> entityClass;
        final String collectionName;
        final Long mofId;
        
        Eviction(Class<?> entityClass, Long mofId)
        {
            this.entityClass = entityClass;
            this.collectionName = null;
            this.mofId = mofId;
        }
        
        Eviction(Class<?> entityClass, String collectionName, Long mofId)
        {
            this.entityClass = entityClass;
            this.collectionName = collectionName;
            this.mofId = mofId;
        }
        
        public boolean equals(Object other)
        {
            Eviction that = (Eviction)other;
            
            if (this.mofId.longValue() != that.mofId.longValue()) {
                return false;
            }
            
            if (this.collectionName == null) {
                return that.collectionName == null;
            } else {
                return this.collectionName.equals(that.collectionName);
            }
        }
        
        public int hashCode()
        {
            return mofId.intValue();
        }
        
        public void evict(SessionFactory sessionFactory)
        {
            sessionFactory.evict(entityClass, mofId);
            
            if (collectionName != null) {
                sessionFactory.evictCollection(
                    entityClass.getName() + "." + collectionName, mofId);
            }
        }
    }
    
    private static class OrderedAssocFixUp
    {
        final long mofId;
        final int initialOrdinal;
        final List<String> childTypes;
        final List<Long> childIds;
        
        OrderedAssocFixUp(long mofId, int initialOrdinal)
        {
            this.mofId = mofId;
            this.initialOrdinal = initialOrdinal;
            
            this.childTypes = new ArrayList<String>();
            this.childIds = new ArrayList<Long>();
        }
    }
}

// End HibernateMassDeletionUtil.java
