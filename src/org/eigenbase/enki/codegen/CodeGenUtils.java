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
    private static final Logger log = 
        Logger.getLogger(CodeGenUtils.class.getName());
    
    private static final String COLLECTION_INTERFACE = 
        Collection.class.getName();

    private static final String ORDERED_COLLECTION_INTERFACE = 
        List.class.getName();

    private static boolean enableGenerics = true;
    
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

    /**
     * Tag identifier for a custom Enki tag to control whether an association
     * should anticipate a large number of members.  The tag identifier is
     * {@value}.
     */
    public static final String HIGH_CARDINALITY_ASSOCIATION_TAG_NAME =
        "org.eigenbase.enki.highCardinalityAssociation";
    
    private CodeGenUtils()
    {
    }
    
    /**
     * Enables or disables generic types.  If enabled, collections include 
     * generic type specifications.  If disabled, the generic types are 
     * commented out (e.g., <tt>List/*&lt;SomeType&gt;*&#x2f;</tt>) 
     * 
     * @param enableGenericsInit controls whether generic types are enabled 
     *                           (true) or not (false)
     */
    public static void setEnableGenerics(boolean enableGenericsInit)
    {
        enableGenerics = enableGenericsInit;
    }

    /**
     * Determines whether the given {@link ModelElement} is transient.  Only
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
     * Determines whether the given {@link Association} is labeled as a
     * high-cardinality association.
     * 
     * @param assoc the association to test for the tag
     * @return true if the tag is present and has a value of "true"
     */
    public static boolean isHighCardinalityAssociation(Association assoc)
    {
        return isHighCardinalityAssociationImpl(assoc);
    }

    /**
     * Determines whether the given {@link Attribute} is labeled as a
     * high-cardinality association.  The high-cardinality tag is only
     * appropriate if the Attribute references another model element.
     * 
     * @param attrib the attribute to test for the tag
     * @return true if the tag is present and has a value of "true"
     */
    public static boolean isHighCardinalityAssociation(Attribute attrib)
    {
        return isHighCardinalityAssociationImpl(attrib);
    }
    
    private static boolean isHighCardinalityAssociationImpl(ModelElement elem)
    {
        String value = TagUtil.getTagValue(
            elem, HIGH_CARDINALITY_ASSOCIATION_TAG_NAME);
        if (value == null) {
            return false;
        }
        
        return Boolean.valueOf(value);
    }

    /**
     * Find all associations that refer to the given MofClass which are not
     * described by {@link Reference} instances.
     * 
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
                        new ReferenceInfoImpl(assoc, refEnd);
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
     *     Otherwise, if the {@link Classifier} or one of its supertypes
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
     * @param cls Classifier in which the Attribute appears (perhaps via
     *            inheritance)
     * @param attrib Attribute to get a maximum length
     * @param defaultMaxLength the default to return if no other value is found
     * @param logger optional Logger to use if an out-of-range value is 
     *               truncated 
     * @return the maximum length for the Classifier and Attribute given,
     *         Integer.MAX_VALUE for unlimited.
     * @throws NumberFormatException if a tag value cannot be converted to an 
     *                               integer
     */
    public static int findMaxLengthTag(
        Classifier cls, 
        Attribute attrib, 
        int defaultMaxLength,
        Logger logger)
    {
        String maxLen = TagUtil.getTagValue(attrib, MAX_LENGTH_TAG_NAME);
        if (maxLen != null) {
            return convertMaxLengthToInt(maxLen, logger, attrib.getName());
        }

        // Check the attribute's container (Classifier) for a class-level 
        // default.
        maxLen = findMaxLength(cls); 
        if (maxLen != null) {
            return convertMaxLengthToInt(maxLen, logger, attrib.getName());
        }
        
        return defaultMaxLength;
    }

    /**
     * Returns the maximum length for a given attribute in a given class.
     * Equivalent to 
     * {@link #findMaxLengthTag(Classifier, Attribute, int, Logger) 
     *        findMaxLengthTag(cls, attrib, defaultMaxLength, null)}.
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
        return findMaxLengthTag(cls, attrib, defaultMaxLength, null);
    }
    
    /**
     * Converts a maximum length string into an integer.  Performs truncation 
     * for out-of bounds values and supports
     * {@value #MAX_LENGTH_UNLIMITED_VALUE} as specified in
     * {@link #findMaxLengthTag(Classifier, Attribute, int, Logger)}.
     * 
     * @param maxLen maximum length string
     * @param logger logger for out-of-bounds logging
     * @param attribName attribute name for out-of-bounds logging
     * @return int representation of maxLen
     * @throws NumberFormatException if maxLen cannot be converted to an 
     *                               integer
     */
    private static int convertMaxLengthToInt(
        String maxLen, Logger logger, String attribName)
    throws NumberFormatException
    {
        if (MAX_LENGTH_UNLIMITED_VALUE.equals(maxLen)) {
            return Integer.MAX_VALUE;
        }
        
        int max = Integer.parseInt(maxLen);
        
        if (max < 1) {
            if (logger != null) {
                logger.warning(
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
    
    /**
     * Iterates over contents of the entity and returns a collection 
     * containing all contents of the given type.  Objects of any scope and 
     * visibility are returned.  Super types are not searched.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    public static <E> Collection<E> contentsOfType(
        GeneralizableElement entity, Class<E> cls)
    {
        return contentsOfType(
            entity, 
            HierachySearchKindEnum.ENTITY_ONLY,
            null, 
            null,
            cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type.
     * Objects of any scope and visibility are returned.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    public static <E> Collection<E> contentsOfType(
        GeneralizableElement entity,
        HierachySearchKindEnum search, 
        Class<E> cls)
    {
        return contentsOfType(entity, search, null, null, cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type
     * with the given scope.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param scope content scope
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    public static <E> Collection<E> contentsOfType(
        GeneralizableElement entity, 
        HierachySearchKindEnum search, 
        ScopeKind scope,
        Class<E> cls)
    {
        return contentsOfType(entity, search, null, scope, cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type
     * with the given visibility.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param visibility content visibility
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    public static <E> Collection<E> contentsOfType(
        GeneralizableElement entity, 
        HierachySearchKindEnum search, 
        VisibilityKind visibility, 
        Class<E> cls)
    {
        return contentsOfType(entity, search, visibility, null, cls);
    }
    
    /**
     * Iterates over contents of the entity (and possibly its super types)
     * and returns a collection containing all contents of the given type
     * with the given visibility and scope.
     * 
     * @param <E> content type
     * @param entity entity to search over
     * @param search whether to search only the entity, or include super types
     * @param visibility content visibility
     * @param scope content scope
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    public static <E> Collection<E> contentsOfType(
        GeneralizableElement entity,
        HierachySearchKindEnum search, 
        VisibilityKind visibility,
        ScopeKind scope,
        Class<E> cls)
    {
        // LinkedHashSet prevents duplicate entries and preserves insertion
        // order as the iteration order, which are both desired here.
        LinkedHashSet<E> result = new LinkedHashSet<E>();
        
        if (search == HierachySearchKindEnum.INCLUDE_SUPERTYPES) {
            for(Namespace namespace: 
                    GenericCollections.asTypedList(
                        entity.allSupertypes(), Namespace.class))
            {
                result.addAll(
                    contentsOfType(namespace, visibility, scope, cls));
            }
        }
        
        result.addAll(
            contentsOfType(entity, visibility, scope, cls));

        return result;
    }

    /**
     * Iterates over contents of the namespace and returns a collection 
     * containing all contents of the given type with the given visibility and
     * scope.
     * 
     * @param <E> content type
     * @param namespace namespace to search over
     * @param visibility content visibility
     * @param scope content scope
     * @param cls Class for E
     * @return collection of E that are contents of entity
     */
    public static <E> Collection<E> contentsOfType(
        Namespace namespace,
        VisibilityKind visibility,
        ScopeKind scope,
        Class<E> cls)
    {
        LinkedHashSet<E> result = new LinkedHashSet<E>();

        for(Object o: namespace.getContents()) {
            if (!cls.isInstance(o)) {
                logDiscard(
                    namespace, 
                    visibility, 
                    scope, 
                    cls, 
                    "wrong type", 
                    o.getClass().getName());
                continue;
            }
            
            if (visibility != null && 
                !((Feature)o).getVisibility().equals(visibility))
            {
                logDiscard(
                    namespace, 
                    visibility, 
                    scope, 
                    cls,
                    "wrong visibility", 
                    ((Feature)o).getVisibility().toString());
                continue;
            }
            
            if (scope != null &&
                !((Feature)o).getScope().equals(scope))
            {
                logDiscard(
                    namespace, 
                    visibility, 
                    scope, 
                    cls,
                    "wrong scope", 
                    ((Feature)o).getScope().toString());
                continue;
            }

            logAccept(namespace, visibility, scope, cls);

            result.add(cls.cast(o));
        }
        
        return result;
    }

    private static <E> void logAccept(
        Namespace namespace,
        VisibilityKind visibility,
        ScopeKind scope,
        Class<E> cls)
    {
        if (!log.isLoggable(Level.FINEST)) {
            return;
        }

        log.finest(
            "contentsOfType(" +
            namespace.getName() + ": " +
            (visibility == null ? "<any-vis>" : visibility.toString()) + ", " +
            (scope == null ? "<any-scope>" : scope.toString()) + ", " +
            cls.getName() + "): ok");
    }
    
    private static void logDiscard(
        Namespace namespace, 
        VisibilityKind visibility, 
        ScopeKind scope, 
        Class<?> cls, 
        String desc, 
        String value)
    {
        if (!log.isLoggable(Level.FINEST)) {
            return;
        }
        
        log.finest(
            "contentsOfType(" +
            namespace.getName() + ": " +
            (visibility == null ? "<any-vis>" : visibility.toString()) + ", " +
            (scope == null ? "<any-scope>" : scope.toString()) + ", " +
            cls.getName() + "): " + desc + ": " + value);
    }
    /**
     * Returns a 2-element array containing the type and variable name for
     * the given {@link ModelElement}.
     * 
     * @param param a ModelElement
     * @return a 2-element array containing the type and variable name
     */
    public static String[] getParam(ModelElement param)
    {
        String[] result = new String[2];

        if (param instanceof StructuralFeature) {
            result[0] = getTypeName((StructuralFeature)param);
        } else if (param instanceof Parameter) {
            result[0] = getTypeName((Parameter)param);            
        } else if (param instanceof TypedElement) {
            result[0] = getTypeName((TypedElement)param);
        } else {
            assert(false);
        }
        
        String name = 
            TagUtil.getTagValue(param, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (name == null) {
            name = param.getName();
        }
        
        result[1] = 
            StringUtil.mangleIdentifier(
                name, StringUtil.IdentifierType.CAMELCASE_INIT_LOWER);
        
        return result;
    }
    
    /**
     * Returns the type name for the given {@link StructuralFeature}.  
     * Queries the underlying type (via the {@link TypedElement} subclass)
     * and combines it with the StructuralFeature's {@link MultiplicityType}
     * to return the correct type name. 
     * 
     * @param feature StructuralFeature for which to compute a type name
     * @return a type name string (e.g., "List&lt;a.b.Class&gt;").
     */
    public static String getTypeName(StructuralFeature feature)
    {
        return getTypeName(feature, feature.getMultiplicity());
    }

    /**
     * Returns the type name for the given {@link StructuralFeature}.  
     * Queries the underlying type (via the {@link TypedElement} subclass)
     * and combines it with the StructuralFeature's {@link MultiplicityType}
     * to return the correct type name. 
     * 
     * @param feature StructuralFeature for which to compute a type name
     * @param suffix suffix for the type name
     * @return a type name string (e.g., "List&lt;a.b.Class&gt;").
     */
    public static String getTypeName(StructuralFeature feature, String suffix)
    {
        return getTypeName(feature, feature.getMultiplicity(), suffix);
    }

    /**
     * Returns the type name for the given {@link Parameter}.  
     * Queries the underlying type (via the {@link TypedElement} subclass)
     * and combines it with the Parameter's {@link MultiplicityType}
     * to return the correct type name. 
     * 
     * @param param Parameter for which to compute a type name
     * @return a type name string (e.g., "List&lt;a.b.Class&gt;").
     */
    public static String getTypeName(Parameter param)
    {
        return getTypeName(param, param.getMultiplicity());
    }

    /**
     * Returns the type name for the given {@link TypedElement}.  
     * Queries the underlying type and presumes multiplicity of exactly 1.
     * 
     * @param type TypedElement for which to compute a type name
     * @return a type name string
     */
    public static String getTypeName(TypedElement type)
    {
        return getTypeName(type, (MultiplicityType)null);
    }
    
    /**
     * Returns the type name for the given {@link TypedElement}.  
     * Queries the underlying type and and combines it with the given
     * multiplicity.
     * 
     * @param elem TypedElement for which to compute a type name
     * @param mult multiplicity of the element
     * @return a type name string (e.g., "List&lt;a.b.Class&gt;").
     */
    public static String getTypeName(TypedElement elem, MultiplicityType mult)
    {
        return getTypeName(elem, mult, null);
    }
    
    /**
     * Converts a collection and element type into a single, possibly 
     * generic type name.  For example, "java.util.List" and 
     * "java.lang.String" are combined into one of:
     * 
     * <ul>
     * <li>"java.util.List&lt;java.lang.String&gt;"</li>
     * <li>"java.util.List/*&lt;java.lang.String&gt;*&#x2f;"</li>
     * </ul>
     * 
     * depending on whether generic types are enabled or not.
     * 
     * @param collectionType collection type reference
     * @param elementType element type name
     * @return combined collection type name
     */
    public static String getCollectionType(
        JavaClassReference collectionType, String elementType)
    {
        return getCollectionType(
            collectionType.toString(), elementType);
    }
    
    private static String getCollectionType(
        String collectionType, String elementType)
    {
        StringBuffer result = new StringBuffer(collectionType);
        if (!enableGenerics) {
            result.append("/*");
        }

        result.append('<').append(elementType).append('>');

        if (!enableGenerics) {
            result.append("*/");
        }

        return result.toString();
    }


    /**
     * Converts a map and element types into a single, possibly 
     * generic type name.  For example, "java.util.Map", "java.lang.Integer",
     * and "java.lang.String" are combined into one of:
     * 
     * <ul>
     * <li>"java.util.Map&lt;java.lang.Integer, java.lang.String&gt;"</li>
     * <li>"java.util.Map/*&lt;java.lang.Integer, java.lang.String&gt;*&#x2f;"</li>
     * </ul>
     * 
     * depending on whether generic types are enabled or not.
     * 
     * @param mapType map type reference
     * @param keyType element type name
     * @param valueType element type name
     * @return combined map type name
     */
    public static String getMapType(
        JavaClassReference mapType, 
        String keyType,
        String valueType)
    {
        StringBuilder result = new StringBuilder(mapType.toString());
        if (!enableGenerics) {
            result.append("/*");
        }

        result
            .append('<')
            .append(keyType)
            .append(", ")
            .append(valueType)
            .append('>');

        if (!enableGenerics) {
            result.append("*/");
        }

        return result.toString();
    }
    

    /**
     * Converts a simple ModelElement into a type name.
     * 
     * @param elem ModelElement for which a type name should be generated
     * @return type name
     */
    public static String getTypeName(ModelElement elem)
    {
        return getTypeName(elem, "");
    }

    /**
     * Converts a simple ModelElement into a type name with a given suffix.
     * 
     * @param elem ModelElement for which a type name should be generated
     * @param suffix suffix for the type name
     * @return type name
     */
    public static String getTypeName(ModelElement elem, String suffix)
    {
        String name = getSimpleTypeName(elem, suffix == null ? "" : suffix);
        
        if (elem instanceof PrimitiveType) {
            // REVIEW: SWZ: 11/07/2007: I think it's nicer to leave off the
            // extraneous "java.lang".  This differs from Netbeans MDR and
            // could cause problems if a model has unpackaged elements with
            // the same names as the Java primitive wrapper classes.  Note
            // that Primitives contains entries for the bare and fully
            // qualified versions.

            return /*"java.lang." + */ name;
        }
        
        return 
            getTypePrefix(elem, new StringBuilder())
            .append('.')
            .append(name)
            .toString();
    }
    
    /**
     * Converts a simple ModelElement into a type name without any package 
     * names.
     * 
     * @param elem ModelElement for which a simple type name should be 
     *             generated
     * @return a simple (package-less) type name
     */
    public static String getSimpleTypeName(ModelElement elem)
    {
        return getSimpleTypeName(elem, "");
    }

    /**
     * Converts a simple ModelElement into a type name without any package 
     * names, but with the given suffix.
     * 
     * @param elem ModelElement for which a simple type name should be 
     *             generated
     * @param suffix suffix for the type name
     * @return a simple (package-less) type name
     */
    public static String getSimpleTypeName(ModelElement elem, String suffix)
    {
        String name = TagUtil.getTagValue(elem, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (name == null) {
            name = elem.getName();
        }
        
        if (elem instanceof PrimitiveType) {
            return name;
        } else if (elem instanceof Constant) {
            name = 
                StringUtil.mangleIdentifier(
                    name, StringUtil.IdentifierType.ALL_CAPS);
        } else {
            boolean initCaps = 
                elem instanceof MofClass || 
                elem instanceof MofPackage ||
                elem instanceof Association || 
                elem instanceof MofException ||
                elem instanceof StructureType || 
                elem instanceof EnumerationType ||
                elem instanceof CollectionType || 
                elem instanceof Import;
            name = 
                StringUtil.mangleIdentifier(
                    name, 
                    initCaps 
                        ? StringUtil.IdentifierType.CAMELCASE_INIT_UPPER 
                        : StringUtil.IdentifierType.CAMELCASE_INIT_LOWER);
        }

        // SPECIAL CASE: If the name happens to end with Exception, don't
        // double it.
        if (ExceptionHandler.EXCEPTION_SUFFIX.equals(suffix) &&
            name.endsWith(suffix))
        {
            return name;
        }
        
        return name + suffix;
    }
    
    private static StringBuilder getTypePrefix(
        ModelElement elem, StringBuilder buffer)
    {
        ModelElement pkg = elem;

        while (!(pkg instanceof MofPackage)) {
            pkg = pkg.getContainer();
        }

        Namespace container = pkg.getContainer();
        if (container == null) {
            String pkgPrefix = 
                TagUtil.getTagValue(pkg, TagUtil.TAGID_PACKAGE_PREFIX);
            if (pkgPrefix != null) {
                // Package names are all-lowercase alphabetic. Let javac
                // worry about legality.
                buffer.append(pkgPrefix.toLowerCase(Locale.US)).append('.');
            }
        } else {
            getTypePrefix(container, buffer).append('.');
        }

        String packageName =
            TagUtil.getTagValue(pkg, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (packageName == null) {
            packageName = 
                StringUtil.mangleIdentifier(pkg.getName(), StringUtil.IdentifierType.ALL_LOWER);
        }

        // Package names are all-lowercase
        return buffer.append(packageName.toLowerCase(Locale.US));
    }


    /**
     * Converts a {@link StructuralFeature} into the name of an accessor
     * method.  Delegates to 
     * {@link #getAccessorName(Generator, TypedElement, MultiplicityType, boolean)}
     * with special case for booleans enabled.
     * 
     * @param feature StructuralFeature that requires an accessor
     * @return accessor name
     * @see #getAccessorName(Generator, TypedElement, MultiplicityType, boolean)
     */
    public static String getAccessorName(
        Generator generator, StructuralFeature feature)
    {
        return getAccessorName(
            generator, feature, feature.getMultiplicity(), true);
    }

    /**
     * Converts a {@link TypedElement} and {@link MultiplicityType} into the 
     * name of an accessor method.    Delegates to 
     * {@link #getAccessorName(Generator, TypedElement, MultiplicityType, boolean)}
     * with special case for booleans enabled.
     *
     * @param elem TypedElement that requires an accessor
     * @param mult the element's multiplicity
     * @return accessor name
     */
    public static String getAccessorName(
        Generator generator, TypedElement elem, MultiplicityType mult)
    {
        return getAccessorName(generator, elem, mult, true);
    }

    /**
     * Converts a {@link StructuralFeature} into the name of an accessor
     * method.  Delegates to 
     * {@link #getAccessorName(Generator, TypedElement, MultiplicityType, boolean)}.
     * 
     * @param feature StructuralFeature that requires an accessor
     * @param specialCaseBooleans whether to manipulate boolean attribute
     *                            accessor names
     * @return accessor name
     * @see #getAccessorName(Generator, TypedElement, MultiplicityType, boolean)
     */
    public static String getAccessorName(
        Generator generator,
        StructuralFeature feature,
        boolean specialCaseBooleans)
    {
        return getAccessorName(
            generator, 
            feature, 
            feature.getMultiplicity(), 
            specialCaseBooleans);
    }

    /**
     * Converts a {@link TypedElement} and {@link MultiplicityType} into the 
     * name of an accessor method.  The TypeElements's underlying type name 
     * (via {@link TypedElement#getType()}) is capitalized and given the 
     * prefix "get", <b>unless</b> the following conditions are met:
     * 
     * <ul>
     * <li>The <code>specialCaseBooleans</code> parameter is set to
     *     <code>true</code>.
     * </li>
     * <li>The multiplicity is unspecified (null) or the multiplicity's upper
     *     bound is 1.</li>
     * <li>The type is boolean.</li>
     * </ul>
     * 
     * If the conditions are met, the prefix "is" is prepended, unless the 
     * type name already begins with "is", in which case it is used directly.
     * 
     * <p>If the multiplicity's upper bound is set to 0, this method returns
     * null.
     * 
     * @param generator generator
     * @param elem TypedElement that requires an accessor
     * @param mult the element's multiplicity
     * @param specialCaseBooleans if true
     * @return accessor name
     */
    public static String getAccessorName(
        Generator generator,
        TypedElement elem, 
        MultiplicityType mult,
        boolean specialCaseBooleans)
    {
        Classifier attribType = elem.getType();
        if (attribType instanceof AliasType) {
            attribType = ((AliasType)attribType).getType();
        }
        
        String attribName = elem.getName();

        String substName = 
            TagUtil.getTagValue(elem, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (substName != null) {
            attribName = substName;
        }

        attribName = generator.transformIdentifier(attribName);
            
        String baseName = 
            StringUtil.mangleIdentifier(
                attribName, StringUtil.IdentifierType.CAMELCASE_INIT_UPPER);

        String accessorName = null;

        // Upper bound -1 means infinity.
        if (mult == null || mult.getUpper() == 1) {
                
            if (specialCaseBooleans &&
                attribType instanceof PrimitiveType && 
                attribType.getName().equals("Boolean"))
            {
                
                if (baseName.startsWith("Is")) {
                    accessorName = "is" + baseName.substring(2);
                } else {
                    accessorName = "is" + baseName;
                }
            } else {
                accessorName = "get" + baseName;
            }
            
        } else if (mult.getUpper() != 0) {
            accessorName = "get" + baseName;
        }

        return accessorName;
    }
    

    /**
     * Converts a {@link StructuralFeature} into a mutator method name.
     * Delegates to the method
     * {@link #getMutatorName(Generator, StructuralFeature, boolean)} with the 
     * special base for booleans enabled. 
     * 
     * @param feature StructuralFeature that requires a mutator
     * @return mutator name
     */
    public static String getMutatorName(
        Generator generator, StructuralFeature feature)
    {
        return getMutatorName(generator, feature, true);
    }

    /**
     * Converts a {@link StructuralFeature} into a mutator method name.
     * This method capitalizes the feature's underlying type name 
     * (via {@link TypedElement#getType()}) and prepends "set", unless the 
     * type is a boolean and the special case is enabled, in which
     * case the prefix "Is", if any, is stripped from the attribute name
     * first.
     * 
     * @param generator generator
     * @param feature StructuralFeature that requires a mutator
     * @param specialCaseBooleans control special case for boolean attributes
     * @return mutator name
     * @see #getAccessorName(Generator, TypedElement, MultiplicityType, boolean)
     */
    public static String getMutatorName(
        Generator generator,
        StructuralFeature feature, 
        boolean specialCaseBooleans)
    {
        Classifier attribType = feature.getType();
        if (attribType instanceof AliasType) {
            attribType = ((AliasType)attribType).getType();
        }

        String attribName = feature.getName();

        String substName = 
            TagUtil.getTagValue(feature, TagUtil.TAGID_SUBSTITUTE_NAME);
        if (substName != null) {
            attribName = substName;
        }

        attribName = generator.transformIdentifier(attribName);
        
        String baseName = 
            StringUtil.mangleIdentifier(
                attribName, StringUtil.IdentifierType.CAMELCASE_INIT_UPPER);
            
        String mutatorName = null;

        if (feature.getMultiplicity().getUpper() == 1 &&
            specialCaseBooleans &&
            attribType instanceof PrimitiveType &&
            attribType.getName().equals("Boolean") &&
            baseName.startsWith("Is"))
        {
            mutatorName = "set" + baseName.substring(2);
        } else {
            mutatorName = "set" + baseName;
        }

        return mutatorName;
    }
    
    /**
     * Converts a literal into an enumeration field name.  Splits the literal
     * into words wherever a non-alphanumeric character is found and then 
     * further splits words when a CamelCase boundary is found.  The individual
     * words are then converted to all-caps and concatenated with underscores
     * separating the words.
     * 
     * <p>For example, 
     * <table>
     * <tr><th>Input</th>                 <th>Output</th></tr>
     * <tr><td>MyLiteral_String Thing</td><td>MY_LITERAL_STRING_THING</td></tr>
     * <tr><td>mixed_case_IS_okay</td>    <td>MIXED_CASE_IS_OKAY</td></tr>
     * <tr><td>weirdTESTcase</td>         <td>WEIRD_TESTCASE</td></tr>
     * </table>
     * 
     * @param literal literal to convert to an enumeration field name
     * @return an enumeration field name for the given literal
     */
    public static String getEnumFieldName(String literal)
    {
        return StringUtil.mangleIdentifier(literal, StringUtil.IdentifierType.ALL_CAPS);
    }
    
    /**
     * Converts a literal into a class field name.  Splits the literal into
     * words wherever a non-alphanumeric character is found and then 
     * further splits words when a CamelCase boundary is found.  The returns
     * field name is formed by concatenating the all-lower-case first word
     * with the remaining upper-cased words.
     *
     * @param literal literal to convert to a class field name
     * @return a class field name for the given literal
     */
    public static String getClassFieldName(String literal)
    {
        return StringUtil.mangleIdentifier(literal, StringUtil.IdentifierType.CAMELCASE_INIT_LOWER);
    }
    
    /**
     * Retrieves the two ends of the association, ignoring other contents.
     * 
     * @param assoc an Association
     * @return the (exactly) two ends of the Association
     */
    public static AssociationEnd[] getAssociationEnds(Association assoc)
    {
        List<?> contents = assoc.getContents();

        AssociationEnd[] ends = new AssociationEnd[2];
        Iterator<?> endIter = contents.iterator();
        int i = 0;
        while(endIter.hasNext()) {
            Object o = endIter.next();
            if (o instanceof AssociationEnd) {
                if (i >= 2) {
                    throw new IllegalStateException(
                        "Association has more than two ends");
                }
                
                AssociationEnd end = (AssociationEnd)o;
                
                ends[i++] = end;
            }
        }
        if (i != 2) {
            throw new IllegalStateException(
                "Association does not have exactly two ends");
        }
        
        return ends;
    }
    
    /**
     * Determines the kind of association given.
     * 
     * @param assoc an association
     * @return one of the {@link AssociationKindEnum} values indicating a
     *         1-to-1, 1-to-many or many-to-many association.
     */
    public static AssociationKindEnum getAssociationKind(Association assoc)
    {
        AssociationEnd ends[] = getAssociationEnds(assoc);
        int[] upperBounds = new int[2];
        for(int i = 0; i < 2; i++) {
            AssociationEnd end = (AssociationEnd)ends[i];
                
            upperBounds[i] = end.getMultiplicity().getUpper();
        }
        
        if (upperBounds[0] == 1 && upperBounds[1] == 1) {
            return AssociationKindEnum.ONE_TO_ONE;
        } else if (upperBounds[0] != 1 && upperBounds[1] != 1) {
            return AssociationKindEnum.MANY_TO_MANY;            
        } else {
            return AssociationKindEnum.ONE_TO_MANY;
        }
    }

    public static String getTypeName(
        TypedElement elem, MultiplicityType mult, String suffix)
    {
        ModelElement type = elem.getType();
        if (type instanceof AliasType) {
            type = ((AliasType)type).getType();
        }
        String typeName = getTypeName(type, suffix);
        
        String collType = null;
        if (mult != null && (mult.getUpper() > 1 || mult.getUpper() == -1)) {
            if (mult.isOrdered()) {
                collType = ORDERED_COLLECTION_INTERFACE;
            } else {
                collType = COLLECTION_INTERFACE;
            }
        } else if (mult == null || mult.getLower() >= 1) {
            String primitiveTypeName = 
                Primitives.convertTypeNameToPrimitive(typeName);
            if (primitiveTypeName != null) {
                typeName = primitiveTypeName;
            }
        }
        
        if (collType != null) {
            return getCollectionType(collType, typeName);
        }

        return typeName;
    }
}

// End CodeGenUtils.java
