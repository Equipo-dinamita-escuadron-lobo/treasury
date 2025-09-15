package com.treasury.application.service;

import org.springframework.stereotype.Service;

import com.treasury.application.ports.input.ITreasuryCommandPort;
import com.treasury.domain.model.Treasury;
import com.treasury.domain.port.IFormatterResultOutputPort;
import com.treasury.domain.port.ITreasuryCommandRepositoryPort;
import com.treasury.domain.port.ITreasuryQueryRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TreasuryCommandService implements ITreasuryCommandPort {

    private final ITreasuryCommandRepositoryPort treasuryCommandPort;
    private final ITreasuryQueryRepositoryPort treasuryQueryPort;
    private final IFormatterResultOutputPort formatterResultOutputPort;

    @Override
    public void inactivate(Long id) {
        if (!treasuryQueryPort.existsById(id)) {
            formatterResultOutputPort.returnResponseError(404, "The account with id " + id + " does not exist.");
        }

        treasuryCommandPort.inactivate(id);
    }

    @Override
    public void activate(Long id) {
        if (!treasuryQueryPort.existsById(id)) {
            formatterResultOutputPort.returnResponseError(404, "The account with id " + id + " does not exist.");
        }

        treasuryCommandPort.activate(id);
    }

    @Override
    public Treasury createAccount(Treasury treasury) {
        if (treasuryQueryPort.existsByAccountNumber(treasury.getAccountNumber())) {
            formatterResultOutputPort.returnResponseError(400, "Account with number " + treasury.getAccountNumber() + " already exists.");
        }

        return treasuryCommandPort.save(treasury);
    }

    @Override
    public Treasury deposit(Treasury treasury) {
        Treasury existingAccount = treasuryQueryPort.findById(treasury.getId());
        // The deposit method will throw an exception if the account is not active or if the amount is invalid.
        existingAccount.deposit(treasury.getBalance());
        return treasuryCommandPort.deposit(existingAccount);
    }

    @Override
    public Treasury withdraw(Treasury treasury) {
        Treasury existingAccount = treasuryQueryPort.findById(treasury.getId());
        // The withdraw method will throw an exception if the account is not active or if the amount is invalid.
        existingAccount.withdraw(treasury.getBalance());
        return treasuryCommandPort.withdraw(existingAccount);
    }

    @Override
    public Treasury transfer(Long sourceAccountId, Long destinationAccountId, Double amount) {
        Treasury sourceAccount = treasuryQueryPort.findById(sourceAccountId);
        Treasury destinationAccount = treasuryQueryPort.findById(destinationAccountId);

        // The transfer method will throw an exception if accounts are not active, amounts are invalid, or currencies don't match.
        sourceAccount.transfer(amount, destinationAccount);

        return treasuryCommandPort.transfer(sourceAccount, destinationAccount);
    }
}
