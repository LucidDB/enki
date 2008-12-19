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
package org.eigenbase.enki.hibernate.codegen;

import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;

/**
 * HibernateMofInitHandler extends {@link MofInitHandler} to generate 
 * registrations calls on the {@link HibernateAssociationTypeMapper} for the
 * metamodel.
 * 
 * @author Stephan Zuercher
 */
public class HibernateMofInitHandler extends MofInitHandler
{
    private static final JavaClassReference REF_PACKAGE_CLASS =
        new JavaClassReference(RefPackage.class, false);
    private static final JavaClassReference REF_PACKAGE_BASE_CLASS =
        new JavaClassReference(RefPackageBase.class, false);
    
    private final Map<String, String> typeMap;

    private Generalizes generalizes;
    
    private String initializerPackage;
    
    private List<PackageStitchingDetail> packageStitchingDetails;
    
    public HibernateMofInitHandler(SubordinateHandler subordinateHandler)
    {
        super(subordinateHandler);
        
        this.typeMap = new HashMap<String, String>();
        this.packageStitchingDetails = new ArrayList<PackageStitchingDetail>();
    }

    @Override
    public void beginGeneration()
        throws GenerationException
    {
        ModelPackage modelPackage = 
            (ModelPackage)generator.getRefBaseObject();
        
        for(MofPackage pkg: 
                GenericCollections.asTypedCollection(
                    modelPackage.getMofPackage().refAllOfClass(),
                    MofPackage.class))
        {
            if (isIncluded(pkg)) {
                String pkgName = 
                    TagUtil.getFullyQualifiedPackageName(pkg) + ".init";
                if (initializerPackage == null) {
                    initializerPackage = pkgName;
                } else {
                    if (pkgName.length() < initializerPackage.length()) {
                        initializerPackage = pkgName;
                    }
                }
         
                if (!pluginMode) {
                    continue;
                }
                
                MofPackage owningPackage = (MofPackage)pkg.getContainer();
                if (owningPackage == null) {
                    // REVIEW: SWZ: 2008-12-19: is this a valid case?
                    throw new GenerationException("unowned, included package");
                }
                
                if (isIncluded(owningPackage)) {
                    continue;
                }
                
                PackageStitchingDetail psd = new PackageStitchingDetail();
                
                do {
                    psd.pathToOwningPackage.add(0, owningPackage.getName());
                    
                    owningPackage = (MofPackage)owningPackage.getContainer();
                } while(owningPackage != null);
                
                psd.packageName = pkg.getName();
                psd.packageTypeName = 
                    CodeGenUtils.getTypeName(
                        pkg, 
                        PackageHandler.PACKAGE_SUFFIX 
                        + HibernateJavaHandler.IMPL_SUFFIX);
                
                packageStitchingDetails.add(psd);
            }
        }
        
        if (initializerPackage == null) {
            throw new GenerationException(
                "Unable to find initializer package name; included packages: " 
                + getIncludedPackages());
        }
        
        super.beginGeneration();
        
        generalizes = modelPackage.getGeneralizes();
    }

    
    
    @Override
    public void generateClassInstance(MofClass cls)
        throws GenerationException
    {
        super.generateClassInstance(cls);

        if (cls.isAbstract() || CodeGenUtils.isTransient(cls)) {
            return;
        }
        
        Collection<Attribute> instanceAttributes =
            CodeGenUtils.contentsOfType(
                cls,
                HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                VisibilityKindEnum.PUBLIC_VIS,
                ScopeKindEnum.INSTANCE_LEVEL,
                Attribute.class);
        boolean foundComponentAttrib = false;
        for(Attribute attrib: instanceAttributes) {
            if (!attrib.isDerived() &&
                (!(attrib.getType() instanceof DataType)))
            {
                foundComponentAttrib = true;

                Classifier attribType = attrib.getType();
                if (attribType instanceof AliasType) {
                    attribType = ((AliasType)attribType).getType();
                }
                
                mapAllSubtypes(attribType);
            }
        }
        
        if (foundComponentAttrib) {
            mapAllSubtypes(cls);
        }
    }

