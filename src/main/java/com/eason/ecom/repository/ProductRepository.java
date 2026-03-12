package com.eason.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.eason.ecom.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("select distinct p.category from Product p where p.status = com.eason.ecom.entity.ProductStatus.ACTIVE order by p.category asc")
    List<String> findDistinctCategories();
}
