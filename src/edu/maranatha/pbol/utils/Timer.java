package dfd.pbol.utils;

public class Timer {
	
	long setted,current=0;
	long min = 99999999;
	long max = 0;
	long count = 0;
	long sum = 0;
	long avg;
	
	public void start() {setted = System.nanoTime(); count++;}
	public void stop() {
		current = System.nanoTime() - setted;
		sum += current;
		avg = sum/count;
		if(count > 100)
		{
			count = 0;
			sum = 0;
		}
		if(current > max)
			max = current;
		if(current < min)
			min = current;
		}
	public long getMax() {return max;}
	public long getMin() {return min;}
	public long getAvg() {return avg;}
}
