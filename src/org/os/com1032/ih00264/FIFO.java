package org.os.com1032.ih00264;
/**
 * Class implementing the FIFO page-replacement strategy for the memory management unit
 *
 * Adapted from lab8, now uses and updates PageTableEntry objects instead of just page number
 */

public class FIFO extends ReplacementAlgorithm
{
	// FIFO list of page frames
	private FIFOList frameList;

	/**
	 * @param pageFrameCount - the number of physical page frames
	 */
	public FIFO(int pageFrameCount) {
		super(pageFrameCount);
		frameList = new FIFOList(pageFrameCount);
	}


	/**
	 * insert a page into a page frame.
	 * @param int pageNumber - the page number being inserted.
	 */
	public void insert(PageTableEntry page) {
		System.out.print("[MemoryManagementUnit] FIFO inserting page " + page.getPageNumber() + "\n");
		frameList.insert(page);	
		if (System.getProperty("debug") != null) {
			System.out.print("Inserting " + page.getPageNumber());
			frameList.dump();
			System.out.println();
		}
	}
		
	class FIFOList
	{
		// the page frame list
		PageTableEntry[] pageFrameList;

		// the number of elements in the page frame list
		int elementCount;

		FIFOList(int pageFrameCount) {
			pageFrameList = new PageTableEntry[pageFrameCount];
			elementCount = 0;
		}

		/**
		 * @param pageNumber the number of the page to be 
		 *	inserted into the page frame list.
		 */
		void insert(PageTableEntry page) {
			if (!search(page)) {
				// an asterisk indicates a page fault
				if (System.getProperty("debug") != null)
					System.out.print("*");
				pageFaultCount++;
				int replacedFrame = elementCount++ % pageFrameCount; //id of frame page will be swapped into
				PageTableEntry replacedPage = pageFrameList[replacedFrame]; //page that is being replaced by inserted page
				pageFrameList[replacedFrame] = page; //replace a page
				
				if (pageFaultCount < pageFrameList.length + 1) {//if page inserted did not need to be swapped in
					System.out.println("[MemoryManagementUnit] FRAME: " + replacedFrame + " inserted page " +  page.getPageNumber() + "\n");
				}
				else { //if page swapped
					System.out.println("[MemoryManagementUnit] FRAME: " + replacedFrame + " swapped page " + replacedPage.getPageNumber() + " with " + page.getPageNumber() + "\n");			
					replacedPage.pageReplaced(); //removes old mapping of page no longer in RAM, sets to invalid
				}
				page.setMapping(replacedFrame); //sets mapping from inserted page to frame in pageTable
			}
			else {
				System.out.println("[MemoryManagementUnit] FRAME: " + page.getFrameNumber() + " contains page " +  page.getPageNumber() + "\n"); //if page already in frame
			}
		}

		// dump the page frames
		void dump() {
			for (int i = 0; i < pageFrameList.length; i++)
				System.out.print("["+i+"]"+pageFrameList[i]+", ");
		}


		/**
		 * Searches for page  in the page frame list
		 * @return true if page was found
		 * @return false if page was not found
		 */
		boolean search(PageTableEntry page) {
			boolean returnVal = false;

			for (int i = 0; i < pageFrameList.length; i++) {
				if (page.equals(pageFrameList[i])) {
					returnVal = true;
					break;
				}
			}
			return returnVal;
		}
	}
}
