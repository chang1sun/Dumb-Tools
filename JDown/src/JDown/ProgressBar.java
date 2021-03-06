package JDown;

public class ProgressBar {
    private static final String FINISHED_SIGN = "█";
    private static final String UNFINISHED_SIGN = "-";
    private static final int LENGTH = 25;

    public static String bar(int cut){
        return FINISHED_SIGN.repeat(Math.max(0, cut)) + UNFINISHED_SIGN.repeat(Math.max(0, LENGTH - (cut + 1)));
    }

    public static void flush(){
        System.out.print("\b".repeat(LENGTH + 38));
    }

}
