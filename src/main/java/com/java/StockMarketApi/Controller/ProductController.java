package com.java.StockMarketApi.Controller;

import com.java.StockMarketApi.Models.Item;
import com.java.StockMarketApi.Models.Notification;
import com.java.StockMarketApi.Models.NotificationType;
import com.java.StockMarketApi.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    private Map<String, Object> createResponse(Object data, String message, boolean success) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        return response;
    }

    public Map<String, Object> getProducts() {
        try {
            List<Item> items = productService.getAllItems();
            return createResponse(items, "Items retrieved successfully", true);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    public Map<String, Object> getProduct(Integer id) {
        try {
            Item item = productService.getItemById(id);
            return createResponse(item, "Item retrieved successfully", true);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    public Map<String, Object> createProduct(Item item) {
        try {
            Item savedItem = productService.addProduct(item);
            return createResponse(savedItem, "Item created successfully", true);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    public Map<String, Object> updateProduct(Integer id, Item item) {
        try {
            item.setId(id);
            Item updatedItem = productService.updateItem(id, item);
            return createResponse(updatedItem, "Item updated successfully", true);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    public Map<String, Object> deleteProduct(Integer id) {
        try {
            productService.deleteItem(id);
            return createResponse(null, "Item deleted successfully", true);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }
    public Map<String, Object> sellProduct(Integer id, Map<String, Integer> request) {
//        try {
            // Add null check
            if (request == null || request.get("quantity") == null) {
                return Map.of(
                        "success", false,
                        "message", "Invalid request: quantity is required",
                        "data", null
                );
            }

            Integer quantity = Integer.parseInt(String.valueOf(request.get("quantity")));
            ProductService.StockNotificationStatus status = productService.sellItem(id, quantity);

            // Create response using new HashMap instead of Map.of()
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (status == ProductService.StockNotificationStatus.LOW_QUANTITY ||
                    status == ProductService.StockNotificationStatus.CRITICAL_QUANTITY) {
                response.put("message", "Sale completed successfully. Warning: Stock quantity is low");
                response.put("data", Map.of("notification", "Low stock warning"));
            } else {
                response.put("message", "Sale processed successfully");
                response.put("data", Map.of("status", "NONE"));
            }

            return response;
//        } catch (Exception e) {
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("success", false);
//            errorResponse.put("message", e);
//            errorResponse.put("data", null);
//            return errorResponse;
//        }
    }


    public Map<String, Object> getNotifications() {
        try {
            List<Notification> notifications = productService.getNotifications();

            // Transform the notifications to match desired format
            List<Map<String, Object>> formattedNotifications = notifications.stream()
                    .map(notification -> {
                        Map<String, Object> formatted = new HashMap<>();
                        formatted.put("id", notification.getId());
                        formatted.put("product_id", notification.getProductId());
                        formatted.put("product_name", notification.getProductName());
                        formatted.put("type", formatNotificationType(notification.getNotificationType()));
                        formatted.put("message", notification.getMessage());
                        formatted.put("created_at", formatDateTime(notification.getCreatedAt()));
                        return formatted;
                    })
                    .collect(Collectors.toList());

            return createResponse(formattedNotifications, "Notifications retrieved successfully", true);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    private String formatNotificationType(NotificationType type) {
        switch (type) {
            case CRITICAL_STOCK:
                return "Critical Stock";
            case LOW_STOCK:
                return "Low Stock";
            default:
                return type.toString();
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a");
        return dateTime.format(formatter);
    }
}