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

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;

/**
 * RefBaseObjectBase is a base class for implementations of 
 * {@link RefBaseObject}.  Specific storage implementations may need
 * to override methods to produce the correct results.
 * 
 * @author Stephan Zuercher
 */
public abstract class RefBaseObjectBase implements RefBaseObject
{
    private static final Logger log = 
        Logger.getLogger("org.eigenbase.enki.jmi");
    private static boolean loggingEnabled;
    
    private final MetamodelInitializer initializer;
    
    private long mofId;
    private String refMofId;
    private RefObject metaObj;

    private final Method[] cachedMethods;
    private final Map<String, Method> createMethodCache;
    
    protected RefBaseObjectBase()
    {
        this(MetamodelInitializer.getCurrentInitializer());
    }
    
    protected RefBaseObjectBase(MetamodelInitializer initializer)
    {
        this.initializer = initializer;

        if (initializer != null) {
            setMofId(initializer.nextMofId());
        }

        loggingEnabled = log.isLoggable(Level.FINER);
        
        if (this instanceof RefClass || this instanceof RefPackage) {
            this.cachedMethods = getClass().getMethods();
            this.createMethodCache = new HashMap<String, Method>();
        } else {
            this.cachedMethods = null;
            this.createMethodCache = null;
        }
    }
    
    /**
     * Returns the MOF ID configured via {@link #setMofId(long)}.
     * 
     * @return the MOF ID configured via {@link #setMofId(long)}.
     */
    public String refMofId()
    {
        return refMofId;
    }
    
    /**
     * Returns the {@link RefObject} configured via 
     * {@link #setRefMetaObject(RefObject)}.
     * 
     * @return the {@link RefObject} configured via 
     * {@link #setRefMetaObject(RefObject)}.
     */
    public RefObject refMetaObject()
    {
        logJmi("refMetaObject");
        
        return metaObj;
    }

    /**
     * Sets this instance's reflective meta-object to the given 
     * {@link RefObject}.
     * 
     * @param metaObj new reflective meta-object for this instance
     */
    public void setRefMetaObject(RefObject metaObj)
    {
        this.metaObj = metaObj;
    }
    
    public RefPackage refOutermostPackage()
    {
        logJmi("refOutermostPackage");
        
        if (refImmediatePackage() == null) {
            assert(this instanceof RefPackage);
            return (RefPackage)this;
        }
        
        return refImmediatePackage().refOutermostPackage();
    }


    public final Collection<?> refVerifyConstraints(boolean deepVerify)
    {
        logJmi("refVerifyConstraints");
        
        List<JmiException> errors = new ArrayList<JmiException>();
        
        checkConstraints(errors, deepVerify);
        
        if (errors.isEmpty()) {
            return null;
        }
        
        return errors;
    }
    
    /**
     * Implements {@link #refVerifyConstraints(boolean)}.
     * 
     * @param errors exceptions are added to this collection
     * @param deepVerify if objects should verify their components
     */
    protected abstract void checkConstraints(
        List<JmiException> errors, boolean deepVerify);

    public long getMofId()
    {
        return mofId;
    }
    
    public void setMofId(long refMofId)
    {
        this.mofId = refMofId;
        this.refMofId = MofIdUtil.makeMofIdStr(refMofId);
    }
    
