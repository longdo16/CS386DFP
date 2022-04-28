package lhd379cs386d;
import java.util.*;

import javax.print.DocFlavor.STRING;

public class node {
    String name;
    String new_name;
    ArrayList<String> neighbors;

    node(String name)
    {
        this.name = name;
        this.new_name = null;
        this.neighbors = new ArrayList<>();
    }

    public void add_neighbor(String neighbor)
    {
        if(!neighbors.contains(neighbor))
        {
            neighbors.add(neighbor);
        }
    }

    public ArrayList<String> get_neighbors()
    {
        return neighbors;
    }
}
