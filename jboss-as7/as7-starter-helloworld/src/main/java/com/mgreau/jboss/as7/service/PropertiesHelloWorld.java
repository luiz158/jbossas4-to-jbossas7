package com.mgreau.jboss.as7.service;

import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.log4j.Logger;

@Startup
@Singleton
public class PropertiesHelloWorld {

	private static final Logger sLog = Logger
			.getLogger(PropertiesHelloWorld.class);

	@Resource(name = "appPropertyFile")
	private String appPropertyFile;

	private Properties props;

	@PostConstruct
	private void startup() {
		sLog.info("Load the conf file : " + appPropertyFile);

		try {

			InputStream propsStream = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream(appPropertyFile);
			
			if (propsStream == null){
				sLog.error("File not found : " + appPropertyFile);
			}
			
			props = new Properties();
			props.load(propsStream);

		} catch (Exception e) {
			throw new EJBException("PropertiesHelloWorld initialization error",
					e);
		}
	}

	public String getProperty(String name) {
		return props.getProperty(name);
	}
}
