package com.java.StockMarketApi.Service;

import com.java.StockMarketApi.Models.Item;
import com.java.StockMarketApi.Models.Notification;
import com.java.StockMarketApi.Models.NotificationType;
import com.java.StockMarketApi.Repository.NotificationRepository;
import com.java.StockMarketApi.Repository.ProductRepository;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;

    @Value("${stock.threshold.low:25}")
    private int lowStockThreshold;

    @Value("${stock.threshold.critical:5}")
    private int criticalStockThreshold;

    public ProductService(ProductRepository productRepository,
                          NotificationRepository notificationRepository) {
        this.productRepository = productRepository;
        this.notificationRepository = notificationRepository;
    }

    public class ProductServiceException extends RuntimeException {
        public ProductServiceException(String message) {
            super(message);
        }

        public ProductServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public enum StockNotificationStatus {
        NONE,
        LOW_QUANTITY,
        CRITICAL_QUANTITY
    }

    private StockNotificationStatus checkQuantityStatus(int currentQuantity, int initialQuantity) {
        if (currentQuantity < 0 || initialQuantity <= 0) {
            return StockNotificationStatus.NONE;
        }

        double percentage = ((double) currentQuantity / initialQuantity) * 100;

        if (percentage <= criticalStockThreshold) {
            return StockNotificationStatus.CRITICAL_QUANTITY;
        } else if (percentage <= lowStockThreshold) {
            return StockNotificationStatus.LOW_QUANTITY;
        }

        return StockNotificationStatus.NONE;
    }

    private void createNotification(Integer productId, String productName,
                                    NotificationType type, String message) {
        if (productId == null || StringUtils.isBlank(productName)) {
            return;
        }

        Notification notification = new Notification();
        notification.setProductId(productId);
        notification.setProductName(productName);
        notification.setNotificationType(type);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    @Transactional
    public Item addProduct(Item item) {
        validateItem(item);

        item.setInitialQuantity(item.getQuantity());
        return productRepository.save(item);
    }

    @Transactional
    public Item updateItem(Integer itemId, Item item) {
        validateItem(item);

        Item existingItem = productRepository.findById(itemId)
                .orElseThrow(() -> new ProductServiceException("Item not found"));

        int oldQuantity = existingItem.getQuantity();

        existingItem.setName(item.getName());
        existingItem.setQuantity(item.getQuantity());
        existingItem.setPrice(item.getPrice());

        Item updatedItem = productRepository.save(existingItem);

        if (item.getQuantity() != oldQuantity) {
            checkAndCreateNotification(updatedItem);
        }

        return updatedItem;
    }

    @Transactional
    public void deleteItem(Integer itemId) {
        if (!productRepository.existsById(itemId)) {
            throw new ProductServiceException("Item not found");
        }

        productRepository.deleteById(itemId);
    }
    @Transactional
    public StockNotificationStatus sellItem(Integer itemId, Integer quantity) {
        if (quantity <= 0) {
            throw new ProductServiceException("Quantity must be greater than zero");
        }

        Item item = productRepository.findById(itemId)
                .orElseThrow(() -> new ProductServiceException("Item not found"));

        if (item.getQuantity() < quantity) {
            throw new ProductServiceException("Insufficient stock");
        }

        int newQuantity = item.getQuantity() - quantity;
        item.setQuantity(newQuantity);

        Item updatedItem = productRepository.save(item);

        StockNotificationStatus status = checkQuantityStatus(newQuantity, item.getInitialQuantity());

        switch (status) {
            case LOW_QUANTITY -> createNotification(
                    itemId,
                    item.getName(),
                    NotificationType.LOW_STOCK,
                    String.format("Item \"%s\" quantity less than a quarter (%d%%)",
                            item.getName(),
                            Math.round(((double) newQuantity / item.getInitialQuantity()) * 100))
            );
            case CRITICAL_QUANTITY -> createNotification(
                    itemId,
                    item.getName(),
                    NotificationType.CRITICAL_STOCK,
                    String.format("Item \"%s\" quantity nearing zero (%d%%)",
                            item.getName(),
                            Math.round(((double) newQuantity / item.getInitialQuantity()) * 100))
            );
        }

        return status;
    }

    public List<Item> getAllItems() {
        return productRepository.findAll();
    }

    public Item getItemById(Integer itemId) {
        return productRepository.findById(itemId)
                .orElseThrow(() -> new ProductServiceException("Item not found"));
    }

    public List<Notification> getNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    private void validateItem(Item item) {
        if (item == null) {
            throw new ProductServiceException("Item cannot be null");
        }
        if (item.getQuantity() < 0) {
            throw new ProductServiceException("Current quantity cannot be negative");
        }
        if (item.getPrice() <= 0) {
            throw new ProductServiceException("Price must be greater than zero");
        }
        if (StringUtils.isBlank(item.getName())) {
            throw new ProductServiceException("Item name cannot be empty");
        }
    }

    private void checkAndCreateNotification(Item item) {
        StockNotificationStatus status = checkQuantityStatus(
                item.getQuantity(),
                item.getInitialQuantity()
        );

        switch (status) {
            case LOW_QUANTITY -> createNotification(
                    item.getId(),
                    item.getName(),
                    NotificationType.LOW_STOCK,
                    String.format("Item \"%s\" quantity less than a quarter (%d%%)",
                            item.getName(),
                            Math.round(((double) item.getQuantity() / item.getInitialQuantity()) * 100))
            );
            case CRITICAL_QUANTITY -> createNotification(
                    item.getId(),
                    item.getName(),
                    NotificationType.CRITICAL_STOCK,
                    String.format("Item \"%s\" quantity nearing zero (%d%%)",
                            item.getName(),
                            Math.round(((double) item.getQuantity() / item.getInitialQuantity()) * 100))
            );
        }
    }
}