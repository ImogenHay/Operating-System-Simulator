/**
 * 
 */
package org.os.com1032.ih00264;

import java.util.ArrayList;

/**
 * Decodes and executes instructions for CPUs either from user input or process programs
 * 
 * @author ih00264
 *
 */
public class Instructions {

	private Bank theBank = null;
	private MemoryManagementUnit mmu = null;
	private ProcessCreationThread pc = null;

	/**
	 * @param instruction
	 */
	public Instructions(Bank theBank, MemoryManagementUnit mmu, ProcessCreationThread pc) {
		super();
		this.theBank = theBank;
		this.mmu = mmu;
		this.pc = pc;
		
	}

	
	public void decode(ArrayList<String> instructions) {
		for(String instruction: instructions) { //for every instruction in program
			String[] split = instruction.split(" ");
			if (instruction.contains("ADD")) { //adds two inputs together
				int answer = this.add(Integer.parseInt(split[0]), Integer.parseInt(split[2]));
				System.out.print(instruction + " = " + answer + "\n");
			}
			
			else if (instruction.contains("SUB")) {
				int answer = this.subtract(Integer.parseInt(split[0]), Integer.parseInt(split[2])); //subtracts one input from another
				System.out.print(instruction + " = " + answer + "\n");
			}
			
			else if (instruction.contains("RQ")) { //request resources for customer
				int[] requests = this.getRequests(split);	
				if(this.theBank.requestResources(Integer.parseInt(split[1]), requests)) {
					System.out.println(" Approved\n");
				}
				else {
					System.out.println(" Denied\n");
				}				
			}
			
			else if (instruction.contains("RL")) { //release resources from customer
				int[] requests = this.getRequests(split);	
				this.theBank.releaseResources(Integer.parseInt(split[1]), requests);
				System.out.println(" Completed\n");
			}
			
			else if (instruction.contains("*")) { //display current resource state
				System.out.println("\n[Resources] Current Resource State: ");
				this.theBank.getState();
			}
			
			else if (instruction.contains("READ")) { //read physical address of virtual address
				this.mmu.getPhysicalAddress(Integer.parseInt(split[1]));
				System.out.println();
			}
			
			else if (instruction.contains("NEW")) { //creates new process and add to background queue
				int id = Integer.parseInt(split[1]);
				float at = Float.parseFloat(split[2]);
				float ert = Float.parseFloat(split[3]);
				int pr = Integer.parseInt(split[4]);
				int si = Integer.parseInt(split[5]);
				PCB newprocess;
				try {
					newprocess = new PCB(id, at, ert, pr, si);
					pc.addProcess(newprocess);
					System.out.println("\n[ProcessCreationThread] Added new process "+id + "\n");
				} catch (PSSexception e) {
					System.out.println("\n[ProcessCreationThread] Error, could not create process");
					e.printStackTrace();
				}			
			}
			
			else {
				System.out.println("Invalid Instruction");
			}
		}
	}
	
	public int[] getRequests(String[] instruction) { //used to separate instruction into list of resources needed to parse to bank
		int numberOfResources = instruction.length-2;
		int[] requests = new int[numberOfResources];
		int start = 2;
		for (int i=0; i<instruction.length-2; i++) {
			requests[i] = Integer.parseInt(instruction[start]);
			start++;
		}
		return requests;
	}
	
	
	
	public int add(int input1, int input2) {
		return input1 + input2;
	}
	
	
	
	public int subtract(int input1, int input2) {
		return input1 - input2;
	}
	
}
