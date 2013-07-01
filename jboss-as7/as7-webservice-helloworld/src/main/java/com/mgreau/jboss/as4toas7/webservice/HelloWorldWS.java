package com.mgreau.jboss.as4toas7.webservice;

import javax.ejb.Stateless;
import javax.jws.WebService;

import org.apache.log4j.Logger;


@Stateless
@WebService
@WebContext(contextRoot="/helloworld-ws")
public class HelloWorldWS {

	/** Logger de l'application */
	protected static final Logger sLog = Logger.getLogger(HelloWorldWS.class);

	/** {@inheritDoc} */
	public String sayHello(final String pName) {
		sLog.info("Hello from WS : " + pName);
		return "Hello from WS : " + pName;
	}

	

}
