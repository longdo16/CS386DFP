package lhd379cs386d;

import java.util.*;
import java.io.StringReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

    public static void main(String[] args) {
        test();
    }

    public static void write_connection(Map<String, ArrayList<String>> map, ArrayList<String> connections)
    {
        try
        {
            FileWriter  writer = new FileWriter("connections.txt");
            for(String connection: connections)
            {
                writer.write(connection + "\n");   
            }
            System.out.println("Successfully wrote to the file.");
            writer.close();
        }
        catch(IOException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void write_table_columns(Map<String, ArrayList<String>> map)
    {
        try
        {
            FileWriter  writer = new FileWriter("table_columns.txt");
            for(String key: map.keySet())
            {
                ArrayList<String> list = map.get(key);
                Set<String> set = new HashSet<>(list);
                writer.write(key + ": " + set.toString() + "\n");
            }
            System.out.println("Successfully wrote to the file.");
            writer.close();
        }
        catch(IOException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static ArrayList<String> get_connection(String query, ArrayList<String> columns)
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
                        String left = expr.getLeftExpression().toString();
                        String right = expr.getRightExpression().toString();
                        String op = expr.getStringExpression().toString();

                        if(columns.contains(left) && columns.contains(right))
                        {
                            list.add(exp);
                        }
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
        String query = "SELECT MIN(cn.name) AS producing_company, MIN(miidx.info) AS rating, MIN(t.title) AS movie FROM company_name AS cn, company_type AS ct, info_type AS it, info_type AS it2, kind_type AS kt, movie_companies AS mc, movie_info AS mi, movie_info_idx AS miidx, title AS t WHERE cn.country_code ='[us]' AND ct.kind ='production companies' AND it.info ='rating' AND it2.info ='release dates' AND kt.kind ='movie' AND mi.movie_id = t.id AND it2.id = mi.info_type_id AND kt.id = t.kind_id AND mc.movie_id = t.id AND cn.id = mc.company_id AND ct.id = mc.company_type_id AND miidx.movie_id = t.id AND it.id = miidx.info_type_id AND mi.movie_id = miidx.movie_id AND mi.movie_id = mc.movie_id AND miidx.movie_id = mc.movie_id;";
        query = process_query(query);
        ArrayList<String> aliases = get_aliases(query);
        ArrayList<String> columns = get_columns(query);
        System.out.println(columns.toString());
        Map<String, ArrayList<String>> map = get_table_column_mapping(aliases, columns);
        for(String key: map.keySet())
        {
            System.out.println(key + ": " + map.get(key).toString());
        }
        ArrayList<String> connections = get_connection(query, columns);
        write_table_columns(map);
        write_connection(map, connections);
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
