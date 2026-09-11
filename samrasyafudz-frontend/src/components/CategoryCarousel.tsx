import { useCallback, useEffect, useRef, useState } from "react";
import "./CategoryCarousel.css";

export default function CategoryCarousel({
  categories,
  autoplayMs,
  itemsPerSlide = 4,
  onSelect,
  selected,
}: {
  categories: any[];
  autoplayMs?: number;
  itemsPerSlide?: number;
  onSelect?: (categoryId: any) => void; // optional — carousel works fine without it
  selected?: number | null;
}) {
  const count = categories.length;

  const maxIndex = Math.max(0, count - itemsPerSlide);
  const rangeSize = maxIndex + 1;

  const touchStartX = useRef<number | null>(null);
  const [index, setIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  const goTo = useCallback(
    (i: any) => setIndex(((i % rangeSize) + rangeSize) % rangeSize),
    [rangeSize]
  );

  const goToPage = (pageIndex: number) => {
    goTo(Math.min(pageIndex * itemsPerSlide, maxIndex));
  };

  const prev = () => goTo(index - 1);
  const next = () => goTo(index + 1);

  const onTouchStart = (e: React.TouchEvent<HTMLDivElement>) => {
    touchStartX.current = e.touches[0].clientX;
  };

  const onTouchEnd = (e: React.TouchEvent<HTMLDivElement>) => {
    if (touchStartX.current === null) return;
    const delta = e.changedTouches[0].clientX - touchStartX.current;
    if (delta > 50) {
      prev();
    } else if (delta < -50) {
      next();
    }
  };

  useEffect(() => {
    if (!autoplayMs || isPaused || rangeSize <= 1) return;
    const id = setInterval(next, autoplayMs);
    return () => clearInterval(id);
  }, [autoplayMs, isPaused, next, rangeSize]);

  // Keyboard support
  useEffect(() => {
    const onKey = (e: any) => {
      if (e.key === "ArrowLeft") prev();
      if (e.key === "ArrowRight") next();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [next, prev]);

  const dotCount = Math.ceil(count / itemsPerSlide);
  const slideWidthPercent = 100 / itemsPerSlide;

  if (count === 0) {
    return <div className="carousel-empty">No Categories Found</div>;
  }

  return (
    <div
      className="carousel"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      role="region"
      aria-roledescription="carousel"
      aria-label="Category carousel"
    >
      <div
        className="carousel-viewport"
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
      >
        <div
          className="carousel-track"
          style={
            {
              transform: `translateX(-${index * slideWidthPercent}%)`,
              "--slide-width": `${slideWidthPercent}%`,
            } as React.CSSProperties
          }
        >
          {categories.map((c, i) => (
            <button
              type="button"
              key={c.id ?? i}
              className={`carousel-slide category-tile ${selected === c.id ? "active" : ""
                }`}
              aria-hidden={!(i >= index && i < index + itemsPerSlide)}
              onClick={() => onSelect?.(c.id)}
            >
              {c.imageUrl ? (
                <img
                  className="category-tile-image"
                  src={c.imageUrl}
                  alt={c.name}
                  loading="lazy"
                  draggable={false}
                />
              ) : (
                <span
                  className={`category-tile-image category-tile-fallback ${c.name === "All" ? "category-tile-all" : ""
                    }`}
                >
                  {c.name === "All" ? "All" : c.name.charAt(0)}
                </span>
              )}
              <span className="category-tile-caption">{c.name}</span>
            </button>
          ))}
        </div>
      </div>

      {rangeSize > 1 && (
        <>
          <button
            onClick={prev}
            aria-label="Previous Slide"
            className="carousel-btn carousel-btn-prev"
          >
            &#8249;
          </button>
          <button
            onClick={next}
            aria-label="Next Slide"
            className="carousel-btn carousel-btn-next"
          >
            &#8250;
          </button>
        </>
      )}

      {rangeSize > 1 && (
        <div className="carousel-dots">
          {Array.from({ length: dotCount }).map((_, i) => {
            const isActive =
              index >= i * itemsPerSlide && index < (i + 1) * itemsPerSlide;
            return (
              <button
                key={i}
                onClick={() => goToPage(i)}
                aria-label={`Go to slide ${i + 1}`}
                aria-current={isActive}
                className={`carousel-dot ${isActive ? "carousel-dot-active" : ""
                  }`}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}
