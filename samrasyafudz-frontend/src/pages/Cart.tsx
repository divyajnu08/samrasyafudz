import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import "./Cart.css";

export default function CartPage() {
  const { cart, loading, refreshCart, updateQuantity, removeItem } = useCart();
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  async function handleQuantityChange( productId:number, variantId:number , quantity: number) {
    if (quantity < 0) return;
    setError(null);
    try {
      await updateQuantity(productId,variantId, quantity);
    } catch (err: any) {
      setError(err?.response?.data?.message || "Could not update quantity.");
    }
  }

  async function handleRemove(productId:number, variantId:number ,) {
    setError(null);
    try {
      await removeItem(productId,variantId);
    } catch {
      setError("Could not remove item.");
    }
  }

  if (loading) {
    return <div className="container"><p className="home-status">Loading your cart…</p></div>;
  }
  console.log(cart);
  if (!cart || cart.items.length === 0) {
    return (
      <div className="container cart-page">
        <h1>Your cart</h1>
        <p className="home-status">Your cart is empty.</p>
        <Link to="/" className="btn-primary">Browse products</Link>
      </div>
    );
  }

  return (
    <div className="container cart-page">
      <h1>Your cart</h1>

      {error && <p className="error-text">{error}</p>}

      <div className="cart-items">
        {cart.items.map((item) => (
          item.quantity > 0 && (
            <div key={item.id} className="cart-item">
              <div className="cart-item-info">
                <h3>{item.productName}</h3>
                <span className="cart-item-weight">{item.weightGrams}g</span>
                <span className="cart-item-price">₹{item.unitPrice.toFixed(0)} each</span>
            </div>

            <div className="cart-item-quantity">
              <button
                className="cart-qty-btn"
                onClick={() => handleQuantityChange(item.productId, item.variantId, item.quantity - 1)}
                disabled={item.quantity <= 0}
              >
                −
              </button>
              <span>{item.quantity}</span>
              <button
                className="cart-qty-btn"
                onClick={() => handleQuantityChange(item.productId, item.variantId, item.quantity + 1)}
              >
                +
              </button>
            </div>

            <div className="cart-item-subtotal">₹{item.subtotal.toFixed(0)}</div>

            <button className="cart-item-remove" onClick={() => handleRemove(item.productId,item.variantId)}>
              Remove
            </button>
          </div>
          )
        ))}
      </div>

      <div className="cart-summary">
        <div className="cart-summary-total">
          <span>Total</span>
          <span>₹{cart.total.toFixed(0)}</span>
        </div>
        {cart.items.some(item => item.quantity > 0) && (
          <button className="btn-primary cart-checkout-btn" onClick={() => navigate("/checkout")}>
            Proceed to checkout
          </button>
        )}
      </div>
    </div>
  );
}