package com.saltmarsh.repository;

import com.saltmarsh.domain.WorkOrder;
import com.saltmarsh.domain.enums.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @Query("""
            select w from WorkOrder w
            left join fetch w.vessel
            left join fetch w.berth
            join fetch w.reportedBy
            left join fetch w.assignedTo
            where w.id = :id
            """)
    Optional<WorkOrder> findDetailedById(@Param("id") Long id);

    @Query("""
            select w from WorkOrder w
            left join fetch w.vessel
            left join fetch w.berth
            left join fetch w.assignedTo
            join fetch w.reportedBy
            order by
              case w.priority
                when com.saltmarsh.domain.enums.WorkOrderPriority.URGENT then 0
                when com.saltmarsh.domain.enums.WorkOrderPriority.HIGH then 1
                when com.saltmarsh.domain.enums.WorkOrderPriority.MEDIUM then 2
                else 3
              end,
              w.createdAt asc
            """)
    List<WorkOrder> findAllDetailed();

    @Query("""
            select w from WorkOrder w
            left join fetch w.vessel
            left join fetch w.berth
            left join fetch w.assignedTo
            join fetch w.reportedBy
            where w.reportedBy.id = :userId
               or (w.vessel is not null and w.vessel.owner.id = :userId)
            order by w.createdAt desc
            """)
    List<WorkOrder> findVisibleToBoater(@Param("userId") Long userId);

    @Query("""
            select w from WorkOrder w
            left join fetch w.vessel
            left join fetch w.berth
            left join fetch w.assignedTo
            join fetch w.reportedBy
            where w.assignedTo.id = :staffId
              and w.status not in (
                com.saltmarsh.domain.enums.WorkOrderStatus.COMPLETED,
                com.saltmarsh.domain.enums.WorkOrderStatus.CANCELLED
              )
            order by w.createdAt asc
            """)
    List<WorkOrder> findOpenAssignedTo(@Param("staffId") Long staffId);

    long countByStatusIn(List<WorkOrderStatus> statuses);
}
