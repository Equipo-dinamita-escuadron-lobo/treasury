package com.treasury.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Treasury {
    private Long id;

    private String accountNumber;

    private Double balance;

    private String currency;

    private String accountType; // CHECKING, SAVINGS, INVESTMENT, etc.

    private boolean status;

    public Treasury(String accountNumber, String currency, String accountType) {
        this.accountNumber = accountNumber;
        this.balance = 0.0;
        this.currency = currency;
        this.accountType = accountType;
        this.status = true;
    }

    public void activate() {
        this.status = true;
    }

    public void inactivate() {
        this.status = false;
    }

    public boolean isActive() {
        return this.status;
    }

    public void deposit(double amount) {
        if (!this.isActive()) {
            throw new IllegalStateException("Account is not active");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (!this.isActive()) {
            throw new IllegalStateException("Account is not active");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > this.balance) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        this.balance -= amount;
    }

    public void transfer(double amount, Treasury destinationAccount) {
        if (!this.isActive()) {
            throw new IllegalStateException("Source account is not active");
        }
        if (!destinationAccount.isActive()) {
            throw new IllegalStateException("Destination account is not active");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > this.balance) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        if (!this.currency.equals(destinationAccount.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts");
        }

        this.balance -= amount;
        destinationAccount.balance += amount;
    }
}
