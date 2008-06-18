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
package org.eigenbase.enki.test;

import java.util.*;

import org.junit.*;

import eem.sample.special.*;

/**
 * LazyAssociationTest performs tests lazy one-to-many associations.
 * 
 * @author Stephan Zuercher
 */
public class LazyAssociationTest extends SampleModelTestBase
{
    private static int mamaId = 0;
    private static int babyId = 0;
    
    private List<LazyParent> parents;
    private List<String> parentsMofIds;
    private List<List<LazyChild>> children;
    private List<List<String>> childMofIds;
    
    @After
    public void clearFields()
    {
        parents = null;
        parentsMofIds = null;
        children = null;
        childMofIds = null;
    }
    
    private String nextMama()
    {
        return "mama-" + (++mamaId);
    }
    
    private String nextBaby()
    {
        return "baby-" + (++babyId);
    }
    
    private void createAssociations(int numParents, int numChildrenPerParent)
    {
        createAssociations(numParents, numChildrenPerParent, true, true);
    }
    
    private void createVariedAssociations(
        int numParents, int numChildrenPerParent)
    {
        createAssociations(numParents, numChildrenPerParent, false, true);
    }
    
    private void createAssociations(
        int numParents, 
        int numChildrenPerParent, 
        boolean singleType,
        boolean manageTxn)
    {
        if (manageTxn) {
            getRepository().beginTrans(true);
        }
        
        try {
            parents = new ArrayList<LazyParent>();
            parentsMofIds = new ArrayList<String>();
            children = new ArrayList<List<LazyChild>>();
            childMofIds = new ArrayList<List<String>>();
            
            SpecialPackage specialPkg = getSpecialPackage();
            
            for(int p = 0; p < numParents; p++) {
                LazyParent parent = 
                    specialPkg.getLazyParent().createLazyParent(
                        nextMama());
                
                parents.add(parent);
                parentsMofIds.add(parent.refMofId());
    
                List<LazyChild> progeny = new ArrayList<LazyChild>();
                List<String> progenyMofIds = new ArrayList<String>();
                
                children.add(progeny);
                childMofIds.add(progenyMofIds);
                
                for(int c = 0; c < numChildrenPerParent; c++) {
                    LazyChild child;
                    if (singleType || (c % 2) == 0) {
                        child = specialPkg.getLazyChild().createLazyChild(
                            nextBaby());
                    } else {
                        child = 
                            specialPkg.getLazyChild2().createLazyChild2(
                                nextBaby(), c);
                    }
                    progeny.add(child);
                    progenyMofIds.add(child.refMofId());
                    
                    child.setLazyOwner(parent);
                }
            }
        } finally {
            if (manageTxn) {
                getRepository().endTrans(false);
            }
        }
    }
    
    @Test
    public void testCreateLazyAssociations()
    {
        createAssociations(1, 2);
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        String child2MofId = childMofIds.get(0).get(1);
        
        // read associations
        getRepository().beginTrans(false);
        try {
            LazyParent expectedParent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            
            LazyParent parent = child.getLazyOwner();
            
            Assert.assertEquals(expectedParent, parent);
            
            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            
            parent = child2.getLazyOwner();
            
            Assert.assertEquals(expectedParent, parent);
        } finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testCreateLazyAssociationsByRefAssoc()
    {
        String parentMofId;
        String childMofId;
        String child2MofId;
        
        // create associations
        getRepository().beginTrans(true);
        try {
            LazyParent parent = 
                getSpecialPackage().getLazyParent().createLazyParent(
                    nextMama());
            parentMofId = parent.refMofId();
            
            LazyChild child =
                getSpecialPackage().getLazyChild().createLazyChild(nextBaby());
            childMofId = child.refMofId();
            
            LazyChild child2 =
                getSpecialPackage().getLazyChild().createLazyChild(nextBaby());
            child2MofId = child2.refMofId();
            
            getSpecialPackage().getLazyOwnership().add(parent, child);
            getSpecialPackage().getLazyOwnership().add(parent, child2);
        } finally {
            getRepository().endTrans(false);
        }
        
        // read associations
        getRepository().beginTrans(false);
        try {
            LazyParent expectedParent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            
            LazyParent parent = child.getLazyOwner();
            
            Assert.assertEquals(expectedParent, parent);
            
            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            
            parent = child2.getLazyOwner();
            
            Assert.assertEquals(expectedParent, parent);
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testDeleteLazyAssociation()
    {
        createAssociations(1, 2);
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        String child2MofId = childMofIds.get(0).get(1);
        
        // delete association between parent and 1 child
        getRepository().beginTrans(true);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);

            getSpecialPackage().getLazyOwnership().remove(parent, child);
        } finally {
            getRepository().endTrans(false);
        }
        
        // check other child
        getRepository().beginTrans(false);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            Assert.assertNotNull(parent);
            
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            Assert.assertNotNull(child);

            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            Assert.assertNotNull(child2);

            Assert.assertNull(child.getLazyOwner());
            Assert.assertEquals(parent, child2.getLazyOwner());
        } finally {
            getRepository().endTrans();
        }        
    }
    
