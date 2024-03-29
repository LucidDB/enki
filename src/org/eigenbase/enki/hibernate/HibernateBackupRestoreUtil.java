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
package org.eigenbase.enki.hibernate;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.HibernateMDRepository.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;
import org.hibernate.dialect.*;

/**
 * HibernateBackupRestoreUtil implements backup/restore of repository extents
 * via direct SQL access.
 * 
 * @author Stephan Zuercher
 */
public class HibernateBackupRestoreUtil
{
    /** Backup descriptor property holding the backed up extent's name. */
    public static final String PROP_EXTENT = "enki.backup.extent";
    
    /** 
     * Backup descriptor property holding the backed up extent's annotation.
     */
    public static final String PROP_EXTENT_ANNOTATION = 
        "enki.backup.annotation";
    
    /** 
     * Backup descriptor property holding the name of the metamodel for the 
     * backed up extent. 
     */
    public static final String PROP_METAMODEL = "enki.backup.metamodel";
    
    /** 
     * Backup descriptor property holding the minimum MOF ID used in the 
     * backed up extent. 
     */
    public static final String PROP_MIN_MOF_ID = "enki.backup.minMofId";
    
    /** 
     * Names the property holding the number of MOF IDs used by the backed up
     * extent in the backup descriptor.  The usage of MOF IDs may be sparse, 
     * however. 
     */
    public static final String PROP_MOF_ID_COUNT = "enki.backup.mofIdCount";
    
    /** 
     * Backup descriptor property holding the model's package version.
     */
    public static final String PROP_PACKAGE_VERSION = 
        "enki.backup.packageVersion";

    public static final String FILE_BACKUP_DESCRIPTOR = "enki/backup.desc";
    public static final String FILE_BACKUP_DATA = "enki/backup.data";
    
    private static final String DATA_ENCODING = "UTF-8";
    private static final int BUFFER_SIZE = 16384;
    
    private static final int MAX_UNSTREAMED_STRING_LEN = 1024;
    
    private static final int BATCH_SIZE = 500;
    
    private static final String MOF_ID_COLUMN_NAME = 
        HibernateMappingHandler.MOF_ID_COLUMN_NAME;
    
    private static final Logger log = 
        Logger.getLogger(HibernateBackupRestoreUtil.class.getName());
    
    private final HibernateMDRepository repos;
    private final Dialect dialect;
    private final String tablePrefix;
    
    HibernateBackupRestoreUtil(HibernateMDRepository repos)
    {
        this.repos = repos;
        this.dialect = repos.getSqlDialect();
        this.tablePrefix = repos.getTablePrefix();
    }
    
    /**
     * Backs up the given repository into the given OutputStream.  Writes an
     * uncompressed ZIP stream into the output stream containing a backup
     * descriptor (properties) file and a data file.
     * 
     * @param extentDesc ExtentDescriptor for the extent to back up
     * @param stream output stream
     * @throws EnkiBackupFailedException if there's an error backing up the
     *                                   extent
     */
    void backup(
        HibernateMDRepository.ExtentDescriptor extentDesc, OutputStream stream)
    throws EnkiBackupFailedException
    {
        log.info("Backing up extent '" + extentDesc.name + "'");
        
        // Flush session to make sure pending, uncommitted changes are backed
        // up.  It's weird behavior, but we want to mimic extent export.
        Session session = repos.getCurrentSession();
        session.flush();
        
        Properties backupProps = new Properties();
        backupProps.put(PROP_EXTENT, extentDesc.name);
        if (extentDesc.annotation != null) {
            backupProps.put(PROP_EXTENT_ANNOTATION, extentDesc.annotation);
        } else {
            backupProps.put(PROP_EXTENT_ANNOTATION, "");            
        }
        backupProps.put(PROP_METAMODEL, extentDesc.modelDescriptor.name);
        backupProps.put(
            PROP_PACKAGE_VERSION, HibernateMDRepository.PACKAGE_VERSION);
        
        File propsFile = null;
        File dataFile = null;
        try {
            propsFile = makeTempFile();
            dataFile = makeTempFile();
        } catch (IOException e) {
            if (propsFile != null) {
                propsFile.delete();
            }
            throw new EnkiBackupFailedException(e);
        }
        
        try {
            LongRangeWrapper mofIdRange = 
                dumpData(dataFile, extentDesc.extent);
            
            backupProps.put(PROP_MIN_MOF_ID, String.valueOf(mofIdRange.min));
            backupProps.put(
                PROP_MOF_ID_COUNT, 
                String.valueOf(mofIdRange.max - mofIdRange.min));
            
            FileOutputStream propsStream = new FileOutputStream(propsFile);
            backupProps.storeToXML(
                propsStream, 
                "Enki/Hibernate Backup: " + new java.util.Date().toString(),
                "UTF-8");
            propsStream.flush();
            propsStream.close();
            
            ZipOutputStream zipStream = new ZipOutputStream(stream);
            zipStream.setMethod(ZipOutputStream.STORED);
            
            writeZipEntry(zipStream, FILE_BACKUP_DESCRIPTOR, propsFile);
            writeZipEntry(zipStream, FILE_BACKUP_DATA, dataFile);
            
            // Do not close zipStream: it will close the underlying stream.
            // Instead, just clean up and let the caller close.
            zipStream.finish();
            zipStream.flush();
        } catch(Exception e) {
            throw new EnkiBackupFailedException(e);
        } finally {
            dataFile.delete();
            propsFile.delete();
        }
        
        log.info("Backed up extent '" + extentDesc.name + "'");
    }
    
