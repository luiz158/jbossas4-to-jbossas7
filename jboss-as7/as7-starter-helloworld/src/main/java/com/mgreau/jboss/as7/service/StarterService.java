package com.mgreau.jboss.as7.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;


@Startup
@Singleton
public class StarterService {

	private static final Logger log4jLogger = Logger
			.getLogger(StarterService.class);

	private StdSchedulerFactory sf = new StdSchedulerFactory();
	
	@Inject
	private DemarrageConso conso;
	
	
	@PostConstruct
	public void onInitialize() throws Exception {
		log4jLogger.info("Start Service HELLO WORLD APP");
		startJob();
		conso.startService();
	}

	@PreDestroy
	public void onDestroy() throws Exception {
		log4jLogger.info("shutdown Service HELLO WORLD APP");
		sf.getScheduler().shutdown();
	}

	private void startJob() throws Exception {
		sf.initialize("quartz-helloworld.properties");
		Scheduler sched = sf.getScheduler();

		sched.start();

	}

}
