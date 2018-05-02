package test.lock;

public class ShareResource {

	static int  count = 0;
	public static int add(){
		count++;
		return count;
	}
}
