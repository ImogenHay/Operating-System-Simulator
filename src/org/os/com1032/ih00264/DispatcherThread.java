package org.os.com1032.ih00264;

/**
 * Class for scheduling different processes depending on type of scheduling algorithm used
 * 
 * Adapted from MMayla's ProcessScheduler class from git-hub Process-Scheduling-Simulator
 */

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class DispatcherThread extends Thread{

	private float contextswitching;
	private float timequantum; //fixed time to execute (e.g. for RR)
	public Vector<Event> eventlist;
	public Vector<PCB> processlist; //finished processes
	private boolean fifo; // to choose between using of normal ready_queue or fifo ready queue	
	private int algorithm;
	
	 ReentrantLock lock; //variables for synchronising system
	 Condition conBackground;
	 Condition conRunning;
	 Vector<PCB> input_queue; //variables to be synchronised
	 Vector<PCB> running_buffer;

	// queues
	public ReadyQueueComparator RQC; //so processes can be compared
	public PriorityQueue<PCB> background_queue; // set by process creation thread
	private PriorityQueue<PCB> ready_queue;
	private Queue<PCB> ready_queue_fifo; // to initialise use:
												// ready_queue_fifo = new
												// LinkedList<Process>();
	private MemoryManagementUnit MMU; //both processors share memory and schedular

	
	private void _constructor(float c, float tq, MemoryManagementUnit MMU, ReentrantLock lock, Condition conBackground, Condition conRunning, Vector<PCB> input_queue, Vector<PCB> running_buffer, int algorithm) {
		contextswitching = c;
		timequantum = tq;

		// Initialize the comparator
		RQC = new ReadyQueueComparator(
				ReadyQueueComparator.queueType.background);

		// Initialize back_ground queue
		background_queue = new PriorityQueue<PCB>(50,
				new ReadyQueueComparator(
						ReadyQueueComparator.queueType.background));

		// Initialize event list
		eventlist = new Vector<Event>();

		// Initialize process list
		processlist = new Vector<PCB>();
		
		this.MMU = MMU;
		this.lock = lock;
		this.conBackground = conBackground;
		this.conRunning = conRunning;
		this.input_queue  = input_queue;
		this.running_buffer = running_buffer; 
		this.algorithm = algorithm;
	}
	
	

	@Override
	public void run () {
			 	lock.lock(); //thread obtains lock
			 	while (this.input_queue.isEmpty()) { //while nothing new to schedule
			 		try {
			 			conBackground.await(); //interrupts thread causing it to wait if nothing new to add
			 		} catch (InterruptedException ex) {
			 			System.out.println("[DispatcherThread] Error, thread can't await\n");
			 			ex.printStackTrace();
			 		}
			 	}
				try {
					this.set_BackGroundQueue(input_queue); //adds initial processes from buffer to be scheduled
				} catch (PSSexception e) {
					System.out.println("[DispatcherThread] Error, invalid input queue\n");
					e.printStackTrace();
				}
				System.out.println("[DispatcherThread] Added new PCBs to queue\n");
				this.input_queue.clear(); // clears input buffer so more processes can be added
				lock.unlock(); 
				this.runAlgorithm(algorithm); //runs scheduling algorithm on processes	
				this.printEventList();
				lock.lock();
				conBackground.signal(); //wakes up process creation thread to wait for more input
				lock.unlock();
	}
	
	
	
	private void clearAll() {
		eventlist.clear();
		processlist.clear();
	}

	
	
	private void incrementPriority_ReadyQueue(int dpriority) {
		PCB uproc;
		Vector<PCB> saveprocess = new Vector<PCB>();
		while (!ready_queue.isEmpty()) {
			uproc = ready_queue.poll(); //removes head of queue and returns it, or null if this queue is empty
			uproc.priority += dpriority;//increases priority of each process by dpriority
			saveprocess.add(uproc);
		}

		for (int i = 0; i < saveprocess.size(); i++) {
			ready_queue.add(saveprocess.elementAt(i));//puts updated elements back into ready queue
		}
	}

	
	//multiple constructors, depends on if time quantum is inputed
	public DispatcherThread(float c, float tq, MemoryManagementUnit MMU, ReentrantLock lock, Condition con, Condition conC, Vector<PCB> input_queue, Vector<PCB> running_buffer, int algorithm) {
		_constructor(c, tq, MMU, lock, con, conC, input_queue, running_buffer, algorithm);
	}

	
	public DispatcherThread(float c, MemoryManagementUnit MMU, ReentrantLock lock, Condition con, Condition conC, Vector<PCB> input_queue, Vector<PCB> running_buffer, int algorithm) {
		_constructor(c, 0, MMU, lock, con, conC, input_queue, running_buffer, algorithm); //default timequantum is 0
	}

	
	//puts PCBs from input buffer into background queue to be scheduled
	public void set_BackGroundQueue(Vector<PCB> plist) throws PSSexception {
		for (PCB newproc : plist) {
			if(!this.background_queue.contains(newproc)) {
				background_queue.add(newproc);
			}
		}
	}

	
	
	public void setTimeQuantum(float tq) {
		timequantum = tq;
	}

	public void setContextSwitching(float c) {
		contextswitching = c;
	}

	public void addEvent(int id, float t, Event.eventtype et) {
		eventlist.add(new Event(id, t, et));
	}
	
	

	
	
	public void updateReady(float time) {//updates ready queue if enough time for element in background queue
		
		lock.lock();
		conBackground.signal(); //signals to process creation thread to check for more processes
		conBackground.awaitUninterruptibly();
		if (!this.input_queue.isEmpty()) { //if new processes found add from buffer to queue
			try {
				this.set_BackGroundQueue(input_queue);
			} catch (PSSexception e) {
				System.out.println("[DispatcherThread] Error, invalid input queue\n");
				e.printStackTrace();
			}
			System.out.println("[DispatcherThread] Added new PCBs to queue \n");
			input_queue.clear();
		}
		lock.unlock();

		
		Queue<PCB> RQ;
		if (fifo)
			RQ = ready_queue_fifo;
		else
			RQ = ready_queue;

		while (true) {
			if (background_queue.isEmpty())
				break;

			if (background_queue.element().arrivaltime <= time){
				// add to ready queue
				if(MMU.allocateMemory(background_queue.element())) { //allocates processes program to memory
					RQ.add(background_queue.element());

					// add arriving event
					addEvent(background_queue.element().ID,
							background_queue.element().arrivaltime,					
							Event.eventtype.arrived);

				} 
				// remove the head of background queue
				background_queue.poll();

			} else
				break;
		}
		
		
	}

	
	
	public void terminateProcess(PCB p, float time) {
		addEvent(p.ID, time, Event.eventtype.terminated);
		MMU.deallocateMemory(p); //deallocates processes program from memory
		p.FinishTime = time;
		processlist.add(p); //add to list of finished processes
	}

	
	
	//scheduling algorithms
	public void runAlgorithm(int index){
		switch (index)
		{
		case 0:
			this.FCFS();
			break;
		case 1:
			this.SJF();
			break;
		case 2:
			this.RR();
			break;
		case 3:
			this.HPFSN();
			break;
		case 4:
			this.HPFSP();
			break;
		case 5:
			this.HPFD();
			break;
		case 6:
			this.WRR();
			break;
		}
	}
	
	public void update_running_buffer(PCB process) {	
		this.running_buffer.add(process); //to send process to a processor to be executed
		System.out.println("[DispatcherThread] Updated running buffer\n");
		lock.lock();
		while(!this.running_buffer.isEmpty()) {
			conRunning.signalAll(); //signals to both CPUs that a new process need to be processed
			try {
				conRunning.await();
			} catch (InterruptedException e) {
				System.out.println("[DispatcherThread] Error, thread can't await\n");
				e.printStackTrace();
			}
		}
		lock.unlock();
	}

	
	
	
	public void FCFS(){
		// clear all lists
		clearAll();

		// use normal ready_queue
		fifo = false;

		// set the comparator type for the algorithm (FCFS)
		RQC.setType(ReadyQueueComparator.queueType.FCFS);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		// update ready queue
		updateReady(current_time);

		while (true){
			if (!ready_queue.isEmpty()){
				// add scheduling event
				addEvent(ready_queue.element().ID, current_time, Event.eventtype.scheduled);
				this.update_running_buffer(ready_queue.element());

				// update time
				current_time += ready_queue.element().expectedruntime;

				// update the ready queue + add the arrived elements event
				updateReady(current_time);

				// add termination event of current process
				terminateProcess(ready_queue.element(), current_time);
				// remove from read_queue
				ready_queue.poll();

				// add context switching overhead time
				if (!ready_queue.isEmpty())
					current_time += contextswitching;

			} else {
				// if there is no processes in the background queue
				if (background_queue.isEmpty())
					break;

				// update the current time to time of the head of background
				// queue
				current_time = background_queue.element().arrivaltime;

				// update ready queue
				updateReady(current_time);
			}
		}
	}

	
	
	
	public void SJF() {
		// clear all lists
		clearAll();

		// use normal ready_queue
		fifo = false;

		// set the comparator type for the algorithm (FCFS)
		RQC.setType(ReadyQueueComparator.queueType.SJF);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		PCB running_process;
		// update ready queue
		updateReady(current_time);

		while (true) {
			if (!ready_queue.isEmpty()) {
				// run the take out of ready_queue
				running_process = ready_queue.poll();


				// add scheduling event
				addEvent(running_process.ID, current_time,Event.eventtype.scheduled);
				this.update_running_buffer(running_process); 

				// update time
				current_time += running_process.expectedruntime;

				// update the ready queue + add the arrived elements event
				updateReady(current_time);

				// add termination event of current process
				terminateProcess(running_process, current_time);

				// add context switching overhead time
				if (!ready_queue.isEmpty())
					current_time += contextswitching;

			} else {
				// if there is no processes in the background queue
				if (background_queue.isEmpty())
					return;

				// update the current time to time of the head of background
				// queue
				current_time = background_queue.element().arrivaltime;

				// update ready queue
				updateReady(current_time);
			}
		}
	}

	
	
	
	public void RR() {
		// clear all lists
		clearAll();

		// use FIFO ready_queue
		fifo = true;

		// Initialize the ready_queue with the comparator
		ready_queue_fifo = new LinkedList<PCB>();

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		// update ready queue
		updateReady(current_time);

		PCB running_process = ready_queue_fifo.poll();
		addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
		this.update_running_buffer(running_process); 

		while (true){
			// the process will not finish
			if (running_process.currentruntime + timequantum < running_process.expectedruntime){
				current_time += timequantum;
				running_process.currentruntime += timequantum;

				// update ready queue
				updateReady(current_time);

				if (!ready_queue_fifo.isEmpty())
				{
					addEvent(running_process.ID, current_time, Event.eventtype.preempted);
					ready_queue_fifo.add(running_process);
					running_process = null;

					// add context switching overhead time
					if (!ready_queue_fifo.isEmpty())
						current_time += contextswitching;
				}
			} else {
				// the time that process need to finish
				float dif = running_process.expectedruntime - running_process.currentruntime;
				current_time += dif;
				terminateProcess(running_process, current_time);
				// update ready queue
				updateReady(current_time);

				// add context switching overhead time
				if (!ready_queue_fifo.isEmpty())
					current_time += contextswitching;

				running_process = null;
			}

			// new time slice or finish
			if (ready_queue_fifo.isEmpty() && running_process == null) {
				if (background_queue.isEmpty()) // finished
					break;
				else {
					// update the current time to time of the head of background
					// queue
					current_time = background_queue.element().arrivaltime;

					// update ready queue
					updateReady(current_time);
				}
			}

			// get next process
			if (running_process == null){
				running_process = ready_queue_fifo.poll();
				addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
				this.update_running_buffer(running_process); //reads processes program from memory
			}
		}
	}

	
	
	
	public void HPFSN() {
		// clear all lists
		clearAll();

		fifo = false;

		// set the comparator type for the algorithm (HPFSN)
		RQC.setType(ReadyQueueComparator.queueType.HPFSN);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		PCB running_process;
		// update ready queue
		updateReady(current_time);

		while (true){
			if (!ready_queue.isEmpty()) {
				// run the take out of ready_queue
				running_process = ready_queue.poll();

				// add scheduling event
				addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
				this.update_running_buffer(running_process); //reads processes program from memory

				// update time
				current_time += running_process.expectedruntime;

				// update the ready queue + add the arrived elements event
				updateReady(current_time);

				// add termination event of current process
				terminateProcess(running_process, current_time);

				// add context switching overhead time
				if (!ready_queue.isEmpty())
					current_time += contextswitching;

			} else {
				// if there is no processes in the background queue
				if (background_queue.isEmpty())
					break;

				// update the current time to time of the head of background
				// queue
				current_time = background_queue.element().arrivaltime;

				// update ready queue
				updateReady(current_time);
			}
		}
	}

	
	
	
	public void d_HPFSP() {
		// clear all lists
		clearAll();
		fifo = false;

		// set the comparator type for the algorithm (HPFSP)
		RQC.setType(ReadyQueueComparator.queueType.HPFSP);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		PCB running_process;
		// update ready queue
		updateReady(current_time);

		float nextarrival = 0;

		while (true){
			if (!ready_queue.isEmpty()){
				running_process = ready_queue.poll();
				if (!background_queue.isEmpty())
					nextarrival = background_queue.peek().arrivaltime;
				else
					nextarrival = -1;

				float diff = running_process.expectedruntime - running_process.currentruntime;

				// the process will finish
				if (nextarrival != -1
						&& ((running_process.currentruntime + diff) <= nextarrival)){
					// terminate process
					terminateProcess(running_process, current_time + diff);

					current_time = nextarrival;

					// update ready
					updateReady(current_time);
				} else
				{
					running_process.currentruntime += nextarrival - running_process.currentruntime;

					if (nextarrival != -1)
						current_time = nextarrival;
					else
						current_time = diff + running_process.currentruntime;

					// update ready
					updateReady(current_time);

					if (ready_queue.peek().priority > running_process.priority) {
						addEvent(running_process.ID, current_time, Event.eventtype.preempted);
					}
				}

			}
		}
	}

	
	
	
	public void HPFSP()
	{
		// clear all lists
		clearAll();

		// use FIFO ready_queue
		fifo = false;

		// set the comparator type for the algorithm (HPFSP)
		RQC.setType(ReadyQueueComparator.queueType.HPFSP);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		// update ready queue
		updateReady(current_time);

		PCB running_process = ready_queue.poll();
		addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
		this.update_running_buffer(running_process);
		
		
		while (true) {
			// the process will not finish
			if (running_process.currentruntime + timequantum < running_process.expectedruntime) {
				current_time += timequantum;
				running_process.currentruntime += timequantum;

				// update ready queue
				updateReady(current_time);

				if (!ready_queue.isEmpty()) {
					addEvent(running_process.ID, current_time, Event.eventtype.preempted);
					ready_queue.add(running_process);
					running_process = null;

					// add context switching overhead time
					if (!ready_queue.isEmpty())
						current_time += contextswitching;
				}
			} else {
				// the time that process need to finish
				float dif = running_process.expectedruntime - running_process.currentruntime;
				current_time += dif;
				terminateProcess(running_process, current_time);
				// update ready queue
				updateReady(current_time);

				// add context switching overhead time
				if (!ready_queue.isEmpty())
					current_time += contextswitching;

				running_process = null;
			}

			// new time slice or finish
			if (ready_queue.isEmpty() && running_process == null) {
				if (background_queue.isEmpty()) // finished
					break;
				else {
					// update the current time to time of the head of background
					// queue
					current_time = background_queue.element().arrivaltime;

					// update ready queue
					updateReady(current_time);
				}
			}

			// get next process
			if (running_process == null) {
				running_process = ready_queue.poll();
				addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
				this.update_running_buffer(running_process); //reads processes program from memory
			}
		}
	}

	
	
	
	public void _HPFSP() {
		// clear all lists
		clearAll();

		fifo = false;

		// set the comparator type for the algorithm (HPFSP)
		RQC.setType(ReadyQueueComparator.queueType.HPFSP);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		PCB running_process;
		// update ready queue
		updateReady(current_time);

		while (true) {
			if (!ready_queue.isEmpty()) {
				// run the take out of ready_queue
				running_process = ready_queue.poll();

				// add scheduling event
				addEvent(running_process.ID, current_time,
						Event.eventtype.scheduled);
				this.update_running_buffer(running_process); 

				while (!background_queue.isEmpty()) {
					if ((current_time + (running_process.expectedruntime - running_process.currentruntime)) < (background_queue.element().arrivaltime)) {
						current_time += running_process.expectedruntime - running_process.currentruntime;
						updateReady(current_time);

						// add termination event of current process
						terminateProcess(running_process, current_time);
					}

					else
					{
						running_process.currentruntime += (background_queue.element().arrivaltime - current_time);
						current_time = background_queue.element().arrivaltime;
						updateReady(current_time);
						if (ready_queue.element().priority > running_process.priority){
							ready_queue.add(running_process);
							addEvent(running_process.ID, current_time,Event.eventtype.preempted);

							// add context switching overhead time
							if (!ready_queue.isEmpty())
								current_time += contextswitching;

							running_process = ready_queue.poll();
							addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
							this.update_running_buffer(running_process); 
							// update time
							current_time += running_process.expectedruntime;
							// update the ready queue + add the arrived elements
							// event
							// updateReady(current_time);

							// add termination event of current process
							// addEvent(running_process.ID,
							// current_time,Event.eventtype.terminated);

						}
					}
				}
				// update time
				current_time += running_process.expectedruntime;

				// update the ready queue + add the arrived elements event
				updateReady(current_time);

				// add termination event of current process
				terminateProcess(running_process, current_time);

				// add context switching overhead time
				if (!ready_queue.isEmpty())
					current_time += contextswitching;

			} else
			{
				// if there is no processes in the background queue
				if (background_queue.isEmpty())
					break;

				// update the current time to time of the head of background
				// queue
				current_time = background_queue.element().arrivaltime;

				// update ready queue
				updateReady(current_time);
			}
		}
	}

	
	
	
	public void HPFD()
	{
		// clear all lists
		clearAll();

		// use FIFO ready_queue
		fifo = false;

		// dynamic time quantum
		int dtimequantum = 1;

		// set the comparator type for the algorithm (HPFSP)
		RQC.setType(ReadyQueueComparator.queueType.HPFD);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		// update ready queue
		updateReady(current_time);

		PCB running_process = ready_queue.poll();
		addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
		this.update_running_buffer(running_process); 

		while (true) {
			// the process will not finish
			if (running_process.currentruntime + dtimequantum < running_process.expectedruntime) {
				current_time += dtimequantum;
				running_process.currentruntime += dtimequantum;

				// update ready queue
				updateReady(current_time);

				// change priority
				incrementPriority_ReadyQueue(1);
				running_process.priority = running_process.priority == 0 ? running_process.priority : running_process.priority - 1;

				if (!ready_queue.isEmpty() && (ready_queue.peek().priority > running_process.priority)) {
					addEvent(running_process.ID, current_time, Event.eventtype.preempted);
					ready_queue.add(running_process);
					running_process = null;

					// add context switching overhead time
					if (!ready_queue.isEmpty())
						current_time += contextswitching;
				}
			} else {
				// the time that process need to finish
				float dif = running_process.expectedruntime - running_process.currentruntime;
				current_time += dif;
				terminateProcess(running_process, current_time);

				// update ready queue
				updateReady(current_time);

				// change priority
				incrementPriority_ReadyQueue(1);

				// add context switching overhead time
				if (!ready_queue.isEmpty())
					current_time += contextswitching;

				running_process = null;
			}

			// new time slice or finish
			if (ready_queue.isEmpty() && running_process == null) {
				if (background_queue.isEmpty()) // finished
					break;
				else {
					// update the current time to time of the head of background
					// queue
					current_time = background_queue.element().arrivaltime;

					// update ready queue
					updateReady(current_time);
				}
			}

			// get next process
			if (running_process == null) {
				running_process = ready_queue.poll();
				addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
				this.update_running_buffer(running_process); //reads processes program from memory
			}
		}
	}

	
	
	
	public void WRR() {
		// clear all lists
		clearAll();

		// use FIFO ready_queue
		fifo = false;

		// set the comparator type for the algorithm (HPFSP)
		RQC.setType(ReadyQueueComparator.queueType.WRR);
		// Initialize the ready_queue with the comparator
		ready_queue = new PriorityQueue<PCB>(50, RQC);

		// get the first arrived processes
		float current_time = background_queue.element().arrivaltime;
		// update ready queue
		updateReady(current_time);

		PCB running_process = ready_queue.poll();
		addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
		this.update_running_buffer(running_process); //reads processes program from memory

		while (true){
			// the process will not finish
			if (running_process.currentruntime + timequantum < running_process.expectedruntime) {
				current_time += timequantum;
				running_process.currentruntime += timequantum;

				// update ready queue
				updateReady(current_time);

				if (!ready_queue.isEmpty()){
					addEvent(running_process.ID, current_time,
							Event.eventtype.preempted);
					ready_queue.add(running_process);
					running_process = null;
				}
			} else {
				// the time that process need to finish
				float dif = running_process.expectedruntime
						- running_process.currentruntime;
				current_time += dif;
				terminateProcess(running_process, current_time);
				// update ready queue
				updateReady(current_time);

				running_process = null;
			}

			// new time slice or finish
			if (ready_queue.isEmpty() && running_process == null) {
				if (background_queue.isEmpty()) // finished
					break;
				else {
					// update the current time to time of the head of background
					// queue
					current_time = background_queue.element().arrivaltime;

					// update ready queue
					updateReady(current_time);
				}
			}

			// get next process
			if (running_process == null) {
				running_process = ready_queue.poll();
				addEvent(running_process.ID, current_time, Event.eventtype.scheduled);
				this.update_running_buffer(running_process); //reads processes program from memory
			}
		}
	}
	
	

	// displays list of events that occurred while processing
	public void printEventList() {
		System.out.println("[DispatcherThread] Event List: ");
		for (int i = 0; i < eventlist.size(); i++) {
			String eventtypeS = "";
			if (eventlist.elementAt(i).type == Event.eventtype.arrived)
				eventtypeS = "Arrived";
			if (eventlist.elementAt(i).type == Event.eventtype.scheduled)
				eventtypeS = "Scheduled";
			if (eventlist.elementAt(i).type == Event.eventtype.terminated)
				eventtypeS = "Terminated";
			if (eventlist.elementAt(i).type == Event.eventtype.preempted)
				eventtypeS = "Pre-empted";
			if (eventlist.elementAt(i).type == Event.eventtype.context)
				eventtypeS = "Context_Switching";

			System.out.println("Process " + eventlist.elementAt(i).pID + ": " + eventlist.elementAt(i).time + " " + eventtypeS);
		}
		System.out.println("Page Faults = " + MMU.getPageFaults());
		System.out.println("TLB Hits = " + MMU.getTLBHits());
	}
}
