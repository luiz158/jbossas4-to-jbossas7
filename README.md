jbossas4-to-jbossas7
=====================

How to migrate application (EAR, WAR,SAR, EJB/JAX-WS, Log4j, Quartz...) from JBoss AS 4.2.3.GA to JBoss AS7.2 (EAP6.1)

APP IN JBOSS AS 7.2
--------------------
+ Update $JBOSS_HOME/modules/system/layers/base/org/jboss/log4j/logmanager module and add a dependency to javax.mail.api in order to make SMTP appender works 
&lt;module name="javax.mail.api" services="import"/&gt;
+ Start JBoss AS 7.2 in standalone mode

> $JBOSS_HOME/bin/standalone.sh

+ Add com.mgreau.jboss.as7.helloworld module (${module.dir} = $PROJECT_HOME/jbossas4-to-jbossas7/jboss-as7/as7-ear-helloworld/src/main/modules/helloworld/)

> $JBOSS_HOME/bin/jboss-cli.sh --connect

> module add --name=com.mgreau.jboss.as7.helloworld --module-xml=${module.dir}/module.xml --resources=${module.dir}/application-helloworld.properties:${module.dir}/jobs-helloworld.xml:${module.dir}/quartz-helloworld.properties

+ Build and deploy the HelloWorld EAR App

> cd jboss-as7 && mvn clean package jboss-as:deploy 
