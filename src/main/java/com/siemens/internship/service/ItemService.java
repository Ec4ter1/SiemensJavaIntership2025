package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService implements IService{
    @Autowired
    private ItemRepository itemRepository;
    //we declare thread pool as final for preventic accidental modifications
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    //thread safe for processed items
    private final List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());
    //contor thread safe
    private final AtomicInteger processedCount = new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    /**
     * Asynchronously processes items from the repository.
     *
     * Corrected implementation to address the following issues:
     * thread-safety for shared state (processedItems and processedCount)
     * ensures all async operations complete before returning the result
     * proper error handling
     * avoids blocking the main thread
     * efficient use of system resources
     *
     * @return CompletableFuture with the list of processed items
     */
    //before the async method was using List<Items> instead of CompletableFuture<List<Item>>
    //that made the method return the result before the end of the processing
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        //clear the processed items list
        processedItems.clear();
        processedCount.set(0);

        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Item>> futures = itemIds.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> {
                try{
                    Thread.sleep(100);

                    Item item = itemRepository.findById(id).orElse(null);
                    if (item == null) {
                        return null;
                    }
                    //increment the contor
                    processedCount.incrementAndGet();
                    //update the status
                    item.setStatus("PROCESSED");
                    return itemRepository.save(item);
                } catch (InterruptedException e) { //proper error handling
                    Thread.currentThread().interrupt();
                    throw new CompletionException("Procesare intrerupta", e);} //proper error propagation
                catch(Exception e){
                    throw new CompletionException("Eroare la procesarea item-ului cu id: " + id, e); //proper error prpagation
                }
            }, executor)).collect(Collectors.toList());
        //this method returned processedItems without waiting for the asyncron operations to finish
        //the errors were just caught and printed without propagation to the caller
//        for (Long id : itemIds) {
//            CompletableFuture.runAsync(() -> {
//                try {
//                    Thread.sleep(100);
//
//                    Item item = itemRepository.findById(id).orElse(null);
//                    if (item == null) {
//                        return;
//                    }
//
//                    processedCount++;
//
//                    item.setStatus("PROCESSED");
//                    itemRepository.save(item);
//                    processedItems.add(item);
//
//                } catch (InterruptedException e) {
//                    System.out.println("Error: " + e.getMessage());
//                }
//            }, executor);
//        }

        //we combine all completable futures into one
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    //collect the results
                    List<Item> result = futures.stream().map(CompletableFuture::join).filter(item -> item!=null).collect(Collectors.toList());
                    //update the processed items list
                    processedItems.addAll(result);
                    return processedItems;
                })
                .exceptionally(ex -> {
                    System.out.println("Error: " + ex.getMessage());
                    System.err.println("Error: " + ex.getMessage());
                    ex.printStackTrace();
                    return new ArrayList<>();
                });

    }

    //for closing correctly the executor
    @PreDestroy
    public void destroy() {
        executor.shutdown(); //stop accepting new tasks
        try {
            if(executor.awaitTermination(5, TimeUnit.SECONDS)) //wait for the existing tasks to finish
            {
                executor.shutdownNow(); //force shut down
            }
        }catch (InterruptedException e){
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}

