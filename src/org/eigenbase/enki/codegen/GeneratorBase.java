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
package org.eigenbase.enki.codegen;

import java.io.*;
import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.util.*;

/**
 * GeneratorBase is an abstract base class for Enki code generators.
 * It manages an XMI file (the code generator input), an output directory
 * (code generation target) and a flag that controls whether or not
 * Java generic types are used for collections.  In addition, GeneratorBase
 * provides access to model tags and provides utility methods for 
 * determining the correct name for types, parameters, accessor methods, and
 * mutator methods.
 * 
 * @author Stephan Zuercher
 */
public abstract class GeneratorBase implements Generator
{
    protected File xmiFile;
    protected File outputDir;
    
    private Set<RefObject> visited;
    
    /** All configured handlers. */
    private List<Handler> allHandlers;
    
    /** Handlers to be invoked during the current pass. */
    private List<Handler> handlers;
    
    private RefBaseObject refBaseObject;
    
    private int passIndex;
    
    protected GeneratorBase()
    {
        this.visited = new HashSet<RefObject>();
        this.allHandlers = new ArrayList<Handler>();
        this.handlers = new ArrayList<Handler>();
        this.passIndex = -1;
    }

    // implements Generator
    public void setXmiFile(File xmiFile)
    {
        this.xmiFile = xmiFile;
    }

    // implements Generator
    public File getXmiFile()
    {
        return xmiFile;
    }
    
    // implements Generator
    public void setOutputDirectory(File outputDir)
    {
        this.outputDir = outputDir;
    }
    
    // implements Generator
    public String getImplSuffix()
    {
        return "";
    }
    
    // implements Generator
    public void setOptions(Map<String, String> options)
    {
    }
    
    // implements Generator
    public final void addHandler(Handler handler)
    {
        handler.setGenerator(this);
        handler.setOutputDir(outputDir);
        allHandlers.add(handler);
    }
    
    // implements Generator
    public final RefBaseObject getRefBaseObject()
    {
        return refBaseObject;
    }
    
    protected void visitRefBaseObject(RefBaseObject obj)
    throws GenerationException
    {
        this.refBaseObject = obj;
        
        int numPasses = 1;
        for(Handler handler: allHandlers) {
            numPasses = Math.max(numPasses, handler.getNumPasses());
        }
        
        invokeGenerationStart();
        
        boolean throwing = true;
        try {
            for(int i = 0; i < numPasses; i++) {
                passIndex = i;
                
                // Set up for this pass.
                visited.clear();
                handlers.clear();
                for(Handler handler: allHandlers) {
                    if (i < handler.getNumPasses()) {
                        handlers.add(handler);
                    }
                }
                
                invokeBeginPass();
                
                if (obj instanceof RefPackage) {
                    visitRefPackage((RefPackage)obj);
                } else if (obj instanceof RefObject) {
                    visitRefObject((RefObject)obj);
                } else if (obj instanceof RefAssociation) {
                    visitRefAssociation((RefAssociation)obj);
                } else if (obj instanceof RefClass) {
                    visitRefClass((RefClass)obj);
                }
                
                invokeEndPass();
            }
            throwing = false;
        } finally {
            invokeGenerationEnd(throwing);
        }
    }

    protected void visitRefClass(RefClass cls) throws GenerationException
    {
        if (cls.refImmediatePackage() instanceof ModelPackage) {
            for(Object e: cls.refAllOfClass()) {
                RefObject obj = (RefObject)e;
                
                visitRefObject(obj);
            }
        }
    }

    protected void visitRefAssociation(RefAssociation assoc) 
    throws GenerationException
    {
        visitRefObject(assoc.refMetaObject());
    }
    
