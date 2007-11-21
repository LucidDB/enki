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
package org.eigenbase.enki.prototype;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

class Main
{
    private static SessionFactory sessionFactory;
    
    public static void main(String[] args) throws Exception
    {
        Session session = getSessionFactory(true).getCurrentSession();
        
        session.beginTransaction();
        boolean rollback = true;
        try {
            if (args.length == 0 || args[0].equals("store")) {
                int i = 1;
                boolean doBarCmd = false;
                if (args.length >= 3) {
                    if (args[1].startsWith("BarCmd")) {
                        doBarCmd = true;
                    }
                    i = Integer.parseInt(args[2]);
                }
                
                BarCmd bc = doBarCmd ? new BarCmdImpl() : new FooBarCmdImpl();
                
                bc.setName("number " + i);
                bc.setBar((i % 2 == 0) ? null : (long)i * 100L);
                if (!doBarCmd) {
                    FooBarCmdImpl fbc = (FooBarCmdImpl)bc;
                    fbc.setFoo(i);
                    fbc.setFoobar(i % 2 != 0);
                }
                
                List<Thingy> thingies = new ArrayList<Thingy>();
                for(int j = 0; j < i; j++) {
                    Thingy thingy;
                    if (j % 2 == 0) {
                        thingy = new ThingyImpl();
                        thingy.setName("Thingy " + (j + 1) + " of " + i);
                    } else {
                        Doodad doodad = new DoodadImpl();
                        doodad.setName("Doodad " + (j + 1) + " of " + i);
                        doodad.setDoodadMode(j % 4);
                        thingy = doodad;
                    }
                    
                    thingies.add(thingy);
                }
                if (!thingies.isEmpty()) {
                    bc.setThingies(thingies);
                }
                
                session.save(bc);
            } else if (args[0].equals("load")) { 
                String type = "FooBarCmd";
                if (args.length >= 2) {
                    type = args[1];
                }
                List<?> result = session.createQuery("from " + type).list();
                for(int i = 0; i < result.size(); i++) {
                    Object obj = result.get(i);
                    
                    System.out.println(obj.toString());
                }
            } else if (args[0].equals("remove")) {
                String fromType = args[1];
                long id = Long.parseLong(args[2]);
                int index = Integer.parseInt(args[3]);
                
                List<?> result = 
                    session.createQuery(
                        "from " + fromType + " where id = " + id)
                    .list();
                BarCmd barCmd = (BarCmd)result.get(0);                
                barCmd.getThingies().remove(index);
            } else if (args[0].equals("assoc-setparent")) {
                long assocId = Long.parseLong(args[1]);
                String toType = args[2];
                long toId = Long.parseLong(args[3]);

                OneToManyAssociation assoc =
                    (OneToManyAssociation)session.createQuery(
                        "from OneToManyAssociation where id = " + assocId)
                        .uniqueResult();
                
                BarCmd to = 
                    (BarCmd)session.createQuery(
                        "from " + toType + " where id = " + toId)
                        .uniqueResult();

                assoc.setParent(to);
            } else {
                throw new IllegalArgumentException();
            }
    
            session.getTransaction().commit();
            rollback = false;
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
        finally {
            if (rollback) {
                session.getTransaction().rollback();
            }
        }

        getSessionFactory().close();
    }

    public synchronized static SessionFactory getSessionFactory(boolean create)
    {
        if (sessionFactory == null && create) {
            sessionFactory =
                new Configuration()
                .configure("org/eigenbase/enki/prototype/hibernate-config.xml")
                .buildSessionFactory();
        }
        
        return sessionFactory;
    }
    
    public synchronized static SessionFactory getSessionFactory()
    {
        return getSessionFactory(false);
    }
}

// End Main.java
