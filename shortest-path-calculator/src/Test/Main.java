package Test;

import code.Cal;
import code.Node;
import code.Pair;


import java.util.concurrent.LinkedBlockingQueue;

public class Main {


    public static void main(String[] args) {
        Node node1 = new Node("park");
        Node node2 = new Node("cafe");
        Node node3 = new Node("home");
        Node node4 = new Node("school");
        Node node5 = new Node("NYPD");
        Node node6 = new Node("farm");
        Node node7 = new Node("church");
        Node[] map = new Node[]{node1, node2, node3, node4, node5, node6, node7};
        Node.setConn(node1, node2,5);
        Node.setConn(node1, node3, 6);
        Node.setConn(node2, node4, 3);
        Node.setConn(node2, node5, 5);
        Node.setConn(node3, node5, 1);
        Node.setConn(node3, node6, 8);
        Node.setConn(node4, node5, 1);
        Node.setConn(node5, node7, 3);
        LinkedBlockingQueue<Pair<Node>> tasks = new LinkedBlockingQueue<>();

        tasks.add(new Pair<Node>(node1, node7));
        tasks.add(new Pair<Node>(node1, node6));
        tasks.add(new Pair<Node>(node2, node4));
        tasks.add(new Pair<Node>(node3, node1));
        tasks.add(new Pair<Node>(node5, node1));
        tasks.add(new Pair<Node>(node2, node7));
        tasks.add(new Pair<Node>(node3, node7));
        System.out.println(tasks);

        Cal instance = Cal.getInstance();
        instance.setMap(map);
        instance.setTasks(tasks);
        instance.execute();
    }
}
