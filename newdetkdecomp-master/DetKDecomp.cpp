// DetKDecomp.cpp: implementation of the DetKDecomp class.
//
//////////////////////////////////////////////////////////////////////


#include <cstdio>
#include <iostream>
#include <cmath>
#include <algorithm>

#include <list>
#include <vector>

using namespace std;

#include "DetKDecomp.h"
#include "Hypertree.h"
#include "Hypergraph.h"
#include "Hyperedge.h"
#include "Vertex.h"
#include "Globals.h"
#include "Subedges.h"
#include "SubedgeSeparatorFactory.h"
#include "DecompComponent.h"


//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////


DetKDecomp::DetKDecomp(const HypergraphSharedPtr &HGraph, int k, bool bip) : Decomp(HGraph, k), MyBIP{ bip }
{
	if (MyBIP)
		MySubedges = make_unique<Subedges>(HGraph, k);
}


DetKDecomp::~DetKDecomp()
{
}



//////////////////////////////////////////////////////////////////////
// Class methods
//////////////////////////////////////////////////////////////////////


/*
***Description***
The method selects an initial subset within a set of hyperedges such that a given set of 
nodes is covered.

INPUT:	Nodes: Array of nodes to be covered
		Edges: Array of hyperedges
		bInComp: Boolean array indicating the position of each hyperedge
		iSize: Number of elements in Edges, bInComp, and CovWeights
OUTPUT: Set: Selection of hyperedges in Edges such that all nodes in Nodes are covered
		CovWeights: Array with the number of connector nodes covered by each hyperedge
		return: Number of selected hyperedges; -1 if nodes cannot be covered
*/

int DetKDecomp::setInitSubset(const VertexSet &Vertices, HyperedgeVector &Edges, vector<int> &Set, vector<bool> &InComp, vector<int> &CovWeights) const
{
	int uncov{ (int)Vertices.size() };
	int weight, e;

	// Reset node labels
	MyHg->setVertexLabels(-1);
	for(auto v : Vertices)
		v->setLabel(0);

	// Sort hyperedges according to their weight,
	// i.e., the number of nodes in Nodes they contain
	e = 0;
	for(auto he : Edges) {
		he->setLabel((int)InComp[e]);
		CovWeights[e] = 0;
		for(auto v : he->allVertices())
			if(v->getLabel() == 0)
				--CovWeights[e];
		e++;
	}

	sortVectors<HyperedgeSharedPtr>(Edges, CovWeights, 0, (int)Edges.size()-1);

	// Reset bInComp and summarize weights
	for(int i=0; i < (int)Edges.size(); i++) {
		Edges[i]->getLabel() == 0 ? InComp[i] = false : InComp[i] = true;
		CovWeights[i] = -CovWeights[i];
	}
	weight = 0;
	for(int i=(int)Edges.size()-1; i >= 0; i--) {
		weight += CovWeights[i];
		CovWeights[i] = weight;
	}

	// Select an initial subset of at most iMyK hyperedges
	return coverNodes(Edges, Set, InComp, CovWeights, uncov, false);
}


/*
***Description***
The method selects the next subset within a set of hyperedges such that a given set of 
nodes is covered.

INPUT:	Nodes: Array of nodes to be covered
		Edges: Array of hyperedges
		Set: Integer array of indices in Edges representing a subset selection
		bInComp: Boolean array indicating the position of each hyperedge
		CovWeights: Array with the number of connector nodes covered by each hyperedge
		iSize: Number of elements in Edges, bInComp, and CovWeights
OUTPUT: Set: Selection of hyperedges in Edges such that all nodes in Nodes are covered
		return: Number of selected hyperedges; -1 if there is no alternative selection
*/

int DetKDecomp::setNextSubset(const VertexSet &Vertices, HyperedgeVector &Edges, vector<int> &Set, vector<bool> &InComp, vector<int> &CovWeights) const
{
	MyHg->setVertexLabels(-1);
	for(auto v : Vertices)
		v->setLabel(0);

	// Select the next subset of at most iMyK hyperedges
	return coverNodes(Edges, Set, InComp, CovWeights, Vertices.size(), true);
}


