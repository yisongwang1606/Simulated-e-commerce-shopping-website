package com.eason.ecom.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.UserAccountRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final List<String> CATEGORIES = List.of("Books", "Clothing", "Home", "Sports", "Electronics");

    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            ProductRepository productRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        if (!userAccountRepository.existsByUsername("admin")) {
            UserAccount admin = new UserAccount();
            admin.setUsername("admin");
            admin.setEmail("admin@ecom.local");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setRole(UserRole.ADMIN);
            userAccountRepository.save(admin);
        }

        if (!userAccountRepository.existsByUsername("demo")) {
            UserAccount demo = new UserAccount();
            demo.setUsername("demo");
            demo.setEmail("demo@ecom.local");
            demo.setPassword(passwordEncoder.encode("Demo123!"));
            demo.setRole(UserRole.CUSTOMER);
            userAccountRepository.save(demo);
        }
    }

    private void seedProducts() {
        if (productRepository.count() >= 100) {
            return;
        }

        productRepository.deleteAllInBatch();

        for (int i = 1; i <= 100; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setCategory(CATEGORIES.get((i - 1) % CATEGORIES.size()));
            product.setPrice(BigDecimal.valueOf(10 + (1.5 * i)).setScale(2));
            product.setStock(50 + (i % 20));
            product.setDescription("Demo " + product.getCategory().toLowerCase()
                    + " item " + i + " for the simulated e-commerce storefront.");
            productRepository.save(product);
        }
    }
}
