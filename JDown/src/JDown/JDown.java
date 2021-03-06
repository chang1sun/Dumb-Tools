package JDown;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class JDown {
    private String url;
    private final int threadNum;
    private RandomAccessFile localFile;
    private final String localFileName;
    private AtomicLong hasDone;
    private long fileSize;
    public AtomicBoolean isWrong;
    private boolean isNew;
    public JDownTracer tracer;


    public JDown(String url, int threadNum){
        this.isWrong = new AtomicBoolean(false);
        this.localFileName = url.substring(url.lastIndexOf("/")+1);
        this.threadNum = threadNum;
        this.tracer = new JDownTracer(localFileName, url, threadNum);
        try{
            this.url = url;
            HttpURLConnection conn = getConn(url);
            conn.connect();
            this.fileSize = conn.getContentLength();
            conn.disconnect();
        }catch (MalformedURLException e) {
            logErr("URL Error, please check the correctness of url and retry!\n");
            e.printStackTrace();
        }catch (IOException e){
            logErr("Failed in connecting!");
            e.printStackTrace();
        }
    }


    public static HttpURLConnection getConn(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
//        conn.setRequestProperty("Connection", "Keep-Alive");
        return conn;
    }

    public void startMission(){

        if (fileSize == -1){
            logErr("Can't download an empty file");
            return;
        }
        System.out.println(url+"\n"+fileSize);
        isNew = !Files.exists(Paths.get(localFileName + ".properties"));

        if (!isNew){
            continueMission();
        }
        if(isNew){
            startNewMission();
        }
        run();
    }

    private void continueMission(){
        JDownTracer.load(tracer);
        hasDone = new AtomicLong(tracer.getHasDown());
        if ((doOverwrite())){isNew = true; return;}
        try {
            this.localFile = new RandomAccessFile(localFileName, "rw");
            this.localFile.setLength(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log(String.format("Continue download mission: \n   [url: %s],\n   [localFile: %s],\n   [download progress: %s]\n   [totalFileSize: %d]",
                url,
                localFileName,
                hasDone.get() / fileSize / 0.01,
                fileSize / 1024 / 1024));
    }

    private void startNewMission(){
        tracer = new JDownTracer(localFileName, url, threadNum);
        hasDone = new AtomicLong(0);
        try {
            this.localFile = new RandomAccessFile(localFileName, "rw");
            this.localFile.setLength(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log(String.format("Start new download mission: \n   [url: %s],\n   [localFile: %s],\n   [totalFileSize: %d]",
                url,
                localFileName,
                fileSize));
    }

    private boolean doOverwrite(){
        if (hasDone.get() >= fileSize){
            logErr("File already exists, do you want to overwrite? [y/n]");
            Scanner input = new Scanner(System.in);
            if("y".equals(input.next())){
                try {
                    Files.deleteIfExists(Paths.get(localFileName));
                    Files.deleteIfExists(Paths.get(localFileName+".properties"));
                    log("Preparing to overwrite file...");
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                log("Waiting for program's ending...");
                return false;
            }
        }return false;
    }

    public void allocate(){
        log("Allocating jobs for threads...");
        long blockSize = fileSize / threadNum;
        long pos = 0, limit = 0;
        long[][] range = null;
        int threadID = 0;
        if (!isNew) {
            range = tracer.getRange();
        }
        for (int i = 0; i < threadNum; i++) {
            if (!isNew) {
                assert range != null;
                threadID = (int)range[i][0];
                pos = range[i][1];
                limit = range[i][2];
            } else {
                threadID = i;
                pos = i * blockSize;
                limit = (i == threadNum - 1) ? fileSize-1 : pos + blockSize;
            }
            new JDownThread(threadID, url, pos, limit).start();
        }
    }

    /**
     * blockLen -> progress made 2 seconds before, is used to calculate speed.
     * sec -> render those information every 2 seconds
    * */
    public void run(){
        long startTime = System.currentTimeMillis();
        allocate();
        long blockLen = 0;
        int sec = 0;
        long cur = tracer.getHasDown();
        while (!isWrong.get() && cur < fileSize){
            if(++sec == 1){
                log(String.format("Download speed: %.1fMb/s; Download progress: %.1f%%", (float)(cur-blockLen)/3/1024/1024, (float)cur / fileSize * 100));
                sec = 0;
                blockLen = cur;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cur = tracer.getHasDown();
        }
        try {
            localFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long totalTime = (System.currentTimeMillis()-startTime)/1000;
        int ms = Math.floorMod(totalTime, 60);
        int ss = (int)totalTime % 60;
        if (isWrong.get()){
            try {
                Files.delete(Paths.get(localFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            logErr(String.format("Download mission canceled due to a thread meets with trouble! Total time: %dm%ds", ms, ss));
        }else{
            log(String.format("Download mission completed! Total time: %dm%ds", ms, ss));
        }

    }

    class JDownThread extends Thread{

        private long pos;
        private final long limit;
        private final String url;
        private final int id;

        public JDownThread(int id, String url, long pos, long limit){
            this.url = url;
            this.pos = pos;
            this.limit = limit;
            this.id = id;
        }


        /* 此处其实可以用channel的transferTo方法，但经过百度，发现在传输4G以上大文件时不适用，遂改成用2M大小的ByteBuffer。
        * */
        @Override
        public void run() {
            log(String.format("thread_" + id + "start working!, my responsible range: [%d - %d]", pos, limit));
            ByteBuffer buffer = ByteBuffer.allocate(1024*1024*2);
            FileChannel writeChannel = localFile.getChannel();
            try {
                HttpURLConnection conn = getConn(url);
                conn.setRequestProperty("Range", String.format("bytes=%d-%d", pos, limit));
                conn.connect();
                if (HttpURLConnection.HTTP_PARTIAL != conn.getResponseCode()){
                    logErr("Wrong status code：" + conn.getResponseCode());
                    throw new IOException();
                }
                long curPartLen;
                ReadableByteChannel readChannel = Channels.newChannel(conn.getInputStream());
                while (!isWrong.get() && pos <= limit){
                    buffer.clear();
                    if (-1 != (curPartLen = readChannel.read(buffer))) {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            writeChannel.write(buffer, pos);
                        }
                        hasDone.addAndGet(curPartLen);
                        pos += curPartLen;
                        tracer.update(id, curPartLen, pos, limit);

                    }
                }
                if (!isWrong.get()){
                    log("Thread_" + id + "has complete it's task!");
                }

                if (readChannel != null){readChannel.close();}

            } catch (IOException e) {
                isWrong.set(true);
                logErr("An error occurs with thread_" + id);
                e.printStackTrace();
            }

        }
    }

    private static void log(String s){
        System.out.printf("[%s] %s\n", new SimpleDateFormat("HH:mm:ss").format(new Date()), s);
    }
    private static void logErr(String s){
        System.err.printf("[%s] %s\n", new SimpleDateFormat("HH:mm:ss").format(new Date()), s);
    }

    public static void main(String[] args) {
        new JDown("https://dldir1.qq.com/qqfile/qq/PCTIM/TIM3.3.5/TIM3.3.5.22018.exe", 4).startMission();
    }

}

