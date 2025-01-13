package com.java.StockMarketApi.Models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(name = "quantity")
    private Integer quantity;

    private Double price;

    @Column(name = "initial_quantity")
    private Integer initialQuantity;
}
