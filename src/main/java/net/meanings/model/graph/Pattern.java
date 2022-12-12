/**
 * 
 */
package net.meanings.model.graph;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.types.Node;

import net.meanings.model.PathElement;
import net.meanings.model.QHelper;
import net.meanings.model.SemanticLevel;

/**
 * @author sev
 *
 */
public class Pattern {
	
	private Node node;
	private boolean needRecalcLevel = true;
	private QHelper qhelper;
	protected List<SemanticLevel> sLevels;
	SemanticLevel level;
	private int levelNum;
	private List<String> forms;


	public Pattern(Node node, List<SemanticLevel> sLevels, QHelper qhelper) {
		this.node = node;
		this.sLevels = sLevels;
		this.qhelper = qhelper;
	}
	
	public boolean equals(Pattern obj) {
		return this.getNode().equals(obj.getNode());
	}
	
	public long id() {
		return node.id();
	}
	
	public List<PathElement> getParents() {
		return qhelper.getParents(this);
	}

	public List<PathElement> getChilds() {
		return qhelper.getChildPatterns(this);
	}
	
	public boolean hasLabel(String label) {
		return node.hasLabel(label);
	}
	
	public String getName() {
		return node.get("name").asString("!NO_VALUE!");
	}
	
	public void activate() {
		setActivityInternal(1);
	}
	
	public void deactivate() {
		setActivityInternal(-1);
	}
	
	protected void setActivityInternal(float val) {
		qhelper.setPatternActivity(this, val);
	}
	
	public SemanticLevel getLevel() {
		if(needRecalcLevel) {
			calculateLevel();
		}
		return level;
	}
	
	public int getLevelNum() {
		if(needRecalcLevel) {
			calculateLevel();
		}
		return levelNum;
	}
	
	public Node getNode() {
		return node;
	}
	
	public void setNeedRecalcLevel() {
		needRecalcLevel = true;
	}
	
	public String getResolved() {
		if(needRecalcLevel) {
			calculateLevel();
		}
		
		String res = "";
		
		for (String form : forms) {
			res = res + form;
		}
		
		return res;
	}
	
	protected void calculateLevel() {
		forms = qhelper.resolveInList(this);
		
		int num = 0;
		for (SemanticLevel lvl : sLevels) {
			for (String form : forms) {
				if(lvl.hasDelimitter(form)) {
					level = lvl;
					levelNum = num;
					break;
				}
			}
			num++;
		}
		
		needRecalcLevel = false;
	}
	
	public boolean isDelimitter() {
		if(needRecalcLevel) {
			calculateLevel();
		}
		
		List<String> allDelimitters = new ArrayList<String>();
		
		for (SemanticLevel lvl : sLevels) {
			allDelimitters.addAll(lvl.getAllDelimitters());
		}
		
		for (String form : forms) {
			if(!allDelimitters.contains(form)) {
				return false;
			}
		}
		
		return true;
	}

}