    /**
     * Test against another object for equality.  This method compares
     * objects by MOF ID (via {@link #getMofId()} if possible, else
     * {@link #refMofId()}).
     * 
     * <p>Note that this method is final to aid repository implementations 
     * (e.g., Hibernate) that generate proxy classes.  Marking this method 
     * final prevents Hibernate from needlessly loading the entire object's
     * contents when we only require the primary key.
     */
    public final boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof RefBaseObjectBase) {
            long thisMofId = this.getMofId();
            long otherMofId = ((RefBaseObjectBase)other).getMofId();

            assert(thisMofId != 0L);
            assert(otherMofId != 0L);

            return thisMofId == otherMofId;
        } else if (other == null) {
            return false;
        } else if (other instanceof RefBaseObject) {
            return this.refMofId().equals(
                ((RefBaseObject)other).refMofId());
        } else {
            return false;
        }
    }
    
    /**
     * Returns the hash code for this model entity.  The hash code is based
     * on the MOF ID.
     * 
     * <p>Note that this method is final to aid repository implementations 
     * (e.g., Hibernate) that generate proxy classes.  Marking this method 
     * final prevents Hibernate from needlessly loading the entire object's
     * contents when we only require the primary key.
     *
     * @return the hash code for this model entity.
     */
    public final int hashCode()
    {
        // Note that this matches Netbean's behavior, which causes HashSet
        // iteration order to match.
        return (int)getMofId();
    }

    /**
     * Creates an instance of the named or given type by invoking a factory
     * method implemented by a sub-type.
     * 
     * <p>The given meta-object or type name is used to search for a method
     * that returns the result type.  The method's name must start with the
     * word "create."  The remainder of the method's name is the meta-object's
     * name (via {@link ModelElement#getName()} or the given type name.
     * 
     * <p>In addition to having the correct name, the number of parameters
     * must match the given parameter list and the method's parameter types
     * must be assignable from the objects in the parameter list.
     * 
     * @param <E> return type
     * @param type meta-object for the type to create
     * @param typeName type name for the type to create
     * @param params creation parameters
     * @param resultType return type class
     * @return an newly created instance matching the type/typeName and 
     *         resultType
     * @throws InternalJmiError if type and typeName are both null or both 
     *                          non-null, or if this instance is not a
     *                          {@link RefClass} or {@link RefPackage}
     * @throws WrongSizeException if the number of parameters does not match
     *                            the factory method.
     * @throws TypeMismatchException if the parameters cannot be converted to
     *                               the factory method's parameter types
     * @throws InvalidCallException if no method matches type
     * @throws InvalidNameException if no method matches typeName
     */
    protected <E> E createInstance(
        RefObject type,
        String typeName,
        List<?> params,
        Class<E> resultType)
    {
        if (cachedMethods == null) {
            // We only initialize this field for RefClass of RefPackage 
            // instances, so if it's no initialized, it's an error.  (Only
            // RefClass and RefPackage have refCreateXXX methods.)
            throw new InternalJmiError(
                "bad call: createInstance only valid on RefClass/RefPackage");
        }
        
        if ((type == null && typeName == null) ||
            (type != null && typeName != null)) 
        {
            throw new InternalJmiError(
                "bad call: either type or typeName must be non-null");
        }
        
        if (type != null) {
            ModelElement modelElem = (ModelElement)type;
            typeName = modelElem.getName();
        }
        
        if (params == null) {
            params = Collections.emptyList();
        }        

        WrongSizeException wse = null;
        TypeMismatchException tme = null;

        String cacheKey = 
            new StringBuilder(typeName)
                .append('/')
                .append(params.size())
                .toString();
        
        Method cachedCreateMethod = createMethodCache.get(cacheKey);
        if (cachedCreateMethod != null) {
            // Validate that the given parameters match the cached method.
            JmiException ex = 
                checkCreateMethod(
                    cachedCreateMethod, resultType, type, typeName, params);
            if (ex != null) {
                throw ex;
            }

            return invokeMethod(
                resultType, this, cachedCreateMethod, params.toArray());
        }

        // Search all methods for one that matches.
        for(Method method: cachedMethods) {
            JmiException ex =
                checkCreateMethod(method, resultType, type, typeName, params);
            if (ex == null) {
                createMethodCache.put(cacheKey, method);

                return invokeMethod(
                    resultType, this, method, params.toArray());
            }

            // If the returned exception is one of these types, then it must
            // have matched the name check 
            if (tme == null && ex instanceof TypeMismatchException) {
                tme = (TypeMismatchException)ex;
            } else if (wse == null && ex instanceof WrongSizeException) {
                wse = (WrongSizeException)ex;
            }
        }
        
        if (tme != null) {
            throw tme;
        }
        
        if (wse != null) {
            throw wse;
        }
        
        if (type != null) {
            throw new InvalidCallException(this, type, typeName);
        } else {
            throw new InvalidNameException(typeName);
        }
    }

    /**
     * Tests the given method to see if it is a factory method for the given
     * type and parameters.  If the method returns null, the method is the
     * correct factory method and may be invoked with the given parameters.
     * Otherwise the result is the appropriate {@link WrongSizeException},
     * {@link TypeMismatchException}, {@link InvalidCallException}, or
     * {@link InvalidNameException} which should be thrown if no other 
     * appropriate method can be found through further searching.
     *
     * @param method the method to test
     * @param type the excepted result type as a RefObject
     * @param typeName the excepted result type's name, may be null
     * @param params params passed by the caller
     * @return null if method matches type/params; an exception otherwise
     */
    private JmiException checkCreateMethod(
        Method method,
        Class<?> resultType,
        RefObject type,
        String typeName,
        List<?> params)
    {
        if (resultType.isAssignableFrom(method.getReturnType()) &&
            method.getName().startsWith("create")) {
    
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != params.size()) {
                return new WrongSizeException(type, typeName);
            }
    
            for(int i = 0; i < paramTypes.length; i++) {
                Object param = params.get(i);
                if (param == null) {
                    // Assume a type match.
                    continue;
                }
                
                Class<?> callerParamType = param.getClass();
                Class<?> paramType = paramTypes[i];
                
                if (paramType.isPrimitive()) {
                    paramType = Primitives.getWrapper(paramType);
                }
                
                if (!paramType.isAssignableFrom(callerParamType)) {
                    return new TypeMismatchException(
                        paramType, params.get(i), type, typeName);
                }
            }

            return null;
        }

        return new InvalidNameException(typeName);
    }

    /**
     * Retrieves the specified enumeration literal.
     * 
     * @param enumType meta-object representing the enumeration
     * @param enumTypeName enumeration type name
     * @param name enumeration literal
     * @return the RefEnum representing the given enumeration literal
     * @throws InternalJmiError if enumType and enumTypeName are both null or 
     *                          both non-null, or if this instance is not a
     *                          {@link RefClass} or {@link RefPackage}
     * @throws InvalidCallException if no method matches enumType
     * @throws InvalidNameException if no method matches enumTypeName
     * 
     */
    protected RefEnum getEnum(RefObject enumType, String enumTypeName, String name)
    {
        if (!(this instanceof RefClass || this instanceof RefPackage)) {
            throw new InternalJmiError(
                "bad call: getEnum only valid on RefClass/RefPackage");
        }
                
        if ((enumType == null && enumTypeName == null) ||
            (enumType != null && enumTypeName != null)) 
        {
            throw new InternalJmiError(
                "bad call: either enumType or enumTypeName must be non-null");
        }
        
        StringBuilder className = new StringBuilder();

        // REVIEW jvs 1-Jul-2008: Need some MDR testing (as reference
        // implementation, since JMI and MOF specs are silent on this) to see
        // whether it's legal to pass in any enumType (as opposed to only a
        // child of this package or class).  If that is legal, then to deal
        // with imports, we need to reactivate this code, but change it to
        // understand import spanning trees.  If that is illegal, then we can
        // just get rid of it.
        
        if (false) {
            ModelElement modelElem = (ModelElement)enumType;

            // REVIEW: Should this differ for a call from a class proxy?
            List<?> qualifiedName = modelElem.getQualifiedName();
            
            RefPackage pkg = null;
            Iterator<?> iter =  qualifiedName.iterator();
            while(iter.hasNext()) {
                String part = iter.next().toString();
                
                boolean lastPart = !iter.hasNext();
                if (!lastPart) {
                    if (pkg == null) {
                        pkg = refOutermostPackage();
                    } else {
                        try {
                            pkg = pkg.refPackage(part);
                        }
                        catch(InvalidNameException e) {
                            throw new InvalidCallException(this, enumType);
                        }
                    }
    
                    ModelElement pkgElem = (ModelElement)pkg.refMetaObject();
                    String pkgPrefix = 
                        getTag(pkgElem, TagUtil.TAGID_PACKAGE_PREFIX);
                    if (pkgPrefix != null) {
                        className.append(pkgPrefix);
                    }
                }
                
                if (className.length() > 0) {
                    className.append('.');
                }
                
                if (lastPart) {
                    className.append(
                        StringUtil.mangleIdentifier(
                            part, 
                            StringUtil.IdentifierType.CAMELCASE_INIT_UPPER));
                } else {
                    className.append(
                        StringUtil.mangleIdentifier(
                            part, StringUtil.IdentifierType.ALL_LOWER));
                }
                
            }
            className.append("Enum");
        }

        if (enumTypeName == null) {

            // REVIEW jvs 1-Jul-2008:  If we can get rid of
            // the deactivated code above, then this code can stay;
            // otherwise, it should be eliminated instead.
            
            ModelElement modelElem = (ModelElement) enumType;
            List<?> qualifiedName = modelElem.getQualifiedName();
            
            className
                .append(getClass().getPackage().getName())
                .append('.')
                .append(qualifiedName.get(qualifiedName.size() - 1))
                .append("Enum");
        } else {
            // REVIEW: Should this differ for a call from a class proxy?
            className
                .append(getClass().getPackage().getName())
                .append('.')
                .append(enumTypeName)
                .append("Enum");
        }
        
        
        try {
            return getEnum(className.toString(), name);
        }
        catch (Exception e) {
            // fall through
        }
        
        if (enumTypeName == null) {
            throw new InvalidCallException(this, enumType);
        } else {
            throw new InvalidNameException(enumTypeName);
        }
    }

    private RefEnum getEnum(String className, String literal) 
    throws 
        ClassNotFoundException, NoSuchMethodException, IllegalAccessException, 
        InvocationTargetException
    {
        Class<? extends RefEnum> cls =
            Class.forName(className.toString()).asSubclass(RefEnum.class);
        
        Method method = cls.getMethod("forName", String.class);

        RefEnum refEnum = cls.cast(method.invoke(null, literal));
        return refEnum;
    }

    /**
     * Invokes the given method on this instance using reflection.  
     * The result is cast to be of type <code>cls</code>, 
     * unless <code>cls == Void.class</code> in which case null is always 
     * returned.  Invocation exceptions are wrapped in {@link InternalJmiError}.
     * 
     * @param <E> return type
     * @param returnType return type class
     * @param method method to invoke
     * @return the result of the method invocation (see above)
     */
    protected <E> E invokeMethod(Class<E> returnType, Method method)
    {
        return invokeMethod(returnType, this, method);
    }
    
    /**
     * Invokes the given method using reflection.  The result is cast to be
     * of type <code>cls</code>, unless <code>cls == Void.class</code> in
     * which case null is always returned.  Invocation exceptions are
     * wrapped in {@link InternalJmiError}.
     * 
     * @param <E> return type
     * @param returnType return type class
     * @param instance instance on which to invoke the method
     * @param method method to invoke
     * @param params method parameters
     * @return the result of the method invocation (see above)
     */
    protected <E> E invokeMethod(
        Class<E> returnType, Object instance, Method method, Object... params)
    {
        try {
            Object result = method.invoke(instance, params);
            if (returnType == Void.class) {
                assert(result == null);
                return null;
            }
            
            return returnType.cast(result);
        }
        catch(Exception e) {
            throw new InternalJmiError(e);
        }
    }
    
    public final MetamodelInitializer getInitializer()
    {
        return initializer;
    }
    
    protected final MetamodelInitializer getCurrentInitializer()
    {
        MetamodelInitializer tlsInitializer =
            MetamodelInitializer.getCurrentInitializer();
        if (tlsInitializer != null) {
            return tlsInitializer;
        } else {
            return initializer;
        }
    }
    
    protected String getTag(ModelElement modelElem, String tagId)
    {
        RefAssociation attachesTo =
            refMetaObject().refImmediatePackage().refAssociation("AttachesTo");
        
        Collection<Tag> tags =
            GenericCollections.asTypedCollection(
                attachesTo.refQuery("modelElement", modelElem), Tag.class);
        for(Tag tag: tags) {
            if (tag.getTagId().equals(tagId)) {
                return tag.getValues().get(0).toString();
            }
        }
        
        return null;
    }
    
    /**
     * Retrieves the {@link EnkiMDRepository} that stores this object.
     * 
     * @return the EnkiMDRepository that stores this object
     */
    public abstract EnkiMDRepository getRepository();
    
    protected void logJmi(String msg)
    {
        if (!loggingEnabled) {
            return;
        }
        
        log.finer(getClass().getSimpleName() + ": " + msg);
    }
}

// End RefBaseObjectBase.java
