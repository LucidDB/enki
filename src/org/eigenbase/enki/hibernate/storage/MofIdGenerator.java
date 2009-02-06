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
package org.eigenbase.enki.hibernate.storage;

import java.sql.*;
import java.util.*;

import org.hibernate.*;
import org.hibernate.cfg.*;
import org.hibernate.dialect.*;
import org.hibernate.id.*;

/**
 * MofIdGenerator extends Hibernate's {@link TableGenerator} to provide a 
 * source of unique MOF ID values. 
 * 
 * @author Stephan Zuercher
 */
public class MofIdGenerator
{
    private static final String PROPERTY_TABLE_NAME = "enki.mofid.table";
    private static final String PROPERTY_BLOCK_SIZE = "enki.mofid.blocksize";
    
    private static final String DEFAULT_TABLE_NAME = "ENKI_MOF_ID_SEQUENCE";
    
    private static final String COLUMN_NAME = "NEXT_MOF_ID_BLOCK";

    private static final int DEFAULT_BLOCK_SIZE = 1000;
    
    private final SessionFactory sessionFactory;
    
    private final String tableName;
    private final String querySql;
    private final String updateSql;
    private final String createDdl;
    private final String initSql;
    private final String dropDdl;
    
    private final int blockSize;

    private long nextMofId;
    private long lastMofId;
    
    public MofIdGenerator(
        SessionFactory sessionFactory, Configuration config, Properties enkiProps)
    {
        this.sessionFactory = sessionFactory;

        String blockSizeStr = 
            enkiProps.getProperty(
                PROPERTY_BLOCK_SIZE, String.valueOf(DEFAULT_BLOCK_SIZE));
        int blockSizeInit;
        try {
            blockSizeInit = Integer.parseInt(blockSizeStr);
        }
        catch(NumberFormatException e) {
            blockSizeInit = DEFAULT_BLOCK_SIZE;
        }
        this.blockSize = blockSizeInit;
        
        this.tableName = 
            enkiProps.getProperty(PROPERTY_TABLE_NAME, DEFAULT_TABLE_NAME);
        
        Dialect dialect = Dialect.getDialect(config.getProperties());
        
        this.querySql = 
            "select "
            + HibernateDialectUtil.quote(dialect, COLUMN_NAME)
            + " from "
            + dialect.appendLockHint(LockMode.UPGRADE, tableName)
            + dialect.getForUpdateString();
        
        this.updateSql = 
            "update "
            + HibernateDialectUtil.quote(dialect, tableName) 
            + " set "
            + HibernateDialectUtil.quote(dialect, COLUMN_NAME)
            + " = ? where "
            + HibernateDialectUtil.quote(dialect, COLUMN_NAME)
            + " = ?";

        this.createDdl = generateCreateDdl(enkiProps, dialect);
        
        this.dropDdl = 
            "drop table " + HibernateDialectUtil.quote(dialect, tableName);
        
        this.initSql = generateInsertDml(enkiProps, dialect);
        
        // Load block on first call to nextMofId()
        this.nextMofId = 0;
        this.lastMofId = 0;
    }
    
    public static String generateCreateDdl(
        Properties storageProps, Dialect dialect)
    {
        String tableName = 
            storageProps.getProperty(PROPERTY_TABLE_NAME, DEFAULT_TABLE_NAME);
        
        return 
            "create table " + HibernateDialectUtil.quote(dialect, tableName) +
            " (" + COLUMN_NAME + " " + dialect.getTypeName(Types.BIGINT) + ")"
            + dialect.getTableTypeString();
    }
    
    public static String generateInsertDml(
        Properties storageProps, Dialect dialect)
    {
        String tableName = 
            storageProps.getProperty(PROPERTY_TABLE_NAME, DEFAULT_TABLE_NAME);
        
        return 
            "insert into " + 
            HibernateDialectUtil.quote(dialect, tableName) +
            " values (0)";
    }
    
    public Validity isGeneratorTableValid(Connection conn)
    {
        try {
            PreparedStatement stmt =
                conn.prepareStatement("select * from " + tableName);
            try {
                ResultSet rs = stmt.executeQuery();
                try {
                    // Allow extraneous columns
                    ResultSetMetaData metaData = rs.getMetaData();
                    if (!metaData.getColumnName(1).equals(COLUMN_NAME) ||
                        metaData.getColumnType(1) != Types.BIGINT)
                    {
                        return Validity.INVALID_WRONG_TABLE_DEFINITION;
                    }
                    
                    if (!rs.next()) {
                        return Validity.INVALID_MISSING_ROW;
                    }
                    
                    if (rs.next()) {
                        return Validity.INVALID_TOO_MANY_ROWS;
                    }
                }
                finally {
                    rs.close();
                }
            }
            finally {
                stmt.close();
            }

            return Validity.VALID;
        }
        catch(SQLException e) {
            return Validity.INVALID_MISSING_TABLE;
        }
    }
   
