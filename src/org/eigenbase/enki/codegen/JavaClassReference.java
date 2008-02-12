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

import java.util.*;

/**
 * JavaClassReference represents a reference to library class used in code
 * generation.  This provides two useful functions.  First, by using this
 * class, you guarantee a reference to the class exists in your source code.
 * Having that reference allows IDEs to automatically rename classes and
 * find uses a particular class in code generation.  Second, it provides
 * a mechanism to simplify generated code by automatically generating Java 
 * import statements.
 *  
 * @author Stephan Zuercher
 */
public class JavaClassReference
{
    private static final int MIN_REFS_FOR_STAR = 1;
    
    private final String simpleName;
    private final String fullName;
    private final String packageName;
    private final boolean useImport;
    
    /**
     * Constructs a JavaClassReference to <code>cls</code> that does not
     * use import statements.
     * 
     * @param cls a Java class
     */
    public JavaClassReference(Class<?> cls)
    {
        this.simpleName = cls.getSimpleName();
        this.fullName = cls.getName();
        this.packageName = cls.getPackage().getName();
        this.useImport = false;
    }
    
    /**
     * Constructs a JavaClassReference to <code>cls</code> and configures
     * whether the reference uses imports or not.
     * 
     * @param cls a Java class
     * @param useImport if true, use an import statement for this class
     */
    public JavaClassReference(Class<?> cls, boolean useImport)
    {
        this.simpleName = cls.getSimpleName();
        this.fullName = cls.getName();
        this.packageName = cls.getPackage().getName();
        this.useImport = useImport;
    }
    
    /**
     * Constructs a JavaClassReference from the names of as yet non-existent
     * classes.
     * 
     * @param packageName fully qualified package name (e.g., "java.lang")
     * @param className simple class name (e.g., "String");
     */
    public JavaClassReference(String packageName, String className)
    {
        this.packageName = packageName;
        this.simpleName = className;
        this.fullName = packageName + "." + className;
        this.useImport = false;
    }
    
    /**
     * Constructs a JavaClassReference from another reference, and allows
     * configuration of whether the new reference uses imports.  This is
     * useful when the same Java class is used in different ways during
     * code generation, but one wishes to guarantee that the same class
     * is used.
     *   
     * @param ref an existing JavaClassReference
     * @param useImport if true, the new reference uses an import statement
     *                  for this class
     */
    public JavaClassReference(JavaClassReference ref, boolean useImport)
    {
        this.simpleName = ref.simpleName;
        this.fullName = ref.fullName;
        this.packageName = ref.packageName;
        this.useImport = useImport;
    }
    
    /**
     * Returns whether this reference should use an import statement or not.
     * @return whether this reference should use an import statement or not.
     */
    public boolean useImport()
    {
        return useImport;
    }
    
    /**
     * Returns a JavaClassReferences that represents the same class as this
     * instance, but with the import flag set.  This instance is simply 
     * returned if it already has the import flag set. 
     *
     * @return an imported JavaClassReference to this instance's reference
     */
    public JavaClassReference asImport()
    {
        if (useImport()) {
            return this;
        }
        
        return new JavaClassReference(this, true);
    }
    
    /**
     * Returns the name of this class in a format suitable for use in code
     * generation.  If {@link #useImport()} returns <code>true</code>, this
     * method returns the simple Java class name.  Otherwise, it returns
     * a fully-qualified class name.
     * 
     * @return class name formatted for source code
     */
    public String toString()
    {
        if (useImport) {
            return toSimple();
        } else {
            return toFull();
        }
    }
    
    /**
     * Returns the fully-qualified class name.
     * @return the fully-qualified class name.
     */
    public String toFull()
    {
        return fullName;
    }
    
    /**
     * Returns the simple class name.
     * @return the simple class name.
     */
    public String toSimple()
    {
        return simpleName;
    }
    
    /**
     * Returns the name of the class's package.
     * @return the name of the class's package.
     */
    public String getPackageName()
    {
        return packageName;
    }
    
    /**
     * For the given array of JavaClassReference instances, computes a list
     * class names for import statements.  The resulting array is compatible
     * with {@link JavaHandlerBase#writeClassHeader(
     *          javax.jmi.model.ModelElement, String, String, String[], 
     *          String[], boolean, String)}. 
     * 
     * @param refs references for which to generate import information
     * @return an array of import class names
     */
    public static String[] computeImports(JavaClassReference... refs)
    {
        Map<String, List<String>> imports =
            new TreeMap<String, List<String>>();
        
        if (refs != null) {
            for(JavaClassReference ref: refs) {
                if (ref.useImport) {
                    String pkgName = ref.getPackageName();
                    List<String> classNames = imports.get(pkgName);
                    if (classNames == null) {
                        classNames = new ArrayList<String>();
                        imports.put(pkgName, classNames);
                    }
                    classNames.add(ref.toSimple());
                }
            }
        }
        
        List<String> result = new ArrayList<String>();
        for(Map.Entry<String, List<String>> entry: imports.entrySet()) {
            String pkgName = entry.getKey();
            List<String> classNames = entry.getValue();
            
            if (classNames.size() >= MIN_REFS_FOR_STAR) {
                result.add(pkgName + ".*");
            } else {
                Collections.sort(classNames);

                for(String className: classNames) {
                    result.add(pkgName + "." + className);
                }
            }
        }
        
        return result.toArray(new String[result.size()]);
    }
}

// End ClassReference.java
