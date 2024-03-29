/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.mgreau.jboss.as7.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.VersionHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class CliLauncher {

    public static void main(String[] args) throws Exception {
        int exitCode = 0;
        CommandContext cmdCtx = null;
        boolean gui = false;
        String appName = "";
        try {
            String argError = null;
            List<String> commands = null;
            File file = null;
            boolean connect = false;
            String defaultControllerProtocol = "http-remoting";
            String defaultControllerHost = null;
            int defaultControllerPort = -1;
            boolean version = false;
            String username = null;
            char[] password = null;
            int connectionTimeout = -1;
            
            //App deployment
            boolean isAppDeployment = false;
            final Properties props = new Properties();

            for(String arg : args) {
                if(arg.startsWith("--controller=") || arg.startsWith("controller=")) {
                    final String fullValue;
                    final String value;
                    if(arg.startsWith("--")) {
                        fullValue = arg.substring(13);
                    } else {
                        fullValue = arg.substring(11);
                    }
                    final int protocolEnd = fullValue.lastIndexOf("://");
                    if (protocolEnd == -1) {
                        value = fullValue;
                    } else {
                        value = fullValue.substring(protocolEnd + 3);
                        defaultControllerProtocol = fullValue.substring(0, protocolEnd);
                    }

                    String portStr = null;
                    int colonIndex = value.lastIndexOf(':');
                    if(colonIndex < 0) {
                        // default port
                        defaultControllerHost = value;
                    } else if(colonIndex == 0) {
                        // default host
                        portStr = value.substring(1);
                    } else {
                        final boolean hasPort;
                        int closeBracket = value.lastIndexOf(']');
                        if (closeBracket != -1) {
                            //possible ip v6
                            if (closeBracket > colonIndex) {
                                hasPort = false;
                            } else {
                                hasPort = true;
                            }
                        } else {
                            //probably ip v4
                            hasPort = true;
                        }
                        if (hasPort) {
                            defaultControllerHost = value.substring(0, colonIndex).trim();
                            portStr = value.substring(colonIndex + 1).trim();
                        } else {
                            defaultControllerHost = value;
                        }
                    }

                    if(portStr != null) {
                        int port = -1;
                        try {
                            port = Integer.parseInt(portStr);
                            if(port < 0) {
                                argError = "The port must be a valid non-negative integer: '" + args + "'";
                            } else {
                                defaultControllerPort = port;
                            }
                        } catch(NumberFormatException e) {
                            argError = "The port must be a valid non-negative integer: '" + arg + "'";
                        }
                    }
                } else if("--connect".equals(arg) || "-c".equals(arg)) {
                    connect = true;
                } else if("--version".equals(arg)) {
                    version = true;
                } else if ("--gui".equals(arg)) {
                    gui = true;
                } else if (arg.startsWith("--appDeployment=")  || arg.startsWith("appDeployment=")) {
                    isAppDeployment = true;
                    appName = arg.startsWith("--") ? arg.substring(16) : arg.substring(14);
                } else if(arg.startsWith("--file=") || arg.startsWith("file=")) {
                    if(file != null) {
                        argError = "Duplicate argument '--file'.";
                        break;
                    }
                    if(commands != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time.";
                        break;
                    }

                    final String fileName = arg.startsWith("--") ? arg.substring(7) : arg.substring(5);
                    if(!fileName.isEmpty()) {
                        file = new File(fileName);
                        if(!file.exists()) {
                            argError = "File " + file.getAbsolutePath() + " doesn't exist.";
                            break;
                        }
                    } else {
                        argError = "Argument '--file' is missing value.";
                        break;
                    }
                } else if(arg.startsWith("--commands=") || arg.startsWith("commands=")) {
                    if(file != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time.";
                        break;
                    }
                    if(commands != null) {
                        argError = "Duplicate argument '--command'/'--commands'.";
                        break;
                    }
                    final String value = arg.startsWith("--") ? arg.substring(11) : arg.substring(9);
                    commands = Util.splitCommands(value);
                } else if(arg.startsWith("--command=") || arg.startsWith("command=")) {
                    if(file != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time.";
                        break;
                    }
                    if(commands != null) {
                        argError = "Duplicate argument '--command'/'--commands'.";
                        break;
                    }
                    final String value = arg.startsWith("--") ? arg.substring(10) : arg.substring(8);
                    commands = Collections.singletonList(value);
                } else if (arg.startsWith("--user=")) {
                    username = arg.startsWith("--") ? arg.substring(7) : arg.substring(5);
                } else if (arg.startsWith("--password=")) {
                    password = (arg.startsWith("--") ? arg.substring(11) : arg.substring(9)).toCharArray();
                } else if (arg.startsWith("--timeout=")) {
                    if (connectionTimeout > 0) {
                        argError = "Duplicate argument '--timeout'";
                        break;
                    }
                    final String value = arg.substring(10);
                    try {
                        connectionTimeout = Integer.parseInt(value);
                    } catch (final NumberFormatException e) {
                        //
                    }
                    if (connectionTimeout <= 0) {
                        argError = "The timeout must be a valid positive integer: '" + value + "'";
                    }
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    commands = Collections.singletonList("help");
                } else if (arg.startsWith("--properties=")) {
                    final String value  = arg.substring(13);
                    final File propertiesFile = new File(value);
                    if(!propertiesFile.exists()) {
                        argError = "File doesn't exist: " + propertiesFile.getAbsolutePath();
                        break;
                    }
                    
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(propertiesFile);
                        props.load(fis);
                    } catch(FileNotFoundException e) {
                        argError = e.getLocalizedMessage();
                        break;
                    } catch(java.io.IOException e) {
                        argError = "Failed to load properties from " + propertiesFile.getAbsolutePath() + ": " + e.getLocalizedMessage();
                        break;
                    } finally {
                        if(fis != null) {
                            try {
                                fis.close();
                            } catch(java.io.IOException e) {
                            }
                        }
                    }
                    for(final Object prop : props.keySet()) {
                    	AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            public Object run() {
                                System.setProperty((String)prop, (String)props.get(prop));
                                return null;
                            }
                        });
                    }
                } else if(!(arg.startsWith("-D") || arg.equals("-XX:"))) {// skip system properties and jvm options
                    // assume it's commands
                    if(file != null) {
                        argError = "Only one of '--file', '--commands' or '--command' can appear as the argument at a time: " + arg;
                        break;
                    }
                    if(commands != null) {
                        argError = "Duplicate argument '--command'/'--commands'.";
                        break;
                    }
                    commands = Util.splitCommands(arg);
                }
            }

            if(argError != null) {
                System.err.println(argError);
                exitCode = 1;
                return;
            }

            if(version) {
                cmdCtx = initCommandContext(defaultControllerProtocol, defaultControllerHost, defaultControllerPort, username, password, false, connect, connectionTimeout);
                VersionHandler.INSTANCE.handle(cmdCtx);
                return;
            }

            if(file != null) {
                cmdCtx = initCommandContext(defaultControllerProtocol, defaultControllerHost, defaultControllerPort, username, password, false, connect, connectionTimeout);
                processFile(file, cmdCtx, isAppDeployment, appName, props);
                return;
            }

            if(commands != null) {
                cmdCtx = initCommandContext(defaultControllerProtocol, defaultControllerHost, defaultControllerPort, username, password, false, connect, connectionTimeout);
                processCommands(commands, cmdCtx);
                return;
            }

            // Interactive mode
            cmdCtx = initCommandContext(defaultControllerProtocol, defaultControllerHost, defaultControllerPort, username, password, true, connect, connectionTimeout);
            cmdCtx.interact();
        } catch(Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            if(cmdCtx != null && cmdCtx.getExitCode() != 0) {
                exitCode = cmdCtx.getExitCode();
            }
            if (!gui) {
                System.exit(exitCode);
            }
        }
        System.exit(exitCode);
    }

    private static CommandContext initCommandContext(String defaultProtocol, String defaultHost, int defaultPort, String username, char[] password, boolean initConsole, boolean connect, final int connectionTimeout) throws CliInitializationException {
        final CommandContext cmdCtx = CommandContextFactory.getInstance().newCommandContext(defaultHost, defaultPort, username, password, initConsole, connectionTimeout);
        if(connect) {
            try {
                cmdCtx.connectController();
            } catch (CommandLineException e) {
                throw new CliInitializationException("Failed to connect to the controller", e);
            }
        }
        return cmdCtx;
    }


    private static void processCommands(List<String> commands, CommandContext cmdCtx) {
        int i = 0;
        try {
            while (cmdCtx.getExitCode() == 0 && i < commands.size() && !cmdCtx.isTerminated()) {
                cmdCtx.handleSafe(commands.get(i));
                ++i;
            }
        } finally {
            cmdCtx.terminateSession();
        }
    }

    private static void processFile(File file, final CommandContext cmdCtx, final boolean isAppDeployment, final String appName, final Properties props) {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (cmdCtx.getExitCode() == 0 && !cmdCtx.isTerminated() && line != null) {
            	if (isAppDeployment)
            		processAppDeployment(cmdCtx, appName, props, line);
            	else
            		cmdCtx.handleSafe(line.trim());
                line = reader.readLine();
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to process file '" + file.getAbsolutePath() + "'", e);
        } finally {
            StreamUtils.safeClose(reader);
            cmdCtx.terminateSession();
        }
    }
    
    private static void processAppDeployment(final CommandContext cmdCtx, final String appName, final Properties props, String line) throws IOException, CommandLineException {
    	
    	String command = null;
    	 //check if module already exist then add it
		if (line.startsWith("module add")) {
			ModelNode moduleInfo = cmdCtx
					.buildRequest("/core-service=module-loading/:list-resource-loader-paths(module="
							+ props.getProperty("module.name") + ")");
			String result = cmdCtx.getModelControllerClient()
					.execute(moduleInfo).get("outcome").asString();
			
			if ("success".equals(result)) {
				// suppression du module existant
				try {
					command = "module remove --name="
							+ props.getProperty("module.name");
					cmdCtx.handle(command);
				} catch (CommandLineException cmdEx) {
					if (!cmdEx.getMessage().contains("Failed to locate module")) {
						throw cmdEx;
					}
					// module remove OK
				}
			}

			// Command ajout du module
			command = "module add --name=" + props.getProperty("module.name")
					+ " --module-xml="+ props.getProperty("module.dir") + System.getProperty("file.separator")+"module.xml --resources=" + props.getProperty("module.dir") + System.getProperty("file.separator")+"quartz-helloworld.properties ";
			//cmdCtx.handle(command);
		} 
		//check is app is already deployed, then deploy it
		else if(line.startsWith("deploy")) {
			command = line;
			cmdCtx.handle(command);
			
		}
		//nothing special for this command
		else{
			cmdCtx.handleSafe(line.trim());
		}
		
	}
    
   
}