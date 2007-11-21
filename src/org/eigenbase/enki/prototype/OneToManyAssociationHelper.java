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
package org.eigenbase.enki.prototype;

import java.util.*;

public class OneToManyAssociationHelper
{
    /**
     * Tests whether the given parent and child have the named one-to-many
     * association.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of the association
     * @param child child of the association
     * @return true if the parent and child are associated (with the named
     *         association); false otherwise
     */
    public static boolean isAssociatedWith(
        String name, Associable parent, Associable child)
    {
        return parent.getAssociation(name) == child.getAssociation(name);
    }
    
    /**
     * Get the children of the parent via the named association.  Returns
     * null if no children are associated with the parent.
     *   
     * @param <E> class implementing {@link Associable}
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of association
     * @param cls class representing E
     * @return List of children or null if none exist
     * @throw ClassCastException if any children cannot be cast to E
     */
    public static <E extends Associable> List<E> getChildren(
        String name, Associable parent, Class<E> cls)
    {
        OneToManyAssociation parentAssoc = 
            parent.getAssociation(name, OneToManyAssociation.class);
        if (parentAssoc == null) {
            return null;
        }
        
        return parentAssoc.getChildren(cls);
    }
    
    
    /**
     * Create an association between a parent and child with the given name.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of association
     * @param child child of association
     * @return a new OneToManyAssociation between the parent and child
     * @throws IllegalAssociationException if parent or child already
     *                                     belong to an association with the
     *                                     given name
     */
    public static OneToManyAssociation create(
        String name, Associable parent, Associable child)
    {
        return create(name, parent, new Associable[] { child });
    }

    /**
     * Create an association between a parent and children with the given name.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of association
     * @param children children of association
     * @return a new OneToManyAssociation between the parent and child
     * @throws IllegalAssociationException if parent or children already
     *                                     belong to an association with the
     *                                     given name
     */
    public static OneToManyAssociation create(
        String name, Associable parent, Associable... children)
    {
        return create(name, parent, Arrays.asList(children));
    }

    /**
     * Create an association between a parent and children with the given name.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of association
     * @param children children of association
     * @return a new OneToManyAssociation between the parent and child
     * @throws IllegalAssociationException if parent or children already
     *                                     belong to an association with the
     *                                     given name
     */
    public static OneToManyAssociation create(
        String name, Associable parent, List<? extends Associable> children)
    {
        OneToManyAssociation assoc = new OneToManyAssociation();
        assoc.setType(name);
        assoc.setParent(parent);
        assoc.setChildren(new ArrayList<Associable>(children));

        if (parent.getAssociation(name) != null) {
            throw new IllegalAssociationException(
                "parent '" + parent.toString() + 
                "' already has association named '" + name + "'");
        }
        
        parent.setAssociation(name, assoc);
        for(Associable child: children) {
            if (child.getAssociation(name) != null) {
                throw new IllegalAssociationException(
                    "child '" + child.toString() + 
                    "' already has association named '" + name + "'");
            }
            
            child.setAssociation(name, assoc);
        }
        
        return assoc;
    }
    
    /**
     * Add a child to the parent via the named association.  The association
     * is created if necessary.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent with an existing association
     * @param child new child of the association
     * @throws IllegalAssociationException if the child already has the
     *                                     named association with a different
     *                                     parent object.
     */
    public static void addChild(
        String name, Associable parent, Associable child)
    {
        addChildren(name, parent, new Associable[] { child });
    }
    
    /**
     * Adds children to the parent via the named association.  The association
     * is created if necessary.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent with an existing association
     * @param children new children of the association
     * @throws IllegalAssociationException if a child already has the
     *                                     named association with a different
     *                                     parent object.
     */
    public static void addChildren(
        String name, Associable parent, Associable... children)
    {
        addChildren(name, parent, Arrays.asList(children));
    }