    @Test
    public void testDeleteLazyAssociationSingleEnd()
    {
        createAssociations(1, 2);
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        String child2MofId = childMofIds.get(0).get(1);
        
        // delete parent (single end)
        getRepository().beginTrans(true);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            
            parent.refDelete();
        } finally {
            getRepository().endTrans(false);
        }
        
        // check children
        getRepository().beginTrans(false);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            Assert.assertNull(parent);
            
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            Assert.assertNotNull(child);

            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            Assert.assertNotNull(child2);

            Assert.assertNull(child.getLazyOwner());
            Assert.assertNull(child2.getLazyOwner());
        } finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testDeleteLazyAssociationMultiEnd()
    {
        createAssociations(1, 2);
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        String child2MofId = childMofIds.get(0).get(1);
        
        // delete 1 child
        getRepository().beginTrans(true);
        try {
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            
            child.refDelete();
        } finally {
            getRepository().endTrans(false);
        }
        
        // check other child
        getRepository().beginTrans(false);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            Assert.assertNotNull(parent);
            
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            Assert.assertNotNull(child);

            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            Assert.assertNull(child2);

            Assert.assertEquals(parent, child.getLazyOwner());
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testNonNavigableQuery()
    {
        createAssociations(1, 2);
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        String child2MofId = childMofIds.get(0).get(1);
        
        // read associations
        getRepository().beginTrans(false);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);

            Assert.assertNotNull(parent);
            Assert.assertNotNull(child);
            Assert.assertNotNull(child2);
            
            Collection<LazyChild> children = 
                getSpecialPackage().getLazyOwnership().getOwnsLazily(parent);

            Assert.assertEquals(2, children.size());
            Assert.assertTrue(children.contains(child));
            Assert.assertTrue(children.contains(child2));
            
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testNavigableQuery()
    {
        createAssociations(1, 2);
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        String child2MofId = childMofIds.get(0).get(1);
        
        // read associations
        getRepository().beginTrans(false);
        try {
            LazyParent expectedParent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);

            Assert.assertNotNull(expectedParent);
            Assert.assertNotNull(child);
            Assert.assertNotNull(child2);
            
            LazyParent parent = 
                getSpecialPackage().getLazyOwnership().getLazyOwner(child);
            Assert.assertEquals(expectedParent, parent);
            
            parent = 
                getSpecialPackage().getLazyOwnership().getLazyOwner(child2);
            Assert.assertEquals(expectedParent, parent);
        } finally {
            getRepository().endTrans();
        }
    }

    @Test
    public void testReassociation()
    {
        createAssociations(2, 1);
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        String parent2MofId = parentsMofIds.get(1);
        String child2MofId = childMofIds.get(1).get(0);
        
        // move child2 to parent
        getRepository().beginTrans(true);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            
            child2.setLazyOwner(parent);
        } finally {
            getRepository().endTrans(false);
        }
        
        // check associations
        getRepository().beginTrans(false);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyParent parent2 = 
                (LazyParent)getRepository().getByMofId(parent2MofId);
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            LazyChild child2 = 
                (LazyChild)getRepository().getByMofId(child2MofId);
            
            Collection<LazyChild> parent2Children = 
                getSpecialPackage().getLazyOwnership().getOwnsLazily(parent2);
            Assert.assertTrue(parent2Children.isEmpty());
            
            Collection<LazyChild> parentChildren =
                getSpecialPackage().getLazyOwnership().getOwnsLazily(parent);
            Assert.assertEquals(2, parentChildren.size());
            Assert.assertTrue(parentChildren.contains(child));
            Assert.assertTrue(parentChildren.contains(child2));
            Assert.assertEquals(parent, child.getLazyOwner());
            Assert.assertEquals(parent, child2.getLazyOwner());
        } finally {
            getRepository().endTrans();
        }
        
    }
    
    @Test
    public void testDuplicates()
    {
        getRepository().beginTrans(true);
        try {
            createAssociations(1, 1, true, false);
            LazyParent parent = parents.get(0);
            
            LazyChild child = children.get(0).get(0);

            for(int i = 0; i < 10; i++) {
                getSpecialPackage().getLazyOwnership().add(parent, child);
            }
        } finally {
            getRepository().endTrans(false);
        }
        
        String parentMofId = parentsMofIds.get(0);
        String childMofId = childMofIds.get(0).get(0);
        
        // read associations
        getRepository().beginTrans(false);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);
            LazyChild child = 
                (LazyChild)getRepository().getByMofId(childMofId);
            
            Collection<LazyChild> children = 
                getSpecialPackage().getLazyOwnership().getOwnsLazily(parent);

            Assert.assertEquals(1, children.size());
            Assert.assertTrue(children.contains(child));
            Assert.assertEquals(parent, child.getLazyOwner());
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testLargeAssociation()
    {
        createVariedAssociations(1, 50);
        String parentMofId = parentsMofIds.get(0);
        List<String> child1MofIds = childMofIds.get(0);
        
        getRepository().endSession();
        getRepository().beginSession();
        
        // read associations
        getRepository().beginTrans(false);
        try {
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentMofId);

            Assert.assertEquals(parentMofId, parent.refMofId());
            
            Collection<LazyChild> children = 
                getSpecialPackage().getLazyOwnership().getOwnsLazily(parent);
            Assert.assertEquals(child1MofIds.size(), children.size());
            
            Set<String> childMofIdsCopy = new HashSet<String>(child1MofIds);
            
            for(LazyChild child: children) {
                Assert.assertTrue(childMofIdsCopy.contains(child.refMofId()));
                childMofIdsCopy.remove(child.refMofId());
            }

            Assert.assertTrue(childMofIdsCopy.isEmpty());
        } finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testUnsavedAssociation()
    {
        getRepository().beginTrans(true);
        try {
            createAssociations(1, 10, false, false);
            
            LazyParent parent = 
                (LazyParent)getRepository().getByMofId(parentsMofIds.get(0));
            
            List<String> child1MofIds = childMofIds.get(0);
            
            Collection<LazyChild> children = 
                getSpecialPackage().getLazyOwnership().getOwnsLazily(parent);
            Assert.assertEquals(child1MofIds.size(), children.size());
            
            Set<String> childMofIdsCopy = new HashSet<String>(child1MofIds);
            
            for(LazyChild child: children) {
                Assert.assertTrue(childMofIdsCopy.contains(child.refMofId()));
                childMofIdsCopy.remove(child.refMofId());
            }

            Assert.assertTrue(childMofIdsCopy.isEmpty());
        } finally {
            getRepository().endTrans(false);
        }
    }
}

// End LazyAssociationTest.java
