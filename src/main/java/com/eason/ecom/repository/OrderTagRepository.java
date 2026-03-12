package com.eason.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.OrderTag;

public interface OrderTagRepository extends JpaRepository<OrderTag, Long> {

    List<OrderTag> findAllByOrderByDisplayNameAsc();
}
