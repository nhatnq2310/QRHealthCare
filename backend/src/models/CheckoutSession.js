import mongoose from "mongoose";
import { idTransform } from "./User.js";

/**
 * One row per checkout attempt. Created the moment a user lands on the
 * shipping-info screen (before anything is saved as an Order), then updated
 * as they move through the funnel. This is what lets us measure abandonment
 * and per-step drop-out — the Order collection alone only shows completed
 * checkouts, so it can't tell us where people quit.
 *
 * step values (furthest step reached):
 *   1 = shipping info screen opened
 *   2 = profile selected
 *   3 = payment method selected
 *   4 = order confirmed (completed = true, mirrors an Order)
 */
const checkoutSessionSchema = new mongoose.Schema(
  {
    userId:        { type: String, required: true, index: true },
    cartValue:     { type: Number, default: 0 },   // subtotal at session start, for value-of-abandoned-carts reporting
    itemCount:     { type: Number, default: 0 },
    step:          { type: Number, default: 1 },   // furthest step reached so far
    completed:     { type: Boolean, default: false },
    orderId:       { type: String, default: "" },  // set once the matching Order is created
    paymentMethod: { type: String, default: "" },
    startedAt:     { type: Number, default: () => Date.now() },
    updatedAt:     { type: Number, default: () => Date.now() },
  },
  { toJSON: idTransform }
);

export default mongoose.model("CheckoutSession", checkoutSessionSchema);
