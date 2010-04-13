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
package org.eigenbase.enki.test.hibernate;

import java.sql.*;
import java.util.*;
import java.util.regex.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.test.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;
import org.hibernate.dialect.*;
import org.junit.*;
import org.junit.runner.*;

import eem.sample.simple.*;

/**
 * HibernateConstraintsTest tests Enki/Hibernate's constraint testing for
 * database references that cannot be modeled as foreign keys.
 * 
 * @author Stephan Zuercher
 */
@RunWith(HibernateOnlyTestRunner.class)
public class HibernateConstraintsTest extends SampleModelTestBase
{
    private static final String[][] assocDelDatas = new String[][] {
        { 
            HibernateMappingHandler.ASSOC_ONE_TO_ONE_LAZY_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_ONE_PARENT_ID_COLUMN
        },
        { 
            HibernateMappingHandler.ASSOC_ONE_TO_ONE_LAZY_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_ONE_CHILD_ID_COLUMN
        },
        { 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_CHILDREN_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN
        },                    
        { 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN
        },
        {
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_ORDERED_CHILDREN_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN
        },                    
        {
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_ORDERED_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN
        },                    
        {
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_HC_CHILDREN_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN
        },                    
        {
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_HC_TABLE, 
            HibernateMappingHandler.ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN
        },
        {
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_LAZY_TARGET_TABLE, 
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN
        },                    
        {
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_LAZY_TABLE, 
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN
        },
        {
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_LAZY_ORDERED_TARGET_TABLE, 
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN
        },                    
        {
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_LAZY_ORDERED_TABLE, 
            HibernateMappingHandler.ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN
        },
    };


    @Test
    public void testReferenceConstraintsMissingEnd1() throws Exception
    {
        testReferenceDataConstraint(false);
    }
    
    @Test
    public void testReferenceConstraintMissingEnd2() throws Exception
    {
        testReferenceDataConstraint(true);
    }
    
    private void testReferenceDataConstraint(boolean delEnd2) throws Exception
    {
        List<String> end1TableList = new ArrayList<String>();
        List<String> end2TableList = new ArrayList<String>();
        List<String> end1List = new ArrayList<String>();
        List<String> end2List = new ArrayList<String>();
        
        getRepository().beginTrans(true);
        try {
            SimplePackage simplePkg = getSimplePackage();
            
            // 1-1
            Entity1 e1 = simplePkg.getEntity1().createEntity1();
            Entity2 e2 = simplePkg.getEntity2().createEntity2();
            
            e1.setEntity2(e2);

            end1TableList.add(table(simplePkg.getEntity1()));
            end2TableList.add(table(simplePkg.getEntity2()));
            end1List.add(e1.refMofId());
            end2List.add(e2.refMofId());
            
            // 1-*
            Entity10 e10 = simplePkg.getEntity10().createEntity10();
            Entity11 e11 = simplePkg.getEntity11().createEntity11();
            
            e10.getEntity11().add(e11);
            
            end1TableList.add(table(simplePkg.getEntity10()));
            end2TableList.add(table(simplePkg.getEntity11()));
            end1List.add(e10.refMofId());
            end2List.add(e11.refMofId());

            // 1-* high cardinality
            Entity12 e12 = simplePkg.getEntity12().createEntity12();
            Entity13 e13 = simplePkg.getEntity13().createEntity13();
            
            e12.getEntity13().add(e13);
            
            end1TableList.add(table(simplePkg.getEntity12()));
            end2TableList.add(table(simplePkg.getEntity13()));
            end1List.add(e12.refMofId());
            end2List.add(e13.refMofId());

            // 1-* ordered
            Entity16 e16 = simplePkg.getEntity16().createEntity16();
            Entity17 e17 = simplePkg.getEntity17().createEntity17();
            
            e16.getEntity17().add(e17);
            
            end1TableList.add(table(simplePkg.getEntity16()));
            end2TableList.add(table(simplePkg.getEntity17()));
            end1List.add(e16.refMofId());
            end2List.add(e17.refMofId());

            // *-*
            Entity20 e20 = simplePkg.getEntity20().createEntity20();
            Entity21 e21 = simplePkg.getEntity21().createEntity21();
            
            e20.getEntity21().add(e21);
            
            end1TableList.add(table(simplePkg.getEntity20()));
            end2TableList.add(table(simplePkg.getEntity21()));
            end1List.add(e20.refMofId());
            end2List.add(e21.refMofId());

            // *-* ordered
            Entity22 e22 = simplePkg.getEntity22().createEntity22();
            Entity23 e23 = simplePkg.getEntity23().createEntity23();
            
            e22.getEntity23().add(e23);
            
            end1TableList.add(table(simplePkg.getEntity22()));
            end2TableList.add(table(simplePkg.getEntity23()));
            end1List.add(e22.refMofId());
            end2List.add(e23.refMofId());
        } finally {
            getRepository().endTrans(false);
        }
        
        Set<Long> deletedMofIds = new HashSet<Long>();
        
        getRepository().beginTrans(true);
        try {
            // convince Enki we did something
            Entity1 e1 = getSimplePackage().getEntity1().createEntity1();
            Assert.assertNotNull(e1);
            
            HibernateMDRepository hibernateRepos = 
                (HibernateMDRepository)getRepository();
            
            Session session = hibernateRepos.getCurrentSession();
            Dialect sqlDialect = hibernateRepos.getSqlDialect();
            
            List<String> delList = delEnd2 ? end2List : end1List;
            List<String> delTableList = delEnd2 ? end2TableList : end1TableList;
    
            Assert.assertEquals(delList.size(), delTableList.size());
            
            Connection conn = session.connection();
            Statement stmt = conn.createStatement();
    
            try {
                for(int i = 0; i < delList.size(); i++) {
                    long mofId = MofIdUtil.parseMofIdStr(delList.get(i));
                    
                    deletedMofIds.add(mofId);
                    
                    stmt.executeUpdate(
                        "delete from " + 
                        HibernateDialectUtil.quote(
                            sqlDialect, delTableList.get(i)) +
                        " where " +
                        HibernateDialectUtil.quote(sqlDialect, "mofId") +
                        " = " +
                        mofId);
                }
            } finally {
                stmt.close();
            }
        } finally {
            getRepository().endTrans(false);
        }
        
        getRepository().endSession();
        getRepository().beginSession();

        try {
            // Now verify that we get errors
            getRepository().beginTrans(false);
            try {
                Collection<?> errors = 
                    getSimplePackage().refVerifyConstraints(true);
                
                Pattern RE = 
                    Pattern.compile("Missing RefObject: MOF ID: ([0-9]+),.*");
                
                Set<Long> errorMofIds = new HashSet<Long>();
                for(Object e: errors) {
                    Throwable t = (Throwable)e;

                    String msg = t.getMessage();
                    
                    Matcher m = RE.matcher(msg);
                    
                    if (!m.matches()) {
                        Assert.fail(
                            "Constraint error message mismatch: " + msg);
                    }
                    
                    String mofId = m.group(1);
                    errorMofIds.add(Long.parseLong(mofId));
                }
                
                Assert.assertTrue(
                    "Mismatch in expected errors",
                    errorMofIds.equals(deletedMofIds));
            } finally {
                getRepository().endTrans();
            }
        } finally {
            cleanup(
                union(end1TableList, end2TableList),
                union(end1List, end2List));
        }
    }

