package com.flowops.warehouse.service;

import com.flowops.warehouse.client.InventoryClient;
import com.flowops.warehouse.model.WarehouseStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock  InventoryClient inventoryClient;
    @InjectMocks RoutingService routingService;

    private WarehouseStock stock(String warehouseId, int qty) {
        WarehouseStock s = new WarehouseStock();
        s.setWarehouseId(warehouseId);
        s.setSku("SKU-001");
        s.setQuantity(qty);
        return s;
    }

    @Test
    @DisplayName("Selects warehouse with highest stock that can fulfill the order")
    void selectsHighestStockWarehouse() {
        when(inventoryClient.getStockBySku("SKU-001")).thenReturn(List.of(
            stock("WH-ATLANTA", 150),
            stock("WH-DALLAS",  200),
            stock("WH-CHICAGO", 50)
        ));
        Optional<String> result = routingService.findBestWarehouse("SKU-001", 10);
        assertThat(result).isPresent().hasValue("WH-DALLAS");
    }

    @Test
    @DisplayName("Skips warehouses with insufficient stock")
    void skipsInsufficientStock() {
        when(inventoryClient.getStockBySku("SKU-001")).thenReturn(List.of(
            stock("WH-ATLANTA", 5),
            stock("WH-DALLAS",  3),
            stock("WH-CHICAGO", 100)
        ));
        Optional<String> result = routingService.findBestWarehouse("SKU-001", 10);
        assertThat(result).isPresent().hasValue("WH-CHICAGO");
    }

    @Test
    @DisplayName("Returns empty when no warehouse has enough stock")
    void returnsEmptyWhenNoStock() {
        when(inventoryClient.getStockBySku("SKU-001")).thenReturn(List.of(
            stock("WH-ATLANTA", 2),
            stock("WH-DALLAS",  1)
        ));
        Optional<String> result = routingService.findBestWarehouse("SKU-001", 10);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when stock list is empty")
    void returnsEmptyWhenNoWarehouses() {
        when(inventoryClient.getStockBySku("SKU-001")).thenReturn(List.of());
        Optional<String> result = routingService.findBestWarehouse("SKU-001", 5);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Tie-breaks consistently — reversed ID comparator picks alphabetically first warehouse")
    void tieBreaksConsistently() {
        when(inventoryClient.getStockBySku("SKU-001")).thenReturn(List.of(
            stock("WH-ATLANTA", 100),
            stock("WH-DALLAS",  100)
        ));
        Optional<String> result = routingService.findBestWarehouse("SKU-001", 10);
        assertThat(result).isPresent();
        // Secondary sort uses .reversed() on natural order, so max() picks the
        // alphabetically first ID (reversed makes "A" > "D", so WH-ATLANTA wins)
        assertThat(result.get()).isEqualTo("WH-ATLANTA");
    }

    @Test
    @DisplayName("Handles exact stock match — warehouse with exactly required qty is selected")
    void exactStockMatch() {
        when(inventoryClient.getStockBySku("SKU-001")).thenReturn(List.of(
            stock("WH-ATLANTA", 10),
            stock("WH-DALLAS",  9)
        ));
        Optional<String> result = routingService.findBestWarehouse("SKU-001", 10);
        assertThat(result).isPresent().hasValue("WH-ATLANTA");
    }
}
