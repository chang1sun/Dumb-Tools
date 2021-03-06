package JDown;


import java.io.*;
import java.util.Properties;

class JDownTracer {
    private String logFileName;
    private Properties properties;


    JDownTracer(String logFileName, String url, int threadNum) {
        this.logFileName = logFileName + ".properties";
        this.properties = new Properties();
        properties.put("url", url);
        properties.put("hasDown", "0");
        properties.put("threadNum", String.valueOf(threadNum));
        for (int i = 0; i < threadNum; i++) {
            properties.put("thread_" + i, "0-0");
        }
    }

    static void load(JDownTracer tracer) {
        tracer.properties = new Properties();
        try {
            FileInputStream fs = new FileInputStream(tracer.logFileName);
            tracer.properties.load(fs);
            // close stream in time in case holding the file source too long and thus affect other calls' request.
            fs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    synchronized void update(int threadID, long length, long pos, long limit) {
        properties.put("thread_"+threadID, pos + "-" + limit);
        properties.put("hasDown", String.valueOf(length + Long.parseLong(properties.getProperty("hasDown"))));

        try {
            FileOutputStream file = new FileOutputStream(logFileName);
            properties.store(file, null);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * 2d-array storing pos and limit for all threads.
     */
    long[][] getRange() {
        long[][] range = new long[getThreadNum()][2];
        int threadNum = getThreadNum();
        for(int i = 0; i < threadNum; i++){
            String[] val = properties.getProperty(String.format("thread_%d", i)).split("-");
            range[i][0] = Long.parseLong(val[0]);
            range[i][1] = Long.parseLong(val[1]);
        }
        return range;
    }
    long getHasDown() {
        return Long.parseLong(properties.getProperty("hasDown"));
    }
    int getThreadNum(){
        return Integer.parseInt(properties.getProperty("threadNum"));
    }
}