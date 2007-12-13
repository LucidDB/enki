package org.eigenbase.enki.test;

import eem.*;
import eem.sample.*;

/**
 * SampleModelTestBase provides access to an instance of the Enki sample model
 * via the top-level ({@link EemPackage}.
 *  
 * @author Stephan Zuercher
 */
public abstract class SampleModelTestBase
    extends ModelTestBase
{
    private EemPackage eemPkg;
    
    protected EemPackage getEemPackage()
    {
        if (eemPkg == null) {
            eemPkg = (EemPackage)getPackage();
        }
        
        return eemPkg;
    }
    
    protected SamplePackage getSamplePackage()
    {
        return getEemPackage().getSample();
    }
}

// End SampleModelTestBase.java
