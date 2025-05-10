package com.siemens.internship.service;

import com.siemens.internship.model.Item;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IService {
    List<Item> findAll();
    Optional<Item> findById(Long id);
    Item save(Item item);
    void deleteById(Long id);
    CompletableFuture<List<Item>> processItemsAsync();  // Asynchronous processing defined in the interface
}