    public void configureTable(boolean createTable)
    {
        StatelessSession session = sessionFactory.openStatelessSession();
        
        Connection conn = session.connection();

        try {
            Validity validity = isGeneratorTableValid(conn);
            
            if (validity == Validity.VALID) {
                return;
            }

            switch(validity) {
            case INVALID_TOO_MANY_ROWS:
                throw new HibernateException(
                    "Too many rows in MOF ID generator table: " + tableName);
                
            case INVALID_WRONG_TABLE_DEFINITION:
                throw new HibernateException(
                    "Incorrect MOF ID generator table definition: "
                    + tableName);
                
            case INVALID_MISSING_ROW:
                throw new HibernateException(
                    "Missing MOF ID generator row in "
                    + tableName
                    + "; insert a row with a value exceeding that of the largest MOF ID in the schema");
                
            case INVALID_MISSING_TABLE:
                // Fall through and create the table
                break;
                
            default:
                throw new AssertionError("Internal error: missing enum case");
            }

            if (!createTable) {
                throw new HibernateException(
                    "MOF ID generator table is missing");
            }
            
            try {
                Statement stmt = conn.createStatement();
                try {
                    stmt.execute(createDdl);
                    stmt.executeUpdate(initSql);
                    conn.commit();
                }
                finally {
                    stmt.close();
                }
            }
            catch(SQLException e) {
                throw new HibernateException(
                    "Cannot create MOF ID generator table " + tableName, e);
            }
        }
        finally {
            session.close();
        }
    }
    
    public void dropTable()
    {
        StatelessSession session = sessionFactory.openStatelessSession();
        
        Connection conn = session.connection();

        try {
            Validity validity = isGeneratorTableValid(conn);
            
            if (validity == Validity.INVALID_MISSING_TABLE) {
                return;
            }

            try {
                Statement stmt = conn.createStatement();
                try {
                    stmt.execute(dropDdl);
                    conn.commit();
                }
                finally {
                    stmt.close();
                }
            }
            catch(SQLException e) {
                throw new HibernateException(
                    "Failed to drop MOF ID generator table " + tableName, e);
            }
        }
        finally {
            session.close();
        }
    }
    
    public synchronized long nextMofId()
    {
        if (nextMofId >= lastMofId) {
            try {
                readNextBlock();
            }
            catch(SQLException e) {
                throw new HibernateException(e);
            }
        }
        
        long mofId = nextMofId++;
        
        return mofId;
    }
    
    public synchronized long allocate(long numMofIds)
    {
        if (nextMofId + numMofIds >= lastMofId) {
            // Determine how many blocks we need and advance that many
            // blocks.  This causes a gap at the end of the current block 
            // (same as a repository restart would).

            // Always round up.  If blockSize doesn't divide evenly into
            // numMofIds we need the round up to get enough MOF IDs.  If
            // it does go in evenly, we'll use the extra MOF IDs for future
            // nextMofId() calls.
            long numBlocks = (numMofIds / blockSize) + 1;
            
            try {
                readNextBlocks(numBlocks);
            }
            catch(SQLException e) {
                throw new HibernateException(e);
            }
        }
        
        // Now have (or always had) enough MOF IDs before lastMofId, return 
        // nextMofId and increment it by the number of MOF IDs allocated.
        // Future callers to this method (or nextMofId()) will obtain values
        // from beyond the end of the allocated block. 
        long result = nextMofId;
            
        nextMofId += numMofIds;
        
        return result;
    }
    
    private void readNextBlock() throws SQLException
    {
        readNextBlocks(1);
    }
    
    private void readNextBlocks(long numBlocks) throws SQLException
    {
        final long numMofIds = numBlocks * (long)blockSize;
        
        StatelessSession session = sessionFactory.openStatelessSession();
        
        Connection conn = session.connection();
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
        }
        
        try {
            long next;
            int rows;

            // Repeat this update until we manage to update the row.  If the
            // update statement doesn't return 1 row modified, some other
            // MofIdGenerator must have beat us to it.
            do {
                PreparedStatement queryStmt = conn.prepareStatement(querySql);
                try {
                    ResultSet rs = queryStmt.executeQuery();
                    if (!rs.next()) {
                        throw new HibernateException(
                            "MOF ID sequence table not initialized");
                    }
                    
                    next = rs.getLong(1);
                    
                    if (rs.next()) {
                        throw new HibernateException(
                            "MOF ID sequence table has multiple rows");
                    }
                }
                finally {
                    queryStmt.close();
                }
                
                PreparedStatement updateStmt = 
                    conn.prepareStatement(updateSql);
                try {
                    updateStmt.setLong(1, next + numMofIds);
                    updateStmt.setLong(2, next);
                    rows = updateStmt.executeUpdate();
                }
                finally {
                    updateStmt.close();
                }
            } while(rows == 0);

            conn.commit();
            
            nextMofId = next;
            lastMofId = next + numMofIds;
            
            // Make sure we skip MOF ID 0 to avoid conflicts with the default
            // value Hibernate will pick should be ever fail to set an object's
            // MOF ID.
            if (nextMofId == 0L) {
                nextMofId++;
            }
        }
        catch(SQLException e) {
            conn.rollback();
            throw e;
        }
        finally {
            session.close();
        }
    }
    
    public static enum Validity
    {
        VALID,
        INVALID_MISSING_TABLE,
        INVALID_WRONG_TABLE_DEFINITION,
        INVALID_MISSING_ROW,
        INVALID_TOO_MANY_ROWS;
    }
}

// End MofIdGenerator.java
