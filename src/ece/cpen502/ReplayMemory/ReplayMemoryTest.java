//package ece.cpen502.ReplayMemory;
//
//import org.hamcrest.MatcherAssert;
//import org.hamcrest.core.IsEqual;
//import org.hamcrest.core.IsNot;
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.assertArrayEquals;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//class ReplayMemoryTest {
//
//    @Test
//    void addOne() {
//        ReplayMemory<Integer> memory = new ReplayMemory<Integer>(10);
//        int emptySize = memory.sizeOf();
//        assertEquals(0, emptySize);
//
//        // Add one item
//        memory.add(584);
//        int oneItemMemorySize = memory.sizeOf();
//        assertEquals(1, oneItemMemorySize);
//    }
//
//    @Test
//    void addThree() {
//        ReplayMemory<Integer> memory = new ReplayMemory<Integer>(10);
//        memory.add(243);
//        memory.add(853);
//        memory.add(123);
//        int memorySize = memory.sizeOf();
//        assertEquals(3, memorySize);
//    }
//
//    @Test
//    void sample5Of7() {
//        ReplayMemory<Integer> memory = new ReplayMemory<Integer>(10);
//        memory.add(243);
//        memory.add(853);
//        memory.add(123);
//        memory.add(765);
//        memory.add(555);
//        memory.add(333);
//        memory.add(666);
//        Object [] sample = memory.sample(5);
//        assertEquals(5, sample.length);
//    }
//
//    @Test
//    void exceedCapacity() {
//        int capacity = 3;
//        ReplayMemory<Integer> memory = new ReplayMemory<Integer>(capacity);
//        memory.add(243);
//        memory.add(853);
//        memory.add(123);
//        memory.add(765);
//        memory.add(555);
//        memory.add(333);
//        memory.add(666);
//        assertEquals(capacity, memory.sizeOf());
//    }
//
//    @Test
//    void confirmOldestRemoved() {
//        int capacity = 4;
//        int sampleSize = capacity - 1;
//        ReplayMemory<Integer> memory = new ReplayMemory<Integer>(capacity);
//        int a = 2534;
//        int b = 8531;
//        int c = 3983;
//        int d = 2137;
//        int e = 3873;
//        int f = 8876;
//        memory.add(a);
//        memory.add(b);
//        memory.add(c);
//        memory.add(d);
//        memory.add(e);
//        memory.add(f);
//        assertEquals(capacity, memory.sizeOf());
//
//        Object [] sample = memory.sample(sampleSize);
//        Integer [] expected = {d, e, f};
//
//        assertArrayEquals(expected, sample);
//    }
//
//    @Test
//    void sampleRequestTooLarge() {
//        boolean thrown = false;
//
//        try {
//            int capacity = 4;
//            int sampleSize = 5;
//            ReplayMemory<Integer> memory = new ReplayMemory<Integer>(capacity);
//            int a = 2534;
//            int b = 8531;
//            int c = 3983;
//            int d = 2137;
//            int e = 3873;
//            int f = 8876;
//            memory.add(a);
//            memory.add(b);
//            memory.add(c);
//            memory.add(d);
//            memory.add(e);
//            memory.add(f);
//
//            Object [] sample = memory.sample(sampleSize);
//            assertEquals(capacity, memory.sizeOf());
//        }
//        catch (ArrayIndexOutOfBoundsException e) {
//            thrown = true;
//        }
//        assertEquals(true, thrown);
//    }
//
//    @Test
//    void capacityEqualSampleSize() {
//        int capacity = 9;
//        int sampleSize = capacity;
//        ReplayMemory<Integer> memory = new ReplayMemory<Integer>(capacity);
//        int a = 1534;
//        int b = 2531;
//        int c = 3983;
//        int d = 4137;
//        int e = 5873;
//        int f = 6876;
//        int g = 7206;
//        int h = 8456;
//        int i = 9800;
//        memory.add(a);
//        memory.add(b);
//        memory.add(c);
//        memory.add(d);
//        memory.add(e);
//        memory.add(f);
//        memory.add(g);
//        memory.add(h);
//        memory.add(i);
//        assertEquals(capacity, memory.sizeOf());
//
//        Object [] sample = memory.sample(sampleSize);
//        Integer [] expected = {a, b, c, d, e, f, g, h, i};
//
//        assertArrayEquals(expected, sample);
//    }
//
//    @Test
//    void sample5Of9Shuffled() {
//        int capacity = 5;
//        int sampleSize = 5;
//        ReplayMemory<Integer> memory = new ReplayMemory<Integer>(capacity);
//        int a = 1534;
//        int b = 2531;
//        int c = 3983;
//        int d = 4003;
//        int e = 5976;
//        memory.add(a);
//        memory.add(b);
//        memory.add(c);
//        memory.add(d);
//        memory.add(e);
//
//        assertEquals(capacity, memory.sizeOf());
//        Object[] shuffledSample = memory.randomSample(sampleSize);
//        assertEquals(sampleSize, shuffledSample.length);
//        MatcherAssert.assertThat(shuffledSample, IsNot.not(IsEqual.equalTo(memory.sample(sampleSize))));
//    }
//
//    @Test
//    void addCustomType() {
//
//        class Experience {
//            int state;
//            int action;
//            double reward;
//            int nextState;
//        }
//
//        Experience a = new Experience();
//        a.state = 12;
//        a.action = 3;
//        a.reward = 0.82;
//        a.nextState = 13;
//
//        ReplayMemory<Experience> memory = new ReplayMemory<Experience>(10);
//        int emptySize = memory.sizeOf();
//        assertEquals(0, emptySize);
//
//        // Add one item
//        memory.add(a);
//        int oneItemMemorySize = memory.sizeOf();
//        assertEquals(1, oneItemMemorySize);
//    }
//
//
//}