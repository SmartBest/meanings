/**
 * 
 */
package net.meanings.model.graph;

import org.neo4j.driver.types.Relationship;

import net.meanings.model.QHelper;
import net.meanings.types.RelationshipType;

/**
 * @author sev
 *
 */
public class RelationEnumerated extends Relation {

	/**
	 * @param rel
	 * @param qhelper
	 */
	public RelationEnumerated(Relationship rel, QHelper qhelper) {
		super(rel, qhelper);
	}

	/* (non-Javadoc)
	 * @see net.meanings.model.graph.Relation#getType()
	 */
	@Override
	public String getType() {
		return RelationshipType.Element();
	}
	
	@Override
	public Integer getNum() {
		return rel.get("num").asInt();
	}

}
