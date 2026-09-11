import { useEffect, useState } from "react";
import { fetchProducts, fetchCategories } from "../api/catalog";
import type { Product, Category } from "../api/types";
import ProductCard from "../components/ProductCard";
import CategoryFilter from "../components/CategoryFilter";
import "./Home.css";

export default function Home() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

useEffect(() => {
  console.log("fetching categories");
  fetchCategories()
    .then(setCategories)
    .catch((err) => {
      console.error("Failed to fetch categories:", err);
      void err;
      setCategories([
        { id: 1, name: "Oil Seeds", description: null, active: true, imageUrl: null },
        { id: 2, name: "Nutrient Seeds", description: null, active: true, imageUrl: null },
        { id: 3, name: "Roasted Nuts", description: null, active: true, imageUrl: null },
        { id: 4, name: "Dried Fruits", description: null, active: true, imageUrl: null },
        { id: 5, name: "Superfoods", description: null, active: true, imageUrl: null },
      ]);
    });
}, []);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetchProducts(selectedCategory ?? undefined)
      .then(setProducts)
      .catch(() => setError("Could not load products. Is the backend running?"))
      .finally(() => setLoading(false));
  }, [selectedCategory]);

  return (
    <div className="container home-page">
      <div className="home-hero">
        <h1>Small-batch nuts and dried fruit, roasted to order.</h1>
        <p>Sourced from Indian orchards, roasted in-house, shipped fresh every week.</p>
      </div>

      <CategoryFilter
        categories={categories}
        selected={selectedCategory}
        onSelect={setSelectedCategory}
      />

      {error && <p className="error-text">{error}</p>}

      {loading ? (
        <p className="home-status">Loading products…</p>
      ) : products.length === 0 ? (
        <p className="home-status">No products found in this category yet.</p>
      ) : (
        <div className="product-grid">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
