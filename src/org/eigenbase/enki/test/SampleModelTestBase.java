package org.eigenbase.enki.test;

import eem.*;
import eem.sample.*;
import eem.sample.simple.*;
import eem.sample.special.*;

/**
 * SampleModelTestBase provides access to an instance of the Enki sample model
 * via the top-level ({@link EemPackage}.
 *  
 * @author Stephan Zuercher
 */
public abstract class SampleModelTestBase
    extends ModelTestBase
{
    private static EemPackage eemPkg;
    
    protected static EemPackage getEemPackage()
    {
        if (eemPkg == null) {
            eemPkg = (EemPackage)getPackage();
        }
        
        return eemPkg;
    }
    
    protected static SamplePackage getSamplePackage()
    {
        return getEemPackage().getSample();
    }
    
    protected static SimplePackage getSimplePackage()
    {
        return getSamplePackage().getSimple();
    }

    protected static SpecialPackage getSpecialPackage()
    {
        return getSamplePackage().getSpecial();
    }
}

// End SampleModelTestBase.java
