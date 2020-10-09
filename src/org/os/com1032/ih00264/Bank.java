/**
 * Interface to solution to bankers algorithm for deadlock prevention
 *
 * Taken from lab 6, adapted to allow number of customers to be determined by config file
 */
package org.os.com1032.ih00264;

public interface Bank {

	/**
	 * Add a customer to the bank.
	 * @param customerNumber The number of the customer being added.
	 * @param maxDemand The maximum demand for this customer.
	 */
	public void addCustomer(int customerNumber, int[] maximumDemand);

	/**
	 * Outputs the available, allocation, max, and need matrices.
	 */
	public void getState();

	/**
	 * Make a request for resources.
	 * @param customerNumber The number of the customer being added.
	 * @param request The request from this customer.
	 * @return  true The request is granted.
	 * @return  false The request is not granted.
	 */
	public boolean requestResources(int customerNumber, int[] request);

	/**
	 * Release resources.
	 * @param customerNumber The number of the customer being added.
	 * @param release The resources to be released.
	 */
	public  void releaseResources(int customerNumber, int[] release);
}
