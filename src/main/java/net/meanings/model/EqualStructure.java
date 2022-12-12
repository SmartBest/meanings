/**
 * 
 */
package net.meanings.model;

import java.util.List;

import org.neo4j.driver.types.Node;

import net.meanings.model.graph.Pattern;

/**
 * @author seliv
 *
 */
public class EqualStructure {
	
	private Pattern sequence;
	private List<PathElement> elPaths;
	private List<PathElement> parentPaths = null;
	private QHelper qhelper;
	private Boolean hasAnotherElements = null;
	private Integer seqLen = null;
	
	public EqualStructure(Pattern sequence, List<PathElement> elPaths, QHelper qhelper) {
		this.sequence = sequence;
		this.elPaths = elPaths;
		this.qhelper = qhelper;
	}
	
	public Pattern getSequence() {
		return sequence;
	}
	
	public int getSeqLength() {
		if(seqLen == null) {
			seqLen = qhelper.lengthOfSequence(sequence);
		}
		return seqLen;
	}
	
	public boolean hasAnotherElements() {
		if(hasAnotherElements == null) {
			if (elPaths.size()==getSeqLength()) {
				hasAnotherElements = false;
			} else {
				hasAnotherElements = true;
			}
		}
		return hasAnotherElements;
	}
	
	public List<PathElement> getParentPaths() {
		if(parentPaths==null) {
			parentPaths = sequence.getParents();
		}
		return parentPaths;
		//return sequence.getParents();
	}
	
	public int getParentCount() {
		return getParentPaths().size();
	}
	
	public double getAvgParentConnectionWeights() {
		
		double res = 0;
		
		if (getParentCount()!=0) {
			for (PathElement pe : getParentPaths()) {
				res = res + pe.getWay().getWeight();
			}
			res = res/getParentCount();
		}
		
		return res;
	}
	
	public double getAvgChildConnectionWeights() {
		
		double res = 0;
		
		for (PathElement pe : elPaths) {
			res = res + pe.getWay().getWeight();
		}
		
		return res/elPaths.size();
	}
	
	public void replaceThis(Pattern replacer) {
		if (this.hasAnotherElements()) {
			
			int posForReplacer = elPaths.get(0).getWay().getNum();
			double newRelWeight = getAvgChildConnectionWeights();
			
			for (PathElement pe : elPaths) {
				qhelper.removePatternFromSequence(sequence, posForReplacer);
			}
			
			qhelper.insertPatternsIntoSequence(sequence, replacer, posForReplacer, newRelWeight);
			
		} else {
			
			if(getParentPaths().size()>0) {
				for (PathElement pe : getParentPaths()) {
					qhelper.createRelationFromSample(pe.getPattern(), replacer, pe.getWay());
				}
			}
			
			qhelper.deletePattern(sequence);
		}
	}

}
