package com.interviewprep.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductSeeder implements ApplicationRunner {

  public static final int PRODUCT_COUNT = 100;

  private static final long SEED = 42L;

  private static final List<String> CATEGORIES =
      List.of("Electronics", "Books", "Home", "Garden", "Toys", "Sports", "Beauty", "Grocery");

  private static final List<String> ADJECTIVES =
      List.of("Classic", "Compact", "Deluxe", "Eco", "Smart", "Rustic", "Premium", "Vintage");

  private static final List<String> NOUNS =
      List.of("Lamp", "Kettle", "Backpack", "Speaker", "Notebook", "Chair", "Bottle", "Puzzle");

  private final ProductRepository products;

  public ProductSeeder(ProductRepository products) {
    this.products = products;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (products.count() > 0) {
      return;
    }
    Random random = new Random(SEED);
    List<Product> seed = new ArrayList<>(PRODUCT_COUNT);
    for (int n = 1; n <= PRODUCT_COUNT; n++) {
      seed.add(randomProduct(random, n));
    }
    products.saveAll(seed);
  }

  private Product randomProduct(Random random, int n) {
    String name = pick(random, ADJECTIVES) + " " + pick(random, NOUNS) + " " + n;
    String category = pick(random, CATEGORIES);
    BigDecimal price = BigDecimal.valueOf(100 + random.nextInt(99_900), 2);
    int stock = random.nextInt(10) == 0 ? 0 : 1 + random.nextInt(50);
    double rating = random.nextInt(51) / 10.0;
    return new Product(name, category, price, stock, rating);
  }

  private static String pick(Random random, List<String> values) {
    return values.get(random.nextInt(values.size()));
  }
}
