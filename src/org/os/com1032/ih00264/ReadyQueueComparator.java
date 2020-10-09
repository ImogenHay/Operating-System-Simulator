package org.os.com1032.ih00264;

/**
 * 
 * Class for comparing different processes depending on type of scheduling algorithm used
 * 
 * Used MMayla's ReadyQueueComparator class from git-hub Process-Scheduling-Simulator
 */

import java.util.Comparator;

public class ReadyQueueComparator implements Comparator<PCB>{
	// privates
		private queueType type;

		// algorithm comparing scheduling methods
		private int comparing_FCFS(PCB p1, PCB p2)
		{
			if (p1.arrivaltime < p2.arrivaltime)
				return -1;
			if (p1.arrivaltime > p2.arrivaltime)
				return 1;
			return 0;
		}

		private int comparing_SJF(PCB p1, PCB p2)
		{
			if (p1.expectedruntime < p2.expectedruntime)
				return -1;
			if (p1.expectedruntime > p2.expectedruntime)
				return 1;
			return 0;
		}
		
		private int comparing_HPFSN(PCB p1, PCB p2)
		{
			if(p1.priority > p2.priority)
				return -1;
			if(p1.priority < p2.priority)
				return 1;
			return 0;
		}
		
		private int comparing_HPFSP(PCB p1, PCB p2)
		{
			if(p1.priority > p2.priority)
				return -1;
			if(p1.priority < p2.priority)
				return 1;
			return 0;
		}
		
		private int comparing_HPFD(PCB p1, PCB p2)
		{
			if(p1.priority > p2.priority)
				return -1;
			if(p1.priority < p2.priority)
				return 1;
			return 0;
		}
		
		private int comparing_WRR(PCB p1, PCB p2)
		{
			float p1weight = p1.arrivaltime + p1.expectedruntime + p1.priority;
			float p2weight = p2.arrivaltime + p2.expectedruntime + p2.priority;
			
			if(p1weight > p2weight)
				return -1;
			if(p1weight < p2weight)
				return 1;
			return 0;
		}
		
		private int comparing_background(PCB p1, PCB p2)
		{
			if (p1.arrivaltime < p2.arrivaltime)
				return -1;
			if (p1.arrivaltime > p2.arrivaltime)
				return 1;
			return 0;
		}
		
		public static enum queueType
		{
			background, 
			FCFS,
			SJF,
			RR,
			HPFSN,
			HPFSP,
			HPFD,
			WRR;
		}

		public ReadyQueueComparator(queueType qt)
		{
			super();
			type = qt;
		}

		public void setType(queueType qt)
		{
			type = qt;
		}

		@Override
		public int compare(PCB p1, PCB p2)
		{
			switch (type)
			{
			case FCFS:
				return comparing_FCFS(p1, p2);

			case SJF:
				return comparing_SJF(p1, p2);
				
			case HPFSN:
				return comparing_HPFSN(p1, p2);
				
			case HPFSP:
				return comparing_HPFSP(p1, p2);
				
			case WRR:
				return comparing_WRR(p1, p2);
			
			case HPFD:
				return comparing_HPFD(p1, p2);
			
			case background:
				return comparing_background(p1, p2);
			default:
				break;
			}

			return 0;
		}
}