    /**
     * Adds children to the parent via the named association.  The association
     * is created if necessary.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent with an existing association
     * @param children new children of the association
     * @throws IllegalAssociationException if a child already has the
     *                                     named association with a different
     *                                     parent object
     * @throws ClassCastException if the parent's existing association is
     *                            not a {@link OneToManyAssociation}
     */
    public static void addChildren(
        String name, Associable parent, List<? extends Associable> children)
    {
        OneToManyAssociation parentAssoc = 
            parent.getAssociation(name, OneToManyAssociation.class);
        if (parentAssoc == null) {
            create(name, parent, children);
            return;
        }
        
        for(Associable child: children) {
            Association childAssoc = child.getAssociation(name); 

            // REVIEW: SWZ: 10/30/2007: Does not allow same child to be added 
            // to the association multiple times.  Not sure if that is valid.
            if (childAssoc != parentAssoc) {
                throw new IllegalAssociationException(
                    "child '" + child.toString() + 
                    "' has an association to a different parent");
            }
            
            parentAssoc.addChild(child);
            child.setAssociation(name, parentAssoc);
        }
    }
    
    /**
     * Removes the given children from the named association with the parent.
     * If no children remain in the association after invocation, the
     * parent's association is nulled out. 
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of the association
     * @param child child of the association to be removed
     * @throws IllegalAssociationException if the parent and child are not 
     *                                     associated
     * @throws ClassCastException if the parent's existing association is
     *                            not a {@link OneToManyAssociation}
     */
    public static void removeChild(
        String name, Associable parent, Associable child)
    {
        removeChildren(name, parent, new Associable[] { child });
    }
    
    /**
     * Removes the given children from the named association with the parent.
     * If no children remain in the association after invocation, the
     * parent's association is nulled out. 
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of the association
     * @param children children of the association to be removed
     * @throws IllegalAssociationException if the parent and one of more
     *                                     children are not associated
     * @throws ClassCastException if the parent's existing association is
     *                            not a {@link OneToManyAssociation}
     */
    public static void removeChildren(
        String name, Associable parent, Associable... children)
    {
        removeChildren(name, parent, Arrays.asList(children));
    }

    /**
     * Removes the given children from the named association with the parent.
     * If no children remain in the association after invocation, the
     * parent's association is nulled out. 
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of the association
     * @param children children of the association to be removed
     * @throws IllegalAssociationException if the parent and one of more
     *                                     children are not associated
     * @throws ClassCastException if the parent's existing association is
     *                            not a {@link OneToManyAssociation}
     */
    public static void removeChildren(
        String name, Associable parent, List<? extends Associable> children)
    {
        OneToManyAssociation parentAssoc = 
            parent.getAssociation(name, OneToManyAssociation.class);
        
        if (parentAssoc == null) {
            throw new IllegalAssociationException(
                "parent '" + parent.toString() + 
                "' has no association named '" + name + "'");
        }
    
        for(Associable child: children) {
            if (child.getAssociation(name) != parentAssoc) {
                throw new IllegalAssociationException(
                    "parent '" + parent.toString() + 
                    "' has no child '" + child.toString() + 
                    "' associated via '" + name + "'");
            }
            
            parentAssoc.removeChild(child);
            child.setAssociation(name, null);
        }
        
        if (parentAssoc.getChildren().isEmpty()) {
            parent.setAssociation(name, null);
        }
    }
    
    public static List<? extends Associable> replaceChild(
        String name, Associable parent, Associable child)
    {
        return replaceChildren(name, parent, child);
    }

    public static List<? extends Associable> replaceChildren(
        String name, Associable parent, Associable... children)
    {
        return replaceChildren(name, parent, Arrays.asList(children));        
    }

    /**
     * Replace the existing children in the parent's name association with
     * those given.  If the parent has no such association, one is created.
     * 
     * @param name association name (see {@link AssociationBase})
     * @param parent parent of the association
     * @param children replacement children for the association
     * @return the children that were replaced
     */
    public static List<? extends Associable> replaceChildren(
        String name, Associable parent, List<? extends Associable> children)
    {
        OneToManyAssociation parentAssoc = 
            parent.getAssociation(name, OneToManyAssociation.class);
        
        if (parentAssoc == null) {
            create(name, parent, children);
            return Collections.emptyList();
        }
        
        ArrayList<Associable> oldChildren = 
            new ArrayList<Associable>(parentAssoc.getChildren());
        for(Associable oldChild: oldChildren) {
            parentAssoc.removeChild(oldChild);
            oldChild.setAssociation(name, null);
        }

        addChildren(name, parent, children);
        
        return oldChildren;
    }
}
