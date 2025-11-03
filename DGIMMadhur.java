import java.util.*;

public class DGIMMadhur {
    public static record Bucket(int size, long rightTs) {
        @Override public String toString() { return "{size=" + size + ", rightTs=" + rightTs + "}"; }
    }
    public static class DGIM {
        private final int windowSizeN, maxBucketsPerSize;
        private long currentTs = 0;
        private final Deque<Bucket> buckets = new ArrayDeque<>();
        private final boolean keepExactBuffer;
        private final int[] exactBuffer;
        private int exactBufferCount = 0;
        public DGIM(int windowSizeN, int maxBucketsPerSize, boolean keepExactBuffer) {
            if (windowSizeN <= 0) throw new IllegalArgumentException("windowSizeN > 0");
            if (maxBucketsPerSize < 2) throw new IllegalArgumentException("maxBucketsPerSize >= 2");
            this.windowSizeN = windowSizeN; this.maxBucketsPerSize = maxBucketsPerSize;
            this.keepExactBuffer = keepExactBuffer;
            this.exactBuffer = keepExactBuffer ? new int[windowSizeN] : null;
        }
        public void addBit(int bit) {
            if (bit != 0 && bit != 1) throw new IllegalArgumentException("bit must be 0 or 1");
            currentTs++;
            if (keepExactBuffer) {
                int idx = (int) ((currentTs - 1) % windowSizeN);
                exactBuffer[idx] = bit;
                if (exactBufferCount < windowSizeN) exactBufferCount++;
            }
            expireOldBuckets();
            if (bit == 1) {
                buckets.addFirst(new Bucket(1, currentTs));
                enforceMergeRules();
            }
        }
        private void expireOldBuckets() {
            long earliestTs = Math.max(1, currentTs - windowSizeN + 1);
            while (!buckets.isEmpty() && buckets.peekLast().rightTs() < earliestTs) buckets.pollLast();
        }
        private void enforceMergeRules() {
            boolean merged;
            do {
                merged = false;
                Map<Integer, Integer> countBySize = new HashMap<>();
                for (Bucket b : buckets) countBySize.put(b.size(), countBySize.getOrDefault(b.size(), 0) + 1);
                List<Integer> sizes = new ArrayList<>(countBySize.keySet());
                Collections.sort(sizes);
                for (int size : sizes) {
                    if (countBySize.get(size) > maxBucketsPerSize) {
                        mergeTwoOldestOfSize(size);
                        merged = true; break;
                    }
                }
            } while (merged);
        }
        private void mergeTwoOldestOfSize(int size) {
            List<Bucket> list = new ArrayList<>(buckets);
            Bucket oldest = null, secondOldest = null;
            for (int i = list.size() - 1; i >= 0; i--) {
                Bucket b = list.get(i);
                if (b.size() == size) {
                    if (oldest == null) oldest = b;
                    else { secondOldest = b; break; }
                }
            }
            if (oldest == null || secondOldest == null) return;
            removeFirstIdentity(secondOldest); removeFirstIdentity(oldest);
            long newRight = Math.max(oldest.rightTs(), secondOldest.rightTs());
            insertBucketByRightTs(new Bucket(size * 2, newRight));
        }
        private void removeFirstIdentity(Bucket target) {
            Iterator<Bucket> it = buckets.iterator();
            while (it.hasNext()) if (it.next() == target) { it.remove(); return; }
        }
        private void insertBucketByRightTs(Bucket b) {
            if (buckets.isEmpty()) { buckets.add(b); return; }
            List<Bucket> temp = new ArrayList<>(buckets);
            int pos = 0;
            while (pos < temp.size() && temp.get(pos).rightTs() > b.rightTs()) pos++;
            temp.add(pos, b);
            buckets.clear(); buckets.addAll(temp);
        }
        public int effectiveWindow() { return (int) Math.min(currentTs, windowSizeN); }
        public long currentTs() { return currentTs; }
        public Deque<Bucket> buckets() { return buckets; }
        public int[] snapshotExactWindow() {
            if (!keepExactBuffer) return null;
            int len = effectiveWindow(); int[] out = new int[len];
            for (int i = 0; i < len; i++) {
                long t = currentTs - len + 1 + i;
                int idx = (int) ((t - 1) % windowSizeN); out[i] = exactBuffer[idx];
            }
            return out;
        }
    }
    private static List<Integer> parseBits(String s) {
        List<Integer> bits = new ArrayList<>();
        if (s == null) return bits;
        s = s.trim(); if (s.isEmpty()) return bits;
        if (s.matches("[01]+")) {
            for (char c : s.toCharArray()) bits.add(c == '1' ? 1 : 0);
        } else {
            s = s.replace(",", " ");
            for (String tok : s.trim().split("\\s+")) {
                if (tok.isEmpty()) continue;
                if (!tok.matches("[01]")) throw new IllegalArgumentException("Invalid bit:" + tok);
                bits.add(Integer.parseInt(tok));
            }
        }
        return bits;
    }
    public static void printBucketsChronologically(DGIM dgim) {
        System.out.println("### Stream Segments View (Oldest -> Newest) ###");
        if (dgim.buckets().isEmpty()) { System.out.println("(No buckets to display)"); return; }
        int[] exactWindow = dgim.snapshotExactWindow();
        if (exactWindow == null) { System.out.println("Cannot display bits..."); return; }
        long windowStartTs = dgim.currentTs() - dgim.effectiveWindow() + 1;
        List<Bucket> bucketList = new ArrayList<>(dgim.buckets());
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < bucketList.size(); i++) {
            Bucket b = bucketList.get(i);
            long bucketEndTs = b.rightTs();
            long bucketStartTs = (i + 1 < bucketList.size()) ? bucketList.get(i + 1).rightTs() + 1 : windowStartTs;
            List<String> bitsInSegment = new ArrayList<>();
            for (long ts = bucketStartTs; ts <= bucketEndTs; ts++) {
                int bitIndex = (int) (ts - windowStartTs);
                if (bitIndex >= 0 && bitIndex < exactWindow.length) bitsInSegment.add(String.valueOf(exactWindow[bitIndex]));
            }
            segments.add("Size " + b.size() + ": [" + String.join(",", bitsInSegment) + "]");
        }
        Collections.reverse(segments);
        System.out.println(String.join(" | ", segments));
    }
    public static void main(final String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Enter the window size (N):");
            int N = Integer.parseInt(sc.nextLine().trim());
            System.out.println("Enter the initial input stream (will be read from left to right):");
            List<Integer> initBits = parseBits(sc.nextLine());
            System.out.println("Enter the new input stream (will be read from LEFT to RIGHT):");
            List<Integer> newBits = parseBits(sc.nextLine());
            DGIM dgim = new DGIM(N, 2, true);
            for (int b : initBits) dgim.addBit(b);
            for (int b : newBits) dgim.addBit(b);
            System.out.println("\n========== DGIM Final State ==========");
            int[] finalWindow = dgim.snapshotExactWindow();
            if (finalWindow != null) {
                System.out.println("Combined Stream in Final Window (Oldest -> Newest):");
                System.out.println(Arrays.toString(finalWindow)); System.out.println();
            }
            printBucketsChronologically(dgim);
        }
    }
}