package scribe;

import java.util.Vector;

import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.p2p.commonapi.NodeHandle;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

public class Main {
    Vector<Client> apps = new Vector<Client>();

    public Main(int numOfNodes, int port, Environment env) throws Exception {
        NodeIdFactory nodeIdFactory = new RandomNodeIdFactory(env);
        PastryNodeFactory factory = new SocketPastryNodeFactory(nodeIdFactory, numOfNodes, env);
        InetSocketAddress bootaddress = new InetSocketAddress(InetAddress.getLocalHost(), port);

        for (int i = 0; i < numOfNodes; i++) {
            PastryNode node = factory.newNode();
            Client app = new Client(node);
            apps.add(app);
            node.boot(bootaddress);

            synchronized(node) {
                while (!node.isReady() && !node.joinFailed()) {
                    node.wait(500);

                    if (node.joinFailed()) {
                        throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason()); 
                    }
                }
            }
            
        }

        Iterator<Client> it = apps.iterator();
        Client app = it.next();
        app.subscribe();
        app.startPublishTask();

        while (it.hasNext()) {
            app = it.next();
            app.subscribe();
        }

        env.getTimeSource().sleep(5000);

    }

    public static void printTree(Vector<Client> apps) {
        // build a hashtable of the apps, keyed by nodehandle
        Hashtable<NodeHandle, Client> appTable = new Hashtable<NodeHandle, Client>();
        Iterator<Client> i = apps.iterator();
        while (i.hasNext()) {
          Client app = (Client) i.next();
          appTable.put(app.endpoint.getLocalNodeHandle(), app);
        }
        NodeHandle seed = ((Client) apps.get(0)).endpoint
            .getLocalNodeHandle();
    
        // get the root
        NodeHandle root = getRoot(seed, appTable);
    
        // print the tree from the root down
        recursivelyPrintChildren(root, 0, appTable);
    }

    public static NodeHandle getRoot(NodeHandle seed, Hashtable<NodeHandle, Client> appTable) {
        Client app = (Client) appTable.get(seed);
        if (app.isRoot())
          return seed;
        NodeHandle nextSeed = app.getParent();
        return getRoot(nextSeed, appTable);
    }

    public static void recursivelyPrintChildren(NodeHandle curNode, int recursionDepth, Hashtable<NodeHandle, Client> appTable) {
        // print self at appropriate tab level
        String s = "";
        for (int numTabs = 0; numTabs < recursionDepth; numTabs++) {
            s += "  ";
        }
        s += curNode.getId().toString();
        System.out.println(s);

        // recursively print all children
        Client app = (Client) appTable.get(curNode);
        NodeHandle[] children = app.getChildren();
        for (int curChild = 0; curChild < children.length; curChild++) {
            recursivelyPrintChildren(children[curChild], recursionDepth + 1, appTable);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Create a Scanner
            Scanner input = new Scanner(System.in);
            // Block until user enters a line of input as a array of String
            args = input.nextLine().split("\\s+");
            // Close the Scanner
            input.close();
        }
        // Get the command line arguments
        // Store the first argument in a variable `N`
        int N = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);

        // Assert that there is one argument (no less and no more) and it is a positive
        // integer
        // Abort otherwise
        // Run `java -ea SimpleAggr 5` to get assertion error
        assert args.length == 1 && N > 0;

        // Print "Success" and value of `N`
        System.out.println("N = " + N);
        System.out.println("args.length = " + args.length);
        System.out.println("Success");

        try {
            // Launch the application
            Environment env = new Environment();

            Main m = new Main(N, port, env);
        } catch (Exception e) {
            // Print error message
            System.out.println(e.getMessage());

            throw e;
        }
    }
}