package com.mgreau.jboss.as7.job;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.mgreau.jboss.as7.service.PropertiesHelloWorld;

/**
 * 
 *
 */
public class HelloWorldJob implements Job {
	
	private static final Logger log4jLogger = Logger.getLogger(HelloWorldJob.class);
	
	@Inject
	private PropertiesHelloWorld props;

	public void execute(JobExecutionContext context) throws JobExecutionException {
	   JobDataMap jdMap = context.getJobDetail().getJobDataMap();
       String message = jdMap.get("message").toString();
       if (props == null)
    	   log4jLogger.error("props null");
       //log in the file
       log4jLogger.info(/*props.getProperty("app.message") +*/ " from Quartz Job - " + message);
       //Log + send a email
       log4jLogger.error("Send a mail please");
	}


}
