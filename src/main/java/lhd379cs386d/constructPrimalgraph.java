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

public class constructPrimalgraph {

    static String TPC_H_dir = "TPC-HBenchmark/";
    static String JOB_dir = "JOBBenchmark/";
    static String MLK_dir = "MLKBenchmark/";

    public static void main(String[] args) {
        // ArrayList<String> hypergraph = read_hypergraph("TPC-HBenchmark/Hypergraph/q5.txt");
        // construct_primalgraph(hypergraph, "q5.txt");

        String[] dirs = new String[] {TPC_H_dir, JOB_dir, MLK_dir};

        for(String dir: dirs)
        {
            ArrayList<String> files = get_hypergraph(dir);
            for(String file: files)
            {
                ArrayList<String> hypergraph = read_hypergraph(dir + "/Hypergraph/" + file + ".txt");
                construct_primalgraph(hypergraph, file, dir);
            }
        }
    }

    public static String display_option_and_get_input()
    {
        System.out.println("Benchmark Options");
        System.out.println("TPC_H");
        System.out.println("JOB");
        System.out.println("MLK");

        Scanner console = new Scanner(System.in);
        System.out.print("Enter Benchmark: ");
        String input = console.nextLine().toLowerCase();

        return input;
    }

    public static ArrayList<String> get_hypergraph(String dir)
    {
        ArrayList<String> files = new ArrayList<>();
        File folder = new File(dir + "Hypergraph");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
		  File file = listOfFiles[i];
		  if (file.isFile() && file.getName().endsWith(".txt")) {
              if(file.getName().compareTo("scheme.txt") != 0)
              {
                files.add(file.getName().substring(0, file.getName().length() - ".txt".length()));
              }
		  } 
		}

        return files;
    }

    public static ArrayList<String> read_hypergraph(String dir)
    {
        ArrayList<String> hypergraph = new ArrayList<>();

        try
        {
            File file = new File(dir);

            Scanner console = new Scanner(file);

            while(console.hasNextLine())
            {
                String data = console.nextLine();
                // System.out.println(data);
                data = data.substring(0, data.length() - 2).replace(" (", ", ");
                // System.out.println(data);
                hypergraph.add(data);
            }
            console.close();
        }
        catch(FileNotFoundException e)
        {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return hypergraph;
    }

    public static void construct_primalgraph(ArrayList<String> hypergraph, String filename, String dir)
    {
        ArrayList<Integer[]> edges = new ArrayList<>();
        int max = 0;
        System.out.println();
        for(String h: hypergraph)
        {
            String[] cur = h.split(", ");
            ArrayList<Integer> list = new ArrayList<>();
            String h_name = cur[0];

            for(int i = 1; i < cur.length; i++)
            {
                list.add(Integer.parseInt(cur[i].substring(1)));
            }
            Collections.sort(list);
            // System.out.println(list);
            for(int i = 0; i < list.size() - 1; i++)
            {
                int left = list.get(i);
                max = Math.max(max, left);
                for(int j = i + 1; j < list.size(); j++)
                {
                    int right = list.get(j);
                    edges.add(new Integer[] {left, right});
                    max = Math.max(max, right);
                }
            }
        }
        Collections.sort(edges, new Comparator<Integer[]>() {
            @Override
            public int compare(Integer[] o1, Integer[] o2) {
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

        write_primal_graph(edges, max, filename, dir);
    }

    public static void write_primal_graph(ArrayList<Integer[]> edges, int max, String filename, String dir)
    {
        try
        {
            FileWriter  writer = new FileWriter(dir + "/Primalgraph/" + filename + ".graph");
            writer.write("p " + max + " " + edges.size() + "\n");
            for(Integer[] edge: edges)
            {
                writer.write("e " + (edge[0] - 1) + " " + (edge[1] - 1) + "\n");   
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
}
