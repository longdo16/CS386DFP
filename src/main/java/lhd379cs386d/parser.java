package lhd379cs386d;

import java.util.*;
import java.io.StringReader;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.parser.CCJSqlParserDefaultVisitor;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserTreeConstants;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.statement.Statement;

public class parser {

    static String prefix = "CommonAttr";

    public static void main(String[] args) {
        test();
    }

    public static Map<String, ArrayList<String>> rename(ArrayList<String> list, ArrayList<String> columns)
    {
        Map<String, ArrayList<String>> map = new HashMap<>();

        for(int i = 0; i < list.size(); i++)
        {
            String[] cur = list.get(i).split(" ");
            String left = cur[0];
            String right = cur[2];

            if(columns.contains(left) && columns.contains(right))
            {
                int size = map.size() + 1;
                String new_name = prefix + size;
            }
        }

        return map;
    }

    public static void construct_hypergraph(Map<String, ArrayList<String>> map)
    {
        int size = map.size();
        for(int i = 0; i < size; i++)
        {

        }
    }

    public static ArrayList<String> get_connection(String query)
    {
        ArrayList<String> list = new ArrayList<>();
        try
        {
            Statement statement = CCJSqlParserUtil.parse(query);
            String wherebody = ((PlainSelect) ((Select) statement).getSelectBody()).getWhere().toString();
            Expression expr = CCJSqlParserUtil.parseCondExpression(wherebody);
            expr.accept(new ExpressionVisitorAdapter() {
    
                @Override
                protected void visitBinaryExpression(BinaryExpression expr) {
                    if (expr instanceof ComparisonOperator) {
                        String exp = expr.getLeftExpression() + " " + expr.getStringExpression() + " " + expr.getRightExpression();
                        // System.out.println("left=" + expr.getLeftExpression() + "  op=" +  expr.getStringExpression() + "  right=" + expr.getRightExpression());
                        list.add(exp);
                    }
    
                    super.visitBinaryExpression(expr); 
                }
            });
        }
        catch(JSQLParserException exceptionInstance)
        {
            exceptionInstance.printStackTrace();
        }
        return list;
    }

    public static void test()
    {
        String query = "SELECT MIN(t.title) AS typical_european_movie FROM company_type AS ct, info_type AS it, movie_companies AS mc, movie_info AS mi, title AS t WHERE ct.kind  = 'production companies' AND mc.note  like '%(theatrical)%' and mc.note like '%(France)%' AND mi.info  IN ('Sweden', 'Norway', 'Germany', 'Denmark', 'Swedish', 'Denish', 'Norwegian', 'German') AND t.production_year > 2005 AND t.id = mi.movie_id AND t.id = mc.movie_id AND mc.movie_id = mi.movie_id AND ct.id = mc.company_type_id AND it.id = mi.info_type_id;";
        query = process_query(query);
        ArrayList<String> aliases = get_aliases(query);
        ArrayList<String> columns = get_columns(query);
        System.out.println(columns.toString());
        Map<String, ArrayList<String>> map = get_table_column_mapping(aliases, columns);
        for(String key: map.keySet())
        {
            System.out.println(key + ": " + map.get(key).toString());
        }
    }

    public static ArrayList<String> get_columns(String query)
    {
        ArrayList<String> columns = new ArrayList<>();
        try
        {
            Statement stmt = CCJSqlParserUtil.parse(query);
            Select selectStatement = (Select) stmt;
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder() {
                @Override
                public void visit(Column tableColumn) {
                    columns.add(tableColumn.toString());
                }
            };
            tablesNamesFinder.getTableList(selectStatement);
        }
        catch(JSQLParserException exceptionInstance)
        {
            exceptionInstance.printStackTrace();
        }
        return columns;
    }

    public static Map<String, ArrayList<String>> get_table_column_mapping(ArrayList<String> aliases, ArrayList<String> columns)
    {
        Map<String, ArrayList<String>> map = new HashMap<>();
        for(String alias: aliases)
        {
            String cur = alias + ".";
            for(int i = 0; i < columns.size(); i++)
            {
                if(columns.get(i).contains(cur))
                {
                    if(!map.containsKey(cur))
                    {
                        map.put(cur, new ArrayList<>());
                    }
                    map.get(cur).add(columns.get(i));
                }
            }
        }

        return map;
    }

    public static String process_query(String query)
    {
        query = query.toLowerCase();
        query = query.replaceAll("\\s{2,}", " ").trim();
        return query;
    }

    public static ArrayList<String> get_aliases(String query)
    {
        // convert to lower case
        query = process_query(query);
        
        ArrayList<String> list = new ArrayList<>();
        Set<String> set = new HashSet<>();
        
        int start = query.indexOf("from") + "from".length();
        int end = query.contains("where")? query.indexOf("where"): query.length();

        String sub = query.substring(start, end);
        String[] token = sub.split(",");

        for(int i = 0; i < token.length; i++)
        {
            String cur = token[i].trim();
            if(cur.contains("as"))
            {
                String[] split = cur.split(" ");
                set.add(split[2].trim());
            }
            else
            {
                set.add(cur);
            }
        }

        for(String str: set)
        {
            list.add(str);
        }
        
        return list;
    }
}