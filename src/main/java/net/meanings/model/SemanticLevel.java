/**
 * 
 */
package net.meanings.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author seliv
 *
 */
public class SemanticLevel {
	
	private List<String> delimitters;
	private String name;
	private Integer sequenceSize = null;
	private boolean hasDelimitters;
	private int minMatchesToReplace = 1;
	
	public SemanticLevel(String name, boolean hasDelimitters) {
		this.name = name;
		this.hasDelimitters = hasDelimitters;
		delimitters = new ArrayList<String>();
	}
	
	@Override
	public String toString() {
		return "SemanticLevel:"+name;
	}

	public String getName() {
		return name;
	}
	

	public void setSequenceSize(Integer size) {
		//not used
		sequenceSize = size;
	}
	
	public Integer getSequenceSize() {
		return sequenceSize;
	}
	
	public void addDelimitter(String form) {
		delimitters.add(form);
	}
	
	public boolean hasDelimitter(String form) {
		if(!hasDelimitters) {
			return true;
		}
		return delimitters.contains(form);
	}
	
	public boolean emptyDelimitters() {
		return !hasDelimitters;
	}
	
	public boolean isLVLDelimitter(String form) {
		return delimitters.contains(form);
	}
	
	public List<String> getAllDelimitters() {
		return delimitters;
	}
	
	public void setMinMatchesToReplace(int size) {
		minMatchesToReplace = size;
	}
	
	public int getMinMatchesToReplace() {
		return minMatchesToReplace;
	}
	
}
