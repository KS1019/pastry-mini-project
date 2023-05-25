package scribe;

import java.util.HashSet;
import java.util.Iterator;

import rice.p2p.commonapi.Application;
import rice.p2p.scribe.ScribeClient;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.Topic;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.pastry.commonapi.PastryIdFactory;


@SuppressWarnings("deprecation")
public class Client implements ScribeClient, Application {
  int seqNum = 0;
  CancellableTask publishTask;
  CancellableTask endingTask;
  Scribe myScribe;
  Topic myTopic;
  protected Endpoint endpoint;
  HashSet<Id> children = new HashSet<Id>();
  public Client(Node node) {
    this.endpoint = node.buildEndpoint(this, "myinstance");
    // construct Scribe
    myScribe = new ScribeImpl(node,"myScribeInstance");

    // construct the topic
    myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), "SimpleAggr");
    System.out.println("myTopic = "+myTopic.toString());
    
    // now we can receive messages
    endpoint.register();
  }
  
  /**
   * Subscribes to myTopic.
   */
  public void subscribe() {
    myScribe.subscribe(myTopic, this); 
  }

  public void unsubscribe() {
    myScribe.unsubscribe(myTopic, this);
  }
  
  /**
   * Starts the publish task.
   */
  public void startPublishTask() {
    publishTask = endpoint.scheduleMessage(new PublishContent(), 5000, 5000);
    System.out.println("Started publishing task.");
  }

  public void stopPublishTask() {
    publishTask.cancel();
    endingTask = endpoint.scheduleMessage(new EndingContent(), 0, 500);
    System.out.println("Stopped publishing task.");
  }
  
  
  /**
   * Part of the Application interface.  Will receive PublishContent every so often.
   */
  public void deliver(Id id, Message message) {
    if (message instanceof PublishContent) {
      sendMulticast();
      sendAnycast();
      // Prints delivery message
      System.out.println("deliver(Id id, Message message). Received PublishContent message ");
    } else if (message instanceof EndingContent) {
      // Prints the received message
      System.out.println("deliver(Id id, Message message). Received EndingContent message ");
      if (isRoot()) {
        HashSet<Id> children = getChildrenSet();
        Iterator<Id> itc = children.iterator();
        System.out.println("=".repeat(50));
        // Print all the children message
        System.out.println("Printing children of root node " + endpoint.getId() + ":");
        while (itc.hasNext()) {
            Id child = itc.next();
            System.out.println("Child Id: " + child.toString());
        }
        System.out.println("=".repeat(50));
        endingTask.cancel();
      } else {
        // get parent node
        NodeHandle parent = getParent();
        // pass message to parent
        endpoint.route(parent.getId(), message, null);
      }
    } else {
      System.out.println("deliver(Id id, Message message). Received message "+message);
    }
  }
  
  /**
   * Sends the multicast message.
   */
  public void sendMulticast() {
    System.out.println("Node "+endpoint.getLocalNodeHandle()+" broadcasting "+seqNum);
    // Add self to children
    HashSet<Id> children = new HashSet<Id>(this.children);
    children.add(this.endpoint.getLocalNodeHandle().getId());
    Content myMessage = new Content(endpoint.getLocalNodeHandle(), seqNum, children);
    myScribe.publish(myTopic, myMessage); 
    seqNum++;
  }

  /**
   * Called whenever we receive a published message.
   */
  public void deliver(Topic topic, ScribeContent content) {
    if (((Content)content).from == null) {
      new Exception("Stack Trace").printStackTrace();
    }

    // Prints the received message
    System.out.println("deliver(Topic topic, ScribeContent content). Received message #"+((Content)content).seq+" from "+((Content)content).from.getId());
  }

  /**
   * Sends an anycast message.
   */
  public void sendAnycast() {
    HashSet<Id> children = new HashSet<Id>(this.children);
    children.add(this.endpoint.getId());
    Content myMessage = new Content(endpoint.getLocalNodeHandle(), seqNum, children);
    myScribe.anycast(myTopic, myMessage); 
    seqNum++;
  }

  public boolean anycast(Topic topic, ScribeContent content) {
    boolean returnValue = myScribe.getEnvironment().getRandomSource().nextInt(3) == 0;
    System.out.println("Client.anycast("+topic+","+content+"):"+returnValue);
    return returnValue;
  }

  public void childAdded(Topic topic, NodeHandle child) {
    // Print child node id
    System.out.println("childAdded(Topic topic, NodeHandle child). Child added: "+child.getId());
    // Add child node id to children HashSet
    children.add(child.getId());
  }

  public void childRemoved(Topic topic, NodeHandle child) {
  }

  public void subscribeFailed(Topic topic) {
  }

  public boolean forward(RouteMessage message) {
    return true;
  }

  public void unsubscribe(Topic topic) {
    // Print called message
    System.out.println("unsubscribe(Topic topic). Unsubscribed from "+topic);
    // Check         
  }


  public void update(NodeHandle handle, boolean joined) {
    System.out.println("Node "+handle.getId()+" update called. joined:"+joined);
  }

  class PublishContent implements Message {
    public int getPriority() {
      return MAX_PRIORITY;
    }
  }

  class EndingContent implements Message {
    public int getPriority() {
      return MAX_PRIORITY;
    }
  }

  
  /************ Some passthrough accessors for the myScribe *************/
  public boolean isRoot() {
    return myScribe.isRoot(myTopic);
  }
  
  public NodeHandle getParent() {
    // NOTE: Was just added to the Scribe interface.  May need to cast myScribe to a
    // ScribeImpl if using 1.4.1_01 or older.
    return ((ScribeImpl)myScribe).getParent(myTopic); 
  }
  
  public NodeHandle[] getChildren() {
    return myScribe.getChildren(myTopic); 
  }

  public HashSet<Id> getChildrenSet() {
    return children;
  }
}