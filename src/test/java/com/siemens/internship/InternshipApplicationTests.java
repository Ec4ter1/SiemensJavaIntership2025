package com.siemens.internship;

import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InternshipApplicationTests {
	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	@Test
	void contextLoads() {
		//pt spring
	}

	@BeforeEach
	void setup(){
		itemRepository.deleteAll();//clean database before testing
		itemRepository.save(new Item(null, "Item1", "D1", "satus","test1@ex.com"));
		itemRepository.save(new Item(null, "Item2", "D2", "satus","test2@ex.com"));

	}

	@Test
	void testProcessItemsAsync() throws Exception{
		//number of items before processing
		int initialCount = itemRepository.findAll().size();
		//call async processing
		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		//wait for the result
		List<Item> items = future.get(10, TimeUnit.SECONDS);
		assertEquals(initialCount, items.size(), "Number of processed items should be equal to initial number of processed items");
		//check if all the items were processed
		for (Item item : items) {
			assertEquals("PROCESSED", item.getStatus(), "Item status should be PROCESSED");
		}
		//check in repository
		List<Item> allItems = itemRepository.findAll();
		for(Item item : allItems) {
			assertEquals("PROCESSED", item.getStatus(), "Item status should be PROCESSED in repo");
		}
	}

	@Test
	void testprocessItemsAsyncConcurrency() throws Exception{
		//add more items to test
		for (int i = 0; i < 50; i++) {
			itemRepository.save(new Item(null, "ConcurrentItem" + i, "Testing concurrency", "NEW", "concurrent" + i + "test@example.com"));
		}
		int total = itemRepository.findAll().size();
		//call for async processing
		long startTime = System.currentTimeMillis();
		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> processedItems = future.get(30, TimeUnit.SECONDS);
		long endTime = System.currentTimeMillis();

		long executionTime = endTime - startTime;

		//check if all items were processed
		assertEquals(total, processedItems.size(), "All items should be processed");
		//check performance
		//if its sequencial it should be total *100ms
		//if its paralel it should be less
		long sequentialTime = total * 100L;
		assertTrue(executionTime < sequentialTime, "Parallel processing should be faster than sequential processing");
		System.out.println("Parallel execution time: " + executionTime + "ms");
		System.out.println("Estimated sequential time: " + sequentialTime + "ms");
		System.out.println("Speedup: " + (double) sequentialTime / executionTime + "x");
	}

	@Test
	void testFindById() {
		//save an item
		Item savedItem = itemRepository.save(new Item(null, "TestItem", "Test Description", "NEW", "test@example.com"));

		// Find the item by ID
		assertTrue(itemService.findById(savedItem.getId()).isPresent(), "The item should be found by ID");

		//verify the retrieved data matches
		Item foundItem = itemService.findById(savedItem.getId()).get();
		assertEquals(savedItem.getName(), foundItem.getName(), "Item name should match");
		assertEquals(savedItem.getDescription(), foundItem.getDescription(), "Item description should match");
		assertEquals(savedItem.getEmail(), foundItem.getEmail(), "Item email should match");
	}

	@Test
	void testFindAll() {
		//retrieve all items using the service
		List<Item> allItems = itemService.findAll();
		//Check if the count matches repository count
		assertEquals(itemRepository.count(), allItems.size(), "findAll should return all items in the repository");
		//ensure all repository items are in the returned list
		List<Item> repoItems = itemRepository.findAll();
		assertTrue(allItems.containsAll(repoItems), "Returned list should include all items from the repository");
	}

	@Test
	void testSave() {
		//create a new item
		Item newItem = new Item(null, "New Item", "New Description", "NEW", "new@example.com");

		//save it using the service
		Item savedItem = itemService.save(newItem);

		//check that it was assigned an ID
		assertNotNull(savedItem.getId(), "Saved item should have a non-null ID");

		//retrieve from repository
		Optional<Item> foundItem = itemRepository.findById(savedItem.getId());
		assertTrue(foundItem.isPresent(), "Saved item should exist in the repository");

		//Verify the data matches
		assertEquals(newItem.getName(), foundItem.get().getName(), "Item name should be saved correctly");
		assertEquals(newItem.getDescription(), foundItem.get().getDescription(), "Item description should be saved correctly");
		assertEquals(newItem.getStatus(), foundItem.get().getStatus(), "Item status should be saved correctly");
		assertEquals(newItem.getEmail(), foundItem.get().getEmail(), "Item email should be saved correctly");
	}

	@Test
	void testUpdate() {
		//Save an initial item
		Item item = itemRepository.save(new Item(null, "Original", "Original Desc", "NEW", "original@example.com"));

		//Modify its fields
		item.setName("Updated");
		item.setDescription("Updated Desc");
		item.setStatus("UPDATED");
		item.setEmail("updated@example.com");

		//Save the updated item
		Item updatedItem = itemService.save(item);
		//ID should remain the same

		assertEquals(item.getId(), updatedItem.getId(), "ID should remain unchanged after update");
		//Verify the updates were saved

		Item foundItem = itemRepository.findById(item.getId()).get();
		assertEquals("Updated", foundItem.getName(), "Item name should be updated");
		assertEquals("Updated Desc", foundItem.getDescription(), "Item description should be updated");
		assertEquals("UPDATED", foundItem.getStatus(), "Item status should be updated");
		assertEquals("updated@example.com", foundItem.getEmail(), "Item email should be updated");
	}

	@Test
	void testDeleteById() {
		//save an item
		Item item = itemRepository.save(new Item(null, "ToDelete", "To be deleted", "NEW", "delete@example.com"));
		Long id = item.getId();
		//verify it exists
		assertTrue(itemRepository.existsById(id), "Item should exist before deletion");
		//delete the item
		itemService.deleteById(id);
		//verify it no longer exists
		assertFalse(itemRepository.existsById(id), "Item should no longer exist after deletion");
	}
}
