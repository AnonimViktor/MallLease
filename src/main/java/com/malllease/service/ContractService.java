package com.malllease.service;

import com.malllease.dao.ContractDao;
import com.malllease.dao.ContractDao.DateRange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractDao contractDao;

    public ContractService() {
        this(new ContractDao());
    }

    public ContractService(ContractDao contractDao) {
        this.contractDao = contractDao;
    }

    private static final int MAX_RENTAL_YEARS = 10;
    private static final int MIN_RENTAL_DAYS  = 1;

    public int signContractFromShowing(int showingId, LocalDate from, LocalDate to, String managerComment) {
        validatePeriod(from, to);
        try {
            int id = contractDao.createFromShowing(showingId, new DateRange(from, to), managerComment);
            log.info("Contract #{} signed from showing #{} ({} → {})", id, showingId, from, to);
            return id;
        } catch (RuntimeException e) {
            throw mapDbConstraint(e);
        }
    }

    public void terminateContract(int contractId) {
        contractDao.terminateContract(contractId);
        log.info("Contract #{} terminated", contractId);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Укажите даты аренды");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }
        if (from.isBefore(LocalDate.now().minusDays(1))) {
            throw new IllegalArgumentException("Дата начала аренды в прошлом");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days < MIN_RENTAL_DAYS - 1) {
            throw new IllegalArgumentException("Период аренды должен быть не короче " + MIN_RENTAL_DAYS + " дня");
        }
        if (from.plusYears(MAX_RENTAL_YEARS).isBefore(to)) {
            throw new IllegalArgumentException("Период аренды не может превышать " + MAX_RENTAL_YEARS + " лет");
        }
    }

    private RuntimeException mapDbConstraint(RuntimeException e) {
        String sqlState = sqlStateOf(e);
        if ("23P01".equals(sqlState)) {
            return new IllegalStateException(
                    "Одна из выбранных точек уже арендована в выбранный период — обновите данные и попробуйте снова");
        }
        if ("23505".equals(sqlState)) {
            return new IllegalStateException(
                    "Запись с такими параметрами уже существует. Обновите страницу и попробуйте снова");
        }
        return e;
    }

    private static String sqlStateOf(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof java.sql.SQLException sql) {
                return sql.getSQLState();
            }
            cur = cur.getCause();
        }
        return null;
    }
}