int DetKDecomp::coverNodes(HyperedgeVector &Edges, vector<int> &Set, vector<bool> &InComp, vector<int> &CovWeights, size_t Uncovered, bool Reconstr) const
{
	int i;

	int pos, nbr_sel, in_comp_sel, weight;
	int size{ (int)Edges.size() };
	bool covered, back, select;
	list<vector<int>> label_stack;
	list<int *>::iterator ListIter;
	vector<int> tmp_labels;

	pos = nbr_sel = in_comp_sel = 0;
	Uncovered == 0 ? covered = true : covered = false;

	// Reconstruct the search tree according to Set
	if (Reconstr) {
		if (Set[0] == -1) return -1;

		for (nbr_sel = 0; Set[nbr_sel + 1] != -1; nbr_sel++) {
			pos = Set[nbr_sel];
			if (InComp[pos])
				++in_comp_sel;

			tmp_labels.clear();
			tmp_labels.resize(Edges[pos]->getNbrOfVertices());
			//if(iTmpLabels == NULL)
			//	writeErrorMsg("Error assigning memory.", "DetKDecomp::coverNodes");

			// Save labels of nodes in the actual hyperedge and label these nodes by 1
			i = 0;
			for (auto v : Edges[pos]->allVertices()) {
				tmp_labels[i] = v->getLabel();
				v->setLabel(1);
				i++;
			}

			label_stack.push_back(tmp_labels);
		}

		pos = Set[nbr_sel];
		Uncovered = 0;
		for (auto v : Edges[pos]->allVertices())
			if(v->getLabel() == 0)
				++Uncovered;
		++pos;
	}

	// Search for a new set of covering hyperedges
	while(!covered) {
		for(back=false; !covered; pos++) { 

			// Check if nodes can be covered by the remaining hyperedges;
			// prune the search tree if not
			i = pos + (MyK-nbr_sel);
			if(i < size)
				weight = CovWeights[pos] - CovWeights[i];
			else
				if(pos < size)
					weight = CovWeights[pos];
				else
					weight = 0;
			if((weight < Uncovered) || (weight == 0)) {
				back = true;
				break;
			}

			// Check if the actual hyperedge covers some uncovered node
			select = false;
			if(InComp[pos] || (in_comp_sel > 0) || (nbr_sel < MyK-1))
				for(auto v : Edges[pos]->allVertices())
					if(v->getLabel() == 0) {
						select = true;
						break;
					}

			if (select) {
				// Select the actual hyperedge
				Set[nbr_sel++] = pos;
				if (InComp[pos])
					++in_comp_sel;

				tmp_labels.clear();
				tmp_labels.resize(Edges[pos]->getNbrOfVertices());

				//iTmpLabels = new int[Edges[iPos]->getNbrOfNodes()];
				//if(iTmpLabels == NULL)
				//	writeErrorMsg("Error assigning memory.", "DetKDecomp::coverNodes");

				// Save labels of nodes in the actual hyperedge and label these nodes by 1
				i = 0;
				for (auto v : Edges[pos]->allVertices()) {
					tmp_labels[i] = v->getLabel();
					v->setLabel(1);
					if(tmp_labels[i] == 0)
						--Uncovered;
					i++;
				}
				label_stack.push_back(tmp_labels);

				// Check whether all nodes are covered
				if(Uncovered == 0)
					covered = true;
			}
		}

		if(back) {
			if(nbr_sel == 0) {
				// No more possibilities to cover the nodes
				nbr_sel = -1;
				break;
			}
			else {
				// Undo the last selection if the nodes cannot be covered in this way
				pos = Set[--nbr_sel];
				if (InComp[pos])
					--in_comp_sel;

				tmp_labels = label_stack.back();
				label_stack.pop_back();

				i = 0;
				for (auto v : Edges[pos]->allVertices()) {
					v->setLabel(tmp_labels[i]);
					if(tmp_labels[i] == 0)
						++Uncovered;
					i++;
				}
				//delete [] iTmpLabels;
				++pos;
			}
		}
	}

	if(nbr_sel >= 0)
		Set[nbr_sel] = -1;

	//for(ListIter=LabelStack.begin(); ListIter != LabelStack.end(); ListIter++)
	//	delete [] *ListIter;

	return nbr_sel;
}

