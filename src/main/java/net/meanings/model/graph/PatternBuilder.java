/**
 * 
 */
package net.meanings.model.graph;

import java.util.List;

import org.neo4j.driver.types.Node;

import net.meanings.model.QHelper;
import net.meanings.model.SemanticLevel;

/**
 * @author sev
 *
 */
public class PatternBuilder {
	private List<SemanticLevel> sLevels;
	private QHelper qhelper;
	
	public PatternBuilder(List<SemanticLevel> sLevels, QHelper qhelper) {
		this.sLevels = sLevels;
		this.qhelper = qhelper;
	}
	
	public Pattern getPattern(Node node) {
		Pattern res = new Pattern(node,sLevels,qhelper);
		return res;
	}

}
