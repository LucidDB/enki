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
package org.eigenbase.enki.hibernate.prototype;

import java.util.*;

import org.hibernate.*;
import org.hibernate.cfg.*;

import eem.sample.*;

/**
 * @author Stephan Zuercher
 */
public class Main
{
    private static final String MODIFY_DRIVERS = "drivers";
    private static final String MODIFY_REGISTRATIONS = "registrations";
    private static final String MODIFY_DEREGISTER = "deregister";
    private static final String MODIFY_CREATE_PASSENGER = "create-passenger";
    private static final String MODIFY_ADD_PASSENGER = "add-passenger";
    private static final String MODIFY_SET_DRIVEN = "set-driven";
    private static final String MODIFY_SET_RIDDEN = "set-ridden";
    
    private static final String PASSENGER_JOHN_MADDEN = "John Madden";
    private static final String PASSENGER_HUNTER_S_THOMPSON = "Hunter S. Thompson";
    private static final String PERSON_BOB_NEWHART = "Bob Newhart";

    private static final String DRIVER_JOE_MITCHELL = "Joe Mitchell";
    private static final String DRIVER_THE_STIG = "The Stig";
    private static final String DRIVER_WILLIE_YARBROUGH = "Willie Yarbrough";

    private static final String MAKE_PORSCHE = "Porsche";
    private static final String MODEL_911_TURBO_GT3 = "911 Turbo GT3";

    private static final String MAKE_MCI = "MCI";
    private static final String MODEL_E4500 = "E4500";
    
    private static final String STATE_ARIZONA = "Arizona";
    private static final String STATE_CALIFORNIA = "California";
    private static final String STATE_NEVADA = "Nevada";

    private static SessionFactory sessionFactory;

    public static void main(String[] args) throws Exception
    {        
        Command cmd;
        String[] params;
        boolean showSqlMode = false;
        
        if (args.length < 1) {
            cmd = Command.CREATE;
            showSqlMode = false;
            params = new String[0];
        } else {
            int iArg = 0;
            if (args[0].startsWith("-s")) {
                iArg++;
                showSqlMode = true;
            }
            
            cmd = Command.forName(args[iArg]);
            if (cmd == null) {
                throw new IllegalArgumentException(args[iArg]);
            }
            iArg++;
            
            params = new String[args.length - iArg];
            for(int i = iArg; i < args.length; i++) {
                params[i - iArg] = args[i];
            }
        }
        
        boolean createSchemaMode = cmd.requiresCreateSchema();
        
        createSessionFactory(createSchemaMode, showSqlMode);

        try {
            switch(cmd) {
            case SCRIPT:
                executeScript();
                break;
                
            default:
                executeBasicCommand(cmd, params);
                break;
            }
        }
        catch(Throwable t) {
            t.printStackTrace();
        }

        getSessionFactory().close();
    }

    private static void executeBasicCommand(Command cmd, String... params)
    {
        Session session = getSessionFactory().getCurrentSession();
        session.beginTransaction();
        
        boolean rollback = true;
        try {
            switch(cmd) {
            case CREATE:
            default:
                write(session);
                break;
                
            case READ:
                read(session);
                break;
                
            case MODIFY:
                modify(session, params);
                break;
            }
            
            rollback = false;
        }
        finally {
            if (rollback) {
                session.getTransaction().rollback();
            }
        }
    }
    
