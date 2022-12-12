/**
 * 
 */
package net.meanings.model;

/**
 * @author seliv
 *
 */
public interface IBrain {
	public String perceive(String message);
	public void terminate();
	public void reset();
	public void sleep();
}
