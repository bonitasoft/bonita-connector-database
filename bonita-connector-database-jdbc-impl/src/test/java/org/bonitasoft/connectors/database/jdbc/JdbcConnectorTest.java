/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.bonitasoft.connectors.database.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.matchers.JUnitMatchers.hasItems;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Baptiste Mesta
 * @author Frédéric Bouquet
 */
public class JdbcConnectorTest {

	public static String PASSWORD;

	public static String USERNAME;

	public static String JDBC_DRIVER;

	public static String JDBC_URL;

	private Properties prop;

	private String db;

	private String tableName;

	@Before
	public void setUp() throws Exception {
		getProperties();
		createTable();
		insertValues();
	}

	@After
	public void tearDown() throws Exception {
		dropTable();
	}

	private void getProperties() throws Exception {
		prop = new Properties();
		prop.load(this.getClass().getResourceAsStream("/Databases.properties"));
		db = prop.get("type_db") + ".";
		PASSWORD = (String) prop.get(db + "PASSWORD");
		USERNAME = (String) prop.get(db + "USERNAME");
		JDBC_DRIVER = (String) prop.get(db + "JDBC_DRIVER");
		JDBC_URL = (String) prop.get(db + "JDBC_URL");

	}

	@Test
	public void testValidateInputParametersWithNullValues() {
		Map<String, Object> parameters = new HashMap<String, Object>();
		JdbcConnector jdbcConnector = new JdbcConnector();
		jdbcConnector.setInputParameters(parameters);
		try {
			jdbcConnector.validateInputParameters();
			fail("Connector validation should be failing");
		} catch (ConnectorValidationException e) {
			assertThat(e.getMessage(), containsString("Driver"));
			assertThat(e.getMessage(), containsString("Url"));
			assertThat(e.getMessage(), containsString("Script"));
		}
	}

	@Test
	public void testValidateInputParametersWithEmptyValues() {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(JdbcConnector.DRIVER, "");
		parameters.put(JdbcConnector.URL, "");
		parameters.put(JdbcConnector.SCRIPT, "");
		JdbcConnector jdbcConnector = new JdbcConnector();
		jdbcConnector.setInputParameters(parameters);
		try {
			jdbcConnector.validateInputParameters();
			fail("Connector validation should be failing");
		} catch (ConnectorValidationException e) {
			assertThat(e.getMessage(), containsString("Driver"));
			assertThat(e.getMessage(), containsString("Url"));
			assertThat(e.getMessage(), containsString("Script"));
		}
	}

	@Test
	public void testValidateInputParameters() throws ConnectorValidationException {
		JdbcConnector jdbcConnector = getJdbcConnectorWithNoParameter();
		jdbcConnector.validateInputParameters();
	}

	@Test
	public void testGetAllValues() throws Exception {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithNoParameter();
		final List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
		assertEquals(1, rowSet.get(0).get(0));
		assertEquals("John", rowSet.get(0).get(1));
		assertEquals("Doe", rowSet.get(0).get(2));
		assertEquals(27, rowSet.get(0).get(3));
		assertEquals(15.4, rowSet.get(0).get(4));
		assertEquals(2, rowSet.get(1).get(0));
		assertEquals("Jane", rowSet.get(1).get(1));
		assertEquals("Doe", rowSet.get(1).get(2));
		assertEquals(31, rowSet.get(1).get(3));
		assertEquals(15.9, rowSet.get(1).get(4));
	}

	@Test
	public void testGetFirstNames() throws Exception {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithNoParameter();
		final List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
		final Object johnName = rowSet.get(0).get(1);
		assertEquals("John", johnName);
		final Object janeName = rowSet.get(1).get(1);
		assertEquals("Jane", janeName);
	}

	@Test
	public void testGetInBoundIndexes() throws Exception {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithNoParameter();
		final List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
		final Object johnName = rowSet.get(0).get(1);
		assertEquals("John", johnName);
		final Object janeName = rowSet.get(1).get(1);
		assertEquals("Jane", janeName);
	}

