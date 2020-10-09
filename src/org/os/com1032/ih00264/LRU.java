package org.os.com1032.ih00264;
/**
 * Class implementing the LRU page-replacement strategy for the memory management unit
 *
 * Adapted from lab8, now uses and updates PageTableEntry objects instead of just page number
 */

public class LRU extends ReplacementAlgorithm
{
	// LRU list of page frames
	private LRUList frameList;

	/**
	 * @param pageFrameCount - the number of physical page frames
	 */
	public LRU(int pageFrameCount) {
		super(pageFrameCount);
		frameList = new LRUList(pageFrameCount);
	}

	/**
	 * Insert a page into a page frame.
	 */
	public void insert(PageTableEntry page) {
		System.out.print("[MemoryManagementUnit] LRU inserting page " + page.getPageNumber() + "\n");
		frameList.insert(page);
		if (System.getProperty("debug") != null) {
			System.out.print("Inserting " + page.getPageNumber());
			frameList.dump();
			System.out.println();
		}
	}
		
	class LRUList
	{
		// the page frame list
		PageTableEntry[] pageFrameList;

		// the number of elements in the page frame list
		int elementCount;

		// the last page inserted
		PageTableEntry lastInserted = null;;

		LRUList(int pageFrameCount) {
			pageFrameList = new PageTableEntry[pageFrameCount];
			elementCount = 0;
		}

		/**
		 * @param pageNumber the number of the page to be 
		 *	inserted into the page frame list.
		 */
		void insert(PageTableEntry page) {
			int searchVal;

			// if we didn't find it, replace the LRU page
			if ((searchVal = search(page)) == -1) { 
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
				lastInserted = page;
			}
			else if (page != lastInserted) {
				/**
				 * We only update the page table if the page being
				 * referenced was not the last one referenced.
				 * If it was the last page referenced, leave the
				 * page table as is.
				 */
				updatePageTable(searchVal);
				System.out.println("[MemoryManagementUnit] FRAME: " + page.getFrameNumber() + " contains page " +  page.getPageNumber() + "\n"); //if page already in frame
			}
			else {
				System.out.println("[MemoryManagementUnit] FRAME: " + page.getFrameNumber() + " contains page " +  page.getPageNumber() + "\n"); //if page already in frame
			}
		}

		/**
		 * @param int searchVal - the index to be updated
		 */
		void updatePageTable(int searchVal) {
			// first save the next page to be replaced  
			PageTableEntry savedPage = pageFrameList[elementCount % pageFrameList.length];

			PageTableEntry insertedPage = pageFrameList[searchVal];
	
			// if the page to be updated is the next page to be modified
			// just increment elementCount and return
			if (savedPage == insertedPage) {
				elementCount++;
				return;
			}

			// now copy the page just referenced to this position
			//pageFrameList[elementCount % pageFrameList.length] = pageFrameList[searchVal];
			if (System.getProperty("debug") != null) 
				System.out.println("sp = " + savedPage + " ec = " + elementCount + " sv = " + searchVal);

			// now shift all elements
			int rightIndex = searchVal;
			int leftIndex = (rightIndex==0)?pageFrameList.length-1:searchVal-1;
			
			while (rightIndex != (elementCount % pageFrameList.length) ) {
				pageFrameList[rightIndex] = pageFrameList[leftIndex];
				rightIndex = leftIndex;

				leftIndex = (leftIndex==0)?pageFrameList.length-1:leftIndex-1;
			}
			pageFrameList[rightIndex] = insertedPage;
			elementCount++;
		}

		void dump() {
			for (int i = 0; i < pageFrameList.length; i++)
				System.out.print("["+i+"]"+pageFrameList[i]+", ");
			System.out.print(" element count = " + elementCount);
		}


		/**
		 * Searches for page pageNumber in the page frame list
		 * @return non-negative integer if pageNumber was found
		 * @return -1 if pageNumber was not found
		 */
		int search(PageTableEntry pageNumber) {
			int returnVal = -1;

			for (int i = 0; i < pageFrameList.length; i++) {
				if (pageNumber.equals(pageFrameList[i])) {
					returnVal = i;
					break;
				}
			}
			return returnVal;
		}
	}
}
