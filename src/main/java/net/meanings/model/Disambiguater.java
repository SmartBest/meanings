/**
 * 
 */
package net.meanings.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.neo4j.driver.types.Node;

import net.meanings.app.DependencyInjection;
import net.meanings.model.graph.Pattern;

/**
 * @author seliv
 *
 */
public class Disambiguater {
	private int minMatchesToReplace = 1;
	
	private List<Pattern> toFind;
	private DependencyInjection di;
	private List<EqualStructure> matches;
	
	public Disambiguater(DependencyInjection di) {
		this.di = di;
	}
	
	public Pattern tryToReplace(List<Pattern> toFind) {
		this.toFind = toFind;
		
		calcMinMatchesToReplace();
		
		findMatches();
		
		if(needReplaceFound()) {
			sortMatches();
			
			Pattern replacer = null;
			
			if(hasFullPatterns()) {
				replacer = matches.get(matches.size()-1).getSequence();
				matches.remove(matches.size()-1);
			} else {
				replacer = di.QHelper().createNewSequence(this.toFind);
			}
			
			replaceMatches(replacer);
			
			return replacer;
		}

		return null;
	}
	
	private void calcMinMatchesToReplace() {
		SemanticLevel topLvl = null;
		int topNum = -1;
		
		//find top level
		for (Pattern lp : toFind) {
			if(topLvl == null) {
				topLvl = lp.getLevel();
			}
			if(lp.getLevelNum()>topNum) {
				topLvl = lp.getLevel();
				topNum = lp.getLevelNum();
			}
		}
		
		this.minMatchesToReplace = topLvl.getMinMatchesToReplace();
		
		//если более двух букв - заменяем, если есть хотя бы одно совпадение
		/*if(topLvl.getName().equals("Symbols")) {
			if(di.QHelper().calcIOPower(toFind)>2) {
				this.minMatchesToReplace = 1;
			}
		}*/
		
	}

	private void findMatches() {
		/*List<Node> seq = new ArrayList<Node>();
		for (Pattern lp : toFind) {
			seq.add(lp.getNode());
		}*/
		
		matches = di.QHelper().getSequenceMatches(toFind);
	}
	
	private boolean needReplaceFound() {
		int matchesRealSize = calcMatchesRealSize();
		if(matchesRealSize>=minMatchesToReplace) {
			return true;
		}
		return false;
	}
	
	private int calcMatchesRealSize() {
		int res = matches.size();
		for (EqualStructure es : matches) {
			//если это самостоятельный паттерн, нужно подсчитать количество его использований
			if(!es.hasAnotherElements()) {
				res = res - 1; //вычитаем из общего кол-ва сам самостоятельный паттерн
				res = res + es.getParentCount(); //добавляем к общему кол-ву совпадений кол-во использований этого самостоятельного паттерна
			}
		}
		return res;
	}

	private void sortMatches() {
		Comparator<EqualStructure> esComparator = new EqualStructureComparator();
		Collections.sort(matches, esComparator);
	}
	
	private boolean hasFullPatterns() {
		for (EqualStructure es : matches) {
			if(!es.hasAnotherElements()) {
				return true;
			}
		}
		return false;
	}
	
	private void replaceMatches(Pattern replacer) {
		for (EqualStructure es : matches) {
			es.replaceThis(replacer);
		}
	}

}
