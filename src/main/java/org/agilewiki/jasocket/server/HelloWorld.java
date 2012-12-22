package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;

public class HelloWorld extends Server {

    protected String serviceName() {
        return "helloWorld";
    }

    protected void startService(PrintJid out, RP rp) throws Exception {
        registerServiceCommand(new ServiceCommand("hi", "says hello") {
            @Override
            public void eval(String args, PrintJid out, RP rp) throws Exception {
                println(out, "Hello!");
                rp.processResponse(out);
            }
        });
        super.startService(out, rp);
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            node.startup(HelloWorld.class, "");
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
