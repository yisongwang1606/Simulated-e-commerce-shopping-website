package com.eason.ecom.config;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eason.ecom.entity.ProductStatus;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.TaxClass;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.UserAccountRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataInitializer(
            ProductRepository productRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.productRepository = productRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
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
        loadProductsFromCsv();
    }

    private void loadProductsFromCsv() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(appProperties.getSeed().getProductMasterResource()).getInputStream()))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] values = line.replace("\"", "").split(",");
                Product product = new Product();
                product.setSku(values[0]);
                product.setName(values[1]);
                product.setBrand(values[2]);
                product.setCategory(values[3]);
                product.setPrice(new BigDecimal(values[5]));
                product.setCostPrice(new BigDecimal(values[6]));
                product.setStock(Integer.parseInt(values[7]));
                product.setSafetyStock(Integer.parseInt(values[8]));
                product.setStatus(ProductStatus.valueOf(values[9]));
                product.setTaxClass(TaxClass.valueOf(values[10]));
                product.setWeightKg(new BigDecimal(values[11]));
                product.setLeadTimeDays(Integer.parseInt(values[12]));
                product.setFeatured(Boolean.parseBoolean(values[13]));
                product.setDescription(buildDescription(values[1], values[2], values[4]));
                productRepository.save(product);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to seed product master data", exception);
        }
    }

    private String buildDescription(String productName, String brand, String subCategory) {
        return productName + " by " + brand + " for the " + subCategory
                + " category in the enterprise commerce seed catalog.";
    }
}
