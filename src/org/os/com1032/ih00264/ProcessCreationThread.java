/**
 * 
 */
package org.os.com1032.ih00264;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Produces background queue buffer by reading from input file and user input, creates PCB for each Process
 * 
 * @author ih00264
 *
 */
public class ProcessCreationThread extends Thread {
	
	 ReentrantLock lock;
	 Condition con;
	 Vector<PCB> background_queue;
	 protected Vector<PCB> input_list;
	 int buffer_size;
	 
	 public ProcessCreationThread(ReentrantLock lock, Condition con, Vector<PCB> background_queue, int buffer_size) { 
			 this.lock = lock;
			 this.con = con;
			 this.background_queue  = background_queue;
			 this.input_list = new Vector<PCB>();
			 this.buffer_size = buffer_size;
	 } 
	 
	 private String removeComments(String dir) throws PSSexception {
			try
			{
				Scanner in = new Scanner(new File(dir));
				String infile = "",line;
				while (in.hasNextLine())
				{
					line = in.nextLine();
					if (line.charAt(0) != '#')
					{
						infile = infile.concat(line+" ");
					}
				}
				
				in.close();
				return infile;
			} catch (FileNotFoundException e)
			{
				throw new PSSexception(PSSexception.errortype.inputfile_error,
						"Input file not found");
			}
		}
					
		
		public void Read(String dir) throws PSSexception {	
			String infile = removeComments(dir); //remove comments from the input file	 
			Scanner in = new Scanner(infile);
			
			int id,pr,n, si;
			float at,ert;
			String inst;
			
			n = in.nextInt(); //number of processes

			for(int i=0;i<n;i++)
			{
				id = in.nextInt();
				at = in.nextFloat();
				ert = in.nextFloat();
				pr = in.nextInt();
				si = in.nextInt();
				
				PCB newprocess = new PCB(id, at, ert, pr, si); //creates process from input file

				//gets program for process which is between 'Code:' and 'exit;' in file
				if(in.hasNext("Code:")) {
					in.next("Code:");
					while(!in.hasNext("exit;")){
						inst = "";
						if(in.hasNext("[\"]")) {
							in.next("[\"]");
							while(!in.hasNext("[\"]")) {
								inst += in.next("[A-Z0-9*.]*") + " ";
							}
							in.next("[\"]");
						}
						newprocess.addInstruction(inst); //adds instruction to processes program
					}
					in.next("exit;");
				}
				
				
				if (!input_list.contains(newprocess)) {
					input_list.add(newprocess);
				}
				
				
							
			}
			in.close();
		}

		
		
		public void test_print()
		{
			for(int i=0;i<background_queue.size();i++)
			{
				System.out.println(background_queue.elementAt(i).ID+" "+background_queue.elementAt(i).arrivaltime+" "+
						background_queue.elementAt(i).expectedruntime+" "+background_queue.elementAt(i).priority+" "+background_queue.elementAt(i).size);
			}
		}
		
		
		public void addProcess(PCB process){
			if (!input_list.contains(process)) {
				input_list.add(process);
			}
		}
	 
	 @Override
	 public void run() { 
		 while(true) { //always checking for new input
			 	lock.lock();
			 	while (this.input_list.isEmpty() || this.background_queue.size() >= this.buffer_size) {
			 		con.signal(); //signals to dispatcher thread to continue since nothing new to add to buffer
			 		try {
			 			con.await(); //interrupts thread causing it to wait if nothing new to add
			 		} catch (InterruptedException ex) {
			 			ex.printStackTrace();
			 		}
			 	}
				background_queue.add(this.input_list.get(0));
				System.out.println("[ProcessCreationThread] Produced : " + this.input_list.get(0).getID() + " to background queue");
				this.input_list.remove(0);		
				lock.unlock(); 

		 }
	}
}









