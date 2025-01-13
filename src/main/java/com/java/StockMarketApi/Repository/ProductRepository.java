package com.java.StockMarketApi.Repository;

import com.java.StockMarketApi.Models.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Item, Integer> {
    Optional<Item> findById(Integer id);
}