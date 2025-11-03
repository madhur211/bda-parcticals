import java.util.*;

public class FM_Madhur {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the stream of numbers, separated by spaces\n(e.g., 1 3 2 1 2 3 4 3 1 2 3 1 5):");
        
        String[] input = sc.nextLine().trim().split("\\s+");
        sc.close();

        if (input.length == 0) {
            System.out.println("\nNo input provided. Exiting.");
            return;
        }

        // Parse numbers
        int[] nums = new int[input.length];
        int count = 0;
        for (String s : input) {
            try {
                nums[count++] = Integer.parseInt(s);
            } catch (Exception e) {}
        }
        nums = Arrays.copyOf(nums, count);

        if (nums.length == 0) {
            System.out.println("The stream contains no valid numbers.");
            return;
        }

        // Get distinct elements
        Set<Integer> distinct = new HashSet<>();
        for (int n : nums) distinct.add(n);
        int m = distinct.size();
        
        System.out.println("\nDistinct elements = " + distinct);
        System.out.println("Number of distinct elements (m) = " + m);

        // Method 1
        System.out.println("\n\n======== METHOD 1 ========");
        int R1 = process(nums, 6, 1, m);
        double est1 = Math.pow(2, R1);
        System.out.println("Maximum r(a): " + R1);
        System.out.println("Estimated Distinct Count = " + est1);

        // Method 2
        System.out.println("\n\n======== METHOD 2 ========");
        int[][] params = {{6,1,5}, {3,1,32}, {1,6,32}, {1,1,5}};
        double[] estimates = new double[4];
        
        for (int i = 0; i < 4; i++) {
            System.out.printf("\n--- Hash Function %d: H(x) = (%dx + %d) mod %d ---\n", 
                i+1, params[i][0], params[i][1], params[i][2]);
            estimates[i] = Math.pow(2, process(nums, params[i][0], params[i][1], params[i][2]));
        }

        // Results
        System.out.println("\n\n===== Final Results (Method 2) =====");
        System.out.println("Estimates from 4 hash functions: " + Arrays.toString(estimates));
        
        Arrays.sort(estimates);
        double avg = (estimates[0] + estimates[1] + estimates[2] + estimates[3]) / 4;
        double median = (estimates[1] + estimates[2]) / 2;
        
        System.out.println("Average Estimate: " + avg);
        System.out.println("Median Estimate: " + median);
        System.out.println("\n\n--- Code Execution Successful ---");
    }

    private static int process(int[] nums, int a, int b, int m) {
        System.out.printf("%-8s %-10s %-10s %-12s %s\n", "x", "ax+b", "Remainder", "Binary", "r(a)");
        System.out.println("----------------------------------------------------------------");
        
        int maxR = 0;
        for (int n : nums) {
            long axb = (long)a * n + b;
            int rem = (int)(axb % m);
            String bin = String.format("%5s", Integer.toBinaryString(rem)).replace(' ', '0');
            int r = (rem == 0) ? m : Integer.numberOfTrailingZeros(rem);
            maxR = Math.max(maxR, r);
            
            System.out.printf("%-8d %-10d %-10d %-12s %d\n", n, axb, rem, bin, r);
        }
        return maxR;
    }
}