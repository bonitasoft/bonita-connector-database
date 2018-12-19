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
package org.bonitasoft.connectors.database.datasource;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import oracle.jdbc.pool.OracleDataSource;

/**
 * @author Baptiste Mesta
 * @author Frédéric Bouquet
 */
public class DatasourceConnectorTest {

    public static final String DATASOURCE = "java:/comp/env/jdbc/bonita";

    private InitialContext ic;

    private String tableName;

    private Properties prop;

    private String db;

    @Before
    public void setUp() throws Exception {
        final Properties p = new Properties();
        p.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
        p.put(Context.URL_PKG_PREFIXES, "org.apache.naming");
        ic = new InitialContext(p);
        ic.createSubcontext("java:");
        ic.createSubcontext("java:/comp");
        ic.createSubcontext("java:/comp/env");
        ic.createSubcontext("java:/comp/env/jdbc");

        final DataSource ds = getProperties();
        ic.bind("java:/comp/env/jdbc/bonita", ds);

        createTable();
        insertValues();
    }

    @After
    public void tearDown() throws Exception {
        dropTable();
        ic.unbind("java:/comp/env/jdbc/bonita");
        ic.destroySubcontext("java:");
        ic.close();
    }

    private DataSource getProperties() throws Exception {
        prop = new Properties();
        prop.load(this.getClass().getResourceAsStream("/connectors_config.properties"));
        DataSource ds;
        db = prop.getProperty("database.type_db");
        final TypeDatabaseEnum type = TypeDatabaseEnum.valueOf(db.toUpperCase());
        db = db + ".datasource" + ".";
        switch (type) {
            case MYSQL:
                ds = new MysqlDataSource();
                ((MysqlDataSource) ds).setServerName(prop.getProperty(db + "SERVERNAME"));
                ((MysqlDataSource) ds).setPort(Integer.parseInt(prop.getProperty(db + "PORT")));
                ((MysqlDataSource) ds).setDatabaseName(prop.getProperty(db + "DATABASENAME"));
                ((MysqlDataSource) ds).setUser(prop.getProperty(db + "USERNAME"));
                ((MysqlDataSource) ds).setPassword(prop.getProperty(db + "PASSWORD"));
                break;
            case HSQL:
                ds = new org.hsqldb.jdbc.jdbcDataSource();
                ((org.hsqldb.jdbc.jdbcDataSource) ds).setDatabase(prop.getProperty(db + "JDBC_URL"));
                ((org.hsqldb.jdbc.jdbcDataSource) ds).setUser(prop.getProperty(db + "USERNAME"));
                ((org.hsqldb.jdbc.jdbcDataSource) ds).setPassword(prop.getProperty(db + "PASSWORD"));
                break;
            case H2:
                ds = new org.h2.jdbcx.JdbcDataSource();
                ((org.h2.jdbcx.JdbcDataSource) ds).setURL(prop.getProperty(db + "JDBC_URL"));
                ((org.h2.jdbcx.JdbcDataSource) ds).setUser(prop.getProperty(db + "USERNAME"));
                ((org.h2.jdbcx.JdbcDataSource) ds).setPassword(prop.getProperty(db + "PASSWORD"));
                break;
            case ORACLE:
                ds = new OracleDataSource();
                ((OracleDataSource) ds).setServerName(prop.getProperty(db + "SERVERNAME"));
                ((OracleDataSource) ds).setPortNumber(Integer.parseInt(prop.getProperty(db + "PORT")));
                ((OracleDataSource) ds).setDatabaseName(prop.getProperty(db + "DATABASENAME"));
                ((OracleDataSource) ds).setUser(prop.getProperty(db + "USERNAME"));
                ((OracleDataSource) ds).setPassword(prop.getProperty(db + "PASSWORD"));

                break;
            default:
                throw new Exception("This type of database is not supported");
        }
        return ds;
    }