    private static void executeScript()
    {
        executeBasicCommand(Command.CREATE);
        executeBasicCommand(Command.MODIFY, MODIFY_DRIVERS);
        executeBasicCommand(Command.MODIFY, MODIFY_REGISTRATIONS);
        executeBasicCommand(Command.MODIFY, MODIFY_DEREGISTER);
        executeBasicCommand(
            Command.MODIFY,
            MODIFY_CREATE_PASSENGER, PASSENGER_HUNTER_S_THOMPSON);
        executeBasicCommand(
            Command.MODIFY, 
            MODIFY_ADD_PASSENGER, MAKE_MCI, MODEL_E4500, PASSENGER_HUNTER_S_THOMPSON);
        executeBasicCommand(
            Command.MODIFY,
            MODIFY_SET_DRIVEN, MAKE_MCI, MODEL_E4500, DRIVER_WILLIE_YARBROUGH);
        executeBasicCommand(Command.READ);
        
        executeBasicCommand(
            Command.MODIFY,
            MODIFY_SET_RIDDEN,
            MAKE_PORSCHE, MODEL_911_TURBO_GT3, PASSENGER_HUNTER_S_THOMPSON);
        executeBasicCommand(Command.READ);
    }

    private static void write(Session session)
    {
        SamplePackage samplePkg = new SamplePackage_Impl();
        Person newhart = samplePkg.getPerson().createPerson();
        
        newhart.setName(PERSON_BOB_NEWHART);
        session.save(newhart);
        
        State calif = samplePkg.getState().createState(STATE_CALIFORNIA);
        State nevada = samplePkg.getState().createState(STATE_NEVADA);
        State arizona = samplePkg.getState().createState(STATE_ARIZONA);
        
        Passenger madden = 
            samplePkg.getPassenger().createPassenger(PASSENGER_JOHN_MADDEN);
        
        Bus maddenCruiser = samplePkg.getBus().createBus();
        maddenCruiser.setAxles(3);
        maddenCruiser.setMake(MAKE_MCI);
        maddenCruiser.setModel(MODEL_E4500);
        
        madden.getOwned().add(maddenCruiser);
        
        Driver yarbrough = 
            samplePkg.getDriver().createDriver(
                DRIVER_WILLIE_YARBROUGH, "XX0001234");
        
        maddenCruiser.setDriver(yarbrough);
        maddenCruiser.getRegistrar().add(calif);
        maddenCruiser.getRider().add(madden);
        
        Driver theStig = samplePkg.getDriver().createDriver();
        theStig.setName(DRIVER_THE_STIG);
        theStig.setLicense("1");
        
        Car porsche = samplePkg.getCar().createCar();
        porsche.setMake(MAKE_PORSCHE);
        porsche.setModel(MODEL_911_TURBO_GT3);
        porsche.setDoors(2);
        porsche.setDriver(theStig);
        newhart.getOwned().add(porsche);
        porsche.getRegistrar().add(nevada);
        
        session.save(calif);
        session.save(nevada);
        session.save(arizona);
        session.save(madden);
        session.save(maddenCruiser);
        session.save(yarbrough);
        
        session.save(porsche);
        session.save(theStig);
        
        session.getTransaction().commit();
    }
    
