/**
 * 
 */
package net.meanings.params;

import java.util.List;

import org.neo4j.driver.types.Relationship;

import net.meanings.model.graph.Relation;

/**
 * @author seliv
 *
 */
public class ParamValues {
	
	public static String ActivityFormula(String patternAlias, String wayAlias) {
		return wayAlias+".weight*(1+"+patternAlias+".actVal*(10/toFloat(((timestamp()-"+patternAlias+".actTime)/1000)+10)))";
	}
	
	public static String OrderByActivity(String patternAlias, String wayAlias) {
		return "ORDER BY "+ActivityFormula(patternAlias, wayAlias)+" DESC LIMIT 1";
	}
	
	public static String excludeWays(String wayAlias, List<Relation> ways) {
		if(ways == null) {
			return "";
		}
		if(ways.size() == 0) {
			return "";
		}
		
		String res = " AND (NOT id("+wayAlias+") IN [";
		
		for (Relation w : ways) {
			res = res+w.id()+",";		
		}
		
		return res.substring(0, res.length() - 1)+"]) ";
	}

}
