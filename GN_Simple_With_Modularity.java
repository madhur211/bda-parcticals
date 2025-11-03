import java.util.*;

public class GN_Simple_With_Modularity {
    
    // Adjacency list for the (mutable) working graph
    static Map<String, List<String>> g = new HashMap<>();
    // Store original edges to calculate modularity correctly
    static List<String[]> originalEdges = new ArrayList<>();

    public static void main(String[] args) {
        // --- Hard-coded graph edges ---
        String[][] edges = {
            {"A","B"}, {"A","C"}, {"A","D"},
            {"B","C"}, {"C","D"}, {"D","E"},
            {"E","F"}, {"E","G"}, {"E","H"},
            {"F","G"}, {"G","H"}
        };

        for (String[] e : edges) {
            addEdge(e[0], e[1]);
            originalEdges.add(new String[]{e[0], e[1]});
        }

        // Pre-calculate total edges (m) and degrees for the original graph
        int m = originalEdges.size();
        Map<String, Integer> degrees = new HashMap<>();
        for (String[] e : originalEdges) {
            degrees.put(e[0], degrees.getOrDefault(e[0], 0) + 1);
            degrees.put(e[1], degrees.getOrDefault(e[1], 0) + 1);
        }

        // Build original adjacency (immutable for modularity calc)
        Map<String, Set<String>> originalAdj = new HashMap<>();
        for (String[] e : originalEdges) {
            originalAdj.computeIfAbsent(e[0], k -> new HashSet<>()).add(e[1]);
            originalAdj.computeIfAbsent(e[1], k -> new HashSet<>()).add(e[0]);
        }

        System.out.println("Initial Graph:");
        printGraph();

        double peakQ = Double.NEGATIVE_INFINITY;
        List<List<String>> peakCommunities = null;
        Map<String, List<String>> peakGraphState = null;

        int step = 1;
        while (true) {
            Map<String, Double> bet = calcEdgeBetweenness();
            if (bet.isEmpty()) break;

            double maxBetweenness = Collections.max(bet.values());

            // Remove all edges with the highest betweenness
            List<String> removed = new ArrayList<>();
            for (Map.Entry<String, Double> entry : bet.entrySet()) {
                if (Double.compare(entry.getValue(), maxBetweenness) == 0) {
                    removeEdge(entry.getKey());
                    removed.add(entry.getKey());
                }
            }

            System.out.println("\n==================== STEP " + step++ + " ====================");
            System.out.println("Removed edge(s) with max betweenness (" + String.format("%.2f", maxBetweenness) +"): " + removed);
            printGraph();

            List<List<String>> communities = getCommunities();
            System.out.println("Current Communities: " + communities);

            // Individual modularities (using ORIGINAL adjacency)
            System.out.println("\n--- Individual Community Modularities ---");
            Map<List<String>, Double> communityModularities = calcIndividualModularitiesUsingOriginal(communities, m, degrees, originalAdj);
            for (Map.Entry<List<String>, Double> e : communityModularities.entrySet()) {
                System.out.printf("  - Modularity of %s: %.4f%n", e.getKey(), e.getValue());
            }
            
            // Overall Q is just the sum of the individual modularities
            double Q = communityModularities.values().stream().mapToDouble(Double::doubleValue).sum();
            System.out.printf("\nOverall Modularity of Partitioning (Q): %.4f%n", Q);

            if (Q > peakQ) {
                peakQ = Q;
                peakCommunities = deepCopyCommunities(communities);
                peakGraphState = deepCopyGraph(g);
            }

            // Stop when no edges are left
            boolean anyEdgesLeft = g.values().stream().anyMatch(list -> !list.isEmpty());
            if (!anyEdgesLeft) {
                System.out.println("\nAlgorithm finished: No more edges to remove.");
                break;
            }
        }

        System.out.println("\n==================== PEAK MODULARITY ====================");
        if (peakCommunities != null) {
            System.out.printf("Peak Overall Modularity (Q): %.4f\n", peakQ);
            System.out.println("Communities at peak: " + peakCommunities);
            System.out.println("Graph snapshot (adjacency) at peak:");
            List<String> nodes = new ArrayList<>(peakGraphState.keySet());
            Collections.sort(nodes);
            for (String n : nodes) {
                System.out.println("  " + n + " -> " + peakGraphState.get(n));
            }
        } else {
            System.out.println("No peak recorded.");
        }
    }

