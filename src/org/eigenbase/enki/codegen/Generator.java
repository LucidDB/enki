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
 * Generator represents a class that generates JMI code for a UML model.
 * 
 * @author Stephan Zuercher
 */
public interface Generator
{
    /**
     * Configures the XMI file containing the model for which code will be
     * generated.
     * 
     * @param xmiFile a filename
     */
    public void setXmiFile(File xmiFile);

    /**
     * Retrieves the XMI file containing the model for which code is being
     * generated.
     * 
     * @return the XMI file's name
     */
    public File getXmiFile();
    
    /**
     * Configures the directory where code generated for the model will be
     * written.
     * 
     * @param outputDir an existing directory
     */
    public void setOutputDirectory(File outputDir);

    /**
     * Enables or disables generic types.  If enabled, collections include 
     * generic type specifications.  If disabled, the generic types are 
     * commented out (e.g., <tt>List/*&lt;SomeType&gt;*&#x2f;</tt>) 
     * 
     * @param enable controls whether generic types are enabled (true) or not 
     *               (false)
     * @return the previous value of the setting
     */
    public boolean setUseGenerics(boolean enable);

    /**
     * Configures implementation-specific options for this Generator.  Unknown
     * options should be ignored.
     * 
     * @param options map of option name to value
     */
    public void setOptions(Map<String, String> options);
    
    /**
     * Adds a {@link Handler} implementation to the list of handlers for this
     * generator.  All calls to this method must occur <b>before</b> 
     * {@link #execute()} or as part of a callback initiated by that method.
     * The Generator implementation must call 
     * {@link Handler#setGenerator(Generator)} before invoking any method
     * on the handler.
     * 
     * @param handler a {@link Handler} implementation.  In practice, the
     *                object given must also implement one of Handler's
     *                sub-interfaces.
     */
    public void addHandler(Handler handler);

    /**
     * Generates code with the given configuration.
     * 
     * @throws GenerationException if there is a file or other error during
     *                             code generation
     */
    public void execute() throws GenerationException;

    /**
     * Retrieves the RefBaseObject for the current metamodel.
     * 
     * @return the RefBaseObject for the current metamodel.
     */
    public RefBaseObject getRefBaseObject();
    
    /**
     * Returns a 2-element array containing the type and variable name for
     * the given {@link ModelElement}.
     * 
     * @param param a ModelElement
     * @return a 2-element array containing the type and variable name
     */
    public String[] getParam(ModelElement param);

    /**
     * Returns the type name for the given {@link StructuralFeature}.  
     * Queries the underlying type (via the {@link TypedElement} subclass)
     * and combines it with the StructuralFeature's {@link MultiplicityType}
     * to return the correct type name. 
     * 
     * @param feature StructuralFeature for which to compute a type name
     * @return a type name string (e.g., "List&lt;a.b.Class&gt;").
     */
    public String getTypeName(StructuralFeature feature);

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
    public String getTypeName(StructuralFeature feature, String suffix);

    /**
     * Returns the type name for the given {@link Parameter}.  
     * Queries the underlying type (via the {@link TypedElement} subclass)
     * and combines it with the Parameter's {@link MultiplicityType}
     * to return the correct type name. 
     * 
     * @param param Parameter for which to compute a type name
     * @return a type name string (e.g., "List&lt;a.b.Class&gt;").
     */
    public String getTypeName(Parameter param);

    /**
     * Returns the type name for the given {@link TypedElement}.  
     * Queries the underlying type and presumes multiplicity of exactly 1.
     * 
     * @param type TypedElement for which to compute a type name
     * @return a type name string
     */
    public String getTypeName(TypedElement type);
    
    /**
     * Returns the type name for the given {@link TypedElement}.  
     * Queries the underlying type and and combines it with the given
     * multiplicity.
     * 
     * @param type TypedElement for which to compute a type name
     * @return a type name string (e.g., "List&lt;a.b.Class&gt;").
     */
    public String getTypeName(TypedElement type, MultiplicityType mult);
    
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
    public String getCollectionType(
        JavaClassReference collectionType, String elementType);

    /**
     * Converts a simple ModelElement into a type name.
     * 
     * @param elem ModelElement for which a type name should be generated
     * @return type name
     */
    public String getTypeName(ModelElement elem);

