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

public class MLK_Parser {

    static String prefix = "CommonAttr";

    public static void main(String[] args) {
        ArrayList<String> files = get_queries();
        Map<String, ArrayList<String>> scheme = read_scheme();
        // Set<String> columns = get_columns(scheme);
        // System.out.println(columns.toString());
        process(files, scheme);
    }

    public static void process(ArrayList<String> files, Map<String, ArrayList<String>> scheme)
    {
        for(String file: files)
        {
            try {
                // hard code 3 queries largest number of join
                System.out.println(file);
                File f = new File("MLKBenchmark/Query/" + file);
                Scanner console = new Scanner(f);
                String query = console.nextLine();
                query = process_query(query);
                Map<String, ArrayList<String>> aliases = new HashMap<>();
                // System.out.println(query);
                System.out.println(file);

                ArrayList<String> edges = get_connection(query, scheme, aliases);
                Map<String, node> column_node_mapping = build_graph(edges);

                rename(column_node_mapping);

                Map<String, ArrayList<String>> new_schame = construct_scheme_with_new_column_name(scheme, column_node_mapping, aliases, edges);

                file = file.substring(0, file.length() - ".txt".length());
                ArrayList<String> hypergraph = construct_hypergraph(new_schame, file);
    
            } catch(FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
            // break;
        }
    }

    public static ArrayList<String> get_queries()
    {
        ArrayList<String> files = new ArrayList<>();
        File folder = new File("MLKBenchmark/Query");
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
            FileWriter  writer = new FileWriter("MLKBenchmark/Hypergraph/" + filename + ".txt");

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
                        str.append(", ");
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
                                                    Map<String, node> column_node_mapping, Map<String, ArrayList<String>> aliases, ArrayList<String> edges)
    {
        Map<String, ArrayList<String>> new_scheme = new HashMap<>();

        for(String edge: edges)
        {
            String[] data = edge.split(" = ");
            String left = data[0];
            String right = data[1];

            String[] left_data = left.split("\\.");
            String left_alias = left_data[0];
            String left_column = left_data[1];
            String[] right_data = right.split("\\.");
            String right_alias = right_data[0];
            String right_column = right_data[1];

            // System.out.println(left_alias);
            // System.out.println(left_column);
            // System.out.println(right_alias);
            // System.out.println(right_column);
            

            // process left
            for(String table: aliases.keySet())
            {
                boolean flag = false;

                ArrayList<String> list = aliases.get(table);

                if(list.contains(left_alias))
                {
                    flag = true;
                }

                if(flag == true)
                {
                    if(!new_scheme.containsKey(left_alias))
                    {
                        new_scheme.put(left_alias, new ArrayList<>(scheme.get(table)));
                    }
                    ArrayList<String> temp = new_scheme.get(left_alias);
                    
                    // System.out.println(edge);
                    // System.out.println(left_column);
                    // System.out.println(left_alias);
                    // System.out.println(temp.toString());

                    // System.out.println(temp.indexOf(left_column));

                    // new_scheme.get(left_alias).set(temp.indexOf(left_column), column_node_mapping.get(left).new_name);
                    String new_name = column_node_mapping.get(left).new_name;
                    if(new_name != null && !temp.contains(new_name))
                    {
                        new_scheme.get(left_alias).set(temp.indexOf(left_column), new_name);
                    }
                    break;
                }
            }

            // process right
            for(String table: aliases.keySet())
            {
                boolean flag = false;

                ArrayList<String> list = aliases.get(table);

                if(list.contains(right_alias))
                {
                    flag = true;
                }

                if(flag == true)
                {
                    if(!new_scheme.containsKey(right_alias))
                    {
                        new_scheme.put(right_alias, new ArrayList<>(scheme.get(table)));
                    }
                    ArrayList<String> temp = new_scheme.get(right_alias);

                    String new_name = column_node_mapping.get(right).new_name;
                    if(new_name != null && !temp.contains(new_name))
                    {
                        new_scheme.get(right_alias).set(temp.indexOf(right_column), new_name);
                    }
                    break;
                }
            }
        }

        // for(String table: scheme.keySet())
        // {
        //     ArrayList<String> columns = scheme.get(table);
        //     Set<String> list = new HashSet<>();

        //     boolean flag = false;

        //     ArrayList<String> temp = aliases.get(table);

        //     for(String column: columns)
        //     {
                
        //         if(column_node_mapping.containsKey(column) && column_node_mapping.get(column).new_name != null)
        //         {
        //             list.add(column_node_mapping.get(column).new_name);
        //             flag = true;
        //         }
        //         else
        //         {
        //             list.add(column);
        //         }
        //     }
        //     if(flag == true)
        //     {
        //         new_scheme.put(table, new ArrayList<>(list));
        //     }
        // }

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

    public static ArrayList<String> get_connection(String query, Map<String, ArrayList<String>> scheme, Map<String, ArrayList<String>> aliases)
    {
        ArrayList<String> list = new ArrayList<>();
        try
        {
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            CCJSqlParserManager parserManager = new CCJSqlParserManager();
            Statement statement = parserManager.parse(new StringReader(query));
            String joinType = "";
            if(statement instanceof Select)
            {
                Select selectstatement = (Select) statement;
                // System.out.println(selectstatement);
                PlainSelect plainSelect = (PlainSelect) selectstatement.getSelectBody();
                String fromitems = plainSelect.getFromItem().toString();
                String fielditems = plainSelect.getSelectItems().toString();

                // System.out.println("fromItem===>" + fromitems);
                // System.out.println("fieldItems===>" + fielditems);

                List tableList = tablesNamesFinder.getTableList(selectstatement);

                // System.out.println(tableList.toString());

                joinType = plainSelect.getJoins().toString();
                // System.out.println("joinType===>" + joinType);

                update_aliases(aliases, fromitems, tableList.toString(), joinType);
                get_connection_helper(list, joinType, aliases, scheme);
            }
        } catch(JSQLParserException exceptionInstance) {
            exceptionInstance.printStackTrace();
        }
        
        return list;
    }

    public static void get_connection_helper(ArrayList<String> list, String joinType, Map<String, ArrayList<String>> aliases, 
    Map<String, ArrayList<String>> scheme)
    {
        String[] joins = joinType.substring(1, joinType.length() - 1).split(", ");

        for(String join: joins)
        {
            join = join.substring(join.indexOf("ON") + "ON ".length());
            String[] edges = join.split(" AND ");

            for(String edge: edges)
            {
                String[] data = edge.split(" ");
                String left = data[0];
                String op = data[1];
                String right = data[2];

                if(op.compareTo("=") == 0 && left.contains(".") && right.contains("."))
                {
                    list.add(edge);
                }
            }
        }
    }

    public static void update_aliases(Map<String, ArrayList<String>>aliases, String fromitems, String tables, String joins)
    {
        String[] fromItems = fromitems.split(" ");

        if(fromItems.length > 1)
        {
            aliases.put(fromItems[0], new ArrayList<>());
            aliases.get(fromItems[0]).add(fromItems[1]);
        }

        String[] list_tables = tables.substring(1, tables.length() - 1).split(", ");
        
        for(String table: list_tables)
        {
            if(!aliases.containsKey(table))
            {
                aliases.put(table, new ArrayList<>());
            }
        }

        String[] list_joins = joins.substring(1, joins.length() - 1).split(", ");
        
        for(String join: list_joins)
        {
            if(join.contains("LEFT JOIN"))
            {
                join = join.substring("LEFT JOIN".length() + 1, join.indexOf("ON"));
            }
            else if(join.contains("RIGHT JOIN"))
            {
                join = join.substring("RIGHT JOIN".length() + 1, join.indexOf("ON"));
            }
            else if(join.contains("INNER JOIN"))
            {
                join = join.substring("INNER JOIN".length() + 1, join.indexOf("ON"));
            }
            else if(join.contains("JOIN"))
            {
                join = join.substring("JOIN".length() + 1, join.indexOf("ON"));
            }

            String[] temp = null;

            if(join.contains("AS"))
            {
                temp = join.split(" AS ");
            }
            else
            {
                temp = join.split(" ");
            }
            if(temp.length == 1)
            {
                continue;
            }
            else
            {
                if(!aliases.containsKey(temp[0]))
                {
                    aliases.put(temp[0], new ArrayList<>());
                }
                aliases.get(temp[0]).add(temp[1].trim());
            }
        }

        for(String key: aliases.keySet())
        {
            if(aliases.get(key).size() == 0)
            {
                aliases.get(key).add(key);
            }
        }

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
            File file = new File("MLKBenchmark/Query/scheme.txt");
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
            }

        } catch(FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return table;
    }
}
