/*
 * Copyright (C) 2009 - 2020 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.bonitasoft.connectors.database.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Baptiste Mesta
 * @author Frédéric Bouquet
 */
class JdbcConnectorTest {

    public static String PASSWORD;

    public static String USERNAME;

    public static String JDBC_DRIVER;

    public static String JDBC_URL;

    private Properties prop;

    private String db;

    private String tableName;

    @BeforeEach
    void setUp() throws Exception {
        getProperties();
        createTable();
        insertValues();
    }

    @AfterEach
    void tearDown() throws Exception {
        dropTable();
    }

    private void getProperties() throws Exception {
        prop = new Properties();
        prop.load(this.getClass().getResourceAsStream("/connectors_config.properties"));
        db = prop.get("database.type_db") + ".";
        PASSWORD = (String) prop.get(db + "PASSWORD");
        USERNAME = (String) prop.get(db + "USERNAME");
        JDBC_DRIVER = (String) prop.get(db + "JDBC_DRIVER");
        JDBC_URL = (String) prop.get(db + "JDBC_URL");

    }

    @Test
    void testValidateInputParametersWithNullValues() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        JdbcConnector jdbcConnector = new JdbcConnector();
        jdbcConnector.setInputParameters(parameters);
        try {
            jdbcConnector.validateInputParameters();
            fail("Connector validation should be failing");
        } catch (ConnectorValidationException e) {
            assertThat(e.getMessage()).contains("Driver");
            assertThat(e.getMessage()).contains("Url");
            assertThat(e.getMessage()).contains("Script");
        }
    }

    @Test
    void testValidateInputParametersWithEmptyValues() {
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
            assertThat(e.getMessage()).contains("Driver");
            assertThat(e.getMessage()).contains("Url");
            assertThat(e.getMessage()).contains("Script");
        }
    }

    @Test
    void testValidateInputParameters() throws ConnectorValidationException {
        JdbcConnector jdbcConnector = getJdbcConnectorWithNoParameter();
        jdbcConnector.validateInputParameters();
    }

    @Test
    void testGetAllValues() throws Exception {
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
    void testGetFirstNames() throws Exception {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
        final Object johnName = rowSet.get(0).get(1);
        assertEquals("John", johnName);
        final Object janeName = rowSet.get(1).get(1);
        assertEquals("Jane", janeName);
    }

    @Test
    void testGetInBoundIndexes() throws Exception {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
        final Object johnName = rowSet.get(0).get(1);
        assertEquals("John", johnName);
        final Object janeName = rowSet.get(1).get(1);
        assertEquals("Jane", janeName);
    }

    @Test
    void testManipulateResultSet() throws Exception {
        final JdbcConnector datasourceConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getSelectAllQuery()));
        datasourceConnector.connect();
        final Map<String, Object> execute = datasourceConnector.execute();
        ResultSet resultSet = (ResultSet) execute.get("resultset");
        assertNotNull(resultSet);
        resultSet.first();
        assertThat(resultSet.getString(2)).isEqualTo("John");
        resultSet.next();
        assertThat(resultSet.getString(2)).isEqualTo("Jane");
        datasourceConnector.disconnect();
    }

    @Test
    void testWrongTableQuery() throws Throwable {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) "SELECT * FROM Bonita"));
        jdbcConnector.connect();
        assertThrows(ConnectorException.class, () -> jdbcConnector.execute());
    }

    @Test
    void testWrongPersonIdQuery() throws Exception {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getInvalidSelectUserId()));
        final List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
        assertThat(rowSet).isEmpty();
    }

    @Test
    void testCreateSelectAndDropTable() throws Exception {
        JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getSelectAllQuery()));
        List<List<Object>> rowSet = executeAndGetResult(jdbcConnector);
        List<Object> actual = rowSet.get(1);
        final List<Object> expected = Arrays.asList(new Object[] { 2, "Jane", "Doe", 31, 15.9 });

        assertEquals("John", rowSet.get(0).get(1));
        assertEquals("Jane", rowSet.get(1).get(1));
        assertEquals(expected, actual);

        jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getDeleteFirstInsert()));
        jdbcConnector.connect();
        jdbcConnector.execute();
        jdbcConnector.disconnect();

        jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getSelectAllQuery()));
        rowSet = executeAndGetResult(jdbcConnector);
        actual = rowSet.get(0);
        assertEquals(1, rowSet.size());
        assertEquals("Jane", rowSet.get(0).get(1));
        assertEquals(expected, actual);
    }

    @Test
    void testQueryShouldNotGetBackUnNeededColumn() throws Exception {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getFirstnameLastnameQuery()));
        final List<String> headers = executeAndGetColumns(jdbcConnector);
        final List<String> upperCaseHeaders = new ArrayList<String>(headers.size());
        for (final String header : headers) {
            upperCaseHeaders.add(header.toUpperCase());
        }
        assertThat(upperCaseHeaders).contains("FIRSTNAME", "LASTNAME").doesNotContain("ID");
    }

    @Test
    void executeInsertOneLine() throws Exception {
        final List<List<Object>> result = queryAndCheck(
                insertBuilder("(firstname, age, lastname, average)", "('Arthur', 25, 'Doe', 17)"),
                selectBuilder("*", "firstname='Arthur'", "id"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("Arthur", 25);
    }

    @Test
    void executeInsertMultiLine() throws Exception {
        List<List<Object>> result;
        if (db.contains("hsql")) {
            System.out.println("Multi-insertion is not possible with hsql version older than 2.0");
            return;
        } else {
            result = queryAndCheck(
                    insertBuilder("(firstname, age, lastname, average)",
                            "('Elias', 25, 'Doe', 17), ('Fred', 28, 'Da', 18)"),
                    selectBuilder("*", "firstname='Elias' or firstname='Fred'", "firstname"));
        }
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).contains("Elias", 25);
        assertThat(result.get(1)).contains("Fred", 28);

    }

    @Test
    void executeUpdateOneLine() throws Exception {
        final List<List<Object>> result = queryAndCheck(updateBuilder("age=25", "firstname='John'"),
                selectBuilder("*", "firstname='John'", "id"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("John", 25);
    }

    @Test
    void executeUpdateMultiLine() throws Exception {
        simpleQuery(updateBuilder("age=93, firstname='Jany'", "firstname='Jane'"));
        final List<List<Object>> result = queryAndCheck(updateBuilder("age=97, firstname='Johny'", "firstname='John'"),
                selectBuilder("*", "age>90", "id"));
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).contains("Johny", 97);
        assertThat(result.get(1)).contains("Jany", 93);
    }

    @Test
    void executeDeleteOneLine() throws Exception {
        List<List<Object>> result = queryAndCheck(
                insertBuilder("(firstname, age, lastname, average)", "('Arthur', 25, 'Doe', '17.0')"),
                selectBuilder("*", "firstname='Arthur'", "id"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("Arthur", "Doe", 25, 17.0);
        result = queryAndCheck(deleteBuilder("Firstname='Arthur'"), selectBuilder("*", "firstname='Arthur'", "id"));
        assertThat(result).isEmpty();
    }

    @Test
    void executeBatchScript() throws Exception {
        genericBatchScriptTest(";");
    }

    @Test
    void executeBatchScriptWithAnOtherSeparator() throws Exception {
        genericBatchScriptTest("|");
    }

    private void createTable() throws Exception {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getCreateTable()));
        jdbcConnector.connect();
        jdbcConnector.execute();
        jdbcConnector.disconnect();
    }

    private void simpleQuery(final Object query) throws ConnectorException {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, query));
        jdbcConnector.connect();
        jdbcConnector.execute();
        jdbcConnector.disconnect();
    }

    private List<List<Object>> queryAndCheck(final Object query, final Object checkQuery) throws Exception {
        List<List<Object>> result;
        final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, query));
        jdbcConnector.connect();
        jdbcConnector.execute();
        jdbcConnector.disconnect();
        final JdbcConnector checkConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, checkQuery));
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
        JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getFirstInsertQuery()));
        jdbcConnector.connect();
        jdbcConnector.execute();
        jdbcConnector.disconnect();
        jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getSecondInsertQuery()));
        jdbcConnector.connect();
        jdbcConnector.execute();
        jdbcConnector.disconnect();
    }

    private void dropTable() throws Exception {
        final JdbcConnector jdbcConnector = getJdbcConnectorWithParameters(
                Collections.singletonMap(JdbcConnector.SCRIPT, (Object) getDropTableQuery()));
        jdbcConnector.connect();
        jdbcConnector.execute();
        jdbcConnector.disconnect();
    }

    private String getBatchScript(final String separator) {
        String strBatchScript = (String) prop.get("database.batch_script");
        strBatchScript = strBatchScript.replace(";", separator);
        strBatchScript = strBatchScript.replace("auto_increment", (String) prop.get(db + "auto_increment"));
        if (db.contains("oracle")) {
            strBatchScript = strBatchScript.replace("id INTEGER", "id");
            strBatchScript = strBatchScript.replace("after_table_creation",
                    prop.getProperty(db + "after_table_creation"));
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
        strSelect = strSelect.replace("columns", columns).replace("table_name", getTableName())
                .replace("condition", condition).replace("order", order);
        return strSelect;
    }

    private String insertBuilder(final String columns, final String values) {
        String strInsert = (String) prop.get(db + "insert");
        String strIntoValues = "";
        if (db.contains("oracle")) {
            if (values.contains("),")) {
                final String[] tabValues = values.split("),");
                for (final String val : tabValues) {
                    strIntoValues = strIntoValues + "\nINTO " + getTableName() + " " + columns + " \nVALUES " + val
                            + ")";
                }
            }
        } else {
            strInsert = strInsert.replace("table_name", getTableName()).replace("columns", columns).replace("values",
                    values);
        }
        return strInsert;
    }

    private String updateBuilder(final String set_clause, final String condition) {
        String strUpdate = (String) prop.get(db + "update");
        strUpdate = strUpdate.replace("table_name", getTableName()).replace("set_clause", set_clause)
                .replace("condition", condition);
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
        ResultSet data = (ResultSet) execute.get("resultset");
        final ResultSetMetaData metaData = data.getMetaData();
        final int fetchSize = data.getFetchSize();
        final int columnsCount = metaData.getColumnCount();
        List<List<Object>> result = toList(data, fetchSize, columnsCount);
        datasourceConnector.disconnect();

        return result;
    }

    private List<List<Object>> toList(final ResultSet resultSet, final int fetchSize, final int columnsCount)
            throws SQLException {
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

    private List<String> executeAndGetColumns(final JdbcConnector datasourceConnector) throws Exception {
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
