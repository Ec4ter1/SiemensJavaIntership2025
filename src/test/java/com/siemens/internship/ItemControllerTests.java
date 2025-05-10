package com.siemens.internship;

import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ItemControllerTests {
    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemController itemController;

    @BeforeEach
    void cleanDatabase() {
        itemRepository.deleteAll();
    }

    @Test
    void testGetAllItems() {
        //get all items from the controller
        ResponseEntity<List<Item>> response = itemController.getAllItems();

        //check if the response status is OK
        assertEquals(HttpStatus.OK, response.getStatusCode(), "The status should be OK");

        //ceck that the response body contains items from the repository
        List<Item> items = response.getBody();
        assertNotNull(items, "The response body should not be null");
        assertEquals(itemRepository.count(), items.size(), "The number of items should match the repository count");
    }


    @Test
    void testCreateItem() {
        //create a new item
        Item newItem = new Item(null, "Controller Test", "Created via controller", "NEW", "controller@example.com");

        //mock a BindingResult with no errors
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        //call the controller to create the item
        ResponseEntity<Item> response = itemController.createItem(newItem, bindingResult);

        //check that the status is CREATED
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "The status should be CREATED");

        //check that the item was returned and saved
        Item createdItem = response.getBody();
        assertNotNull(createdItem, "The response body should not be null");
        assertNotNull(createdItem.getId(), "The created item should have an ID");
        assertEquals(newItem.getName(), createdItem.getName(), "The item name should match the input");

        //check that the item was saved in the repository
        assertTrue(itemRepository.existsById(createdItem.getId()), "The item should exist in the repository");
    }

    @Test
    void testCreateItemWithValidationErrors() {
        //create an item
        Item item = new Item(null, "Invalid", "Invalid item", "NEW", "invalid@example.com");

        //mock a BindingResult with errors
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        //call the controller to create the item
        ResponseEntity<Item> response = itemController.createItem(item, bindingResult);

        //check if the status is BAD_REQUEST
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "The status should be BAD_REQUEST for invalid input");

        // Check that the response body is null
        assertNull(response.getBody(), "The response body should be null for invalid requests");
    }

    @Test
    void testGetItemById() {
        //save a test item
        Item item = itemRepository.save(new Item(null, "GetById", "Get by ID test", "NEW", "getbyid@example.com"));

        //call the controller to get the item by ID
        ResponseEntity<Item> response = itemController.getItemById(item.getId());

        // Check that the status is OK
        assertEquals(HttpStatus.OK, response.getStatusCode(), "The status should be OK");

        //check that the correct item was returned
        Item foundItem = response.getBody();
        assertNotNull(foundItem, "The response body should not be null");
        assertEquals(item.getId(), foundItem.getId(), "The item ID should match");
        assertEquals(item.getName(), foundItem.getName(), "The item name should match");
    }

    @Test
    void testGetItemByIdNotFound() {
        //call the controller with a non-existent ID
        ResponseEntity<Item> response = itemController.getItemById(99999L);

        // Check if the status is NOT_FOUND
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "The status should be NOT_FOUND for non-existent ID");

        // Check that the response body is null
        assertNull(response.getBody(), "The response body should be null for a non-existent item");
    }

    @Test
    void testUpdateItem() {
        //save an initial item
        Item originalItem = itemRepository.save(new Item(null, "Original", "Original description", "NEW", "original@example.com"));

        //create an updated item with new values
        Item updatedItem = new Item(null, "Updated via Controller", "Updated description", "UPDATED", "updated@example.com");

        //call the controller to update the item
        ResponseEntity<Item> response = itemController.updateItem(originalItem.getId(), updatedItem);

        //check that the status is OK
        assertEquals(HttpStatus.OK, response.getStatusCode(), "The status should be OK");

        // Check that the updated item was returned
        Item returnedItem = response.getBody();
        assertNotNull(returnedItem, "The response body should not be null");
        assertEquals(originalItem.getId(), returnedItem.getId(), "The item ID should remain the same");
        assertEquals(updatedItem.getName(), returnedItem.getName(), "The item name should be updated");
        assertEquals(updatedItem.getDescription(), returnedItem.getDescription(), "The item description should be updated");

        // Check that the item was updated in the repository
        Item itemInRepo = itemRepository.findById(originalItem.getId()).get();
        assertEquals(updatedItem.getName(), itemInRepo.getName(), "The item name should be updated in the repository");
    }

    @Test
    void testUpdateItemNotFound() {
        //create an item to update
        Item updateItem = new Item(null, "Won't Update", "This item doesn't exist", "NEW", "wontupdate@example.com");

        //call the controller with a non-existent ID
        ResponseEntity<Item> response = itemController.updateItem(99999L, updateItem);

        //check that the status is NOT_FOUND
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "The status should be NOT_FOUND for non-existent ID");

        //check that the response body is null
        assertNull(response.getBody(), "The response body should be null for non-existent item");
    }

    @Test
    void testDeleteItem() {
        //save an item
        Item item = itemRepository.save(new Item(null, "To Delete", "This will be deleted", "NEW", "todelete@example.com"));
        Long id = item.getId();

        //verify the item exists
        assertTrue(itemRepository.existsById(id), "The item should exist before deletion");

        // Call the controller to delete the item
        ResponseEntity<Void> response = itemController.deleteItem(id);

        // Check that the status is NO_CONTENT
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "The status should be NO_CONTENT");

        // Verify the item was deleted
        assertFalse(itemRepository.existsById(id), "The item should no longer exist after deletion");
    }

    @Test
    void testProcessItems() throws InterruptedException, ExecutionException, TimeoutException {
        // Call the controller to process items
        ResponseEntity<List<Item>> response = itemController.processItems();

        // Check that the status is OK
        assertEquals(HttpStatus.OK, response.getStatusCode(), "The status should be OK");

        // Check that the processed items were returned
        List<Item> processedItems = response.getBody();
        assertNotNull(processedItems, "The response body should not be null");

        // Verify all items are marked as PROCESSED
        for (Item item : processedItems) {
            assertEquals("PROCESSED", item.getStatus(), "All items should have status PROCESSED");
        }

        // Check that the repository also reflects the processed status
        List<Item> itemsInRepo = itemRepository.findAll();
        for (Item item : itemsInRepo) {
            assertEquals("PROCESSED", item.getStatus(), "All items in the repository should be processed");
        }
    }

    @Test
    void testProcessItemsWithError() throws InterruptedException, ExecutionException, TimeoutException {
        // Create a mock for ItemService
        ItemService mockItemService = mock(ItemService.class);
        ItemController controller = new ItemController();
        try {
            var field = ItemController.class.getDeclaredField("itemService");
            field.setAccessible(true);
            field.set(controller, mockItemService);
        } catch (Exception e) {
            fail("Failed to inject mock ItemService: " + e.getMessage());
        }

        //configure the mock to throw an exception
        CompletableFuture<List<Item>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Test exception"));
        when(mockItemService.processItemsAsync()).thenReturn(failedFuture);

        ResponseEntity<List<Item>> response = controller.processItems();

        //check that the status is INTERNAL_SERVER_ERROR
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "The status should be INTERNAL_SERVER_ERROR on failure");

        //check that the body is null
        assertNull(response.getBody(), "The response body should be null on error");

        // Verify the async method was called
        verify(mockItemService, times(1)).processItemsAsync();
    }

    @Test
    void testErrorHandlingInProcessItemsAsync() throws InterruptedException, ExecutionException {
        // Save a valid item
        Item validItem = itemRepository.save(new Item(null, "Valid", "Valid item", "NEW", "valid@example.com"));

        // Call the async processor
        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get();

        // The result should not be null
        assertNotNull(result, "The result should not be null even with potential errors");

        // Check that the item was processed
        Item processedItem = itemRepository.findById(validItem.getId()).get();
        assertEquals("PROCESSED", processedItem.getStatus(), "The valid item should be processed");
    }
}