    @Override
    public void generateAssociation(Association assoc)
        throws GenerationException
    {
        super.generateAssociation(assoc);
        
        if (CodeGenUtils.isTransient(assoc)) {
            return;
        }
        
        for(AssociationEnd end: CodeGenUtils.getAssociationEnds(assoc)) {
            Classifier type = end.getType();
            if (type instanceof AliasType) {
                type = ((AliasType)type).getType();
            }

            mapAllSubtypes(type);
        }
    }
    
    /**
     * Add type mapping information to {@link #typeMap} for all included,
     * concrete subtypes of <code>type</code>.
     * 
     * @param type a model type
     */
    private void mapAllSubtypes(Classifier type)
    {
        for(Classifier subtype: getAllSubtypes(type)) {
            if (subtype.isAbstract()) {
                continue;
            }

            if (!isIncluded(subtype)) {
                continue;
            }

            String simpleTypeName = CodeGenUtils.getSimpleTypeName(subtype);
            String implTypeName = 
                CodeGenUtils.getTypeName(subtype) + 
                HibernateJavaHandler.IMPL_SUFFIX;
            
            typeMap.put(implTypeName, simpleTypeName);
        }
    }
    
    /**
     * Obtain a collection of all subtypes of <code>type</code>.  The
     * collection includes <code>type</code> itself.
     * 
     * @param type a model type
     * @return a collection of all subtypes of type, in no particular order
     */
    private Collection<Classifier> getAllSubtypes(Classifier type)
    {
        Collection<GeneralizableElement> subtypes = 
            GenericCollections.asTypedCollection(
                generalizes.getSubtype(type), GeneralizableElement.class);
        
        if (subtypes.isEmpty()) {
            return Collections.singleton(type);
        }
        
        Set<Classifier> result = new HashSet<Classifier>();
        
        result.add(type);
        
        for(GeneralizableElement elem: subtypes)
        {
            if (elem instanceof Classifier) {
                type = (Classifier)elem;
                
                result.addAll(getAllSubtypes(type));
            }
        }
        
        return result;
    }

    @Override
    protected void customStitchPackages()
    {
        if (!pluginMode) {
            return;
        }
        
        newLine();
        startBlock(
            "public void stitchPackages(", REF_PACKAGE_CLASS, " topLevelPkg)");

        if (!packageStitchingDetails.isEmpty()) {
            writeln(REF_PACKAGE_CLASS, " pluginPkg;");
            writeln(REF_PACKAGE_BASE_CLASS, " basePkg;");
            for(PackageStitchingDetail psd: packageStitchingDetails) {
                newLine();
                writeln(
                    "assert(topLevelPkg.refMetaObject().refGetValue(", 
                    QUOTE, "name", QUOTE, ").equals(", 
                    QUOTE, psd.pathToOwningPackage.get(0), QUOTE, "));");
                
                writeln(
                    "basePkg = (", REF_PACKAGE_BASE_CLASS, ")topLevelPkg;");
                if (psd.pathToOwningPackage.size() > 1) {
                    for(int i = 1; i < psd.pathToOwningPackage.size(); i++) {
                        writeln(
                            "basePkg = (", 
                            REF_PACKAGE_BASE_CLASS, ")basePkg.refPackage(", 
                            QUOTE, psd.pathToOwningPackage.get(i), QUOTE,
                            ");");
                    }
                }
                
                writeln(
                    "pluginPkg = new " + psd.packageTypeName + "(basePkg);");
                writeln(
                    "basePkg.addPackage(",
                    QUOTE, psd.packageName, QUOTE, ", pluginPkg);");
            }
        }
        
        endBlock();        
    }
    
    @Override
    protected String computeInitializerPackage() throws GenerationException
    {
        return initializerPackage;
    }
    
    private static class PackageStitchingDetail
    {
        private List<String> pathToOwningPackage;
        private String packageName;
        private String packageTypeName;
        
        private PackageStitchingDetail()
        {
            this.pathToOwningPackage = new ArrayList<String>();
        }
    }
}

// End HibernateMofInitHandler.java
