package com.flowops.inventory.service;

import com.flowops.inventory.model.Inventory;
import com.flowops.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock  InventoryRepository repo;
    @InjectMocks InventoryService service;

    @Test
    @DisplayName("isInStock returns true when total stock exceeds requested quantity")
    void isInStock_trueWhenSufficientStock() {
        when(repo.totalStockBySku("SKU-001")).thenReturn(350);

        assertThat(service.isInStock("SKU-001", 100)).isTrue();
    }

    @Test
    @DisplayName("isInStock returns true on exact stock match")
    void isInStock_trueForExactMatch() {
        when(repo.totalStockBySku("SKU-001")).thenReturn(50);

        assertThat(service.isInStock("SKU-001", 50)).isTrue();
    }

    @Test
    @DisplayName("isInStock returns false when stock is insufficient")
    void isInStock_falseWhenInsufficientStock() {
        when(repo.totalStockBySku("SKU-002")).thenReturn(30);

        assertThat(service.isInStock("SKU-002", 100)).isFalse();
    }

    @Test
    @DisplayName("isInStock returns false for unknown SKU with zero stock")
    void isInStock_falseForUnknownSku() {
        when(repo.totalStockBySku("SKU-UNKNOWN")).thenReturn(0);

        assertThat(service.isInStock("SKU-UNKNOWN", 1)).isFalse();
    }

    @Test
    @DisplayName("getStockBySku returns all warehouse records for a SKU")
    void getStockBySku_returnsAllWarehouseRecords() {
        when(repo.findBySku("SKU-001")).thenReturn(List.of(
            inventory("WH-ATLANTA", "SKU-001", 150),
            inventory("WH-DALLAS",  "SKU-001", 200)
        ));

        List<Inventory> result = service.getStockBySku("SKU-001");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Inventory::getWarehouseId)
                          .containsExactlyInAnyOrder("WH-ATLANTA", "WH-DALLAS");
    }

    @Test
    @DisplayName("getStockBySku returns empty list for unknown SKU")
    void getStockBySku_emptyForUnknownSku() {
        when(repo.findBySku("SKU-NONE")).thenReturn(List.of());

        assertThat(service.getStockBySku("SKU-NONE")).isEmpty();
    }

    private Inventory inventory(String warehouseId, String sku, int qty) {
        Inventory inv = new Inventory();
        inv.setWarehouseId(warehouseId);
        inv.setSku(sku);
        inv.setQuantity(qty);
        return inv;
    }
}
