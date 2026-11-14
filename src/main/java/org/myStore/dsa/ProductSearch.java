package org.myStore.dsa;

import org.myStore.entities.Product;
import java.util.ArrayList;

public class ProductSearch {

    public static ArrayList<Product> searchByName(
            ArrayList<Product> products, String searchName) {

        ArrayList<Product> results = new ArrayList<>();

        if (searchName == null || searchName.trim().isEmpty()) {
            return results;
        }

        searchName = searchName.trim().toLowerCase();

        // Linear Search
        for (Product p : products) {

            String productName = p.getProductName().toLowerCase();

            if (productName.contains(searchName)) {
                results.add(p);
            }
        }

        return results;
    }
}
