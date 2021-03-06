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

package org.teiid.query.eval;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.teiid.adminapi.Model.Type;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.common.buffer.BufferManagerFactory;
import org.teiid.jdbc.FakeServer;
import org.teiid.jdbc.HardCodedExecutionFactory;
import org.teiid.jdbc.TeiidDriver;
import org.teiid.language.QueryExpression;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.transport.SSLConfiguration;
import org.teiid.transport.SocketConfiguration;
import org.teiid.transport.SocketListener;

@SuppressWarnings("nls")
public class TestJDBCSocketPerformance {
	
	static InetSocketAddress addr;
	static SocketListener jdbcTransport;
	static FakeServer server;
	
	@BeforeClass public static void oneTimeSetup() throws Exception {
		SocketConfiguration config = new SocketConfiguration();
		config.setSSLConfiguration(new SSLConfiguration());
		addr = new InetSocketAddress(0);
		config.setBindAddress(addr.getHostName());
		config.setPortNumber(0);
		
		EmbeddedConfiguration dqpConfig = new EmbeddedConfiguration();
		server = new FakeServer(false);
		server.start(dqpConfig);
		ModelMetaData mmd = new ModelMetaData();
		mmd.setName("x");
		mmd.setModelType(Type.PHYSICAL);
		mmd.addSourceMapping("x", "hc", null);
		mmd.setSchemaSourceType("ddl");
		StringBuffer ddl = new StringBuffer("create foreign table x (col0 string");
		for (int i = 1; i < 10; i++) {
			ddl.append(",").append(" col").append(i).append(" string");
		}
		ddl.append(");");
		mmd.setSchemaText(ddl.toString());
		server.addTranslator("hc", new HardCodedExecutionFactory() {
			@Override
			protected List<? extends List<?>> getData(QueryExpression command) {
				List<List<String>> result = new ArrayList<List<String>>();
				int size = command.getProjectedQuery().getDerivedColumns().size();
				for (int i = 0; i < 64; i++) {
					List<String> row = new ArrayList<String>(size);
					for (int j = 0; j < size; j++) {
						row.add("abcdefghi" + j);
					}
					result.add(row);
				}
				return result;
			}
		});
		server.deployVDB("x", mmd);
		
		jdbcTransport = new SocketListener(addr, config, server.getClientServiceRegistry(), BufferManagerFactory.getStandaloneBufferManager());
	}
	
	@AfterClass public static void oneTimeTearDown() throws Exception {
		if (jdbcTransport != null) {
			jdbcTransport.stop();
		}
		server.stop();
	}
	
	@Test public void testLargeSelects() throws Exception {
		Properties p = new Properties();
		p.setProperty("user", "testuser");
		p.setProperty("password", "testpassword");
		Connection conn = TeiidDriver.getInstance().connect("jdbc:teiid:x@mm://"+addr.getHostName()+":" +jdbcTransport.getPort(), p);
		long start = System.currentTimeMillis();
		for (int j = 0; j < 10; j++) {
			Statement s = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			assertTrue(s.execute("select * from x as x1, x as x2, x as x3"));
			ResultSet rs = s.getResultSet();
			int i = 0;
			while (rs.next()) {
				i++;
			}
			s.close();
		}
		System.out.println((System.currentTimeMillis() - start));
	}

}
