/**
 * 
 */
package org.os.com1032.ih00264;

/**
 * 
 * Represents page in page table
 * 
 * @author ih00264
 *
 */
public class PageTableEntry {
	
	private int pageNumber; //used for page replacement algorithm
	private boolean valid; //indicates if the page is in RAM
	private int frameNumber; //frame number of the page in RAM
	private boolean dirty; //indicates if the page has been written to
	//private int requested; // non-zero only if that page is not in RAM and has been requested by the MMU. In this case it's value is the PID of the MMU

	public PageTableEntry(int pageNumber) {
		// initially we do not have a valid mapping
		this.pageNumber = pageNumber;
		valid = false;
		dirty = false;
		frameNumber = -1;
	}
	
	public int getPageNumber() {
		return this.pageNumber;
	}

	public boolean getValidBit() {
		return this.valid;
	}

	public int getFrameNumber() {
		return this.frameNumber;
	}

	public void setMapping(int frameNumber) {
		this.frameNumber = frameNumber;

		valid = true;
	}
	
	public void setDirty(boolean value) {
		this.dirty = value;
	}

	public boolean isDirty() {
		return this.dirty;
	}
	
	public void pageReplaced() {
		this.valid = false;
		this.frameNumber = -1;
	}
}
