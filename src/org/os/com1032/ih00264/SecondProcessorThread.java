/**
 * 
 */
package org.os.com1032.ih00264;

import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread to simulate fetch, decode, execute cycle of a processor 
 * 
 * @author ih00264
 */
public class SecondProcessorThread extends Thread {
	String frequency;
	ReentrantLock lock;
	Condition con;
	Vector<PCB> running_buffer; //synchronised
	MemoryManagementUnit mmu; //both processors share memory
	Instructions decodeExecute;
	int burstTime; //CPU burst time is count of instructions
	int clock; //increases as burst time increases by factor depending on frequency of CPU
	int overflow;
	
	public SecondProcessorThread (String frequency, ReentrantLock lock, Condition con, Vector<PCB> running_buffer, MemoryManagementUnit mmu, Instructions decodeExecute) {
		this.frequency = frequency;
		this.lock = lock;
		this.con = con; 
		this.running_buffer = running_buffer;
		this.mmu = mmu;
		this.decodeExecute = decodeExecute;
	}
	
	@Override
	public void run () {
		 while(true) {
			 	if(!lock.isHeldByCurrentThread()) {
				 	lock.lock();
			 	}
			 	if (!this.running_buffer.isEmpty()) { //until no processes left to process
				 	this.processing(this.running_buffer.remove(0)); //executes first process in buffer and removes it from buffer
			 	}
				con.signalAll(); //wakes up one waiting threads
				while(this.running_buffer.isEmpty()) {
					try {
						con.await(); //waits until more processes to process
					} catch (InterruptedException e) {
						System.out.println("[CPU2] Can't await");
						e.printStackTrace();
					} 
				}
		 }
	}
	
	
	public int getBurstTime() {
		return this.burstTime;
	}
	
	
	public void updateClock(int requestedCPUburst) { //depends on processor frequency
		if(this.frequency.equals("fast")) {
			this.incrementClock((int) (requestedCPUburst/2.0));
		}
		else if(this.frequency.equals("slow")) {
			this.incrementClock(requestedCPUburst*2);
		}
		else {
			this.incrementClock(requestedCPUburst);
		}
	}
	
	
	public void incrementClock(int value) {
		while(value != 0) {
			clock++;
			if (clock >= Integer.MAX_VALUE - 1) {
				clock = 0;
				overflow ++;
				}
			value--;
		}
	}
	
	
	
	public void processing (PCB program) { //fetch decode execute cycle
		System.out.println("[CPU2] Fetching Process " + program.getID() + "s program\n");
		mmu.read(program); //reads program from memory, moving from virtual to physical if not currently there to simulate fetching instructions
		int instructionCount = program.getProgram().size();
		if (instructionCount > 0) { //if process has program
			System.out.println("[CPU2] Decoding Process " + program.getID() + "s program\n");
			System.out.println("---------------------------------- PROGRAM " +  program.getID() + " ----------------------------------");
			decodeExecute.decode(program.getProgram()); //Instruction class used to decode and execute instruction
			System.out.println("--------------------------------------------------------------------------------\n");
			program.executed();
			System.out.println("[CPU2] Executed Process " + program.getID() + "s program\n");
			this.burstTime+= instructionCount; //CPU burst is count of instructions
			this.updateClock(instructionCount);
			System.out.println("[CPU2] Clock = " + this.clock + "\n");
		}
	}
}
