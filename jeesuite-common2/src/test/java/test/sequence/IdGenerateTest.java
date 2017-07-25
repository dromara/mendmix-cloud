package test.sequence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.jeesuite.common2.sequence.SnowflakeGenerator;

public class IdGenerateTest {

	public static void main(String[] args) {
		
		long start = System.currentTimeMillis();
		
		SnowflakeGenerator generator = new SnowflakeGenerator(1,1);
		
		AtomicInteger count = new AtomicInteger(0);
		
		ExecutorService service = Executors.newFixedThreadPool(100);
		
		while(true){	
			if(System.currentTimeMillis() - start > 1000){
				System.out.println(count.get());
				break;
			}
			service.execute(new Runnable() {
				
				@Override
				public void run() {
					generator.nextId();
					count.incrementAndGet();
				}
			});
		}

	}

}
