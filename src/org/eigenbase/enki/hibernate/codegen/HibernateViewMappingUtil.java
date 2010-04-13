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
package org.eigenbase.enki.hibernate.codegen;

import java.util.*;

import javax.jmi.model.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.util.*;
import org.hibernate.dialect.*;

/**
 * HibernateViewMappingUtil generates all-of-type and all-of-class views
 * for MOF Classifier objects.
 * 
 * <p>HibernateViewMappingUtil supports the generation of views for the 
 * following Hibernate SQL dialects:
 * <ul>
 * <li>MySQL (MyISAM and InnoDB)</li>
 * <li>HSQLDB (<b>WARNING: untested</b>)</li>
 * <li>PostgreSQL (<b>WARNING: untested</b>)</li>
 * </ul>
 * 
 * To add a new dialect, modify {@link #getMofIdConversion(String, Dialect)}
 * to support conversion from the integer MOF ID stored in the database to
 * a MOF ID string.  The result must be equivalent to calling 
 * {@link MofIdUtil#makeMofIdStr(long)} in Java.  Note that the dialect set
 * is divided in groups and only the exemplar (first element in each group) is
 * used to generate SQL.  If you add your Dialect to an existing group, the 
 * output of {@link #getMofIdConversion(String, Dialect)} for the exemplar
 * must be valid for your dialect. 
 * 
 * @author Stephan Zuercher
 */
public class HibernateViewMappingUtil
{
    private static final String MOF_CLASSNAME_COLUMN_NAME = "mofClassName";

    private final CodeGenXmlOutput output;
    private final Dialect[][] dialectSet;
    private final String tablePrefix;
    private HashMultiMap<Classifier, Classifier> subtypesMap;
    
    public HibernateViewMappingUtil(
        CodeGenXmlOutput output, 
        Dialect[][] dialectSet,
        String tablePrefix)
    {
        this.output = output;
        this.dialectSet = dialectSet;
        this.tablePrefix = tablePrefix;
        this.subtypesMap = new HashMultiMap<Classifier, Classifier>(true);
    }
    
    public void generateViews(
        Collection<Classifier> types)
    throws GenerationException
    {        
        for(Classifier cls: types) {
            subtypesMap.put(cls, cls);
            for(Object o: cls.allSupertypes()) {
                subtypesMap.put((Classifier)o, cls);
            }
        }
        
        for(Classifier cls: types) {
            generateClassView(cls);
        }
        for(Classifier cls: types) {
            generateTypeView(cls);
        }
    }
    private void generateClassView(Classifier cls) throws GenerationException
    {
        if (cls.isAbstract()) {
            return;
        }
        
        for(Dialect[] dialects: dialectSet) {
            Dialect exemplar = dialects[0];
            
            String tableName = 
                HibernateMappingHandler.computeBaseTableName(cls);
            String viewName = viewName(tableName, exemplar, true);

            Collection<Attribute> instanceAttributes =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    VisibilityKindEnum.PUBLIC_VIS,
                    ScopeKindEnum.INSTANCE_LEVEL,
                    Attribute.class);
            Collection<Reference> instanceReferences =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    VisibilityKindEnum.PUBLIC_VIS,
                    Reference.class);

            output.startElem("database-object");
            output.startElem("create");
            output.startCData();
            output.writeln("create view ", viewName, "(");
            output.increaseIndent();
            generateColumnNameList(
                cls,
                instanceAttributes,
                instanceReferences,
                exemplar);
            output.decreaseIndent();
            output.writeln(") as");
            output.increaseIndent();

            generateClassViewSubquery(
                cls, 
                instanceAttributes,
                instanceReferences,
                exemplar);

            output.decreaseIndent();
            output.endCData();            
            output.endElem("create");
    
            output.startElem("drop");
            output.writeCData("drop view ", viewName);
            output.endElem("drop");
            
