/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dtstack.logstash.http.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtstack.logstash.distributed.ZkDistributed;
import com.dtstack.logstash.http.common.HttpCommon;
import com.dtstack.logstash.http.common.Urls;
import com.sun.net.httpserver.HttpServer;

/**
 * 
 * Reason: TODO ADD REASON(可选)
 * Date: 2016年12月30日 下午1:16:37
 * Company: www.dtstack.com
 * @author sishu.yss
 *
 */
@SuppressWarnings("restriction")
public class LogstashHttpServer {
	
	private static final Logger logger = LoggerFactory.getLogger(LogstashHttpServer.class);

	private String host="0.0.0.0";
	
	private int port;
	
	private ZkDistributed zkDistributed;
	
	private HttpServer server;
	
	public LogstashHttpServer(ZkDistributed zkDistributed) throws Exception{
		this.zkDistributed = zkDistributed;
		this.port = (int) HttpCommon.getUrlPort(zkDistributed.getLocalAddress())[1];
		init();
	}
	
	private void init() throws Exception{
		this.server = HttpServer.create(new InetSocketAddress(InetAddress.getByAddress(host.getBytes()),port), 0);
		server.setExecutor(null);
		setHandler();
		this.server.start();
		logger.warn("LogstashHttpServer start at:{}",String.valueOf(port));
	}
	
	private void setHandler(){
		this.server.createContext(Urls.LOADNODEDATA,new ImmediatelyLoadNodeDataHandler(this.zkDistributed));
	}

}