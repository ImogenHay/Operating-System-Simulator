package org.os.com1032.ih00264;

import java.util.ArrayList;

/**
 * Class for creating a PCB for a process as an object
 * 
 * @author ih00264
 */



public class PCB {
	
		public int ID;
		public float arrivaltime;
		public float expectedruntime;
		public float currentruntime;
		public int priority;
		public int size;
		public ArrayList<String> program;
		
		public float Time_around; //calculated times
		public float Weighted_around;
		public float FinishTime;
		

		public PCB(int id,float at,float ert,int pr, int size) throws PSSexception {
			this.ID = id;
			this.arrivaltime = at;
			this.expectedruntime = ert;
			this.priority = pr;
			this.currentruntime = 0;
			this.size = size;
			this.program = new ArrayList<String>();
			

			//exception handling
			if(ID<0)
				throw new PSSexception(PSSexception.errortype.inputfile_error, 
						"ID is a negative value");
			
			if(arrivaltime<0)
				throw new PSSexception(PSSexception.errortype.inputfile_error, 
						"Arrival Time is a negative value");
		
			if(expectedruntime<0)
				throw new PSSexception(PSSexception.errortype.inputfile_error, 
						"Expected run time is a negative value");
			
			if(priority<0)
				throw new PSSexception(PSSexception.errortype.inputfile_error, 
						"Priority is a negative value");
			if(size<0)
				throw new PSSexception(PSSexception.errortype.inputfile_error, 
						"Size is a negative value");
		}
		
		
		public void addInstruction(String instruction) {
			this.program.add(instruction);
		}
		
		public void executed() {
			this.program.clear();
		}
			
		
		public int getID() {
			return this.ID;
		}
		
		public int getSize() {
			return this.size;
		}
		
		public ArrayList<String> getProgram() {
			return this.program;
		}
		
		public String toString()
		{
			String out=ID+" ";
			return out;
		
		}
		
}
