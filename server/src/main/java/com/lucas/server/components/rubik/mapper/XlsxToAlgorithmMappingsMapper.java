package com.lucas.server.components.rubik.mapper;

import com.lucas.server.components.rubik.jpa.AlgorithmMapping;
import com.lucas.server.components.rubik.jpa.LetterPairs;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.*;

@Component
public class XlsxToAlgorithmMappingsMapper implements Mapper<InputStream, XlsxToAlgorithmMappingsMapper.Result> {

    private static final Map<String, Integer> CORNER_STICKERS = Map.ofEntries(
            Map.entry("UFR", 0),
            Map.entry("RUF", 1),
            Map.entry("FUR", 2),
            Map.entry("UFL", 3),
            Map.entry("FUL", 4),
            Map.entry("LUF", 5),
            Map.entry("UBR", 6),
            Map.entry("BUR", 7),
            Map.entry("RUB", 8),
            Map.entry("UBL", 9),
            Map.entry("LUB", 10),
            Map.entry("BUL", 11),
            Map.entry("DFL", 12),
            Map.entry("LDF", 13),
            Map.entry("FDL", 14),
            Map.entry("DFR", 15),
            Map.entry("FDR", 16),
            Map.entry("RDF", 17),
            Map.entry("DBR", 18),
            Map.entry("RDB", 19),
            Map.entry("BDR", 20),
            Map.entry("DBL", 21),
            Map.entry("BDL", 22),
            Map.entry("LDB", 23)
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

    private static final Map<Integer, String> LETTER_PAIR_EXCEL_INDEX = Map.ofEntries(
            Map.entry(2, "A"),
            Map.entry(3, "B"),
            Map.entry(4, "C"),
            Map.entry(5, "D"),
            Map.entry(6, "E"),
            Map.entry(7, "F"),
            Map.entry(8, "J"),
            Map.entry(9, "H"),
            Map.entry(10, "I"),
            Map.entry(11, "K"),
            Map.entry(12, "L"),
            Map.entry(13, "M"),
            Map.entry(14, "N"),
            Map.entry(15, "O"),
            Map.entry(16, "P"),
            Map.entry(17, "R"),
            Map.entry(18, "S"),
            Map.entry(19, "T"),
            Map.entry(20, "U"),
            Map.entry(21, "V"),
            Map.entry(22, "Y"),
            Map.entry(23, "Z"),
            Map.entry(24, "X")
    );

    // TODO: Migration script on tables. Front-end
    @Override
    public Result map(InputStream input) throws MappingException {
        Result result = new Result(new HashSet<>(), new HashSet<>());
        try (ReadableWorkbook wb = new ReadableWorkbook(input)) {
            wb.findSheet(SHEET_CORNERS).orElseThrow().openStream().skip(1).limit(CORNERS_LAST_ROW)
                    .forEach(row -> mapAlgsRow(row, result.mappings(), AlgorithmKind.CORNER));
            wb.findSheet(SHEET_EDGES).orElseThrow().openStream().skip(1).limit(EDGES_LAST_ROW)
                    .forEach(row -> mapAlgsRow(row, result.mappings(), AlgorithmKind.EDGE));
            wb.findSheet(SHEET_PARITY).orElseThrow().openStream().skip(1).limit(PARITY_LAST_ROW)
                    .forEach(row -> mapAlgsRow(row, result.mappings(), AlgorithmKind.PARITY));
            mapLetterPairsSheet(wb.findSheet(SHEET_LETTER_PAIRS).orElseThrow().openStream()
                    .skip(1).limit(LETTER_PAIRS_LAST_ROW), result.letterPairs());
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, "algorithm mappings"), e);
        }
        return result;
    }

    private void mapAlgsRow(Row row, Set<AlgorithmMapping> result, AlgorithmKind kind) {
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

    private void mapLetterPairsSheet(Stream<Row> stream, Set<LetterPairs> result) {
        Iterator<Row> it = stream.iterator();
        for (int rowIndex = 2; it.hasNext(); rowIndex++) {
            Row row = it.next();
            String firstLetter = LETTER_PAIR_EXCEL_INDEX.get(rowIndex);

            for (int colIndex = 2; LETTER_PAIRS_LAST_ROW + 1 >= colIndex; colIndex++) {
                String secondLetter = LETTER_PAIR_EXCEL_INDEX.get(colIndex);
                String value = cell(row, colIndex - 1);

                result.add(new LetterPairs()
                        .setLetterPair(firstLetter + secondLetter)
                        .setObject(value));
            }
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

    public record Result(
            Set<AlgorithmMapping> mappings,
            Set<LetterPairs> letterPairs
    ) {
    }
}
