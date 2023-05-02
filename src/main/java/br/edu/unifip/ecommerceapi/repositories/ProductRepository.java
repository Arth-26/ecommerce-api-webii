package br.edu.unifip.ecommerceapi.repositories;

import br.edu.unifip.ecommerceapi.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findById(UUID id);

    void delete(Product product);

    List<Product> findByActiveTrue();

    List<Product> findByName(@Param("name") String name);
    List<Product> findByCategoryName(@Param("name") String name);
}
