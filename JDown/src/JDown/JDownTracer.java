package JDown;

import javax.swing.text.html.HTMLDocument;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

class JDownTracer {
    private String logFileName; // 下载的文件的名字
    private Properties log;

    /**
     * 重新开始下载时，使用该构造函数
     * @param logFileName
     */


    JDownTracer(String logFileName, String url, int threadCount) {
        this.logFileName = logFileName + ".properties";
        this.log = new Properties();
        log.put("url", url);
        log.put("hasDown", "0");
        log.put("threadNum", String.valueOf(threadCount));
        for (int i = 0; i < threadCount; i++) {
            log.put("thread_" + i, "0-0");
        }
    }

    static void load(JDownTracer tracer) {
        tracer.log = new Properties();
        try {
            FileInputStream fs = new FileInputStream(tracer.logFileName);
            tracer.log.load(fs);
            fs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    synchronized void update(int threadID, long length, long pos, long limit) {
        log.put("thread_"+threadID, pos + "-" + limit);
        log.put("hasDown", String.valueOf(length + Long.parseLong(log.getProperty("hasDown"))));

        try {
            FileOutputStream file = new FileOutputStream(logFileName); // 每次写时都清空文件
            log.store(file, null);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取区间信息
     *      ret[i][0] = threadID, ret[i][1] = lowerBoundID, ret[i][2] = upperBoundID
     * @return
     */
    long[][] getRange() {
        long[][] range = new long[Integer.parseInt(log.get("threadNum").toString())][3];
        int[] index = {0};
        log.forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith("thread_")) {
                String[] interval = v.toString().split("-");
                range[index[0]][0] = Long.parseLong(key.substring(key.indexOf("_") + 1));
                range[index[0]][1] = Long.parseLong(interval[0]);
                range[index[0]++][2] = Long.parseLong(interval[1]);
            }
        });
        return range;
    }
    long getHasDown() {
        return Long.parseLong(log.getProperty("hasDown"));
    }
}