    protected void visitRefObject(RefObject obj) throws GenerationException
    {
        if (!generateObject(obj)) {
            return;
        }
        
        Namespace outermost = (Namespace)obj;
        while(true) {
            Namespace container = outermost.getContainer();
            if (container == null) {
                break;
            }
            outermost = container;
        }

        if (!(outermost instanceof MofPackage)) {
            return;
        }
        
        String ignoreLifecycleString =
            TagUtil.getTagValue(outermost, TagUtil.TAGID_IGNORE_LIFECYCLE);
        boolean ignoreLifecycle = Boolean.parseBoolean(ignoreLifecycleString);

        if (obj instanceof Association) {
            if (!ignoreLifecycle) {
                invokeAssociationTemplate((Association)obj);
            }
        } else if (obj instanceof MofClass || obj instanceof MofPackage) {
            GeneralizableElement elm = (GeneralizableElement)obj;
            visited.add(elm);
            
            for(Object superType: elm.getSupertypes()) {
                visitRefObject((RefObject)superType);
            }
            
            Collection<?> contents = elm.getContents();
            for(Object content: contents) {
                visitRefObject((RefObject)content);
            }

            if (elm instanceof MofPackage) {
                if (!ignoreLifecycle) {
                    invokePackageTemplate((MofPackage)elm);
                }
            } else {
                invokeClassInstanceTemplate((MofClass)elm);

                if (!ignoreLifecycle) {
                    invokeClassProxyTemplate((MofClass)elm);
                }
            }
        } else if (obj instanceof EnumerationType) {
            EnumerationType et = (EnumerationType)obj;

            invokeEnumerationInterfaceTemplate(et);
            invokeEnumerationClassTemplate(et);
        } else if (obj instanceof StructureType) {
            invokeStructureTemplate((StructureType)obj);
        } else if (obj instanceof MofException) {
            invokeExceptionTemplate((MofException)obj);
        } else {
//            throw new GenerationException(
//                "unknown type '" + obj.getClass() + "'");
        }

    }

