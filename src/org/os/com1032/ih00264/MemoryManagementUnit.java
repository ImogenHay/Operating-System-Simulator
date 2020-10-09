package org.os.com1032.ih00264;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates allocating and deallocation virtual memory and manages page replacement algorithm and TLBCache
 * 
 * @author ih00264
 *
 */

public class MemoryManagementUnit {

	private int frameSize;
	private int numberOfFrames;
	private int virtualMemorySize;
	private int pageSize;
	private int numberOfPages;
	private int tlbSize; //tlb reach = tlbSize * pageSize, considered cache of the pageTable
	
	private PageTableEntry[] pageTable= null;
	private List<PCB> virtualMemory = null;
	private int[] frames = null;
	private TLBEntry[] TLB;	//cache of page frame mappings
	private int TLBHits; //number of TLB hits
	private int nextTLBEntry;
	
	private ReplacementAlgorithm replacementAlgorithm;
	

	public MemoryManagementUnit(int RAM, int frameSize, int pageSize, int numberOfPages, int tlbSize, String replacementAlgorithm) {
		super();
		this.frameSize = frameSize;
		this.numberOfFrames = RAM / frameSize;
		this.pageSize = pageSize;
		this.numberOfPages = numberOfPages;
		this.virtualMemorySize = pageSize * numberOfPages;
		this.tlbSize = tlbSize;
		
		this.pageTable = new PageTableEntry[this.numberOfPages]; //creates list of pages, starts at page 0
		for (int i = 0; i < this.numberOfPages; i++)
			pageTable[i] = new PageTableEntry(i);
		
		this.virtualMemory =  new ArrayList<PCB>(); //to fill with references to processes containing program, number of positions in list taken up by one program = size of program
		PCB empty = null;
		for (int i = 0; i < this.virtualMemorySize; i++)
			this.virtualMemory.add(empty);
		
		this.frames = new int[numberOfFrames]; 
		for (int i = 0; i < numberOfFrames; i++)
			frames[i] = i;
		
		this.TLB = new TLBEntry[tlbSize];
		for (int i = 0; i < tlbSize; i++)
			this.TLB[i] = new TLBEntry();
		
		this.TLBHits = 0;
		this.nextTLBEntry = 0;
		
		if (replacementAlgorithm.equals("lru")) { //determines which replacement algorithm is used, default is FIFO
			this.replacementAlgorithm = new LRU(this.numberOfFrames);
		}
		else {
			this.replacementAlgorithm = new FIFO(this.numberOfFrames);
		}
	}
	
	
	
	private int numFreePages() { //checks if page has been written to
		int num = 0;
		for (PageTableEntry page : this.pageTable) {
			if (!page.isDirty()) {
				num++;
			}
		}
		return num;
	}
	
	
	
	public int checkTLB(int pageNumber) { //Check the TLB for a mapping of page number to physical frame
		int frameNumber = -1;
		for (int i = 0; i < this.tlbSize; i++) { 
			if (this.TLB[i].checkPageNumber(pageNumber)) { //if page in TLB
				frameNumber = this.TLB[i].getFrameNumber(); //get pages frame number
				this.TLBHits++;
				System.out.println("[MemoryManagementUnit] TLB Hit");
				break;
			}
		}

		return frameNumber;
	}
	
	
	public int getTLBHits() { //how often mapping is used that is stored in TLB cache
		return this.TLBHits;
	}

	
	
	public void setTLBMapping(int pageNumber, int frameNumber) { //Update the next TLB entry.
		if (this.checkTLB(pageNumber) == -1) { //checks if this is already mapped
			this.TLB[this.nextTLBEntry].setMapping(pageNumber, frameNumber); // establish the mapping
			this.nextTLBEntry = (this.nextTLBEntry + 1) % this.tlbSize; //uses a very simple FIFO approach for managing entries in the TLB.
		}
	}
	

