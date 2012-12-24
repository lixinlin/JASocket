/*
 * Copyright 2012 Bill La Forge
 *
 * This file is part of AgileWiki and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * or navigate to the following url http://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Note however that only Scala, Java and JavaScript files are being covered by LGPL.
 * All other files are covered by the Common Public License (CPL).
 * A copy of this license is also included and can be
 * found as well at http://www.opensource.org/licenses/cpl1.0.txt
 */
package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.Closable;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.cluster.RegisterServer;
import org.agilewiki.jasocket.cluster.UnregisterServer;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;

import java.util.Iterator;
import java.util.TreeMap;

public class Server extends JLPCActor implements Closable {
    private Node node;
    protected TreeMap<String, ServerCommand> serverCommands = new TreeMap<String, ServerCommand>();
    protected String startupArgs;

    protected String serverName() {
        return this.getClass().getName();
    }

    protected Node node() {
        return node;
    }

    protected AgentChannelManager agentChannelManager() {
        return node.agentChannelManager();
    }

    protected void registerServerCommand(ServerCommand serverCommand) {
        serverCommands.put(serverCommand.name, serverCommand);
    }

    public void startup(Node node, final String args, final PrintJid out, final RP rp) throws Exception {
        this.node = node;
        this.startupArgs = args;
        node.mailboxFactory().addClosable(this);
        RegisterServer registerServer = new RegisterServer(serverName(), this);
        registerServer.send(this, agentChannelManager(), new RP<Boolean>() {
            @Override
            public void processResponse(Boolean response) throws Exception {
                if (response)
                    startServer(out, rp);
                else {
                    out.println("Server already registered: " + serverName());
                    rp.processResponse(out);
                }
            }
        });
    }

    protected void startServer(PrintJid out, RP rp) throws Exception {
        registerShutdownCommand();
        registerHelpCommand();
        out.println(serverName() + " started");
        rp.processResponse(out);
    }

    public void close() {
        UnregisterServer unregisterServer = new UnregisterServer(serverName());
        try {
            unregisterServer.sendEvent(agentChannelManager());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void evalServerCommand(String commandString, PrintJid out, RP rp) throws Exception {
        commandString = commandString.trim();
        int i = commandString.indexOf(' ');
        String command = commandString;
        String args = "";
        if (i > -1) {
            command = commandString.substring(0, i);
            args = commandString.substring(i + 1).trim();
        }
        ServerCommand serverCommand = serverCommands.get(command);
        if (serverCommand == null) {
            out.println("Unknown command for " + serverName() + ": " + command);
            rp.processResponse(out);
            return;
        }
        serverCommand.eval(args, out, rp);
    }

    protected void registerShutdownCommand() {
        registerServerCommand(new ServerCommand("shutdown", "Stops and unregisters the server") {
            @Override
            public void eval(String args, PrintJid out, RP<PrintJid> rp) throws Exception {
                close();
                out.println("Stopped " + serverName());
                rp.processResponse(out);
            }
        });
    }

    protected void registerHelpCommand() {
        registerServerCommand(new ServerCommand("help", "List the commands supported by the server") {
            @Override
            public void eval(String args, PrintJid out, RP<PrintJid> rp) throws Exception {
                Iterator<String> it = serverCommands.keySet().iterator();
                while (it.hasNext()) {
                    ServerCommand ac = serverCommands.get(it.next());
                    out.println(ac.name + " - " + ac.description);
                }
                rp.processResponse(out);
            }
        });
    }
}