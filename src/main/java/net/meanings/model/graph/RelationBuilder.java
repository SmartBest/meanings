/**
 * 
 */
package net.meanings.model.graph;

import java.util.List;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import net.meanings.model.QHelper;
import net.meanings.model.SemanticLevel;
import net.meanings.types.RelationshipType;

/**
 * @author sev
 *
 */
public class RelationBuilder {
	private QHelper qhelper;
	
	public RelationBuilder(QHelper qhelper) {
		this.qhelper = qhelper;
	}
	
	public Relation getRelation(Relationship rel) {
		Relation res = null;
		
		if (rel.type().equals(RelationshipType.Element())) {
			res = new RelationEnumerated(rel,qhelper);
		}
		if (rel.type().equals(RelationshipType.Same())) {
			res = new RelationSimilarity(rel,qhelper);
		}
		
		return res;
	}

}
