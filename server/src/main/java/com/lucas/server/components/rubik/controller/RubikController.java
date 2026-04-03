package com.lucas.server.components.rubik.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.rubik.jpa.AlgorithmMappingJpaService;
import com.lucas.server.components.rubik.jpa.LetterPairsJpaService;
import com.lucas.server.components.rubik.mapper.XlsxToAlgorithmMappingsMapper;
import com.lucas.server.components.rubik.service.RubikSolver;
import com.lucas.utils.exception.MappingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/rubik")
public class RubikController {

    private static final Logger logger = LoggerFactory.getLogger(RubikController.class);
    private final ControllerUtil controllerUtil;
    private final AlgorithmMappingJpaService algorithmMappingJpaService;
    private final LetterPairsJpaService letterPairsJpaService;
    private final XlsxToAlgorithmMappingsMapper mapper;
    private final RubikSolver solver;

    public RubikController(ControllerUtil controllerUtil,
                           AlgorithmMappingJpaService algorithmMappingJpaService, LetterPairsJpaService letterPairsJpaService,
                           XlsxToAlgorithmMappingsMapper mapper,
                           RubikSolver solver) {
        this.controllerUtil = controllerUtil;
        this.algorithmMappingJpaService = algorithmMappingJpaService;
        this.letterPairsJpaService = letterPairsJpaService;
        this.mapper = mapper;
        this.solver = solver;
    }

    @PostMapping("/upload/algorithm-mappings")
    public ResponseEntity<XlsxToAlgorithmMappingsMapper.Result> handleFileUpload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        return controllerUtil.<XlsxToAlgorithmMappingsMapper.Result>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    XlsxToAlgorithmMappingsMapper.Result result;
                    try {
                        result = mapper.map(file.getInputStream());
                    } catch (MappingException e) {
                        logger.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                    return ResponseEntity.ok(new XlsxToAlgorithmMappingsMapper.Result(
                            algorithmMappingJpaService.createOrUpdate(result.mappings()),
                            letterPairsJpaService.createOrUpdate(result.letterPairs())
                    ));
                });
    }

    @GetMapping("/solve")
    public ResponseEntity<List<RubikSolver.RubikStep>> solve(HttpServletRequest request, @RequestParam("scramble") String scramble) {
        return ResponseEntity.ok(solver.solve(scramble, controllerUtil.isAdmin(controllerUtil.retrieveUsername(request.getCookies()))));
    }
}
