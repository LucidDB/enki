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
package org.eigenbase.enki.test.events;

import org.junit.*;
import org.netbeans.api.mdr.events.*;

/**
 * TransactionEventValidator validates transaction begin and end events.
 * 
 * @author Stephan Zuercher
 */
public class TransactionEventValidator extends EventValidator
{
    /** Whether the event is expected to be a begin or end event. */
    private final boolean isBeginTxn;
    
    public TransactionEventValidator(
        EventType expectedEventType, boolean isBeginTxn)
    {
        super(expectedEventType);
        
        this.isBeginTxn = isBeginTxn;
    }
    
    @Override
    public void validateEvent(TransactionEvent event, int seq)
    {
        int type = 
            isBeginTxn 
                ? TransactionEvent.EVENT_TRANSACTION_START 
                : TransactionEvent.EVENT_TRANSACTION_END; 
        Assert.assertTrue(toString(), event.isOfType(type));
    }
    
    public String toString()
    {
        return super.toString() + "(begin=" + isBeginTxn + ")";
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        return new TransactionEventValidator(newEventType, isBeginTxn);
    }
}

// End TransactionEventValidator.java