    private static void read(Session session)
    {
        Query personQuery = 
            session.createQuery("from " + Person.class.getName());
        List<?> people = personQuery.list();
     
        System.out.println("People:");
        for(Object o: people) {
            Person person = (Person)o;
            
            System.out.println("\t" + person.getName());
            Collection<Vehicle> owned = person.getOwned();
            if (!owned.isEmpty()) {
                System.out.print("\t\tOwns: ");
                boolean multi = (owned.size() > 1); 
                if (multi) {
                    System.out.println();
                }
                for(Vehicle vehicle: owned) {
                    if (multi) {
                        System.out.print("\t\t\t");
                    }
                    System.out.println(
                        vehicle.getMake() + " " + vehicle.getModel());
                }
            }
            
            if (person instanceof Driver) {
                Vehicle vehicle = ((Driver)person).getDriven();
                if (vehicle != null) {
                    System.out.print("\t\tDrives: ");
                    System.out.println(
                        vehicle.getMake() + " " + vehicle.getModel());
                }
            }
            
            if (person instanceof Passenger) {
                Vehicle vehicle = ((Passenger)person).getRidden();
                if (vehicle != null) {
                    System.out.print("\t\tRides: ");
                    System.out.println(
                        vehicle.getMake() + " " + vehicle.getModel());
                }
            }
        }
        System.out.println();
        
        Query vehicleQuery = 
            session.createQuery("from " + Vehicle.class.getName());
        List<?> vehicles = vehicleQuery.list();
     
        System.out.println("Vehicles:");
        for(Object o: vehicles) {
            Vehicle vehicle = (Vehicle)o;
            
            System.out.println(
                "\t" + vehicle.getMake() + " " + vehicle.getModel());

            Driver driver = vehicle.getDriver();
            if (driver != null) {
                System.out.println("\t\tDriven By: " + driver.getName());
            }
            
            Person owner = vehicle.getOwner();
            if (owner != null) {
                System.out.println("\t\tOwned By: " + owner.getName());
            }

            System.out.println("\t\tRegistered In:");
            for(State state: vehicle.getRegistrar()) {
                System.out.println("\t\t\t" + state.getName());
            }
            
            if (vehicle instanceof Car) {
                System.out.println(
                    "\t\t" + ((Car)vehicle).getDoors() + " door");
            }
            
            if (vehicle instanceof Bus) {
                System.out.println(
                    "\t\t" + ((Bus)vehicle).getAxles() + " axles");
            }
            
            Collection<Passenger> riders = vehicle.getRider();
            if (!riders.isEmpty()) {
                System.out.println("\t\tRidden By:");
                for(Passenger passenger: riders) {
                    System.out.println("\t\t\t" + passenger.getName());
                }
            }
        }
        System.out.println();
        
        Query stateQuery = 
            session.createQuery("from " + State.class.getName());
        List<?> states = stateQuery.list();
     
        System.out.println("States:");
        for(Object o: states) {
            State state = (State)o;
            
            System.out.println("\t" + state.getName());
            System.out.println("\t\tRegisters:");
            for(Vehicle vehicle: state.getRegistered()) {
                System.out.println(
                    "\t\t\t" + vehicle.getMake() + " " + vehicle.getModel());
            }
        }
    }

