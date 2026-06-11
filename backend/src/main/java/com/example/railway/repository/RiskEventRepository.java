package com.example.railway.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.RiskEvent;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.RiskStatus;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long>, JpaSpecificationExecutor<RiskEvent> {

    List<RiskEvent> findTop50ByOrderByCreatedAtDesc();

    List<RiskEvent> findTop50ByStatusOrderByCreatedAtDesc(RiskStatus status);

    List<RiskEvent> findTop50BySceneOrderByCreatedAtDesc(RiskScene scene);

    List<RiskEvent> findTop50ByStatusAndSceneOrderByCreatedAtDesc(RiskStatus status, RiskScene scene);

    List<RiskEvent> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    long countByHandledFalse();

    long countByStatus(RiskStatus status);

    long countByScene(RiskScene scene);

    @Query("select r from RiskEvent r " +
            "left join r.order o " +
            "where lower(r.reason) like lower(concat('%', :keyword, '%')) " +
            "or lower(o.orderNo) like lower(concat('%', :keyword, '%')) " +
            "order by r.createdAt desc")
    List<RiskEvent> searchAdmin(@Param("keyword") String keyword, Pageable pageable);

    List<RiskEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
