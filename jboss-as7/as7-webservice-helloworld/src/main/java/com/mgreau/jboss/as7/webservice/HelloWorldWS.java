package com.mgreau.jboss.as7.webservice;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jws.WebService;

import org.apache.log4j.Logger;
import org.jboss.ws.api.annotation.WebContext;

import com.mgreau.jboss.as7.service.PropertiesHelloWorld;


@Stateless
@WebService
@WebContext(contextRoot="/helloworld-ws")
public class HelloWorldWS {

	/** Logger de l'application */
	protected static final Logger sLog = Logger.getLogger(HelloWorldWS.class);
	
	@Inject
	private PropertiesHelloWorld props;
	
	/**
	 * Web Method
	 * @param pName
	 * @return Hello World message 
	 */
	public String sayHello(final String pName) {
		sLog.info(props.getProperty("app.message") + " from WS : " + pName);
		return props.getProperty("app.message") + " from WS (EJB 3.1 : JAX-WS) : " + pName;
	}

	

}
