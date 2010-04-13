/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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
package org.eigenbase.enki.codegen;

import java.text.*;
import java.util.*;

/**
 * MofImplementationGenerator outputs a basic implementation of the MOF M3
 * model.  This generator was useful for creating much of the 
 * {@link org.eigenbase.enki.jmi} package.
 * 
 * @author Stephan Zuercher
 */
public class MofImplementationGenerator
    extends MdrGenerator
{
    // Note: extra concatenation prevents P4 from modifying this id keyword
    private static final String COMMON_HEADER = 
        "$" + "Id: $\n" +
        "Enki generates and implements the JMI and MDR APIs for MOF metamodels.\n" +
        "Copyright (C) 2007-@YEAR@ The Eigenbase Project\n" +
        "Copyright (C) 2007-@YEAR@ SQLstream, Inc.\n" +
        "Copyright (C) 2007-@YEAR@ Dynamo BI Corporation\n" +
        "\n" +
        "This library is free software; you can redistribute it and/or modify it\n" +
        "under the terms of the GNU Lesser General Public License as published by\n" +
        "the Free Software Foundation; either version 2.1 of the License, or (at\n" +
        "your option) any later version.\n" +
        "\n" +
        "This library is distributed in the hope that it will be useful,\n" +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
        "GNU Lesser General Public License for more details.\n" +
        "\n" +
        "You should have received a copy of the GNU Lesser General Public\n" +
        "License along with this library; if not, write to the Free Software\n" +
        "Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA";
    
    public MofImplementationGenerator()
    {
        super();
    }
    
    @Override
    protected void configureHandlers()
    {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        String header = COMMON_HEADER.replaceAll("@YEAR@", sdf.format(now));
        
        MofImplementationHandler mofHandler = new MofImplementationHandler();
        mofHandler.setCommonHeader(header);   
        addHandler(mofHandler);
        
        MofInitHandler initHandler = new MofInitHandler();
        initHandler.setCommonHeader(header);
        addHandler(initHandler);
    }

    public static void main(String[] args)
    {
        MofImplementationGenerator g = new MofImplementationGenerator();
        
        g.doMain(args, false);
    }
}

// End MofImplementationGenerator.java
