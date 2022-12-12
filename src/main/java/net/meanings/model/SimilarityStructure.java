/**
 * 
 */
package net.meanings.model;

import java.util.ArrayList;
import java.util.List;

import net.meanings.model.graph.Pattern;

/**
 * @author seliv
 *
 */
public class SimilarityStructure {
	
	private Pattern original;
	private Pattern similar;
	private List<Pattern> fromSequences;
	
	public SimilarityStructure(Pattern original, Pattern similar) {
		this.original = original;
		this.similar = similar;
		fromSequences = new ArrayList<Pattern>();
	}
	
	public void addSequence(Pattern seq) {
		fromSequences.add(seq);
	}
	
	public int size() {
		return fromSequences.size();
	}
	
	public Pattern getSimilarityPattern() {
		return similar;
	}
	
	public Pattern getOriginalPattern() {
		return original;
	}
	
	public List<Pattern> getSequences() {
		return fromSequences;
	}

}
