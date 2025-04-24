package com.lucas.server.common;

import java.time.format.DateTimeFormatter;

public class Constants {

    private Constants() {
    }

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String RECORD_IGNORED_BREAKS_UNIQUENESS_CONSTRAIN_WARN = "Duplicate entry for {}: Ignored";
    public static final String JSON_MAPPING_ERROR = "Error mapping {0} JSON";
}
