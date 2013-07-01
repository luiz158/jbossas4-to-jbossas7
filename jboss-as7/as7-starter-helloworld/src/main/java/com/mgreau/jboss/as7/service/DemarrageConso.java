package com.mgreau.jboss.as7.service;

import org.apache.log4j.Logger;

public class DemarrageConso {
	
	public void DemarrageConso() {
		
	}
	
	private static final String FICHIER_CONFIG = "conso-conso.hibernate.cfg.xml";
	private static final String FICH_MAPPING = "conso-conso.hibernate.mapping.xml";
	
	private static final String EAR_NAME = "conso-ear-batch_conso";

	/** le logger */
	private static final Logger sLog = Logger.getLogger(DemarrageConso.class);

	public void startService()  {
		sLog.info("Conso : Demarrage du noyau conso");
		
	}
}
