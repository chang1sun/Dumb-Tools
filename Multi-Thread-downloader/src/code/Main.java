package code;

import javax.swing.plaf.synth.SynthOptionPaneUI;

public class Main {
    public static void main(String[] args) throws Exception {
        String b = "https://www.baidu.com/img/bd_logo1.png";
        //初始化DownUtil对象
        String path = "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=1870521716,857441283&fm=26&gp=0.jpg";
        final Down down=new Down(path,"target.png",5);
        //开始下载
        down.download();
        new Thread(()->{
            while (true){
                //每隔0.01秒查询一次任务的完成进度
                System.out.printf("已完成：%.2f%%\n", down.getCompleteRate());
                try {
                    if(down.getCompleteRate() >= 100) break;
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();
    }
}