    static Map<List<String>, Double> calcIndividualModularitiesUsingOriginal(List<List<String>> comms, int m, Map<String, Integer> deg, Map<String, Set<String>> originalAdj) {
        Map<List<String>, Double> results = new HashMap<>();
        if (m == 0) return results;

        for (List<String> c : comms) {
            double communityQ = 0.0;
            for (String i : c) {
                for (String j : c) {
                    boolean Aij = originalAdj.getOrDefault(i, Collections.emptySet()).contains(j);
                    communityQ += (Aij ? 1.0 : 0.0) - (double)(deg.get(i) * deg.get(j)) / (2.0 * m);
                }
            }
            results.put(new ArrayList<>(c), communityQ / (2.0 * m));
        }
        return results;
    }

    static void addEdge(String a, String b) {
        g.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
        g.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
    }

    static void removeEdge(String e) {
        String[] x = e.split("-");
        List<String> la = g.get(x[0]);
        List<String> lb = g.get(x[1]);
        if (la != null) la.remove(x[1]);
        if (lb != null) lb.remove(x[0]);
    }

    static void printGraph() {
        System.out.println("\nCurrent Graph State:");
        List<String> sortedNodes = new ArrayList<>(g.keySet());
        Collections.sort(sortedNodes);
        for (String node : sortedNodes) {
            System.out.println("  " + node + " -> " + g.get(node));
        }
    }

    static Map<String, Double> calcEdgeBetweenness() {
        Map<String, Double> betweenness = new HashMap<>();
        List<String> nodes = new ArrayList<>(g.keySet());

        for (String s : nodes) {
            Map<String, List<String>> parents = new HashMap<>();
            Map<String, Integer> dist = new HashMap<>();
            Map<String, Double> paths = new HashMap<>();
            Stack<String> stack = new Stack<>();
            Queue<String> q = new LinkedList<>();

            for (String node : nodes) {
                dist.put(node, -1);
                paths.put(node, 0.0);
            }

            dist.put(s, 0);
            paths.put(s, 1.0);
            q.add(s);
            
            while (!q.isEmpty()) {
                String v = q.poll();
                stack.push(v);
                
                for (String w : g.getOrDefault(v, Collections.emptyList())) {
                    if (dist.get(w) < 0) {
                        q.add(w);
                        dist.put(w, dist.get(v) + 1);
                    }
                    if (dist.get(w) == dist.get(v) + 1) {
                        paths.put(w, paths.get(w) + paths.get(v));
                        parents.computeIfAbsent(w, k -> new ArrayList<>()).add(v);
                    }
                }
            }

            Map<String, Double> dependency = new HashMap<>();
            while (!stack.isEmpty()) {
                String w = stack.pop();
                if (s.equals(w)) continue;

                for (String v : parents.getOrDefault(w, Collections.emptyList())) {
                    double credit = (paths.get(v) / paths.get(w)) * (1.0 + dependency.getOrDefault(w, 0.0));
                    String edge = v.compareTo(w) < 0 ? v + "-" + w : w + "-" + v;
                    betweenness.put(edge, betweenness.getOrDefault(edge, 0.0) + credit);
                    dependency.put(v, dependency.getOrDefault(v, 0.0) + credit);
                }
            }
        }

        betweenness.replaceAll((k, v) -> v / 2.0);
        return betweenness;
    }

    static List<List<String>> getCommunities() {
        Set<String> visited = new HashSet<>();
        List<List<String>> communities = new ArrayList<>();
        for (String node : g.keySet()) {
            if (!visited.contains(node)) {
                List<String> component = new ArrayList<>();
                dfs(node, visited, component);
                if (!component.isEmpty()) communities.add(component);
            }
        }
        return communities;
    }

    static void dfs(String node, Set<String> visited, List<String> component) {
        visited.add(node);
        component.add(node);
        for (String neighbor : g.getOrDefault(node, Collections.emptyList())) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, visited, component);
            }
        }
    }

    static List<List<String>> deepCopyCommunities(List<List<String>> comms) {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> c : comms) copy.add(new ArrayList<>(c));
        return copy;
    }

    static Map<String, List<String>> deepCopyGraph(Map<String, List<String>> graph) {
        Map<String, List<String>> cp = new HashMap<>();
        for (Map.Entry<String, List<String>> e : graph.entrySet()) {
            cp.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return cp;
    }
}