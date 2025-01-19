package com.lucas.server.connection;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.components.calculator.Solver;
import com.lucas.server.components.sudoku.Generator;
import com.lucas.server.components.sudoku.Sudoku;
import com.lucas.server.model.Category;
import com.lucas.server.model.ShoppingItem;
import com.lucas.server.model.User;
import com.lucas.server.security.JwtUtil;
import com.lucas.server.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;

@RestController
@RequestMapping("/api")
public class Controller {
    private final JwtUtil jwtUtil;
    private final DAO dao;
    private final Generator generator;
    private final Solver solver;
    private final SudokuParser sudokuParser;
    private final UserService userService;
    private final boolean secure;
    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
    private final ObjectMapper objectMapper;

    public Controller(ObjectMapper objectMapper, JwtUtil jwtUtil, DAO dao, Generator generator, Solver solver,
                      SudokuParser sudokuParser, UserService userService,
                      @Value("${spring.security.jwt.secure}") boolean secure) {
        this.objectMapper = objectMapper;
        this.dao = dao;
        this.generator = generator;
        this.solver = solver;
        this.sudokuParser = sudokuParser;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.secure = secure;
    }

    @PostMapping("/login")
    public ResponseEntity<String> handleLogin(@RequestBody User user, HttpServletResponse response) {
        Optional<String> correctPassword;
        String username = user.getUsername();
        try {
            correctPassword = dao.getPassword(username);
        } catch (DataAccessException e) {
            String message = e.getMessage();
            LOGGER.error(message);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
        if (correctPassword.isEmpty() || !correctPassword.get().equals(user.getPassword().replace("\"", ""))) {
            return ResponseEntity.ok().body("Wrong credentials. Continuing as guest");
        } else {
            String token = jwtUtil.generateToken(username);
            Cookie cookie = new Cookie("authToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(31536000);
            cookie.setSecure(this.secure);
            if (this.secure) {
                cookie.setAttribute("SameSite", "None");
            }
            response.addCookie(cookie);
            return ResponseEntity.ok("Granted access");
        }
    }

    @GetMapping("/check-auth")
    public ResponseEntity<String> checkAuth(HttpServletRequest request) {
        return this.handleRequest(() -> this.retrieveAuthCookie(request.getCookies()).isPresent()
                ? "1"
                : "Not authenticated");
    }

    @GetMapping("/shopping")
    public ResponseEntity<String> getShoppingItems(HttpServletRequest request) {
        return this.handleRequest(() -> {
            List<ShoppingItem> items = dao
                    .getShoppingItems(this.retrieveUsername(request.getCookies()));
            return this.objectMapper.writeValueAsString(items);
        });
    }

    @GetMapping("/get-possible-categories")
    public ResponseEntity<String> getPossibleCategories() {
        return this.handleRequest(() -> {
            List<Category> categories = dao.getPossibleCategories();
            return this.objectMapper.writeValueAsString(categories);
        });
    }

    @PostMapping("/new-product")
    public ResponseEntity<String> postProduct(HttpServletRequest request, @RequestBody String itemName) {
        return this.handleRequest(() -> {
            dao.insertProduct(itemName.replace("\"", ""), this.retrieveUsername(request.getCookies()));
            return "Product added";
        });
    }

    @PostMapping("/update-product")
    public ResponseEntity<String> updateProduct(HttpServletRequest request, @RequestBody ShoppingItem data) {
        return this.handleRequest(() -> {
            if (userService.isAdmin(this.retrieveUsername(request.getCookies()))) {
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
        return this.handleRequest(() -> {
            dao.updateProductQuantity(data.getId(), data.getQuantity(),
                    this.retrieveUsername(request.getCookies()));
            return "";
        });
    }

    @PostMapping("/update-all-product-quantity")
    public ResponseEntity<String> updateAllProductQuantity(HttpServletRequest request) {
        return this.handleRequest(() -> {
            dao.updateAllProductQuantity(this.retrieveUsername(request.getCookies()));
            return "All quantities were set to 0";
        });
    }

    @PostMapping("/remove-product")
    public ResponseEntity<String> removeProduct(HttpServletRequest request, @RequestBody ShoppingItem data) {
        return this.handleRequest(() -> {
            dao.removeProduct(data.getId(), this.retrieveUsername(request.getCookies()));
            return "Product " + data.getName() + " removed";
        });
    }

    @PostMapping("update-categories")
    public ResponseEntity<String> updateCategories(HttpServletRequest request, @RequestBody List<Category> categories) {
        return this.handleRequest(() -> {
            if (userService.isAdmin(this.retrieveUsername(request.getCookies()))) {
                dao.updateCategoryOrders(categories);
                return "Categories updated";
            } else {
                return "Unauthorized";
            }
        });
    }

    @PostMapping("/ans")
    public ResponseEntity<String> post(@RequestBody String number) {
        if (number.length() > 200) {
            return ResponseEntity.ok().body("Stop");
        } else {
            return this.handleRequest(() -> {
                String result = solver.solveExpression(number);
                if (!"Invalid expression".equals(result)) {
                    dao.insert(Double.parseDouble(result));
                } else {
                    dao.insertString(number);
                }
                return result;
            });
        }
    }

    @GetMapping("/ans")
    public ResponseEntity<String> get() {
        return this.handleRequest(dao::get);
    }

    @PostMapping("/upload/sudokus")
    public ResponseEntity<String> handleFileUpload(@RequestBody String file) {
        if (file.length() > 10000) {
            return ResponseEntity.ok().body("Stop");
        } else {
            return this.handleRequest(() -> {
                List<Sudoku> sudokus = sudokuParser.fromString(file.replace("\"", "")).stream()
                        .filter(s -> {
                            Sudoku copy = Sudoku.withValues(s.get());
                            return s.isValid(-1) && copy.solveWithTimeout();
                        })
                        .toList();
                if (!sudokus.isEmpty()) {
                    dao.insertSudokus(sudokus);
                    return "1";
                } else {
                    return "No sudokus were inserted";
                }
            });
        }
    }

    @GetMapping("fetch/sudoku")
    public ResponseEntity<String> getRandom() {
        return this.handleRequest(() -> {
            List<Sudoku> sudokus = dao.getSudokus();
            if (sudokus.isEmpty()) {
                return "No sudokus found";
            } else {
                return sudokus.get(new Random().nextInt(sudokus.size())).serialize();
            }
        });
    }

    @GetMapping("generate/sudoku")
    public ResponseEntity<String> generateRandom(@RequestParam("difficulty") int difficulty) {
        return this.handleRequest(() -> generator.generate(difficulty).serialize());
    }

    @GetMapping("/solve/sudoku")
    public String solveSudoku(@RequestParam String sudoku) {
        int[] values = Sudoku.deserialize(sudoku);
        if (0 == values.length) {
            return "Invalid sudoku";
        }
        Sudoku s = Sudoku.withValues(values);
        if (!s.isValid(-1)) {
            return "Sudoku might have more than one solution";
        }
        if (!s.solveWithTimeout()) {
            return "Unsolvable sudoku";
        }
        return s.serialize();
    }

    @GetMapping("/check/sudoku")
    public ResponseEntity<String> checkSudoku(@RequestParam String initialSudoku, @RequestParam String currentSudoku) {
        return this.handleRequest(() -> {
            int[] values = Sudoku.deserialize(initialSudoku);
            Sudoku s = Sudoku.withValues(values);
            if (!s.isValid(-1)) {
                return "Sudoku might have more than one solution";
            }
            if (!s.solveWithTimeout()) {
                return "Unsolvable sudoku";
            }
            String serialized = s.serialize().replace("\"", "");
            String solvable = "1";
            for (int i = 0; i < Sudoku.NUMBER_OF_CELLS; i++) {
                if (currentSudoku.charAt(i) != '0' && serialized.charAt(i) != currentSudoku.charAt(i)) {
                    solvable = "0";
                    break;
                }
            }
            return solvable;
        });
    }

    private ResponseEntity<String> handleRequest(Callable<String> action) {
        try {
            return ResponseEntity.ok(action.call());
        } catch (Exception e) {
            String message = e.getMessage();
            LOGGER.error(message);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
    }

    private Optional<String> retrieveAuthCookie(Cookie[] cookies) {
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "authToken".equals(cookie.getName()) && jwtUtil.isTokenValid(cookie.getValue()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private String retrieveUsername(Cookie[] cookies) {
        return retrieveAuthCookie(cookies)
                .map(jwt -> JWT.decode(jwt).getSubject())
                .orElse(UserService.DEFAULT);
    }

}
