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
package org.eigenbase.enki.jmi.impl;

import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;

/**
 * RefObjectBase is a base class for {@link RefObject} implementations.
 * It provides implementations of various MOF mode 
 * {@link Operation Operations}.  These methods are not useful in for 
 * generic storage implementations, although the JMI methods may be.
 * 
 * @author Stephan Zuercher
 */
public abstract class RefObjectBase 
    extends RefFeaturedBase 
    implements RefObject
{
    private RefClass refClass;

    protected RefObjectBase(RefClass refClass)
    {
        super();
        
        this.refClass = refClass;
        MetamodelInitializer currInitializer = getCurrentInitializer();
        if (currInitializer != null) {
            currInitializer.register(this);
        }
    }
    
    protected RefObjectBase()
    {
        super();
    }
    
    protected RefObjectBase(MetamodelInitializer initializer)
    {
        super(initializer);
    }
    
    // Implement RefObjectBaseObject/RefObjectBase
    public RefPackage refImmediatePackage()
    {
        logJmi("refImmediatePackage");
        
        return refClass().refImmediatePackage();
    }
    
    public RefClass refClass()
    {
        logJmi("refClass");
        
        return refClass;
    }

    @Override
    public RefObject refMetaObject()
    {
        logJmi("refMetaObject");
        
        return refClass().refMetaObject();
    }
    
    @Override
    public void setRefMetaObject(RefObject metaObj)
    {
        throw new UnsupportedOperationException();
    }
    
    public void refDelete()
    {
        logJmi("refDelete");
        
        throw new UnsupportedOperationException("RefObject.refDelete()");
    }
    
    /**
     * Unregisters this object.  Must be called explicitly by generated code
     * to avoid allowing deletion of metamodel objects, which is not
     * supported.
     */
    protected void unregister()
    {
        ((RefClassBase)refClass()).unregister(this);
    }

    public boolean refIsInstanceOf(RefObject objType, boolean considerSubtypes)
    {
        logJmi("refIsInstanceOf");

        if (refClass().refMetaObject().equals(objType)) {
            return true;
        }

        if (!considerSubtypes) {
            return false;
        }

        // Consider all our super types.  If one of them is objType, then
        // this is "an object whose class is a subclass of the class
        // described by objType" and we return true.
        GeneralizableElement thisGenElem = 
            (GeneralizableElement)this.refMetaObject();
        if (thisGenElem.allSupertypes().contains(objType)) {
            return true;
        }
        
        return false;
    }

    public RefFeatured refImmediateComposite()
    {
        logJmi("refImmediateComposite");
        
        // This is cheating:  The only composite aggregation in the M3 is
        // Contains, so just return the container if this is a ModelElement
        // and null otherwise.
        if (this instanceof ModelElement) {
            return ((ModelElement)this).getContainer();
        }
        
        return null;
    }

    public RefFeatured refOutermostComposite()
    {
        logJmi("refOutermostComposite");

        RefFeatured immediateComposite = refImmediateComposite();
        if (immediateComposite == null) {
            return this;
        } else if (immediateComposite instanceof RefObject) {
            return ((RefObject)immediateComposite).refOutermostComposite();
        }
        
        // must be a RefClass
        return immediateComposite;
    }
    
    @Override
    public EnkiMDRepository getRepository()
    {
        return ((RefClassBase)refClass()).getRepository();
    }
    
    /**
     * Helper method for {@link #checkConstraints(List, boolean)} 
     * implementations.  Finds the given Attribute in this object's 
     * inheritance hierarchy.
     * 
     * @param name name of the attribute
     * @return the Attribute representing the attribute or null if not found
     */
    protected Attribute findAttribute(String name)
    {
        // REVIEW: SWZ: 2008-04-14. This originally used Queue<Classifier>
        // with calls to offer() and poll(), but the same pattern in another
        // class (CodeGenUtils) was shown to throw spurious exceptions on
        // JRockit 27.4 (JVM bug reported).  Pre-emptively modified this code
        // to use add/removeFirst, which seems to work.
        Classifier startCls = (Classifier)refClass().refMetaObject();
        
        LinkedList<Classifier> queue = new LinkedList<Classifier>();
     
        queue.add(startCls);
        
        while(!queue.isEmpty()) {
            Classifier cls = queue.removeFirst();
            
            Collection<ModelElement> elements = 
                GenericCollections.asTypedCollection(
                    cls.getContents(), ModelElement.class);
            for(ModelElement elem: elements) {
                if (elem instanceof Attribute) {
                    if (elem.getName().equals(name)) {
                        return (Attribute)elem;
                    }
                }
            }
                    
            List<Classifier> supertypes = 
                GenericCollections.asTypedList(
                    cls.getSupertypes(), Classifier.class);
            for(Classifier supertype: supertypes) {
                queue.add(supertype);
            }
        }

        return null;
    }

    protected AssociationEnd findAssociationEnd(
        String assocName, String endName)
    {
        // REVIEW: SWZ: 3/17/08: This only works for the MOF model (where
        // all the associations are in one package).  In general, an 
        // association need not live in an immediate package of either end's
        // class.
        RefPackage refPkg = refClass().refImmediatePackage();
        
        do {
            try {
                RefAssociation refAssoc = refPkg.refAssociation(assocName);
                Association assoc = (Association)refAssoc.refMetaObject();
                
                Collection<ModelElement> contents = 
                    GenericCollections.asTypedCollection(
                        assoc.getContents(), ModelElement.class);
                for(ModelElement elem: contents) {
                    if (elem instanceof AssociationEnd) {
                        if (elem.getName().equals(endName)) {
                            return (AssociationEnd)elem;
                        }
                    }
                }
            } catch(InvalidNameException e) {
                // Ignored -- we'll try the parent package.
            }
            
            refPkg = refPkg.refImmediatePackage();
        } while(refPkg != null);
        
        return null;
    }
}

// End RefObjectBase.java
