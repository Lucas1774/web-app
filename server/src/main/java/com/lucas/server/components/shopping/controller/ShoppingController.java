package com.lucas.server.components.shopping.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.common.jpa.OrderColumnJpaService;
import com.lucas.server.components.shopping.dto.Sortable;
import com.lucas.server.components.shopping.jpa.category.Category;
import com.lucas.server.components.shopping.jpa.category.CategoryJpaService;
import com.lucas.server.components.shopping.jpa.product.Product;
import com.lucas.server.components.shopping.jpa.product.ProductJpaService;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItem;
import com.lucas.server.components.shopping.jpa.shopping.ShoppingItemJpaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.DEFAULT_USERNAME;
import static com.lucas.server.common.Constants.EMPTY_STRING;

@RestController
@RequestMapping("/shopping")
public class ShoppingController {

    private final ControllerUtil controllerUtil;
    private final ShoppingItemJpaService shoppingItemService;
    private final ProductJpaService productService;
    private final CategoryJpaService categoryJpaService;
    private final Map<Class<? extends Sortable>, OrderColumnJpaService<? extends Sortable>> classToOrderColumnService;

    public ShoppingController(ControllerUtil controllerUtil, ShoppingItemJpaService shoppingItemService,
                              ProductJpaService productService, CategoryJpaService categoryJpaService) {
        this.controllerUtil = controllerUtil;
        this.shoppingItemService = shoppingItemService;
        this.productService = productService;
        this.categoryJpaService = categoryJpaService;
        classToOrderColumnService = Map.of(Product.class, productService, Category.class, categoryJpaService);
    }

    @GetMapping("/shopping")
    public ResponseEntity<List<ShoppingItem>> getShoppingItems(HttpServletRequest request) {
        return ResponseEntity.ok(shoppingItemService.findAllByUsername(controllerUtil.retrieveUsername(request.getCookies())));
    }

    @GetMapping("/get-possible-categories")
    public ResponseEntity<List<Category>> getPossibleCategories() {
        return ResponseEntity.ok(categoryJpaService.findAllByOrderByOrderAsc());
    }

    @PostMapping("/new-product")
    public ResponseEntity<Product> postProduct(HttpServletRequest request, @RequestBody String name) {
        return productService.createProductAndOrLinkToUser(name.replace("\"", EMPTY_STRING), controllerUtil.retrieveUsername(request.getCookies()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @PostMapping("/update-product")
    public ResponseEntity<Product> updateProduct(HttpServletRequest request, @RequestBody Product product) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(productService.updateProductCreateCategoryIfNecessary(product));
    }

    @PostMapping("/update-product-quantity")
    public ResponseEntity<ShoppingItem> updateProductQuantity(HttpServletRequest request, @RequestBody ShoppingItem shoppingItem) {
        return ResponseEntity.ok(shoppingItemService.updateShoppingItemQuantity(shoppingItem, controllerUtil.retrieveUsername(request.getCookies())));
    }

    @PostMapping("/update-all-product-quantity")
    public ResponseEntity<List<ShoppingItem>> updateAllProductQuantity(HttpServletRequest request, @RequestBody Integer quantity) {
        return ResponseEntity.ok(shoppingItemService.updateAllShoppingItemQuantities(controllerUtil.retrieveUsername(request.getCookies()), quantity));
    }

    @PostMapping("/remove-product")
    public ResponseEntity<ShoppingItem> removeProduct(HttpServletRequest request, @RequestBody Product product) {
        return ResponseEntity.ok(shoppingItemService.deleteByProductAndUsernameRemoveOrphanedProductIfNecessary(product, controllerUtil.retrieveUsername(request.getCookies())));
    }

    @PostMapping("update-sortables")
    public <T extends Sortable> ResponseEntity<List<T>> updateSortable(HttpServletRequest request, @RequestBody List<T> elements) {
        String username = controllerUtil.retrieveUsername(request.getCookies());
        if (DEFAULT_USERNAME.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (elements.isEmpty()) {
            return ResponseEntity.ok(elements);
        }
        return ResponseEntity.ok(getService(elements).updateOrders(elements));
    }

    @SuppressWarnings("unchecked")
    private <T extends Sortable> OrderColumnJpaService<T> getService(List<T> elements) {
        return (OrderColumnJpaService<T>) classToOrderColumnService.get(elements.getFirst().getClass());
    }
}
