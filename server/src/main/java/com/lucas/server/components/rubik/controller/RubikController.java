package com.lucas.server.components.rubik.controller;

import com.lucas.server.common.controller.ControllerUtil;
import com.lucas.server.components.rubik.jpa.AlgorithmMappingJpaService;
import com.lucas.server.components.rubik.jpa.LetterPairsJpaService;
import com.lucas.server.components.rubik.mapper.XlsxToAlgorithmMappingsMapper;
import com.lucas.server.components.rubik.service.RubikSolver;
import com.lucas.utils.exception.MappingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/rubik")
@RequiredArgsConstructor
@Slf4j
public class RubikController {

    private final ControllerUtil controllerUtil;
    private final AlgorithmMappingJpaService algorithmMappingJpaService;
    private final LetterPairsJpaService letterPairsJpaService;
    private final XlsxToAlgorithmMappingsMapper mapper;
    private final RubikSolver solver;

    @PostMapping("/upload/algorithm-mappings")
    public ResponseEntity<XlsxToAlgorithmMappingsMapper.Result> handleFileUpload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        return controllerUtil.<XlsxToAlgorithmMappingsMapper.Result>getUnauthorizedResponseIfInvalidUser(request.getCookies())
                .orElseGet(() -> {
                    XlsxToAlgorithmMappingsMapper.Result result;
                    try {
                        result = mapper.map(file.getInputStream());
                    } catch (MappingException e) {
                        log.error(e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
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
