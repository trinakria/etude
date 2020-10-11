package org.trinakria.etude.problem1;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * In the classic problem FizzBuzz , you are told to print the numbers from 1 to n.
 * However, when the number is divisible by 3, print "Fizz". <br>
 * When it is divisible by 5, print "Buzz". <br>
 * When it is divisible by 3 and 5, print "FizzBuzz". <br>
 * This is Multi-threaded version of FizzBuzz with four threads. <br>
 * One thread checks for divisibility of 3 and prints "Fizz". <br>
 * Another thread is responsible for divisibility of 5 and prints "Buzz"<br>
 * A third thread is responsible for divisibility of 3 and 5 and prints "FizzBuzz"<br>
 * A four th thread does the numbers.
 * <p>
 * The problem is about coordination of threads and it is solved using a {@link Phaser} synchronizer.
 * The behavior is similar to a {@link java.util.concurrent.CountDownLatch} but it is more flexible as it allows
 * threads to repeatedly perform actions for a given number of iterations which is the case of this game.
 * </p>
 * <p>
 *     My first attempt used CountDownLatch an it was inspired by an example in Item81 of Effective Java 3rd Edition<br>
 *     I realized soon that CountDownLatch is better suited to sync a 1 off action across a set of Threads.
 *     I started looking into other synchronizers in the java.util.concurrent package and I stumbled upon Phaser.<br>
 *     I reworked the problem using it and I was satisfied with the result.
 *     I was reminded by another great piece of advice contained in Item59 of Effective Java: Know and use the libraries
 * </p>
 */
public class ConcurrentFizzBuzz {

    private static final AtomicLong nextNumber = new AtomicLong();
    private static final int NUMBER_OF_PLAYERS = 3;
    private static final int FIRST_PLAYER = 1;
    private static final int SECOND_PLAYER = 2;
    private static final int THIRD_PLAYER = 3;
    private final ExecutorService executorService;
    private final long lastNumber;
    private final Map<Integer, Consumer<Long>> actions;

    public ConcurrentFizzBuzz(long lastNumber) {

        //this is a good choice for this program. No configuration required and it works with sensible defaults
        //It won't be a good choice for a heavily load server!
        //In a cached thread pool submitted tasks are not queued but immediately handed off to a thread for execution
        //If no threads are available, a new one is created. A fixed thread pool would be better.
        this.executorService = Executors.newCachedThreadPool();
        this.lastNumber = lastNumber;
        this.actions = Map.of(
                FIRST_PLAYER, (number) -> {
                    if (divisibleBy(number, 3)) fizz(number);
                },
                SECOND_PLAYER, (number) -> {
                    if (divisibleBy(number, 5)) buzz(number);
                },
                THIRD_PLAYER, (number) -> {
                    if (divisibleBy(number, 3) && divisibleBy(number, 5)) fizzBuzz(number);
                }
        );
    }

    public void play() {

        final Phaser phaser = new Phaser(1) {
            protected boolean onAdvance(int phase, int registeredParties) {
                return phase >= lastNumber || registeredParties == 0;
            }
        };

        IntStream.range(1, NUMBER_OF_PLAYERS + 1).forEach(i -> executorService.execute(() -> {
            phaser.register();
            long last = nextNumber.get();
            do {
                long current = nextNumber.get();
                //counting is slower than reading the current number which causes players to potentially exclaim Fizz, Buzz
                //multiple time for the same number. To avoid that skip consecutive reads of the same number
                if (current != last) {
                    actions.get(i).accept(current);
                }
                last = current;
                phaser.arriveAndAwaitAdvance();
            } while (!phaser.isTerminated());
        }));

        long startNanos = System.nanoTime();
        System.out.println("Ready Set Go!!!");
        while (!phaser.isTerminated()) {
            count(nextNumber.getAndIncrement());
            phaser.arriveAndAwaitAdvance();
        }
        System.out.println("Game over!" + "Took: " + ((System.nanoTime() - startNanos) / 1000000) + "ms");

        executorService.shutdown();
    }

    public static void main(String[] args) {
        new ConcurrentFizzBuzz(50).play();
    }

    private void count(long number) {
        System.out.println(Thread.currentThread().getName() + " - Next number: " + number);
    }

    private boolean divisibleBy(Long number, int i) {
        return number % i == 0;
    }

    private void fizzBuzz(long number) {
        System.out.println(Thread.currentThread().getName() + " - Got " + number + " Fizz Buzz! ");
    }

    private void buzz(long number) {
        System.out.println(Thread.currentThread().getName() + " - Got " + number + " Buzz!");
    }

    private void fizz(long number) {
        System.out.println(Thread.currentThread().getName() + " - Got " + number + " Fizz!");
    }

}
