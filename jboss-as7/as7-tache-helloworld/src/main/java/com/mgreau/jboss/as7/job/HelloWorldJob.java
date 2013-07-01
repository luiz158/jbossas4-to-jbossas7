package com.mgreau.jboss.as7.job;

import java.util.Date;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class HelloWorldJob implements Job {
	
	 private static final Logger log4jLogger = Logger.getLogger(HelloWorldJob.class);
	
	private String message;

	public void execute(JobExecutionContext context) throws JobExecutionException {
	    // Say Hello to the World and display the date/time
	   System.out.println("Hello World! - " + new Date());
	   JobDataMap jdMap = context.getJobDetail().getJobDataMap();
       String message = jdMap.get("message").toString();
       System.out.println("Message - " + message);
       log4jLogger.info("Message - " + message);
       log4jLogger.error("send a mail please");
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	

}
