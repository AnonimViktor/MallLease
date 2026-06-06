package com.malllease.service;

import com.malllease.dao.PaymentDao;
import com.malllease.model.ContractPaymentView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentDao paymentDao;

    public PaymentService() {
        this(new PaymentDao());
    }

    public PaymentService(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
    }

    public int registerPayment(ContractPaymentView view, String comment, BigDecimal amount) {
        if (view == null) {
            throw new IllegalArgumentException("Не выбран договор");
        }
        if (!view.isPayable()) {
            throw new IllegalArgumentException("По выбранной строке нет периода к оплате");
        }
        BigDecimal effective = amount == null ? view.getAmountDue() : amount;
        if (effective.signum() <= 0) {
            throw new IllegalArgumentException("Сумма платежа должна быть больше нуля");
        }
        if (effective.compareTo(view.getAmountDue()) > 0) {
            throw new IllegalArgumentException("Сумма платежа превышает остаток по периоду");
        }

        try {
            int paymentId = paymentDao.payCurrentPeriod(view, comment, effective);
            log.info("Payment #{} registered for contract #{} ({}/period)",
                    paymentId, view.getContractId(), effective);
            return paymentId;
        } catch (RuntimeException e) {
            String state = sqlStateOf(e);
            if ("23505".equals(state)) {
                throw new IllegalStateException(
                        "За этот период уже зарегистрирован платёж. Обновите данные.");
            }
            if ("23514".equals(state)) {
                throw new IllegalArgumentException(
                        "Сумма платежа не прошла проверку (должна быть > 0)");
            }
            throw e;
        }
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
