package jxmvc.models;

import jxmvc.core.DBRow;
import jxmvc.core.DBRowSet;
import jxmvc.core.JxDB;

public class TestModel extends JxDB {
    public TestModel() { super(); }

    public DBRowSet GetInscrips(String filter, Object... params) {
        if (filter == null || filter.isBlank()) return GetTable("tblInscrips");
        return GetTable("tblInscrips", filter, params);
    }

    public DBRowSet DicOficinas(String filter) {
        return GetTable("dicOficinas", filter);
    }

    public DBRow FindBy(String table, String field, String id) {
        return GetRow(table, field + " = ?", id);
    }

    public DBRowSet FindAllById(String table, String field, String id) {
        return GetTable(table, field + " = ?", "ORDER BY id", id);
    }
}
