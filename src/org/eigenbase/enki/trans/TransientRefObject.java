/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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
package org.eigenbase.enki.trans;

import javax.jmi.reflect.*;

/**
 * TransientRefObject represents a transient repository object. 
 * 
 * @author Stephan Zuercher
 */
public interface TransientRefObject
{
    /** Internal use only.  Allows on object which exists as the value of
     * another object's Attribute to record its immediate composite.
     * 
     * @param owner object which owns this object as an Attribute
     */
    public void markOwner(RefObject owner);
    
    /**
     * Annotates this object with the given String.
     * 
     * @param annotation annotation for this object, may be null
     */
    public void annotate(String annotation);
    
    /**
     * Retrieves this object's annotation.
     * 
     * @return this object annotation, may be null
     */
    public String annotation();
}

// End TransientRefObject.java