    @Test
    public void testGetAllValues() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
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
    public void testValidateInputWithNullParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        DatasourceConnector datasourceConnector = new DatasourceConnector();
        datasourceConnector.setInputParameters(parameters);
        try {
            datasourceConnector.validateInputParameters();
            fail("Connector validation should be failing");
        } catch (ConnectorValidationException e) {
            assertThat(e.getMessage(), containsString("Datasource"));
            assertThat(e.getMessage(), not(containsString("Properties")));
            assertThat(e.getMessage(), containsString("Script"));
        }
    }

    @Test
    public void testValidateInputWithGoodParameters() throws ConnectorValidationException {
        DatasourceConnector datasourceConnector =
                getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT, (Object) getFirstInsertQuery()));
        datasourceConnector.validateInputParameters();
    }

    @Test
    public void testValidateInputWithEmptyParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(DatasourceConnector.DATASOURCE_INPUT, "");
        parameters.put(DatasourceConnector.PROPERTIES_INPUT, new ArrayList<Object>());
        parameters.put(DatasourceConnector.SCRIPT_INPUT, "");
        DatasourceConnector datasourceConnector = new DatasourceConnector();
        datasourceConnector.setInputParameters(parameters);
        try {
            datasourceConnector.validateInputParameters();
            fail("Connector validation should be failing");
        } catch (ConnectorValidationException e) {
            assertThat(e.getMessage(), containsString("Datasource"));
            assertThat(e.getMessage(), not(containsString("Properties")));
            assertThat(e.getMessage(), containsString("Script"));
        }
    }

    @Test
    public void testGetFirstNames() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        final Object johnName = rowSet.get(0).get(1);
        assertEquals("John", johnName);
        final Object janeName = rowSet.get(1).get(1);
        assertEquals("Jane", janeName);
    }

    @Test
    public void testInsertOneLine() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getThirdInsertQuery()));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
        final DatasourceConnector datasourceConnectorRes = getDatasourceConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnectorRes);

        final Object jennyFirstName = rowSet.get(2).get(1);
        assertEquals("Jenny", jennyFirstName);
        final Object jennyName = rowSet.get(2).get(2);
        assertEquals("Smith", jennyName);
        final Object jennyAge = rowSet.get(2).get(3);
        assertEquals(18, jennyAge);
        final Object jennyAverage = rowSet.get(2).get(4);
        assertEquals(15.9, jennyAverage);
    }

    @Test
    public void testManipulateResultSet() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getSelectAllQuery()));
        datasourceConnector.connect();
        final Map<String, Object> execute = datasourceConnector.execute();
        ResultSet resultSet = (ResultSet) execute.get("resultset");
        assertNotNull(resultSet);
        assertThat(resultSet.getFetchSize(), is(1));
        datasourceConnector.disconnect();
    }

    @Test
    public void testGetOneLine() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getSelectLine("John")));
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        assertEquals(1, rowSet.size());

        final Object jennyFirstName = rowSet.get(0).get(1);
        assertEquals("John", jennyFirstName);
        final Object jennyName = rowSet.get(0).get(2);
        assertEquals("Doe", jennyName);
        final Object jennyAge = rowSet.get(0).get(3);
        assertEquals(27, jennyAge);
        final Object jennyAverage = rowSet.get(0).get(4);
        assertEquals(15.4, jennyAverage);
    }

    @Test
    public void testGetOneField() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getFirstNames()));
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        assertEquals(2, rowSet.size());
        assertEquals(1, rowSet.get(0).size());
        assertEquals(1, rowSet.get(1).size());

        assertEquals(rowSet.get(0).get(0), "John");
        assertEquals(rowSet.get(1).get(0), "Jane");
    }

    @Test
    public void testGetOneFieldofOneLine() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getAgeOf("John")));
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        assertEquals(1, rowSet.size());
        assertEquals(1, rowSet.get(0).size());

        assertThat((Integer) rowSet.get(0).get(0), is(27));
    }

    @Test
    public void testUpdate() throws Exception {
        DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getUpdateAgeOf("John", "44")));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();

        datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT, (Object) getAgeOf("John")));
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        assertEquals(1, rowSet.size());
        assertEquals(1, rowSet.get(0).size());

        assertThat((Integer) rowSet.get(0).get(0), is(44));
    }

    @Test
    public void testDeleteOneLine() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getDeleteFirstInsert()));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
        final DatasourceConnector datasourceConnectorRes = getDatasourceConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnectorRes);

        assertEquals(1, rowSet.size());
        assertEquals("Jane", rowSet.get(0).get(1));

    }

    @Test
    public void testDeleteMultiLine() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getMultipleDeleteQuery()));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
        final DatasourceConnector datasourceConnectorRes = getDatasourceConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnectorRes);

        assertEquals(0, rowSet.size());
    }

    @Test
    public void testGetInBoundIndexes() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithNoParameter();
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        assertTrue(rowSet.get(0).size() >= 4);
    }

    @Test
    public void testInvalidColumnName() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithNoParameter();
        final List<String> columns = executeAndGetColumns(datasourceConnector);
        assertFalse("state is not a valid column name", columns.contains("state"));
    }

    @Test(expected = ConnectorException.class)
    public void testWrongTableQuery() throws Throwable {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) "SELECT * FROM Bonita"));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
    }

    @Test
    public void testWrongPersonIdQuery() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getInvalidSelectUserId()));
        final List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        assertTrue(rowSet.isEmpty());
    }

    @Test
    public void testCreateSelectAndDropTable() throws Exception {
        DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getSelectAllQuery()));
        List<List<Object>> rowSet = executeAndGetResult(datasourceConnector);
        List<Object> actual = rowSet.get(1);
        final List<Object> expected = Arrays.asList(new Object[] { 2, "Jane", "Doe", 31, 15.9 });

        assertEquals("John", rowSet.get(0).get(1));
        assertEquals("Jane", rowSet.get(1).get(1));
        assertEquals(expected, actual);

        datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT, (Object) getDeleteFirstInsert()));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();

        datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT, (Object) getSelectAllQuery()));
        rowSet = executeAndGetResult(datasourceConnector);
        actual = rowSet.get(0);
        assertEquals(1, rowSet.size());
        assertEquals("Jane", rowSet.get(0).get(1));
        assertEquals(expected, actual);
    }

    @Test
    public void executeBatchScript() throws Exception {

        final String script = getBatchScript(";");

        final Map<String, Object> parameters = new HashMap<String, Object>(6);
        parameters.put(DatasourceConnector.SCRIPT_INPUT, script);
        parameters.put(DatasourceConnector.SEPARATOR_INPUT, ";");
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(parameters);
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();

    }

    private void createTable() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getCreateTable()));
        datasourceConnector.validateInputParameters();
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
    }

    private DatasourceConnector getDatasourceConnectorWithNoParameter() {
        final Map<String, Object> emptyMap = Collections.emptyMap();
        return getDatasourceConnectorWithParameters(emptyMap);
    }

    private DatasourceConnector getDatasourceConnectorWithParameters(final Map<String, Object> parameters) {
        final DatasourceConnector datasourceConnector = new DatasourceConnector();
        final Map<String, Object> defaultParameters = initiateConnectionParameters();
        List<List<Object>> properties = new ArrayList<List<Object>>();

        List<Object> line = new ArrayList<Object>();
        line.add(Context.INITIAL_CONTEXT_FACTORY);
        line.add("org.apache.naming.java.javaURLContextFactory");
        properties.add(line);
        line = new ArrayList<Object>();
        line.add(Context.URL_PKG_PREFIXES);
        line.add("org.apache.naming");
        properties.add(line);

        defaultParameters.put(DatasourceConnector.PROPERTIES_INPUT, properties);
        defaultParameters.put(DatasourceConnector.SCRIPT_INPUT, getSelectAllQuery());
        defaultParameters.putAll(parameters);
        datasourceConnector.setInputParameters(defaultParameters);
        return datasourceConnector;
    }

    private Map<String, Object> initiateConnectionParameters() {
        final Map<String, Object> defaultParameters = new HashMap<String, Object>();
        defaultParameters.put(DatasourceConnector.DATASOURCE_INPUT, DATASOURCE);
        return defaultParameters;
    }

    private void insertValues() throws Exception {
        DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getFirstInsertQuery()));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
        datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT, (Object) getSecondInsertQuery()));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
    }

    private void dropTable() throws Exception {
        final DatasourceConnector datasourceConnector = getDatasourceConnectorWithParameters(Collections.singletonMap(DatasourceConnector.SCRIPT_INPUT,
                (Object) getDropTableQuery()));
        datasourceConnector.connect();
        datasourceConnector.execute();
        datasourceConnector.disconnect();
    }

    private String getBatchScript(final String separator) {
        String strBatchScript = prop.getProperty("datasource.batch_script");
        strBatchScript = strBatchScript.replace(";", separator);
        strBatchScript = strBatchScript.replace("auto_increment", prop.getProperty(db + "auto_increment"));
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

    private String getThirdInsertQuery() {
        return insertBuilder("(firstname, age, lastname, average)", "('Jenny', 18, 'Smith', 15.9)");
    }

    protected String getSelectAllQuery() {
        return selectBuilder("*");
    }

    private String getSelectLine(final String firstname) {
        return selectBuilder("*", "firstname = '" + firstname + "'");
    }

    private String getFirstNames() {
        return selectBuilder("firstname");
    }

    private String getAgeOf(final String firstname) {
        return selectBuilder("age", "firstname = '" + firstname + "'");
    }

    protected String getDeleteFirstInsert() {
        return deleteBuilder("firstname = 'John'");
    }

    protected String getMultipleDeleteQuery() {
        return deleteBuilder("firstname = 'John' OR firstname = 'Jane'");
    }

    protected String getDropTableQuery() {
        return dropBuilder();
    }

    protected String getInvalidSelectUserId() {
        return selectBuilder("*", "id = 3");
    }

    private String getUpdateAgeOf(final String firstname, final String newAge) {
        return updateBuilder("age = " + newAge, "firstname = '" + firstname + "'");
    }

    private List<String> executeAndGetColumns(final DatasourceConnector datasourceConnector) throws Exception{
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

    private List<List<Object>> executeAndGetResult(final DatasourceConnector datasourceConnector) throws Exception {
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

    private String createBuilder() {
        String strCreate = prop.getProperty(db + "create_table");
        strCreate = strCreate.replace("table_name", getTableName());
        strCreate = strCreate.replace("auto_increment", prop.getProperty(db + "auto_increment"));

        if (db.contains("oracle")) {
            strCreate = strCreate + prop.get(db + "after_table_creation");
        }

        System.out.println(strCreate);

        return strCreate;

    }

    private String dropBuilder() {
        final String strDrop = prop.getProperty(db + "drop_table");
        return strDrop.replace("table_name", getTableName());
    }

    private String selectBuilder(final String columns) {
        String strSelect = prop.getProperty(db + "selectWithoutCondition");
        strSelect = strSelect.replace("columns", columns).replace("table_name", getTableName());
        return strSelect;
    }

    private String selectBuilder(final String columns, final String condition) {
        String strSelect = prop.getProperty(db + "select");
        strSelect = strSelect.replace("columns", columns).replace("table_name", getTableName()).replace("condition", condition);
        return strSelect;
    }

    private String insertBuilder(final String columns, final String values) {
        String strInsert = prop.getProperty(db + "insert");
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
        String strUpdate = prop.getProperty(db + "update");
        strUpdate = strUpdate.replace("table_name", getTableName()).replace("set_clause", set_clause).replace("condition", condition);
        return strUpdate;
    }

    private String deleteBuilder(final String condition) {
        String strDelete = prop.getProperty(db + "delete");
        strDelete = strDelete.replace("table_name", getTableName()).replace("condition", condition);
        return strDelete;
    }

}
