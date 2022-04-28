package lhd379cs386d;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.io.FileNotFoundException;

import javax.lang.model.util.ElementScanner6;
import javax.swing.plaf.synth.SynthTextAreaUI;

public class rename {

    static String prefix = "CommonAttr";

    public static void main(String[] args) {
        Map<String, node> map = new HashMap<>();
        ArrayList<String> list = read();
        build_graph(list, map);
        rename(map);
        Map<String, ArrayList<String>> table_columns = construct_table_with_new_name(map);
        ArrayList<String>  hypergraph = construct_hypergraph(table_columns);
        construct_primalgraph(hypergraph);
    }

    public static void construct_primalgraph(ArrayList<String> hypergraph)
    {
        ArrayList<String[]> edges = new ArrayList<>();
        for(String h: hypergraph)
        {
            String[] cur = h.split(" ");
            ArrayList<String> list = new ArrayList<>();
            String h_name = cur[0];

            for(int i = 1; i < cur.length; i++)
            {
                list.add(cur[i]);
            }
            Collections.sort(list);
            for(int i = 0; i < list.size() - 1; i++)
            {
                for(int j = i + 1; j < list.size(); j++)
                {
                    // String edge = list.get(i) + " " + list.get(j);
                    //System.out.println(list.get(i) + " " + list.get(j));
                    edges.add(new String[] {list.get(i), list.get(j)});
                }
            }
        }
        Collections.sort(edges, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                if(o1[0].compareTo(o2[0]) < 0)
                {
                    return -1;
                }
                else if(o1[0].compareTo(o2[0]) > 0)
                {
                    return 1;
                }
                else
                {
                    return o1[1].compareTo(o2[1]);
                }
            }
        });
        for(int i = 0; i < edges.size(); i++)
        {
            System.out.println(edges.get(i)[0] + " " + edges.get(i)[1]);
        }
    }

    public static ArrayList<String> construct_hypergraph(Map<String, ArrayList<String>> table_columns)
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

        for(String key: table_columns.keySet())
        {
            ArrayList<String> list = table_columns.get(key);
            StringBuilder sb = new StringBuilder();
            String hyperedge = "E" + (e_count + 1);
            System.out.print(hyperedge + " (");
            sb.append(hyperedge);

            for(int i = 0; i < list.size(); i++)
            {
                if(i != 0)
                {
                    System.out.print(" ");
                }
                sb.append(" ");
                System.out.print(table_vertex_mapping.get(list.get(i)));
                sb.append(table_vertex_mapping.get(list.get(i)));
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
            hypergraph.add(sb.toString());
        }

        for(String h: hypergraph)
        {
            System.out.println(h);
        }

        return hypergraph;
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
