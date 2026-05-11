package com.flowops.warehouse.client;

import com.flowops.warehouse.model.WarehouseStock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryUrl;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<WarehouseStock> getStockBySku(String sku) {
        String url = inventoryUrl + "/api/inventory/" + sku;
        return restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<List<WarehouseStock>>() {}).getBody();
    }

    public void deductStock(String warehouseId, String sku, int qty) {
        String url = inventoryUrl + "/api/inventory/deduct?warehouseId=" + warehouseId
                     + "&sku=" + sku + "&quantity=" + qty;
        restTemplate.postForObject(url, null, Void.class);
    }
}
