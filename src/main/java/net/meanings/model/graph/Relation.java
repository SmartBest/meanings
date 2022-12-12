/**
 * 
 */
package net.meanings.model.graph;

import org.neo4j.driver.types.Relationship;

import net.meanings.model.QHelper;

/**
 * @author sev
 *
 */
public abstract class Relation {

	private final double minWeight = 0.001;
	private final double maxWeight = 1;
	
	protected Relationship rel;
	private QHelper qhelper;
	
	public Relation(Relationship rel, QHelper qhelper) {
		this.qhelper = qhelper;
		this.rel = rel;
	}
	
	public Relationship getRelationship() {
		return rel;
	}
	
	public double getWeight() {
		return rel.get("weight").asDouble();
	}
	
	public void setWeight(double val) {
				
		if (val > maxWeight) {
			val = maxWeight;
		}
		if (val < minWeight) {
			val = minWeight;
		}
		
		if(getWeight()==val) {
			return;
		}
		
		qhelper.setRelationWeight(this, val);
	}
	
	public void plusWeight(double val) {
		
		double newWeight = this.getWeight() + val;
		
		setWeight(newWeight);
	}

	public void minusWeight(double val) {
		
		double newWeight = this.getWeight() - val;
		
		setWeight(newWeight);
	}

	public long id() {
		return rel.id();
	}
	
	public boolean containsKey(String key) {
		return rel.containsKey(key);
	}
	
	public boolean isEnumerated() {
		return rel.containsKey("num");
	}

	public abstract String getType();
	public abstract Integer getNum();

}
