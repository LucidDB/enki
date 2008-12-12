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
package org.eigenbase.enki.test.performance;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;
import javax.jmi.xmi.*;

import org.eigenbase.enki.test.*;
import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;
import org.netbeans.api.mdr.*;
import org.netbeans.api.xmi.*;

import eem.sample.special.*;

/**
 * BackupRestoreLargeDataSet test performs a backup/restore test with a large
 * data set.  This test is intended primarily as a why to test the performance
 * of larger data sets.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class BackupRestoreLargeDataSetTest extends SampleModelTestBase
{
    private static final int N_OBJ = 1000;
    private static Random rng;
    private long mark;
    
    /**
     * Generates an XMI document containing a large number of objects.
     * 
     * @param args args[0] must be the XMI output's filename
     */
    public static void main(String[] args) throws Exception
    {
        File output = new File(args[0]);
        
        setUpTestClass();
        
        createLargeDataSet();
        
        exportXmi(output);
        
        tearDownTestClass();
    }
    
    private static void createLargeDataSet() throws Exception
    {
        resetRng();
        
        // Create N_OBJ objects of each type        
        createObjects();
        createAssociations();
    }
    
    private static void createObjects() throws Exception
    {
        getRepository().beginTrans(true);        
        try {
            LinkedList<RefPackage> packageQueue = new LinkedList<RefPackage>();
            packageQueue.add(getPackage());

            while(!packageQueue.isEmpty()) {
                RefPackage pkg = packageQueue.removeFirst();
                
                packageQueue.addAll(
                    GenericCollections.asTypedCollection(
                        pkg.refAllPackages(), RefPackage.class));
                
                for(RefClass cls:
                        GenericCollections.asTypedCollection(
                            pkg.refAllClasses(), RefClass.class))
                {
                    if (cls instanceof StringManglerTestClass) {
                        // Ignore. This class's attribute names cause problems
                        // with XMI import/export (XMI code doesn't handle
                        // attributes with characters that are not legal as
                        // XML attribute names).
                        continue;
                    }
                    
                    Method m = getTypedFactoryMethod(cls);
                    if (m == null) {
                        // Abstract.
                        continue;
                    }
                    Class<?>[] paramTypes = m.getParameterTypes();
                        
                    for(int i = 0; i < N_OBJ; i++) {
                        Object[] params = new Object[paramTypes.length];
                        for(int j = 0; j < paramTypes.length; j++) {
                            Class<?> paramType = paramTypes[j];
                            
                            if (RefObject.class.isAssignableFrom(paramType)) {
                                params[j] = null;
                            } else {
                                params[j] = generateValue(paramType);
                            }
                        }
                        
                        m.invoke(cls, params);  
                    }
                }
            }
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    private static Method getTypedFactoryMethod(RefClass cls)
    {
        Method noArgMethod = null;
        
        Method[] methods = cls.getClass().getMethods();
        for(Method method: methods) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && 
                !Modifier.isStatic(modifiers) &&
                method.getName().startsWith("create"))
            {
                if (method.getParameterTypes().length > 0) {
                    return method;
                } else {
                    noArgMethod = method;
                }
            }
        }
        
        return noArgMethod;
    }
    
    private static Object generateValue(Class<?> type) throws Exception
    {
        if (String.class.equals(type)) {
            StringBuilder b = new StringBuilder();
            int len = rng.nextInt(17) + 16;
            for(int i = 0; i < len; i++) {
                b.append((char)('A' + rng.nextInt(26)));
            }
            return b.toString();
        }
        
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return rng.nextBoolean();
        }
        
        if (Integer.class.equals(type) || int.class.equals(type)) {
            return rng.nextInt();
        }
        
        if (Long.class.equals(type) || long.class.equals(type)) {
            return rng.nextLong();
        }
        
        if (Float.class.equals(type) || float.class.equals(type)) {
            return rng.nextFloat();
        }
        
        if (Double.class.equals(type) || double.class.equals(type)) {
            return rng.nextDouble();
        }
        
        if (RefEnum.class.isAssignableFrom(type)) {
            return generateEnumLiteral(type);
        }
        
        if (Collection.class.isAssignableFrom(type)) {
            return null;
        }
        
        throw new IllegalArgumentException("Type: " + type.getName());
    }
    
    private static Object generateEnumLiteral(Class<?> type) throws Exception
    {
        Class<?> enumType = type;
        if (type.isInterface()) {
            enumType = Class.forName(type.getName() + "Enum");
        }

        Field[] fields = enumType.getDeclaredFields();
        
        List<Field> literalFields = new ArrayList<Field>();
        
        for(Field field: fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) &&
                Modifier.isStatic(modifiers) &&
                field.getType().equals(enumType))
            {
                literalFields.add(field);
            }
        }
        
        if (literalFields.isEmpty()) {
            throw new IllegalArgumentException(
                "No literals in " + enumType.getName());
        }
        
        int pick = rng.nextInt(literalFields.size());

        return literalFields.get(pick).get(null);
    }
    
    private static void createAssociations()
    {
        getRepository().beginTrans(true);        
        try {
            LinkedList<RefPackage> packageQueue = new LinkedList<RefPackage>();
            packageQueue.add(getPackage());

            while(!packageQueue.isEmpty()) {
                RefPackage pkg = packageQueue.removeFirst();
                
                packageQueue.addAll(
                    GenericCollections.asTypedCollection(
                        pkg.refAllPackages(), RefPackage.class));
                
                for(RefAssociation assoc:
                    GenericCollections.asTypedCollection(
                        pkg.refAllAssociations(), RefAssociation.class))
                {
                    Association a = (Association)assoc.refMetaObject();
                    AssociationEnd e1 = (AssociationEnd)a.getContents().get(0);
                    if (e1.getAggregation() == AggregationKindEnum.COMPOSITE) {
                        continue;
                    }
                    RefClass c1 = findRefClass(e1.getType());
                    MultiplicityType m1 = e1.getMultiplicity();
                    AssociationEnd e2 = (AssociationEnd)a.getContents().get(1);
                    if (e2.getAggregation() == AggregationKindEnum.COMPOSITE) {
                        continue;
                    }
                    RefClass c2 = findRefClass(e2.getType());
                    MultiplicityType m2 = e2.getMultiplicity();
                    
                    List<RefObject> objs1 = 
                        new ArrayList<RefObject>(
                            GenericCollections.asTypedCollection(
                                c1.refAllOfType(),
                                RefObject.class));
                    Collections.sort(objs1, new RefObjComparator());
                    List<RefObject> objs2 = 
                        new ArrayList<RefObject>(
                            GenericCollections.asTypedCollection(
                                c2.refAllOfType(),
                                RefObject.class));
                    Collections.sort(objs2, new RefObjComparator());
                    
                    if (m1.getUpper() == 1 && m2.getUpper() == 1) {
                        // 1-1
                        ONE_TO_ONE_LOOP:
                        while(!objs1.isEmpty() && !objs2.isEmpty()) {
                            RefObject o1 = null;
                            RefObject o2 = null;
                            while(true) {
                                int p1 = rng.nextInt(objs1.size());
                                int p2 = rng.nextInt(objs2.size());
                                
                                o1 = objs1.get(p1);
                                o2 = objs2.get(p2);
                                
                                if (!o1.equals(o2)) {
                                    objs1.remove(p1);
                                    objs2.remove(p2);
                                    break;
                                }
                                
                                if (objs1.size() == 1 && objs2.size() == 1) {
                                    // Same object is only choice left from
                                    // both sides: give up.
                                    break ONE_TO_ONE_LOOP;
                                }
                            }
                            
                            assoc.refAddLink(o1, o2);
                        }
                    } else if (m1.getUpper() != 1 && m2.getUpper() != 1) {
                        // *-*
                        for(RefObject o1: objs1) {
                            Collections.shuffle(objs2, rng);
                            List<RefObject> o2s = objs2.subList(0, 5);
                            
                            for(RefObject o2: o2s) {
                                if (!o1.equals(o2)) {
                                    assoc.refAddLink(o1, o2);
                                }
                            }
                        }
                    } else {
                        List<RefObject> objsSingle = objs1;
                        List<RefObject> objsMult = objs2;
                        boolean reversed = false;
                        if (m1.getUpper() != 1) {
                            objsSingle = objs2;
                            objsMult = objs1;
                            reversed = true;
                        }

                        int step = 1;
                        int choose = objsMult.size() / objsSingle.size();
                        if (choose <= 1) {
                            choose = 2;
                            step = 
                                objsSingle.size() * choose / objsMult.size();
                        }
                        
                        Collections.shuffle(objs2, rng);
                        for(int i = 0; i < objsSingle.size(); i += step) {
                            RefObject o1 = objsSingle.get(i);
                            
                            List<RefObject> o2s = objsMult.subList(0, choose);
                            
                            for(RefObject o2: o2s) {
                                if (!o1.equals(o2)) {
                                    if (reversed) {
                                        assoc.refAddLink(o2, o1);
                                    } else {
                                        assoc.refAddLink(o1, o2);
                                    }
                                }
                            }
                            
                            o2s.clear();
                        }
                    }
                }
            }
        } finally {
            getRepository().endTrans(false);
        }
    }
    
    private static RefClass findRefClass(Classifier cls)
    {
        List<Namespace> containers = new ArrayList<Namespace>();
        {
            Namespace ns = cls.getContainer();
            while(ns != null) {
                containers.add(ns);
                ns = ns.getContainer();
            }
        }
        
        Collections.reverse(containers);
        
        RefPackage pkg = getPackage();
        MofPackage mofPkg = (MofPackage)pkg.refMetaObject();
        if (!mofPkg.equals(containers.get(0))) {
            throw new IllegalArgumentException(
                "root pkg = " + mofPkg.getName() + "; obj root = " 
                + containers.get(0).getName());
        }
        containers.remove(0);
        for(Namespace ns: containers) {
            pkg = pkg.refPackage(ns.getName());
            if (pkg == null) {
                throw new IllegalArgumentException(
                    "cannot find " + ns.getName() + " pkg in " 
                    + ((MofPackage)pkg.refMetaObject()).getName());
            }
        }
        
        RefClass refClass = pkg.refClass(cls.getName());
        if (refClass == null) {
            throw new IllegalArgumentException(
                "cannot find " + cls.getName() + " cls in " 
                + ((MofPackage)pkg.refMetaObject()).getName());
        }
        
        return refClass;
    }
    
    private static void resetRng()
    {
        rng = new Random(135711);
    }
    
    @Test
    public void test() throws Exception
    {
        URL importSource = 
            BackupRestoreLargeDataSetTest.class.getResource(
                "BackupRestoreLargeDataSet.xmi");
        if (importSource == null) {
            Assert.fail(
                "Execute this class's main() method to generate an XMI large data set XMI file");
        }
        File backup = 
            new File("test/results/BackupRestoreLargeDataSetTest.backup");
        
        mark();
        
        importXmi(importSource);
        
        long importTime = mark();
        
        backup(backup);
        
        long backupTime = mark();
        
        restore(backup);
        
        long restoreTime = mark();
        
        System.out.println("Import:  " + importTime + " ms");
        System.out.println("Backup:  " + backupTime + " ms");
        System.out.println("Restore: " + restoreTime + " ms");
    }
    
    private long mark()
    {
        long now = System.currentTimeMillis();
        
        long delta = now - mark;
        
        mark = now;
        
        return delta;
    }
    
    private static void importXmi(URL xmi) throws Exception
    {
        MDRepository mdrRepos = getRepository();

        XmiReader xmiReader = XMIReaderFactory.getDefault().createXMIReader();
        boolean rollback = false;
        try {
            mdrRepos.beginTrans(true);
            rollback = true;
            
            xmiReader.read(xmi.toString(), getPackage());
            
            rollback = false;
            mdrRepos.endTrans();
        } finally {
            if (rollback) {
                mdrRepos.endTrans(true);
            }
        }
    }
    
    private void backup(File file) throws Exception
    {
        getRepository().endSession();
        getRepository().beginSession();
        
        FileOutputStream output = new FileOutputStream(file);
        
        getRepository().beginTrans(true);
        try {
            getRepository().backupExtent(getTestExtentName(), output);
        } finally {
            getRepository().endTrans(false);
        }
        
        output.flush();
        output.close();
    }
    
    private void restore(File file) throws Exception
    {
        getRepository().endSession();
        getRepository().beginSession();
        
        FileInputStream input = new FileInputStream(file);
        
        try {
            getRepository().beginTrans(true);
            try {
                getRepository().restoreExtent(
                    getTestExtentName(), 
                    "SampleMetamodel", 
                    "EEM", 
                    input);
            } finally {
                getRepository().endTrans(false);
            }
        } finally {
            input.close();
            resetMetaModel();
        }
    }
    
    private static void exportXmi(File xmi) throws Exception
    {
        getRepository().beginTrans(false);
        
        try {
            XmiWriter xmiWriter = 
                XMIWriterFactory.getDefault().createXMIWriter();
            FileOutputStream outStream = new FileOutputStream(xmi);
            try {
                xmiWriter.write(outStream, getPackage(), "1.2");
            } finally {
                outStream.close();
            }
        } finally {
            getRepository().endTrans();
        }
    }
    
    private static class RefObjComparator implements Comparator<RefObject>
    {
        public int compare(RefObject o1, RefObject o2)
        {
            return o1.refMofId().compareTo(o2.refMofId());
        }
    }
}

// End BackupRestoreLargeDataSetTest.java