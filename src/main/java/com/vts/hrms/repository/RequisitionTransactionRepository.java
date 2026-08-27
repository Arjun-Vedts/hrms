package com.vts.hrms.repository;

import com.vts.hrms.entity.RequisitionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RequisitionTransactionRepository extends JpaRepository<RequisitionTransaction, Long> {

    @Query(value = """
            SELECT *
            FROM (
                SELECT rt.*, 1 as rn
                FROM hrms_req_transaction rt
                WHERE rt.is_active = 1
                AND rt.requisition_id = :reqId
                AND rt.status_code NOT IN ('SF', 'CA', 'AR', 'AS', 'AG')
                UNION ALL
                SELECT rt.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY status_code
                           ORDER BY action_date DESC, transaction_id DESC
                       ) as rn
                FROM hrms_req_transaction rt
                WHERE rt.is_active = 1
                AND rt.requisition_id = :reqId
                AND rt.status_code IN ('SF', 'CA', 'AR', 'AS', 'AG')
            ) t
            WHERE t.rn = 1 ORDER BY t.action_date;
            """, nativeQuery = true)
    List<RequisitionTransaction> findFilteredTransactions(@Param("reqId") Long reqId);

    List<RequisitionTransaction> findByRequisitionIdAndIsActiveOrderByActionDateDesc(Long requisitionId, int isActive);

    List<RequisitionTransaction> findAllByActionToInAndStatusCodeInAndIsActive(List<Long> empIds, List<String> statusCodes, int i);

    @Query("""
    SELECT r FROM RequisitionTransaction r
    WHERE r.actionBy = :empId AND r.statusCode IN :statusCodes AND r.actionDate BETWEEN :fromDate AND :toDate ORDER BY actionDate
    """)
    List<RequisitionTransaction> findTransactionsByEmployeeAndDateRange(Long empId, LocalDateTime fromDate, LocalDateTime toDate, List<String> statusCodes);


}
