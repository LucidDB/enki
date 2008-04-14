/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007-2008 The Eigenbase Project
// Copyright (C) 2007-2008 Disruptive Tech
// Copyright (C) 2007-2008 LucidEra, Inc.
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

import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;

import org.eigenbase.enki.util.*;

/**
 * CodeGenUtils contains utilities for generating Hibernate model
 * code.
 * 
 * @author Stephan Zuercher
 */
public class CodeGenUtils
{
    /** 
     * Tag identifier for a custom Enki tag to override the default column 
     * length for a particular string attribute.  The tag identifier is
     * {@value}.
     */
    public static final String MAX_LENGTH_TAG_NAME = 
        "org.eigenbase.enki.maxLength";

    /**
     * Tag value for {@link #MAX_LENGTH_TAG_NAME} that indicates no limit.
     * The tag value is {@value}.
     */
    public static final String MAX_LENGTH_UNLIMITED_VALUE = "unlimited";
    
    /**
     * Default length for string attributes.  Should be passed as the
     * <code>defaultMaxLength</code> parameter to 
     * {@link #findMaxLengthTag(Classifier, Attribute, int, Logger)} unless
     * the user has specified an alternative.  The default is {@value}.
     */
    public static final int DEFAULT_STRING_LENGTH = 128;
    
    /**
     * Tag identifier for a custom Enki tag to control whether a package is
     * considered transient.  The tag identifier is {@value}.
     */
    public static final String TRANSIENT_PKG_TAG_NAME = 
        "org.eigenbase.enki.transientPackage";
    
    private CodeGenUtils()
    {
    }

    /**
     * Determine whether the given {@link ModelElement} is transient.  Only
     * {@link MofPackage} instances may be transient.  A MofPackage is 
     * considered transient if it (or one of its containers) contains a 
     * {@link Tag} identified by {@value #TRANSIENT_PKG_TAG_NAME}.  The Tag's
     * value is ignored.
     * 
     * @param modelElement any model element 
     * @return true if the model element is a member of a transient package
     */
    public static boolean isTransient(ModelElement modelElement)
    {
        if (modelElement instanceof MofPackage) {
            String value = 
                TagUtil.getTagValue(modelElement, TRANSIENT_PKG_TAG_NAME);
            if (value != null) {
                return true;
            }
        }
        
        Namespace container = modelElement.getContainer();
        if (container == null) {
            return false;
        } else {
            return isTransient(container);
        }
    }

    /**
     * Find all associations that refer to the given MofClass which are not
     * described by {@link Reference} instances.
     * 
     * @param generator code generator
     * @param assocInfoMap map of {@link Association} to 
     *                     {@link AssociationInfo} for all associations in the 
     *                     model
     * @param cls {@link MofClass} to find unreferenced associations
     * @param references collection of {@link Reference} instances associated
     *                   with <code>cls</code>
     * @param unrefAssocRefInfoMap empty, modifiable map which is populated 
     *                             with pseudo-{@link ReferenceInfo} objects
     *                             to facilitate code generation for the
     *                             unreferenced associations
     * @return collection of unreferenced {@link Association} instances
     */
    public static Collection<Association> findUnreferencedAssociations(
        Generator generator,
        Map<Association, AssociationInfo> assocInfoMap,
        MofClass cls, 
        Collection<Reference> references,
        Map<Association, ReferenceInfo> unrefAssocRefInfoMap)
    {
        // Start with all associations
        Set<Association> result = 
            new LinkedHashSet<Association>(assocInfoMap.keySet());

        
        Map<Association, Reference> circularAssociations = 
            new HashMap<Association, Reference>();
        
        // Remove associations that are included in references.  If the
        // referenced end type is the same as this class (or one of its super
        // types), make note of it.  It will only be removed if there is a
        // matching Reference in the reverse direction (e.g., circular 
        // association with a reference to both ends.)
        for(Reference ref: references) {
            Association assoc = 
                (Association)ref.getExposedEnd().getContainer();
            
            Classifier refEndType = ref.getReferencedEnd().getType();
            if (refEndType instanceof AliasType) {
                refEndType = ((AliasType)refEndType).getType();
            }

            if (cls.equals(refEndType) || 
                cls.allSupertypes().contains(refEndType))
            {
                if (!circularAssociations.containsKey(assoc)) {
                    // No matching association previously found; keep track
                    // of this one for later, but don't remove the association
                    // from the result set.
                    circularAssociations.put(assoc, ref);
                    continue;
                } else {
                    // Matching association, fall through and remove it from
                    // the result set.
                }
            }

            result.remove(assoc);                
        }

        // Remove associations that don't refer to the given MofClass
        Iterator<Association> iter = result.iterator();
        ASSOC_LOOP:
        while(iter.hasNext()) {
            Association assoc = iter.next();
            
            AssociationInfo assocInfo = assocInfoMap.get(assoc);
            
            for(int endIndex = 0; endIndex < 2; endIndex++) {
                AssociationEnd end = assocInfo.getEnd(endIndex);
    
                // Ignore the end that's already exposed via a reference.
                if (circularAssociations.containsKey(assoc)) {
                    AssociationEnd refExposedEnd = 
                        circularAssociations.get(assoc).getExposedEnd();
                    
                    if (end.equals(refExposedEnd)) {
                        continue;
                    }
                }
                
                Classifier endType = end.getType();
                if (endType instanceof AliasType) {
                    endType = ((AliasType)endType).getType();
                }
                
                if (cls.equals(endType) || 
                    cls.allSupertypes().contains(endType))
                {
                    // End "endIndex" is the exposed end
                    AssociationEnd refEnd = assocInfo.getEnd(1 - endIndex);
                 
                    ReferenceInfoImpl refInfo = 
                        new ReferenceInfoImpl(generator, assoc, refEnd);
                    unrefAssocRefInfoMap.put(assoc, refInfo);
                    
                    continue ASSOC_LOOP;
                }
            }
            
            iter.remove();
        }
        
        return result;
    }
    
