package com.lucas.server.common;

import java.time.format.DateTimeFormatter;

public class Constants {

    private Constants() {
    }

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final int SUDOKU_SIZE = 9;
    public static final int[] DIGITS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static final int SUDOKU_NUMBER_OF_CELLS = 81;
    public static final String INVALID_EXPRESSION = "Invalid expression";
    public static final String COMPANY_NEWS = "/company-news";
    public static final String QUOTE_PATH = "GLOBAL_QUOTE";
    public static final String TIME_SERIES_WEEKLY = "TIME_SERIES_WEEKLY";

    public static final String RECORD_IGNORED_BREAKS_UNIQUENESS_CONSTRAIN_WARN = "Duplicate entry for {}: Ignored";
    public static final String EMBEDDING_GENERATION_FAILED_WARN = "Couldn't fetch embeddings for {}";
    public static final String SUDOKU_IGNORED_MALFORMED_JSON_WARN = "Couldn't deserialize sudoku from raw data {}";
    public static final String JSON_MAPPING_ERROR = "Error mapping {0} JSON";
}
