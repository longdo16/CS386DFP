package lhd379cs386d;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.io.FileNotFoundException;

import javax.swing.plaf.synth.SynthTextAreaUI;

public class rename {

    static String prefix = "CommonAttr";

    public static void main(String[] args) {
        Map<String, node> map = new HashMap<>();
        ArrayList<String> list = read();
        build_graph(list, map);
        rename(map);
        Map<String, ArrayList<String>> table_columns = construct_table_with_new_name(map);
        // for(String key: table_columns.keySet())
        // {
        //     System.out.println(key + ": " + table_columns.get(key).toString());
        // }
        construct_hypergraph(table_columns);
    }

    public static void construct_hypergraph(Map<String, ArrayList<String>> table_columns)
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
        int v_count = 0;
        int e_count = 0;
        for(String column: set)
        {
            table_vertex_mapping.put(column, "V" + (v_count + 1));
            v_count += 1;
        }

        int count = 0;

        for(String key: table_columns.keySet())
        {
            ArrayList<String> list = table_columns.get(key);
            System.out.print("E" + (e_count + 1) + " (");

            for(int i = 0; i < list.size(); i++)
            {
                if(i != 0)
                {
                    System.out.print(" ");
                }
                System.out.print(table_vertex_mapping.get(list.get(i)));
            }
            if(count == table_columns.size() - 1)
            {
                System.out.println(").");
            }
            else
            {
                System.out.println("),");
            }
            count += 1;
            e_count += 1;
        }
    }

    public static Map<String, ArrayList<String>> construct_table_with_new_name(Map<String, node> map)
    {
        Map<String, ArrayList<String>> table_columns = new HashMap<>();
        try
        {
            File file = new File("table_columns.txt");

            Scanner console = new Scanner(file);

            while(console.hasNextLine())
            {
                String[] data = console.nextLine().split(": ");
                ArrayList<String> list = new ArrayList<>();
                String[] columns = data[1].substring(1, data[1].length() - 1).split(", ");
                for(int i = 0; i < columns.length; i++)
                {
                    String cur = columns[i].trim();
                    if(map.containsKey(cur) && map.get(cur).new_name != null)
                    {
                        list.add(map.get(cur).new_name);
                    }
                    else
                    {
                        list.add(cur);
                    }
                }
                table_columns.put(data[0], list);
            }
            console.close();
        }
        catch(FileNotFoundException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return table_columns;
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

        for(String key: map.keySet())
        {
            node cur = map.get(key);
            System.out.println(cur.name + " = " + cur.new_name);
        }
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

    public static void build_graph(ArrayList<String> list, Map<String, node> map)
    {
        for(String connection: list)
        {
            String[] edge = connection.split(" = ");
            String u = edge[0];
            String v = edge[1];
            add_node(map, u, v);
        }
    }

    public static ArrayList<String> read()
    {
        ArrayList<String> list = new ArrayList<>();
        try
        {
            File file = new File("connections.txt");

            Scanner console = new Scanner(file);

            while(console.hasNextLine())
            {
                String data = console.nextLine();
                list.add(data);
            }
            console.close();
        }
        catch(FileNotFoundException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return list;
    }
}
