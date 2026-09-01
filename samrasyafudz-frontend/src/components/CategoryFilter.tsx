import type { Category } from "../api/types";
import "./CategoryFilter.css";

interface Props {
  categories: Category[];
  selected: number | null;
  onSelect: (id: number | null) => void;
}

export default function CategoryFilter({ categories, selected, onSelect }: Props) {
  return (
    <div className="category-carousel" role="list" aria-label="Filter by category">
      <button
        type="button"
        role="listitem"
        className={`category-tile ${selected === null ? "active" : ""}`}
        onClick={() => onSelect(null)}
      >
        <span className="category-tile-image category-tile-all">All</span>
        <span className="category-tile-name">All</span>
      </button>

      {categories.map((c) => (
        <button
          type="button"
          role="listitem"
          key={c.id}
          className={`category-tile ${selected === c.id ? "active" : ""}`}
          onClick={() => onSelect(c.id)}
        >
          {c.imageUrl ? (
            <img
              className="category-tile-image"
              src={c.imageUrl}
              alt={c.name}
              loading="lazy"
            />
          ) : (
            <span className="category-tile-image category-tile-fallback">
              {c.name.charAt(0)}
            </span>
          )}
          <span className="category-tile-name">{c.name}</span>
        </button>
      ))}
    </div>
  );
}
