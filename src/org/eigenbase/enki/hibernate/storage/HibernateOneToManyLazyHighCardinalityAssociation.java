/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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
package org.eigenbase.enki.hibernate.storage;

/**
 * HibernateOneToManyLazyHighCardinalityAssociation represents an association
 * between two metamodel types with especially lazy loading semantics and
 * a Hibernate mapping that reduces query size.  The association
 * to metamodel instances is not known to Hibernate, which allows us to 
 * avoid loading instance objects except when truly necessary.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyLazyHighCardinalityAssociation
    extends HibernateOneToManyLazyAssociation
{
    public HibernateOneToManyLazyHighCardinalityAssociation()
    {
        super();
    }
    
    public Kind getKind()
    {
        return Kind.ONE_TO_MANY_HIGH_CARDINALITY;
    }
}

// End HibernateOneToManyLazyHighCardinalityAssociation.java
