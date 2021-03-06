/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.translator.loopback;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;
import org.teiid.cdk.api.ConnectorHost;
import org.teiid.cdk.api.TranslationUtility;
import org.teiid.cdk.unittest.FakeTranslationFactory;
import org.teiid.translator.TranslatorException;


public class TestLoopbackExecution  {

    public LoopbackExecutionFactory exampleProperties(int waitTime, int rowCount) {
    	LoopbackExecutionFactory config = new LoopbackExecutionFactory();
    	config.setWaitTime(waitTime);
    	config.setRowCount(rowCount);
        return config;
    }
    
    public void helpTestQuery(String sql, TranslationUtility metadata, Object[][] expectedResults) throws TranslatorException {
        TestHelper.helpTestQuery(false, sql, metadata, 0, 1, expectedResults);
    }

    @Test
    public void testSimple() throws Exception {
        Object[][] results = new Object[][] {
            new Object[] { new Integer(0) }  
        };
        helpTestQuery("SELECT intkey FROM BQT1.SmallA", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), results);     //$NON-NLS-1$
    }
    @Test
    public void testMostTypes() throws Exception {
        Object[][] results = new Object[][] {
            new Object[] { new Integer(0), "ABCDEFGHIJ", new Float(0), new Long(0), new Double(0), new Byte((byte)0), //$NON-NLS-1$
                new Date(0), new Time(0), 
                new Timestamp(0), Boolean.FALSE, 
                new BigInteger("0"), new BigDecimal("0.0"), "ABCDEFGHIJ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                new Short((short)0), new Character('c')
                 }  
        };
        
        helpTestQuery("SELECT intkey, StringKey, floatnum, longnum, doublenum, bytenum, " + //$NON-NLS-1$
            "datevalue, timevalue, timestampvalue, booleanvalue, bigintegervalue, bigdecimalvalue, " + //$NON-NLS-1$
            "objectvalue, shortvalue, charvalue FROM BQT1.SmallA", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), results);      //$NON-NLS-1$
    }
    @Test
    public void testInsert() throws Exception {
        Object[][] results = new Object[][] {
            new Object[] { new Integer(0) }  
        };
        helpTestQuery("INSERT INTO BQT1.SmallA (stringkey) VALUES ('x')", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), results);     //$NON-NLS-1$
    }
    @Test
    public void testUpdate() throws Exception {
        Object[][] results = new Object[][] {
            new Object[] { new Integer(0) }  
        };
        helpTestQuery("UPDATE BQT1.SmallA SET stringkey = 'x'", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), results);     //$NON-NLS-1$
    }
    @Test
    public void testDelete() throws Exception {
        Object[][] results = new Object[][] {
            new Object[] { new Integer(0) }  
        };
        helpTestQuery("DELETE FROM BQT1.SmallA", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), results);     //$NON-NLS-1$
    }
    @Test
    public void testExec() throws Exception {
        Object[][] results = new Object[][] {
            new Object[] { "ABCDEFGHIJ" } //$NON-NLS-1$  
        };
        helpTestQuery("EXEC mmspTest1.MMSP1()", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), results);     //$NON-NLS-1$
    }
    @Test
    public void testExecWithoutResultSet() throws Exception {
    	Object[][] results = new Object[][] {  
        };
    	helpTestQuery("exec pm4.spTest9(1)", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), results); //$NON-NLS-1$
    }
    
    /**
     * wait time is implemented as a random value up to the specified value.  assertions are then not really possible
     * based upon that time.
     */
    public void defer_testWaitTime() throws Exception {
        int waitTime = 100;
        int testCount = 10;
        
        ConnectorHost host = new ConnectorHost(exampleProperties(waitTime, 1), null, FakeTranslationFactory.getInstance().getBQTTranslationUtility());
                
        for(int i=0; i<testCount; i++) {
            long before = System.currentTimeMillis();
            host.executeCommand("SELECT intkey FROM BQT1.SmallA"); //$NON-NLS-1$
            long after = System.currentTimeMillis();
            Assert.assertTrue("Waited too long", (after-before) <= waitTime); //$NON-NLS-1$
        }            
    }
    @Test
    public void testQueryWithLimit() throws Exception {
        Object[][] expected = {{new Integer(0)},
                                {new Integer(0)},
                                {new Integer(0)}};
        TestHelper.helpTestQuery(false, "SELECT intkey FROM BQT1.SmallA LIMIT 3", FakeTranslationFactory.getInstance().getBQTTranslationUtility(), 0, 100, expected); //$NON-NLS-1$
    }
    
}
