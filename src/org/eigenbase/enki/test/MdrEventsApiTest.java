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
package org.eigenbase.enki.test;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.test.events.*;
import org.junit.*;
import org.junit.runner.*;
import org.netbeans.api.mdr.events.*;

import eem.sample.*;
import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * Tests the MDR Events API.
 * 
 * See the org.netbeans.api.mdr.events package.
 * 
 * @author Stephan Zuercher
 */
@RunWith(SelectiveLoggingTestRunner.class)
@SelectiveLoggingTestRunner.Exclude(MdrProvider.ENKI_TRANSIENT)
public class MdrEventsApiTest extends SampleModelTestBase
{
    private static final boolean PRINT_EVENTS = true;

    private static final long EVENT_TIMEOUT = 10000L;
    
    private EventValidatingChangeListener listener;
    
    @BeforeClass
    public static void createTestObjects() throws Exception
    {
        TxnEndEventListener listener = new TxnEndEventListener();
        
        getRepository().addListener(listener);
        getRepository().beginTrans(true);
        try {
            Car mustang =
                getSamplePackage().getCar().createCar("Ford", "Mustang", 2);
            
            Driver bullitt =
                getSamplePackage().getDriver().createDriver(
                    "Bullitt", "ABC555");
            
            mustang.setDriver(bullitt);
        }
        finally {
            getRepository().endTrans();
        }
        
        listener.waitForEvent();
        
        getRepository().removeListener(listener);
    }

    private void configureListener(int mask, EventValidator eventValidator)
    {
        if (listener != null) {
            destroyListener();
        }
        
        listener = 
            new EventValidatingChangeListener(eventValidator, PRINT_EVENTS);
        
        getRepository().addListener(listener, mask);
    }

    @After
    public void destroyListener()
    {
        if (listener == null) {
            return;
        }
        
        getRepository().removeListener(listener);
        listener = null;
    }
    
    @Test
    public void testTransactionRollbackEvents()
    {
        testTransactionEvents(true);
    }
    
    @Test
    public void testTransactionCommitEvents()
    {
        testTransactionEvents(false);
    }
    
    private void testTransactionEvents(boolean rollback)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;
        
