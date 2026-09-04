package com.travelsky.base.repository.helper;


import com.travelsky.base.common.exception.BusinessException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

public class SqlParseHelper {

    public static SqlContext parse(String sql) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(sql);
        SqlContext sqlContext;
        if (statement instanceof Select) {
            sqlContext = new SqlContext(Type.SELECT, statement, sql);
        } else if (statement instanceof Insert) {
            sqlContext = new SqlContext(Type.INSERT, statement, sql);
        } else if (statement instanceof Update) {
            sqlContext = new SqlContext(Type.UPDATE, statement, sql);
        } else if (statement instanceof Delete) {
            sqlContext = new SqlContext(Type.DELETE, statement, sql);
        } else {
            throw new BusinessException("not support");
        }
        return sqlContext;
    }

    public enum Type {
        INSERT, UPDATE, DELETE, SELECT,
        ;
    }

    public static class SqlContext {
        private Table table;
        private Expression where;
        private String sql;
        private Type type;
        private Insert insert;
        private Update update;
        private Delete delete;
        private Select select;
        public SqlContext(Type type, Statement statement, String sql) {
            this.sql = sql;
            this.type = type;
            switch (type) {
                case INSERT:
                    this.insert = (Insert) statement;
                    this.table = this.insert.getTable();
                    break;
                case UPDATE:
                    this.update = (Update) statement;
                    this.table = this.update.getTable();
                    this.where = this.update.getWhere();
                    break;
                case DELETE:
                    this.delete = (Delete) statement;
                    this.table = this.delete.getTable();
                    this.where = this.delete.getWhere();
                    break;
                default:
                    this.select = (Select) statement;
                    this.table = this.select.getPlainSelect().getFromItem(Table.class);
                    this.where = this.select.getPlainSelect().getWhere();
                    break;
            }

        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public Statement getStatement() {
            return insert != null ? insert : (update != null ? update : (delete != null ? delete : select));
        }

        public String getSql() {
            return this.sql;
        }

        public Table getTable() {
            return table;
        }

        public void setTable(Table table) {
            this.table = table;
        }

        public Expression getWhere() {
            return where;
        }

        public void setWhere(Expression where) {
            this.where = where;
        }
    }

}
