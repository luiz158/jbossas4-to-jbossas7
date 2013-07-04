jbossas4-to-jbossas7
=====================

How to migrate application (EAR, WAR,SAR, EJB/JAX-WS, Log4j, Quartz...) from JBoss AS 4.2.3.GA to JBoss AS7.2 (EAP6.1)

APP IN JBOSS AS 7.2
--------------------
+ Update $JBOSS_HOME/modules/system/layers/base/org/jboss/log4j/logmanager module and add a dependency to javax.mail.api in order to make SMTP appender works 
&lt;module name="javax.mail.api" services="import"/&gt;
+ Start JBoss AS 7.2
+ $JBOSS_HOME/bin/jboss-cli.sh
+ add com.mgreau.jboss.as7.helloworld module present in jboss-as7/as7-ear-helloworld/src/main/modules
+ cd jboss-as7 && mvn clean package jboss-as:deploy 
