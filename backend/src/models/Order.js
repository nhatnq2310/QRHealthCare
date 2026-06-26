import mongoose from "mongoose";
import { idTransform } from "./User.js";

const orderItem = new mongoose.Schema(
  {
    productId:        String,
    productName:      String,
    price:            Number,
    quantity:         Number,
    emergencyContact: { type: String, default: "" }, // phone number entered at add-to-cart time
  },
  { _id: false }
);

const shippingAddress = new mongoose.Schema(
  {
    fullName: { type: String, default: "" },
    phone:    { type: String, default: "" },
    address:  { type: String, default: "" },  // street address
    city:     { type: String, default: "" },
    note:     { type: String, default: "" },  // optional delivery note
  },
  { _id: false }
);

const orderSchema = new mongoose.Schema(
  {
    userId:        { type: String, required: true, index: true },
    profileId:     { type: String, default: "" },
    items:         { type: [orderItem], default: [] },
    totalAmount:   { type: Number, default: 0 },     // final amount after discount
    discountAmount:{ type: Number, default: 0 },     // amount knocked off by the coupon
    couponCode:    { type: String, default: "" },    // empty when no coupon used
    paymentMethod: { type: String, default: "" }, // "vietqr" | "cash" | ...
    status:        { type: String, default: "pending" },
    shippingAddress: { type: shippingAddress, default: () => ({}) }, // per-order delivery details
    qrTagIds:      { type: [String], default: [] },
    createdAt:     { type: Number, default: () => Date.now() },
  },
  { toJSON: idTransform }
);

export default mongoose.model("Order", orderSchema);