    private File makeTempFile() throws IOException
    {
        return File.createTempFile("enkiBackup", ".data");
    }
    
    /**
     * Dumps data from the given RefPackage into the given data file.
     * 
     * @param dataFile temporary output file for data
     * @param extent extent to dump
     * @return range of MOF IDs encountered during backup
     * @throws EnkiBackupFailedException on any error
     */
    private LongRangeWrapper dumpData(File dataFile, RefPackage extent) 
        throws EnkiBackupFailedException
    {
        log.fine("Dumping extent data");
        
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(dataFile),
                    DATA_ENCODING));
            
            LongRangeWrapper mofIdRange = dumpData(output, extent);
            
            output.flush();
            BufferedWriter w = output;
            output = null;
            w.close();
            
            return mofIdRange;
        } catch(Exception e) {
            if (output != null) {
                try {
                    output.close();
                } catch(IOException e2) {
                    // Ignored
                }
            }
            
            throw new EnkiBackupFailedException(e);
        }
    }
    
    /**
     * Dumps data from the given RefPackage into the given writer.
     * 
     * @param output output writer for the backup
     * @param extent extent to dump
     * @return range of MOF IDs encountered during backup
     * @throws IOException on write error
     * @throws SQLException on database error
     */
    private LongRangeWrapper dumpData(BufferedWriter output, RefPackage extent)
        throws IOException, SQLException
    {
        Set<HibernateAssociation.Kind> assocTypesSeen = 
            new HashSet<HibernateAssociation.Kind>();
        
        List<String> assocTables = new ArrayList<String>();
        HashMultiMap<String, String> assocTableMofIdCols =
            new HashMultiMap<String, String>();
        List<String> objectTables = new ArrayList<String>();
        HashMultiMap<String, String> objectTableMofIdCols =
            new HashMultiMap<String, String>();
        LinkedList<HibernateRefPackage> pkgs = 
            new LinkedList<HibernateRefPackage>();
        pkgs.add((HibernateRefPackage)extent);
        
        List<String> assocColList = Arrays.asList(
            new String[] {
                HibernateMappingHandler.MOF_ID_COLUMN_NAME,
                HibernateMappingHandler.ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN,
                HibernateMappingHandler.ASSOC_ONE_TO_MANY_CHILD_ID_COLUMN,
                HibernateMappingHandler.ASSOC_MANY_TO_MANY_SOURCE_ID_COLUMN,
                HibernateMappingHandler.ASSOC_MANY_TO_MANY_TARGET_ID_COLUMN
            });
        
        while(!pkgs.isEmpty()) {
            HibernateRefPackage pkg = pkgs.removeFirst();
            
            for(RefPackage subPkg: 
                    GenericCollections.asTypedCollection(
                        pkg.refAllPackages(), RefPackage.class)) {
                if (subPkg instanceof HibernateRefPackage) {
                    pkgs.add((HibernateRefPackage)subPkg);
                }
            }
            
            for(HibernateRefAssociation assoc:
                    GenericCollections.asTypedCollection(
                        pkg.refAllAssociations(), 
                        HibernateRefAssociation.class))
            {
                HibernateAssociation.Kind kind = assoc.getKind();
                if (assocTypesSeen.contains(kind)) {
                    continue;
                }
                assocTypesSeen.add(kind);
                
                String table = tablePrefix + assoc.getTable();
                assocTables.add(table);
                assocTableMofIdCols.putValues(table, assocColList);
                String collectionTable = assoc.getCollectionTable();
                if (collectionTable != null) {
                    collectionTable = tablePrefix + collectionTable;
                    assocTables.add(collectionTable);
                    assocTableMofIdCols.putValues(
                        collectionTable, assocColList);
                }
            }

            for(HibernateRefClass cls:
                    GenericCollections.asTypedCollection(
                        pkg.refAllClasses(), 
                        HibernateRefClass.class))
            {
                if (((Classifier)cls.refMetaObject()).isAbstract()) {
                    continue;
                }

                String table = tablePrefix + cls.getTable();
                objectTables.add(table);
                
                objectTableMofIdCols.put(table, MOF_ID_COLUMN_NAME);
                for(String columnName: cls.getAssociationColumnNames()) {
                    objectTableMofIdCols.put(table, columnName);
                }
            }
        }
        
        LongRangeWrapper assocMofIdRange =
            dumpTables(output, assocTables, assocTableMofIdCols);
        LongRangeWrapper objectMofIdRange =
            dumpTables(output, objectTables, objectTableMofIdCols);
        
        return assocMofIdRange.union(objectMofIdRange);
    }
    
    /**
     * Dumps data from the given tables into the output writer.  This method
     * creates a temporary file and stores data in that file before copying
     * it into the output writer.  The temporary file is deleted before the
     * method exits.
     * 
     * @param output output writer for the backup
     * @param tables list of table names, ready for dialect-specific quoting,
     *               to backup
     * @param tableMofIdCols multi-map of table names to columns that store
     *                       MOF ID values (primary key or otherwise) 
     * @return range of MOF IDs encountered during backup
     * @throws IOException on write error
     * @throws SQLException on database error
     */
    private LongRangeWrapper dumpTables(
        BufferedWriter output, 
        List<String> tables, 
        HashMultiMap<String, String> tableMofIdCols)
    throws IOException, SQLException
    {
        long minMofId = Long.MAX_VALUE;
        long maxMofId = Long.MIN_VALUE;
        
        File tempFile = makeTempFile();
        
        try {
            Session session = repos.getCurrentSession();
            
            Connection conn = session.connection();
    
            Statement stmt = conn.createStatement();
            
            try {
                for(String table: tables) {
                    BufferedWriter tempOutput = 
                        new BufferedWriter(
                            new OutputStreamWriter(
                                new FileOutputStream(tempFile, false),
                                DATA_ENCODING));
    
                    StringBuilder tableHeader = new StringBuilder(table);
                    int numRows = 0;

                    Collection<String> mofIdCols = 
                        tableMofIdCols.getValues(table);
                    
                    ResultSet rset = 
                        stmt.executeQuery(
                            "select * from " 
                            + HibernateDialectUtil.quote(dialect, table));
                    try {
                        ResultSetMetaData metadata = rset.getMetaData();
                        int numCols = metadata.getColumnCount();
                        String[] columnNames = new String[numCols];
                        for(int i = 1; i <= numCols; i++) {
                            String columnName = metadata.getColumnName(i);
    
                            tableHeader.append(',').append(columnName);
                            columnNames[i - 1] = columnName;
                        }
                        
                        while(rset.next()) {
                            for(int i = 1; i <= numCols; i++) {
                                Object value = rset.getObject(i);
                                Type type = Type.fromObject(value);
                                if (type == Type.LONG && 
                                    mofIdCols.contains(columnNames[i - 1]))
                                {
                                    long mofId = rset.getLong(i);
                                    minMofId = Math.min(minMofId, mofId);
                                    maxMofId = Math.max(maxMofId, mofId);
                                    type = Type.MOFID;
                                }
                                
                                if (i != 1) {
                                    tempOutput.write(',');
                                }
                                tempOutput.write(type.encode(value));
                            }
                            tempOutput.newLine();
                            numRows++;
                        }
                        
                        tempOutput.flush();
                        tempOutput.close();
                    } finally {
                        rset.close();
                    }
                    
                    if (numRows > 0) {
                        output.write(tableHeader.toString());
                        output.newLine();
                        output.write(String.valueOf(numRows));
                        output.newLine();
                        append(output, tempFile);
                    }
                }
            } finally {
                stmt.close();
            }
            
            return new LongRangeWrapper(minMofId, maxMofId);
        } finally {
            tempFile.delete();
        }
    }
    
    /**
     * Appends the contents of the given file to the given output.
     * 
     * @param output writer to append data to 
     * @param tempFile source of data
     * @throws IOException on I/O error
     */
    private void append(BufferedWriter output, File tempFile)
        throws IOException
    {
        BufferedReader input = 
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(tempFile), DATA_ENCODING));
     
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int len;
            while((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
        } finally {
            input.close();
        }
    }
    
    /**
     * Appends the given file to the given {@link ZipOutputStream}.  This
     * method works for compressed or uncompressed ZIP output streams.  It
     * computes the required length and CRC fields for the ZIP entry before
     * copying the file into the ZIP output stream.
     * 
     * @param zipStream ZIP output stream
     * @param entryName name of entry in ZIP output stream
     * @param source source file
     * @throws IOException on I/O error
     */
    private void writeZipEntry(
        ZipOutputStream zipStream, String entryName, File source)
    throws IOException
    {
        log.fine("Writing ZIP entry");

        ZipEntry entry = new ZipEntry(entryName);
        entry.setSize(source.length());
        
        // Have to pre-compute CRC and size or else ZipOutputStream throws
        // if set to STORED (no compression) instead of default DEFLATED. 
        CRC32 crc32 = new CRC32();
        FileInputStream in = new FileInputStream(source);
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while((len = in.read(buffer)) > 0) {
            crc32.update(buffer, 0, len);
        }
        in.close();
        
        entry.setCrc(crc32.getValue());
        
        zipStream.putNextEntry(entry);
        
        in = new FileInputStream(source);
        while((len = in.read(buffer)) > 0) {
            zipStream.write(buffer, 0, len);
        }
        in.close();
        zipStream.closeEntry();

    }
    
    /**
     * Restores the backup stored in the given input stream into an existing
     * extent. 
     *
     * @param extentDesc
     * @param stream
     * @throws EnkiRestoreFailedException
     */
    void restore(
        ExtentDescriptor extentDesc,
        InputStream stream) 
    throws EnkiRestoreFailedException
    {
        log.info("Restoring extent '" + extentDesc.name + "'");
        
        Session session = repos.getCurrentSession();
        session.clear();

        ZipInputStream zipStream = new ZipInputStream(stream);
        
        boolean throwing = true;
        try {
            // Load backup descriptor.
            readZipEntry(zipStream, FILE_BACKUP_DESCRIPTOR);
            
            Properties backupProps = new Properties();
            backupProps.loadFromXML(new UncloseableInputStream(zipStream));

            String pkgVersion = backupProps.getProperty(PROP_PACKAGE_VERSION);
            if (!pkgVersion.equals(HibernateMDRepository.PACKAGE_VERSION)) {
                throw new EnkiRestoreFailedException(
                    "Cannot restore from incompatible package version '" 
                    + pkgVersion 
                    + "'");
            }
            
            String metaModelExtent = backupProps.getProperty(PROP_METAMODEL);
            String extentAnnotation = 
                backupProps.getProperty(PROP_EXTENT_ANNOTATION);
            
            long minMofId = 
                Long.parseLong(backupProps.getProperty(PROP_MIN_MOF_ID));
            long mofIdCount =
                Long.parseLong(backupProps.getProperty(PROP_MOF_ID_COUNT));
            
            if (repos.getExtent(metaModelExtent) == null) {
                throw new EnkiRestoreFailedException(
                    "Must install metamodel extent '" 
                    + metaModelExtent 
                    + "' prior to restoring this backup");
            }
            
            // Load backup data.
            readZipEntry(zipStream, FILE_BACKUP_DATA);

            BufferedReader reader = makeReader(zipStream);
            
            Map<String, Class<? extends RefObject>> tableClassMap = 
                prepareSchema(extentDesc);
            loadData(reader, minMofId, mofIdCount, tableClassMap);

            repos.setAnnotation(extentDesc.name, extentAnnotation);
            
            throwing = false;
        } catch(IOException e) {
            throw new EnkiRestoreFailedException(e);
        } catch(SQLException e) {
            throw new EnkiRestoreFailedException(e);
        } finally {
            try {
                zipStream.closeEntry();
                
                ZipEntry entry;
                while((entry = zipStream.getNextEntry()) != null) {
                    if (!throwing) {
                        log.warning(
                            "Ignoring unexpected backup stream entry '" 
                            + entry.getName() 
                            + "'");
                    }
                    
                    zipStream.closeEntry();
                }
            } catch(IOException e) {
                if (!throwing) {
                    throw new EnkiRestoreFailedException(e);
                }
            }
        }
        
        log.info("Restored extent '" + extentDesc.name + "'");
    }

    /**
     * Reads the next {@link ZipEntry} from the given {@link ZipInputStream}.
     * If the given file name does not match the ZIP entry's name, this method
     * throws an exception.
     * 
     * @param zipStream ZIP input stream
     * @param filename expected entry name
     * @return next ZipEntry from the ZIP input stream
     * @throws EnkiRestoreFailedException on I/O error or if the next ZipEntry
     *                                    does not have the expected name.
     */
    private ZipEntry readZipEntry(ZipInputStream zipStream, String filename)
        throws EnkiRestoreFailedException
    {
        try {
            ZipEntry entry = zipStream.getNextEntry();
            if (!entry.getName().equals(filename)) {
                throw new EnkiRestoreFailedException(
                    "Encountered unexpected backup file; expected '" 
                    + filename 
                    + "', got '" 
                    + entry.getName() + "'");
            }

            return entry;
        } catch(IOException e) {
            throw new EnkiRestoreFailedException(e);
        }
    }

    /**
     * Wraps the given {@link InputStream} in a {@link BufferedReader} using
     * the standard backup {@link #DATA_ENCODING encoding}.
     * 
     * @param in input stream
     * @return BufferedReader wrapping the input stream
     * @throws IOException on I/O error
     */
    private BufferedReader makeReader(InputStream in) throws IOException
    {
        return
            new BufferedReader(
                new InputStreamReader(in, DATA_ENCODING));

    }
    
    /**
     * Prepares the database schema for restoration of a backup and collects
     * table/class name mapping information from the metamodel.  This method
     * truncates metamodel-specific tables in the schema and deletes all 
     * metamodel-related values from the type-lookup mapping table.
     * 
     * @param extentDesc extent to prepare
     * @return map of table names to instance classes
     * @throws SQLException on database error
     * @throws IOException on I/O error reading metamodel DDL files
     */
    private Map<String, Class<? extends RefObject>> prepareSchema(
        ExtentDescriptor extentDesc)
    throws SQLException, IOException
    {
        log.fine("Delete existing extent data");
        
        Map<String, Class<? extends RefObject>> tableClassMap =
            new HashMap<String, Class<? extends RefObject>>();
        Set<String> assocTables = new HashSet<String>();
        Set<String> assocChildTables = new HashSet<String>();
        
        LinkedList<HibernateRefPackage> queue = 
            new LinkedList<HibernateRefPackage>();
        queue.add((HibernateRefPackage)extentDesc.extent);
        while(!queue.isEmpty()) {
            HibernateRefPackage pkg = queue.removeFirst();
            
            for(RefPackage subPkg: 
                    GenericCollections.asTypedCollection(
                        pkg.refAllPackages(), RefPackage.class)) {
                if (subPkg instanceof HibernateRefPackage) {
                    queue.add((HibernateRefPackage)subPkg);
                }
            }
            
            for(HibernateRefClass cls:
                    GenericCollections.asTypedCollection(
                        pkg.refAllClasses(), 
                        HibernateRefClass.class))
            {
                if (((Classifier)cls.refMetaObject()).isAbstract()) {
                    continue;
                }
    
                tableClassMap.put(
                    tablePrefix + cls.getTable(), cls.getInstanceClass());
            }
            
            for(HibernateRefAssociation assoc:
                    GenericCollections.asTypedCollection(
                        pkg.refAllAssociations(), 
                        HibernateRefAssociation.class))
            {
                assocTables.add(tablePrefix + assoc.getTable());
                String collectionTable = assoc.getCollectionTable();
                if (collectionTable != null) {
                    assocChildTables.add(tablePrefix + collectionTable);
                }
            }
        }

        Session session = repos.getCurrentSession();
        
        Connection conn = session.connection();

        String deleteMechanism = "truncate table ";
        if (dialect instanceof HSQLDialect) {
            deleteMechanism = "delete from ";
        }
        
        Statement stmt = conn.createStatement();
        try {
            for(String table: tableClassMap.keySet()) {
                stmt.execute(
                    deleteMechanism +
                    HibernateDialectUtil.quote(dialect, table));
            }
            
            for(String table: assocChildTables) {
                stmt.execute(
                    deleteMechanism +
                    HibernateDialectUtil.quote(dialect, table));
            }
            
            for(String table: assocTables) {
                stmt.execute(
                    deleteMechanism +
                    HibernateDialectUtil.quote(dialect, table));
            }
        } finally {
            stmt.close();
        }
            
        SessionFactory sessionFactory = session.getSessionFactory();
        
        PreparedStatement typeLookupStmt = 
            conn.prepareStatement(
                "delete from " +
                HibernateDialectUtil.quote(dialect, "ENKI_TYPE_LOOKUP") +
                " where " +
                HibernateDialectUtil.quote(dialect, "tablePrefix") +
                " = ? and " +
                HibernateDialectUtil.quote(dialect, "typeName") +
                " = ?");
        try {
            int batchSize = 0;
            for(Class<? extends RefObject> cls: tableClassMap.values()) {
                // Evict these from the cache; we've just deleted them all.
                sessionFactory.evict(cls);
                
                typeLookupStmt.setString(1, cls.getName());
                typeLookupStmt.setString(2, tablePrefix);
                typeLookupStmt.addBatch();
                batchSize++;
                
                if (batchSize == BATCH_SIZE) {
                    typeLookupStmt.executeBatch();
                }
            }
            
            if (batchSize > 0) {
                typeLookupStmt.executeBatch();
            }

            // Dump any queries that may reference evicted objects.
            sessionFactory.evictQueries();
        } finally {
            typeLookupStmt.close();
        }
        
        return tableClassMap;
    }
    
    /**
     * Restores backed up data to metamodel tables given a reader on a backup 
     * data file.  MOF IDs are automatically mapped to values available in
     * target database.
     * 
     * @param data BufferedReader on the backed up data
     * @param minMofId minimum MOF ID in the backed up data
     * @param mofIdCount the number of MOF IDs to allocate
     * @param tableClassMap map of table name to instance class
     * @throws IOException on I/O error reading the backup data
     * @throws SQLException on database error
     */
    private void loadData(
        BufferedReader data, 
        long minMofId,
        long mofIdCount,
        Map<String, Class<? extends RefObject>> tableClassMap)
    throws IOException, SQLException
    {
        MofIdGenerator mofIdGenerator = repos.getMofIdGenerator();
        
        log.fine("Update MOF ID generator");
        
        long baseMofId = mofIdGenerator.allocate(mofIdCount);
        
        log.fine("Restore backup data");
        
        // Offset may be negative.
        final long offset = baseMofId - minMofId;
        
        Session session = repos.getCurrentSession();
        
        Connection conn = session.connection();
        
        PreparedStatement typeLookupStmt = 
            conn.prepareStatement(
                "insert into "
                + HibernateDialectUtil.quote(dialect, "ENKI_TYPE_LOOKUP")
                + " ("
                + HibernateDialectUtil.quote(dialect, MOF_ID_COLUMN_NAME)
                + ", "
                + HibernateDialectUtil.quote(dialect, "tablePrefix")
                + ", "
                + HibernateDialectUtil.quote(dialect, "typeName")
                + ") values (?, ?, ?)");
        try {
            String line;
            while((line = data.readLine()) != null) {
                String[] descriptor = line.split(",");
                
                String numRowsStr = data.readLine();
                if (numRowsStr == null) {
                    throw new IOException("Premature end of data file");
                }
                
                int numRows;
                try {
                    numRows = Integer.parseInt(numRowsStr);
                } catch(NumberFormatException e) {
                    throw new IOException(
                        "Invalid row count value: " + numRowsStr);
                }
                
                String sql = getInsertSql(descriptor);
                
                int mofIdCol = -1;
                for(int i = 1; i < descriptor.length; i++) {
                    if (descriptor[i].equals(MOF_ID_COLUMN_NAME)) {
                        mofIdCol = i;
                        break;
                    }
                }
                if (mofIdCol < 0) {
                    throw new IOException("Record missing MOF ID column");
                }
                
                int numCols = descriptor.length - 1;
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                try {
                    List<String> values = new ArrayList<String>(numCols);
                    List<Type> types = new ArrayList<Type>(numCols);
                    
                    int batchSize = 0;
                    List<Long> mofIds = new ArrayList<Long>();
                    for(int i = 0; i < numRows; i++) {
                        String dataRow = data.readLine();
                        if (dataRow == null) {
                            throw new IOException(
                                "Premature end of data file");
                        }
    
                        values.clear();
                        types.clear();
                        
                        Type.split(dataRow, values, types);
                        assert(values.size() == types.size());
                        if (values.size() != numCols) {
                            throw new IOException("Invalid record size");
                        }
    
                        int pos = 1;
                        Iterator<String> valueIter = values.iterator();
                        Iterator<Type> typeIter = types.iterator();
                        while(valueIter.hasNext()) {
                            String value = valueIter.next();
                            Type type = typeIter.next();
                            
                            Object v = type.decode(value);
    
                            if (type == Type.MOFID) {
                                Long mofIdLong = 
                                    ((Long)v).longValue() + offset;
                                v = mofIdLong;
                                
                                if (pos == mofIdCol) {
                                    mofIds.add(mofIdLong);
                                }
                            }
                            
                            if (type == Type.CHARACTER) {
                                String vs = (String)v;
                                int vsLen = vs.length();
                                if (vsLen > MAX_UNSTREAMED_STRING_LEN) {
                                    stmt.setCharacterStream(
                                        pos,
                                        new StringReader(vs),
                                        vsLen);
                                } else {
                                    stmt.setString(pos, vs);                                
                                }
                            } else {
                                stmt.setObject(pos, v);
                            }
                            pos++;
                        }
                        
                        stmt.addBatch();
                        batchSize++;
                        
                        if (batchSize == BATCH_SIZE) {
                            stmt.executeBatch();
                            batchSize = 0;
                        }
                    }
                    
                    if (batchSize > 0) {
                        stmt.executeBatch();
                    }
                    
                    if (!mofIds.isEmpty()) {
                        Class<? extends RefObject> cls = 
                            tableClassMap.get(descriptor[0]);
                        if (cls != null) {                            
                            String typeName = cls.getName();
                            batchSize = 0;
                            for(Long mofId: mofIds) {
                                typeLookupStmt.setLong(1, mofId);
                                typeLookupStmt.setString(2, tablePrefix);
                                typeLookupStmt.setString(3, typeName);
                                typeLookupStmt.addBatch();
                                batchSize++;
                                
                                if (batchSize == BATCH_SIZE) {
                                    typeLookupStmt.executeBatch();
                                    batchSize = 0;
                                }
                            }
                            
                            if (batchSize > 0) {
                                typeLookupStmt.executeBatch();
                            }
                        }
                    }
                } finally {
                    stmt.close();
                }
            }
        } finally {
            typeLookupStmt.close();
        }        
    }

    /**
     * Generates an insert statement given a backup file table descriptor.
     * This method assumes <code>descriptor[0]</code> is a table name and
     * all remaining elements of descriptor are column names.
     * 
     * @param descriptor table descriptor
     * @return Parameterized SQL insert statement.
     */
    private String getInsertSql(String[] descriptor)
    {
        StringBuilder sql = new StringBuilder();
        
        sql
            .append("insert into ")
            .append(HibernateDialectUtil.quote(dialect, descriptor[0]))
            .append(" (");
        for(int i = 1; i < descriptor.length; i++) {
            if (i > 1) {
                sql.append(", ");
            }
            String colName = descriptor[i];
            sql.append(HibernateDialectUtil.quote(dialect, colName));
        }
        
        sql.append(") values (");
        for(int i = 1; i < descriptor.length; i++) {
            if (i > 1) {
                sql.append(", ");
            }
            sql.append('?');
        }
        sql.append(')');
        
        String result = sql.toString();

        return result;
    }

    /**
     * Type represents the various types of data which may be stored in the
     * repository's metamodel tables.
     */
    public enum Type
    {
        /** Any character data. Quoted, no suffix. */
        CHARACTER(true),
        
        /** 
         * MOF ID data. Distinguished from {@link #LONG} to allow MOF ID
         * manipulation on restoration.  Suffixed with "M".
         */
        MOFID('M', Long.class),
        
        /** Long integer data. Suffixed with "L". */
        LONG('L', Long.class),
        
        /** Integer data. No suffix. */
        INT(false, Integer.class),
        
        /** Single precision floating point data. Suffixed with "f". */
        FLOAT('f', Float.class),
        
        /** Double precision floating point data. Suffixed with "d". */
        DOUBLE('d', Double.class),
        
        /** Arbitrary precision floating point data. Suffixed with "D". */
        BIG_DECIMAL('D', BigDecimal.class),
        
        /** BigInteger data. Suffixed with "I". */
        BIG_INTEGER('I', BigInteger.class),
        
        /** Boolean data. No suffix. */
        BOOLEAN(false, Boolean.class),
        
        /** Null value. No suffix. */
        NULL;
        
        private static final Map<Class<?>, Type> classTypeLookup;
        static {
            Map<Class<?>, Type> m = new HashMap<Class<?>, Type>();
            m.put(String.class, CHARACTER);
            m.put(Long.class, LONG);
            m.put(Integer.class, INT);
            m.put(Float.class, FLOAT);
            m.put(Double.class, DOUBLE);
            m.put(BigDecimal.class, BIG_DECIMAL);
            m.put(BigInteger.class, BIG_INTEGER);
            m.put(Boolean.class, BOOLEAN);
            classTypeLookup = Collections.unmodifiableMap(m);
        }
        
        private static final Map<Character, Type> suffixLookup;
        static {
            Map<Character, Type> m = new HashMap<Character, Type>();
            m.put(MOFID.suffix, MOFID);
            m.put(LONG.suffix, LONG);
            m.put(FLOAT.suffix, FLOAT);
            m.put(DOUBLE.suffix, DOUBLE);
            m.put(BIG_DECIMAL.suffix, BIG_DECIMAL);
            m.put(BIG_INTEGER.suffix, BIG_INTEGER);
            suffixLookup = Collections.unmodifiableMap(m);
        }
        
        private final char suffix;
        private final boolean quote;
        private final boolean isNull;
        private final Class<?> cls;
        private final Constructor<?> cons;
        
        private Type(char suffix, Class<?> cls)
        {
            this(suffix, false, cls);
        }
        
        private Type(boolean quote)
        {
            this((char)0, quote, null);
        }
        
        private Type(boolean quote, Class<?> cls)
        {
            this((char)0, quote, cls);
        }
        
        private Type(char suffix, boolean quote, Class<?> cls)
        {
            this.suffix = suffix;
            this.quote = quote;
            this.isNull = false;
            this.cls = cls;
            if (cls != null) {
                try {
                    this.cons = cls.getConstructor(String.class);
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(e);
                }
            } else {
                this.cons = null;
            }
        }
        
        private Type()
        {
            this.suffix = 0;
            this.quote = false;
            this.isNull = true;
            this.cls = null;
            this.cons = null;
        }
        
        public static Type fromObject(Object o) {
            if (o == null) {
                return NULL;
            }
            
            return fromClass(o.getClass());
        }
        
        public static Type fromClass(Class<?> cls)
        {
            Type type = classTypeLookup.get(cls);
            if (type == null) {
                throw new IllegalArgumentException(
                    "Unknown type mapping for: " + cls.getName());
            }
            return type;
        }
        
        /**
         * Determines the type of the value in data starting at character
         * pos.
         * 
         * @param data data string
         * @param pos start type detection here
         * @param endPos this wrapper is updated to contain the index of the 
         *               character immediately after the end of the type
         *               (which may be beyond the end of the data)
         * @return type of the data at pos
         */
        private static Type fromString(String data, int pos, IntWrapper endPos)
        {
            if (data.startsWith("null", pos)) {
                endPos.value = pos + 4; 
                return NULL;
            }
            
            if (data.startsWith("'", pos)) {
                int start = pos + 1;
                int end = data.indexOf('\'', start);
                if (end < 0) {
                    throw new IllegalStateException(
                        "Unterminated character string");
                }
                endPos.value = end + 1;
                return CHARACTER;
            }
            
            int commaPos = data.indexOf(',', pos);
            int endPosValue = (commaPos < 0) ? data.length() : commaPos;
            endPos.value = endPosValue;
            int suffixPos = endPosValue - 1;

            char suffix = data.charAt(suffixPos);
            
            Type type = suffixLookup.get(suffix);
            if (type != null) {
                return type;
            }

            if (data.startsWith("true", pos) || data.startsWith("false", pos))
            {
                return BOOLEAN;
            }
            
            return INT;
        }
        
        public static void split(
            String values, List<String> result, List<Type> types)
        {
            IntWrapper endPosWrapper = new IntWrapper();
            
            int pos = 0;
            int len = values.length();
            while(pos < len) {
                Type t = fromString(values, pos, endPosWrapper);
                types.add(t);
                int commaPos = endPosWrapper.value;
                result.add(values.substring(pos, commaPos));
                pos = commaPos + 1;
            }
        }
        
        public String encode(Object o)
        {
            if (o == null || isNull) {
                return "null";
            }
            
            String s = o.toString();
            if (quote) {
                StringBuilder b = new StringBuilder();
                int len = s.length();

                b.append('\'');
                for(int i = 0; i < len; i++) {
                    char ch = s.charAt(i);
                    switch(ch) {
                    case '\\':
                        b.append('\\').append(ch);
                        break;
                        
                    case '\'':
                        b.append("\\a");
                        break;
                        
                    case '\n':
                        b.append("\\n");
                        break;
                        
                    case '\r':
                        b.append("\\r");
                        break;
                        
                    default:
                        b.append(ch);
                    }
                }
                b.append('\'');
                return b.toString();
            } else if (suffix != 0) {
                return s + suffix;
            } else {
                return s;
            }
        }
        
        public Object decode(String value)
        {
            if (isNull) {
                return null;
            }
            
            if (quote) {
                StringBuilder b = new StringBuilder();
                int len = value.length() - 1;
                
                int i = 1;
                while(i < len) {
                    int backslash = value.indexOf('\\', i);
                    if (backslash < 0) {
                        b.append(value.substring(i, len));
                        break;
                    }

                    if (backslash > i) {
                        b.append(value.substring(i, backslash));
                    }
                    i = backslash + 1;

                    char next = value.charAt(i);
                    switch(next) {
                    default:
                        b.append(next);
                        break;
                    case 'a':
                        b.append('\'');
                        break;
                    case 'n':
                        b.append('\n');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    }
                    
                    i++;
                }
                return b.toString();
            }
            
            assert(cls != null);
            assert(cons != null);
            
            String consValue = value;
            if (suffix != 0) {
                int suffixPos = value.length() - 1;
                assert(value.charAt(suffixPos) == suffix);
                
                consValue = value.substring(0, suffixPos);
            }
            
            try {
                return cons.newInstance(consValue);
            } catch (Exception e) {
                throw new IllegalArgumentException(value, e);
            }            
        }
    }
    
    private static class IntWrapper
    {
        public int value;

        public IntWrapper()
        {
        }
    }
    
    private static class LongRangeWrapper
    {
        public long min;
        public long max;
        
        public LongRangeWrapper(long min, long max)
        {
            this.min = min;
            this.max = max;
        }
        
        public LongRangeWrapper union(LongRangeWrapper that)
        {
            return new LongRangeWrapper(
                Math.min(this.min, that.min),
                Math.max(this.max, that.max));
        }
    }
    
    /**
     * UncloseableInputStream allows data being read from a 
     * {@link ZipInputStream} to be adapted to methods which close their given
     * input stream.  For example, {@link Properties#loadFromXML(InputStream)}
     * closes its input stream when parsing is finished and would prevent a
     * caller from reading additional data from a ZipInputStream.  All methods
     * except {@link #close} are delegated to the wrapped {@link InputStream}. 
     */
    private static class UncloseableInputStream extends InputStream
    {
        private final InputStream in;
        
        public UncloseableInputStream(InputStream in)
        {
            this.in = in;
        }

        @Override
        public int read() throws IOException
        {
            return in.read();
        }
        
        @Override
        public void close() throws IOException
        {
            // NO OP
        }
        
        @Override
        public int available()
            throws IOException
        {
            return in.available();
        }

        @Override
        public void mark(int readlimit)
        {
            in.mark(readlimit);
        }

        @Override
        public boolean markSupported()
        {
            return in.markSupported();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            return in.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException
        {
            return in.read(b);
        }

        @Override
        public void reset() throws IOException
        {
            in.reset();
        }

        @Override
        public long skip(long n) throws IOException
        {
            return in.skip(n);
        }
    }
}