/*
***Description***
The method divides a given set of hyperedges into inner hyperedges and its boundary according
to a given set of boundary nodes. The inner hyperedges are those not containing a boundary node.
All other hyperedges containing a boundary node (within the given set or outside) belong to
the boundary hyperedges.

INPUT:	HEdges: Hyperedges
		Nodes: Boundary nodes
OUTPUT: Inner: Inner hyperedges not containing a boundary node
		Bound: Boundary hyperedges containing a boundary node
		return: Index in Bound that separates boundary hyperedges within and outside the given 
		set of hyperedges; all hyperedges before this index are inside the given set and all
		hyperedges starting at this index are outside the given set.
*/

size_t DetKDecomp::divideCompEdges(const HyperedgeVector &HEdges, const VertexSet &Vertices, HyperedgeVector &Inner, HyperedgeVector &Bound) const
{
	bool covered;
	size_t cnt_edges{ HEdges.size() };
	list<HyperedgeSharedPtr> innerb, outerb;
	unordered_map<HyperedgeSharedPtr, int> edgeLabels;
	unordered_map<VertexSharedPtr, int> vertexLabels;
	
	for (auto he : HEdges)
		edgeLabels[he] = 1;

	// Compute the hyperedges containing a boundary node and store them
	// in InnerB if they are contained in the given set and in OuterB otherwise
	for(auto v : Vertices)
		for(auto he : MyHg->allVertexNeighbors(v)) {
			// Only use normal edges for separators
			if (!he->isHeavy()) {
				switch (edgeLabels[he]) {
				case 0:	// Hyperedge is not contained in HEdges
					edgeLabels[he]  = -1;
					outerb.push_back(he);
					break;
				case 1:	// Hyperedge is contained in HEdges
					edgeLabels[he] = -1;
					innerb.push_back(he);
					break;
				}
			}
		}

	// Store hyperedges not containing a boundary node in the array
	for (auto he : HEdges)
		if (edgeLabels[he] > 0)
			Inner.push_back(he);

	edgeLabels.clear();
	for(auto he : outerb)
		edgeLabels[he] = 1;

	

	// Remove redundant hyperedges from OuterB; such a hyperedge is redundant if the set of its
	// boundary nodes is covered by some other hyperedge in OuterB
	for(auto it=outerb.begin(); it != outerb.end(); it++) {
		// Label all boundary nodes by 1
		for (auto v : Vertices)
			vertexLabels[v] = (1);

		// Reset the labels of all nodes of the actual hyperedge in OuterB
		for(auto v : (*it)->allVertices())
			vertexLabels[v] = 0;

		// Check whether some hyperedge in OuterB in the neighbourhood of the actual hyperedge
		// contains no node labeled with 1; in this case it can be removed from OuterB since its 
		// boundery nodes are covered by the actual hyperedge in OuterB
		for(auto he : MyHg->allEdgeNeighbors(*it))
			if(edgeLabels[he] != 0) {
				covered = true;
				for(auto v : he->allVertices())
					if (vertexLabels[v] != 0) {
						covered = false;
						break;
					}
				if(covered)
					outerb.remove(he);
			}
	}

	// Store hyperedges containing a boundary node in the array
	for(auto he : innerb)
		Bound.push_back(he);
	for(auto he : outerb)
		Bound.push_back(he);

	return innerb.size();
}

CompCache & DetKDecomp::getSepParts(SeparatorSharedPtr & sep) const
{
	for (auto t : MyTriedSeps)
		if (t.first == sep) {
			sep = t.first;
			break;
		}
	
	return MyTriedSeps[sep];
}