	public boolean allocateMemory(PCB program) { //fill virtual memory with pid of programs process, number of positions taken in list = size of program, returns whether allocated or not
		boolean allocated = false;
		int sizeLeft = program.getSize(); //size still needed to be allocated
		int numPages = (int) Math.ceil(((float)sizeLeft)/this.pageSize); //number of pages process will take up
		int freePages = this.numFreePages();	
		if (freePages >= numPages) { //if programs process can fit in free pages
			
			for (PageTableEntry p : this.pageTable) { //for each page
				
				if (numPages > 0 && !p.isDirty()) { //if more pages need to be allocated and page is not written to
					
					int virtualAddress = p.getPageNumber() * this.pageSize; //first vitrualAddress in page
					p.setDirty(true); //indicates page has been written to
					numPages--; //reduces number of pages that need to be written to
					for (int i = 0; i < this.pageSize; i++) { //for each virtualAddress in page allocate programs process
						
						if (sizeLeft > 0) { //if still need to allocate space
							
							this.virtualMemory.set(virtualAddress+i, program);
						}
						sizeLeft--;
					}
				}

			}
			allocated = true;	
			System.out.println("[MemoryManagementUnit] Allocated program " + program.getID() + " " + this.virtualMemory.toString() + "\n");
		}
		else {
			System.out.println("[MemoryManagementUnit] Run out of memory for " + program.getID());
		}



		return allocated;
	}
	
	
	
	public int getPhysicalAddress(int virtualAddress) { //returns -1 if not in RAM
		if (virtualAddress >= this.virtualMemorySize) { //if invalid input
			throw new IllegalArgumentException("Invalid Virtual Address");
		}
		
		int frame;
		int page = ((int) Math.ceil(((float)virtualAddress + 1)/this.pageSize)) - 1; // +/- 1 since pages and addresses start at 0
		
		
		if ( (frame = checkTLB(page)) == -1 ) { //don't need to run algorithm if page in TLB (cache)
			System.out.println("[MemoryManagementUnit] TLB Miss");
			frame = this.pageTable[page].getFrameNumber(); //uses pageTable to get frame
			if (frame == -1) {
				System.out.println("[MemoryManagementUnit] Virtual Address: " + virtualAddress + ", Page: " + page + " not in RAM"); //frameNumber set to -1 in PageTableEntry class if no mapping so not in RAM
				return -1;
			}
		}
		
		int positionInPage = virtualAddress - (page * this.pageSize); //starting at 0 so position 0-3 if page size = 4
		int physicalAddress = (frame * this.frameSize) + positionInPage;
		System.out.println("[MemoryManagementUnit] Virtual Address: " + virtualAddress + ", Page: " + page + ", Frame: " + frame + ", Physical Address: " + physicalAddress);
		return physicalAddress;

	}
	
	
	
	public void read(PCB program) { //swaps pages from virtual memory into physical so they can be read from
		if (this.virtualMemory.contains(program)) {
			for (int i = 0; i < this.virtualMemory.size()/this.pageSize; i++) { //for each page
				if (this.virtualMemory.get(i*this.pageSize) != null) {
					if (this.virtualMemory.get(i*this.pageSize).equals(program)) { //if page contains process
						this.replacementAlgorithm.insert(this.pageTable[i]); //swap into physical memory using pre determined page swapping algorithm						
						this.setTLBMapping(this.pageTable[i].getPageNumber(), this.pageTable[i].getFrameNumber());
					}
				}
			}
		
		}
		else {
			System.out.println("[MemoryManagementUnit]" +  program.getID() + " is not in memory.");
		}
	}
	
	
	
	public void deallocateMemory(PCB program) { //automatically clears program from memory, changes to null, need to fix allocation
		PCB empty = null;
		if (this.virtualMemory.contains(program)) {
			int i = 0;
			for (PCB p : this.virtualMemory) {
				if(program.equals(p)) {
					this.virtualMemory.set(i, empty);
					int page = ((int) Math.ceil(((float)i + 1)/this.pageSize)) - 1; //find page data being deallocated from
					this.pageTable[page].setDirty(false); //indicates page is now clear
					
				}
				i++;
			}
		}

		System.out.println("[MemoryManagementUnit] Deallocated program " + program.getID() + " " + this.virtualMemory + "\n");
	}
	
	
	
	public int getPageFaults() {
		return this.replacementAlgorithm.getPageFaultCount();
	}


}
