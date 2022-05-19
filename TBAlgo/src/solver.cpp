#include "graph.hpp"
#include "IO.hpp"
#include "timer.hpp"
#include "global.hpp"
#include "minfill.hpp"
#include "MCS.hpp"
#include "treewidth.hpp"
#include "bayes.hpp"
#include "hyper.hpp"
#include "PMC.hpp"

#include <iostream>
#include <vector>
#include <cassert>

// addition
#include <fstream>
#include <string>

#define F first
#define S second

using namespace std;

int main(int argc, char** argv) {
	// (argc == 1 || argc == 2 || argc == 3);
	int upperBound = -1;
	int problem = -1;
	
	for (int i = 1; i < argc; i++) {
		string s(argv[i]);
		if (s == "-treewidth") {
			problem = PROBLEM_TREEWIDTH;
		}
		else if (s == "-minfill") {
			problem = PROBLEM_MINFILL;
		}
		else if (s == "-bayes") {
			problem = PROBLEM_BAYES;
		}
		else if (s == "-hyper") {
			problem = PROBLEM_HYPER;
		}
		else if (s == "-frachyper") {
			problem = PROBLEM_FRACHYPER;
		}
		else if (s == "-pmcprogress") {
			PMC::setPrintProgress(true);
		}
		else if (s.size() > 3 && s.substr(0, 3) == "-k=") {
			upperBound = 0;
			for (int j = 3; j < (int)s.size(); j++) {
				assert(isdigit(s[j]));
				upperBound *= 10;
				upperBound += (s[j] - '0');
			}
			cerr<<"Upper bound set to "<<upperBound<<endl;
		}
		// else {
		// 	cerr<<"invalid arg"<<endl;
		// 	abort();
		// }
	}
	if (problem == -1) {
		cerr<<"No problem specified, defaulting to treewidth"<<endl;
		problem = PROBLEM_TREEWIDTH;
	}
	
	if (upperBound != -1 && problem != PROBLEM_MINFILL && problem != PROBLEM_TREEWIDTH) {
		cerr<<"WARNING: UPPER BOUND HAS EFFECT ONLY FOR TREEWIDTH AND MINFILL"<<endl;
	}
	
	if (problem == PROBLEM_BAYES) {
		Bayes::solve();
	}
	else if (problem == PROBLEM_HYPER) {
		Hyper::solve();
	}
	else if (problem == PROBLEM_FRACHYPER) {
		Hyper::solveFrac();
	}
	else {
		Timer timer;
		timer.start();
		set<pair<int, int> > edges;
		IO io;
		int n = io.readInput(cin, edges);
		assert(n <= maxN);
		Graph G(edges);
		
		if (upperBound == -1) {
			upperBound = (int)1e9;
		}
		if (problem == PROBLEM_MINFILL) {
			if (n == 0) return 0;
			vector<pair<int, int> > sol = MinFill::SolveGraph::solveGraph(G, 0, upperBound, 0);
			
			if (sol == FAILSOLUTION) {
				assert(upperBound < (int)1e9);
				cout<<"No fill in with <= "<<upperBound<<" edges"<<endl;
				cerr<<"No fill in with <= "<<upperBound<<" edges"<<endl;
			}
			else {
				// check correctness
				G.addEdges(sol);
				auto fill0 = MCS::MCS_MP(G);
				assert(fill0.F.size() == 0);
				
				sol = G.mapBack(sol);
				
				io.printFill(cout, sol);
				
				cerr<<"Fill edges: "<<sol.size()<<endl;
				cerr<<"*Treewidth: "<<MCS::getTreeWidth(G)<<endl;
				cerr<<"Time: "<<timer.getTime().count()<<endl;
			}
		}
		else if (problem == PROBLEM_TREEWIDTH) {
			pair<int, vector<pair<int, int> > > sol = TreeWidth::SolveGraph::solveGraph(G, 0, 0, upperBound);
			
			if (sol.S == FAILSOLUTION) {
				assert(upperBound < (int)1e9);
				cout<<"Treewidth > "<<upperBound<<endl;
				cerr<<"Treewidth > "<<upperBound<<endl;
			}
			else {
				// check correctness
				G.addEdges(sol.S);
				auto fill0 = MCS::MCS_MP(G);
				assert(fill0.F.size() == 0);
				
				int treeWidth = MCS::getTreeWidth(G);
				
				assert(treeWidth == sol.F);
				
				auto treeDecomposition = MCS::getTreeDecomposition(G);
				treeDecomposition = G.mapBack(treeDecomposition);
				
				for (auto& bag : treeDecomposition.F) {
					assert((int)bag.size()-1 <= treeWidth);
				}
				
				if (G.n > 0) {
					Graph testT(treeDecomposition.S);
					assert(testT.isConnected());
					assert(testT.n == G.n);
					assert(testT.m == G.n - 1);
				}

				Graph testT(treeDecomposition.S);
				
				if(argc > 2)
				{
					
					string benchmark = argv[2];
					string file = argv[3];
					int count = 0;
					for(int i = 0; i < testT.n - 1; i++)
					{
						for(int j = i + 1; j < testT.n; j++)
						{
							if(testT.hasEdge(i, j))
							{
								count += 1;
							}
						}
					}

					string dir = benchmark + "/TreeDecomp/" + file + ".graph";

					ofstream myfile(dir, std::ofstream::out);

					if(myfile.is_open())
					{
						myfile << "p " << testT.n << " " << count << "\n";
						for(int i = 0; i < testT.n - 1; i++)
						{
							for(int j = i + 1; j < testT.n; j++)
							{
								if(testT.hasEdge(i, j))
								{
									myfile << "e " << i << " " << j << "\n";
								}
							}
						}
					}
					else
					{
						cerr << "Unable to open file" << endl;
					}
				}
				
				// io.printTreeDecomposition(cout, treeDecomposition);
				
				cerr<<"Treewidth: "<<treeWidth<<endl;
				cerr<<"*Fill edges: "<<sol.S.size()<<endl;
				cerr<<"Time: "<<timer.getTime().count()<<endl;
			}
		}
		else {
			abort();
		}
	}
}