        configureListener(
            TransactionEvent.EVENTMASK_TRANSACTION | 
                InstanceEvent.EVENTMASK_INSTANCE,
            new DelegatingEventValidator(
                new TransactionEventValidator(EventType.PLANNED, true),
                new CreateInstanceEventValidator(
                    EventType.PLANNED,
                    getSamplePackage().getPerson(),
                    Person.class,
                    Arrays.asList((Object)"John Doe")),
                new TransactionEventValidator(EventType.PLANNED, false),
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2)));
        
        getRepository().beginTrans(true);
        try {
            getSamplePackage().getPerson().createPerson("John Doe");
        } finally {
            getRepository().endTrans(rollback);
        }

        waitAndCheckErrors();
    }

    private void waitAndCheckErrors()
    {
        int numEventsExpected = listener.getNumEventsExpected();
        
        boolean timedOut = 
            listener.waitForSomeEvents(numEventsExpected, EVENT_TIMEOUT);
        
        listener.rethrow();
        
        Assert.assertFalse(
            "timed out before " + numEventsExpected + " events", 
            timedOut);
    }

    @Test
    public void testCreateInstanceEventsWithRollback()
    {
        testCreateInstanceEvents(true, CreateMethod.CLASS_PROXY);
    }
    
    @Test
    public void testRefCreateInstanceEventsWithRollback()
    {
        testCreateInstanceEvents(true, CreateMethod.REFLECTIVE_API);
    }
    
    @Test
    public void testCreateInstanceEventsWithCommit()
    {
        testCreateInstanceEvents(false, CreateMethod.CLASS_PROXY);
    }

    @Test
    public void testRefCreateInstanceEventsWithCommit()
    {
        testCreateInstanceEvents(false, CreateMethod.CLASS_PROXY);
    }

    private void testCreateInstanceEvents(
        boolean rollback, CreateMethod method)
    {
        final List<Object> carArgs1 = 
            Arrays.asList(new Object[] { "Ford", "Mustang", new Integer(2) });
        final List<Object> carArgs2 = 
            Arrays.asList(new Object[] { "Dodge", "Charger", new Integer(2) });
        final List<Object> carArgs3 = null;
        
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;
        
        configureListener(
            InstanceEvent.EVENTMASK_INSTANCE,
            new DelegatingEventValidator(
                new CreateInstanceEventValidator(
                    EventType.PLANNED, 
                    getSamplePackage().getCar(), 
                    Car.class, 
                    carArgs1),
                new CreateInstanceEventValidator(
                    EventType.PLANNED, 
                    getSamplePackage().getCar(), 
                    Car.class, 
                    carArgs2),
                new CreateInstanceEventValidator(
                    EventType.PLANNED, 
                    getSamplePackage().getCar(), 
                    Car.class, 
                    carArgs3),
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2)));
        
        getRepository().beginTrans(true);
        try {
            switch(method) {
            case CLASS_PROXY: 
                getSamplePackage().getCar().createCar("Ford", "Mustang", 2);
                getSamplePackage().getCar().createCar("Dodge", "Charger", 2);
                Car c = getSamplePackage().getCar().createCar();
                c.setMake("Chevrolet");
                c.setModel("Camaro");
                c.setDoors(2);
                break;
                
            case REFLECTIVE_API:
                RefClass carClass = getSamplePackage().getCar();
                
                carClass.refCreateInstance(carArgs1);
                carClass.refCreateInstance(carArgs2);
                RefObject o = carClass.refCreateInstance(carArgs3);
                o.refSetValue("make", "Chevrolet");
                o.refSetValue("model", "Camaro");
                o.refSetValue("doors", 2);
                break;
                
            default:
                Assert.fail("Unknown CreateMethod: " + method);
            }
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }
    
    @Test
    public void testDeleteInstanceEventsWithRollback()
    {
        testDeleteInstanceEvents(true, false);
    }
    
    @Test
    public void testDeleteInstanceEventsPreviewWithRollback()
    {
        testDeleteInstanceEvents(true, true);
    }
    
    @Test
    public void testDeleteInstanceEventsWithCommit()
    {
        testDeleteInstanceEvents(false, false);
    }
    
    @Test
    public void testDeleteInstanceEventsPreviewWithCommit()
    {
        testDeleteInstanceEvents(false, true);
    }
    
    private void testDeleteInstanceEvents(
        boolean rollback, boolean preview)
    {
        if (!getRepository().supportsPreviewRefDelete()) {
            return;
        }
        
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        // Swallow events silently.  Want a listener so that we can wait
        // for all the events generated to be delivered.  Otherwise, the
        // new association remove event listener below may receive some
        // stale CHANGED events from the object creation step.
        configureListener(
            InstanceEvent.EVENT_INSTANCE_CREATE,
            new LenientEventValidator(2));

        String refDeleteMofId;
        getRepository().beginTrans(true);
        try {
            IceCreamCone cone = 
                getSpecialPackage().getIceCreamCone().createIceCreamCone(
                    IceCreamFlavorEnum.VANILLA, 2, true);
            refDeleteMofId = cone.refMofId();
        }
        finally {
            getRepository().endTrans(false);
        }

        waitAndCheckErrors();
        destroyListener();
        
        List<EventValidator> validators =
            new ArrayList<EventValidator>();
        validators.add(
            new DeleteInstanceEventValidator(
                EventType.PLANNED, refDeleteMofId));
        // For preview, don't expect post-change event
        if (!preview) {
            validators.add(
                new DuplicateEventValidator(postTxnEventType, 0));
        }
        configureListener(
            InstanceEvent.EVENTMASK_INSTANCE, 
            new DelegatingEventValidator(
                validators.toArray(
                    new EventValidator[0])));
        
        getRepository().beginTrans(true);
        try {
            IceCreamCone cone = 
                (IceCreamCone)getRepository().getByMofId(refDeleteMofId);

            if (preview) {
                getRepository().previewRefDelete(cone);
            } else {
                cone.refDelete();
            }

            if (preview) {
                // verify that the cone is still there
                IceCreamCone cone2 = 
                    (IceCreamCone)getRepository().getByMofId(refDeleteMofId);
                Assert.assertEquals(cone, cone2);
            }
            
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }
    
    @Test
    public void testDeleteInstanceEventCascadeWithRollback()
    {
        testDeleteInstanceEventCascade(true, false);
    }
    
    @Test
    public void testDeleteInstanceEventCascadePreviewWithRollback()
    {
        testDeleteInstanceEventCascade(true, true);
    }
    
    @Test
    public void testDeleteInstanceEventCascadeWithCommit()
    {
        testDeleteInstanceEventCascade(false, false);
    }
    
    @Test
    public void testDeleteInstanceEventCascadePreviewWithCommit()
    {
        testDeleteInstanceEventCascade(false, true);
    }
    
    private void testDeleteInstanceEventCascade(
        boolean rollback, boolean preview)
    {
        if (!getRepository().supportsPreviewRefDelete()) {
            return;
        }
        
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        // Swallow events silently.  Want a listener so that we can wait
        // for all the events generated to be delivered.  Otherwise, the
        // new association remove event listener below may receive some
        // stale CHANGED events from the object creation step.
        configureListener(
            InstanceEvent.EVENT_INSTANCE_CREATE,
            new LenientEventValidator(8));

        String[] refDeleteMofIds = { null, null, null };
        getRepository().beginTrans(true);
        try {
            Bus bus = getSamplePackage().getBus().createBus(
                "GMC", "Yellow School Bus", 2);
            
            Driver driver = 
                getSamplePackage().getDriver().createDriver(
                    "Otto Mann", "--suspended--");
            
            bus.setDriver(driver);
            refDeleteMofIds[0] = bus.refMofId();
            
            AreaCode areaCode = 
                getSpecialPackage().getAreaCode().createAreaCode("415", true);
            PhoneNumber phoneNumber =
                getSpecialPackage().getPhoneNumber().createPhoneNumber(
                    areaCode, "555-1234");
            refDeleteMofIds[1] = phoneNumber.refMofId();
            refDeleteMofIds[2] = areaCode.refMofId();
        }
        finally {
            getRepository().endTrans(false);
        }

        waitAndCheckErrors();
        destroyListener();
        
        List<EventValidator> validators =
            new ArrayList<EventValidator>();
        validators.add(
            new DeleteInstanceEventValidator(
                EventType.PLANNED, refDeleteMofIds[0]));
        validators.add(
            new AssociationRemoveEventValidator(
                EventType.PLANNED, "Driven",
                Bus.class, "make", "GMC",
                Driver.class, "name", "Otto Mann"));
        validators.add(
            new DeleteInstanceEventValidator(
                EventType.PLANNED, refDeleteMofIds[1]));
        validators.add(
            new DeleteInstanceEventValidator(
                EventType.PLANNED, refDeleteMofIds[2]));
        // Don't expect post-change events for preview
        if (!preview) {
            validators.add(
                new DuplicateEventValidator(postTxnEventType, 0));
            validators.add(
                new AssociationRemoveEventValidator(
                    postTxnEventType, "Driven",
                    Bus.class, null, null, // will be an invalid object
                    Driver.class, "name", "Otto Mann"));
            validators.add(
                new DuplicateEventValidator(postTxnEventType, 2));
            validators.add(
                new DuplicateEventValidator(postTxnEventType, 3));
        }
        configureListener(
            InstanceEvent.EVENTMASK_INSTANCE | 
                AssociationEvent.EVENTMASK_ASSOCIATION, 
            new DelegatingEventValidator(
                validators.toArray(
                    new EventValidator[0])));
        
        getRepository().beginTrans(true);
        try {
            Bus bus = (Bus)getRepository().getByMofId(refDeleteMofIds[0]);
            PhoneNumber phoneNumber = 
                (PhoneNumber)getRepository().getByMofId(refDeleteMofIds[1]);

            // Delete bus, cascades to "remove" AssociationEvent
            if (preview) {
                getRepository().previewRefDelete(bus);
            } else {
                bus.refDelete();
            }
            
            // Delete phoneNumber, cascades to delete areaCode
            if (preview) {
                getRepository().previewRefDelete(phoneNumber);
            } else {
                phoneNumber.refDelete();
            }

            if (preview) {
                // verify that everything is still there
                Bus bus2 = (Bus)getRepository().getByMofId(refDeleteMofIds[0]);
                PhoneNumber phoneNumber2 = 
                    (PhoneNumber)getRepository().getByMofId(refDeleteMofIds[1]);
                Assert.assertEquals(bus, bus2);
                Assert.assertEquals("Otto Mann", bus2.getDriver().getName());
                Assert.assertEquals(bus, bus2.getDriver().getDriven());
                Assert.assertEquals(phoneNumber, phoneNumber2);
                Assert.assertEquals(
                    refDeleteMofIds[2],
                    phoneNumber2.getAreaCode().refMofId());
            }
            
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }
    
    @Test
    public void testAssociationAddEventsWithRollback()
    {
        testAssociationAddEvents(true, AssocMethod.REFERENCE);
    }
    
    @Test
    public void testAssociationProxyAddEventsWithRollback()
    {
        testAssociationAddEvents(true, AssocMethod.ASSOCIATION_PROXY);
    }
    
    @Test
    public void testAssociationRefAddEventsWithRollback()
    {
        testAssociationAddEvents(true, AssocMethod.REFLECTIVE_API);
    }
    
    @Test
    public void testAssociationAddEventsWithCommit()
    {
        testAssociationAddEvents(false, AssocMethod.REFERENCE);
    }
    
    @Test
    public void testAssociationProxyAddEventsWithCommit()
    {
        testAssociationAddEvents(false, AssocMethod.ASSOCIATION_PROXY);
    }
    
    @Test
    public void testAssociationRefAddEventsWithCommit()
    {
        testAssociationAddEvents(false, AssocMethod.REFLECTIVE_API);
    }
    
    private void testAssociationAddEvents(boolean rollback, AssocMethod method)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            MDRChangeEvent.EVENTMASK_ON_ASSOCIATION,
            new DelegatingEventValidator(
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Driven",
                    Car.class, "model", "Mustang",
                    Driver.class, "name", "Bullitt"),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Ridden",
                    Bus.class, "model", "E4500",
                    Passenger.class, "name", "John Madden"),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Ridden",
                    Bus.class, "model", "E4500",
                    Passenger.class, "name", "Turducken"),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Registered",
                    Bus.class, "model", "E4500",
                    State.class, "name", "CA"),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Registered",
                    Bus.class, "model", "E4500",
                    State.class, "name", "NV"),
                method == AssocMethod.REFERENCE
                    ? new AssociationAddEventValidator(
                        EventType.PLANNED, "Registrar",
                        State.class, "name", "CA",
                        Bus.class, "model", "C2045")
                    : new AssociationAddEventValidator(
                        EventType.PLANNED, "Registered",
                        Bus.class, "model", "C2045",
                        State.class, "name", "CA"),
                method == AssocMethod.REFERENCE
                    ? new AssociationAddEventValidator(
                        EventType.PLANNED, "Registrar",
                        State.class, "name", "OR",
                        Bus.class, "model", "C2045")
                    : new AssociationAddEventValidator(
                        EventType.PLANNED, "Registered",
                        Bus.class, "model", "C2045",
                        State.class, "name", "OR"),
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2),
                new DuplicateEventValidator(postTxnEventType, 3),
                new DuplicateEventValidator(postTxnEventType, 4),
                new DuplicateEventValidator(postTxnEventType, 5),
                new DuplicateEventValidator(postTxnEventType, 6)
            ));

        createAssociationObjects(rollback, method);

        waitAndCheckErrors();
    }
    
    @Test
    public void testOrderedAssociationAddEventsWithRollback()
    {
        testOrderedAssociationAddEvents(true);
    }
    
    @Test
    public void testOrderedAssociationAddEventsWithCommit()
    {
        testOrderedAssociationAddEvents(false);
    }
    
    private void testOrderedAssociationAddEvents(boolean rollback)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            MDRChangeEvent.EVENTMASK_ON_ASSOCIATION,
            new DelegatingEventValidator(
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null), // pos = end (0)
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null), // pos = end (1)
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null, 1),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null, 0),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null), // pos = end (4)
                    
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null), // pos = end (0)
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null, 0),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null), // pos = end (2)
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null, 2),
                new AssociationAddEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null), // pos = end (4)

                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2),
                new DuplicateEventValidator(postTxnEventType, 3),
                new DuplicateEventValidator(postTxnEventType, 4),
                new DuplicateEventValidator(postTxnEventType, 5),
                new DuplicateEventValidator(postTxnEventType, 6),
                new DuplicateEventValidator(postTxnEventType, 7),
                new DuplicateEventValidator(postTxnEventType, 8),
                new DuplicateEventValidator(postTxnEventType, 9)
            ));
        
        createOrderedAssociationObjects(rollback, false);

        waitAndCheckErrors();
    }

    @Test
    public void testAssociationRemoveEventsWithRollback()
    {
        testAssociationRemoveEvents(true, AssocMethod.REFERENCE);
    }
    
    @Test
    public void testAssociationProxyRemoveEventsWithRollback()
    {
        testAssociationRemoveEvents(true, AssocMethod.ASSOCIATION_PROXY);
    }
    
    @Test
    public void testAssociationRefRemoveEventsWithRollback()
    {
        testAssociationRemoveEvents(true, AssocMethod.REFLECTIVE_API);
    }
    
    @Test
    public void testAssociationRemoveEventsWithCommit()
    {
        testAssociationRemoveEvents(false, AssocMethod.REFERENCE);
    }
    
    @Test
    public void testAssociationProxyRemoveEventsWithCommit()
    {
        testAssociationRemoveEvents(false, AssocMethod.ASSOCIATION_PROXY);
    }
    
    @Test
    public void testAssociationRefRemoveEventsWithCommit()
    {
        testAssociationRemoveEvents(false, AssocMethod.REFLECTIVE_API);
    }
    
    private void testAssociationRemoveEvents(
        boolean rollback, AssocMethod method)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        // Swallow events silently.  Want a listener so that we can wait
        // for all the events generated to be delivered.  Otherwise, the
        // new association remove event listener below may receive some
        // stale CHANGED events from the object creation step.
        configureListener(
            AssociationEvent.EVENTMASK_ASSOCIATION,
            new LenientEventValidator(14));
        
        String[] mofIds = 
            createAssociationObjects(false, AssocMethod.REFERENCE);

        waitAndCheckErrors();
        destroyListener();
        
        configureListener(
            MDRChangeEvent.EVENTMASK_ON_ASSOCIATION, 
            new DelegatingEventValidator(
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Driven",
                    Car.class, "model", "Mustang",
                    Driver.class, "name", "Bullitt"),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Ridden",
                    Bus.class, "model", "E4500",
                    Passenger.class, "name", "Turducken"),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Registered",
                    Bus.class, "model", "E4500",
                    State.class, "name", "NV"),
                method == AssocMethod.REFERENCE
                    ? new AssociationRemoveEventValidator(
                        EventType.PLANNED, "Registrar",
                        State.class, "name", "CA",
                        Bus.class, "model", "C2045")
                    : new AssociationRemoveEventValidator(
                        EventType.PLANNED, "Registered",
                        Bus.class, "model", "C2045",
                        State.class, "name", "CA"),
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2),
                new DuplicateEventValidator(postTxnEventType, 3)));
        
        int i = 0;
        getRepository().beginTrans(true);
        try {
            // 1-to-1
            Car mustang = (Car)getRepository().getByMofId(mofIds[i++]);
            
            Driver bullitt = (Driver)getRepository().getByMofId(mofIds[i++]);

            switch(method) {
            case REFERENCE:
                bullitt.setDriven(null);
                break;
                
            case ASSOCIATION_PROXY: 
                getSamplePackage().getDrives().remove(mustang, bullitt);
                break;

            case REFLECTIVE_API: 
                getSamplePackage().getDrives().refRemoveLink(mustang, bullitt);
                break;
                
            default:
                Assert.fail("unknown AssocMethod: " + method);
            }
            
            // 1-to-many
            Bus bus = (Bus)getRepository().getByMofId(mofIds[i++]);

            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Passenger);
            Passenger turducken = (Passenger)getRepository().getByMofId(mofIds[i++]);

            switch(method) {
            case REFERENCE:
                bus.getRider().remove(turducken);
                break;
                
            case ASSOCIATION_PROXY:
                getSamplePackage().getRides().remove(bus, turducken);
                break;
                
            case REFLECTIVE_API:
                getSamplePackage().getRides().refRemoveLink(bus, turducken);
                break;
            }
            
            // many-to-many
            Bus greyhound = (Bus)getRepository().getByMofId(mofIds[i++]);

            State ca = (State)getRepository().getByMofId(mofIds[i++]);
            State nv = (State)getRepository().getByMofId(mofIds[i++]);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof State);

            switch(method) {
            case REFERENCE:
                bus.getRegistrar().remove(nv);
                ca.getRegistered().remove(greyhound);
                break;
                
            case ASSOCIATION_PROXY:
                getSamplePackage().getRegistrations().remove(bus, nv);
                getSamplePackage().getRegistrations().remove(greyhound, ca);
                break;
                
            case REFLECTIVE_API:
                getSamplePackage().getRegistrations().refRemoveLink(bus, nv);
                getSamplePackage().getRegistrations().refRemoveLink(
                    greyhound, ca);
                break;
            }
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }
    
    @Test
    public void testOrderedAssociationRemoveEventsWithRollback()
    {
        testOrderedAssociationRemoveEvents(true);
    }
    
    @Test
    public void testOrderedAssociationRemoveEventsWithCommit()
    {
        testOrderedAssociationRemoveEvents(false);
    }
    
    private void testOrderedAssociationRemoveEvents(boolean rollback)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            AssociationEvent.EVENTMASK_ASSOCIATION,
            new LenientEventValidator(20));
        
        String[] mofIds = createOrderedAssociationObjects(false, false);

        waitAndCheckErrors();
        destroyListener();
        
        configureListener(
            MDRChangeEvent.EVENTMASK_ON_ASSOCIATION, 
            new DelegatingEventValidator(
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null, 2),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null, 1),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null, 0),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null, 1),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null, 0),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null, 3),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null, 2),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null, 1),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null, 0),
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null, 0),
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2),
                new DuplicateEventValidator(postTxnEventType, 3),
                new DuplicateEventValidator(postTxnEventType, 4),
                new DuplicateEventValidator(postTxnEventType, 5),
                new DuplicateEventValidator(postTxnEventType, 6),
                new DuplicateEventValidator(postTxnEventType, 7),
                new DuplicateEventValidator(postTxnEventType, 8),
                new DuplicateEventValidator(postTxnEventType, 9)
            ));
        
        int i = 0;
        getRepository().beginTrans(true);
        try {
            // 1-to-many
            Entity16 e16 = (Entity16)getRepository().getByMofId(mofIds[i++]);

            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            
            e16.getEntity17().remove(2);
            e16.getEntity17().remove(1);
            e16.getEntity17().remove(0);
            e16.getEntity17().remove(1);
            e16.getEntity17().remove(0);
            
            // many-to-many
            Entity22 e22 = (Entity22)getRepository().getByMofId(mofIds[i++]);

            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);

            e22.getEntity23().remove(3);
            e22.getEntity23().remove(2);
            e22.getEntity23().remove(1);
            e22.getEntity23().remove(0);
            e22.getEntity23().remove(0);
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }

    @Test
    public void testOrderedAssociationSetEventsWithRollback()
    {
        testOrderedAssociationSetEvents(true);
    }

    @Test
    public void testOrderedAssociationSetEventsWithCommit()
    {
        testOrderedAssociationSetEvents(false);
    }

    private void testOrderedAssociationSetEvents(boolean rollback)
    {
        // Netbeans + MDRJDBC causes a ClassCastException on calls to
        // set(int, Object) on List instances returned by association
        // accessors.  So, skip the test.
        if (getMdrProvider() == MdrProvider.NETBEANS_MDR) {
            getTestLogger().info(
                "Skipping testOrderedAssociationSetEvents for Netbeans");
            return;
        }
        
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            AssociationEvent.EVENTMASK_ASSOCIATION,
            new LenientEventValidator(20));
        
        String[] mofIds = createOrderedAssociationObjects(false, true);

        waitAndCheckErrors();
        destroyListener();
        
        configureListener(
            MDRChangeEvent.EVENTMASK_ON_ASSOCIATION, 
            new DelegatingEventValidator(
                new AssociationSetEventValidator(
                    EventType.PLANNED, "Entity16",
                    Entity16.class, null, null,
                    Entity17.class, null, null,
                    Entity17.class, null, null, 2),
                new AssociationSetEventValidator(
                    EventType.PLANNED, "Entity22",
                    Entity22.class, null, null,
                    Entity23.class, null, null,
                    Entity23.class, null, null, 2),

                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1)
            ));
        
        int i = 0;
        getRepository().beginTrans(true);
        try {
            // 1-to-many
            Entity16 e16 = (Entity16)getRepository().getByMofId(mofIds[i++]);

            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity17);
            
            Entity17 extraE17 = 
                (Entity17)getRepository().getByMofId(mofIds[i++]);
            
            e16.getEntity17().set(2, extraE17);
            
            // many-to-many
            Entity22 e22 = (Entity22)getRepository().getByMofId(mofIds[i++]);

            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);
            Assert.assertTrue(
                getRepository().getByMofId(mofIds[i++]) instanceof Entity23);

            Entity23 extraE23 = 
                (Entity23)getRepository().getByMofId(mofIds[i++]);
            
            e22.getEntity23().set(2, extraE23);
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();        
    }
    
    private String[] createAssociationObjects(
        boolean rollback, AssocMethod assocMethod)
    {
        String[] mofIds = new String[9];
        
        int i = 0;
        
        getRepository().beginTrans(true);
        try {
            // 1-to-1
            Car mustang = 
                getSamplePackage().getCar().createCar("Ford", "Mustang", 2);
            mofIds[i++] = mustang.refMofId();
            
            Driver bullitt =
                getSamplePackage().getDriver().createDriver(
                    "Bullitt", "ABC999");
            mofIds[i++] = bullitt.refMofId();
     
            switch(assocMethod) {
            case REFERENCE:
                bullitt.setDriven(mustang);
                break;
                
            case ASSOCIATION_PROXY:
                getSamplePackage().getDrives().add(mustang, bullitt);
                break;
                
            case REFLECTIVE_API:
                getSamplePackage().getDrives().refAddLink(mustang, bullitt);
                break;
                
            default:
                Assert.fail("unknown AssocMethod: " + assocMethod);
            }
            
            // 1-to-many
            Bus bus =
                getSamplePackage().getBus().createBus("MCI", "E4500", 3);
            mofIds[i++] = bus.refMofId();
            
            Passenger madden =
                getSamplePackage().getPassenger().createPassenger(
                    "John Madden");
            mofIds[i++] = madden.refMofId();
            
            Passenger turducken =
                getSamplePackage().getPassenger().createPassenger("Turducken");
            mofIds[i++] = turducken.refMofId();
            
            switch(assocMethod) {
            case REFERENCE:
                bus.getRider().add(madden);
                turducken.setRidden(bus);
                break;
                
            case ASSOCIATION_PROXY:
                getSamplePackage().getRides().add(bus, madden);
                getSamplePackage().getRides().add(bus, turducken);
                break;
                
            case REFLECTIVE_API:
                getSamplePackage().getRides().refAddLink(bus, madden);
                getSamplePackage().getRides().refAddLink(bus, turducken);
                break;
            }
            // many-to-many
            Bus greyhound = 
                getSamplePackage().getBus().createBus("VanHool", "C2045", 3);
            mofIds[i++] = greyhound.refMofId();

            State ca = getSamplePackage().getState().createState("CA");
            State nv = getSamplePackage().getState().createState("NV");
            State or = getSamplePackage().getState().createState("OR");
            mofIds[i++] = ca.refMofId();
            mofIds[i++] = nv.refMofId();
            mofIds[i++] = or.refMofId();

            Registrations regAssoc = getSamplePackage().getRegistrations();
            switch(assocMethod) {
            case REFERENCE:
                bus.getRegistrar().add(ca);
                bus.getRegistrar().add(nv);

                ca.getRegistered().add(greyhound);
                or.getRegistered().add(greyhound);
                break;
                
            case ASSOCIATION_PROXY:
                regAssoc.add(bus, ca);
                regAssoc.add(bus, nv);

                regAssoc.add(greyhound, ca);
                regAssoc.add(greyhound, or);
                break;
                
            case REFLECTIVE_API:
                regAssoc.refAddLink(bus, ca);
                regAssoc.refAddLink(bus, nv);

                regAssoc.refAddLink(greyhound, ca);
                regAssoc.refAddLink(greyhound, or);
                break;
            }

            return mofIds;
        } finally {
            getRepository().endTrans(rollback);
        }
    }
    
    private String[] createOrderedAssociationObjects(
        boolean rollback, boolean createAdditional)
    {
        String[] mofIds = new String[createAdditional ? 14 : 12];
        
        int i = 0;
        
        getRepository().beginTrans(true);
        try {
            // 1-to-many
            Entity16 e16 =
                getSimplePackage().getEntity16().createEntity16();
            mofIds[i++] = e16.refMofId();
            
            Entity17[] e17s = {
                getSimplePackage().getEntity17().createEntity17(),
                getSimplePackage().getEntity17().createEntity17(),
                getSimplePackage().getEntity17().createEntity17(),
                getSimplePackage().getEntity17().createEntity17(),
                getSimplePackage().getEntity17().createEntity17(),
            };
            
            for(Entity17 e17: e17s) {
                mofIds[i++] = e17.refMofId();
            }            
            
            if (createAdditional) {
                Entity17 extraE17 = 
                    getSimplePackage().getEntity17().createEntity17();
                mofIds[i++] = extraE17.refMofId();
            }
            
            // final order by index into e17s: 3, 0, 2, 1, 4
            e16.getEntity17().add(e17s[0]);
            e16.getEntity17().add(e17s[1]);
            e16.getEntity17().add(1, e17s[2]);
            e16.getEntity17().add(0, e17s[3]);
            e16.getEntity17().add(e17s[4]);

            // many-to-many
            Entity22 e22 = getSimplePackage().getEntity22().createEntity22();
            mofIds[i++] = e22.refMofId();
            
            Entity23[] e23s = {
                getSimplePackage().getEntity23().createEntity23(),
                getSimplePackage().getEntity23().createEntity23(),
                getSimplePackage().getEntity23().createEntity23(),
                getSimplePackage().getEntity23().createEntity23(),
                getSimplePackage().getEntity23().createEntity23(),
            };
            
            for(Entity23 e23: e23s) {
                mofIds[i++] = e23.refMofId();
            }
            
            if (createAdditional) {
                Entity23 extraE23 = 
                    getSimplePackage().getEntity23().createEntity23();
                mofIds[i++] = extraE23.refMofId();
            }
            
            // final order by index into e23s: 1, 0, 3, 2, 4
            e22.getEntity23().add(e23s[0]);
            e22.getEntity23().add(0, e23s[1]);
            e22.getEntity23().add(e23s[2]);
            e22.getEntity23().add(2, e23s[3]);
            e22.getEntity23().add(e23s[4]);
            
            return mofIds;
        } finally {
            getRepository().endTrans(rollback);
        }
    }

    @Test
    public void testCompositeAssociationRemoveEventsWithRollback()
    {
        testCompositeAssociationRemoveEvents(true, false);
    }

    @Test
    public void testCompositeAssociationRemoveEventsPreviewWithRollback()
    {
        testCompositeAssociationRemoveEvents(true, true);
    }
    
    @Test
    public void testCompositeAssociationRemoveEventsWithCommit()
    {
        testCompositeAssociationRemoveEvents(false, false);
    }
    
    @Test
    public void testCompositeAssociationRemoveEventsPreviewWithCommit()
    {
        testCompositeAssociationRemoveEvents(false, true);
    }
    
    private void testCompositeAssociationRemoveEvents(
        boolean rollback, boolean preview)
    {
        if (!getRepository().supportsPreviewRefDelete()) {
            return;
        }
        
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        // Swallow events silently.  Want a listener so that we can wait
        // for all the events generated to be delivered.  Otherwise, the
        // new association remove event listener below may receive some
        // stale CHANGED events from the object creation step.
        configureListener(
            AssociationEvent.EVENTMASK_ASSOCIATION,
            new LenientEventValidator(4));

        String[] mofIds = createCompositeAssociationsObjects();

        waitAndCheckErrors();
        destroyListener();
        
        int pos = 
            getMdrProvider() == MdrProvider.ENKI_HIBERNATE 
                ? 0 
                : AssociationEvent.POSITION_NONE;

        List<AssociationRemoveEventValidator> validators =
            new ArrayList<AssociationRemoveEventValidator>();
        validators.add(
            new AssociationRemoveEventValidator(
                EventType.PLANNED, "building",
                Building.class, "address", "1510 Fashion Island Blvd.",
                Floor.class, "floorNumber", "2",
                pos));
        if (preview) {
            // In preview mode, we will hear "echoes" of the events too.
            validators.add(
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "floors",
                    Floor.class, "floorNumber", "2",
                    Building.class, "address", "1510 Fashion Island Blvd."));
        }
        validators.add(
            new AssociationRemoveEventValidator(
                EventType.PLANNED, "floor",
                Floor.class, "floorNumber", "2",
                Room.class, "roomNumber", "240"));
        if (preview) {
            // In preview mode, we will hear "echoes" of the events too.
            validators.add(
                new AssociationRemoveEventValidator(
                    EventType.PLANNED, "rooms",
                    Room.class, "roomNumber", "240",
                    Floor.class, "floorNumber", "2"));
        }
        // Can't access the objects on commit, so just verify types.
        // No commit events are expected for preview.
        if (!preview) {
            validators.add(
                new AssociationRemoveEventValidator(
                    postTxnEventType, "building",
                    Building.class, null, null,
                    Floor.class, null, null,
                    pos));
            validators.add(
                new AssociationRemoveEventValidator(
                    postTxnEventType, "floor",
                    Floor.class, null, null,
                    Room.class, null, null));
        }
        
        configureListener(
            MDRChangeEvent.EVENTMASK_ON_ASSOCIATION, 
            new DelegatingEventValidator(
                validators.toArray(new AssociationRemoveEventValidator[0])));
        
        getRepository().beginTrans(true);
        try {
            Building building = 
                (Building)getRepository().getByMofId(mofIds[0]);

            if (preview) {
                getRepository().previewRefDelete(building);
            } else {
                building.refDelete();
            }

            if (preview) {
                // verify that everything is still there
                Building building2 = 
                    (Building)getRepository().getByMofId(mofIds[0]);
                Assert.assertEquals(building, building2);
                Assert.assertEquals(1, building2.getFloors().size());
                Floor floor = building2.getFloors().get(0);
                Assert.assertEquals(mofIds[1], floor.refMofId());
                Assert.assertEquals(2, floor.getFloorNumber());
                Assert.assertEquals(building, floor.getBuilding());
                Assert.assertEquals(1, floor.getRooms().size());
                Room room = floor.getRooms().iterator().next();
                Assert.assertEquals(mofIds[2], room.refMofId());
                Assert.assertEquals(floor, room.getFloor());
            }
            
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }
    
    private String[] createCompositeAssociationsObjects()
    {
        getRepository().beginTrans(true);
        try {
            Building building = 
                getSpecialPackage().getBuilding().createBuilding(
                    "1510 Fashion Island Blvd.", "San Mateo", "CA", "94404");

            Floor floor = 
                getSpecialPackage().getFloor().createFloor(2, 10);
            
            building.getFloors().add(floor);
                
            Room room = getSpecialPackage().getRoom().createRoom(240, 20, 20);
                    
            floor.getRooms().add(room);
            
            return new String[] {
                building.refMofId(),
                floor.refMofId(),
                room.refMofId()
            };
        }
        finally {
            getRepository().endTrans();
        }
    }
    
    @Test
    public void testAttributeSetEventsWithRollback()
    {
        testAttributeSetEvents(true, AttributeMethod.METHOD);
    }
    
    @Test
    public void testAttributeRefSetEventsWithRollback()
    {
        testAttributeSetEvents(true, AttributeMethod.REFLECTIVE_API);
    }
    
    @Test
    public void testAttributeSetEventsWithCommit()
    {
        testAttributeSetEvents(false, AttributeMethod.METHOD);
    }
    
    @Test
    public void testAttributeRefSetEventsWithCommit()
    {
        testAttributeSetEvents(false, AttributeMethod.REFLECTIVE_API);
    }
    
    private void testAttributeSetEvents(
        boolean rollback, AttributeMethod method)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            AttributeEvent.EVENTMASK_ATTRIBUTE | 
                InstanceEvent.EVENTMASK_INSTANCE,
            new DelegatingEventValidator(
                new CreateInstanceEventValidator(
                    EventType.PLANNED,
                    getSamplePackage().getCar(),
                    Car.class,
                    null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "make", "Chevrolet", null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "model", "Camaro", null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "doors", 2, 0),
                    
                new CreateInstanceEventValidator(
                    EventType.PLANNED,
                    getSpecialPackage().getAreaCode(),
                    AreaCode.class,
                    null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "code", "650", null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "domestic", true, false),
                    
                new CreateInstanceEventValidator(
                    EventType.PLANNED,
                    getSpecialPackage().getPhoneNumber(),
                    PhoneNumber.class,
                    null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "areaCode", "code", 
                    AreaCode.class, "650",
                    null, null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "number", "555-1212", null),
                    
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2),
                new DuplicateEventValidator(postTxnEventType, 3),
                new DuplicateEventValidator(postTxnEventType, 4),
                new DuplicateEventValidator(postTxnEventType, 5),
                new DuplicateEventValidator(postTxnEventType, 6),
                new DuplicateEventValidator(postTxnEventType, 7),
                new DuplicateEventValidator(postTxnEventType, 8),
                new DuplicateEventValidator(postTxnEventType, 9)
            ));
        
        getRepository().beginTrans(true);
        try {
            switch(method) {
            case METHOD: 
                {
                    Car c = getSamplePackage().getCar().createCar();
                    c.setMake("Chevrolet");
                    c.setModel("Camaro");
                    c.setDoors(2);
                    
                    AreaCode ac = 
                        getSpecialPackage().getAreaCode().createAreaCode();
                    ac.setCode("650");
                    ac.setDomestic(true);
                    
                    PhoneNumber pn =
                        getSpecialPackage().getPhoneNumber().createPhoneNumber();
                    pn.setAreaCode(ac);
                    pn.setNumber("555-1212");
                }
                break;
                
            case REFLECTIVE_API:
                {
                    RefClass carClass = getSamplePackage().getCar();                
                    RefObject c = carClass.refCreateInstance(null);
                    c.refSetValue("make", "Chevrolet");
                    c.refSetValue("model", "Camaro");
                    c.refSetValue("doors", 2);
                    
                    RefClass areaCodeClass = getSpecialPackage().getAreaCode();
                    RefObject ac = areaCodeClass.refCreateInstance(null);
                    ac.refSetValue("code", "650");
                    ac.refSetValue("domestic", true);
                    
                    RefClass phoneNumberClass = 
                        getSpecialPackage().getPhoneNumber();
                    RefObject pn = phoneNumberClass.refCreateInstance(null);
                    pn.refSetValue("areaCode", ac);
                    pn.refSetValue("number", "555-1212");
                }                
                break;
                
            default:
                Assert.fail("Unknown AttributeMethod: " + method);
            }
        } finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }
    
    @Test
    public void testAttributeResetEventsWithRollback()
    {
        testAttributeResetEvents(true);
    }
    
    @Test
    public void testAttributeResetEventsWithCommit()
    {
        testAttributeResetEvents(false);
    }

    private void testAttributeResetEvents(boolean rollback)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            InstanceEvent.EVENTMASK_INSTANCE,
            new LenientEventValidator(4));
        
        String phoneNumberMofId;
        getRepository().beginTrans(true);
        try {
            AreaCode ac = 
                getSpecialPackage().getAreaCode().createAreaCode("408", true);
            
            PhoneNumber pn =
                getSpecialPackage().getPhoneNumber().createPhoneNumber(
                    ac, "555-5555");
            phoneNumberMofId = pn.refMofId();
        } finally {
            getRepository().endTrans(false);
        }

        waitAndCheckErrors();
        
        configureListener(
            AttributeEvent.EVENTMASK_ATTRIBUTE | 
                InstanceEvent.EVENTMASK_INSTANCE,
            new DelegatingEventValidator(
                new AttributeSetEventValidator(
                    EventType.PLANNED, "areaCode", "code",
                    null, null,
                    AreaCode.class, "408"),
            new DuplicateEventValidator(postTxnEventType, 0)));
        
        getRepository().beginTrans(true);
        try {
            PhoneNumber pn = 
                (PhoneNumber)getRepository().getByMofId(phoneNumberMofId);
            
            pn.setAreaCode(null);
        }
        finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }

    @Test
    public void testAttributeAddEventsWithRollback()
    {
        testAttributeAddEvents(true);
    }
    
    @Test
    public void testAttributeAddEventsWithCommit()
    {
        testAttributeAddEvents(false);
    }
    
    private void testAttributeAddEvents(boolean rollback)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            AttributeEvent.EVENTMASK_ATTRIBUTE | 
                InstanceEvent.EVENTMASK_INSTANCE,
            new DelegatingEventValidator(
                new CreateInstanceEventValidator(
                    EventType.PLANNED,
                    getSpecialPackage().getRow(),
                    Row.class,
                    null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "rowNumber", 1, 0),
                new AttributeAddEventValidator(
                    EventType.PLANNED, "columns", "A"),
                new AttributeAddEventValidator(
                    EventType.PLANNED, "columns", "B"),
                    
                new CreateInstanceEventValidator(
                    EventType.PLANNED,
                    getSpecialPackage().getRow(),
                    Row.class,
                    null),
                new AttributeSetEventValidator(
                    EventType.PLANNED, "rowNumber", 2, 0),
                new AttributeAddEventValidator(
                    EventType.PLANNED, "columns", "C"),
                new AttributeAddEventValidator(
                    EventType.PLANNED, "columns", "D"),

                new CreateInstanceEventValidator(
                    EventType.PLANNED,
                    getSpecialPackage().getTable(),
                    Table.class,
                    null),
                new AttributeAddEventValidator(
                    EventType.PLANNED, "rows", "rowNumber",
                    Row.class, 1),
                new AttributeAddEventValidator(
                    EventType.PLANNED, "rows", "rowNumber",
                    Row.class, 2),
                    
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2),
                new DuplicateEventValidator(postTxnEventType, 3),
                new DuplicateEventValidator(postTxnEventType, 4),
                new DuplicateEventValidator(postTxnEventType, 5),
                new DuplicateEventValidator(postTxnEventType, 6),
                new DuplicateEventValidator(postTxnEventType, 7),
                new DuplicateEventValidator(postTxnEventType, 8),
                new DuplicateEventValidator(postTxnEventType, 9),
                new DuplicateEventValidator(postTxnEventType, 10)
            ));
        
        createAttributeObjects(rollback);
        
        waitAndCheckErrors();
    }

    @Test
    public void testAttributeRemoveEventsWithRollback()
    {
        testAttributeRemoveEvents(true);
    }
    
    @Test
    public void testAttributeRemoveEventsWithCommit()
    {
        testAttributeRemoveEvents(false);
    }
    
    private void testAttributeRemoveEvents(boolean rollback)
    {
        final EventType postTxnEventType = 
            rollback ? EventType.CANCELED : EventType.CHANGED;

        configureListener(
            TransactionEvent.EVENTMASK_TRANSACTION,
            new LenientEventValidator(4));
        
        String tableMofId = createAttributeObjects(false);
        
        waitAndCheckErrors();
        destroyListener();
        
        configureListener(
            AttributeEvent.EVENTMASK_ATTRIBUTE | 
                InstanceEvent.EVENTMASK_INSTANCE,
            new DelegatingEventValidator(
                new AttributeRemoveEventValidator(
                    EventType.PLANNED, "columns", "A"),
                new AttributeRemoveEventValidator(
                    EventType.PLANNED, "columns", "B"),
                new AttributeRemoveEventValidator(
                    EventType.PLANNED, "rows", "rowNumber",
                    Row.class, 1),
                new AttributeRemoveEventValidator(
                    EventType.PLANNED, "columns", "D"),
                new AttributeRemoveEventValidator(
                    EventType.PLANNED, "columns", "C"),
                new AttributeRemoveEventValidator(
                    EventType.PLANNED, "rows", "rowNumber",
                    Row.class, 2),
                                        
                new DuplicateEventValidator(postTxnEventType, 0),
                new DuplicateEventValidator(postTxnEventType, 1),
                new DuplicateEventValidator(postTxnEventType, 2),
                new DuplicateEventValidator(postTxnEventType, 3),
                new DuplicateEventValidator(postTxnEventType, 4),
                new DuplicateEventValidator(postTxnEventType, 5)
            ));
        
        getRepository().beginTrans(true);
        try {
            Table table = (Table)getRepository().getByMofId(tableMofId);
            
            Collection<Row> rows = table.getRows();
            
            Iterator<Row> iter = rows.iterator();
            
            Row row1 = iter.next();
            Row row2;
            if (row1.getRowNumber() == 2) {
                row2 = row1;
                row1 = iter.next();
            } else {
                row2 = iter.next();
            }
            
            row1.getColumns().remove("A");
            row1.getColumns().remove("B");
            table.getRows().remove(row1);
            row2.getColumns().remove("D");
            row2.getColumns().remove("C");
            table.getRows().remove(row2);
        }
        finally {
            getRepository().endTrans(rollback);
        }
        
        waitAndCheckErrors();
    }

    private String createAttributeObjects(boolean rollback)
    {
        getRepository().beginTrans(true);
        try {
            Row r1 = 
                getSpecialPackage().getRow().createRow();
            r1.setRowNumber(1);
            r1.getColumns().addAll(
                Arrays.asList(new String[] { "A", "B" } ));                    
            
            Row r2 = 
                getSpecialPackage().getRow().createRow();
            r2.setRowNumber(2);
            r2.getColumns().addAll(
                Arrays.asList(new String[] { "C", "D" } ));                    
            
            Table t =
                getSpecialPackage().getTable().createTable();
            t.getRows().add(r1);
            t.getRows().add(r2);
            
            return t.refMofId();
        } finally {
            getRepository().endTrans(rollback);
        }
    }

    private enum CreateMethod
    {
        CLASS_PROXY,
        REFLECTIVE_API;
    }

    private enum AssocMethod
    {
        REFERENCE,
        ASSOCIATION_PROXY,
        REFLECTIVE_API;
    }

    private enum AttributeMethod
    {
        METHOD,
        REFLECTIVE_API;
    }

    private static class TxnEndEventListener implements MDRChangeListener
    {
        private volatile boolean gotEvent = false;
        
        public void change(MDRChangeEvent event)
        {
            int type = event.getType();
            if ((type & TransactionEvent.EVENT_TRANSACTION_END) != 0)
            {
                synchronized(this) {
                    gotEvent = true;
                    notifyAll();
                }
            }
        }
        
        public void waitForEvent() throws InterruptedException
        {
            synchronized(this) {
                while(!gotEvent) {
                    wait();
                }
            }
        }
        
    }
}

// End MdrEventsApiTest.java
