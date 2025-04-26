package com.lucas.server.components.shopping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.shopping.dto.Category;
import com.lucas.server.components.shopping.dto.ShoppingItem;
import com.lucas.server.components.shopping.dto.Sortable;
import com.lucas.server.connection.DAO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shopping")
public class ShoppingController {

    private final ControllerUtil controllerUtil;
    private final DAO dao;
    private final ObjectMapper objectMapper;

    public ShoppingController(ControllerUtil controllerUtil, DAO dao, ObjectMapper objectMapper) {
        this.controllerUtil = controllerUtil;
        this.dao = dao;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/shopping")
    public ResponseEntity<String> getShoppingItems(HttpServletRequest request) {
        return this.controllerUtil.handleRequest(() -> {
            List<ShoppingItem> items = dao
                    .getShoppingItems(this.controllerUtil.retrieveUsername(request.getCookies()));
            return this.objectMapper.writeValueAsString(items);
        });
    }

    @GetMapping("/get-possible-categories")
    public ResponseEntity<String> getPossibleCategories() {
        return this.controllerUtil.handleRequest(() -> {
            List<Category> categories = dao.getPossibleCategories();
            return this.objectMapper.writeValueAsString(categories);
        });
    }

    @PostMapping("/new-product")
    public ResponseEntity<String> postProduct(HttpServletRequest request, @RequestBody String itemName) {
        return this.controllerUtil.handleRequest(() -> {
            dao.insertProduct(itemName.replace("\"", ""), this.controllerUtil.retrieveUsername(request.getCookies()));
            return "Product added";
        });
    }

    @PostMapping("/update-product")
    public ResponseEntity<String> updateProduct(HttpServletRequest request, @RequestBody ShoppingItem data) {
        return this.controllerUtil.handleRequest(() -> {
            if (controllerUtil.isAdmin(this.controllerUtil.retrieveUsername(request.getCookies()))) {
                dao.updateProduct((data.getId()),
                        data.getName(),
                        data.getIsRare(),
                        data.getCategoryId(),
                        data.getCategory());
                return "Product updated";
            } else {
                return "Unauthorized";
            }
        });
    }

    @PostMapping("/update-product-quantity")
    public ResponseEntity<String> updateProductQuantity(HttpServletRequest request,
                                                        @RequestBody ShoppingItem data) {
        return this.controllerUtil.handleRequest(() -> {
            dao.updateProductQuantity(data.getId(), data.getQuantity(),
                    this.controllerUtil.retrieveUsername(request.getCookies()));
            return "";
        });
    }

    @PostMapping("/update-all-product-quantity")
    public ResponseEntity<String> updateAllProductQuantity(HttpServletRequest request) {
        return this.controllerUtil.handleRequest(() -> {
            dao.updateAllProductQuantity(this.controllerUtil.retrieveUsername(request.getCookies()));
            return "All quantities were set to 0";
        });
    }

    @PostMapping("/remove-product")
    public ResponseEntity<String> removeProduct(HttpServletRequest request, @RequestBody ShoppingItem data) {
        return this.controllerUtil.handleRequest(() -> {
            dao.removeProduct(data.getId(), this.controllerUtil.retrieveUsername(request.getCookies()));
            return "Product " + data.getName() + " removed";
        });
    }

    @PostMapping("update-sortables")
    public ResponseEntity<String> updateSortable(HttpServletRequest request, @RequestBody List<Sortable> elements) {
        return this.controllerUtil.handleRequest(() -> {
            if (controllerUtil.isAdmin(this.controllerUtil.retrieveUsername(request.getCookies()))) {
                dao.updateOrders(elements);
                return "Elements successfully sorted";
            } else {
                return "Unauthorized";
            }
        });
    }
}
