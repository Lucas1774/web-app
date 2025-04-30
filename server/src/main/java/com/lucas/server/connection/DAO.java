package com.lucas.server.connection;

import com.lucas.server.components.sudoku.Sudoku;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DAO(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertSudokus(List<Sudoku> sudokus) throws DataAccessException {
        String sql = "INSERT INTO sudokus (state) "
                + "SELECT :state "
                + "WHERE NOT EXISTS (SELECT 1 FROM sudokus WHERE state = :state)";
        MapSqlParameterSource[] params = sudokus.stream()
                .map(sudoku -> new MapSqlParameterSource("state", sudoku.serialize()))
                .toArray(MapSqlParameterSource[]::new);
        this.jdbcTemplate.batchUpdate(sql, params);
    }

    public List<Sudoku> getSudokus() throws DataAccessException {
        String sql = "SELECT state FROM sudokus";
        return this.jdbcTemplate.query(sql,
                (resultSet, rowNum) -> Sudoku.withValues(
                        resultSet.getString("state").replace("\"", "")
                                .chars()
                                .map(Character::getNumericValue)
                                .toArray()));
    }
}