    protected void visitRefPackage(RefPackage pkg) throws GenerationException
    {
        if (pkg instanceof ModelPackage) {
            ModelPackage modelPkg = (ModelPackage)pkg;

            // To deal with the general case of a graph of package imports, we
            // order the visit calls via preorder BFS from the roots.  This
            // allows the generator implementation to rely on the guarantee
            // that before visiting any imported package, at least one of the
            // packages which imports it is visited first.  (Not all of them;
            // that would be a topological sort.)  The Hibernate generator
            // relies on this for designating the first such importing
            // package as the "primary" for building a spanning tree.
            // Dude, where's my JGraphT?

            // Note that this does not currently do anything special for normal
            // package containment (which in fact comes out postorder
            // because of the call ordering in visitRefObject).  In fact,
            // there's an assumption here that a package is reachable either by
            // containment or by (possibly multiple) imports, but not both
            // (if this assumption doesn't hold, the package will be
            // generated twice).

            // First, we need to know which packages are imported
            // so that we don't treat them as roots.
            Set<Namespace> clusteredNamespaces = new HashSet<Namespace>();
            for (Object o : modelPkg.getImport().refAllOfClass()) {
                Import imp = (Import) o;
                if (imp.isClustered()) {
                    clusteredNamespaces.add(imp.getImportedNamespace());
                }
            }

            // Data structures for primitive BFS.  (Use LinkedList
            // instead of Queue interface to avoid JRockit bug.)
            LinkedList<MofPackage> queue = new LinkedList<MofPackage>();
            Set<MofPackage> visitedPackages = new HashSet<MofPackage>();

            // Find the roots and queue them up.
            for (Object e: modelPkg.getMofPackage().refAllOfClass()) {
                MofPackage elem = (MofPackage)e;
                
                if ((elem.getContainer() == null)
                    && !clusteredNamespaces.contains(elem)) {
                    queue.add(elem);
                }
            }

            // Main BFS loop.  Note that we don't traverse containment edges
            // here (that happens implicitly in visitRefObject).
            while (!queue.isEmpty()) {
                MofPackage currPkg = queue.poll();
                if (!visitedPackages.contains(currPkg)) {
                    visitedPackages.add(currPkg);
                    visitRefObject(currPkg);
                } else {
                    continue;
                }

                // REVIEW jvs 1-Jul-2008:  do we need to care about
                // import/package visibility here?

                // Traverse import edges.
                for (Object o : currPkg.getContents()) {
                    if (o instanceof Import) {
                        Import imp = (Import) o;
                        if (imp.isClustered()) {
                            Namespace ns = imp.getImportedNamespace();
                            if (ns instanceof MofPackage) {
                                MofPackage impPkg = (MofPackage) ns;
                                queue.add(impPkg);
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean generateObject(RefObject obj)
    {
        if (visited.contains(obj) || !(obj instanceof Namespace)) {
            return false;
        }
        
        return true;
    }

    private void invokeGenerationStart() throws GenerationException
    {
        for(Handler h: allHandlers) {
            h.beginGeneration();
        }
    }
    
    private void invokeGenerationEnd(boolean throwing) 
    throws GenerationException
    {
        for(Handler h: allHandlers) {
            h.endGeneration(throwing);
        }
    }
    
    private void invokeBeginPass() throws GenerationException
    {
        for(Handler h: handlers) {
            h.beginPass(passIndex);
        }
    }
    
    private void invokeEndPass() throws GenerationException
    {
        for(Handler h: handlers) {
            h.endPass(passIndex);
        }
    }

    private void invokeAssociationTemplate(Association assoc) 
        throws GenerationException
    {
        for(AssociationHandler h: handlersOfType(AssociationHandler.class)) {
            h.generateAssociation(assoc);
        }
    }
    
    private void invokePackageTemplate(MofPackage pkg) 
        throws GenerationException
    {
        for(PackageHandler h: handlersOfType(PackageHandler.class)) {
            h.generatePackage(pkg);
        }
    }

    private void invokeClassInstanceTemplate(MofClass cls)
        throws GenerationException
    {
        for(ClassInstanceHandler h: handlersOfType(ClassInstanceHandler.class))
        {
            h.generateClassInstance(cls);
        }
    }

    private void invokeClassProxyTemplate(MofClass cls)
        throws GenerationException
    {
        for(ClassProxyHandler h: 
            handlersOfType(ClassProxyHandler.class))
        {
            h.generateClassProxy(cls);
        }
    }

    private void invokeEnumerationInterfaceTemplate(EnumerationType enm)
        throws GenerationException
    {
        for(EnumerationInterfaceHandler h: 
            handlersOfType(EnumerationInterfaceHandler.class))
        {
            h.generateEnumerationInterface(enm);
        }
    }

    private void invokeEnumerationClassTemplate(EnumerationType enm)
        throws GenerationException
    {
        for(EnumerationClassHandler h: 
            handlersOfType(EnumerationClassHandler.class))
        {
            h.generateEnumerationClass(enm);
        }
    }


    private void invokeStructureTemplate(StructureType struct)
        throws GenerationException
    {
        for(StructureHandler h: handlersOfType(StructureHandler.class)) {
            h.generateStructure(struct);
        }
    }

    private void invokeExceptionTemplate(MofException ex)
        throws GenerationException
    {
        for(ExceptionHandler h: handlersOfType(ExceptionHandler.class)) {
            h.generateException(ex);
        }
    }

    private <E> Collection<E> handlersOfType(
        final Class<E> cls)
    {
        assert(Handler.class.isAssignableFrom(cls));
        
        return new AbstractCollection<E>() {
            @Override
            public Iterator<E> iterator()
            {
                return new Iterator<E>() {
                    Iterator<Handler> iter = handlers.iterator();
                    Class<E> type = cls;
                    E next = null;
                    
                    public boolean hasNext()
                    {
                        if (next != null) {
                            return true;
                        }
                        
                        while(iter.hasNext()) {
                            Handler candidate = iter.next();
                            if (type.isInstance(candidate)) {
                                next = type.cast(candidate);
                                return true;
                            }
                        }
                        
                        return false;
                    }

                    public E next()
                    {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        
                        E result = next;
                        next = null;
                        return result;
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                    
                };
            }

            @Override
            public int size()
            {
                int size = 0;
                for(Iterator<E> iter = iterator(); iter.hasNext(); ) {
                    iter.next();
                    size++;
                }
                return size;
            }
        };
    }
        
    public String transformIdentifier(String identifier)
    {
        return identifier;
    }
}
