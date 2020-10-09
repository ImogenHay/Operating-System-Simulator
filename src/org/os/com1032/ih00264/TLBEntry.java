/**
 * 
 */
package org.os.com1032.ih00264;

/**
 * Represents entry to TLB Cache containing recently used page frame mapping
 *
 * Used from lab8
 */
public class TLBEntry {
	
	private int pageNumber;		// the virtual page number
	private int frameNumber;	// the physical frame number
	private boolean isValid;	// flag that indicates if the mapping
					// is valid.

	public TLBEntry() {
		pageNumber = -1;
		frameNumber = -1;
		isValid = false;
	}

	public boolean setMapping(int pageNumber, int frameNumber) {
		this.pageNumber = pageNumber;
		this.frameNumber = frameNumber;
		isValid = true;

		return isValid;
	}	

	public boolean checkPageNumber(int pageNumber) {
		if (pageNumber == this.pageNumber)
			return true;
		else
			return false;
	}

	public int getFrameNumber() {
		return frameNumber;
	}
}
