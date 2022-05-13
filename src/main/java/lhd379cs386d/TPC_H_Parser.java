package lhd379cs386d;

import java.util.*;

import javax.print.DocFlavor.STRING;

import java.io.StringReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileNotFoundException;

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

public class TPC_H_Parser {

    static String prefix = "CommonAttr";

    public static void main(String[] args) {
        ArrayList<String> files = get_queries();
        Map<String, ArrayList<String>> scheme = read_scheme();
        Set<String> columns = get_columns(scheme);
        process(files, scheme, columns);
    }

    public static void process(ArrayList<String> files, Map<String, ArrayList<String>> scheme, Set<String> columns)
    {
        for(String file: files)
        {
            try {
                // hard code 3 queries largest number of join
                if(!(file.compareTo("q5.txt") == 0 || file.compareTo("q8.txt") == 0 || file.compareTo("q9.txt") == 0))
                {
                    continue;
                }
                System.out.println(file);
                File f = new File("TPC-HBenchmark/Query/" + file);
                Scanner console = new Scanner(f);
                String query = console.nextLine();
                query = process_query(query);
                Map<String, ArrayList<String>> aliases = new HashMap<>();

                // hard code to get subquery
                if(file.compareTo("q8.txt") == 0)
                {
                    query = query.substring(query.indexOf("from (") + "from (".length(), query.indexOf(") as all_nations"));
                }

                if(file.compareTo("q9.txt") == 0)
                {
                    query = query.substring(query.indexOf("from (") + "from (".length(), query.indexOf(") as profit"));
                }

                if(file.compareTo("q8.txt") == 0)
                {
                    update_aliases(aliases, query);
                }

                ArrayList<String> edges = get_connection(query, columns);
                // System.out.println(edges.toString());
                Map<String, node> column_node_mapping = build_graph(edges);
                rename(column_node_mapping);

                Map<String, ArrayList<String>> new_schame = construct_scheme_with_new_column_name(scheme, column_node_mapping, aliases);
                add_connection_with_alias(new_schame, aliases, edges, scheme, column_node_mapping);

                file = file.substring(0, file.length() - ".txt".length());
                ArrayList<String> hypergraph = construct_hypergraph(new_schame, file);
    
            } catch(FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
    }

    public static void add_connection_with_alias(Map<String, ArrayList<String>> new_schame, 
                        Map<String, ArrayList<String>> aliases, ArrayList<String> edges, Map<String, ArrayList<String>> scheme,
                        Map<String, node> column_node_mapping)
    {
        ArrayList<String> connection_with_alias = new ArrayList<>();

        for(String edge: edges)
        {
            String[] data = edge.split(" ");
            String left = data[0];
            String op = data[1];
            String right = data[2];

            if(op.compareTo("=") != 0)
            {
                continue;
            }

            for(String table: aliases.keySet())
            {
                boolean flag = false;
                for(String alias: aliases.get(table))
                {
                    if(left.contains(alias + ".") || right.contains(alias + "."))
                    {
                        connection_with_alias.add(edge);
                        flag = true;
                        break;
                    }
                }
                if(flag == true)
                {
                    break;
                }
            }
        }


        for(String table: aliases.keySet())
        {
            for(String alias: aliases.get(table))
            {
                new_schame.put(alias, new ArrayList<>(scheme.get(table)));
            }
        }

        for(String edge: connection_with_alias)
        {
            String[] data = edge.split(" ");
            String left = data[0];
            String right = data[2];

            // left
            if(left.contains("."))
            {
                String[] temp = left.split("\\.");
                String t = temp[0];
                String c = temp[1];
                new_schame.get(t).set(new_schame.get(t).indexOf(c), column_node_mapping.get(left).new_name);
            }

            if(right.contains("."))
            {
                String[] temp = right.split("\\.");
                String t = temp[0];
                String c = temp[1];
                new_schame.get(t).set(new_schame.get(t).indexOf(c), column_node_mapping.get(right).new_name);
            }
        }
    }

    public static void update_aliases(Map<String, ArrayList<String>> aliases, String query)
    {
        query = query.substring(query.indexOf("from", query.indexOf("from") + 1) + "from".length(), query.indexOf("where")).trim();
        String[] tables = query.split(", ");
        for(String table: tables)
        {
            String[] data = table.split(" ");
            if(!aliases.containsKey(data[0]))
            {
                aliases.put(data[0], new ArrayList<>());
            }
            if(data.length == 2)
            {
                aliases.get(data[0]).add(data[1]);
            }
            else
            {
                aliases.get(data[0]).add(data[0]);
            }
        }

        // for(String s: aliases.keySet())
        // {
        //     System.out.println(s + ": " + aliases.get(s).toString());
        // }
    }

    public static ArrayList<String> get_queries()
    {
        ArrayList<String> files = new ArrayList<>();
        File folder = new File("TPC-HBenchmark/Query");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
		  File file = listOfFiles[i];
		  if (file.isFile() && file.getName().endsWith(".txt")) {
              if(file.getName().compareTo("scheme.txt") != 0)
              {
                files.add(file.getName());
              }
		  } 
		}

        return files;
    }

    public static ArrayList<String> construct_hypergraph(Map<String, ArrayList<String>> table_columns, String filename)
    {
        Set<String> set = new HashSet<>();
        for(String key: table_columns.keySet())
        {
            ArrayList<String> list = table_columns.get(key);
            for(String column: list)
            {
                set.add(column);
            }
        }

        Map<String, String> table_vertex_mapping = new HashMap<>();
        ArrayList<String> hypergraph = new ArrayList<>();
        int v_count = 0;
        int e_count = 0;
        for(String column: set)
        {
            table_vertex_mapping.put(column, "V" + (v_count + 1));
            v_count += 1;
        }

        int count = 0;

        try
        {
            FileWriter  writer = new FileWriter("TPC-HBenchmark/Hypergraph/" + filename + ".txt");

            for(String key: table_columns.keySet())
            {
                ArrayList<String> list = table_columns.get(key);
                StringBuilder sb = new StringBuilder();
                StringBuilder str = new StringBuilder();
                String hyperedge = "E" + (e_count + 1);
                sb.append(hyperedge);
                str.append(hyperedge + " (");

                for(int i = 0; i < list.size(); i++)
                {
                    if(i != 0)
                    {
                        str.append(" ");
                    }
                    sb.append(" ");
                    sb.append(table_vertex_mapping.get(list.get(i)));
                    str.append(table_vertex_mapping.get(list.get(i)));
                }
                if(count == table_columns.size() - 1)
                {
                    str.append(").");
                }
                else
                {
                    str.append("),");
                }
                count += 1;
                e_count += 1;
                writer.write(str.toString() + "\n");
                hypergraph.add(sb.toString().trim());
            }

            System.out.println("Successfully wrote to the file.");
            writer.close();
        }
        catch(IOException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return hypergraph;
    }

    public static Map<String, ArrayList<String>> construct_scheme_with_new_column_name(Map<String, ArrayList<String>> scheme, 
                                                                    Map<String, node> column_node_mapping, Map<String, ArrayList<String>> aliases)
    {
        Map<String, ArrayList<String>> new_scheme = new HashMap<>();

        for(String table: scheme.keySet())
        {
            ArrayList<String> columns = scheme.get(table);
            Set<String> list = new HashSet<>();

            boolean flag = false;

            for(String column: columns)
            {
                if(column_node_mapping.containsKey(column) && column_node_mapping.get(column).new_name != null)
                {
                    list.add(column_node_mapping.get(column).new_name);
                    flag = true;
                }
                else
                {
                    list.add(column);
                }
            }
            if(flag == true)
            {
                new_scheme.put(table, new ArrayList<>(list));
            }
        }

        // for(String table: new_scheme.keySet())
        // {
        //     System.out.println(table + " " + new_scheme.get(table).toString());
        // }

        return new_scheme;
    }

    public static void rename(Map<String, node> map)
    {
        Map<String, Boolean> visited = new HashMap<>();
        
        // initialize visited map
        for(String key: map.keySet())
        {
            visited.put(key, false);
        }

        // rename process
        int val = 0;
        for(String key: map.keySet())
        {
            if(!visited.get(key))
            {
                node cur = map.get(key);
                DFS(cur, visited, map, val);
                val += 1;
            }
        }

        // for(String key: map.keySet())
        // {
        //     node cur = map.get(key);
        //     System.out.println(cur.name + " = " + cur.new_name);
        // }
    }

    public static void DFS(node cur, Map<String, Boolean> visited, Map<String, node> map, int val)
    {
        cur.new_name = prefix + val;
        String name = cur.name;
        visited.put(name, true);
        ArrayList<String> adj = cur.get_neighbors();

        for(String neighbor: adj)
        {
            if(!visited.get(neighbor))
            {
                node next = map.get(neighbor);
                DFS(next, visited, map, val);
            }
        }
    }

    public static void add_node(Map<String, node> map, String u, String v)
    {
        if(!map.containsKey(u))
        {
            node new_node = new node(u);
            map.put(u, new_node);
        }
        if(!map.containsKey(v))
        {
            node new_node = new node(v);
            map.put(v, new_node);
        }
        map.get(u).add_neighbor(v);
        map.get(v).add_neighbor(u);
    }

    public static Map<String, node> build_graph(ArrayList<String> list)
    {
        Map<String, node> column_node_mapping = new HashMap<>();
        for(String connection: list)
        {
            String[] edge = connection.split(" = ");
            String u = edge[0];
            String v = edge[1];
            add_node(column_node_mapping, u, v);
        }
        return column_node_mapping;
    }

    public static Set<String> get_columns(Map<String, ArrayList<String>> scheme)
    {
        Set<String> columns = new HashSet<>();

        for(String table: scheme.keySet())
        {
            for(String column: scheme.get(table))
            {
                columns.add(column);
            }
        }

        return columns;
    }

    public static ArrayList<String> get_connection(String query, Set<String> columns)
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

                        // System.out.println(exp);

                        if(((columns.contains(left) && columns.contains(right)) || (left.contains(".") && right.contains(".")) || 
                        (columns.contains(left) && right.contains(".")) || (columns.contains(right) && left.contains("."))) && op.compareTo("=") == 0)
                        {
                            list.add(exp);
                        }
                    }
                    super.visitBinaryExpression(expr); 
                }
            });
        } catch(JSQLParserException exceptionInstance) {
            exceptionInstance.printStackTrace();
        }

        // System.out.println("Debug");
        // for(String connection: list)
        // {
        //     System.out.println(connection);
        // }
        return list;
    }

    public static String process_query(String query)
    {
        query = query.toLowerCase();
        query = query.replaceAll("\\s{2,}", " ").trim();
        return query;
    }

    public static Map<String, ArrayList<String>> read_scheme()
    {
        Map<String, ArrayList<String>> table = new HashMap<>();

        try {
            File file = new File("TPC-HBenchmark/Query/scheme.txt");
            Scanner console = new Scanner(file);

            while(console.hasNextLine())
            {
                String[] data = console.nextLine().toLowerCase().split(": ");
                ArrayList<String> columns = new ArrayList<>();
                String[] column_list = data[1].substring(1, data[1].length() - 1).split(", ");
                for(int i = 0; i < column_list.length; i++)
                {
                    String column = column_list[i].trim();
                    columns.add(column);
                }
                table.put(data[0].trim(), columns);
                // System.out.println(data[0].trim() + ": " + columns.toString());
            }

        } catch(FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return table;
    }
}
