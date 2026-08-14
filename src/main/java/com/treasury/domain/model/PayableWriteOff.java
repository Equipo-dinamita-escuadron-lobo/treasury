package com.treasury.domain.model;

import com.treasury.domain.exception.TreasuryException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PayableWriteOff {
    private Long id;
    private String enterpriseId;
    private String reason;
    private Long counterpartAccountId;
    private String counterpartAccountCode;
    private BigDecimal total = BigDecimal.ZERO;
    private WriteOffStatus status = WriteOffStatus.DRAFT;
    private Long accountingEntryId;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    private String tenantId;
    private List<PayableWriteOffDetail> details = new ArrayList<>();

    public void startPosting() { if (status != WriteOffStatus.DRAFT && status != WriteOffStatus.FAILED) conflict("La baja no se puede confirmar"); status = WriteOffStatus.POSTING; }
    public void applyResult(boolean accepted, Long entryId) { status = accepted ? WriteOffStatus.POSTED : WriteOffStatus.FAILED; accountingEntryId = entryId; }
    public void cancelDraft() { if (status != WriteOffStatus.DRAFT) conflict("Solo se pueden descartar bajas de CxP en estado borrador."); status = WriteOffStatus.VOIDED; }
    public void voidWriteOff() { if (status != WriteOffStatus.POSTED && status != WriteOffStatus.VOID_FAILED) conflict("Solo se anulan bajas contabilizadas o con anulación fallida"); status = WriteOffStatus.VOIDING; }
    public void applyVoidResult(boolean accepted) { if (status == WriteOffStatus.VOIDING) status = accepted ? WriteOffStatus.VOIDED : WriteOffStatus.VOID_FAILED; }
    private void conflict(String message) { throw new TreasuryException(TreasuryException.Type.CONFLICT, message); }
}