    private String table(RefClass refCls)
    {
        return "SMPL_" + ((HibernateRefClass)refCls).getTable();
    }
    
    private <T> List<T> union(List<T> a, List<T> b)
    {
        ArrayList<T> union = new ArrayList<T>();
        union.addAll(a);
        union.addAll(b);
        return union;
    }
    
    private void cleanup(List<String> tables, List<String> mofIds)
        throws Exception
    {
        getRepository().beginTrans(true);

        try {
            HibernateMDRepository hibernateRepos = 
                (HibernateMDRepository)getRepository();
        
            String tablePrefix = hibernateRepos.getTablePrefix();
            
            Session session = hibernateRepos.getCurrentSession();
            Dialect sqlDialect = hibernateRepos.getSqlDialect();
        
            Connection conn = session.connection();
            Statement stmt = conn.createStatement();
    
            try {
                StringBuilder inList = new StringBuilder();
                
                // Delete all the objects
                for(int i = 0; i < tables.size(); i++) {
                    long mofId = MofIdUtil.parseMofIdStr(mofIds.get(i));
                    
                    stmt.executeUpdate(
                        "delete from " + 
                        HibernateDialectUtil.quote(sqlDialect, tables.get(i)) +
                        " where " +
                        HibernateDialectUtil.quote(sqlDialect, "mofId") +
                        " = " +
                        mofId);
                    
                    if (inList.length() > 0) {
                        inList.append(",");
                    }
                    inList.append(mofId);
                }
                
                stmt.executeUpdate(
                    "delete from " + 
                    HibernateDialectUtil.quote(
                        sqlDialect, "ENKI_TYPE_LOOKUP") +
                    " where " + 
                    HibernateDialectUtil.quote(sqlDialect, "mofId") +
                    " in (" + inList + ")");
                
                DatabaseMetaData metaData = conn.getMetaData();
                for(String[] data: assocDelDatas) {
                    String tableName = 
                        getTableName(metaData, tablePrefix, data[0]);

                    stmt.executeUpdate(
                        "delete from " +
                        HibernateDialectUtil.quote(sqlDialect, tableName) +
                        " where " +
                        HibernateDialectUtil.quote(sqlDialect, data[1]) +
                        " in (" + inList + ")");
                }
            } finally {
                stmt.close();
            }
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    private String getTableName(
        DatabaseMetaData metaData,
        String tablePrefix,
        String tablePattern)
    throws SQLException
    {
        String tableName = tablePrefix + tablePattern;
        ResultSet rs = metaData.getTables(null, null, tableName, null);
        try {
            Assert.assertTrue("Missing table: " + tableName, rs.next());
            return rs.getString(3);
        } finally {
            Assert.assertFalse(
                "Found multiple instances of table: " + tableName,
                rs.next());
            rs.close();
        }
    }
}

// End HibernateConstraintsTest.java
