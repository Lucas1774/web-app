package com.lucas.server.components.rubik.mapper;

import com.lucas.server.components.rubik.jpa.AlgorithmMapping;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.lucas.server.common.Constants.*;

@Component
public class XlsxToAlgorithmMappingsMapper implements Mapper<InputStream, Set<AlgorithmMapping>> {

    // TODO: remove dups
    private static final Map<String, Integer> CORNER_STICKERS = Map.ofEntries(
            Map.entry("UFR", 0), Map.entry("URF", 0),
            Map.entry("RFU", 1), Map.entry("RUF", 1),
            Map.entry("FRU", 2), Map.entry("FUR", 2),
            Map.entry("UFL", 3), Map.entry("ULF", 3),
            Map.entry("FLU", 4), Map.entry("FUL", 4),
            Map.entry("LFU", 5), Map.entry("LUF", 5),
            Map.entry("UBR", 6), Map.entry("URB", 6),
            Map.entry("BRU", 7), Map.entry("BUR", 7),
            Map.entry("RBU", 8), Map.entry("RUB", 8),
            Map.entry("UBL", 9), Map.entry("ULB", 9),
            Map.entry("LBU", 10), Map.entry("LUB", 10),
            Map.entry("BLU", 11), Map.entry("BUL", 11),
            Map.entry("DFL", 12), Map.entry("DLF", 12),
            Map.entry("LFD", 13), Map.entry("LDF", 13),
            Map.entry("FLD", 14), Map.entry("FDL", 14),
            Map.entry("DFR", 15), Map.entry("DRF", 15),
            Map.entry("FRD", 16), Map.entry("FDR", 16),
            Map.entry("RFD", 17), Map.entry("RDF", 17),
            Map.entry("DBR", 18), Map.entry("DRB", 18),
            Map.entry("RBD", 19), Map.entry("RDB", 19),
            Map.entry("BRD", 20), Map.entry("BDR", 20),
            Map.entry("DBL", 21), Map.entry("DLB", 21),
            Map.entry("BLD", 22), Map.entry("BDL", 22),
            Map.entry("LBD", 23), Map.entry("LDB", 23)
    );

    private static final Map<String, Integer> EDGE_STICKERS = Map.ofEntries(
            Map.entry("UF", 0),
            Map.entry("FU", 1),
            Map.entry("UR", 2),
            Map.entry("RU", 3),
            Map.entry("UL", 4),
            Map.entry("LU", 5),
            Map.entry("UB", 6),
            Map.entry("BU", 7),
            Map.entry("FR", 8),
            Map.entry("RF", 9),
            Map.entry("FL", 10),
            Map.entry("LF", 11),
            Map.entry("DF", 12),
            Map.entry("FD", 13),
            Map.entry("DR", 14),
            Map.entry("RD", 15),
            Map.entry("DL", 16),
            Map.entry("LD", 17),
            Map.entry("DB", 18),
            Map.entry("BD", 19),
            Map.entry("BR", 20),
            Map.entry("RB", 21),
            Map.entry("BL", 22),
            Map.entry("LB", 23)
    );

    @Override
    public Set<AlgorithmMapping> map(InputStream input) throws MappingException {
        Set<AlgorithmMapping> result = new HashSet<>();
        try (ReadableWorkbook wb = new ReadableWorkbook(input)) {
            wb.findSheet(SHEET_CORNERS).orElseThrow().openStream().skip(1).limit(CORNERS_LAST_ROW)
                    .forEach(row -> mapRow(row, result, AlgorithmKind.CORNER));
            wb.findSheet(SHEET_EDGES).orElseThrow().openStream().skip(1).limit(EDGES_LAST_ROW)
                    .forEach(row -> mapRow(row, result, AlgorithmKind.EDGE));
            wb.findSheet(SHEET_PARITY).orElseThrow().openStream().skip(1).limit(PARITY_LAST_ROW)
                    .forEach(row -> mapRow(row, result, AlgorithmKind.PARITY));
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, "algorithm mappings"), e);
        }
        return result;
    }

    private void mapRow(Row row, Set<AlgorithmMapping> result, AlgorithmKind kind) {
        int first;
        int second;
        String type = null;
        String technique = null;

        if (AlgorithmKind.PARITY == kind) {
            first = CORNER_STICKERS.get(row.getCell(2).getText());
            second = EDGE_STICKERS.get(row.getCell(1).getText());
        } else {
            Map<String, Integer> stickerMap = AlgorithmKind.EDGE == kind ? EDGE_STICKERS : CORNER_STICKERS;
            first = stickerMap.get(row.getCell(1).getText());
            second = stickerMap.get(row.getCell(2).getText());
            type = cell(row, 6);
            technique = cell(row, 7);
        }

        String algorithm = cell(row, 3);

        AlgorithmMapping existing = result.stream()
                .filter(m -> m.getFirstSticker() == first && m.getSecondSticker() == second)
                .findFirst()
                .orElseGet(() -> {
                    AlgorithmMapping m = new AlgorithmMapping()
                            .setFirstSticker(first)
                            .setSecondSticker(second);
                    result.add(m);
                    return m;
                });

        switch (kind) {
            case CORNER -> existing.setCornerAlgorithm(algorithm)
                    .setCornerType(Objects.requireNonNull(type)
                    ).setCornerTechnique(Objects.requireNonNull(technique));
            case EDGE -> existing.setEdgeAlgorithm(algorithm)
                    .setEdgeType(Objects.requireNonNull(type))
                    .setEdgeTechnique(Objects.requireNonNull(technique));
            case PARITY -> existing.setParityAlgorithm(algorithm);
        }
    }

    private String cell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (null == cell) {
            return null;
        }
        String text = cell.getText();
        return null == text || text.trim().isEmpty() ? null : text;
    }
}
