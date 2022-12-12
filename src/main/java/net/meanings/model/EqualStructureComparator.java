/**
 * 
 */
package net.meanings.model;

import java.util.Comparator;

/**
 * @author seliv
 *
 */
public class EqualStructureComparator implements Comparator<EqualStructure> {

	public int compare(EqualStructure a, EqualStructure b) {
		
		if(!a.hasAnotherElements() && !b.hasAnotherElements()) {
			
			int parentCountDiff = a.getParentCount() - b.getParentCount();
			if(parentCountDiff==0) {
				
				double parentConnectionWeightsDiff = a.getAvgParentConnectionWeights() - b.getAvgParentConnectionWeights();
				return (int) (parentConnectionWeightsDiff*2);
				
			} else {
				return parentCountDiff;
			}
			
		} else if (a.hasAnotherElements() && b.hasAnotherElements()) {
			
			double childConnectionWeightsDiff = a.getAvgChildConnectionWeights() - b.getAvgChildConnectionWeights();
			return (int) (childConnectionWeightsDiff*2);
			
		} else if (a.hasAnotherElements() && !b.hasAnotherElements()) {
			return -1;
		} else if (!a.hasAnotherElements() && b.hasAnotherElements()) {
			return 1;
		}
		
		return 0;
	}

}
