/**
 * 
 */
package net.meanings;

import static org.junit.Assert.*;
import org.junit.Test;

import net.meanings.app.App;

/**
 * @author seliv
 *
 */
public class ResourcesTest {
	@Test
	public void testLoggingSettings() {
		assertNotNull(App.class.getResourceAsStream("/logback.xml"));
	}
}
