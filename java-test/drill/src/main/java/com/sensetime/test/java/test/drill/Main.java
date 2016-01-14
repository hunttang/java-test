package com.sensetime.test.java.test.drill;

import java.sql.*;

/**
 * Created by Hunt on 12/22/15.
 */
public class Main {

    private static String DB_URL = "jdbc:drill:zk=10.0.8.127:2181/drill/drillbits;schema=hbase";

    private static String QUERY = "select convert_from(row_key, 'INT_BE'), convert_from(COLL.attr.name, 'UTF8') from COLL";

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.drill.jdbc.Driver");
        Connection connection = DriverManager.getConnection(DB_URL);
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(QUERY);
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCnt = rsmd.getColumnCount();
        for (int i = 1; i <= columnCnt; ++i) {
            String out = rsmd.getCatalogName(i) + "\t"
                    + rsmd.getColumnClassName(i) + "\t"
                    + rsmd.getColumnLabel(i) + " \t"
                    + rsmd.getColumnName(i) + "\t"
                    + rsmd.getColumnTypeName(i) + "\t"
                    + rsmd.getColumnType(i);
            System.out.println(out);
        }

        System.out.println();

        while (rs.next()) {
            String out = "";
            for (int i = 1; i <= columnCnt; ++i) {
                out += rs.getObject(i) + "\t";
            }
            System.out.println(out);
        }
    }
}
