package code;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
public class Down {

    /*
    * url -> URL : get assigned in the contributor given specific path;
    * targetFile -> String: name of local file;
    * threadNum -> int : amount of thread;
    * threads -> WorkerThread[] : store threads for later use, notice that WorkerThread extends Thread.
    * filesize -> int : size of target file
    * */
    //定义下载资源的路径
    private URL url;
    private String targetFile;
    private int threadNum;
    private WorkerThread[] threads;
    private int fileSize;

    public Down(String path,String targetFile,int threadNum) throws MalformedURLException {
        this.url=new URL(path);
        this.threadNum=threadNum;
        //初始化threads数组
        threads=new WorkerThread[threadNum];
        this.targetFile=targetFile;
    }

    /*
    * Set optional format of target file and from which get a HttpURLConnection object.
    * */
    public HttpURLConnection getConn() throws IOException {
        HttpURLConnection conn=(HttpURLConnection)url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty(
                "Accept",
                "image/gif,image/jpeg,image/pjpeg,"
                        +"application/x-shockwave-flash,application/xaml+xml,"
                        +"application/vnd.ms-xpsdocument,application/x-ms-xbap,"
                        +"application/x-ms-application,application/vnd.ms-excel,"
                        +"application/vnd.ms-powerpoint,application/msword, */*"
        );
        conn.setRequestProperty("Accept-Language","zh-CN");
        conn.setRequestProperty("Charset","UTF-8");
        return conn;
    }

    public void download() throws IOException
    {
        HttpURLConnection conn = getConn();
        // set 'Connection' as 'Keep-Alive' for persistent connection;
        conn.setRequestProperty("Connection","Keep-Alive");

        fileSize=conn.getContentLength();
        conn.disconnect();
        int currentPartSize=fileSize/threadNum+1;
        // create local file
        RandomAccessFile file=new RandomAccessFile(targetFile,"rw");
        System.out.println(fileSize);
        file.setLength(fileSize);
        file.close();
        for (int i=0;i<threadNum;i++){
            int startPos=i*currentPartSize;
            RandomAccessFile currentPart=new RandomAccessFile(targetFile,"rw");
            currentPart.seek(startPos);
            threads[i]=new WorkerThread(startPos,currentPartSize,currentPart);
            threads[i].start();
        }
    }

    public int getLength(){
        return threads[threadNum-1].length;
    }

    // aim to get rate of download process
    public double getCompleteRate(){
        int sumSize=0;
        for (int i=0;i<threadNum;i++){
            sumSize=sumSize+threads[i].length;
        }
        return sumSize*100/fileSize*1.0;
    }


    class WorkerThread extends Thread{
        /*
        * startPos -> int:  onset of currentPart
        * currentPartSize -> int : length of currentPart
        * currentPart -> Object RandomAccessFile : current thread's working part within target file
        * */
        private int startPos;

        private int currentPartSize;

        private RandomAccessFile currentPart;
        public int length;

        public WorkerThread(int startPos, int currentPartSize, RandomAccessFile currentPart){
            this.startPos=startPos;
            this.currentPartSize=currentPartSize;
            this.currentPart=currentPart;
        }

        /*
        * Working thread skip previous occupied part through acknowledging currentPart and the number of previous thread;
        * Use InputStream to buffer data to be downloaded;
        * Exit till all threads finish their job.
        * */
        public void run(){
            try
            {
                HttpURLConnection conn = getConn();
                InputStream inStream=conn.getInputStream();
                inStream.skip(this.startPos);
                byte[] buffer=new byte[1024];
                int hasRead=0;

                while(length<currentPartSize&&(hasRead=inStream.read(buffer))!=-1){
                    currentPart.write(buffer,0, hasRead);
                    length += hasRead;
                }
                currentPart.close();
                inStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


