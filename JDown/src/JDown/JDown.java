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
    private String localFilePath;
    private AtomicLong hasDone;
    private long fileSize;
    public AtomicBoolean isWrong;
    private boolean isNew;
    public JDownTracer tracer;
    private boolean overWriteSign;


    public JDown(String url, int threadNum){
        this.overWriteSign = false;
        this.isWrong = new AtomicBoolean(false);
        this.localFileName = url.substring(url.lastIndexOf("/")+1);
        this.threadNum = threadNum;
        this.tracer = new JDownTracer(localFileName, url, threadNum);
        this.localFilePath = "";
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

    public JDown(String url, int threadNum, String localFilePath){
        this(url, threadNum);
        this.localFilePath = localFilePath;
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
        isNew = !Files.exists(Paths.get(localFileName + ".properties"));

        if (!isNew){
            continueMission();
        }
        if(isNew || overWriteSign){
            startNewMission();
        }
    }

    private void continueMission(){
        JDownTracer.load(tracer);
        hasDone = new AtomicLong(tracer.getHasDown());
        if (hasDone.get() >= fileSize){
            if ((doOverwrite())){overWriteSign = true;}
            return;
        }

        try {
            this.localFile = new RandomAccessFile(localFilePath+localFileName, "rw");
            this.localFile.setLength(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log("loading properties file...");
        String tab = "            ";
        log(String.format("Continue download mission:\n\n%s[url: \t%s],\n%s[localFile: \t%s],\n%s[download progress: \t%.2f%%]\n%s[totalFileSize: \t%.2fMb]\n",
                tab,
                url,
                tab,
                localFileName,
                tab,
                (double)hasDone.get() / fileSize * 100,
                tab,
                (double)fileSize / 1024 / 1024));
        run();
    }

    private void startNewMission(){
        tracer = new JDownTracer(localFileName, url, threadNum);
        hasDone = new AtomicLong(0);
        try {
            this.localFile = new RandomAccessFile(localFilePath+localFileName, "rw");
            this.localFile.setLength(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String tab = "            ";
        log(String.format("Start new download mission:\n\n%s[url: \t%s],\n%s[localFile: \t%s],\n%s[totalFileSize: \t%.2fMb]\n",
                tab,
                url,
                tab,
                localFileName,
                tab,
                (double)fileSize / 1024 / 1024));
        run();
    }

    private boolean doOverwrite() {
        logErr("File already exists, do you want to overwrite? [y/n]");
        Scanner input = new Scanner(System.in);
        if ("y".equals(input.next())) {
            try {
                Files.deleteIfExists(Paths.get(localFilePath + localFileName));
                Files.deleteIfExists(Paths.get(localFileName + ".properties"));
                log("Preparing to overwrite file...");
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log("Waiting for program's ending...");
        }
        return false;
    }

    /*
     * Allocate jobs for threads, job range can be glean from tracer if it exist;
     * */
    public void allocate(){
        log("Allocating jobs for [" + threadNum +"] threads...");
        long blockSize = fileSize / threadNum;
        long pos = 0, limit = 0;
        long[][] range = null;
        int threadID = 0;
        if (!isNew && !overWriteSign) {
            range = tracer.getRange();
        }
        for (int i = 0; i < threadNum; i++) {
            if (!isNew && !overWriteSign) {
                assert range != null;
                threadID = i;
                pos = range[i][0];
                limit = range[i][1];
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
            try {
                Thread.sleep(1000);
                ProgressBar.flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(++sec == 1) {
                System.out.print(String.format("[Speed: %2.1fMb/s][Progress: %2.1f%%]: |-%s-|", (float)(cur-blockLen)/1024/1024, (float)cur / fileSize * 100, ProgressBar.bar((int)(cur * 25 / fileSize ))));
                sec = 0;
                blockLen = cur;
            }

            cur = tracer.getHasDown();
        }
        try {
            localFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long totalTime = (System.currentTimeMillis()-startTime)/1000;
        int ms = (int)totalTime / 60;
        int ss = Math.floorMod(totalTime, 60);
        if (isWrong.get()){
            try {
                Files.delete(Paths.get(localFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            logErr(String.format("\nDownload mission canceled! Total time: %dm%ds", ms, ss));
        }else{
            log(String.format("\nDownload mission completed! Total time: %dm%ds", ms, ss));
        }

    }


    private static void log(String s){
        System.out.printf("[%s] %s\n", new SimpleDateFormat("HH:mm:ss").format(new Date()), s);
    }
    private static void logErr(String s){
        System.err.printf("[%s] %s\n", new SimpleDateFormat("HH:mm:ss").format(new Date()), s);
    }

    class JDownThread extends Thread{

        /**
         * pos, limit represent the write position and the last byte of it's job respectively.
         */

        private long pos;
        private final long limit;
        private final String url;
        private final int id;
        private int bufferSize;

        public JDownThread(int id, String url, long pos, long limit){
            this.url = url;
            this.pos = pos;
            this.limit = limit;
            this.id = id;
            this.bufferSize = 1024 * 1024 * 2;
        }

        public JDownThread(int id, String url, long pos, long limit, int bufferSize){
            this(id, url, pos, limit);
            this.bufferSize = bufferSize;
        }


        @Override
        public void run() {
//            log(String.format("No."+ id + " thread"  + " start working!, responsible range: [%.2f%% - %.2f%%]", (double)(fileSize-pos)/1024/1024, (double)(fileSize-limit)/1024/1024));
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            FileChannel writeChannel = localFile.getChannel();
            try {
                HttpURLConnection conn = getConn(url);
                conn.setRequestProperty("Range", String.format("bytes=%d-%d", pos, limit)); // Set thread's working range
                conn.connect();
                if (HttpURLConnection.HTTP_PARTIAL != conn.getResponseCode()){ // HTTP_PARTIAL is a special http status code: 206; Allow get the web content partially when specify a range;
                    logErr("Wrong status code：" + conn.getResponseCode());
                    throw new IOException();
                }
                long curPartLen;
                ReadableByteChannel readChannel = Channels.newChannel(conn.getInputStream());
                while (!isWrong.get() && pos <= limit){
                    buffer.clear();
                    if (-1 != (curPartLen = readChannel.read(buffer))) {
                        // method "flip" set the pos of buffer to 0; and limit to where pos was just last moment; it is often used to make the buffer ready to write;
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            writeChannel.write(buffer, pos);
                        }
                        hasDone.addAndGet(curPartLen);
                        pos += curPartLen;
                        tracer.update(id, curPartLen, pos, limit);
                    }
                }
//                if (!isWrong.get()){
//                    log("No." + id + " thread" + " has complete it's task!");
//                }

                if (readChannel != null){readChannel.close();}

            } catch (IOException e) {
                isWrong.set(true);
                logErr("An error occurs with thread_" + id);
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        new JDown("https://dldir1.qq.com/qqfile/qq/PCTIM/TIM3.3.5/TIM3.3.5.22018.exe", 4).startMission();
    }

}

