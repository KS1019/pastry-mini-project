package scribe;

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
  Scribe myScribe;
  Topic myTopic;
  protected Endpoint endpoint;
  public Client(Node node) {
    // you should recognize this from lesson 3
    this.endpoint = node.buildEndpoint(this, "myinstance");

    // construct Scribe
    myScribe = new ScribeImpl(node,"myScribeInstance");

    // construct the topic
    myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), "example topic");
    System.out.println("myTopic = "+myTopic);
    
    // now we can receive messages
    endpoint.register();
  }
  
  /**
   * Subscribes to myTopic.
   */
  public void subscribe() {
    myScribe.subscribe(myTopic, this); 
  }
  
  /**
   * Starts the publish task.
   */
  public void startPublishTask() {
    publishTask = endpoint.scheduleMessage(new PublishContent(), 5000, 5000);    
  }
  
  
  /**
   * Part of the Application interface.  Will receive PublishContent every so often.
   */
  public void deliver(Id id, Message message) {
    if (message instanceof PublishContent) {
      sendMulticast();
      sendAnycast();
    }
  }
  
  /**
   * Sends the multicast message.
   */
  public void sendMulticast() {
    System.out.println("Node "+endpoint.getLocalNodeHandle()+" broadcasting "+seqNum);
    Content myMessage = new Content(endpoint.getLocalNodeHandle(), seqNum);
    myScribe.publish(myTopic, myMessage); 
    seqNum++;
  }

  /**
   * Called whenever we receive a published message.
   */
  public void deliver(Topic topic, ScribeContent content) {
    System.out.println("MyScribeClient.deliver("+topic+","+content+")");
    if (((Content)content).from == null) {
      new Exception("Stack Trace").printStackTrace();
    }
  }

  /**
   * Sends an anycast message.
   */
  public void sendAnycast() {
    System.out.println("Node "+endpoint.getLocalNodeHandle()+" anycasting "+seqNum);
    Content myMessage = new Content(endpoint.getLocalNodeHandle(), seqNum);
    myScribe.anycast(myTopic, myMessage); 
    seqNum++;
  }

  public boolean anycast(Topic topic, ScribeContent content) {
    boolean returnValue = myScribe.getEnvironment().getRandomSource().nextInt(3) == 0;
    System.out.println("MyScribeClient.anycast("+topic+","+content+"):"+returnValue);
    return returnValue;
  }

  public void childAdded(Topic topic, NodeHandle child) {
//    System.out.println("MyScribeClient.childAdded("+topic+","+child+")");
  }

  public void childRemoved(Topic topic, NodeHandle child) {
//    System.out.println("MyScribeClient.childRemoved("+topic+","+child+")");
  }

  public void subscribeFailed(Topic topic) {
//    System.out.println("MyScribeClient.childFailed("+topic+")");
  }

  public boolean forward(RouteMessage message) {
    return true;
  }


  public void update(NodeHandle handle, boolean joined) {
    
  }

  class PublishContent implements Message {
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
    //return myScribe.getParent(myTopic); 
  }
  
  public NodeHandle[] getChildren() {
    return myScribe.getChildren(myTopic); 
  }
}