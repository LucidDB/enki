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
package org.eigenbase.enki.hibernate.storage;

import java.util.*;

/**
 * HibernateConstraintViolationException represents one or more metamodel 
 * constraint violations encountered while persisting one or more model 
 * elements.
 * 
 * @author Stephan Zuercher
 */
public class HibernateConstraintViolationException
    extends RuntimeException
{
    private static final long serialVersionUID = -5257723747016060501L;

    public HibernateConstraintViolationException(List<String> messages)
    {
        super(buildMessage(messages));
    }

    private static String buildMessage(List<String> messages)
    {
        StringBuilder b = new StringBuilder();
        switch(messages.size()) {
        case 0:
            b.append("Unknown Constraint Violation");
            break;
            
        case 1:
            b.append("Constraint Violation:\n");
            break;
            
        default:
            b.append("Constraint Violations:\n");
            break;
        }
        
        int index = 1;
        for(String message: messages) {
            b.append(index).append(". ").append(message).append('\n');
        }

        return b.toString();
    }
}

// End ConstraintViolationException.java
