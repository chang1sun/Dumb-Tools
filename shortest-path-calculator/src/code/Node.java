package code;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Node{
    private final HashMap<Node,Integer> conn;
    private final String name;
    private final HashMap<Node,Integer> dists;
    public Node(String name){
        this.name = name;
        this.conn = new HashMap<Node,Integer>();
        this.dists = new HashMap<Node, Integer>();
    }

    public String getName(){
        return this.name;
    }

    public int getConn(Node obj){
        return this.conn.get(obj);
    }

    // Return all adjacent node as an array
    public Node[] getAllObj(){
        return this.conn.keySet().toArray(new Node[0]);
    }

    public void set(Node node, int dist){
        this.conn.put(node, dist);
    }

    public static void setConn(Node x, Node y, int dist){
        x.set(y, dist);
        y.set(x, dist);
    }

    private void calPath(Node[] map){
            if(!dists.isEmpty()){return;}
            for (Node obj: map
            ) {
                dists.put(obj, Integer.MAX_VALUE);
            }
            dists.put(this, 0);
            Stack<Node> todo = new Stack<>();
            HashSet<Node> visited = new HashSet<>();
            todo.add(this);
            while (!todo.isEmpty()){
                Node cur = todo.pop();
                int t = dists.get(cur);
                for (Node node: cur.getAllObj()
                ) {
                    if (!visited.contains(node)) {
                        visited.add(node);
                        todo.add(node);
                        if (t + cur.getConn(node) < dists.get(node)){
                            dists.put(node, t + cur.getConn(node));
                        }
                    }
                }
            }
        }

    public synchronized int getDist(Node ed, Node[] map){
        if(this.dists.isEmpty())
            {this.calPath(map);}
        return this.dists.get(ed)==Integer.MAX_VALUE ? -1 : this.dists.get(ed);
    }

}
