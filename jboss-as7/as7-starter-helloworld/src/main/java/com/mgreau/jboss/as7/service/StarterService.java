package com.mgreau.jboss.as7.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

@Startup
@Singleton
@DependsOn("PropertiesHelloWorld")
public class StarterService {

	/** Logger Log4j */
	private static final Logger log4jLogger = Logger
			.getLogger(StarterService.class);

	@Inject
	private PropertiesHelloWorld props;
	
	@Resource(name="quartzPropertyFile")
	private String quartzPropertyFile;
	
	/** Quartz Scheduler factory */
	private StdSchedulerFactory sf;

	@PostConstruct
	public void onInitialize() throws Exception {
		log4jLogger.info("Start Service HELLO WORLD APP");
		log4jLogger.info("Env : " + props.getProperty("app.env"));
		startJob();
	}

	@PreDestroy
	public void onDestroy() throws Exception {
		log4jLogger.info("shutdown Service HELLO WORLD APP");
		sf.getScheduler().shutdown();
	}

	/**
	 * Start Quartz Scheduler
	 * @throws Exception
	 */
	private void startJob() throws Exception {
		sf = new StdSchedulerFactory();
		log4jLogger.info("Quartz file : " + quartzPropertyFile);
		sf.initialize(quartzPropertyFile);
		Scheduler sched = sf.getScheduler();
		sched.start();
	}
}
