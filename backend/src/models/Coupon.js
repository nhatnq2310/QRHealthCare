import mongoose from "mongoose";
import { idTransform } from "./User.js";

const couponSchema = new mongoose.Schema(
  {
    code:           { type: String, required: true, unique: true, uppercase: true, trim: true, index: true },
    description:    { type: String, default: "" },
    discountType:   { type: String, enum: ["percent", "fixed"], required: true },
    discountValue:  { type: Number, required: true }, // percent (1-100) OR VND amount
    minOrderAmount: { type: Number, default: 0 },     // subtotal must reach this to qualify
    maxDiscount:    { type: Number, default: null },  // cap for percent discounts; null = uncapped
    expiresAt:      { type: Number, default: null },  // epoch ms; null = never expires
    active:         { type: Boolean, default: true },
    usageLimit:     { type: Number, default: null }, // null = unlimited
    usageCount:     { type: Number, default: 0 },
    hidden:         { type: Boolean, default: false }, // true = secret code, not shown in the public store banner
    createdAt:      { type: Number, default: () => Date.now() },
  },
  { toJSON: idTransform }
);

export default mongoose.model("Coupon", couponSchema);
