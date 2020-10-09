/**
 * 
 */
package org.os.com1032.ih00264;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main method to simulate OS booting by loading kernel image, 
 * reading from configuration file and initialising data structures used.
 * 
 * @author ih00264
 *
 */
public class MainBooting {
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String dir = System.getProperty("user.dir"); //gets directory of project to use to get input files
		System.out.println("------------------- OS OPTIONS -------------------");
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in)); //for reading user input
		System.out.print("Choose Configuration File: (e.g. config1.txt) "); 
		String config = input.readLine(); //different config files will show different parts of OS e.g. different algorithms
		Scanner in;
		try {
			in = new Scanner(new File(dir + "/" + config));			
			System.out.print("Choose Page Replacement Algorithm: (lru/fifo) "); 
			String pageReplacmentAlgorithm = input.readLine();
			System.out.print("Choose Process Scheduling Algorithm: (0-6) "); //see what each number represents in DispatcherThread class
			int schedulingAlgorithm = Integer.parseInt(input.readLine());
			
			String processFile;
			String resourceFile;
			
			char inputNumber = config.charAt(6); //id to indicate which input files to use
			processFile = "ProcessInputSample" + inputNumber + ".txt";
			resourceFile = "ResourceInputSample" + inputNumber + ".txt";

			
			String skip = "[A-Z0-9_]*:"; //to skip comments in config file
			
			// INITIALISE MMU
			in.next(skip);		
			final int RAM = in.nextInt();
			in.next(skip);
			final int FRAME_SIZE = in.nextInt();
			in.next(skip);
			final int PAGE_SIZE = in.nextInt();
			in.next(skip);
			final int NUMBER_OF_PAGES = in.nextInt();
			in.next(skip);
			final int TLB_SIZE = in.nextInt(); //reach = TLB_SIZE * PAGE_SIZE = 4*4 = 16

			
			MemoryManagementUnit MMU = new MemoryManagementUnit(RAM, FRAME_SIZE, PAGE_SIZE, NUMBER_OF_PAGES, TLB_SIZE, pageReplacmentAlgorithm);
			System.out.println("\n[RandomAccessMemory] Main memory created of size " + RAM);
			System.out.println("[MemoryManagementUnit] Virtual memory created of size " + PAGE_SIZE*NUMBER_OF_PAGES);
			System.out.println("[MemoryManagementUnit] TLB Cache created of size " + TLB_SIZE);
		
			
			// INITIALISE BANKERS ALGORITHM
			in.next(skip);
			int numOfResources = in.nextInt();
			int[] initialResources= new int[numOfResources]; //initial number of resources
			
			for(int i = 0; i<numOfResources; i++) { //resources involved in the transaction
				in.next(skip);
				initialResources[i] = in.nextInt();;
			}
		
	        int[] maxDemand = new int[numOfResources];
			       
	        String line;
	        Bank theBank;
			try {
				BufferedReader inFile = new BufferedReader(new FileReader(dir + "/" + resourceFile)); //to read initial resource needs

				int threadNum = 0;
	               int resourceNum = 0;
	               
	               inFile.readLine(); //to skip comments
	               inFile.readLine();
	               int number_of_customers = Integer.parseInt(inFile.readLine());
	               theBank = new BankImpl(initialResources, number_of_customers);
	               inFile.readLine();

	               for (int i = 0; i < number_of_customers; i++) {
	               	line = inFile.readLine();
	               	StringTokenizer tokens = new StringTokenizer(line,",");
	                            
	               	while (tokens.hasMoreTokens()) {
	               		int amt = Integer.parseInt(tokens.nextToken().trim());
	               		maxDemand[resourceNum++] = amt;
	               	}

	               	theBank.addCustomer(threadNum,maxDemand);
	               	++threadNum;
	               	resourceNum = 0;
	               }
	          }
			catch (FileNotFoundException fnfe) {
				throw new Error("[Resources] Unable to find resource file ");
			}
			catch (IOException ioe) {
				throw new Error("[Resources] Error processing ");
			}
			
			System.out.println("\n[Resources] Initial Resource State: ");
			theBank.getState();
			
			
				
			
			// INITIALISE PROCESS CREATION THREAD
			in.next(skip);
			final int buffer_size = in.nextInt();
			
			
			ReentrantLock lock =new ReentrantLock(); // owned by the thread last successfully locking, but not yet unlocking it
			Condition conBackground= lock.newCondition();
			Vector<PCB> background_queue = new Vector<PCB>();

			System.out.println("[ProcessCreationThread] Initialised background queue ");
			System.out.println("[ProcessCreationThread] Reading processes ");
			
			ProcessCreationThread pct = new ProcessCreationThread(lock, conBackground, background_queue, buffer_size);
			try {
				pct.Read(dir + "/" + processFile);
			} catch (PSSexception e) {
				System.out.println("[ProcessCreationThread] Invalid Process File ");
				e.printStackTrace();
			}
			
			
			
			// INITIALISE DISPATCHER THREAD
			in.next(skip);
			final int context_switch  = in.nextInt(); 
			in.next(skip);
			final int time_quantum = in.nextInt();
			
			
			Condition conRunning= lock.newCondition();
			Vector<PCB> running_buffer = new Vector<PCB>();
			DispatcherThread processScheduler = new DispatcherThread(context_switch, time_quantum, MMU, lock, conBackground, conRunning, background_queue, running_buffer, schedulingAlgorithm);
			
			System.out.println("[DispatcherThread] Initialised running buffer \n");
			
			
			
			// INITIALISE CPUS
			in.next(skip);
			final String cpu1_freq  = in.next("[a-z]*") ;
			in.next(skip);
			final String cpu2_freq = in.next("[a-z]*") ;

			Instructions inst = new Instructions(theBank, MMU, pct);
			FirstProcessorThread CPU1 = new FirstProcessorThread(cpu1_freq, lock, conRunning, running_buffer, MMU, inst);
			SecondProcessorThread CPU2 = new SecondProcessorThread(cpu2_freq, lock, conRunning, running_buffer, MMU, inst);
			
			
			CPU1.setPriority(1); //so that moving processes to buffer is prioritised over processing otherwise nothing to process
			CPU2.setPriority(1);
					
			pct.start();
			processScheduler.start();
			CPU1.start();
			CPU2.start();
			
			try {
				processScheduler.join(); //once current processes all dispatched
				boolean exit = false;
				while(!exit) { //until exit entered
					System.out.println("[Input] Enter Instruction: \n"); //check for input
					String newimput = input.readLine();
					if (newimput.toLowerCase().contains("exit")) {
						exit = true;
					}
					else {
						ArrayList<String> instruction = new ArrayList<String>(); //process input
						instruction.add(newimput);
						inst.decode(instruction);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("[ERROR] Invalid Configuration File ");
			e.printStackTrace();
		}
		

	}
}