    /**
     * Determines the maximum length, in characters, for the given Attribute, 
     * appearing in the given Classifier.  The Classifier may be a subtype of
     * the Attribute's container.
     * 
     * <p>The value is obtained as follows:
     * <ol>
     *   <li>
     *     If the {@link Attribute} contains a {@link Tag} identified by
     *     {@value #MAX_LENGTH_TAG_NAME}, the tag's value is used.
     *   </li>
     *   <li>
     *     Otherwise, if the {@link Classifier} or one of it's supertypes
     *     contains a Tag identified by {@value #MAX_LENGTH_TAG_NAME}, the
     *     tag's value is used.  Supertypes are searched breadth-first, and
     *     the first value found is used.  This is primarily useful if an
     *     attribute in a common subclass requires a different maximum length
     *     in some special case.
     *   </li>
     * </ol>
     * 
     * <p>The special tag value {@value #MAX_LENGTH_UNLIMITED_VALUE} specifies 
     * that the maximum storage size for the underlying database should be used
     * and causes the value {@link Integer#MAX_VALUE} to be returned.
     * 
     * <p><b>NOTE:</b> 
     * {@link TagUtil#findMaxLengthTag(Classifier, Attribute, int)}</p>
     * 
     * @param cls Classifier in which the Attribute appears (perhaps via
     *            inheritance)
     * @param attrib Attribute to get a maximum length
     * @param defaultMaxLength the default to return if no other value is found
     * @param log optional Logger to use if an out-of-range value is truncated 
     * @return the maximum length for the Classifier and Attribute given,
     *         Integer.MAX_VALUE for unlimited.
     * @throws NumberFormatException if a tag value cannot be converted to an 
     *                               integer
     */
    public static int findMaxLengthTag(
        Classifier cls, 
        Attribute attrib, 
        int defaultMaxLength,
        Logger log)
    {
        String maxLen = TagUtil.getTagValue(attrib, MAX_LENGTH_TAG_NAME);
        if (maxLen != null) {
            return convertMaxLengthToInt(maxLen, log, attrib.getName());
        }

        // Check the attribute's container (Classifier) for a class-level 
        // default.
        maxLen = findMaxLength(cls); 
        if (maxLen != null) {
            return convertMaxLengthToInt(maxLen, log, attrib.getName());
        }
        
        return defaultMaxLength;
    }

    /**
     * Converts a maximum length string into an integer.  Performs truncation 
     * for out-of bounds values and supports
     * {@value #MAX_LENGTH_UNLIMITED_VALUE} as specified in
     * {@link #findMaxLengthTag(Classifier, Attribute, int, Logger)}.
     * 
     * @param maxLen maximum length string
     * @param log logger for out-of-bounds logging
     * @param attribName attribute name for out-of-bounds logging
     * @return int representation of maxLen
     * @throws NumberFormatException if maxLen cannot be converted to an 
     *                               integer
     */
    private static int convertMaxLengthToInt(
        String maxLen, Logger log, String attribName)
    throws NumberFormatException
    {
        if (MAX_LENGTH_UNLIMITED_VALUE.equals(maxLen)) {
            return Integer.MAX_VALUE;
        }
        
        int max = Integer.parseInt(maxLen);
        
        if (max < 1) {
            if (log != null) {
                log.warning(
                    "Adjusted string length for attribute '"
                    + attribName
                    + "' to 1");
            }
            max = 1;
        }
        
        return max;
    }

    /**
     * Performs a breadth-first search of the given Classifier's super types
     * looking for a Classifier that has the {@link #MAX_LENGTH_TAG_NAME} tag
     * set.
     * 
     * @param startCls starting Classifier
     * @return the value of the tag or null if not found
     */
    private static String findMaxLength(Classifier startCls)
    {
        // REVIEW: SWZ: 2008-04-14: This originally used Queue<Classifier>
        // with calls to offer() and poll(), but the poll() operation throws 
        // NoSuchElementException on JRockit 27.4  even though that should not 
        // be possible. (JVM bug reported.)
        LinkedList<Classifier> queue = new LinkedList<Classifier>();
        
        queue.add(startCls);
        
        while(!queue.isEmpty()) {
            Classifier cls = queue.removeFirst();
            
            String maxLen = TagUtil.getTagValue(cls, MAX_LENGTH_TAG_NAME);
            if (maxLen != null) {
                return maxLen;
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
}

// End CodeGenUtils.java
