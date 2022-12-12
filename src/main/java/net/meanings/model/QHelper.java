package net.meanings.model;

import static org.neo4j.driver.Values.parameters;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.meanings.model.graph.Pattern;
import net.meanings.model.graph.PatternBuilder;
import net.meanings.model.graph.Relation;
import net.meanings.model.graph.RelationBuilder;
import net.meanings.params.ParamValues;
import net.meanings.types.NodeLabel;
import net.meanings.types.RelationshipType;

public class QHelper {
	
	private Session session;
	private List<SemanticLevel> sLevels;
	private PatternBuilder pBuilder;
	private RelationBuilder rBuilder;
	
	private static final Logger logger = LoggerFactory.getLogger(
			QHelper.class);
	
	public QHelper(Session session, List<SemanticLevel> sLevels) {
		this.session = session;
		this.sLevels = sLevels;
		
		//Фабрика паттернов
		pBuilder = new PatternBuilder(sLevels, this);
		//Фабрика соединений
		rBuilder = new RelationBuilder(this);
	}
	
	public PatternBuilder getPatternBuilder() {
		return pBuilder;
	}

	/**
	 * Разделитель? (пробел и знаки препинания)
	 * @param pattern строковое имя
	 * @return да или нет
	 */
	public boolean isDelimiter(final String pattern) {
        for (SemanticLevel level : sLevels) {
            if (level.isLVLDelimitter(pattern)) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Установить активность паттерна
	 * @param pattern паттерн
	 * @param newActVal новое значение активности
	 */
	public void setPatternActivity(Pattern pattern, float newActVal) {
		session.run("MATCH (n:Pattern) WHERE id(n) = $currentPatternID SET n.actVal = $newActVal, n.actTime = timestamp()",
        		parameters("currentPatternID", pattern.id(), "newActVal", newActVal));
	}

	/**
	 * Длина иерархической последовательности
	 * @param pattern паттерн
	 * @return длина
	 */
	public Integer lengthOfSequence(Pattern pattern) {
		Integer res = null;
		
		if(pattern.hasLabel(NodeLabel.IO())) {
			return 1;
		}
		
		if(!pattern.hasLabel(NodeLabel.Sequence())) {
			return null;
		}
		
		Result r = session.run("MATCH (pt:Pattern)-[way:ELEMENT]->(:Pattern) WHERE id(pt) = $currentPatternID RETURN count(way) AS relCount",
        		parameters("currentPatternID", pattern.id()));
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = rec.get("relCount").asInt();
        }
		return res;
	}

	/**
	 * Число смыслов для паттерна
	 * @param pattern паттерн
	 * @return Число смыслов
	 */
	public Integer countOfMeanings(Pattern pattern) {
		Integer res = null;
		
		if(!pattern.hasLabel(NodeLabel.Meaning())) {
			return null;
		}
		
		Result r = session.run("MATCH (pt:Pattern)-[way:MEAN]->(:Pattern) WHERE id(pt) = $currentPatternID RETURN count(way) AS relCount",
        		parameters("currentPatternID", pattern.id()));
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = rec.get("relCount").asInt();
        }
		return res;
	}

