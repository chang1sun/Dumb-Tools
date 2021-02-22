package code;
/*
 * a simple implementation of Tuple
 * For method 'get',
 * params: idx = 0 -> first element;
 *               otherwise -> second element;
 * returns: the element itself.
 * */
public class Pair<T>{
    private final T x;
    private final T y;
    public Pair(T x, T y){
        this.x = x;
        this.y = y;
    }
    public T get(int idx){
        return idx==0 ? x : y;
    }
}