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
package org.eigenbase.enki.hibernate.mbean;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.mbean.*;

/**
 * HibernateMDRepositoryMBeanFactory creates instances of 
 * {@link HibernateMDRepositoryMBean}.  This factory class allows the MBean's
 * constructor to be obscured from JMX.
 * 
 * @author Stephan Zuercher
 */
public class HibernateMDRepositoryMBeanFactory
{
    private HibernateMDRepositoryMBeanFactory()
    {
    }
    
    public static EnkiRepository create(
        HibernateMDRepository repos)
    {
        return new HibernateMDRepositoryMBean(repos);
    }
}

// End HibernateMDRepositoryMBeanFactory.java
