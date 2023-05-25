package scribe;

import rice.p2p.scribe.ScribeContent;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;

import java.util.HashSet;

public class Content implements ScribeContent {
  NodeHandle from;
  int seq;
  HashSet<Id> children;
  public Content(NodeHandle from, int seq, HashSet<Id> children) {
    this.from = from;
    this.seq = seq;
    this.children = children;
  }
  public String toString() {
    return "Content #"+seq+" from "+from.getId();
  }  
}