            for(Dialect dialect: dialects) {
                output.writeEmptyElem(
                    "dialect-scope", 
                    "name", dialect.getClass().getName());
            }
            output.endElem("database-object");
        }
    }
    
    private void generateColumnNameList(
        Classifier cls,
        Collection<Attribute> attribs, 
        Collection<Reference> refs, 
        Dialect dialect)
    throws GenerationException
    {
        output.writeln(
            HibernateDialectUtil.quote(
                dialect, HibernateMappingHandler.MOF_ID_COLUMN_NAME), ", ");
        output.write(
            HibernateDialectUtil.quote(dialect, MOF_CLASSNAME_COLUMN_NAME));
        
        List<ReferenceInfo> refInfos = new ArrayList<ReferenceInfo>();
        for(Attribute attrib: attribs) {
            if (attrib.isDerived()) {
                continue;
            }
            
            HibernateMappingHandler.MappingType mappingType = 
                HibernateMappingHandler.getMappingType(
                    attrib.getType(), attrib.getMultiplicity());

            switch(mappingType) {
            case COLLECTION:
            case LIST:
                // Ignored
                continue;
            
            case CLASS:
                refInfos.add(
                    new ComponentInfo((MofClass)cls, attrib, true));
                continue;
                
            case BOOLEAN:
            case STRING:
            case ENUMERATION:
            case OTHER_DATA_TYPE:
                break;
                    
            default:
                throw new GenerationException(
                    "unknown mapping type: " +  mappingType);
            }
            
            // Use the plain field name to mimic behavior required by Farrago.
            String aliasName = attrib.getName();

            output.writeln(",");
            output.write(HibernateDialectUtil.quote(dialect, aliasName));
        }
        for(Reference ref: refs) {
            ReferenceInfo refInfo = new ReferenceInfoImpl(ref);
            refInfos.add(refInfo);
        }

        for(ReferenceInfo refInfo: refInfos) {
            if (refInfo.isSingle()) {
                String aliasName;
                if (refInfo.getReference() != null) {
                    aliasName = 
                        refInfo.getReference().getReferencedEnd().getName();
                } else {
                    Attribute attrib = 
                        ((ComponentInfo)refInfo).getOwnerAttribute();
                    aliasName = attrib.getName();

                    int pos = aliasName.indexOf('$');
                    if (pos > 0) {
                        aliasName = aliasName.substring(0, pos);
                    }
                }
                
                output.writeln(",");
                output.write(HibernateDialectUtil.quote(dialect, aliasName));
            }
        }

        output.writeln();
    }
    
    private void generateClassViewSubquery(
        Classifier cls, 
        Collection<Attribute> attribs, 
        Collection<Reference> refs, 
        Dialect dialect)
    throws GenerationException
    {
        String tableName = 
            HibernateMappingHandler.computeBaseTableName(cls);

        List<ReferenceInfo> refInfos = new ArrayList<ReferenceInfo>();
        
        String quotedMofId = 
            HibernateDialectUtil.quote(
                dialect, HibernateMappingHandler.MOF_ID_COLUMN_NAME);
        
        output.writeln("select");
        output.increaseIndent();
        output.writeln(
            getMofIdConversion("t." + quotedMofId, dialect),
            " as ", quotedMofId,
            ",");
        output.write(
            "'", cls.getName(), "' as ", 
            HibernateDialectUtil.quote(dialect, MOF_CLASSNAME_COLUMN_NAME));
        for(Attribute attrib: attribs) {
            if (attrib.isDerived()) {
                continue;
            }
            
            HibernateMappingHandler.MappingType mappingType = 
                HibernateMappingHandler.getMappingType(
                    attrib.getType(), attrib.getMultiplicity());

            switch(mappingType) {
            case COLLECTION:
            case LIST:
                // Ignored
                continue;
            
            case CLASS:
                refInfos.add(
                    new ComponentInfo((MofClass)cls, attrib, true));
                continue;
                
            case BOOLEAN:
            case STRING:
            case ENUMERATION:
            case OTHER_DATA_TYPE:
                break;
                    
            default:
                throw new GenerationException(
                    "unknown mapping type: " +  mappingType);
            }
            
            // Use the plain field name to mimic behavior required by Farrago.
            String aliasName = attrib.getName();
            String fieldName = CodeGenUtils.getClassFieldName(aliasName);

            output.writeln(",");
            output.write("t.", HibernateDialectUtil.quote(dialect, fieldName));
            if (!fieldName.equals(aliasName)) {
                output.write(
                    " as ", HibernateDialectUtil.quote(dialect, aliasName));
            }
        }
        for(Reference ref: refs) {
            ReferenceInfo refInfo = new ReferenceInfoImpl(ref);
            refInfos.add(refInfo);
        }

        List<Join> joins = new ArrayList<Join>(); 
        
        for(ReferenceInfo refInfo: refInfos) {
            if (refInfo.isSingle()) {
                String alias = "a" + (joins.size() + 1);
                String table;
                String column;
                
                switch(refInfo.getKind()) {
                case ONE_TO_ONE:
                    table = HibernateMappingHandler.ASSOC_ONE_TO_ONE_LAZY_TABLE;
                    column = 
                        refInfo.isReferencedEndFirst()
                            ? HibernateMappingHandler.ASSOC_ONE_TO_ONE_PARENT_ID_COLUMN
                            : HibernateMappingHandler.ASSOC_ONE_TO_ONE_CHILD_ID_COLUMN;
                    break;
                    
                case ONE_TO_MANY:
                    boolean isOrdered = 
                        refInfo.isOrdered(0) || refInfo.isOrdered(1);
                    if (isOrdered) {
                        table = 
                            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_ORDERED_TABLE;
                    } else if (HibernateJavaHandler.isHighCardinalityAssociation(
                                   refInfo)) {
                        table =
                            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_HC_TABLE;
                    } else {
                        table = 
                            HibernateMappingHandler.ASSOC_ONE_TO_MANY_LAZY_TABLE;
                    }
                    column = HibernateMappingHandler.ASSOC_ONE_TO_MANY_PARENT_ID_COLUMN;
                    break;
                    
                case MANY_TO_MANY:
                    throw new GenerationException(
                        "many-to-many assoc with single end");
                    
                default:
                    throw new GenerationException(
                        "unknown association kind: " + refInfo.getKind());
                }
                
                String fieldName = refInfo.getFieldName();
                Join join = new Join(table, alias, fieldName);
                joins.add(join);
                
                String aliasName;
                if (refInfo.getReference() != null) {
                    aliasName = 
                        refInfo.getReference().getReferencedEnd().getName();
                } else {
                    Attribute attrib = 
                        ((ComponentInfo)refInfo).getOwnerAttribute();
                    aliasName = attrib.getName();

                    int pos = aliasName.indexOf('$');
                    if (pos > 0) {
                        aliasName = aliasName.substring(0, pos);
                    }
                }
                
                output.writeln(",");
                output.write(
                    getMofIdConversion(
                        alias
                        + "."
                        + HibernateDialectUtil.quote(dialect, column), 
                        dialect), 
                    " as ", HibernateDialectUtil.quote(dialect, aliasName));
            }
        }
        
        output.writeln();
        output.decreaseIndent();
        output.writeln("from ", tableName(tableName, dialect), " as t");
        output.increaseIndent();
        for(Join join: joins) {
            output.writeln(
                "left outer join ", tableName(join.table, dialect), 
                " as ", join.alias);
            output.increaseIndent();
            output.writeln(
                " on t.", 
                HibernateDialectUtil.quote(dialect, join.column),
                " = ", 
                join.alias, ".", quotedMofId);
            output.decreaseIndent();
        }
        output.decreaseIndent();
    }

    private String getMofIdConversion(String mofIdColumn, Dialect dialect)
        throws GenerationException
    {
        StringBuilder b = new StringBuilder();
        if (dialect instanceof MySQLDialect) {
            // e.g., concat('j:', lpad(hex(c), 16, '0'))
            b
                .append("concat('j:', lpad(hex(")
                .append(mofIdColumn)
                .append("), 16 ,'0'))");
            return b.toString();
        } else if (dialect instanceof HSQLDialect) {
            // REVIEW: SWZ: 2008-08-28: This is untested.
            // e.g., "org.eigenbase.enki.util.MofIdUtil.makeMofIdStr"(c)
            b
                .append('"')
                .append(MofIdUtil.class.getName())
                .append('.')
                .append("makeMofIdStr")
                .append("\"(")
                .append(mofIdColumn)
                .append(")");
            return b.toString();
        } else if (dialect instanceof PostgreSQLDialect) {
            // REVIEW: SWZ: 2008-08-28: This is untested.
            // e.g., 'j:' || lpad(upper(to_hex(c)), 16, '0')
            b
                .append("'j:' || lpad(upper(to_hex(")
                .append(mofIdColumn)
                .append(")), 16, '0')");
        } else {
            throw new GenerationException(
                "MOFID format conversion not support for " 
                + dialect.getClass().getSimpleName());
        }
        
        return b.toString();
    }
    
    private void generateTypeView(Classifier cls) throws GenerationException
    {
        for(Dialect[] dialects: dialectSet) {
            Dialect exemplar = dialects[0];
            
            String tableName = 
                HibernateMappingHandler.computeBaseTableName(cls);
            String viewName = viewName(tableName, exemplar, false);

            Collection<Classifier> subtypes = subtypesMap.getValues(cls);

            Collection<Attribute> instanceAttributes =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    VisibilityKindEnum.PUBLIC_VIS,
                    ScopeKindEnum.INSTANCE_LEVEL,
                    Attribute.class);
            Collection<Reference> instanceReferences =
                CodeGenUtils.contentsOfType(
                    cls,
                    HierachySearchKindEnum.INCLUDE_SUPERTYPES, 
                    VisibilityKindEnum.PUBLIC_VIS,
                    Reference.class);

            output.startElem("database-object");
            output.startElem("create");
            output.startCData();
            output.writeln("create view ", viewName, "(");
            output.increaseIndent();
            generateColumnNameList(
                cls, 
                instanceAttributes,
                instanceReferences,
                exemplar);
            output.decreaseIndent();
            output.writeln(") AS");
            output.increaseIndent();

            boolean first = true;
            for(Classifier subtype: subtypes) {
                if (subtype.isAbstract()) {
                    continue;
                }
                
                if (first) {
                    first = false;
                } else {
                    output.writeln("union all");
                }
                generateTypeViewSubquery(
                    subtype, 
                    instanceAttributes,
                    instanceReferences,
                    exemplar);
            }

            if (first) {
                // no non-abstract subtypes
                throw new GenerationException(
                    "cannot handle view creation on abstract types with no concrete subtypes: " + cls.getName());
            }
            
            output.decreaseIndent();
            output.endCData();            
            output.endElem("create");
    
            output.startElem("drop");
            output.writeCData("drop view ", viewName);
            output.endElem("drop");
            
            for(Dialect dialect: dialects) {
                output.writeEmptyElem(
                    "dialect-scope", 
                    "name", dialect.getClass().getName());
            }
            output.endElem("database-object");
        }
    }
    
    private void generateTypeViewSubquery(
        Classifier subtype, 
        Collection<Attribute> attribs, 
        Collection<Reference> refs, 
        Dialect dialect)
    throws GenerationException
    {
        String tableName = 
            HibernateMappingHandler.computeBaseTableName(subtype);

        List<ReferenceInfo> refInfos = new ArrayList<ReferenceInfo>();
        
        output.writeln("select");
        output.increaseIndent();
        output.writeln(
            HibernateDialectUtil.quote(
                dialect, HibernateMappingHandler.MOF_ID_COLUMN_NAME), ",");
        output.write(
            HibernateDialectUtil.quote(dialect, MOF_CLASSNAME_COLUMN_NAME));
        for(Attribute attrib: attribs) {
            if (attrib.isDerived()) {
                continue;
            }
            
            HibernateMappingHandler.MappingType mappingType = 
                HibernateMappingHandler.getMappingType(
                    attrib.getType(), attrib.getMultiplicity());

            switch(mappingType) {
            case COLLECTION:
            case LIST:
                // Ignored
                continue;
            
            case CLASS:
                refInfos.add(
                    new ComponentInfo((MofClass)subtype, attrib, true));
                continue;
                
            case BOOLEAN:
            case STRING:
            case ENUMERATION:
            case OTHER_DATA_TYPE:
                String fieldName = attrib.getName();

                output.writeln(",");
                output.write(HibernateDialectUtil.quote(dialect, fieldName));
                break;
                    
            default:
                throw new GenerationException(
                    "unknown mapping type: " +  mappingType);
            }
        }
        for(Reference ref: refs) {
            ReferenceInfo refInfo = new ReferenceInfoImpl(ref);
            refInfos.add(refInfo);
        }
        
        for(ReferenceInfo refInfo: refInfos) {
            if (refInfo.isSingle()) {
                String fieldName;
                if (refInfo.getReference() != null) {
                    fieldName = 
                        refInfo.getReference().getReferencedEnd().getName();
                } else {
                    fieldName = 
                        ((ComponentInfo)refInfo).getOwnerAttribute().getName();
                    int pos = fieldName.indexOf('$');
                    if (pos > 0) {
                        fieldName = fieldName.substring(0, pos);
                    }
                }
                
                output.writeln(",");
                output.write(HibernateDialectUtil.quote(dialect, fieldName));
            }
        }
        
        output.writeln();
        output.decreaseIndent();
        output.writeln("from ", viewName(tableName, dialect, true));
    }    
    
    private String tableName(String tableName, Dialect dialect)
    {
        if (tablePrefix != null) {
            tableName = tablePrefix + tableName;
        }
        
        return HibernateDialectUtil.quote(dialect, tableName);
    }
    
    private String viewName(
        String tableName, Dialect dialect, boolean isClassView)
    {
        String prefix = isClassView ? "VC_" : "VT_";

        tableName = prefix + tableName;
        if (tablePrefix != null) {
            tableName = tablePrefix + tableName;
        }
        
        return HibernateDialectUtil.quote(dialect, tableName);
    }
    
    private final class Join
    {
        final String table;
        final String alias;
        final String column;

        Join(String table, String alias, String column)
        {
            this.table = table;
            this.alias = alias;
            this.column = column;
        }
    }
}

// End HibernateViewMappingUtil.java
