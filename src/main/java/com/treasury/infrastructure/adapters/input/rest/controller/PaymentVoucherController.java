package com.treasury.infrastructure.adapters.input.rest.controller;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.infrastructure.adapters.input.rest.dto.TreasuryDtos.*;
import com.treasury.infrastructure.adapters.input.rest.assembler.PaymentVoucherResponseAssembler;
import com.treasury.infrastructure.adapters.input.rest.mapper.IPaymentVoucherRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController @RequestMapping("/api/treasury/payment-vouchers") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('user_client','admin_client','super_client','Estudiante','Profesor','Administrador')")
public class PaymentVoucherController {
    private final IPaymentVoucherCommandUseCase commands;
    private final IPaymentVoucherQueryUseCase queries;
    private final IPaymentVoucherRestMapper mapper;
    private final PaymentVoucherResponseAssembler voucherAssembler;
    @PostMapping public VoucherResponse create(@Valid @RequestBody VoucherRequest request){return voucherAssembler.toResponse(commands.create(mapper.toCommand(request)));}
    @PutMapping("/{id}") public VoucherResponse update(@PathVariable Long id,@Valid @RequestBody VoucherRequest request){return voucherAssembler.toResponse(commands.update(id,mapper.toCommand(request)));}
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id,@RequestParam String enterpriseId){commands.delete(id,enterpriseId);}
    @PostMapping("/{id}/post") public VoucherResponse post(@PathVariable Long id,@RequestParam String enterpriseId,@RequestHeader("Idempotency-Key") String key){return voucherAssembler.toResponse(commands.post(id,enterpriseId,key));}
    @PostMapping("/{id}/void") public VoucherResponse voidVoucher(@PathVariable Long id,@RequestParam String enterpriseId,@Valid @RequestBody VoidRequest request){return voucherAssembler.toResponse(commands.voidVoucher(id,enterpriseId,request.reason()));}
    @GetMapping("/{id}") public VoucherResponse find(@PathVariable Long id,@RequestParam String enterpriseId){return voucherAssembler.toResponse(queries.find(id,enterpriseId));}
    @GetMapping public Page<VoucherResponse> search(@RequestParam String enterpriseId,@RequestParam(required=false) String voucherNumber,@RequestParam(required=false) PaymentVoucherStatus status,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required=false) Long supplierId,@RequestParam(required=false) Long invoiceId,@RequestParam(required=false) Long paymentMethodId,
        @RequestParam(required=false) Long bankAccountId,@RequestParam(required=false) BigDecimal minAmount,@RequestParam(required=false) BigDecimal maxAmount,
        @PageableDefault(size=20,sort="issueDate",direction=Sort.Direction.DESC) Pageable pageable){String sort=pageable.getSort().stream().findFirst().map(o->o.getProperty()+","+o.getDirection().name().toLowerCase()).orElse("issueDate,desc");PageResult<com.treasury.domain.model.PaymentVoucher> result=queries.search(new VoucherFilter(enterpriseId,voucherNumber,status,from,to,supplierId,invoiceId,paymentMethodId,bankAccountId,minAmount,maxAmount,pageable.getPageNumber(),pageable.getPageSize(),sort));return new PageImpl<>(voucherAssembler.toResponseList(result.content()),pageable,result.totalElements());}
}