	@Test
	public void testManipulateResultSet() throws Exception {
		final JdbcConnector datasourceConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT,
				(Object) getSelectAllQuery()));
		datasourceConnector.connect();
		final Map<String, Object> execute = datasourceConnector.execute();
		ResultSet resultSet = (ResultSet) execute.get("resultset");
		assertNotNull(resultSet);
		assertThat(resultSet.getFetchSize(), is(1));
		datasourceConnector.disconnect();
	}

	@Test(expected = ConnectorException.class)
	public void testWrongTableQuery() throws Throwable {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) "SELECT * FROM Bonita"));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
	}

	@Test
	public void testWrongPersonIdQuery() throws Exception {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getInvalidSelectUserId()));
		final List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
		assertTrue(rowSet.isEmpty());
	}

	@Test
	public void testCreateSelectAndDropTable() throws Exception {
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getSelectAllQuery()));
		List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
		List<Object> actual = rowSet.get(1);
		final List<Object> expected = Arrays.asList(new Object[] { 2, "Jane", "Doe", 31, 15.9 });

		assertEquals("John", rowSet.get(0).get(1));
		assertEquals("Jane", rowSet.get(1).get(1));
		assertEquals(expected, actual);

		jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getDeleteFirstInsert()));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();

		jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getSelectAllQuery()));
		rowSet = executeAndGetResult(jdbcConnector);
		actual = rowSet.get(0);
		assertEquals(1, rowSet.size());
		assertEquals("Jane", rowSet.get(0).get(1));
		assertEquals(expected, actual);
	}

	@Test
	public void testSingleOutputResult() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String query = " SELECT COUNT(*) FROM "+getTableName();
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.SINGLE);
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		Map<String, Object> result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		assertNull(result.get(JdbcConnector.RESULTSET_OUTPUT));
		assertNull(result.get(JdbcConnector.NROW_ONECOL_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.ONEROW_NCOL_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.TABLE_RESULT_OUTPUT));
		Object countResult = result.get(JdbcConnector.SINGLE_RESULT_OUTPUT);
		assertNotNull(countResult);
		assertEquals(2, countResult);

		query = " SELECT AVG(age) FROM "+getTableName();
		map.put(JdbcConnector.SCRIPT, query);
		jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		Object avgResult = result.get(JdbcConnector.SINGLE_RESULT_OUTPUT);
		assertNotNull(avgResult);
		assertEquals(String.valueOf(29), String.valueOf(avgResult));

		query = selectBuilder("age", "age=65", "id");
		map.put(JdbcConnector.SCRIPT, query);
		jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		Object nullResult = result.get(JdbcConnector.SINGLE_RESULT_OUTPUT);
		assertNull(nullResult);
	}
	
	

	@Test(expected=ConnectorException.class)
	public void testInvalidQueryForSingleOutputResult() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String query = "SELECT AVG(age),COUNT(*) FROM "+getTableName();
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.SINGLE);
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(map);
		try{
			jdbcConnector.connect();
			jdbcConnector.execute();
		}finally{
			jdbcConnector.disconnect();
		}
	}

	@Test
	public void testOneRowNColOutputResult() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String query = "SELECT firstname,lastname FROM "+getTableName() +" WHERE age=27";
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.ONE_ROW);
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		Map<String, Object> result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		assertNull(result.get(JdbcConnector.RESULTSET_OUTPUT));
		assertNull(result.get(JdbcConnector.NROW_ONECOL_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.SINGLE_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.TABLE_RESULT_OUTPUT));
		Object listResult = result.get(JdbcConnector.ONEROW_NCOL_RESULT_OUTPUT);
		assertNotNull(listResult);
		assertTrue(listResult instanceof List);
		assertEquals(2, ((List) listResult).size());
		assertEquals("John",((List) listResult).get(0));
		assertEquals("Doe",((List) listResult).get(1));
		
		query = "SELECT firstname,lastname FROM "+getTableName() +" WHERE age=12";
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.ONE_ROW);
		jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		assertNull(result.get(JdbcConnector.RESULTSET_OUTPUT));
		assertNull(result.get(JdbcConnector.NROW_ONECOL_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.SINGLE_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.TABLE_RESULT_OUTPUT));
		listResult  =result.get(JdbcConnector.ONEROW_NCOL_RESULT_OUTPUT);
		assertNotNull(listResult);
		assertTrue(listResult instanceof List);
		assertEquals(0, ((List) listResult).size());
	}
	
	@Test(expected=ConnectorException.class)
	public void testInvalidQueryForOneRowNColumnOutputResult() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String query = "SELECT firstname,lastname FROM "+getTableName();
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.ONE_ROW);
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(map);
		try{
			jdbcConnector.connect();
			jdbcConnector.execute();
		}finally{
			jdbcConnector.disconnect();
		}
	}
	
	@Test
	public void testNRowOneColOutputResult() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String query = "SELECT firstname FROM "+getTableName();
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.N_ROW);
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		Map<String, Object> result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		assertNull(result.get(JdbcConnector.RESULTSET_OUTPUT));
		assertNull(result.get(JdbcConnector.ONEROW_NCOL_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.SINGLE_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.TABLE_RESULT_OUTPUT));
		Object listResult = result.get(JdbcConnector.NROW_ONECOL_RESULT_OUTPUT);
		assertNotNull(listResult);
		assertTrue(listResult instanceof List);
		assertEquals(2, ((List) listResult).size());
		assertEquals("John",((List) listResult).get(0));
		assertEquals("Jane",((List) listResult).get(1));
		
		query = "SELECT firstname FROM "+getTableName() +" WHERE age=12";
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.N_ROW);
		jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		assertNull(result.get(JdbcConnector.RESULTSET_OUTPUT));
		assertNull(result.get(JdbcConnector.ONEROW_NCOL_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.SINGLE_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.TABLE_RESULT_OUTPUT));
		listResult  =result.get(JdbcConnector.NROW_ONECOL_RESULT_OUTPUT);
		assertNotNull(listResult);
		assertTrue(listResult instanceof List);
		assertEquals(0, ((List) listResult).size());
	}
	
	@Test(expected=ConnectorException.class)
	public void testInvalidQueryForNRowOneColumnOutputResult() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String query = "SELECT firstname,lastname FROM "+getTableName();
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.N_ROW);
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(map);
		try{
			jdbcConnector.connect();
			jdbcConnector.execute();
		}finally{
			jdbcConnector.disconnect();
		}
	}
	
	@Test
	public void testTableOutputResult() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		String query = "SELECT * FROM "+getTableName();
		map.put(JdbcConnector.SCRIPT, query);
		map.put(JdbcConnector.OUTPUT_TYPE, JdbcConnector.TABLE);
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(map);
		jdbcConnector.connect();
		Map<String, Object> result = jdbcConnector.execute();
		jdbcConnector.disconnect();
		assertNull(result.get(JdbcConnector.RESULTSET_OUTPUT));
		assertNull(result.get(JdbcConnector.ONEROW_NCOL_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.SINGLE_RESULT_OUTPUT));
		assertNull(result.get(JdbcConnector.NROW_ONECOL_RESULT_OUTPUT));
		Object tableResult = result.get(JdbcConnector.TABLE_RESULT_OUTPUT);
		assertNotNull(tableResult);
		assertTrue(tableResult instanceof List);
		assertEquals(2, ((List) tableResult).size());
		Object row1 = ((List) tableResult).get(0);
		assertTrue(row1 instanceof List);
		assertEquals("1",String.valueOf(((List) row1).get(0)));
		assertEquals("John",((List) row1).get(1));
		assertEquals("Doe",((List) row1).get(2));
		assertEquals("27",String.valueOf(((List) row1).get(3)));
		assertEquals("15.4",String.valueOf(((List) row1).get(4)));
		
		Object row2 = ((List) tableResult).get(1);
		assertTrue(row2 instanceof List);
		assertEquals("2",String.valueOf(((List) row2).get(0)));
		assertEquals("Jane",((List) row2).get(1));
		assertEquals("Doe",((List) row2).get(2));
		assertEquals("31",String.valueOf(((List) row2).get(3)));
		assertEquals("15.9",String.valueOf(((List) row2).get(4)));
	}
	
	@Test
	public void testQueryShouldNotGetBackUnNeededColumn() throws Exception {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getFirstnameLastnameQuery()));
		final List<String> headers = executeAndGetColumns(jdbcConnector);
		final List<String> upperCaseHeaders = new ArrayList<String>(headers.size());
		for (final String header : headers) {
			upperCaseHeaders.add(header.toUpperCase());
		}
		assumeThat(upperCaseHeaders, hasItems("FIRSTNAME", "LASTNAME"));
		assertThat(upperCaseHeaders, not(hasItems("ID")));
	}

	@Test
	public void executeInsertOneLine() throws Exception {
		final List<List<Object>> result = queryAndCheck(insertBuilder("(firstname, age, lastname, average)", "('Arthur', 25, 'Doe', 17)"),
				selectBuilder("*", "firstname='Arthur'", "id"));
		assertThat(result.size(), equalTo(1));
		assertThat(result.get(0), hasItems((Object) "Arthur", 25));
	}

	@Test
	public void executeInsertMultiLine() throws Exception {
		List<List<Object>> result;
		if (db.contains("hsql")) {
			System.out.println("Multi-insertion is not possible with hsql version older than 2.0");
			return;
		} else {
			result = queryAndCheck(insertBuilder("(firstname, age, lastname, average)", "('Elias', 25, 'Doe', 17), ('Fred', 28, 'Da', 18)"),
					selectBuilder("*", "firstname='Elias' or firstname='Fred'", "firstname"));
		}
		assertThat(result.size(), equalTo(2));
		assertThat(result.get(0), hasItems((Object) "Elias", 25));
		assertThat(result.get(1), hasItems((Object) "Fred", 28));

	}

	@Test
	public void executeUpdateOneLine() throws Exception {
		final List<List<Object>> result = queryAndCheck(updateBuilder("age=25", "firstname='John'"), selectBuilder("*", "firstname='John'", "id"));
		assertThat(result.size(), equalTo(1));
		assertThat(result.get(0), hasItems((Object) "John", 25));
	}

	@Test
	public void executeUpdateMultiLine() throws Exception {
		simpleQuery(updateBuilder("age=93, firstname='Jany'", "firstname='Jane'"));
		final List<List<Object>> result = queryAndCheck(updateBuilder("age=97, firstname='Johny'", "firstname='John'"), selectBuilder("*", "age>90", "id"));
		assertThat(result.size(), equalTo(2));
		assertThat(result.get(0), hasItems((Object) "Johny", 97));
		assertThat(result.get(1), hasItems((Object) "Jany", 93));
	}

	@Test
	public void executeDeleteOneLine() throws Exception {
		List<List<Object>> result = queryAndCheck(insertBuilder("(firstname, age, lastname, average)", "('Arthur', 25, 'Doe', '17.0')"),
				selectBuilder("*", "firstname='Arthur'", "id"));
		assertThat(result.size(), equalTo(1));
		assertThat(result.get(0), hasItems("Arthur", (Object) "Doe", 25, 17.0));
		result = queryAndCheck(deleteBuilder("Firstname='Arthur'"), selectBuilder("*", "firstname='Arthur'", "id"));
		assertThat(result.size(), equalTo(0));
	}

	@Test
	public void executeBatchScript() throws Exception {
		genericBatchScriptTest(";");
	}

	@Test
	public void executeBatchScriptWithAnOtherSeparator() throws Exception {
		genericBatchScriptTest("|");
	}

	private void createTable() throws Exception {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getCreateTable()));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
	}

	private void simpleQuery(final Object query) throws ConnectorException {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, query));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
	}

	private List<List<Object>> queryAndCheck(final Object query, final Object checkQuery) throws Exception {
		List<List<Object>> result;
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, query));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
		final JdbcConnector checkConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, checkQuery));
		result = executeAndGetResult(checkConnector);
		return result;
	}

	private JdbcConnector getJdbcConnectorWithNoParameter() {
		final Map<String, Object> emptyMap = Collections.emptyMap();
		return getJdbcConnectorWithParameters(emptyMap);
	}

	JdbcConnector getJdbcConnectorWithParameters(final Map<String, Object> parameters) {
		final JdbcConnector jdbcConnector = new JdbcConnector();
		final Map<String, Object> defaultParameters = initiateConnectionParameters();
		defaultParameters.put(JdbcConnector.SCRIPT, getSelectAllQuery());
		defaultParameters.putAll(parameters);
		jdbcConnector.setInputParameters(defaultParameters);
		return jdbcConnector;
	}

	private Map<String, Object> initiateConnectionParameters() {
		final Map<String, Object> defaultParameters = new HashMap<String, Object>();
		defaultParameters.put(JdbcConnector.URL, JDBC_URL);
		defaultParameters.put(JdbcConnector.DRIVER, JDBC_DRIVER);
		defaultParameters.put(JdbcConnector.USERNAME, USERNAME);
		defaultParameters.put(JdbcConnector.PASSWORD, PASSWORD);
		return defaultParameters;
	}

	private void insertValues() throws Exception {
		JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getFirstInsertQuery()));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
		jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getSecondInsertQuery()));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
	}

	private void dropTable() throws Exception {
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getDropTableQuery()));
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
	}

	private String getBatchScript(final String separator) {
		String strBatchScript = (String) prop.get("batch_script");
		strBatchScript = strBatchScript.replace(";", separator);
		strBatchScript = strBatchScript.replace("auto_increment", (String) prop.get(db + "auto_increment"));
		if (db.contains("oracle")) {
			strBatchScript = strBatchScript.replace("id INTEGER", "id");
			strBatchScript = strBatchScript.replace("after_table_creation", prop.getProperty(db + "after_table_creation"));
		} else {
			strBatchScript = strBatchScript.replace("after_table_creation", "");
		}
		return strBatchScript;

	}

	protected String getCreateTable() {
		return createBuilder();
	}

	private String getFirstInsertQuery() {
		return insertBuilder("(firstname, age, lastname, average)", "('John', 27, 'Doe', 15.4)");
	}

	private String getSecondInsertQuery() {
		return insertBuilder("(firstname, age, lastname, average)", "('Jane', 31, 'Doe', 15.9)");
	}

	protected String getSelectAllQuery() {
		return selectBuilder("*", "1=1", "id");
	}

	private String createBuilder() {
		String strCreate = (String) prop.get(db + "create_table");
		strCreate = strCreate.replace("table_name", getTableName());
		strCreate = strCreate.replace("auto_increment", (String) prop.get(db + "auto_increment"));

		if (db.contains("oracle")) {
			strCreate = strCreate + prop.get(db + "after_table_creation");
		}

		return strCreate;

	}

	private String dropBuilder() {
		final String strDrop = (String) prop.get(db + "drop_table");
		return strDrop.replace("table_name", getTableName());

	}

	private String selectBuilder(final String columns, final String condition, final String order) {
		String strSelect = (String) prop.get(db + "select");
		strSelect = strSelect.replace("columns", columns).replace("table_name", getTableName()).replace("condition", condition).replace("order", order);
		return strSelect;
	}

	private String insertBuilder(final String columns, final String values) {
		String strInsert = (String) prop.get(db + "insert");
		String strIntoValues = "";
		if (db.contains("oracle")) {
			if (values.contains("),")) {
				final String[] tabValues = values.split("),");
				for (final String val : tabValues) {
					strIntoValues = strIntoValues + "\nINTO " + getTableName() + " " + columns + " \nVALUES " + val + ")";
				}
			} else {
				strIntoValues = "\nINTO " + getTableName() + columns + " \nVALUES " + values;
			}
		} else {
			strInsert = strInsert.replace("table_name", getTableName()).replace("columns", columns).replace("values", values);
		}
		return strInsert;
	}

	private String updateBuilder(final String set_clause, final String condition) {
		String strUpdate = (String) prop.get(db + "update");
		strUpdate = strUpdate.replace("table_name", getTableName()).replace("set_clause", set_clause).replace("condition", condition);
		return strUpdate;
	}

	private String deleteBuilder(final String condition) {
		String strDelete = (String) prop.get(db + "delete");
		strDelete = strDelete.replace("table_name", getTableName()).replace("condition", condition);
		return strDelete;
	}

	protected String getDeleteFirstInsert() {
		return deleteBuilder("firstname='John'");
	}

	protected String getDropTableQuery() {
		return dropBuilder();
	}

	protected String getInvalidSelectUserId() {
		return selectBuilder("*", "id=3", "id");
	}

	private String getFirstnameLastnameQuery() {
		return selectBuilder("firstname, lastname", "1=1", "id");
	}

	private List<List<Object>> executeAndGetResult(final JdbcConnector datasourceConnector) throws Exception {
		datasourceConnector.connect();
		final Map<String, Object> execute = datasourceConnector.execute();
		ResultSet data = (ResultSet) execute.get(JdbcConnector.RESULTSET_OUTPUT);
		final ResultSetMetaData metaData = data.getMetaData();
		final int fetchSize = data.getFetchSize();
		final int columnsCount = metaData.getColumnCount();
		List<List<Object>> result = toList(data, fetchSize, columnsCount);
		datasourceConnector.disconnect();

		return result;
	}
	private List<List<Object>> toList(final ResultSet resultSet, final int fetchSize, final int columnsCount) throws SQLException {
		final List<List<Object>> values = new ArrayList<List<Object>>(fetchSize);
		if (resultSet != null) {
			while (resultSet.next()) {
				final List<Object> row = new ArrayList<Object>();
				for (int j = 1; j <= columnsCount; j++) {
					final Object value = resultSet.getObject(j);
					row.add(value);
				}
				values.add(row);
			}
			resultSet.close();
		}
		return values;
	}
	private List<String> executeAndGetColumns(final JdbcConnector datasourceConnector) throws Exception{
		datasourceConnector.connect();
		final Map<String, Object> execute = datasourceConnector.execute();
		ResultSet data = (ResultSet) execute.get("resultset");

		ResultSetMetaData metaData = data.getMetaData();
		int columnsCount = metaData.getColumnCount();
		List<String> columns = new ArrayList<String>(columnsCount);

		for (int i = 1; i <= columnsCount; i++) {
			final String columnName = metaData.getColumnName(i);
			columns.add(columnName);
		}
		datasourceConnector.disconnect();

		return columns;
	}

	public final String getTableName() {
		if (tableName == null) {
			tableName = "test" + getOSName();
		}
		return tableName;
	}

	private String getOSName() {
		String osName;
		try {
			final String name = System.getProperty("os.name");
			if (name.startsWith("Lin")) {
				osName = "lin";
			} else if (name.startsWith("Win")) {
				osName = "win";
			} else if (name.startsWith("Mac")) {
				osName = "mac";
			} else {
				osName = "undefined" + Math.random();
			}
		} catch (final Exception e) {
			osName = "undefined" + Math.random();
		}
		return osName;
	}

	private void genericBatchScriptTest(final String separator) throws ConnectorException {
		final String script = getBatchScript(separator);

		final Map<String, Object> parameters = new HashMap<String, Object>(6);
		parameters.put(JdbcConnector.SCRIPT, script);
		parameters.put(JdbcConnector.SEPARATOR, separator);
		final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(parameters);
		jdbcConnector.connect();
		jdbcConnector.execute();
		jdbcConnector.disconnect();
	}

}