/*
***Description***
The method checks whether the actual separator occurs in the MySeps list of already
used separators. If so, the method returns two lists of hyperedges identifiying the 
decomposable and undecomposable partitions. It is assumed that separator hyperedges 
are labeled by -1 and no other hyperedges are labeled by -1.

INPUT:	iSepSize: Size of the actual separator
OUTPUT: Separator: Stored separator
		SuccParts: List of decomposable partitions
		FailParts: List of undecomposable partitions
		return: true if separator was found; otherwise false
*/
/*
bool DetKDecomp::getSepParts(int SepSize, HE_VEC **Separator, list<Hyperedge *> **SuccParts, list<Hyperedge *> **FailParts)
{
	int i;
	bool found = false;
	list<list<Hyperedge *> *>::iterator SuccPartIter, FailPartIter;

	// Check whether the actual separator is known to be decomposable or undecomposable
	SuccPartIter = MySuccSepParts.begin();
	FailPartIter = MyFailSepParts.begin();
	for(auto sep : MySeps) {
		found = true;
		i = 0;
		for (auto he : *sep) {
			if (he->getLabel() != -1) {
				found = false;
				break;
			}
			i++;
		}

		if(found && (i == SepSize)) {
			*Separator = sep;
			*SuccParts = *SuccPartIter;
			*FailParts = *FailPartIter;
			return true;
		}

		++SuccPartIter;
		++FailPartIter;
	}

	return false;
}
*/

/*
***Description***
The method searches in a list of hyperedges for an edge with a given label.

INPUT:	HEdges: Pointer to a list of hyperedges
		iLabel: Label
OUTPUT: return: true if there exists an edge in HEdges labeled by iLabel; otherwise false
*/

bool DetKDecomp::containsLabel(list<Hyperedge *> *HEdges, int iLabel)
{
	list<Hyperedge *>::iterator ListIter;

	// Search for a hyperedge labeled by iLabel
	for(ListIter=HEdges->begin(); ListIter != HEdges->end(); ListIter++)
		if((*ListIter)->getLabel() == iLabel)
			return true;

	return false;
}


/*
***Description***
The method decomposes the hyperedges in a subhypergraph as described in Gottlob and 
Samer: A Backtracking-Based Algorithm for Computing Hypertree-Decompositions.

INPUT:	HEdges: Hyperedges in the subgraph
		Connector: Connector nodes that must be covered
		iRecLevel: Recursion level
OUTPUT: return: Hypertree decomposition of HEdges
*/
bool DetKDecomp::covers(const HyperedgeVector &Edges, const VertexSet &Vertices) const
{
	bool found;

	for (auto v : Vertices) {
		found = false;
		for (auto e : Edges) {
			if (e->find(v) != nullptr) {
				found = true;
				break;
			}
		}

		if (!found)
			return false;
	}
		
	return true;
}

