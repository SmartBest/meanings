/**
 * 
 */
package net.meanings.model;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import net.meanings.model.graph.Pattern;
import net.meanings.model.graph.Relation;

/**
 * @author seliv
 *
 */
public class PathElement {
	
	private Pattern pattern;
	private Relation way;
	private List<Relation> excludeWays = null;
	private Integer downPathShift = null;
	
	
	public PathElement(Pattern pattern, Relation way) {
		this.pattern = pattern;
		this.way = way;
		//this.excludeWays = new ArrayList<Relationship>();
	}
	
	private void checkExcludeWays() {
		if(excludeWays==null) {
			excludeWays = new ArrayList<Relation>();
		}
	}
	
	public Pattern getPattern() {
		return pattern;
	}
	
	public Relation getWay() {
		return way;
	}
	
	public Integer getDownPathShift() {
		return downPathShift;
	}
	
	public void setDownPathShift(Integer i) {
		downPathShift = i;
	}
	
	public boolean hasDownPathShift() {
		return downPathShift != null;
	}
	
	public List<Relation> getExcludeWays() {
		checkExcludeWays();
		return excludeWays;
	}
	
	public boolean wayHasNum() {
		if(way == null) {
			return false;
		}
		return way.isEnumerated();
	}
	
	public boolean hasWay() {
		if(way == null) {
			return false;
		}
		return true;
	}
	
	public boolean hasExcludeWays() {
		checkExcludeWays();
		return excludeWays.size() > 0;
	}
	
	public void clearExcludeWays() {
		this.excludeWays = new ArrayList<Relation>();
	}
	
	public void addExcludeWay(Relation way) {
		checkExcludeWays();
		excludeWays.add(way);
	}
}