    private static void modify(Session session, String[] params)
    {
        SamplePackage samplePkg = new SamplePackage_Impl();

        if (params[0].equals(MODIFY_DRIVERS)) {
            // Switch drivers.        
            Driver mitchell = samplePkg.getDriver().createDriver();
            mitchell.setName(DRIVER_JOE_MITCHELL);
            mitchell.setLicense("XX1234000");
    
            Query query = 
                session.createQuery(
                    "from " + Vehicle.class.getName() + 
                    " where make = ? and model = ?");
            query.setString(0, MAKE_MCI);
            query.setString(1, MODEL_E4500);
            Bus maddenCruiser = (Bus)query.uniqueResult();
            
            maddenCruiser.setDriver(mitchell);
            
            session.save(maddenCruiser);
            session.save(mitchell);
        } else if (params[0].equals(MODIFY_REGISTRATIONS)) {
            Query query = 
                session.createQuery(
                    "from " + Vehicle.class.getName() + 
                    " where make = ? and model = ?");
            query.setString(0, MAKE_MCI);
            query.setString(1, MODEL_E4500);
            Bus maddenCruiser = (Bus)query.uniqueResult();

            query = 
                session.createQuery(
                    "from " + State.class.getName() + " where name = ?");
            query.setString(0, STATE_ARIZONA);
            State arizona = (State)query.uniqueResult();
            
            maddenCruiser.getRegistrar().add(arizona);
        } else if (params[0].equals(MODIFY_DEREGISTER)) {
            Query query = 
                session.createQuery("from " + Vehicle.class.getName()); 
            List<?> vehicles = query.list();
            
            for(Object v: vehicles) {
                Vehicle vehicle = (Vehicle)v;
                
                if (vehicle.getRegistrar().size() > 1) {
                    Iterator<State> iter = vehicle.getRegistrar().iterator();
                    boolean first = true;
                    while(iter.hasNext()) {
                        iter.next();
                        if (!first) {
                            iter.remove();
                        }
                        first = false;
                    }
                }
            }
        } else if (params[0].equals(MODIFY_CREATE_PASSENGER)) {
            String name = params[1];
            
            Passenger passenger = samplePkg.getPassenger().createPassenger();
            passenger.setName(name);
            session.save(passenger);
        } else if (params[0].equals(MODIFY_ADD_PASSENGER)) {
            String make = params[1];
            String model = params[2];
            String name = params[3];
            
            Query query = 
                session.createQuery(
                    "from " + Passenger.class.getName() + " where name = ?");
            query.setString(0, name);
            Passenger passenger = (Passenger)query.uniqueResult();
            
            query =
                session.createQuery(
                    "from " + Vehicle.class.getName() +
                    " where make = ? and model = ?");
            query.setString(0, make);
            query.setString(1, model);
            List<?> vehicles = query.list();
            for(Object v: vehicles) {
                Vehicle vehicle = (Vehicle)v;
                
                vehicle.getRider().add(passenger);
            }
        } else if (params[0].equals(MODIFY_SET_DRIVEN)) {
            String make = params[1];
            String model = params[2];
            String name = params[3];
            
            Query query = 
                session.createQuery(
                    "from " + Driver.class.getName() + " where name = ?");
            query.setString(0, name);
            Driver driver = (Driver)query.uniqueResult();
            
            query =
                session.createQuery(
                    "from " + Vehicle.class.getName() +
                    " where make = ? and model = ?");
            query.setString(0, make);
            query.setString(1, model);
            Vehicle vehicle = (Vehicle)query.uniqueResult();
            
            driver.setDriven(vehicle);
        } else if (params[0].equals(MODIFY_SET_RIDDEN)) {
            String make = params[1];
            String model = params[2];
            String name = params[3];
            
            Query query = 
                session.createQuery(
                    "from " + Passenger.class.getName() + " where name = ?");
            query.setString(0, name);
            Passenger passenger = (Passenger)query.uniqueResult();
            
            query =
                session.createQuery(
                    "from " + Vehicle.class.getName() +
                    " where make = ? and model = ?");
            query.setString(0, make);
            query.setString(1, model);
            Vehicle vehicle = (Vehicle)query.uniqueResult();
            
            passenger.setRidden(vehicle);
        } else {
            throw new RuntimeException("unknown modify param: " + params[0]);
        }
        
        session.getTransaction().commit();
    }

    public synchronized static SessionFactory createSessionFactory(
        boolean createSchemaMode, boolean showSqlMode)
    {
        if (sessionFactory == null) {
            Configuration config = new Configuration();

            if (createSchemaMode) {
                config.setProperty("hibernate.hbm2ddl.auto", "create");
            }
            
            if (showSqlMode) {
                config.setProperty("hibernate.show_sql", "true");
            }
            
            config.configure("org/eigenbase/enki/hibernate/prototype/hibernate-config.xml");
            config.addResource("EnkiSampleMetamodel-mapping.xml");

            sessionFactory = config.buildSessionFactory();
        }
        
        return sessionFactory;
    }
    
    public synchronized static SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }
    
    private static enum Command
    {
        CREATE(true, "create"),
        READ("read", "load"),
        MODIFY("modify"),
        SCRIPT(true, "script");
        
        private final String[] names;
        private final boolean createSchema;
        
        private Command(String... names)
        {
            this.names = names;
            this.createSchema = false;
        }
        
        private Command(boolean createSchema, String... names)
        {
            this.names = names;
            this.createSchema = createSchema;
        }
        
        public boolean requiresCreateSchema()
        {
            return createSchema;
        }
        
        public static Command forName(String name)
        {
            for(Command cmd: values()) {
                for(String cmdName: cmd.names) {
                    if (cmdName.equals(name)) {
                        return cmd;
                    }
                }
            }
            
            return null;
        }
    }
}

// End Main.java