	/**
	 * Число похожестей для паттерна
	 * @param pattern паттерн для анализа
	 * @return число похожестей
	 */
	public Integer countOfSimilarities(Pattern pattern) {
		Integer res = null;
		
		if(!pattern.hasLabel(NodeLabel.Similarity())) {
			return null;
		}
		
		Result r = session.run("MATCH (pt:Pattern)-[way:SAME]->(:Pattern) WHERE id(pt) = $currentPatternID RETURN count(way) AS relCount",
        		parameters("currentPatternID", pattern.id()));
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = rec.get("relCount").asInt();
        }
		return res;
	}

	/**
	 * Получить список предков с путями
	 * @param pattern откуда ищем
	 * @return список предков с путями
	 */
	public List<PathElement> getParents(Pattern pattern) {
		List<PathElement> res = new ArrayList<PathElement>();
		
		Result r = session.run("MATCH (res:Pattern)-[way]->(pt:Pattern) WHERE id(pt) = $currentPatternID RETURN res, way",
        		parameters("currentPatternID", pattern.id()));
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   PathElement pe = new PathElement(pBuilder.getPattern(rec.get("res").asNode()),rBuilder.getRelation(rec.get("way").asRelationship()));
    	   res.add(pe);
        }
		
		return res;
	}

	/**
	 * Найти путь вниз
	 * @param currentPattern откуда спускаемся
	 * @param wayUpCurrentNum по какой по счету связи поднялись сюда
	 * @param right ищем вправо или влево (реализовано только вправо)
	 * @param excludeWays исключения - по ним не идем
	 * @return связь и паттерн
	 */
	public PathElement getWayDown(Pattern currentPattern, Integer wayUpCurrentNum, Boolean right, List<Relation> excludeWays) {
		PathElement res = null;
		
		Result r = null;
		
		Integer seqNumNext = wayUpCurrentNum;
		if(right) {
			seqNumNext += 1; 
		} else {
			seqNumNext -= 1; 
		}
		
		if(currentPattern.hasLabel(NodeLabel.Sequence())) {
			r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID AND way.num = $wayNum RETURN pt, way",
        		parameters("currentPatternID", currentPattern.id(),"wayNum",seqNumNext));
		} else if (currentPattern.hasLabel(NodeLabel.Meaning())||currentPattern.hasLabel(NodeLabel.Similarity())) {
			r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID"+ParamValues.excludeWays("way", excludeWays)+" RETURN pt, way "+ParamValues.OrderByActivity("pt", "way"),
	        		parameters("currentPatternID", currentPattern.id()));
		}
		
		while(r.hasNext()) {
	    	   Record rec = r.next();
	    	   Pattern pt = pBuilder.getPattern(rec.get("pt").asNode());
	    	   Relation way = rBuilder.getRelation(rec.get("way").asRelationship());
	    	   
	    	   res = new PathElement(pt, way);
	    	   logger.info("getWayDown "+ pt.getName() + way.getNum());
	        }
		
		return res;
	}

	/**
	 * Вычислить мощность последовательности по количеству её IO-составляющих
	 * @param seq последовательность
	 * @return число
	 */
	public int calcIOPower(List<Pattern> seq) {
		String res = "";
		for (Pattern pattern : seq) {
			res = res+resolve(pattern);
		}
		return res.length();
	}

	/**
	 * Количество всех паттернов в БД
	 * @return количество паттернов
	 */
	public long getAllNodeCount() {
		long res = -1;
		
		String query = "MATCH (n) RETURN count(n) AS CNTNODES";
		Result r = session.run(query);
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = rec.get("CNTNODES").asLong(-1);
		}
		
		return res;
	}

	/**
	 * Количество всех связей в БД
	 * @return количество связей
	 */
	public long getAllRelationCount() {
		long res = -1;
		
		String query = "MATCH ()-[r]->() RETURN count(r) AS CNTREL";
		Result r = session.run(query);
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = rec.get("CNTREL").asLong(-1);
		}
		
		return res;
	}

	/**
	 * Найти похожести по контексту (совпадает префикс и постфикс)
	 * @param leftContext префикс
	 * @param toAnalyze что анализируем
	 * @param rightContext постфикс
	 * @return список похожестей
	 */
	public List<SimilarityStructure> getSimilarities(List<Pattern> leftContext, Pattern toAnalyze, List<Pattern> rightContext) {
		List<SimilarityStructure> res =  new ArrayList<SimilarityStructure>();
		
		List<Pattern> seq =  new ArrayList<Pattern>();
		
		seq.addAll(leftContext);
		seq.add(toAnalyze);
		seq.addAll(rightContext);
		
		int toAnalyzePos = leftContext.size();
		
		String query = "MATCH (toAnalyze:Pattern), ";
		
		for (int i=0; i<seq.size(); i++) {
			if(i!=0) {
				query = query + ",";
			}
			query = query + "(seq:Pattern)-[r"+i+":ELEMENT]->(el"+i+":Pattern)";
		}
		
		query = query + " WHERE id(toAnalyze)="+seq.get(toAnalyzePos).id()+" AND ";
		
		//исключим уже найденные similarities
		query = query + "size((el"+toAnalyzePos+")<-[:SAME]-()-[:SAME]->(toAnalyze)) = 0 AND ";
		
		query = query + "id(el"+toAnalyzePos+")<>"+seq.get(toAnalyzePos).id();
		
		for (int i=0; i<seq.size(); i++) {
			if(i!=toAnalyzePos) {
				query = query + " AND ";
				query = query + "id(el"+i+")="+seq.get(i).id();
			}
		}
				
		for (int i=seq.size()-1; i>0; i--) {
			query = query + " AND r"+i+".num-r"+(i-1)+".num=1";
		}
		
		query = query + " RETURN seq, ";
		
		for (int i=0; i<seq.size(); i++) {
			if(i!=0) {
				query = query + ",";
			}
			query = query + "r"+i+",el"+i;
		}
		
		logger.info(query);
		
		Result r = session.run(query);
		while(r.hasNext()) {
	    	   Record rec = r.next();
	    	   
	    	   Pattern similar = pBuilder.getPattern(rec.get("el"+toAnalyzePos).asNode());
	    	   Pattern fromSeq = pBuilder.getPattern(rec.get("seq").asNode());
	    	   
	    	   boolean processed = false;
	    	   
	    	   for (SimilarityStructure sims : res) {
	    		   if(sims.getSimilarityPattern().equals(similar)) {
	    			   sims.addSequence(fromSeq);
	    			   processed = true;
	    		   }
	    	   }
	    	   
	    	   if(!processed) {
	    		   SimilarityStructure simStruct = new SimilarityStructure(toAnalyze, similar);
	    		   simStruct.addSequence(fromSeq);
	    		   res.add(simStruct);
	    	   }
	        }
		
		return res;
	}

	/**
	 * Найти полностью совпадающие последовательности во всей БД
	 * @param seq последовательность для поиска
	 * @return список эквивалентных последовательностей
	 */
	public List<EqualStructure> getSequenceMatches(List<Pattern> seq) {
		List<EqualStructure> res = new ArrayList<EqualStructure>();
		
		String query = "MATCH ";
		
		for (int i=0; i<seq.size(); i++) {
			if(i!=0) {
				query = query + ",";
			}
			query = query + "(seq:Pattern)-[r"+i+":ELEMENT]->(el"+i+":Pattern)";
		}
		
		query = query + " WHERE ";
		
		for (int i=0; i<seq.size(); i++) {
			if(i!=0) {
				query = query + " AND ";
			}
			query = query + "id(el"+i+")="+seq.get(i).id();
		}
				
		/*
		if(seq.size()==2) {
			query = query + " AND r1.num-r0.num=1";
		}
		if(seq.size()==3) {
			query = query + " AND r2.num-r1.num=1 AND r1.num-r0.num=1";
		}
		*/
		for (int i=seq.size()-1; i>0; i--) {
			query = query + " AND r"+i+".num-r"+(i-1)+".num=1";
		}
		
		query = query + " RETURN seq, ";
		
		for (int i=0; i<seq.size(); i++) {
			if(i!=0) {
				query = query + ",";
			}
			query = query + "r"+i+",el"+i;
		}
		
		Result r = session.run(query);
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   
    	   List<PathElement> elPaths = new ArrayList<PathElement>();
    	   
    	   for (int i=0; i<seq.size(); i++) {
    		   PathElement pe = new PathElement(pBuilder.getPattern(rec.get("el"+i).asNode()), rBuilder.getRelation(rec.get("r"+i).asRelationship()));
    		   elPaths.add(pe);
    	   }
    	   
    	   EqualStructure es = new EqualStructure(pBuilder.getPattern(rec.get("seq").asNode()), elPaths, this);
    	   
    	   res.add(es);
        }
		
		return res;
	}

	/**
	 * Создать IO-паттерн
	 * @param name имя
	 * @return паттерн
	 */
	public Pattern createIOPattern(String name) {

        Result r = session.run("CREATE (newPattern" + prepareIOPatternLabels(name) +
                        "{name: $currentPatternName, actVal: 1, actTime: timestamp()}) RETURN newPattern",
                parameters("currentPatternName", name));
        while(r.hasNext()) {
            Record rec = r.next();
            return pBuilder.getPattern(rec.get("newPattern").asNode());
        }
        return  null;
    }

	/**
	 * Найти IO-паттерн по его текстовому имени
	 * @param name имя
	 * @return паттерн
	 */
	public Pattern findIONodeByName(String name) {
		Pattern res = null;
		
		Result r = session.run("MATCH (current:Pattern:IO {name: $currentPatternName}) RETURN current",
        		parameters("currentPatternName", name));
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = pBuilder.getPattern(rec.get("current").asNode());
        }
		
		return res;
	}

	/**
	 * Найти путь вверх
	 * Выбирается один самый мощный по формуле ActivityFormula
	 * @param currentPattern откуда поднимаемся
	 * @param excludeWays исключая пути
	 * @return связь и паттерн
	 */
	public PathElement getWayUpStrict(Pattern currentPattern, List<Relation> excludeWays) {
		
		//TODO: need to solve identical patterns included in current (??? вроде как работает)
		
		PathElement res = null;
		
		if(currentPattern.hasLabel(NodeLabel.Technical())) {
			return null;
		}
        		
		Result r = session.run("MATCH (current:Pattern)<-[way]-(pt:Pattern) WHERE id(current) = $currentPatternID AND type(way) = $relType"+ParamValues.excludeWays("way", excludeWays)+" RETURN pt, way "+ParamValues.OrderByActivity("pt", "way"),
        		parameters("currentPatternID", currentPattern.id(), "relType", RelationshipType.Element()));
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   Pattern pt = pBuilder.getPattern(rec.get("pt").asNode());
    	   Relation way = rBuilder.getRelation(rec.get("way").asRelationship());
    	   res = new PathElement(pt, way);
    	   logger.info("getWayUpStrict "+ pt.getName() + way.getNum());
        }
        
        return res;
	}

	/**
	 * Подготовить метки для нового IO-паттерна
	 * @param name текстовое представление IO-паттерна
	 * @return строка с метками
	 */
	private String prepareIOPatternLabels (String name) {
        StringBuilder defaultLabels = new StringBuilder(20);
		defaultLabels.append(':')
				.append(NodeLabel.IO() )
				.append(':')
				.append(NodeLabel.Pattern());
	    if (isDelimiter(name)) {
	       defaultLabels.append(':')
				   .append(NodeLabel.Delimiter());
        }
	    return  defaultLabels.toString();
    }


	/**
	 * Получить элемент последовательности
	 * @param sequence последовательность
	 * @param elementNum порядковый номер паттерна
	 * @return паттерн
	 */
	protected Pattern getElementOfSequence(Pattern sequence, int elementNum) {
		Pattern res = null;
		
		if(!sequence.hasLabel(NodeLabel.Sequence())) {
			return null;
		}
		
		Result r = session.run("MATCH (pt:Pattern)-[way:ELEMENT]->(res:Pattern) WHERE id(pt) = $currentPatternID AND way.num = $elNum RETURN res",
        		parameters("currentPatternID", sequence.id(), "elNum", elementNum));
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = pBuilder.getPattern(rec.get("res").asNode());
        }
		
		return res;
	}

	/**
	 * Получить технический TIMELINE паттерн
	 * @return TIMELINE паттерн
	 */
	protected Pattern getTimeline() {
		Pattern res = null;
		
		Result r = session.run("MATCH (res:Tech:Sequence:Pattern) WHERE res.name = \"[TIMELINE]\" RETURN res");
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   res = pBuilder.getPattern(rec.get("res").asNode());
        }
		
		return res;
	}

	/**
	 * Получить имя паттерна из его составляющих
	 * @param currentPattern паттерн
	 * @return имя
	 */
	public String resolve(Pattern currentPattern) {
		String res = "";
		
		Result r = null;
		
		if(currentPattern.hasLabel(NodeLabel.Sequence())) {
			r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID RETURN pt ORDER BY way.num",
        		parameters("currentPatternID", currentPattern.id()));
		} else if (currentPattern.hasLabel(NodeLabel.Meaning())||currentPattern.hasLabel(NodeLabel.Similarity())) {
			r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID RETURN pt "+ParamValues.OrderByActivity("pt", "way"),
	        		parameters("currentPatternID", currentPattern.id()));
		} else if (currentPattern.hasLabel(NodeLabel.IO())) {
			return currentPattern.getName();
		}
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   Pattern pt = pBuilder.getPattern(rec.get("pt").asNode());
    	   if (pt.hasLabel(NodeLabel.IO())) {
    		   res = res + pt.getName();
    	   } else {
    		   res = res + resolve(pt);
    	   }
        }
        
        return res;
	}

	/**
	 * Обертка createNewSequence для вызова без имени
	 * имя берётся из IO-составляющих и обрезается, если слишком длинное
	 * @param patterns последовательность
	 * @return
	 */
	public Pattern createNewSequence(List<Pattern> patterns) {
		Pattern res = null;
		if(patterns.size()>0) {
			//List<Node> nodes = new ArrayList<Node>();
			String name = "";
			
			for (Pattern lp : patterns) {
				//nodes.add(lp.getNode());
				name = name + lp.getResolved();
			}
			if(name.length()==0){
				name = null;
			} else if (name.length() > 200) {
				name = name.substring(1,200) + "[...]";
			}
			res = createNewSequence(patterns, name);
		}
		return res;
	}

	/**
	 * Создать новую иерархическую структуру - похожесть
	 * @param sims подготовленная структура
	 * @return паттерн-похожесть
	 */
	public Pattern createNewSimilarity(SimilarityStructure sims) {
		Pattern res = null;
		
		String name = sims.getOriginalPattern().getResolved()+"|"+sims.getSimilarityPattern().getResolved();
			
		String query = "MATCH ";
		
		query = query + "(orig:Pattern), (similar:Pattern), ";
			
			for (int i=0; i<sims.size(); i++) {
				if(i!=0) {
					query = query + ",";
				}
				query = query + " (el"+i+":Pattern)";
			}
			
			query = query + " WHERE";
			query = query + " id(orig)="+sims.getOriginalPattern().id();
			query = query + " AND id(similar)="+sims.getSimilarityPattern().id();
			
			for (int i=0; i<sims.size(); i++) {
				query = query + " AND ";
				query = query + "id(el"+i+")="+sims.getSequences().get(i).id();
			}
			
			query = query + " CREATE (newPattern:Similarity:Pattern {name: $currentPatternName, actVal: 1, actTime: timestamp()})";
			query = query + ", (orig)<-[:SAME {weight: 0.001}]-(newPattern)-[:SAME {weight: 0.001}]->(similar)";
			
			for (int i=0; i<sims.size(); i++) {
				query = query + ", (el"+i+")<-[:FROM {weight: 0.001}]-(newPattern)";
			}
			
			Result r = session.run(query+" RETURN newPattern",
	        		parameters("currentPatternName", name));
			while(r.hasNext()) {
	    	   Record rec = r.next();
	    	   res = pBuilder.getPattern(rec.get("newPattern").asNode());
	        }
		
		return res;
	}

	/**
	 * Создать новую иерархическую структуру - последовательность
	 * @param patterns упорядоченная последовательность паттернов
	 * @param name имя для предка
	 * @return предок последовательности
	 */
	public Pattern createNewSequence(List<Pattern> patterns, String name) {
		Pattern res = null;
		
		if(patterns.size()>0) {
			
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			
			if(name == null) {
				name = timestamp.toString();
			}
			
			String query = "MATCH";
			
			for (int i=0; i<patterns.size(); i++) {
				if(i!=0) {
					query = query + ",";
				}
				query = query + " (el"+i+":Pattern)";
			}
			
			query = query + " WHERE ";
			
			for (int i=0; i<patterns.size(); i++) {
				if(i!=0) {
					query = query + " AND ";
				}
				query = query + "id(el"+i+")="+patterns.get(i).id();
			}
			
			query = query + " CREATE (newPattern:Sequence:Pattern {name: $currentPatternName, actVal: 1, actTime: timestamp()})";
			
			for (int i=0; i<patterns.size(); i++) {
				query = query + ", (el"+i+")<-[:ELEMENT {num: "+i+", weight: 0.001}]-(newPattern)";
			}
			
			Result r = session.run(query+" RETURN newPattern",
	        		parameters("currentPatternName", name));
			while(r.hasNext()) {
	    	   Record rec = r.next();
	    	   res = pBuilder.getPattern(rec.get("newPattern").asNode());
	        }
						
		}
		
		return res;
	}

	/**
	 * Раскрыть содержимое паттерна (имена его потомков IO-паттернов)
	 * @param currentPattern паттерн
	 * @return список имен
	 */
	public List<String> resolveInList(Pattern currentPattern) {
		List<String> res = new ArrayList<String>();
		
		Result r = null;
		
		if(currentPattern.hasLabel(NodeLabel.Sequence())) {
			r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID RETURN pt ORDER BY way.num",
        		parameters("currentPatternID", currentPattern.id()));
		} else if (currentPattern.hasLabel(NodeLabel.Meaning())||currentPattern.hasLabel(NodeLabel.Similarity())) {
			r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID RETURN pt "+ParamValues.OrderByActivity("pt", "way"),
	        		parameters("currentPatternID", currentPattern.id()));
		} else if (currentPattern.hasLabel(NodeLabel.IO())) {
			res.add(currentPattern.getName());
			return res;
		}
		while(r.hasNext()) {
    	   Record rec = r.next();
    	   Pattern pt = pBuilder.getPattern(rec.get("pt").asNode());
    	   if (pt.hasLabel(NodeLabel.IO())) {
    		   res.add(pt.getName());
    	   } else {
    		   res.addAll(resolveInList(pt));
    	   } 
        }
        
        return res;
	}

	/**
	 * Получить потомков паттерна
	 * @param currentPattern паттерн
	 * @return упорядоченные паттерны со связями
	 */
    public List<PathElement> getChildPatterns(Pattern currentPattern) {
        List<PathElement> res = new ArrayList<PathElement>();

        Result r = null;

        if(currentPattern.hasLabel(NodeLabel.Sequence())) {
            r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID RETURN pt, way ORDER BY way.num",
                    parameters("currentPatternID", currentPattern.id()));
        } else if (currentPattern.hasLabel(NodeLabel.Meaning())||currentPattern.hasLabel(NodeLabel.Similarity())) {
            r = session.run("MATCH (current:Pattern)-[way]->(pt:Pattern) WHERE id(current) = $currentPatternID RETURN pt, way "+ParamValues.OrderByActivity("pt", "way"),
                    parameters("currentPatternID", currentPattern.id()));
        } else if (currentPattern.hasLabel(NodeLabel.IO())) {
            return res;
        }
        while(r.hasNext()) {
            Record rec = r.next();
            PathElement pe = new PathElement(pBuilder.getPattern(rec.get("pt").asNode()),rBuilder.getRelation(rec.get("way").asRelationship()));
            res.add(pe);
        }

        return res;
    }

	public void attachToTimeline(List<Pattern> organizedStructure) {
		
		attachToTimelineNodes(organizedStructure);
		
	}
	
	public void createElementRelation(Pattern parent, Pattern child, int pos, double weight) {

		Result r = session.run("MATCH (parent), (child) " + 
				"WHERE id(parent) = $parentID AND id(child) = $childID " +
				"CREATE (child)<-[rel:ELEMENT]-(parent) SET rel.num = $inPos, rel.weight = $relWeight",
        		parameters("parentID", parent.id(), "childID", child.id(), "inPos", pos, "relWeight", weight)); 		
        	
		wait(100);
		
	}
	
	public void createSameRelation(Pattern parent, Pattern child, double weight) {

		Result r = session.run("MATCH (parent), (child) " + 
				"WHERE id(parent) = $parentID AND id(child) = $childID " +
				"CREATE (child)<-[rel:SAME]-(parent) SET rel.weight = $relWeight",
        		parameters("parentID", parent.id(), "childID", child.id(), "relWeight", weight)); 		
        	
		wait(100);
		
	}
	
	public void createRelationFromSample(Pattern parent, Pattern child, Relation rel) {

		if(rel.getType().equals(RelationshipType.Element())) {
			createElementRelation(parent, child, rel.getNum(), rel.getWeight());
		} else if (rel.getType().equals(RelationshipType.Same())) {
			createSameRelation(parent, child, rel.getWeight());
		}
		
	}

	/**
	 * Обёртка insertPatternsIntoSequence для быстрого вызова без флага unprocessed
	 * unprocessed=false
	 * @param seq Последовательность, в которую вставляем
	 * @param pattern Паттерн, который вставляем
	 * @param pos Позиция в последовательности
	 * @param weight Вес новой связи
	 */
    public void insertPatternsIntoSequence(Pattern seq, Pattern pattern, int pos, double weight) {
        insertPatternsIntoSequence(seq, pattern, pos, weight, false);
    }

    /**
     * Вставляем паттерн в последовательность на определённую позицию.
     * Если после указанной позиции есть паттерны, они будут сдвинуты вперёд (номера на их связях поменяются на +1)
     * @param seq Последовательность, в которую вставляем
     * @param pattern Паттерн, который вставляем
     * @param pos Позиция в последовательности
     * @param weight Вес новой связи
     * @param unprocessed Признак необработанности - для выборки и обработки новых последовательностей в режиме сна
     */
	public void insertPatternsIntoSequence(Pattern seq, Pattern pattern, int pos, double weight, boolean unprocessed) {

        String addRelProperties = "";
		if (unprocessed) {
            addRelProperties = addRelProperties+", rel.unprocessed = true";
        }

		/*Result r = session.run("MATCH p = (seq)-[r:ELEMENT]->(el), (newEl:Pattern) " + 
				"WHERE id(seq) = $seqID AND r.num >= $inPos AND id(newEl) = $newElID " + 
				"FOREACH (r IN relationships(p) | SET r.num = r.num + 1) "
				+ "CREATE (newEl)<-[rel:ELEMENT]-(seq) SET rel.num = $inPos, rel.weight = $relWeight",
        		parameters("seqID", seq.id(), "newElID", pattern.id(), "inPos", pos, "relWeight", weight));
        	*/	
        	
		Result r1 = session.run("MATCH p = (seq)-[r:ELEMENT]->(el) " + 
				"WHERE id(seq) = $seqID AND r.num >= $inPos " + 
				"FOREACH (r IN relationships(p) | SET r.num = r.num + 1) ",
        		parameters("seqID", seq.id(), "inPos", pos));
		
		wait(100);
		
		Result r2 = session.run("MATCH (seq:Pattern), (newEl:Pattern) " + 
				"WHERE id(seq) = $seqID AND id(newEl) = $newElID " + 
				//"MERGE (seq)-[rel:ELEMENT]->(newEl) SET rel.num = $inPos, rel.weight = $relWeight",
				"CREATE (seq)-[rel:ELEMENT]->(newEl) SET rel.num = $inPos, rel.weight = $relWeight"+addRelProperties,
        		parameters("seqID", seq.id(), "newElID", pattern.id(), "inPos", pos, "relWeight", weight));
		
		wait(100);
		
	}
	
	// This remove only relation and recalc nums after
	/**
	 * Удалить паттерн (только его связь!) из последовательности и пересчитать номера остальных связей
	 * @param seq из какой последовательности
	 * @param pos какую по счёту связь
	 */
	public void removePatternFromSequence(Pattern seq, int pos) {
		Result r1 = session.run("MATCH (seq)-[rd:ELEMENT]->(el) WHERE id(seq) = $seqID AND rd.num = $inPos "
				+"DETACH DELETE rd",
        		parameters("seqID", seq.id(), "inPos", pos));
		
		wait(100);
		
		Result r2 = session.run("MATCH p = (seq)-[r:ELEMENT]->(el)" + 
				"WHERE id(seq) = $seqID AND r.num > $inPos " + 
				"FOREACH (r IN relationships(p) | SET r.num = r.num - 1)",
        		parameters("seqID", seq.id(), "inPos", pos));
		
		wait(100);
	}

	/**
	 * Удалить связь
	 * @param rel связь
	 */
	public void deleteRelation(Relationship rel) {
		Result r = session.run("MATCH ()-[rd]-() WHERE id(rd) = $relID "
				+"DETACH DELETE rd",
        		parameters("relID", rel.id()));
		
		wait(100);
	}

	/**
	 * Удалить паттерн из БД
	 * @param pattern паттерн
	 */
	public void deletePattern(Pattern pattern) {
		Result r = session.run("MATCH ()-[rp]->(pt:PATTERN)-[rc]->() WHERE id(pt) = $patternID "
				+"DETACH DELETE rp,rc,pt",
        		parameters("patternID", pattern.id()));
		
		wait(100);
	}

	/**
	 * Уменьшить вес всех связей
	 * @param decreaseValue на сколько уменьшить
	 */
	public void decreaseRelationsWeight(double decreaseValue) {
		Result r = session.run("MATCH ()-[rd]->() WHERE rd.weight > 0.001 SET rd.weight = rd.weight - $decreaseValue",
        		parameters("decreaseValue", decreaseValue));
		
		wait(200);
	}

	/**
	 * Скинуть активность паттернов на 0
	 */
	protected void flushPatternsActivity() {
		Result r = session.run("MATCH (n:Pattern) WHERE n.actVal <> 0 SET n.actVal = 0, n.actTime = timestamp()");
		wait(200);
	}

	/**
	 * Техническое ожидание
	 * @param ms милисекунд
	 */
	public static void wait(int ms)
	{
	    try
	    {
	        Thread.sleep(ms);
	    }
	    catch(InterruptedException ex)
	    {
	        Thread.currentThread().interrupt();
	    }
	}

	/**
	 * Прикрепить новые паттерны к timeline
	 * @param nodes новые паттерны
	 */
	public void attachToTimelineNodes(List<Pattern> nodes) {
		
		Pattern timeline = getTimeline();
		Integer inPos = lengthOfSequence(timeline)-1;
		
		for (Pattern n : nodes) {
			
			insertPatternsIntoSequence(timeline, n, inPos, 0.001, true);
			
			inPos ++;
		}
	}

	/**
	 * Получить последовательность необработанных паттернов, прикреплённых к timeline
	 * @return последовательность необработанных паттернов, прикреплённых к timeline со связями
	 */
    public List<PathElement> getUnprocessedHighLevelSequence() {

        List<PathElement> res = new ArrayList<PathElement>();

        Pattern timeline = getTimeline();

        Result r = null;

        r = session.run("MATCH (timeline:Pattern)-[way]->(pt:Pattern) WHERE id(timeline) = $timelineID AND way.unprocessed = true RETURN pt, way ORDER BY way.num",
                    parameters("timelineID", timeline.id()));

        while(r.hasNext()) {
            Record rec = r.next();
            PathElement pe = new PathElement(pBuilder.getPattern(rec.get("pt").asNode()),rBuilder.getRelation(rec.get("way").asRelationship()));
            res.add(pe);
        }

        return res;

    }

	/**
	 * Скинуть флаг unprocessed
	 * Вызывается после полной обработки новых паттернов
	 * @param seq последовательность, со связей которой скидываем
	 */
	public void setProcessed(List<PathElement> seq) {
		for (PathElement pe:seq
			 ) {
			Result r = null;
			r = session.run("MATCH ()-[way]->() WHERE id(way) = $wayID SET way.unprocessed = false",
					parameters("wayID", pe.getWay().id()));
		}
	}
	
	public void deleteAll() {
		logger.info("[deleteAll] CLEAR ALL DB");
		Result r = session.run("MATCH (n) DETACH DELETE n");
		wait(200);
	}
	
	public void createTestData() {
		logger.info("[createTestData]");
		Result r = session.run("CREATE (m:IO:Pattern { name: \"M\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(e:IO:Pattern { name: \"E\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(n:IO:Pattern { name: \"N\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(t:IO:Pattern { name: \"T\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(o:IO:Pattern { name: \"O\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(i:IO:Pattern { name: \"I\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(h:IO:Pattern { name: \"H\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(terminator:Tech:IO:Pattern { name: \"[terminator]\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(space:IO:Delimiter:Pattern { name: \"_\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(att:IO:Delimiter:Pattern { name: \"!\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(me:Sequence:Pattern { name: \"ME\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(nt:Sequence:Pattern { name: \"NT\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(hi:Sequence:Pattern { name: \"HI\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(memento:Sequence:Pattern { name: \"MEMENTO\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(mei:Sequence:Pattern { name: \"MEI\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(him:Sequence:Pattern { name: \"HIM\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(meiorhim:Meaning:Pattern { name: \"MEI|HIM\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(mementoMei:Sequence:Pattern { name: \"MEMENTO MEI!\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(m)<-[:ELEMENT {num: 0, weight: 0.002}]-(me),(e)<-[:ELEMENT {num: 1, weight: 0.002}]-(me),\r\n" + 
				"(n)<-[:ELEMENT {num: 0, weight: 0.001}]-(nt),(t)<-[:ELEMENT {num: 1, weight: 0.001}]-(nt),\r\n" + 
				"(me)<-[:ELEMENT {num: 0, weight: 0.001}]-(memento),(me)<-[:ELEMENT {num: 1, weight: 0.002}]-(memento),\r\n" + 
				"(nt)<-[:ELEMENT {num: 2, weight: 0.001}]-(memento),(o)<-[:ELEMENT {num: 3, weight: 0.001}]-(memento),\r\n" + 
				"(me)<-[:ELEMENT {num: 0, weight: 0.001}]-(mei),(i)<-[:ELEMENT {num: 1, weight: 0.001}]-(mei),\r\n" + 
				"(h)<-[:ELEMENT {num: 0, weight: 0.001}]-(hi),(i)<-[:ELEMENT {num: 1, weight: 0.001}]-(hi),\r\n" + 
				"(hi)<-[:ELEMENT {num: 0, weight: 0.001}]-(him),(m)<-[:ELEMENT {num: 1, weight: 0.001}]-(him),\r\n" + 
				"(mei)<-[:SAME {weight: 0.004}]-(meiorhim),(him)<-[:SAME {weight: 0.003}]-(meiorhim),\r\n" + 
				"(memento)<-[:ELEMENT {num: 0, weight: 0.001}]-(mementoMei),(space)<-[:ELEMENT {num: 1, weight: 0.001}]-(mementoMei),\r\n" + 
				"(mei)<-[:ELEMENT {num: 2, weight: 0.001}]-(mementoMei), (att)<-[:ELEMENT {num: 3, weight: 0.001}]-(mementoMei),\r\n" + 
				"(timeline:Tech:Sequence:Pattern { name: \"[TIMELINE]\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(him)<-[:ELEMENT {num: 0, weight: 0.001}]-(timeline),\r\n" + 
				"(space)<-[:ELEMENT {num: 1, weight: 0.001}]-(timeline),\r\n" + 
				"(mementoMei)<-[:ELEMENT {num: 2, weight: 0.001}]-(timeline),\r\n" + 
				"(terminator)<-[:ELEMENT {num: 3, weight: 0.001}]-(timeline)");
		wait(1000);
	}
	
	public void createTechPatterns() {
		logger.info("[createTechPatterns]");
		Result r = session.run("CREATE " +
				"(terminator:Tech:IO:Pattern { name: \"[terminator]\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(timeline:Tech:Sequence:Pattern { name: \"[TIMELINE]\", actVal: 1, actTime: 1592850863403 }),\r\n" + 
				"(terminator)<-[:ELEMENT {num: 0, weight: 0.001}]-(timeline)");
		wait(1000);
	}
	
	public void resetDB() {
		deleteAll();
		createTechPatterns();
	}
	
	public void resetDBandCreateTestData() {
		deleteAll();
		createTestData();
	}

	public void setRelationWeight(Relation relation, double newWeight) {
		session.run("MATCH (:Pattern)-[way]-(:Pattern) WHERE id(way) = $currentWayID SET way.weight = $newWeight",
        		parameters("currentWayID", relation.id(), "newWeight", newWeight));
	}

}
