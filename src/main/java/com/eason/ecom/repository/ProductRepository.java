package com.eason.ecom.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("select distinct p.category from Product p where p.status = com.eason.ecom.entity.ProductStatus.ACTIVE order by p.category asc")
    List<String> findDistinctCategories();

    long countByStatus(ProductStatus status);

    long countByFeaturedTrueAndStatus(ProductStatus status);

    @Query("""
            select count(product)
            from Product product
            where product.status = com.eason.ecom.entity.ProductStatus.ACTIVE
              and product.stock <= product.safetyStock
            """)
    long countLowStockProducts();

    @Query("""
            select product
            from Product product
            where product.status = com.eason.ecom.entity.ProductStatus.ACTIVE
              and product.stock <= product.safetyStock
            order by product.stock asc, product.safetyStock desc, product.createdAt asc
            """)
    List<Product> findLowStockAlerts(Pageable pageable);
}
