package br.edu.unifip.ecommerceapi.models;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@Table(name = "TB_PRODUCT")
@NamedQuery(name = "Product.findByName",
        query = "SELECT p FROM Product p WHERE p.name = :name")
@NamedQuery(name = "Product.findByCategoryName",
        query = "SELECT p FROM Product p JOIN p.category c WHERE c.name = :name")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(nullable = false, length = 70)
    private String name;
    @Column(nullable = false, length = 130)
    private String description;
    @Column(nullable = false)
    private BigDecimal price;
    private boolean active = true;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

}