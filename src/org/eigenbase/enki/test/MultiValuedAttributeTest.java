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

import org.junit.*;
import org.junit.runner.*;

import eem.sample.special.*;

/**
 * MultiValuedAttributeTest tests multi-valued primitive and component 
 * attributes.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class MultiValuedAttributeTest extends SampleModelTestBase
{
    @Test
    public void testPrimitiveMultiValuedAttribute()
    {
        String row1MofId, row2MofId;
        
        // Create two Row instances with two "columns" each.
        getRepository().beginTrans(true);
        try {
            Row row1 = getSpecialPackage().getRow().createRow();
            row1.setRowNumber(1);
            row1.getColumns().add("Column 1");
            row1.getColumns().add("Column 2");
            row1MofId = row1.refMofId();
            
            Row row2 =
                getSpecialPackage().getRow().createRow(
                    2, Arrays.asList(new String[] { "Column A", "Column B" }));
            row2MofId = row2.refMofId();
        }
        finally {
            getRepository().endTrans(false);
        }
        
        // Read them back.
        getRepository().beginTrans(false);
        try {
            Row row1 = (Row)getRepository().getByMofId(row1MofId);
            
            Assert.assertEquals(1, row1.getRowNumber());
            Collection<String> row1Cols = row1.getColumns();
            Assert.assertEquals(2, row1Cols.size());
            Assert.assertTrue(row1Cols.contains("Column 1"));
            Assert.assertTrue(row1Cols.contains("Column 2"));
            
            Row row2 = (Row)getRepository().getByMofId(row2MofId);
            Collection<String> row2Cols = row2.getColumns();
            Assert.assertEquals(2, row2Cols.size());
            Assert.assertTrue(row2Cols.contains("Column A"));
            Assert.assertTrue(row2Cols.contains("Column B"));
        }
        finally {
            getRepository().endTrans();
        }

        // Add a new column to each row.
        getRepository().beginTrans(true);
        try {
            Row row1 = (Row)getRepository().getByMofId(row1MofId);
            row1.getColumns().add("Column 3");
            
            Row row2 = (Row)getRepository().getByMofId(row2MofId);
            row2.getColumns().add("Column C");
        }
        finally {
            getRepository().endTrans(false);
        }

        // Read them back.
        getRepository().beginTrans(false);
        try {
            Row row1 = (Row)getRepository().getByMofId(row1MofId);
            
            Assert.assertEquals(1, row1.getRowNumber());
            Collection<String> row1Cols = row1.getColumns();
            Assert.assertEquals(3, row1Cols.size());
            Assert.assertTrue(row1Cols.contains("Column 1"));
            Assert.assertTrue(row1Cols.contains("Column 2"));
            Assert.assertTrue(row1Cols.contains("Column 3"));
            
            Row row2 = (Row)getRepository().getByMofId(row2MofId);
            Collection<String> row2Cols = row2.getColumns();
            Assert.assertEquals(3, row2Cols.size());
            Assert.assertTrue(row2Cols.contains("Column A"));
            Assert.assertTrue(row2Cols.contains("Column B"));
            Assert.assertTrue(row2Cols.contains("Column C"));
        }
        finally {
            getRepository().endTrans();
        }
        
        // Delete them.
        getRepository().beginTrans(true);
        try {
            Row row1 = (Row)getRepository().getByMofId(row1MofId);
            row1.refDelete();
            
            Row row2 = (Row)getRepository().getByMofId(row2MofId);
            row2.refDelete();
        }
        finally {
            getRepository().endTrans(false);
        }
    }
    
    @Test
    public void testComponentMultiValuedAttribute()
    {
        String t1MofId, t2MofId;
        String[][] rowMofIds;
        
        // create two Table instances
        getRepository().beginTrans(true);
        try {
            Row[][] rows = new Row[][] {
                {
                    getSpecialPackage().getRow().createRow(
                        1, Arrays.asList(new String[] { "T1R1C1", "T1R1C2" })),
                    getSpecialPackage().getRow().createRow(
                        2, Arrays.asList(new String[] { "T1R2C1", "T1R2C2" })),
                },
                {
                    getSpecialPackage().getRow().createRow(
                        1, Arrays.asList(new String[] { "T2R1C1", "T2R1C2" })),
                    getSpecialPackage().getRow().createRow(
                        2, Arrays.asList(new String[] { "T2R2C1", "T2R2C2" })),
                }
            };
            
            rowMofIds = new String[rows.length][];
            for(int i = 0; i < rows.length; i++) {
                Row[] rowset = rows[i];
                rowMofIds[i] = new String[rowset.length + 1];
                for(int j = 0; j < rowset.length; j++) {
                    rowMofIds[i][j] = rows[i][j].refMofId();
                }
            }
            
            Table t1 = getSpecialPackage().getTable().createTable();
            t1.getRows().add(rows[0][0]);
            t1.getRows().add(rows[0][1]);
            t1MofId = t1.refMofId();
            
            Table t2 = 
                getSpecialPackage().getTable().createTable(
                    Arrays.asList(rows[1]));
            t2MofId = t2.refMofId();
        }
        finally {
            getRepository().endTrans(false);
        }
        
        // Read them back
        getRepository().beginTrans(false);
        try {
            Table[] tables = {
                (Table)getRepository().getByMofId(t1MofId),
                (Table)getRepository().getByMofId(t2MofId),
            };

            int t = 1;
            for(Table tab: tables) {                
                Collection<Row> rows = tab.getRows();
                Assert.assertEquals(2, rows.size());
                
                HashSet<Integer> seen = new HashSet<Integer>();
                for(Row row: rows) {
                    int r = row.getRowNumber();
                    Assert.assertTrue(r <= rows.size());
                    Assert.assertTrue(r > 0);
                    Assert.assertFalse(seen.contains(r));
                    seen.add(r);
                    
                    ArrayList<String> cellNames = 
                        new ArrayList<String>(row.getColumns());
                    Collections.sort(cellNames);
                    
                    int c = 1;
                    for(String cellName: cellNames) {
                        Assert.assertEquals(
                            "T" + t + "R" + r + "C" + c++, cellName);
                    }
                }
                t++;
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        // Add a row to each table
        getRepository().beginTrans(true);
        try {
            Table[] tables = {
                (Table)getRepository().getByMofId(t1MofId),
                (Table)getRepository().getByMofId(t2MofId),
            };

            int t = 1;
            for(Table tab: tables) {
                int r = tab.getRows().size() + 1;
                String c1 = "T" + t + "R" + r + "C1";
                String c2 = "T" + t + "R" + r + "C2";
                
                Row row;
                if ((t & 1) == 1) {
                    row = getSpecialPackage().getRow().createRow();
                    row.setRowNumber(r);
                    row.getColumns().add(c1);
                    row.getColumns().add(c2);
                } else {
                    row = 
                        getSpecialPackage().getRow().createRow(
                            r, Arrays.asList(new String[] { c1, c2 }));
                }
                
                rowMofIds[t - 1][r - 1] = row.refMofId();
                
                tab.getRows().add(row);
                t++;
            }
        }
        finally {
            getRepository().endTrans(false);
        }
        
        // Read them back
        getRepository().beginTrans(false);
        try {
            Table[] tables = {
                (Table)getRepository().getByMofId(t1MofId),
                (Table)getRepository().getByMofId(t2MofId),
            };

            int t = 1;
            for(Table tab: tables) {                
                Collection<Row> rows = tab.getRows();
                Assert.assertEquals(3, rows.size());
                
                HashSet<Integer> seen = new HashSet<Integer>();
                for(Row row: rows) {
                    int r = row.getRowNumber();
                    Assert.assertTrue(r <= rows.size());
                    Assert.assertTrue(r > 0);
                    Assert.assertFalse(seen.contains(r));
                    seen.add(r);
                    
                    ArrayList<String> cellNames = 
                        new ArrayList<String>(row.getColumns());
                    Collections.sort(cellNames);
                    
                    int c = 1;
                    for(String cellName: cellNames) {
                        Assert.assertEquals(
                            "T" + t + "R" + r + "C" + c++, cellName);
                    }
                }
                t++;
            }
        }
        finally {
            getRepository().endTrans();
        }
        
        // Delete them.
        getRepository().beginTrans(true);
        try {
            Table[] tables = {
                (Table)getRepository().getByMofId(t1MofId),
                (Table)getRepository().getByMofId(t2MofId),
            };

            tables[0].refDelete();
            tables[1].refDelete();
        }
        finally {
            getRepository().endTrans(false);
        }
        
        // Verify the delete cascaded to the rows.
        getRepository().beginTrans(false);
        try {
            for(int i = 0; i < rowMofIds.length; i++) {
                String[] mofIds = rowMofIds[i];
                for(int j = 0; j < mofIds.length; j++) {
                    String mofId = mofIds[j];
                    
                    Assert.assertNull(getRepository().getByMofId(mofId));
                }
            }
        }
        finally {
            getRepository().endTrans();
        }
    }
}

// End MultiValuedAttributeTest.java
