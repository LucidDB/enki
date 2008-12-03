/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.HibernateMDRepository.*;
import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.config.*;
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
    public static final String PROP_EXTENT = "enki.backup.extent";
    public static final String PROP_EXTENT_ANNOTATION = 
        "enki.backup.annotation";
    public static final String PROP_METAMODEL = "enki.backup.metamodel";
    public static final String PROP_MIN_MOF_ID = "enki.backup.minMofId";
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
    
    private final HibernateMDRepository repos;
    private final Dialect dialect;
    
    HibernateBackupRestoreUtil(HibernateMDRepository repos)
    {
        this.repos = repos;
        this.dialect = repos.getSqlDialect();
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
        // Flush session to make sure pending, uncomitted changes are backed
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
            long minMofId = dumpData(dataFile, extentDesc.extent);
            
            backupProps.put(PROP_MIN_MOF_ID, String.valueOf(minMofId));
            
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
     * @return max MOF ID encountered during backup
     * @throws EnkiBackupFailedException on any error
     */
    private long dumpData(File dataFile, RefPackage extent) 
        throws EnkiBackupFailedException
    {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(dataFile),
                    DATA_ENCODING));
            
            long minMofId = dumpData(output, extent);
            
            output.flush();
            BufferedWriter w = output;
            output = null;
            w.close();
            
            return minMofId;
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
     * @return max MOF ID encountered during backup
     * @throws IOException on write error
     * @throws SQLException on database error
     */
    private long dumpData(BufferedWriter output, RefPackage extent)
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
                
                String table = assoc.getTable();
                assocTables.add(table);
                assocTableMofIdCols.putValues(table, assocColList);
                String collectionTable = assoc.getCollectionTable();
                if (collectionTable != null) {
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

                String table = cls.getTable();
                objectTables.add(table);
                
                objectTableMofIdCols.put(table, MOF_ID_COLUMN_NAME);
                for(String columnName: cls.getAssociationColumnNames()) {
                    objectTableMofIdCols.put(table, columnName);
                }
            }
        }
        
        long minAssocMofId = 
            dumpTables(output, assocTables, assocTableMofIdCols);
        long minObjectMofId = 
            dumpTables(output, objectTables, objectTableMofIdCols);
        
        return Math.min(minAssocMofId, minObjectMofId);
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
     * @return max MOF ID encountered during backup
     * @throws IOException on write error
     * @throws SQLException on database error
     */
    private long dumpTables(
        BufferedWriter output, 
        List<String> tables, 
        HashMultiMap<String, String> tableMofIdCols)
    throws IOException, SQLException
    {
        long minMofId = Long.MAX_VALUE;

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
                        stmt.executeQuery("select * from " + table);
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
            
            return minMofId;
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
        Session session = repos.getCurrentSession();
        session.clear();

        ZipInputStream zipStream = new ZipInputStream(stream);
        
        boolean throwing = true;
        try {
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
            
            if (repos.getExtent(metaModelExtent) == null) {
                throw new EnkiRestoreFailedException(
                    "Must install metamodel extent '" 
                    + metaModelExtent 
                    + "' prior to restoring this backup");
            }
            
            readZipEntry(zipStream, FILE_BACKUP_DATA);

            BufferedReader reader = makeReader(zipStream);
            
            Map<String, Class<? extends RefObject>> tableClassMap = 
                prepareSchema(extentDesc);
            loadData(reader, minMofId, tableClassMap);

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
                        repos.log.warning(
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
     * drops and re-creates the metamodel-specific tables in the schema and
     * then deletes all metamodel-related values from the type-lookup mapping
     * table.
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
        // TODO: when to invoke provider DDL script (if ever)?

        Map<String, Class<? extends RefObject>> tableClassMap =
            new HashMap<String, Class<? extends RefObject>>();
        
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
    
                tableClassMap.put(cls.getTable(), cls.getInstanceClass());
            }
        }

        Session session = repos.getCurrentSession();
        
        Connection conn = session.connection();

        // Execute drop scripts.  Plug-ins first, then main model since the
        // plug-ins may have FK references to association tables.
        ModelDescriptor modelDesc = extentDesc.modelDescriptor;
        for(ModelPluginDescriptor mpd: modelDesc.plugins) {
            executeDdl(conn, mpd.dropDdl, true);
        }
        executeDdl(conn, modelDesc.dropDdl, true);
        
        // Execute create scripts. Main model first, then plug-ins (FKs again).
        executeDdl(conn, modelDesc.createDdl, false);
        for(ModelPluginDescriptor mpd: modelDesc.plugins) {
            executeDdl(conn, mpd.createDdl, false);
        }
        
        SessionFactory sessionFactory = session.getSessionFactory();
        
        PreparedStatement stmt = 
            conn.prepareStatement(
                "delete from " +
                HibernateDialectUtil.quote(dialect, "ENKI_TYPE_LOOKUP") +
                " where " +
                HibernateDialectUtil.quote(dialect, "typeName") +
                " = ?");
        try {
            for(Class<? extends RefObject> cls: tableClassMap.values()) {
                // Evict these from the cache; we've just deleted them all.
                sessionFactory.evict(cls);
                
                stmt.setString(1, cls.getName());
                stmt.executeUpdate();
            }
            
            // Dump any queries that may reference evicted objects.
            sessionFactory.evictQueries();
        } finally {
            stmt.close();
        }
        
        return tableClassMap;
    }
    
    /**
     * Executes a metamodel DDL script on the given connection from the given
     * URL.  If configured to ignore errors, errors executing individual
     * DDL statements are ignored.  Errors creating or closing the SQL 
     * {@link Statement} object are always thrown.
     * 
     * @param conn database connection
     * @param ddlScript location of DDL script
     * @param ignoreErrors if true, errors are ignored
     * @throws IOException on error reading from the URL
     * @throws SQLException on database error (see above)
     */
    private void executeDdl(
        Connection conn, 
        URL ddlScript, 
        boolean ignoreErrors)
    throws IOException, SQLException
    {
        StringBuilder sql = new StringBuilder();
        BufferedReader rdr = makeReader(ddlScript.openStream());
        
        Statement stmt = conn.createStatement();
        try {
            String line;
            while((line = rdr.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.endsWith(";")) {
                    sql.append(trimmed);
                    
                    try {
                        stmt.execute(sql.toString());
                    } catch(SQLException e) {
                        if (!ignoreErrors) {
                            throw e;
                        }
                    }
                    
                    sql.setLength(0);
                } else {
                    sql.append(line).append("\n");
                }
            }
        } finally {
            try {
                stmt.close();
            } finally {
                rdr.close();
            }
        }
    }
    
    /**
     * Restores backed up data to metamodel tables given a reader on a backup 
     * data file.  MOF IDs are automatically mapped to values available in
     * target database.
     * 
     * @param data BufferedReader on the backed up data
     * @param minMofId minimum MOF ID in the backed up data
     * @param tableClassMap map of table name to instance class
     * @throws IOException on I/O error reading the backup data
     * @throws SQLException on database error
     */
    private void loadData(
        BufferedReader data, 
        long minMofId,
        Map<String, Class<? extends RefObject>> tableClassMap)
    throws IOException, SQLException
    {
        MofIdGenerator mofIdGenerator = repos.getMofIdGenerator();
        long maxMofId = -1;
        long baseMofId = mofIdGenerator.nextMofId();
        final long offset = baseMofId - minMofId;
        
        Session session = repos.getCurrentSession();
        
        Connection conn = session.connection();
        
        int maxDataLength = computeMaxRowLength(conn);
        
        PreparedStatement typeLookupStmt = 
            conn.prepareStatement(
                "insert into "
                + HibernateDialectUtil.quote(dialect, "ENKI_TYPE_LOOKUP")
                + " ("
                + HibernateDialectUtil.quote(dialect, MOF_ID_COLUMN_NAME)
                + ", "
                + HibernateDialectUtil.quote(dialect, "typeName")
                + ") values (?, ?)");
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
                assert(mofIdCol > 0);
                
                int numCols = descriptor.length - 1;
                
                // Assume all strings, and if that might overflow the
                // maximum packet, stream them all.
                boolean streamAllStrings = false;
                if ((numCols * MAX_UNSTREAMED_STRING_LEN) > maxDataLength) {
                    streamAllStrings = true;
                }
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                try {
                    int batchSize = 0;
                    List<Long> mofIds = new ArrayList<Long>();
                    for(int i = 0; i < numRows; i++) {
                        String dataRow = data.readLine();
                        if (dataRow == null) {
                            throw new IOException(
                                "Premature end of data file");
                        }
    
                        List<String> values = Type.split(dataRow);
                        if (values.size() != numCols) {
                            throw new IOException("Invalid record size");
                        }
    
                        int pos = 1;
                        for(String value: values) {
                            Type type = Type.fromString(value);
                            Object v = type.decode(value);
    
                            if (type == Type.MOFID) {
                                long mofId = ((Long)v).longValue() + offset;
                                maxMofId = Math.max(maxMofId, mofId);
                                
                                Long mofIdLong = mofId;
                                v = mofIdLong;
                                
                                if (pos == mofIdCol) {
                                    mofIds.add(mofIdLong);
                                }
                            }
                            
                            if (type == Type.CHARACTER) {
                                String vs = (String)v;
                                int vsLen = vs.length();
                                if (vsLen > MAX_UNSTREAMED_STRING_LEN ||
                                    streamAllStrings)
                                {
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
                                typeLookupStmt.setString(2, typeName);
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
        
        // Run up the mofId until it is greater than the the largest value we
        // used.
        // TODO: Add a mechanism to explicitly set the value.
        while(mofIdGenerator.nextMofId() <= maxMofId);
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
     * Determines the maximum row length for the current database connection.
     * This method is primarily useful for databases such as MySQL which
     * enforce a limit on the amount of data which may be transmitted in a
     * single packet.  If the data for a particular insert statement is 
     * larger than this value, it must be broken up in some way.
     * 
     * <p>If the database imposes no particular limit, this method returns
     * {@link Integer#MAX_VALUE}.  For MySQL this method returns the value
     * of <code>max_allowed_packet</code> variable or 1 MB if no value is
     * found for the variable.
     * 
     * @param conn database connection
     * @return maximum row length
     * @throws SQLException if there's an error reading the value from the
     *                      database
     */
    private int computeMaxRowLength(Connection conn)
        throws SQLException
    {
        int maxRowLength = Integer.MAX_VALUE;
        if (dialect instanceof MySQLDialect) {
            maxRowLength = 1024 * 1024; // MySQL default
            Statement stmt = conn.createStatement();
            try {
                ResultSet rset = 
                    stmt.executeQuery(
                        "show variables like 'max_allowed_packet'");
                try {
                    if (rset.next()) {
                        String maxAllowedPacketStr = rset.getString(2);
                        
                        try {
                            maxRowLength = 
                                Integer.parseInt(maxAllowedPacketStr);
                        } catch(NumberFormatException e) {
                            repos.log.warning(
                                "Error parsing MySQL max_allowed_packet value '"
                                + maxAllowedPacketStr 
                                + "'; using default (" 
                                + maxRowLength
                                + ")");
                        }
                    }
                } finally {
                    rset.close();
                }
            } finally {
                stmt.close();
            }
        }
        return maxRowLength;
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
        
        public static Type fromString(String data)
        {
            return fromString(data, 0);
        }
        
        public static Type fromString(String data, int pos)
        {
            if (data.regionMatches(pos, "null", 0, 4)) {
                return NULL;
            }
            
            if (data.charAt(pos) == '\'') {
                return CHARACTER;
            }
            
            int commaPos = data.indexOf(',', pos);
            int suffixPos = (commaPos < 0) ? data.length() - 1 : commaPos - 1;

            char suffix = data.charAt(suffixPos);
            
            for(Type type: values()) {
                if (type.suffix != 0 && type.suffix == suffix) {
                    return type;
                }
            }

            if (data.regionMatches(pos, "true", 0, 4) ||
                data.regionMatches(pos, "false", 0, 5))
            {
                return BOOLEAN;
            }
            
            return INT;
        }
        
        public static List<String> split(String values)
        {
            List<String> result = new ArrayList<String>();
            
            int pos = 0;
            int len = values.length();
            while(pos < len) {
                Type t = fromString(values, pos);
                switch(t) {
                default:
                    int commaPos = values.indexOf(',', pos);
                    if (commaPos < 0) {
                        commaPos = len;
                    }
                    result.add(values.substring(pos, commaPos));
                    pos = commaPos + 1;
                    break;
                    
                case CHARACTER:
                    int endPos = pos + 1;
                    CHAR_LOOP:
                    while(endPos < len) {
                        char ch = values.charAt(endPos);
                        switch(ch) {
                        case '\\':
                            endPos += 2;
                            break;
                        case '\'':
                            endPos++;
                            break CHAR_LOOP;
                        default: 
                            endPos++;
                            break;
                        }
                    }
                    assert(endPos == len || values.charAt(endPos) == ',');
                    result.add(values.substring(pos, endPos));
                    pos = endPos + 1;
                    break;
                }
            }
            
            return result;
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
                    case '\'':
                    case '\\':
                        b.append('\\').append(ch);
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
                    char ch = value.charAt(i);
                    if (ch == '\\') {
                        i++;
                        char next = value.charAt(i);
                        switch(next) {
                        default:
                            b.append(next);
                            break;
                        case 'n':
                            b.append('\n');
                            break;
                        case 'r':
                            b.append('\r');
                            break;
                        }
                    } else {
                        b.append(ch);
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
                throw new IllegalArgumentException(value);
            }            
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
