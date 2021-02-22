package code;

import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Queue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
public class Cal{
    Thread th = new Thread();
    private static Node[] map;
    private static Queue<Pair<Node>> taskQueue;
    private static Cal instance = new Cal();
    private final static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            10,
            15,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    private Cal(){}

    public static Cal getInstance(){
        return instance;
    }

    public void setMap(@NotNull ArrayList<Node> trans){
        map = trans.toArray(new Node[0]);
    }


    public void setMap(@NotNull Node[] trans) {
        map = trans;
    }

    public void setTasks(LinkedBlockingQueue<Pair<Node>> tasks){
        taskQueue = tasks;
    }


    public void execute() {
        System.out.println("开始计算");
        ;
        while (!taskQueue.isEmpty()) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (taskQueue.isEmpty()) {threadPoolExecutor.shutdown(); return;}
                        try {
                            Pair<Node> task = taskQueue.poll();
                            System.out.printf("我是线程%s，%s到%s的最短路径为%d\n", Thread.currentThread().getName(), task.get(0).getName(), task.get(1).getName(), task.get(0).getDist(task.get(1), map));
                        } catch (NoSuchElementException | NullPointerException e) {
                            System.out.printf("我是线程%s, 我拿到了空值\n", Thread.currentThread().getName());
                            e.printStackTrace();
                        }
                }
            });
        }
    }
}

