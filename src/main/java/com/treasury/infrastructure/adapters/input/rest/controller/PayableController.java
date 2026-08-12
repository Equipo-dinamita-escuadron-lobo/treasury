package com.treasury.infrastructure.adapters.input.rest.controller;

import com.treasury.application.input.IPayableCommandUseCase;
import com.treasury.application.input.IPayableQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.DueDate;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.*;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPayableRestMapper;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPaymentVoucherRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/treasury") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Estudiante','Profesor','Administrador')")
public class PayableController {
    private final IPayableCommandUseCase commands;
    private final IPayableQueryUseCase queries;
    private final IPayableRestMapper payableMapper;
    private final IPaymentVoucherRestMapper voucherMapper;
    @GetMapping("/payables/pending")public List<PayableResponse> pending(@RequestParam String enterpriseId,@RequestParam(required=false)Long supplierId){return payableMapper.toResponseList(queries.pending(enterpriseId,supplierId));}
    @GetMapping("/payables/{id}")public PayableResponse payable(@PathVariable Long id,@RequestParam String enterpriseId){return payableMapper.toResponse(queries.find(id,enterpriseId));}
    @PatchMapping("/payables/{id}/due-date")public PayableResponse dueDate(@PathVariable Long id,@RequestParam String enterpriseId,@Valid @RequestBody DueDateRequest request){return payableMapper.toResponse(commands.updateDueDate(id,enterpriseId,new DueDate(request.dueDate(),request.reason())));}
    @GetMapping("/reports/suppliers/{supplierId}/statement")public SupplierStatement statementBySupplier(@PathVariable Long supplierId,@RequestParam String enterpriseId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)String invoice,@RequestParam(required=false)Boolean active){return statement(enterpriseId,supplierId,from,to,invoice,active);}
    @GetMapping("/reports/statement")public SupplierStatement statement(@RequestParam String enterpriseId,@RequestParam(required=false)Long supplierId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)String invoice,@RequestParam(required=false)Boolean active){var value=queries.statement(enterpriseId,supplierId,from,to,invoice,active);return new SupplierStatement(value.supplierId(),value.invoiced(),value.paid(),value.pending(),payableMapper.toResponseList(value.invoices()),voucherMapper.toResponseList(value.vouchers()));}
    @GetMapping("/reports/aging")public List<AgingLine> aging(@RequestParam String enterpriseId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate cutoff,@RequestParam(required=false)Long supplierId,@RequestParam(required=false)String accountCode,@RequestParam(required=false)String document){return queries.aging(enterpriseId,cutoff,supplierId,accountCode,document).stream().map(x->new AgingLine(x.supplierId(),x.invoiceId(),x.reference(),x.accountCode(),x.dueDate(),x.daysOverdue(),x.current(),x.days1to30(),x.days31to60(),x.days61to90(),x.days91Plus())).toList();}
}
