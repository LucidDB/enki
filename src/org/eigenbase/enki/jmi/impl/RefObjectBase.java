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
    public static final HashSet<String> ALL_DEP_KINDS = 
        new HashSet<String>(
            Arrays.asList(
                ModelElement.CONSTRAINTDEP,
                ModelElement.CONTAINERDEP,
                ModelElement.CONSTRAINEDELEMENTSDEP,
                ModelElement.SPECIALIZATIONDEP,
                ModelElement.IMPORTDEP,
                ModelElement.CONTENTSDEP,
                ModelElement.SIGNATUREDEP,
                ModelElement.TAGGEDELEMENTSDEP,
                ModelElement.TYPEDEFINITIONDEP,
                ModelElement.REFERENCEDENDSDEP));

    private RefClass refClass;
    
    protected RefObjectBase(RefClass refClass)
    {
        super();
        
        this.refClass = refClass;
        getCurrentInitializer().register(this);
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
        
        // This is cheating:  The only composite aggregation in the M3 is
        // Contains, so just return the outermost container if this is a 
        // ModelElement and null otherwise.
        if (this instanceof ModelElement) {
            ModelElement modelElement = (ModelElement)this;
            Namespace container = modelElement.getContainer();
            if (container != null) {
                return container.refOutermostComposite();
            }
        }
        
        // Either not a ModelElement (in which case no composite) or a
        // ModelElement not contained (e.g. top-most container).  Semantics
        // of this method say return "this".
        return this;
    }
    
    // Implement ModelElement
    protected List<String> getQualifiedName()
    {
        assert(this instanceof ModelElement);

        ArrayList<String> qualifiedName = new ArrayList<String>();
        
        ModelElement modelElement = (ModelElement)this;
        qualifiedName.add(modelElement.getName());
        
        Namespace ns = modelElement.getContainer();
        while(ns != null) {
            qualifiedName.add(ns.getName());
            ns = ns.getContainer();
        }
        
        Collections.reverse(qualifiedName);
        
        return qualifiedName;
    }
    
    // Implement ModelElement
    protected Collection<ModelElement> findRequiredElements(
        Collection<String> kinds,
        boolean recursive)
    {
        assert(this instanceof ModelElement);
        
        if (kinds.contains(ModelElement.ALLDEP)) {
            kinds = ALL_DEP_KINDS;
        }
        
        if (recursive) {
            HashSet<ModelElement> seen = new HashSet<ModelElement>();
            seen.add((ModelElement)this);
            recursiveFindDeps(kinds, seen);
            return seen;
        } else {
            HashSet<ModelElement> deps = new HashSet<ModelElement>();
            for(String kind: kinds) {
                Collection<ModelElement> depsOfKind = findDepsOfKind(kind);
                deps.addAll(depsOfKind);
            }
            return deps;
        }
    }

    // Implement ModelElement
    protected boolean isRequiredBecause(ModelElement other, String[] reason)
    {
        assert(this instanceof ModelElement);
        // OUT_DIR param of multiplicity 1
        assert(reason != null && reason.length == 1);

        for(String kind: ALL_DEP_KINDS) {
            if (isDepOfKind(kind, other)) {
                reason[0] = kind;
                return true;
            }
        }
        
        if (!findRequiredElements(ALL_DEP_KINDS, true).isEmpty()) {
             reason[0] = ModelElement.INDIRECTDEP;
             return true;
        }
        
        reason[0] = "";
        return false;
    }

    // Implement ModelElement
    protected boolean isFrozen()
    {
        assert(this instanceof ModelElement);
        
        return true;
    }

    // Implement ModelElement
    protected boolean isVisible(ModelElement otherElement)
    {
        assert(this instanceof ModelElement);
        
        // JMI spec says this is reserved for future use.  Return true for 
        // now.
        return true;
    }

    // Implement Namespace
    protected ModelElement lookupElement(String name)
    throws NameNotFoundException
    {
        assert(this instanceof Namespace);
        
        for(Object o: ((Namespace)this).getContents()) {
            ModelElement modelElement = (ModelElement)o;
            
            if (modelElement.getName().equals(name)) {
                return modelElement;
            }
        }
        
        throw new NameNotFoundException(name);
    }

    // Implement Namespace
    protected ModelElement resolveQualifiedName(List<?> qualifiedName)
    throws NameNotResolvedException
    {
        assert(this instanceof Namespace);

        if (qualifiedName == null || qualifiedName.isEmpty()) {
            throw new NameNotResolvedException(
                "no qualified name", qualifiedName);
        }

        Namespace ns = (Namespace)this;
        
        NAME_LOOP:
        for(int i = 0; i < qualifiedName.size(); i++) {
            String name = qualifiedName.get(i).toString();
            
            for(Object o: ns.getContents()) {
                if (o instanceof ModelElement) {
                    ModelElement modelElement = (ModelElement)o;
                    if (name.equals(modelElement.getName())) {
                        if (i + 1 >= qualifiedName.size()) {
                            // Found a matching model element and there's no
                            // more work to do.
                            return modelElement;
                        } else if (modelElement instanceof Namespace) {
                            // Found a matching Namespace, continue to the
                            // next qualified name element.
                            ns = (Namespace)modelElement;
                            continue NAME_LOOP;
                        }
                        
                        throw new NameNotResolvedException(
                            modelElement.getName() + " is not a Namespace", 
                            qualifiedName.subList(i, qualifiedName.size()));
                    }
                }
            }
            
            throw new NameNotResolvedException(
                name + " not found",
                qualifiedName.subList(i, qualifiedName.size()));
        }

        // Should be possible to get here.
        throw new NameNotResolvedException("internal error", qualifiedName);
    }

    // Implement Namespace
    protected List<ModelElement> findElementsByType(
        MofClass ofType,
        boolean includeSubtypes)
    {
        assert(this instanceof Namespace);

        ArrayList<ModelElement> result = new ArrayList<ModelElement>();
        
        for(Object o: ((Namespace)this).getContents()) {
            ModelElement modelElem = (ModelElement)o;
            
            if (modelElem.refIsInstanceOf(ofType, includeSubtypes)) {
                result.add(modelElem);
            }
        }

        return result;
    }

    // Implement Namespace
    protected boolean nameIsValid(String proposedName)
    {
        assert(this instanceof Namespace);
        
        Namespace ns = (Namespace)this;
        
        Collection<ModelElement> extendedNamespace = extendedNamespace(ns);

        for(ModelElement modelElement: extendedNamespace) {
            if (modelElement.getName().equals(proposedName)) {
                return false;
            }
        }
        
        return true;
    }

    // Implement GeneralizableElement
    protected List<GeneralizableElement> allSupertypes()
    {
        assert(this instanceof GeneralizableElement);
        
        LinkedHashSet<GeneralizableElement> supertypes = 
            new LinkedHashSet<GeneralizableElement>();

        addAllSupertypes((GeneralizableElement)this, supertypes);
        
        // Convert to List
        return new ArrayList<GeneralizableElement>(supertypes);
    }
    
    // Implement GeneralizableElement
    protected ModelElement lookupElementExtended(String name) 
    throws NameNotFoundException
    {
        assert(this instanceof GeneralizableElement);
        
        for(ModelElement modelElement: extendedNamespace((Namespace)this)) {
            if (modelElement.getName().equals(name)) {
                return modelElement;
            }
        }
        
        throw new NameNotFoundException(name);
    }
    
    // Implement GeneralizableElement
    protected java.util.List<ModelElement> findElementsByTypeExtended(
        MofClass ofType, boolean includeSubtypes)
    {
        assert(this instanceof GeneralizableElement);

        ArrayList<ModelElement> result = new ArrayList<ModelElement>();
        
        for(ModelElement modelElem : extendedNamespace((Namespace)this)) {
            if (modelElem.refIsInstanceOf(ofType, includeSubtypes)) {
                result.add(modelElem);
            }
        }

        return result;
    }
    
    protected AssociationEnd otherEnd()
    {
        assert(this instanceof AssociationEnd);

        AssociationEnd thisEnd = (AssociationEnd)this;
        
        Namespace container = thisEnd.getContainer();
        for(Object o: container.getContents()) {
            if (o instanceof AssociationEnd && o != thisEnd) {
                return (AssociationEnd)o;
            }
        }
        
        // All associations have two ends, so this shouldn't be reachable
        throw new InternalJmiError("Association must have an 'other end'");
    }

    private static void addAllSupertypes(
        GeneralizableElement type, LinkedHashSet<GeneralizableElement> supertypes)
    {
        for(Object o: type.getSupertypes()) {
            GeneralizableElement genElem = (GeneralizableElement)o;
            
            addAllSupertypes(genElem, supertypes);
            supertypes.add(genElem);
        }
    }
    
    private Collection<ModelElement> extendedNamespace(Namespace ns)
    {
        LinkedHashSet<ModelElement> extendedNamespace = 
            new LinkedHashSet<ModelElement>();
        
        MofPackage mofPkg = null;
        GeneralizableElement genElem = null;
        if (this instanceof GeneralizableElement) {
            genElem = (GeneralizableElement)this;

            if (this instanceof MofPackage) {
                mofPkg = (MofPackage)this;
            }
        }
        
        for(Object containedElemObj: ns.getContents()) {
            ModelElement containedElem = (ModelElement)containedElemObj;
        
            extendedNamespace.add(containedElem);

            if (mofPkg != null && containedElem instanceof Import) {
                Import imp = (Import)containedElem;
                
                extendedNamespace.add(imp.getImportedNamespace());
            }
        }
        
        if (genElem != null) {
            for(Object supertypeObj: genElem.allSupertypes()) {
                Namespace supertype = (Namespace)supertypeObj;
                
                for(Object containedElemObj: supertype.getContents()) {
                    ModelElement containedElem = (ModelElement)containedElemObj;
                    
                    extendedNamespace.add(containedElem);
                    
                    if (mofPkg != null && containedElem instanceof Import) {
                        Import imp = (Import)containedElem;
                        
                        extendedNamespace.add(imp);
                    }
                }
            }
        }

        return extendedNamespace;
    }
    
    void recursiveFindDeps(Collection<String> kinds, Set<ModelElement> seen)
    {
        HashSet<ModelElement> seen2 = new HashSet<ModelElement>();
        
        for(String kind: kinds) {
            Collection<ModelElement> depsOfKind = findDepsOfKind(kind);
            seen2.addAll(depsOfKind);
        }
        
        // MOF spec has an "if seen = seen2".  Instead, just add all of seen2
        // into seen and recurse if there are new elements.
        for(Iterator<ModelElement> iter = seen2.iterator(); iter.hasNext(); ) {
            ModelElement elem = iter.next();
            
            if (!seen.add(elem)) {
                // Seen already contained elem.
                iter.remove();
            }
        }
        
        if (!seen2.isEmpty()) {
            for(ModelElement elem: seen2) {
                ((RefObjectBase)elem).recursiveFindDeps(kinds, seen);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private Collection<ModelElement> findDepsOfKind(String kind)
    {
        HashSet<ModelElement> result = new HashSet<ModelElement>();

        if (kind.equals(ModelElement.CONSTRAINTDEP)) {
            result.addAll(((ModelElement)this).getConstraints());
        } else if (kind.equals(ModelElement.CONTAINERDEP)) {
            result.add(((ModelElement)this).getContainer());
        } else if (kind.equals(ModelElement.CONSTRAINEDELEMENTSDEP) && 
                   (this instanceof Constraint))
        {
            result.addAll(((Constraint)this).getConstrainedElements());
        } else if (kind.equals(ModelElement.SPECIALIZATIONDEP) && 
                   (this instanceof GeneralizableElement))
        {
            result.addAll(((GeneralizableElement)this).getSupertypes());
        } else if (kind.equals(ModelElement.IMPORTDEP) && 
                   (this instanceof Import))
        {
            result.add(((Import)this).getImportedNamespace());
        } else if (kind.equals(ModelElement.CONTENTSDEP) && 
                   (this instanceof Namespace))
        {
            result.addAll(((Namespace)this).getContents());
        } else if (kind.equals(ModelElement.SIGNATUREDEP) &&
                   (this instanceof Operation))
        {
            result.addAll(((Operation) this).getExceptions());
        } else if (kind.equals(ModelElement.TAGGEDELEMENTSDEP) && 
                   (this instanceof Tag))
        {
            result.addAll(((Tag)this).getElements());
        } else if (kind.equals(ModelElement.TYPEDEFINITIONDEP) &&
                   (this instanceof TypedElement))
        {
            result.add(((TypedElement)this).getType());
        } else if (kind.equals(ModelElement.REFERENCEDENDSDEP) &&
                   (this instanceof Reference))
        {
            result.add(((Reference)this).getReferencedEnd());
            result.add(((Reference)this).getExposedEnd());
        }

        result.remove(null);
        
        return result;
    }
    
    private boolean isDepOfKind(String kind, ModelElement other)
    {
        return findDepsOfKind(kind).contains(other);
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
