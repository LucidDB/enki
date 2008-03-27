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
package org.eigenbase.enki.util;

import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.jmi.impl.*;
import org.netbeans.lib.jmi.util.*;

/**
 * TagUtil provides commonly used {@link Tag} lookups and other utility
 * methods.
 * 
 * @author Stephan Zuercher
 */
public class TagUtil
{
    public static final String TAGID_IGNORE_LIFECYCLE = 
        "javax.jmi.ignoreLifecycle";
    public static final String TAGID_PACKAGE_PREFIX = 
        "javax.jmi.packagePrefix";
    public static final String TAGID_SUBSTITUTE_NAME = 
        "javax.jmi.substituteName";

    private TagUtil()
    {
    }
    
    public static String mapEnumLiteral(String literal)
    {
        return StringUtil.mangleIdentifier(
            literal, StringUtil.IdentifierType.ALL_CAPS);
    }
    
    public static String getSubstName(ModelElement modelElement)
    {
        // Note this is very similar, but not identical to 
        // Generator.getSimpleTypeName()
        
        String name = getTagValue(modelElement, TAGID_SUBSTITUTE_NAME);
        if (name == null) {
            name = modelElement.getName();
        }
        if (modelElement instanceof Constant) {
            name = 
                StringUtil.mangleIdentifier(
                    name, StringUtil.IdentifierType.ALL_CAPS);
        } else if (modelElement instanceof MofClass ||
                   modelElement instanceof MofPackage ||
                   modelElement instanceof Association ||
                   modelElement instanceof MofException ||
                   modelElement instanceof StructureType ||
                   modelElement instanceof EnumerationType ||
                   modelElement instanceof CollectionType ||
                   modelElement instanceof Import)
        {
            name =
                StringUtil.mangleIdentifier(
                    name, StringUtil.IdentifierType.CAMELCASE_INIT_UPPER);
        } else {
            name = 
                StringUtil.mangleIdentifier(
                    name, StringUtil.IdentifierType.CAMELCASE_INIT_LOWER);
        }

        if (modelElement instanceof MofException && 
            !name.endsWith("Exception"))
        {
            name = name + "Exception";
        }
        
        return name;
    }
 
    /**
     * Retrieves the first value for the {@link Tag} instance associated with
     * the given model element and tag identifier.
     * 
     * @param modelElement model element
     * @param tagId tag identifier
     * @return the first value associated with tag or null if none found
     */
    public static String getTagValue(ModelElement modelElement, String tagId)
    {
        List<String> values = getTagValues(modelElement, tagId);
        if (values.isEmpty()) {
            return null;
        }
        
        return values.get(0);
    }
    
    /**
     * Retrieves the values for the {@link Tag} instance associated with
     * the given model element and tag identifier.  Effectively shorthand for 
     * <code>getTag(modelElement, tagId).getValues()</code>.
     * 
     * @param modelElement model element
     * @param tagId tag identifier
     * @return the values for the tag or an empty list
     * @see #getTag(ModelElement, String)
     */
    public static List<String> getTagValues(
        ModelElement modelElement, String tagId)
    {
        Tag tag = getTag(modelElement, tagId);
        if (tag == null) {
            return Collections.emptyList();
        }
        
        List<?> values = tag.getValues();
        
        return GenericCollections.asTypedList(values, String.class);
    }
    
    /**
     * Retrieves the {@link Tag} instance with the given identifier associated
     * with the given {@link ModelElement}.
     * 
     * @param modelElement model element
     * @param tagId tag identifier
     * @return the Tag instance for the model element and id or null
     */
    public static Tag getTag(ModelElement modelElement, String tagId)
    {
        AttachesTo attachesTo = 
            ((ModelPackage)modelElement.refImmediatePackage())
            .getAttachesTo();
        
        Collection<Tag> tags =
            GenericCollections.asTypedCollection(
                attachesTo.getTag(modelElement), Tag.class);
        
        for(Tag tag: tags) {
            if (tag.getTagId().equals(tagId)) {
                return tag;
            }
        }
        
        return null;
    }
    
    /**
     * Retrieves the fully qualified name of the generated interface that
     * represents the given ModelElement.
     * 
     * @param refClass a JMI reflective class
     * @return fully-qualified interface name for the model element
     */
    public static String getInterfaceFullName(RefClass refClass)
    {
        if (refClass instanceof RefClassBase) {
            Class<?>[] interfaces = refClass.getClass().getInterfaces();
            assert(interfaces.length == 1);
            
            String name = interfaces[0].getName();
            
            return name.substring(0, name.length() - 5);
        } else {
            // Delegate to Netbeans
            TagProvider tagProvider = new TagProvider();
            
            String name = tagProvider.getImplFullName(
                (ModelElement)refClass.refMetaObject(), 
                TagProvider.INSTANCE);
            assert(name.endsWith("Impl"));
            name = name.substring(0, name.length() - 4);
            
            // hack for MDR MOF implementation
            name =
                name.replaceFirst(
                    "org\\.netbeans\\.jmiimpl\\.mof",
                    "javax.jmi");
            
            return name;
        }
    }
    
    private static MofPackage getBasePackage(ModelPackage modelPackage)
    {
        Collection<?> pkgs = modelPackage.getMofPackage().refAllOfType();
        for(MofPackage pkg: 
                GenericCollections.asTypedCollection(pkgs, MofPackage.class))
        {
            // Ignore these ancillary types.
            if (pkg.getName().equals("PrimitiveTypes") ||
                pkg.getName().equals("CorbaIdlTypes"))
            {
                continue;
            }
         
            if (pkg.getContainer() != null) {
                continue;
            }

            return pkg;
        }
        
        return null;
    }
    
    public static String getFullyQualifiedPackageName(ModelPackage modelPackage)
    {
        MofPackage pkg = getBasePackage(modelPackage);
        
        return getFullyQualifiedPackageName(pkg);
    }
    
    public static String getFullyQualifiedPackageName(MofPackage pkg)
    {
        String prefixTagValue = 
            TagUtil.getTagValue(pkg, "javax.jmi.packagePrefix");
        String basePkgName = pkg.getName();
        
        String packageName;
        if (prefixTagValue != null) {
            packageName = 
                prefixTagValue + 
                "." + 
                basePkgName.toLowerCase(Locale.US);
        } else {
            if (pkg.getContainer() != null) {
                packageName = 
                    getFullyQualifiedPackageName(
                        (MofPackage)pkg.getContainer()) + "." + 
                        basePkgName.toLowerCase(Locale.US);
            } else {
                packageName = basePkgName.toLowerCase(Locale.US);
            }
        }
        
        return packageName;
    }
    
    // REVIEW: SWZ: 2008-03-25: Decide whether this code belongs here or in
    // CodeGenUtils and refactor as necessary.
    
    /**
     * Returns the maximum length for a given attribute in a given class.
     * If no tag specifies an alternative maximum length, returns the
     * given default.
     * 
     * See {@link CodeGenUtils#findMaxLengthTag(
     *          Classifier, Attribute, int, java.util.logging.Logger)}
     * 
     * @param cls object's class
     * @param attrib object's attribute
     * @param defaultMaxLength default maximum length
     * @return max length for the class/attribute combination
     */
    public static int findMaxLengthTag(
        Classifier cls, 
        Attribute attrib, 
        int defaultMaxLength)
    {
        return CodeGenUtils.findMaxLengthTag(
            cls, attrib, defaultMaxLength, null);
    }
}

// End TagUtils.java
