/**
 * 
 */
package net.meanings.app;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import net.meanings.model.QHelper;
import net.meanings.model.SemanticLevel;
import net.meanings.model.graph.PatternBuilder;

/**
 * @author sev
 *
 */
public class DependencyInjection {

	private List<SemanticLevel> sLevels;
	private final Driver driver;
	private Session sessionInternal;
	protected QHelper qhelper;
	
	public DependencyInjection(String uri, String user, String password) {
		initSemanticLevels();
		driver = GraphDatabase.driver(uri, AuthTokens.basic( user, password ));
		qhelper = new QHelper(session(),sLevels);
	}
	
	public PatternBuilder PBuilder() {
		return qhelper.getPatternBuilder();
	}
	
	public QHelper QHelper() {
		return qhelper;
	}
	
	public List<SemanticLevel> SLevels() {
		return sLevels;
	}
	
	public void terminate()
    {
        driver.close();
    }
	
	private Session session()
    {
		if (sessionInternal==null) {
			sessionInternal = driver.session();
		}
		return sessionInternal;
    }
	
	private void initSemanticLevels() {
		//Уровни
		sLevels = new ArrayList<SemanticLevel>();
				
		//Буквы
		SemanticLevel symbols = new SemanticLevel("Symbols", false);
		//Сколько объединять в последовательность (пока не используется!)
		symbols.setSequenceSize(2);
		//Минимальное количество таких же последовательностей для выделения в отдельный узел - важно
		symbols.setMinMatchesToReplace(1);

		//Слова
		SemanticLevel words = new SemanticLevel("Words", true);
		words.addDelimitter(" ");
		words.addDelimitter(",");
		words.addDelimitter("-");
		words.addDelimitter("_");
		words.addDelimitter(":");
		//Минимальное количество таких же последовательностей для выделения в отдельный узел - важно
		words.setMinMatchesToReplace(1);

		//Предложения
		SemanticLevel sentences = new SemanticLevel("Sentences", true);
		sentences.addDelimitter(".");
		sentences.addDelimitter(";");
		sentences.addDelimitter("!");
		sentences.addDelimitter("?");
		sentences.addDelimitter("rn");
		sentences.setMinMatchesToReplace(1);

		//Параграфы
		SemanticLevel paragraphs = new SemanticLevel("Paragraph", true);
		paragraphs.addDelimitter("pr");

		//Добавляем уровни в массив
		sLevels.add(symbols);
		sLevels.add(words);
		sLevels.add(sentences);
		sLevels.add(paragraphs);

	}

}
