jbossas4-to-jbossas7
=====================

How to migrate application (EAR, WAR,SAR,JAX-WS, Log4j, Quartz...) from JBoss AS 4.2.3.GA to JBoss AS7.2 (EAP6.1)

1- Update org.jboss.log4.log4manager module :
 add a dependency to javax.mail.api in order to make SMTP appender works

2- Start JBoss AS 7.2

3- mvn clean package jboss-as:deploy 
