package dfd.pbol.utils;

public class BarrierMonitorLock {
	
	int pendingProcess = 0;
	int MAX;
	
	/**
	 * A simple barrier that breaks after n process enters
	 * @param n
	 */
	public BarrierMonitorLock(int n) {
		MAX = n;
	}
	
	public synchronized void enter() throws InterruptedException
	{
		pendingProcess++;
		if(pendingProcess < MAX){
			//System.out.println("New waiting " + pendingProcess);
			wait();
			}
		else{
			notifyAll();
		}
	}
	
	public synchronized void waitBreak() throws InterruptedException
	{
		if(pendingProcess < MAX)
			wait();
		pendingProcess = 0;
		//System.out.println("IT BROKE!!" + pendingProcess);
	}
	
	/**
	 * Return true in no thread is in the barrier
	 * @return
	 */
	public synchronized boolean notBusy()
	{
		if(pendingProcess == MAX){
			pendingProcess = 0;
			//System.out.println("X-");
			return true;
			}
		return false;
	}

}
