package com.mgreau.jboss.as7.job;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 *
 */
public class HelloWorldJob implements Job {
	
	private static final Logger log4jLogger = Logger.getLogger(HelloWorldJob.class);
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
	   JobDataMap jdMap = context.getJobDetail().getJobDataMap();
       String message = jdMap.get("message").toString();
       //log in the file
       log4jLogger.info( " from Quartz Job - " + message);
       //Log + send a email
       log4jLogger.error("Send a mail please");
	}


}