HypertreeSharedPtr DetKDecomp::decomp(const HyperedgeVector &HEdges, const VertexSet &Connector, int RecLevel) const
{
	int i, j;
	
	list<Hypertree *>::iterator TreeIter;
	
	SeparatorSharedPtr separator{ nullptr };
	vector<int> cov_sep_set, cov_weights;
	vector<bool> in_comp;
	size_t cnt_edges{ HEdges.size() };
	int comp_end, nbr_sel_cov, i_add, sep_size;
	size_t nbr_of_parts;
	bool add_edge, fail_sep, sub_edge;
    HypertreeSharedPtr htree{ nullptr };

	HyperedgeVector inner_edges, bound_edges, add_edges;

	/*
	for (int k = 0; k <= RecLevel; k++)
		cout << "+";
	cout << " " << *HEdges << endl;
	*/

	if ((htree = decompTrivial(HEdges, Connector)) != nullptr)
		return htree;	

	// Divide hyperedges into inner hyperedges and hyperedges containing some connecting nodes
	comp_end = (int)divideCompEdges(HEdges, Connector, inner_edges, bound_edges);

	in_comp.resize(bound_edges.size());
	cov_weights.resize(bound_edges.size(),0);
	cov_sep_set.resize(MyK+1);
	for(i=0; i < bound_edges.size(); i++)
		i < comp_end ? in_comp[i] = true : in_comp[i] = false;

	// Select initial hyperedges to cover the connecting nodes
	nbr_sel_cov = setInitSubset(Connector, bound_edges, cov_sep_set, in_comp, cov_weights);

	// Initialize AddEdges array
	for (i = 0; i < bound_edges.size(); i++)
		if (in_comp[i])
			if (!bound_edges[i]->isHeavy())
				add_edges.push_back(bound_edges[i]);
	for (i = 0; i < inner_edges.size(); i++)
		if (!inner_edges[i]->isHeavy())
			add_edges.push_back(inner_edges[i]);
	if(add_edges.size() <= 0)
		writeErrorMsg("Illegal number of hyperedges.", "DetKDecomp::decomp");

	if(nbr_sel_cov >= 0)
		do {
			// Check whether a covering hyperedge within the component was selected
			add_edge = true;
			for(i=0; i < nbr_sel_cov; i++)
				if(in_comp[cov_sep_set[i]]) {
					add_edge = false;
					break;
				}

			// Stop if no inner hyperedge can be in the separator
			if(!add_edge || (MyK-nbr_sel_cov > 0)) {

				i_add = 0;
				add_edge ? sep_size = nbr_sel_cov+1 : sep_size = nbr_sel_cov;

				do {

					sub_edge = false;
					// Output the search progress
					// cout << "(" << RecLevel << ")" << endl;

					// Create a separator 
					separator = make_shared<Separator>();

					for(i=0; i < nbr_sel_cov; i++) 
						separator->insert(bound_edges[cov_sep_set[i]]);
					
					if (add_edge)
						separator->insert(add_edges[i_add]);

					SubedgeSeparatorFactory sub_sep_fac;

					do {
						// Check if selected hyperedges were already used before as separator
						auto &reused = getSepParts(separator);

						/*
						if (!reused_sep) {
							// Create a separator array and lists for decomposable and undecomposable parts
							// keep sub_separator if built from a subedge separator
							if (sub_edge) {
								separator = sub_separator;
							}
							else {
								separator = new HE_VEC(sep_size, nullptr);
								if (separator == nullptr)
									writeErrorMsg("Error assigning memory.", "DetKDecomp::decomp");

								// Store separating hyperedges in the separator array
								for (i = 0; i < nbr_sel_cov; i++)
									(*separator)[i] = bound_edges[cov_sep_set[i]];
								if (add_edge)
									(*separator)[i++] = add_edges[i_add];
							}

							succ_parts = new list<Hyperedge *>;
							fail_parts = new list<Hyperedge *>;
							if ((succ_parts == nullptr) || (fail_parts == nullptr))
								writeErrorMsg("Error assigning memory.", "DetKDecomp::decomp");

							
							//Separator[i] = NULL;

							MySeps.push_back(separator);
							MySuccSepParts.push_back(succ_parts);
							MyFailSepParts.push_back(fail_parts);
						}
						else
							if (sub_edge)
								delete sub_separator;

						//Debugging output
						/*
						cout << "+++ Separator: ";
						for (int i = 0; i < separator->size(); i++) {
							cout << (*separator)[i]->getName();
							if (i < separator->size()-1)
								cout << ",";
						}
						cout << endl;
						*/

						vector<DecompComponent> partitions;
						vector<bool> cut_parts;
						list<HypertreeSharedPtr> Subtrees;

						// Separate hyperedges into partitions with corresponding connector nodes
						nbr_of_parts = separate(separator, HEdges, partitions);

						// Create auxiliary array
						cut_parts.clear();
						cut_parts.resize(nbr_of_parts);

						// Check partitions for decomposibility and undecomposibility
						fail_sep = false;
						for (i = 0; i < partitions.size(); i++) {
							if (partitions[i].size() >= cnt_edges) {
								//writeErrorMsg("Monotonicity violated.", "DetKDecomp::decomp");
								fail_sep = true;
								reused.failed.push_back(partitions[i].first());
								break;
							}

							// Check for undecomposability
							if (partitions[i].containsOneOf(reused.failed)) {
								fail_sep = true;
								break;
							}

							// Check for decomposibility
							if (partitions[i].containsOneOf(reused.succ))
								cut_parts[i] = true;
							else
								cut_parts[i] = false;
						}

						if (!fail_sep) {
							// Decompose partitions into hypertrees
							for (i = 0; i < partitions.size(); i++) {
								if (cut_parts[i]) {
									// Prune subtree
									htree = getCutNode(RecLevel + 1, partitions[i]);
								}
								else {
									// Decompose component recursively
									htree = decomp(partitions[i], RecLevel + 1);
									if (htree == nullptr)
										reused.failed.push_back(partitions[i][0]);
									else
										reused.succ.push_back(partitions[i][0]);
								}

								if (htree != nullptr)
									Subtrees.push_back(htree);
								else break;
							}

							//Either all components decomposed or some component failed
							if (htree != nullptr) {
								// Create a new hypertree node
								if (sub_edge) {
									for (auto he : *separator)
										he->setLabel(-1);
								}
								else {
									for (i = 0; i < nbr_sel_cov; i++) {
										j = cov_sep_set[i];
										in_comp[j] ? bound_edges[j]->setLabel(-1) : bound_edges[j]->setLabel(0);
									}
									if (add_edge)
										add_edges[i_add]->setLabel(-1);
								}
								htree = getHTNode(HEdges, separator, Connector, Subtrees);
							}
						}

						if (MyBIP && htree == nullptr) {
							//Start sub_edge procedure
							if (!sub_edge) {
								sub_edge = true;
								sub_sep_fac.init(MyHg, HEdges, separator, MySubedges);
							}

							bool found = false;
							while (!found && (separator = sub_sep_fac.next())->size() != 0) {
								found = true;
								//Check if new sub_separator still covers all connector nodes
								if (!covers(separator, Connector))
									found = false;
							} 

							// All subedge separators tried
							if (separator->size() == 0)
								sub_edge = false;
						}

					} while (MyBIP && sub_edge && htree == nullptr);
					
				} while(add_edge && (htree == nullptr) && (++i_add < add_edges.size()));
			}

		} while((htree == nullptr) && ((nbr_sel_cov = setNextSubset(Connector, bound_edges, cov_sep_set, in_comp, cov_weights)) > 0));

	return htree;
}