    /**
     * Converts a simple ModelElement into a type name with a given suffix.
     * 
     * @param elem ModelElement for which a type name should be generated
     * @param suffix suffix for the type name
     * @return type name
     */
    public String getTypeName(ModelElement elem, String suffix);

    /**
     * Converts a simple ModelElement into a type name without any package 
     * names.
     * 
     * @param elem ModelElement for which a simple type name should be 
     *             generated
     * @return a simple (package-less) type name
     */
    public String getSimpleTypeName(ModelElement elem);

    /**
     * Converts a simple ModelElement into a type name without any package 
     * names, but with the given suffix.
     * 
     * @param elem ModelElement for which a simple type name should be 
     *             generated
     * @param suffix suffix for the type name
     * @return a simple (package-less) type name
     */
    public String getSimpleTypeName(ModelElement elem, String suffix);

    /**
     * Converts a {@link StructuralFeature} into the name of an accessor
     * method.  Delegates to 
     * {@link #getAccessorName(TypedElement, MultiplicityType, boolean)}
     * with special case for booleans enabled.
     * 
     * @param feature StructuralFeature that requires an accessor
     * @return accessor name
     * @see #getAccessorName(TypedElement, MultiplicityType, boolean)
     */
    public String getAccessorName(StructuralFeature feature);

    /**
     * Converts a {@link TypedElement} and {@link MultiplicityType} into the 
     * name of an accessor method.    Delegates to 
     * {@link #getAccessorName(TypedElement, MultiplicityType, boolean)}
     * with special case for booleans enabled.
     *
     * @param elem TypedElement that requires an accessor
     * @param mult the element's multiplicity
     * @return accessor name
     */
    public String getAccessorName(TypedElement elem, MultiplicityType mult);

    /**
     * Converts a {@link StructuralFeature} into the name of an accessor
     * method.  Delegates to 
     * {@link #getAccessorName(TypedElement, MultiplicityType, boolean)}.
     * 
     * @param feature StructuralFeature that requires an accessor
     * @param specialCaseBooleans whether to manipulate boolean attribute
     *                            accessor names
     * @return accessor name
     * @see #getAccessorName(TypedElement, MultiplicityType, boolean)
     */
    public String getAccessorName(
        StructuralFeature feature, boolean specialCaseBooleans);

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
     * @param elem TypedElement that requires an accessor
     * @param mult the element's multiplicity
     * @param specialCaseBooleans if true
     * @return accessor name
     */
    public String getAccessorName(
        TypedElement elem, MultiplicityType mult, boolean specialCaseBooleans);

    /**
     * Converts a {@link StructuralFeature} into a mutator method name.
     * Delegates to the method
     * {@link #getMutatorName(StructuralFeature, boolean)} with the special
     * base for booleans enabled. 
     * 
     * @param feature StructuralFeature that requires a mutator
     * @return mutator name
     */
    public String getMutatorName(StructuralFeature feature);

    /**
     * Converts a {@link StructuralFeature} into a mutator method name.
     * This method capitalizes the feature's underlying type name 
     * (via {@link TypedElement#getType()}) and prepends "set", unless the 
     * type is a boolean and the special case is enabled, in which
     * case the prefix "Is", if any, is stripped from the attribute name
     * first.
     * 
     * @param feature StructuralFeature that requires a mutator
     * @param specialCaseBooleans control special case for boolean attributes
     * @return mutator name
     * @see #getAccessorName(TypedElement, MultiplicityType, boolean)
     */
    public String getMutatorName(
        StructuralFeature feature, boolean specialCaseBooleans);

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
    public String getEnumFieldName(String literal);
    
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
    public String getClassFieldName(String literal);

    /**
     * Retrieves the two ends of the association, ignoring other contents.
     * 
     * @param assoc an Association
     * @return the (exactly) two ends of the Association
     */
    public AssociationEnd[] getAssociationEnds(Association assoc);
    
    /**
     * Determines the kind of association given.
     * 
     * @param assoc an association
     * @return one of the {@link AssociationKindEnum} values indicating a
     *         1-to-1, 1-to-many or many-to-many association.
     */
    public AssociationKindEnum getAssociationKind(Association assoc);

    /**
     * Applies any transformations required by this generator to an identifier.
     * Example transformations are truncation and mangling.
     *
     * @param identifier identifier to be transformed
     *
     * @return transformed identifier
     */
    public String transformIdentifier(String identifier);
}
