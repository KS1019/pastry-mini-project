package scribe;

import rice.p2p.scribe.ScribeContent;
import rice.p2p.commonapi.NodeHandle;

public class Content implements ScribeContent {
  NodeHandle from;
  int seq;
  public Content(NodeHandle from, int seq) {
    this.from = from;
    this.seq = seq;
  }
  public String toString() {
    return "Content #"+seq+" from "+from;
  }  
}