/*
***Description***
The method expands pruned hypertree nodes, i.e., subgraphs which were not decomposed but are
known to be decomposable are decomposed.

INPUT:	HTree: Hypertree that has to be expanded
OUTPUT: HTree: Expanded hypertree
*/

void DetKDecomp::expandHTree(HypertreeSharedPtr &HTree)
{
	HypertreeSharedPtr cut_node, subtree;
	
	while((cut_node = HTree->getCutNode()) != nullptr) {

		// Store subgraph in an array
		auto &lambda = cut_node->getLambda();
		HyperedgeVector edges(lambda.begin(), lambda.end());

		// Store connector nodes in an array
		auto &chi = cut_node->getChi();
		VertexSet connector(chi);

		// Reconstruct parent separator
		auto parent = cut_node->getParent().lock();
		auto &parent_lambda = parent->getLambda();
		Separator parent_sep(HyperedgeVector(parent_lambda.begin(),parent_lambda.end()));
		
				// Decompose subgraph
		subtree = decomp(edges, connector, cut_node->getLabel());
		if(subtree == nullptr)
			writeErrorMsg("Illegal decomposition pruning.", "H_DetKDecomp::expandHTree");

		// Replace the pruned node by the corresponding subtree
		parent->insChild(subtree);
		parent->remChild(cut_node);

	}
}


/*
***Description***
The method builds a hypertree decomposition of a given hypergraph as described in Gottlob 
and Samer: A Backtracking-Based Algorithm for Computing Hypertree-Decompositions.

INPUT:	iK: Maximum separator size
OUTPUT: return: Hypertree decomposition of HGraph
*/

HypertreeSharedPtr DetKDecomp::buildHypertree()
{
	HypertreeSharedPtr HTree;
	HyperedgeVector HEdges;

	// Order hyperedges heuristically
	HEdges = MyHg->getMCSOrder();

     	//cout << HEdges << endl;

	// Store initial heuristic order as weight
	//for(int i=0; i < HEdges.size(); i++)
	//	HEdges[i]->setWeight(i);

	// Build hypertree decomposition
	HTree = decomp(HEdges);

	// Expand pruned hypertree nodes
	if((HTree != nullptr) && (HTree->getCutNode() != nullptr)) {
		cout << "Expanding hypertree ..." << endl;
		expandHTree(HTree);
	}

	return HTree;
}


